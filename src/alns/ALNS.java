/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package alns;

import com.sun.javafx.scene.control.skin.VirtualFlow;
import gurobi.GRB;
import gurobi.GRBCallback;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBVar;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Frash
 */
public class ALNS extends Orienteering{
    
    /**
     * History of past inserted clusters (long term memory)
     */
    private Deque<Cluster> pastHistory;
    
    /**
     * Maximum size of the past history
     */
    private int maxHistorySize;
    
    /**
     * Number of iterations in an optimization segment
     */
    private int segmentSize;
    
    /**
     * This is an ObjectDistribution for destroy methods.
     * It contains references to all the destroy heuristics for this ALNS implementation
     * and allows ALNS objects to randomly extract such methods according to the
     * weight as defined by the ALNS procedure.
     */
    private ObjectDistribution<BiFunction<List<Cluster>,Integer,List<Cluster>>> destroyMethods;
    
    /**
     * This is an ObjectDistribution for repair methods.
     * It contains references to all the repair heuristics for this ALNS implementation
     * and allows ALNS objects to randomly extract such methods according to the
     * weight as defined by the ALNS procedure.
     */
    private ObjectDistribution<BiFunction<List<Cluster>,Integer,List<Cluster>>> repairMethods;
    
    /**
     * This is the decay parameter of the update process for heuristic method weights.
     * <br>This value should be a double in the interval [0,1].
     * <br>Heuristic method weights are updated following the convex combination
     * <br> newWeight = lambda*oldWeight + (1-lambda)*psi
     * <br>where psi is a value that indicates the relative score to give to an heuristic.
     */
    private double lambda;
    
    /**
     * This is the decay parameter of the update process for Temperature.
     * <br>This value should be a double in the interval [0,1].
     * <br>The temperature is updated at the end of every segment like
     * <br>newTemperature = alpha*Temperature
     * <br>so that a slowly decreasing temperature (alpha->1) will make fluctuations
     * in accepted solutions much stronger
     */
    private double alpha;
    
    /**
     * This constant holds the possible values of psi, the function that prizes
     * good heuristics.
     */
    final static double[] HEURISTIC_SCORES = {3.0, 2.0, 1.0, 0.0};
    
    public ALNS(Orienteering o, int segmentSize, int maxHistorySize, double lambda) throws Exception{
        super(o);
        this.segmentSize = segmentSize;
        this.maxHistorySize=maxHistorySize;
        this.pastHistory = new LinkedBlockingDeque<>();
        
        // Lambda setup - values out of range [0,1] clip to range boundaries
        if(lambda < 1.0) this.lambda = lambda;
        else this.lambda = 1.0;
        if(lambda < 0) this.lambda = 0.0;
        
        // Keeping track of all implemented repair and destroy methods
        destroyMethods = new ObjectDistribution<>();
        destroyMethods.add(this::destroyHeuristicTemplate);
        
        repairMethods = new ObjectDistribution<>();
        repairMethods.add(this::repairHeuristicTemplate);
        
    }
    
