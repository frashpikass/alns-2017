/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package alns;

import com.sun.javafx.scene.control.skin.VirtualFlow;
import gurobi.GRB;
import gurobi.GRBCallback;
import gurobi.GRBConstr;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBExpr;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
     * <br>so that a slowly decreasing temperature (alpha-&gt;1) will make fluctuations
     * in accepted solutions much stronger
     */
    private double alpha;
    
    /**
     * This constant holds the possible values of psi, the function that prizes
     * good heuristics.
     */
    final static double[] HEURISTIC_SCORES = {3.0, 2.0, 1.0, 0.0};
    
    /**
     * Constructor for the class ALNS.
     * @param o an instance of an Orienteering problem
     * @param segmentSize the size of a segment (expressed in number of iteration of the destroy/repair heuristic)
     * @param maxHistorySize the maximum size for the history of previously chosen clusters
     * @param lambda decay parameter of heuristic weights (must be a double in range [0,1])
     * @param alpha decay parameter of temperature (must be a double in range [0,1])
     * @throws Exception if anything goes wrong
     */
    public ALNS(Orienteering o, int segmentSize, int maxHistorySize, double lambda, double alpha) throws Exception{
        super(o);
        this.segmentSize = segmentSize;
        this.maxHistorySize=maxHistorySize;
        this.pastHistory = new LinkedBlockingDeque<>();
        
        // Lambda setup - values out of range [0,1] clip to range boundaries
        if(lambda < 1.0) this.lambda = lambda;
        else this.lambda = 1.0;
        if(lambda < 0) this.lambda = 0.0;
        
        // Alpha setup - values out of range [0,1] clip to values close to range boundaries
        // we want the temperature to always decrease if not explicitly stated otherwise
        if(alpha < 1.0) this.alpha = alpha;
        else this.lambda = 0.9;
        if(lambda < 0) this.lambda = 0.1;
        
        // Keeping track of all implemented repair and destroy methods
        destroyMethods = new ObjectDistribution<>();
        // destroyMethods.add(this::destroyHeuristicTemplate);
        destroyMethods.add(this::destroyGreedyCostInsertion);
        destroyMethods.add(this::destroyGreedyBestInsertion);
        destroyMethods.add(this::destroyGreedyProfitInsertion);
        destroyMethods.add(this::destroyRandomInsertion);
        
        repairMethods = new ObjectDistribution<>();
        // repairMethods.add(this::repairHeuristicTemplate);
        repairMethods.add(this::repairHighCostRemoval);
        repairMethods.add(this::repairRandomRemoval);
        repairMethods.add(this::repairTravelTime);
        repairMethods.add(this::repairVehicleTime);
        repairMethods.add(this::repairWorstRemoval);
        
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
        return testSolution(this.model, proposedSolution, log);
    }
    
    /**
     * use Gurobi to check whether the proposed solution is feasible or not for the specified model.
     * @param model the model to test the solution on
     * @param proposedSolution the solution we want to test
     * @param log true will produce a visible log
     * @return true is the solution is feasible
     */
    private boolean testSolution(
            GRBModel model,
            List<Cluster> proposedSolution,
            boolean log) throws GRBException, Exception {
        
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
        
        // Resetting the callback
        model.setCallback(null);
        
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
        // TODO: change temperature update by following directions
        
        // This is a generic ALNS implementation
        GRBModel relaxedModel = model.relax();
        relaxedModel.optimize();
        
        // Temperature mitigates the effects of the simulated annealing process
        // A lower temperature will make it less likely for the proccess to accept
        // a pejorative solution
        double initialTtemperature = 2*relaxedModel.get(GRB.DoubleAttr.ObjBound);
        double temperature;
        
        // Memory cleanup
        relaxedModel.dispose();
        
        // STEP 1: generate a CONSTRUCTIVE SOLUTION
        // xOld stores the previous solution, the starting point of any iteration
        List<Cluster> xOld = this.ALNSConstructiveSolution();
        // stores the new solution, produced by the destroy and repair heuristics
        List<Cluster> xNew = xOld;
        // stores the best solution found through iterations
        List<Cluster> xBest = xOld;
        
        // old value of the objective function, generated through the old solution
        double oldObjectiveValue = model.get(GRB.DoubleAttr.ObjBound);
        // new value of the objective function produced by the current iteration
        double newObjectiveValue = oldObjectiveValue;
        // best value of the objective function throughout iterations
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
        long maxTimeInSeconds = 6000;
        LocalDateTime maxTime = LocalDateTime.now().plusSeconds(maxTimeInSeconds);
        
        // Keeping track of how many segments of iterations have been performed
        long segments = 0;
        long maxSegments = 1000;
        
        // Keeping track of how many segments without improvement have been seen
        long segmentsWithoutImprovement = 0;
        long maxSegmentsWithoutImprovement = 20;
        
        double bestObjectiveInSegments = oldObjectiveValue;
        List<Cluster> xBestInSegments = xOld;
        
        // TODO: improve logging
        // ALNS START: Cycles every segment
        do{
            q=1;
            temperature = initialTtemperature;
            
            // Reset heuristic weights
            this.resetHeuristicMethodsWeight();

            // Iterations inside a segment
            for(int iterations = 0; iterations < this.segmentSize && q<=qMax; iterations++){
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
                // Gather the value of the objective function for the new solution
                // obtained through the chosen methods
                // TODO: check whether it would be better to query for ObjBound instead
                // ...or whether I should go back to feasibility before attempting further heuristics
                if(testSolution(xNew, true)){
                    newObjectiveValue = model.get(GRB.DoubleAttr.ObjVal);
                }
                else{
//                    // CHOICE 1: discard the solution, penalize the two heuristics
//                    updateHeuristicMethodsWeight(destroyMethod,
//                            repairMethod,
//                            false,
//                            false,
//                            false,
//                            true);
//                    // Update q
//                    q += qDelta;
//
//                    // Update temperature
//                    temperature *= alpha;
//                    continue;

//                    // CHOICE 2: get the best bound on the objective and continue
//                    newObjectiveValue = model.get(GRB.DoubleAttr.ObjBound);
                    
                    // CHOICE 3: bring the solution back to feasibility and go on
                    xNew = repairBackToFeasibility(xNew);
                    newObjectiveValue = model.get(GRB.DoubleAttr.ObjVal);
                }
                
                // In any case, now the model has been updated and it stores data
                // about the current solution
                
                // Solution evaluation: if the solution improves the obj function, save it
                // and give a prize to the heuristics according to their performance
                solutionIsBetterThanCurrent = (newObjectiveValue > oldObjectiveValue);
                if(acceptSolution(oldObjectiveValue, newObjectiveValue, temperature)){
                    if(!solutionIsBetterThanCurrent) solutionIsWorseButAccepted=true;
                    xOld = xNew;
                    oldObjectiveValue = newObjectiveValue;
                }
                else if(!solutionIsBetterThanCurrent) solutionIsWorseAndRejected=true;
                if(newObjectiveValue > bestObjectiveValue){
                    xBest = xNew;
                    bestObjectiveValue = newObjectiveValue;
                    solutionIsNewGlobalOptimum = true;
                    // So, let's check if we've reached the optimum (haha, fat chance)
                    if(model.get(GRB.IntAttr.Status) == GRB.OPTIMAL)
                        break;
                }
                
                // Update the heuristic weights
                updateHeuristicMethodsWeight(destroyMethod, repairMethod,
                        solutionIsNewGlobalOptimum,
                        solutionIsBetterThanCurrent,
                        solutionIsWorseButAccepted,
                        solutionIsWorseAndRejected);    
                
                // Update q
                q += qDelta;
                
                // Update temperature at the end of every iteration
                temperature *= alpha;
            } // for: end of the segment
            
            // At the end of the segment, the best solution is in xBest, with a value in bestObjectiveValue
            // This would be a nice spot to use some local search algorithm:
            // repairBackToFeasibility will check whether the solution found in
            // this segment is infeasible or not; if it is infeasible it will bring
            // it back to feasibility through a special repair heuristic
            List<Cluster> xBestRepaired = repairBackToFeasibility(xBest);
            
            // If xBest and xBestRepaired are not the same solution, make sure the feasible one is the best
            if(!(xBest.containsAll(xBestRepaired) && xBestRepaired.containsAll(xBest))){
                // TODO: I'm not sure about this, I need to check again tomorrow; update bestObjectiveValue accordingly
                xBest = xBestRepaired;
                bestObjectiveValue = model.get(GRB.DoubleAttr.ObjVal);
            }
            
            // We check whether there was an improvement from last segment to this one
            if(bestObjectiveInSegments < bestObjectiveValue){
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
            
        } // do: Stopping criteria
        while(!LocalDateTime.now().isAfter(maxTime)
                && segments < maxSegments
                && segmentsWithoutImprovement<maxSegmentsWithoutImprovement
                && model.get(GRB.IntAttr.Status) != GRB.OPTIMAL);
        
        // Final test to set variables in the model
        testSolution(xBestInSegments, true);
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
    /**
     * This is a template for a destroy (insertion) heuristic.
     * It represents a generic greedy insertion where clusters with the best profit are inserted in the solution.
     * @param inputSolution the solution to destroy
     * @param q the number of clusters to insert
     * @return the destroyed solution
     */
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
    
    /**
     * Insertion heuristic.
     * Clusters with the best profit are inserted in the solution in a greedy way.
     * @param inputSolution the solution to destroy
     * @param q the number of clusters to insert
     * @return the destroyed solution
     */
    private List<Cluster> destroyGreedyProfitInsertion(List<Cluster> inputSolution, int q){
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
    
    /**
     * Insertion heuristic.
     * Clusters with the least cost are inserted in the solution in a greedy way.
     * @param inputSolution the solution to destroy
     * @param q the number of clusters to insert
     * @return the destroyed solution
     */
    private List<Cluster> destroyGreedyCostInsertion(List<Cluster> inputSolution, int q){
        // Initialize available clusters
        List<Cluster> availableClusters = this.getClustersNotInSolution(inputSolution);
        
        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);
        
        // Sort the available clusters
        availableClusters.sort(Cluster.COST_COMPARATOR);
        
        // Add q available clusters (or as many clusters as possible) to the solution
        for(int i = 0; i<q && i<availableClusters.size(); i++){
            output.add(availableClusters.get(i));
        }
        
        // return the destroyed input
        return output;
    }
    
    /**
     * Insertion heuristic.
     * Clusters with the highest profit to cost ratio are inserted in the solution in a greedy way.
     * @param inputSolution the solution to destroy
     * @param q the number of clusters to insert
     * @return the destroyed solution
     */
    private List<Cluster> destroyGreedyBestInsertion(List<Cluster> inputSolution, int q){
        // Initialize available clusters
        List<Cluster> availableClusters = this.getClustersNotInSolution(inputSolution);
        
        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);
        
        // Sort the available clusters
        availableClusters.sort(Cluster.PROFIT_COST_RATIO_COMPARATOR.reversed());
        
        // Add q available clusters (or as many clusters as possible) to the solution
        for(int i = 0; i<q && i<availableClusters.size(); i++){
            output.add(availableClusters.get(i));
        }
        
        // return the destroyed input
        return output;
    }
    
    /**
     * Insertion heuristic.
     * A maximum of q clusters are randomly picked and inserted into the solution.
     * @param inputSolution the solution to destroy
     * @param q the number of clusters to insert
     * @return the destroyed solution
     */
    private List<Cluster> destroyRandomInsertion(List<Cluster> inputSolution, int q){
        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);
        
        // Create an uniform random distribution of clusters and populate it
        ObjectDistribution<Cluster> clustersToInsert = new ObjectDistribution<>();
        clustersToInsert.addAll(getClustersNotInSolution(output));
        
        // Insert q clusters not in the solution, picking them randomly
        for(int i = 0; q>0 && i<q; i++){
            Cluster c = clustersToInsert.getRandom();
            output.add(c);
            clustersToInsert.remove(c);
        }
        
        // Return the repaired input
        return output;
    }
    
    // REPAIR HEURISTICS
    /**
     * This is a repair heuristic template.
     * This is a removal heuristic. It removes from the solution the q clusters with
     * the highest cost.
     * @param inputSolution the solution to repair
     * @param q number of clusters to remove
     * @return the repaired solution
     */
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
     * Removal heuristic.
     * It removes from the solution the q clusters with
     * the highest cost.
     * @param inputSolution the solution to repair
     * @param q number of clusters to remove
     * @return the repaired solution
     */
    private List<Cluster> repairHighCostRemoval(List<Cluster> inputSolution, int q){
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
     * Removal heuristic.
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
     * Removal heuristic.
     * Removes the first cluster with the least profit/cost ratio, then removes
     * the other q-1 clusters which are the most similar to the first one.
     * The similarity criterion is computed as follows:
     * <ul>
     * <li>Find v as the vehicle with the longest streak in the first cluster</li>
     * <li>For every node n in the first cluster get the duration of services servable by v. Sum all the values.</li>
     * <li>Divide the previous value by the total duration of services in the clusters</li>
     * <li>Take the absolute value of the difference between the ratio for the
     * first cluster removed and the ratio for every other cluster as the similarity criterion.</li>
     * </ul>
     * @param inputSolution the solution to repair
     * @param q number of clusters to remove
     * @return the repaired solution
     */
    private List<Cluster> repairVehicleTime(List<Cluster> inputSolution, int q){
        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);
        
        // Sort the clusters in the input solution
        output.sort(Cluster.PROFIT_COST_RATIO_COMPARATOR);
        
        if(q>0){
            // Remove 1 cluster from the solution, following the imposed ordering
            Cluster first = output.remove(0);
            
            if(q>1){
                double firstRatio = 0.0;
                Vehicle firstVehicle = null;
                int biggestStreakSize = 0;
                
                // Stores clusters and their ratios
                LinkedHashMap<Cluster,Double> clustersRatios = new LinkedHashMap<>();
                
                // Find v as the vehicle with the longest streak in the first cluster
                for(Vehicle v:instance.getVehicles()){
                    if(v.canServe(first)){
                        // Get the biggest streak of v in first
                        List<Streak> streaks = first.getStreaks(v);
                        streaks.sort(Streak.SIZE_COMPARATOR.reversed());
                        int streakSize = streaks.get(0).size();
                        
                        // If the streak we've found is bigger than the previous one
                        // update biggestStreakSize and firstVehicle
                        if(streakSize > biggestStreakSize){
                            biggestStreakSize = streakSize;
                            firstVehicle = v;
                        }
                    }
                }
                
                if(firstVehicle != null){
                    // Calculate the ratio for the first cluster
                    firstRatio = first.getTotalCostForVehicle(firstVehicle)/first.getTotalCost();
                    
                    // Populate the map that holds cluster IDs (in the output) and their ratios
                    for(int i =0; i<output.size(); i++){
                        Cluster c = output.get(i);
                        clustersRatios.put(
                                c,
                                Math.abs(firstRatio - c.getTotalCostForVehicle(firstVehicle)/c.getTotalCost())
                        );
                    }
                    
                    // Sort the map by value to get the order for removal (DEBUG: check if it works!)
                    List<Cluster> removeOrder = new ArrayList();
                    clustersRatios.entrySet()
                            .stream()
                            .sorted(Map.Entry.comparingByValue())
                            .forEachOrdered(entry -> removeOrder.add(entry.getKey()));
                    
                    // Now the map is sorted by value (which is the ratio)
                    // in a growing order
                    // Remove the first q-1 elements of the map from the output
                    int removed = 0;
                    for(Cluster c : removeOrder){
                        if(removed<q-1){
                            output.remove(c);
                            removed++;
                        }
                        else break;
                    }
                    
                }
                // debug
                else try {
                    env.message("Error in repairVehicleTime heuristic!\n"
                            + "No serving vehicle found for first cluster "+first.getId());
                } catch (GRBException ex) {
                    Logger.getLogger(ALNS.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        // Return the repaired input
        return output;
    }
    
    /**
     * Removal heuristic.
     * Removes the first cluster with the least profit/cost ratio, then removes
     * the other q-1 clusters which are the most similar to the first one.
     * The similarity criterion is computed as follows:
     * <ul>
     * <li> 1/D * (distanceBetween(i,j) + distanceBetween(firstNode,j) + distanceBetween(j,lastNode))
     * <li> D = 3
     * <li> i = first cluster removed from solution
     * <li> j = cluster to evaluate for removal
     * </ul>
     * @param inputSolution the solution to repair
     * @param q number of clusters to remove
     * @return the repaired solution
     */
    private List<Cluster> repairTravelTime(List<Cluster> inputSolution, int q){
        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);
        
        // Constant at the denominator
        double D = 3.0;
        
        // Sort the clusters in the input solution
        output.sort(Cluster.PROFIT_COST_RATIO_COMPARATOR);
        
        if(q>0){
            // Remove 1 cluster from the solution, following the imposed ordering
            Cluster first = output.remove(0);
            
            if(q>1){
                double firstRatio = 0.0;
                Vehicle firstVehicle = null;
                
                // Stores clusters and their ratios
                LinkedHashMap<Cluster,Double> clustersRatios = new LinkedHashMap<>();
                
                // Initialize the firstNode and lastNode of the instance we're working on
                Node firstNode = instance.getNode(0);
                Node lastNode = instance.getNode(instance.getNum_nodes()-1);

                // Populate the map that holds cluster IDs (in the output) and their ratios
                for(int i =0; i<output.size(); i++){
                    Cluster c = output.get(i);
                    clustersRatios.put(
                            c,
                            (first.distance(c)+c.distance(firstNode)+c.distance(lastNode))/D
                    );
                }

                // Sort the map by value to get the order for removal (DEBUG: check if it works!)
                List<Cluster> removeOrder = new ArrayList();
                clustersRatios.entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByValue())
                        .forEachOrdered(entry -> removeOrder.add(entry.getKey()));

                // Now the map is sorted by value (which is the ratio)
                // in a growing order
                // Remove the first q-1 elements of the map from the output
                int removed = 0;
                for(Cluster c : removeOrder){
                    if(removed<q-1){
                        output.remove(c);
                        removed++;
                    }
                    else break;
                }
            }
        }
        // Return the repaired input
        return output;
    }
    
    
    /**
     * Removal heuristic.
     * Removes at most q randomly picked clusters from the current solution.
     * @param inputSolution the solution to repair
     * @param q number of clusters to remove
     * @return the repaired solution
     */
    private List<Cluster> repairRandomRemoval(List<Cluster> inputSolution, int q){
        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);
        
        // Create an uniform random distribution of clusters and populate it
        ObjectDistribution<Cluster> clustersToRemove = new ObjectDistribution<>();
        clustersToRemove.addAll(output);
        
        // Remove q clusters from the solution, picking them randomly
        for(int i = 0; q>0 && i<q; i++){
            Cluster c = clustersToRemove.getRandom();
            output.remove(c);
            clustersToRemove.remove(c);
        }
        
        // Return the repaired input
        return output;
    }
    
    /**
     * This is a special repair heuristic to bring back an eventual infeasible solution into feasibility.
     * It operates by removing the minimum number of low gain clusters when their
     * total cost is more than or equal the difference between the maximum cost for the solution (Tmax)
     * and the actual cost of the solution.
     * <br>Fesibility is tested at the end of the method, so you can expect to find
     * interesting information about this solution in the current model.
     * @param inputSolution an infeasible solution
     * @return a feasible solution
     */
    private List<Cluster> repairBackToFeasibility(List<Cluster> inputSolution) throws Exception{
        // TODO:
        // 0. Check feasibility. If infeasible goto 1, else goto 9
        // 1. Compute the feasibility relaxation model so that we can retrieve some information on z and Tmax
        // 2. Look at how much into infeasibility we are, so get the maxZ
        // 3. Compute costDifference = maxZ-Tmax
        // 4. Sort inputSolution clusters by increasing profit
        // 5. Sort inputSolution clusters by increasing service time (cost)
        // 6. Get the first cluster from sorted inputSolution with cost >= costDifference
        // 7. Remove the cluster found in 6.
        // 8. Goto 0
        // 9. Return the new feasible solution
        
        int firstNodeID = 0;
        
        // Create a new objective function for the feasibility relaxation model
        // The objective function is setup to "minimize the value of sum(z[*][lastNodeID])"
        // (we want to minimize the time of arrival into the last node)
        int lastNodeID = instance.getNum_nodes()-1;
        GRBLinExpr newObj = new GRBLinExpr();
        for(int s = 0; s < instance.getNum_nodes(); s++){
            newObj.addTerm(1.0, this.z[s][lastNodeID]);
        }
        
        // Setup the output
        List<Cluster> output = new ArrayList<>(inputSolution);
        
        // 0. Check feasibility and start cycling until we have a feasible solution
        boolean isFeasible = testSolution(output, true);
        while(!isFeasible){
            // 1. Compute the feasibilityRelaxation so that we can retrieve some information on z and Tmax
            GRBModel clone = new GRBModel(model);
            
            // Setup the new value of Tmax as the maximum value for a double
            double safeTMax = Double.MAX_VALUE;
            double[] newTMax = new double[this.constraint8.size()];
            for(int t = 0; t<this.constraint8.size(); t++){
                newTMax[t] = safeTMax;
            }
            
            // Set the new value of Tmax as the new upper bound for each z
            for(int i = 0; i < instance.getNum_nodes(); i++){
                for(int j = 0; j < instance.getNum_nodes(); j++){
                    GRBVar toFix = clone.getVarByName("z_("+i+","+j+")");
                    toFix.set(GRB.DoubleAttr.UB, safeTMax);
                }
            }
            
            // TODO: try removing all constraints8 from the cloned model, then
            // rebuild them with the new tmax instead
            
            // Remove all old constraints
            for(GRBConstr grbc : this.constraint8){
                clone.remove(grbc);
            }
            // Add new constraints
            for(int i=firstNodeID; i<instance.getNum_nodes(); i++){
                for(int j=firstNodeID; j<instance.getNum_nodes(); j++){
                    GRBLinExpr expr8 = new GRBLinExpr();
                    for(int v=0; v<instance.getNum_vehicles(); v++){
                        expr8.addTerm(safeTMax, x[v][i][j]);
                    }
                    
                    // Add the constraint to the cloned model
                    // This is one constraint for every z[i][j]
                    clone.addConstr(z[i][j], GRB.LESS_EQUAL, expr8, "c8_arc("+i+","+j+")");
                }
            }
            
            /*
            // For each constraint in the list of constraints, change Tmax to
            // the maximum possible value
            for(int i = 0; i < this.constraint8.size(); i++){
                List<GRBVar> vars = this.constraint8Variables.get(i);
                //DEBUG: check if sizes of all arguments are the same!
                GRBConstr [] constraints = constraint8.toArray(new GRBConstr[constraint8.size()]);
                GRBVar [] variables = vars.toArray(new GRBVar[vars.size()]);
                
                clone.chgCoeffs(constraints, variables, newTMax);
            }
            */
            
            // Now we set the objective function to "minimize the time of arrival into the last node"
            clone.setObjective(newObj, GRB.MINIMIZE);
            
            // Update the cloned model
            clone.update();
            
            clone.write("feasibilitySubmodel.lp");
            
            clone.optimize();
            // Let's test the solution on the feasibility relaxation model (clone)
            //if(true){
            if(this.testSolution(clone, output, true)){
                // If it worked, we're very happy because we can start looking for the maximum value of Z
                double maxZ = -1;
                double tempZ;
                for(int i=0;i<instance.getNum_nodes();i++){
                    tempZ = z[i][lastNodeID].get(GRB.DoubleAttr.X);
                    if(tempZ>=maxZ){
                        maxZ=tempZ;
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
            // In case the feasibility relaxation did not work, something went VERY wrong
            else throw new Exception("PROBLEM: the feasibility relaxation was infeasible!");
            
            // Memory cleanup
            clone.dispose();
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
        
        repairMethods.updateWeightSafely(repairMethod, repairOldWeight*lambda + (1-lambda)*prize);
        destroyMethods.updateWeightSafely(destroyMethod, destroyOldWeight*lambda + (1-lambda)*prize);
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