    /**
     * ALNS first step: build a feasible solution to bootstrap the ALNS procedure.
     * Tentative solutions are built from combinations of promising clusters until
     * an infeasible solution is found. The last feasible solution after an
     * infeasible solution was found is the starting solution.
     * @return the feasible solution found, represented as a list of selected clusters.
     * @throws GRBException if anything goes wrong with logging or callback mechanisms
     */
    public List<Cluster> ALNSConstructiveSolution() throws GRBException, Exception{
        env.message("ALNSConstructiveSolution log start, time "+LocalDateTime.now()+"\n");
        
        List<Cluster> clusters = instance.cloneClusters();
        // Update the vehicle list for each cluster, might take some time
        // This is needed to compute all the parameters for the sorting criteria
        // of clusters
        clusters.forEach(c -> c.setInstanceVehicles(instance.getVehicles()));
        
        // First of all, we must sort the clusters in the instance following the
        // sorting criterion: decreasing order of profit/(number of vehicles in cluster * service duration)
        clusters.sort(Cluster.WEIGHTED_PROFIT_COMPARATOR.reversed());
        
        // Then let's sort clusters by node number in increasing order
        clusters.sort(Cluster.NODE_NUMBER_COMPARATOR);
        
        // Finally, let's sort clusters by maximum vehicle number in increasing order
        clusters.sort(Cluster.MAX_VEHICLES_COMPARATOR);
        
        // Let's keep track of an old and a new solution as sets of clusters
        List<Cluster> solution = new ArrayList<>();
        List<Cluster> newSolution = new ArrayList<>();
        // isFeasible keeps track of the feasibility of the new solution found
        boolean isFeasible = true;
        for(int i = 0; i<clusters.size() && isFeasible; i++){
            Cluster c = clusters.get(i);
            // Let's extract the first cluster from the ordered list of clusters
            // and let's put it into the new solution
            newSolution.add(c);
            
            // Let's use gurobi to check the feasibility of the new solution
            // TODO: find a way to do so and change isFeasible accordingly
            isFeasible = this.testSolution(newSolution, true);
            // If the new solution is feasible, update the old solution
            if(isFeasible){
                solution = new ArrayList<>(newSolution);
            }
        }
        
        // Now solution holds the list of clusters found by our constructive algorithm.
        // Let's update the model so that it cointains the current solution
        testSolution(solution, false);
        return solution;
    }
    
    /**
     * use Gurobi to check whether the proposed solution is feasible or not.
     * @param proposedSolution the solution we want to test
     * @param log true will produce a visible log
     * @return true is the solution is feasible
     */
    private boolean testSolution(List<Cluster> proposedSolution, boolean log) throws GRBException, Exception {
        boolean isFeasible = false;
        
        // Reset the model to an unsolved state, this will allow us to test our solutions freely
        model.reset();
        
        // Clear the solution in the model: no cluster will be choseable at the beginning
        this.clearSolution();
        
        // Place the selected clusters in solution
        this.putInSolution(proposedSolution);
        
        // Setting up the callback
        model.setCallback(new feasibilityCallback());
        model.optimize();
        if(model.get(GRB.IntAttr.SolCount) > 0) isFeasible = true;
        
        if(log){
            env.message("\nTesting solution with clusters: [");
        
            proposedSolution.forEach(c -> {
                try {
                    env.message(c.getId()+" ");
                } catch (GRBException ex) {
                    Logger.getLogger(Orienteering.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            
            if(isFeasible){
                env.message("]: FEASIBLE integer solution found!");
                this.logVisitedClusters();
                this.logVehiclePaths();
            }
            else{
                env.message("]: INFEASIBLE.\n\n");
            }
        }
        
        // Clean up the solution: now every solution can be chosable again
        this.resetSolution();
        
        return isFeasible;
    }
    
    /**
     * Adds the selected cluster to the solution.
     * @param c the cluster to put into solution.
     * @throws GRBException if anything goes wrong with setting the upper bound.
     */
    private void putInSolution(Cluster c) throws GRBException{
        y[c.getId()].set(GRB.DoubleAttr.LB, 1.0);
        y[c.getId()].set(GRB.DoubleAttr.UB, 1.0);
        model.update();
    }
    
    /**
     * Remove the selected cluster from the solution.
     * @param c the cluster to remove from solution.
     * @throws GRBException 
     */
    private void removeFromSolution(Cluster c) throws GRBException{
        y[c.getId()].set(GRB.DoubleAttr.LB, 0.0);
        y[c.getId()].set(GRB.DoubleAttr.UB, 0.0);
        model.update();
    }
    
    /**
     * Adds the selected clusters to the solution.
     * @param l the list of clusters to put into solution.
     * @throws GRBException if anything goes wrong with updating the model.
     */
    private void putInSolution(List<Cluster> l) throws GRBException{
        l.forEach(c -> {
            try {
                y[c.getId()].set(GRB.DoubleAttr.LB, 1.0);
                y[c.getId()].set(GRB.DoubleAttr.UB, 1.0);
            } catch (GRBException ex) {
                Logger.getLogger(Orienteering.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        model.update();
    }
    
    /**
     * Remove the selected clusters from the solution.
     * @param l the list of clusters to remove from the solution.
     * @throws GRBException if anything goes wrong with updating the model.
     */
    private void removeFromSolution(List<Cluster> l) throws GRBException{
        l.forEach(c -> {
            try {
                y[c.getId()].set(GRB.DoubleAttr.LB, 0.0);
                y[c.getId()].set(GRB.DoubleAttr.UB, 0.0);
            } catch (GRBException ex) {
                Logger.getLogger(Orienteering.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        model.update();
    }
    
    /**
     * Reset the solution: all clusters will be free to be chosen
     * @throws GRBException if setting the bounds goes wrong
     */
    private void resetSolution() throws GRBException{
        for(int c = 0; c<instance.getNum_clusters(); c++){
            y[c].set(GRB.DoubleAttr.LB, 0.0);
            y[c].set(GRB.DoubleAttr.UB, 1.0);
        }
        
        model.update();
    }
    
    /**
     * Clear the solution: no clusters will be selectable by the solver
     * @throws GRBException if setting the bounds goes wrong
     */
    private void clearSolution() throws GRBException{
        for(int c = 0; c<instance.getNum_clusters(); c++){
            y[c].set(GRB.DoubleAttr.LB, 0.0);
            y[c].set(GRB.DoubleAttr.UB, 0.0);
        }
        model.update();
    }
    
    /**
     * Run the ALNS optimization on the current Orienteering problem.
     * <br>At the beginning a feasible solution is created by a constructive method.
     * <br>Then, until a stopping criterion is met, a cycle will test different
     * solutions generated through chosen destroy and repair heuristics.
     * <br>Destroy heuristics will move the solution towards an infeasible state
     * by inserting a number <tt>q</tt> of clusters into the solution.
     * <br>Repair heuristics will move the solution back towards feasibility by
     * removing a number <tt>q</tt> of clusters from the solution.
     * <br>The repair heuristic is called upon the result of the destroy heuristic
     * as applied to the last solution found.
     * <br>The chosen solution, regardless of it being feasible, infeasible,
     * pejorative or ameliorative, will be accepted using a simulated annhealing criterion.
     * <br>At the end, if a stopping criterion is met (one of maximum number of iterations met,
     * maximum number of iterations from the last improvement, maximum
     * computation time) the cycle ends and the last best feasible solution is
     * returned.
     * @throws Exception if anything goes wrong
     */
    public void optimize() throws Exception {
        // This is a generic ALNS implementation
        GRBModel relaxedModel = model.relax();
        relaxedModel.optimize();
        
        // Temperature mitigates the effects of the simulated annealing process
        // A lower temperature will make it less likely for the proccess to accept
        // a pejorative solution
        double temperature = 2*relaxedModel.get(GRB.DoubleAttr.ObjBound);
        
        
        // stores the previous solution
        List<Cluster> xOld = this.ALNSConstructiveSolution();
        // stores the new solution
        List<Cluster> xNew = xOld;
        // stores the best solution
        List<Cluster> xBest = xOld;
        
        // old value of the objective function
        double oldObjectiveValue = model.get(GRB.DoubleAttr.ObjVal);
        // new value of the objective function
        double newObjectiveValue = oldObjectiveValue;
        // best value of the objective function
        double bestObjectiveValue = oldObjectiveValue;
        
        // q is the number of clusters to repair and destroy at each iteration
        int q;
        
        // qMax is the maximum number of clusters in the instance
        int qMax = instance.getNum_clusters();
        
        // This is how much q should increase at every iteration
        int qDelta = Math.floorDiv(qMax, 10);
        
        // These variables are references to the chosen destroy and repair methods
        // for the current iteration
        BiFunction<List<Cluster>,Integer,List<Cluster>> destroyMethod;
        BiFunction<List<Cluster>,Integer,List<Cluster>> repairMethod;
        
        // Parameters to determine how to update heuristic weights
        boolean solutionIsNewGlobalOptimum;  // true if c(xNew)>c(xBest)
        boolean solutionIsBetterThanCurrent; // true if c(xNew)>c(xOld)
        boolean solutionIsWorseButAccepted;  // true id c(xNew)<=c(xOld) but xNew is accepted
        boolean solutionIsWorseAndRejected;  // true id c(xNew)<=c(xOld) and xNew is rejected
        
        // Setup of stopping criterions
        long maxTimeInSeconds = 600;
        LocalDateTime maxTime = LocalDateTime.now().plusSeconds(maxTimeInSeconds);
        
        long segments = 0;
        long maxSegments = 1000;
        
        long segmentsWithoutImprovement = 0;
        long maxSegmentsWithoutImprovement = 20;
        
        double bestObjectiveInSegments = oldObjectiveValue;
        List<Cluster> xBestInSegments = xOld;
        
        // TODO: improve logging
        // Cycles every segment
        do{
            q=1;

            // Iterations inside a segment
            for(int iterations = 0; iterations < this.segmentSize; iterations++){
                // Setup of boolean values to evaluate solution quality
                solutionIsNewGlobalOptimum = false;
                solutionIsBetterThanCurrent= false;
                solutionIsWorseButAccepted = false;
                solutionIsWorseAndRejected = false;

                // Picking a destroy and a repair method
                destroyMethod = pickDestroyMethod();
                repairMethod = pickRepairMethod();

                // Apply the methods on the solution
                xNew = repairMethod.apply(destroyMethod.apply(xOld, q), q);
                // Gather the value of the new objective obtained through the chosen methods
                newObjectiveValue = model.get(GRB.DoubleAttr.ObjVal);

                // Solution evaluation: if the solution improves the obj function, save it
                // and give a prize to the heuristics coherent with their performance
                solutionIsBetterThanCurrent = (newObjectiveValue > oldObjectiveValue);
                if(acceptSolution(oldObjectiveValue, newObjectiveValue, temperature)){
                    if(!solutionIsBetterThanCurrent) solutionIsWorseButAccepted=true;
                    xOld = xNew;
                    oldObjectiveValue = newObjectiveValue;
                }
                else if(!solutionIsBetterThanCurrent) solutionIsWorseAndRejected=true;
                if(newObjectiveValue>bestObjectiveValue){
                    xBest = xNew;
                    bestObjectiveValue = newObjectiveValue;
                    solutionIsNewGlobalOptimum = true;
                }
                
                // Update the heuristic weights
                updateHeuristicMethodsWeight(destroyMethod, repairMethod,
                        solutionIsNewGlobalOptimum,
                        solutionIsBetterThanCurrent,
                        solutionIsWorseButAccepted,
                        solutionIsWorseAndRejected);    
                
                // Update q
                q += qDelta;
                if(q>=qMax) q=qMax;
            } // end of the segment
            
            // At the end of the segment, the best solution is in xBest, with a value in bestObjectiveValue
            
            // This would be a nice spot to use some local search algorithm
            
            // We check whether there was an improvement from last segment to this one
            if(bestObjectiveInSegments<bestObjectiveValue){
                // There was an improvement!
                bestObjectiveInSegments = bestObjectiveValue;
                xBestInSegments = xBest;
                // Reset the no-improvement counter
                segmentsWithoutImprovement = 0;
            }
            else{
                // There was no improvement: update the segment counter
                segmentsWithoutImprovement++;
            }
            
        }
        // Stopping criteria
        while(LocalDateTime.now().isAfter(maxTime)
                && segments < maxSegments
                && segmentsWithoutImprovement<maxSegmentsWithoutImprovement);
        
    }
    
    /**
     * Picks a random destroy method from the available ones.
     * <br>The probability of getting a specific method depends on its weight in
     * the <tt>destroyMethods</tt> distribution.
     * @return a random destroy method
     */
    private BiFunction<List<Cluster>,Integer,List<Cluster>> pickDestroyMethod() {
        return this.destroyMethods.getRandom();
    }
    
    /**
     * Pick a random repair method from the available ones.
     * <br>The probability of getting a specific method depends on its weight in
     * the <tt>repairMethods</tt> distribution.
     * @return 
     */
    private BiFunction<List<Cluster>, Integer, List<Cluster>> pickRepairMethod() {
        return this.repairMethods.getRandom();
    }
    
    // ACCEPTION CRITERION
    
    /**
     * Evaluates the new and the old value of the objective function to decide
     * whether the new solution should be accepted or refused.
     * <br>The acceptance criterion is a Simulated Annealing process, repurposed
     * for a maximization problem. A new solution that improves the maximization
     * of the objective function is always accepted, while a pejorative solution
     * can be chosen with probability <br>
     * exp((newObjectiveValue - oldObjectiveValue)/temperature)
     * @param oldObjectiveValue old best value of the objective function
     * @param newObjectiveValue new value of the objective function, obtained through the new solution
     * @param temperature a weighting parameter which should be made slowly decreasing by the caller
     * @return <tt>true</tt> if the new solution is accepted.
     */
    private boolean acceptSolution(double oldObjectiveValue, double newObjectiveValue, double temperature){
        /**
         * The following text defines simulated annealing for a minimum problem
         * 
         * *    The probability function P must be positive
         *      even when e' is greater than e.
         *      This feature prevents the method from becoming stuck at a local minimum
         *      that is worse than the global one.
         * 
         * *    When T tends to zero, the probability P(e,e',T) must tend 
         *      to zero if e'>e and to a positive value otherwise.
         *      For sufficiently small values of T, the system
         *      will then increasingly favor moves that go "downhill" (i.e., to 
         *      lower energy values), and avoid those that go "uphill." With T = 0
         *      the procedure reduces to the greedy algorithm, which makes only the downhill transitions.
         * 
         * *    In the original description of SA, the probability P(e,e',T) was
         *      equal to 1 when e > e' — i.e., the procedure always moved downhill
         *      when it found a way to do so, irrespective of the temperature.
         *      Many descriptions and implementations of SA still take this condition
         *      as part of the method's definition. However, this condition is not
         *      essential for the method to work.
         * 
         * *    The P function is usually chosen so that the probability of accepting
         *      a move decreases when the difference e'-e increases
         *      —that is, small uphill moves are more likely than large ones.
         *      However, this requirement is not strictly necessary, provided that
         *      the above requirements are met.
         */
        
        Random r = new Random();
        
        /**
         * This is a problem of maximization, so the value of the objective function
         * must always increase when possible and could decrease with a specified probability
         * if a pejorative solution is found.
         * 
         * Let's define:
         *  DIFF = newObjectiveValue - oldObjectiveValue > 0
         *  T>0
         * So, if we improve our solution we get that
         *  DIFF>=0 and P=exp(DIFF/T) >= 1 => the solution is always accepted
         * If the solution is worse
         *  DIFF<0 and 0 < P=exp(DIFF/T) < 1 => the solution is accepted with probability P
         */
        return r.nextDouble() < Math.exp( (newObjectiveValue - oldObjectiveValue)/temperature);
    }
    
    // DESTROY HEURISTICS
    private List<Cluster> destroyHeuristicTemplate(List<Cluster> inputSolution, int q){
        // Initialize available clusters
        List<Cluster> availableClusters = this.getClustersNotInSolution(inputSolution);
        
        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);
        
        // Sort the available clusters
        availableClusters.sort(Cluster.PROFIT_COMPARATOR.reversed());
        
        // Add q available clusters (or as many clusters as possible) to the solution
        for(int i = 0; i<q && i<availableClusters.size(); i++){
            output.add(availableClusters.get(i));
        }
        
        // return the destroyed input
        return output;
    }
    
    // REPAIR HEURISTICS
    private List<Cluster> repairHeuristicTemplate(List<Cluster> inputSolution, int q){
        // Sort the clusters in the input solution
        inputSolution.sort(Cluster.COST_COMPARATOR.reversed());
        
        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);
        
        // Remove q clusters from the solution, following the imposed ordering
        int i = 0;
        for(Cluster c : inputSolution){
            output.remove(c);
            i++;
            if(i>=q) break;
        }
        
        // Return the repaired input
        return output;
    }
    
    /**
     * Removes the first q clusters with the least profit/cost ratio.
     * @param inputSolution the solution to repair
     * @param q number of clusters to remove
     * @return the repaired solution
     */
    private List<Cluster> repairWorstRemoval(List<Cluster> inputSolution, int q){
        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);
        
        // Sort the clusters in the input solution
        output.sort(Cluster.PROFIT_COST_RATIO_COMPARATOR);
        
        // Remove q clusters from the solution, following the imposed ordering
        int i = 0;
        for(Cluster c : inputSolution){
            output.remove(c);
            i++;
            if(i>=q) break;
        }
        
        // Return the repaired input
        return output;
    }
    
    /**
     * This is a special repair heuristic to bring back an eventual infeasible solution into feasibility.
     * It operates by removing the minimum number of low gain clusters when their
     * total cost is more than or equal the difference between the maximum cost for the solution (Tmax)
     * and the actual cost of the solution.
     * @param inputSolution an infeasible solution
     * @return a feasible solution
     */
    private List<Cluster> repairBackToFeasibility(List<Cluster> inputSolution) throws Exception{
        // TODO:
        // 0. Check feasibility. If infeasible goto 1, else goto 9
        // 1. Compute the IIS model so that we can retrieve some information on z and Tmax
        // 2. Look at how much into infeasibility we are, so get the maxZ
        // 3. Compute costDifference = maxZ-Tmax
        // 4. Sort inputSolution clusters by increasing profit
        // 5. Sort inputSolution clusters by increasing service time (cost)
        // 6. Get the first cluster from sorted inputSolution with cost >= costDifference
        // 7. Remove the cluster found in 6.
        // 8. Goto 0
        // 9. Return the new feasible solution
        
        GRBModel feasibilityRelaxation = new GRBModel(model);
        
        List<Cluster> output = new ArrayList<>(inputSolution);
        boolean isFeasible = testSolution(output, true);
        // 0. Check feasibility
        while(!isFeasible){
            // 1. Compute the feasibilityRelaxation so that we can retrieve some information on z and Tmax
            feasibilityRelaxation.feasRelax(segmentSize, isFeasible, y, HEURISTIC_SCORES, HEURISTIC_SCORES, grbcs, HEURISTIC_SCORES);
            
            // Find the maximum value for z
            double maxZ = -1;
            double maxI = -1;
            double maxJ = -1;
            double tempZ;
            for(int i=0;i<instance.getNum_nodes();i++){
                for(int j=0; j<instance.getNum_nodes();j++){
                    tempZ = z[i][j].get(GRB.DoubleAttr.X);
                    if(tempZ>maxZ){
                        maxI=i;
                        maxJ=j;
                        maxZ=tempZ;
                    }
                }
            }
            
            // 3. Compute costDifference = maxZ-Tmax
            double costDifference = maxZ-instance.getTmax();
            
            // 4. Sort inputSolution clusters by increasing profit
            output.sort(Cluster.PROFIT_COMPARATOR);
            
            // 5. Sort inputSolution clusters by increasing service time (cost)
            output.sort(Cluster.COST_COMPARATOR);
            
            // 6. Get the first cluster from sorted inputSolution with cost >= costDifference
            Cluster toRemove = null;
            for(Cluster c : output){
                if(c.getTotalCost()>=costDifference){
                    toRemove=c;
                    break;
                }
            }
            
            // 7. Remove the cluster found in 6.
            output.remove(toRemove);
            
            // 8. Goto 0 (test feasibility)
            isFeasible = testSolution(output, true);
        }
        
        return output;
    }
    
    /**
     * Given some solution quality flags, update the weights of the selected destroy
     * and repair methods
     * @param destroyMethod the destroy method to update the weight of
     * @param repairMethod the repair method to update the weight of
     * @param solutionIsNewGlobalOptimum true if the new solution is a new global optimum
     * @param solutionIsBetterThanCurrent true if the new solution is better than the old one
     * @param solutionIsWorseButAccepted true if the new solution is worse than the old one, but is accepted anyway
     * @param solutionIsWorseAndRejected true if the new solution is worse than the old one and is rejected
     */
    private void updateHeuristicMethodsWeight(
            BiFunction<List<Cluster>, Integer, List<Cluster>> destroyMethod,
            BiFunction<List<Cluster>, Integer, List<Cluster>> repairMethod,
            boolean solutionIsNewGlobalOptimum,
            boolean solutionIsBetterThanCurrent,
            boolean solutionIsWorseButAccepted,
            boolean solutionIsWorseAndRejected) {
        
        // Setup the prize that will be given to the selected heuristics
        double prize = 0.0;
        
        // Check solution quality to decide the prize
        if(solutionIsNewGlobalOptimum){
            prize = HEURISTIC_SCORES[0];
        }
        else if(solutionIsBetterThanCurrent){
            prize = HEURISTIC_SCORES[1];
        }
        else if(solutionIsWorseButAccepted){
            prize = HEURISTIC_SCORES[2];
        }
        else if(solutionIsWorseAndRejected){
            prize = HEURISTIC_SCORES[3];
        }
        
        // Give the prize to the selected heuristics, with the best compliments from the house
        double repairOldWeight = repairMethods.getWeightOf(repairMethod);
        double destroyOldWeight = destroyMethods.getWeightOf(destroyMethod);
        
        repairMethods.updateWeightSafely(repairMethod, repairOldWeight*lambda + (1-lambda)*repairOldWeight);
        destroyMethods.updateWeightSafely(destroyMethod, destroyOldWeight*lambda + (1-lambda)*destroyOldWeight);
    }
    
    /**
     * Resets the weights (scores) of every repair and destroy heuristic method.
     * The new value will be 1.0.
     */
    private void resetHeuristicMethodsWeight(){
        repairMethods.resetWeights();
        destroyMethods.resetWeights();
    }
    
    /**
     * Return a list of available clusters which have not been chosen for the given
     * solution.
     * @param solution the solution to analyze
     * @return a list of available clusters which have not been chosen for the given
     * solution
     */
    private List<Cluster> getClustersNotInSolution(List<Cluster> solution){
        List<Cluster> availableClusters = this.instance.cloneClusters();
        availableClusters.removeAll(solution);
        return availableClusters;
    }
    
}

/**
 * Inner class to handle callbacks that check for the first feasible solution
 * @author Frash
 */
class feasibilityCallback extends GRBCallback{
    /**
     * Number of solution nodes to visit before the solver gives up on
     * searching for a feasible solution
     */
    private final static double NODES_BEFORE_ABORT = 100;
    
    @Override
    protected void callback() {
        try {
            if(where == GRB.CB_MIP){
                    if(getIntInfo(GRB.CB_MIP_SOLCNT)>0 || getDoubleInfo(GRB.CB_MIP_NODCNT)>NODES_BEFORE_ABORT)
                    {
                        abort();
                    }
            }
            else if(where == GRB.CB_MIPSOL){
                abort();
            }
        } catch (GRBException ex) {
            Logger.getLogger(Orienteering.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}