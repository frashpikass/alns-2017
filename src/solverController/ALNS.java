/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverController;

import gurobi.GRB;
import gurobi.GRBCallback;
import gurobi.GRBConstr;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import solverModel.Cluster;
import solverModel.Node;
import solverModel.Streak;
import solverModel.Vehicle;

/**
 * A class to solve an orienteering problem using the ALNS heuristic
 *
 * @author Frash
 */
public class ALNS extends Orienteering {

    /**
     * A Javabean that holds all ALNS parameters.
     */
    private ALNSPropertiesBean alnsProperties;

    /**
     * History of past inserted clusters (long term memory)
     */
    private Deque<Cluster> pastHistory;

    /**
     * This is an ObjectDistribution for destroy methods. It contains references
     * to all the destroy heuristics for this ALNS implementation and allows
     * ALNS objects to randomly extract such methods according to the weight as
     * defined by the ALNS procedure.
     */
    private ObjectDistribution<BiFunction<List<Cluster>, Integer, List<Cluster>>> destroyMethods;

    /**
     * This is an ObjectDistribution for repair methods. It contains references
     * to all the repair heuristics for this ALNS implementation and allows ALNS
     * objects to randomly extract such methods according to the weight as
     * defined by the ALNS procedure.
     */
    private ObjectDistribution<BiFunction<List<Cluster>, Integer, List<Cluster>>> repairMethods;

    /**
     * A list of clusters associated to their probability of being used by the
     * destroy methods to increase ALNS solution size
     */
    private ClusterRoulette clusterRoulette;

    /**
     * The controller to notfy of eventual changes
     */
    private Controller controller;

    /**
     * Time elapsed since the beginning of the optimization process
     */
    private long elapsedTime = 0;

    public ALNS(Orienteering o, ALNSPropertiesBean ALNSParams, Controller c) throws Exception {
        // Setting up all parameters
        super(o);
        this.alnsProperties = ALNSParams;
        this.controller = c;

        // Setting up the Cluster Roulette
        clusterRoulette = new ClusterRoulette(instance.cloneClusters());

        // Keeping track of all implemented repair and destroy methods
        destroyMethods = new ObjectDistribution<>();

        destroyMethods.add(this::destroyGreedyCostInsertion, ALNSParams.isUseDestroyGreedyCostInsertion(), "GreedyCostInsertion");
        destroyMethods.add(this::destroyGreedyBestInsertion, ALNSParams.isUseDestroyGreedyBestInsertion(), "GreedyBestInsertion");
        destroyMethods.add(this::destroyGreedyProfitInsertion, ALNSParams.isUseDestroyGreedyProfitInsertion(), "GreedyProfitInsertion");
        destroyMethods.add(this::destroyRandomInsertion, ALNSParams.isUseDestroyRandomInsertion(), "RandomInsertion");

        repairMethods = new ObjectDistribution<>();

        repairMethods.add(this::repairHighCostRemoval, ALNSParams.isUseRepairHighCostRemoval(), "HighCostRemoval");
        repairMethods.add(this::repairRandomRemoval, ALNSParams.isUseRepairRandomRemoval(), "RandomRemoval");
        repairMethods.add(this::repairTravelTime, ALNSParams.isUseRepairTravelTime(), "TravelTime");
        repairMethods.add(this::repairVehicleTime, ALNSParams.isUseRepairVehicleTime(), "VehicleTime");
        repairMethods.add(this::repairWorstRemoval, ALNSParams.isUseRepairWorstRemoval(), "WorstRemoval");

        // Redirect stdout and stderr to the controller
        if (controller != null && controller.getStdoutStream() != null) {
            System.setOut(new PrintStream(controller.getStdoutStream()));
            System.setErr(new PrintStream(controller.getStdoutStream()));
        }
    }

    /**
     * ALNS first step: build a feasible solution to bootstrap the ALNS
     * procedure. Tentative solutions are built from combinations of promising
     * clusters until an infeasible solution is found. The last feasible
     * solution after an infeasible solution was found is the starting solution.
     *
     * @return the feasible solution found, represented as a list of selected
     * clusters.
     * @throws GRBException if anything goes wrong with logging or callback
     * mechanisms
     */
    public List<Cluster> ALNSConstructiveSolution() throws GRBException, Exception {
        env.message("\nALNSConstructiveSolution log start, time " + LocalDateTime.now() + "\n");

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
        for (int i = 0; i < clusters.size() && isFeasible && !this.isCancelled(); i++) {
            Cluster c = clusters.get(i);
            // Let's extract the first cluster from the ordered list of clusters
            // and let's put it into the new solution
            newSolution.add(c);

            // Let's use gurobi to check the feasibility of the new solution
            isFeasible = this.testSolutionForFeasibility(newSolution, true, alnsProperties.getMaxMIPSNodesForFeasibilityCheck());
            // If the new solution is feasible, update the old solution
            if (isFeasible) {
                solution = new ArrayList<>(newSolution);
            }
        }

        // Now solution holds the list of clusters found by our constructive algorithm.
        // Let's update the model so that it cointains the current solution
        testSolution(this.model, solution, true, alnsProperties.getMaxMIPSNodesForFeasibilityCheck());
        env.message("\nALNSConstructiveSolution log end, time " + LocalDateTime.now() + "\n");
        if (this.isCancelled()) {
            throw new InterruptedException("optimizeALNS() interrupted in the constructive solution building phase.");
        }
        return solution;
    }
    
    /**
     * Automatically picks a solver from the controller selection, and optimizes
     * the current model using it.
     *
     * @throws gurobi.GRBException if there are problems with Gurobi
     * @throws Exception if there are generic problems
     */
    public void optimize() throws GRBException, Exception {
        try {
            if (controller.getSolver() != null) {
                switch (controller.getSolver()) {
                    case SOLVE_RELAXED:
                        optimizeRelaxed();
                        break;

                    case SOLVE_MIPS:
                        optimizeMIPS();
                        break;

                    case SOLVE_ALNS:
                        optimizeALNS();
                        break;
                    default:
                        env.message("ALNS optimize() error: null solver\n");
                        break;
                }
            }
        } catch (InterruptedException e) {
            throw new InterruptedException(e.getMessage());
        }
    }

    /**
     * Run the ALNS optimization on the current Orienteering problem.
     * <br>At the beginning a feasible solution is created by a constructive
     * method.
     * <br>Then, until a stopping criterion is met, a cycle will test different
     * solutions generated through chosen destroy and repair heuristics.
     * <br>Destroy heuristics will move the solution towards an infeasible state
     * by inserting a number <tt>q</tt> of clusters into the solution.
     * <br>Repair heuristics will move the solution back towards feasibility by
     * removing a number <tt>q</tt> of clusters from the solution.
     * <br>The repair heuristic is called upon the result of the destroy
     * heuristic as applied to the last solution found.
     * <br>The chosen solution, regardless of it being feasible, infeasible,
     * pejorative or ameliorative, will be accepted using a simulated annhealing
     * criterion.
     * <br>At the end, if a stopping criterion is met (one of maximum number of
     * iterations met, maximum number of iterations from the last improvement,
     * maximum computation time) the cycle ends and the last best feasible
     * solution is returned.
     *
     * @throws java.io.IOException if there are problems with the logger
     * @throws gurobi.GRBException if there are problems with Gurobi
     * @throws java.lang.InterruptedException if the thread was interrupted
     * while solving
     * @throws Exception if anything else goes wrong
     */
    public void optimizeALNS() throws IOException, GRBException, InterruptedException, Exception {
        try {
            // Send the controller a message saying we're starting
            notifyController(elapsedTime, OptimizationStatusMessage.Status.STARTING);
            env.message("\nALNSLOG, " + elapsedTime + ": optimizeALNS starting.\n");

            // Setup the Excel logger
            ALNSExcelLogger xlsxLogger = new ALNSExcelLogger(
                    orienteeringProperties.getOutputFolderPath() + File.separator + instance.getName() + "_ALNS.xlsx",
                    instance.getName());

            // This is a generic ALNS implementation
            GRBModel relaxedModel = model.relax();
            relaxedModel.optimize();

            // Temperature mitigates the effects of the simulated annealing process
            // A lower temperature will make it less likely for the proccess to accept
            // a pejorative solution
            double initialTtemperature = 2 * relaxedModel.get(GRB.DoubleAttr.ObjBound);
            double temperature;
            // The probability barrier to pass to accept a pejorative solution
            double simulatedAnnealingBarrier = -1;

            // Memory cleanup
            relaxedModel.dispose();

            // STEP 1: generate a CONSTRUCTIVE SOLUTION
            // xOld stores the previous solution, the starting point of any iteration
            List<Cluster> xOld = this.ALNSConstructiveSolution();
            // stores the new solution, produced by the destroy and repair heuristics
            List<Cluster> xNew = xOld;
            // stores the best solution found throughout iterations
            List<Cluster> xBest = xOld;
            // stores the best solution found throughout segments
            List<Cluster> xGlobalBest = xOld;

            // old value of the objective function, generated through the old solution
            double oldObjectiveValue = model.get(GRB.DoubleAttr.ObjVal);
            // new value of the objective function produced by the current iteration
            double newObjectiveValue = oldObjectiveValue;
            // best value of the objective function throughout iterations in a segment
            double bestObjectiveValueInSegment = oldObjectiveValue;
            // best global value of the objective function throughout segments
            double bestGlobalObjectiveValue = oldObjectiveValue;

            // qMax is the maximum number of clusters to operate on with heuristic
            int qMax = instance.getNum_clusters();
            int qMin = 1;

            // q is the number of clusters to repair and destroy at each iteration
            int q = alnsProperties.getqStart();

            // This is how much q should increase at every iteration
            // int qDelta = Math.floorDiv(qMax, 10); // to parametrize
            // These variables are references to the chosen destroy and repair methods
            // for the current iteration
            BiFunction<List<Cluster>, Integer, List<Cluster>> destroyMethod;
            BiFunction<List<Cluster>, Integer, List<Cluster>> repairMethod;

            // Parameters to determine how to update heuristic weights
            boolean solutionIsAccepted;
            boolean solutionIsNewGlobalOptimum;  // true if c(xNew)>c(xBest)
            boolean solutionIsBetterThanOld;     // true if c(xNew)>c(xOld)
            boolean solutionIsWorseButAccepted;  // true id c(xNew)<=c(xOld) but xNew is accepted
            boolean solutionIsWorseAndRejected;  // true id c(xNew)<=c(xOld) and xNew is rejected
            boolean repairMethodWasUsed;         // Keeping track whether a repair method had to be used or not

            // Setup of stopping criterions and time management
            long startTimeInNanos = System.nanoTime();

            // Keeping track of how many segments of iterations have been performed
            long segments = 0;
            //long maxSegments = 1000;

            // Keeping track of how many segments without improvement have been seen
            long segmentsWithoutImprovement = 0;
            //long maxSegmentsWithoutImprovement = 20; // to parametrize / REMOVE

            // A string to log why the segment ended.
            StringBuffer segmentEndCause = new StringBuffer();

            // ALNS START: Cycles every segment
            do {
                // Send the controller a message saying we're running
                notifyController(elapsedTime, OptimizationStatusMessage.Status.RUNNING);
                env.message("\nALNSLOG, " + elapsedTime + ": segment " + segments + " started. q = " + q + "\n");
                
                // Reset temperature
                temperature = initialTtemperature;
                
                // Reset the nerf list
                clusterRoulette.resetNerfOccurrences();

                // Reset heuristic weights
                // If we aren't in the first iteration, give a prize and a punishment
                // to the best and the worst heuristics
                if (segments > 0) {
                    // Save the best and the worst heuristics
                    List<BiFunction<List<Cluster>, Integer, List<Cluster>>> bestDestroys = destroyMethods.getAllMostProbable();
                    List<BiFunction<List<Cluster>, Integer, List<Cluster>>> worstDestroys = destroyMethods.getAllLeastProbable();
                    List<BiFunction<List<Cluster>, Integer, List<Cluster>>> bestRepairs = repairMethods.getAllMostProbable();
                    List<BiFunction<List<Cluster>, Integer, List<Cluster>>> worstRepairs = repairMethods.getAllLeastProbable();

                    // Reset all heuristic weights
                    resetHeuristicMethodsWeight();

                    // Reward and punish
                    destroyMethods.scaleAllWeightsOf(bestDestroys, alnsProperties.getRewardForBestSegmentHeuristics());
                    destroyMethods.scaleAllWeightsOf(worstDestroys, alnsProperties.getPunishmentForWorstSegmentHeuristics());
                    repairMethods.scaleAllWeightsOf(bestRepairs, alnsProperties.getRewardForBestSegmentHeuristics());
                    repairMethods.scaleAllWeightsOf(worstRepairs, alnsProperties.getPunishmentForWorstSegmentHeuristics());
                }

                // Iterations inside a segment (SEGMENT START)
                int iterations;
                // Keeping track of the number of iterations without improvement in the segment
                int iterationsWithoutImprovement = 0;
                for (iterations = 0;
                        iterations < alnsProperties.getSegmentSize()
                        && xOld.size() >= 1
                        && elapsedTime <= alnsProperties.getTimeLimitALNS()
                        && iterationsWithoutImprovement < alnsProperties.getMaxIterationsWithoutImprovement()
                        && !this.isCancelled(); // thread check
                        iterations++) {

                    env.message("\nALNSLOG, " + elapsedTime + ": segment " + segments + ", iteration " + iterations + ", without improvement " + iterationsWithoutImprovement + "\n");

                    // Setup of boolean values to evaluate solution quality
                    solutionIsAccepted = false;
                    solutionIsNewGlobalOptimum = false;
                    solutionIsBetterThanOld = false;
                    solutionIsWorseButAccepted = false;
                    solutionIsWorseAndRejected = false;
                    repairMethodWasUsed = false;

                    // Picking a destroy and a repair method
                    destroyMethod = pickDestroyMethod();
                    repairMethod = pickRepairMethod();

                    // Apply the destruction method on the solution
                    env.message("\nALNSLOG, " + elapsedTime + ": segment " + segments + ", iteration " + iterations + ", destroy: " + destroyMethods.getLabel(destroyMethod) + "\n");
                    xNew = destroyMethod.apply(xOld, q);

                    // CLUSTER COOLDOWN: Get the newly inserted clusters (hot clusters)
                    List<Cluster> xHotClusters = new ArrayList<>(xNew);
                    xHotClusters.removeAll(xOld);
                    // CLUSTER COOLDOWN: Cool down hot clusters, warm up the others
                    clusterRoulette.cooldown(alnsProperties.getCooldownGamma(), xHotClusters);
                    clusterRoulette.warmup(alnsProperties.getWarmupGamma(), xHotClusters);

                    //If the new solution is infeasible, apply the repair method
                    if (!testSolutionForFeasibility(xNew, false, alnsProperties.getMaxMIPSNodesForFeasibilityCheck())) {
                        env.message("\nALNSLOG, " + elapsedTime + ": segment " + segments + ", iteration " + iterations + ", repair: " + repairMethods.getLabel(repairMethod) + "\n");
                        xNew = repairBackToFeasibility4(xNew, repairMethod, false);
                        repairMethodWasUsed = true;
                    } else {
                        env.message("\nALNSLOG, " + elapsedTime + ": segment " + segments + ", iteration " + iterations + ", no repair method needed.\n");
                    }

                    // Check if we entered feasibility. If we didn't,
                    // gather the value of the objective function for the new solution
                    // obtained through the chosen methods
                    if (testSolutionForFeasibility(xNew, false, alnsProperties.getMaxMIPSNodesForFeasibilityCheck())) {
                        newObjectiveValue = objectiveValueFromLastFeasibilityCheck;
                    } else { // INFEASIBLE SOLUTION HANDLING
                        // If we're here, it means that the repaired solution was still infeasible,
                        // which is pretty bad and shouldn't happen (too often)

                        // CHOICE 1:
                        // 1 - heavily penalize the infeasible cluster, update nerf list
                        // 2 - Update the elapsed time
                        // 3 - Log the results to XLSX
                        // 4 - discard the solution (set xNew = xOld), penalize the two heuristics
                        
                        // 1 - CLUSTER COOLDOWN PUNISHMENT: heavily penalize the infeasible cluster, update nerf list
                        clusterRoulette.downscale(alnsProperties.getPunishmentGamma(), xNew);
                        clusterRoulette.updateNerfOccurrences();

                        // 2 - Update the elapsed time
                        elapsedTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTimeInNanos);

                        // 3 - Log the results to XLSX
                        String[] logLine = {
                            segments + "", iterations + "", elapsedTime + "",
                            destroyMethods.getLabel(destroyMethod), destroyMethods.getWeightOf(destroyMethod) + "",
                            repairMethods.getLabel(repairMethod), repairMethods.getWeightOf(repairMethod) + "",
                            repairMethodWasUsed ? "1" : "0",
                            temperature + "",
                            simulatedAnnealingBarrier + "",
                            q + "",
                            csvFormatSolution(xOld), oldObjectiveValue + "",
                            csvFormatSolution(xNew), "infeasible", solutionIsAccepted ? "1" : "0", solutionIsWorseButAccepted ? "1" : "0", "1",
                            csvFormatSolution(xBest), bestObjectiveValueInSegment + "",
                            csvFormatSolution(xGlobalBest), bestGlobalObjectiveValue + "",
                            clusterRoulette.toString(),String.valueOf(clusterRoulette.getAverageProbability()),clusterRoulette.nerfOccurrencesString(),
                            "Infeasible and discarded."
                        };
                        xlsxLogger.writeRow(logLine);
                        // Send the controller a message saying we're still running
                        notifyController(elapsedTime, OptimizationStatusMessage.Status.RUNNING);
                        env.message("\nALNSLOG, " + elapsedTime + ": segment " + segments + ", iteration " + iterations + ", repaired solution infeasible and discarded.\n");

                        // 4 - discard the solution (set xNew = xOld), penalize the two heuristics
                        // Update heuristic weights with the worst possible score
                        updateHeuristicMethodsWeight(
                                destroyMethod,
                                repairMethod,
                                false,
                                false,
                                false,
                                true, // the new solution is worse than the old one and is rejected
                                repairMethodWasUsed
                        );

                        // Update temperature, just like at the end of every iteration
                        temperature *= alnsProperties.getAlpha();

                        // Update the count of iterations without improvement
                        iterationsWithoutImprovement++;

                        // proceed with the next iteration
                        continue;

                        //                    // CHOICE 2: get the best bound on the objective and continue
                        //                    newObjectiveValue = model.get(GRB.DoubleAttr.ObjBound);
                        //                    // CHOICE 3: bring the solution back to feasibility and go on
                        //                    xNew = repairBackToFeasibility2(xNew);
                        //                    newObjectiveValue = model.get(GRB.DoubleAttr.ObjVal);
                    } // end of infeasible solution handling

                    // In any case, now the model has been updated and it stores data
                    // about the current solution
                    // Solution evaluation: if the solution improves the obj function, save it
                    // and give a prize to the heuristics according to their performance
                    solutionIsBetterThanOld = (newObjectiveValue > oldObjectiveValue);

                    // In case the solution is accepted, keep track of whether
                    // it's improving the objective
                    simulatedAnnealingBarrier = simulatedAnnealingMaximization(oldObjectiveValue, newObjectiveValue, temperature);
                    solutionIsAccepted = acceptSolution(simulatedAnnealingBarrier);
                    
                    // Proceed with evaluation
                    if (solutionIsAccepted) {
                        if (!solutionIsBetterThanOld) {
                            solutionIsWorseButAccepted = true;
                        }
                    } else {
                        solutionIsWorseAndRejected = true;
                    }

                    // Check if the solution is a new global optimum
                    solutionIsNewGlobalOptimum = newObjectiveValue > bestGlobalObjectiveValue;

                    // Anyway, if the solution is better or equal than the current best for the segment
                    // save it as xBest, and its objective value in bestObjectiveValue
                    if (newObjectiveValue >= bestObjectiveValueInSegment) {
                        // we accept even different solutions with the same objective
                        // to add some variety to solutions
                        xBest = xNew;
                        bestObjectiveValueInSegment = newObjectiveValue;

                        // If there was a real improvement reset the no-improvement counter
                        if (solutionIsBetterThanOld) {
                            iterationsWithoutImprovement = 0;
                            
                            // Warmup the good clusters of the good solution which caused an improvement
                            // so that the cooldown has no effect on them (this will make us save the better clusters)
                            clusterRoulette.upscale((1.0-alnsProperties.getCooldownGamma()), xNew);
                        }
                        
                    } else {
                        // if the solution was without improvement
                        iterationsWithoutImprovement++;
                    }

                    // Update temperature at the end of every iteration
                    temperature *= alnsProperties.getAlpha();

                    // Log the results to CSV
                    // Update the elapsed time
                    elapsedTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTimeInNanos);

                    // Log at the end of the iteration
                    String[] logLine = {
                        segments + "", iterations + "", elapsedTime + "",
                        destroyMethods.getLabel(destroyMethod), destroyMethods.getWeightOf(destroyMethod) + "",
                        repairMethods.getLabel(repairMethod), repairMethods.getWeightOf(repairMethod) + "",
                        repairMethodWasUsed ? "1" : "0",
                        temperature + "",
                        simulatedAnnealingBarrier + "",
                        q + "",
                        csvFormatSolution(xOld), oldObjectiveValue + "",
                        csvFormatSolution(xNew), newObjectiveValue + "", solutionIsAccepted ? "1" : "0", solutionIsWorseButAccepted ? "1" : "0", "0",
                        csvFormatSolution(xBest), bestObjectiveValueInSegment + "",
                        csvFormatSolution(xGlobalBest), bestGlobalObjectiveValue + "",
                        clusterRoulette.toString(),String.valueOf(clusterRoulette.getAverageProbability()),clusterRoulette.nerfOccurrencesString(),
                        ""
                    };
                    xlsxLogger.writeRow(logLine);
                    // Send the controller a message to notify we're still running
                    notifyController(elapsedTime, OptimizationStatusMessage.Status.RUNNING);
                    env.message("\nALNSLOG, " + elapsedTime + ": segment " + segments + ", iteration " + iterations + " end.\n");

                    // Update the heuristic weights
                    updateHeuristicMethodsWeight(
                            destroyMethod, repairMethod,
                            solutionIsNewGlobalOptimum,
                            solutionIsBetterThanOld,
                            solutionIsWorseButAccepted,
                            solutionIsWorseAndRejected,
                            repairMethodWasUsed);
                    
                    // Update the nerfing weights to keep track of ill behaving clusters
                    clusterRoulette.updateNerfOccurrences();

                    // If the new solution was accepted, use it as a starting point for the next iteration
                    if (solutionIsAccepted) {
                        xOld = xNew;
                        oldObjectiveValue = newObjectiveValue;
                        env.message("\nALNSLOG, " + elapsedTime + ": segment " + segments + ", iteration " + iterations + " solution accepted.\n");
                    }

                    // Update the elapsed time
                    elapsedTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTimeInNanos);
                } // for: end of all iterations in the segment

                //            // At the end of the segment, the best solution is in xBest, with a value in bestObjectiveValue
                //            // This would be a nice spot to use some local search algorithm:
                //            // repairBackToFeasibility will check whether the solution found in
                //            // this segment is infeasible or not; if it is infeasible it will bring
                //            // it back to feasibility through a special repair heuristic
                //            List<Cluster> xBestRepaired = repairBackToFeasibility(xBest);
                //            
                //            // If xBest and xBestRepaired are not the same solution, make sure the feasible one is the best
                //            if(!(xBest.containsAll(xBestRepaired) && xBestRepaired.containsAll(xBest))){
                //                // TODO: I'm not sure about this, I need to check again tomorrow; update bestObjectiveValue accordingly
                //                xBest = xBestRepaired;
                //                bestObjectiveValue = model.get(GRB.DoubleAttr.ObjVal);
                //            }
                //            
                // We check whether there was an improvement from last segment to this one
                if (bestObjectiveValueInSegment > bestGlobalObjectiveValue) {
                    env.message("\nALNSLOG, " + elapsedTime + ": segment " + segments + " ended with an improvement!\n");
                    // There was an improvement! Save the best solution found in the iterations
                    bestGlobalObjectiveValue = bestObjectiveValueInSegment;
                    xGlobalBest = xBest;
                    // Reset the no-improvement counter
                    segmentsWithoutImprovement = 0;
                } else {
                    // There was no improvement: update the no-improvement counter
                    segmentsWithoutImprovement++;
                    env.message("\nALNSLOG, " + elapsedTime + ": segment " + segments + " ended without an improvement.\n");
                }

                // Check and log why the segment has ended
                if (iterations >= alnsProperties.getSegmentSize()) {
                    segmentEndCause.append(" Max segment size reached (" + iterations + ")!");
                }
                if (xOld.size() < 1) {
                    segmentEndCause.append(" xOld is too small to work with (size=" + xOld.size() + ")!");
                }
                if (elapsedTime > alnsProperties.getTimeLimitALNS()) {
                    segmentEndCause.append(" ALNS time limit reached!");
                }
                if (iterationsWithoutImprovement >= alnsProperties.getMaxIterationsWithoutImprovement()) {
                    segmentEndCause.append(" Too many iterations without improvement (" + iterationsWithoutImprovement + ")!");
                }

                // Update the elapsed time
                elapsedTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTimeInNanos);

                // Log at the end of the segment
                String[] logLineSegmentEnd = {
                    segments + "", "*" + "", elapsedTime + "",
                    "*", "*", "*", "*", "*",
                    temperature + "",
                    simulatedAnnealingBarrier + "",
                    q + "",
                    "*", "*",
                    "*", "*", "*", "*", "*",
                    csvFormatSolution(xBest), bestObjectiveValueInSegment + "",
                    csvFormatSolution(xGlobalBest), bestGlobalObjectiveValue + "",
                    clusterRoulette.toString(),String.valueOf(clusterRoulette.getAverageProbability()),clusterRoulette.nerfOccurrencesString(),
                    "End of the segment. Reason: " + segmentEndCause.toString()
                };
                xlsxLogger.writeRow(logLineSegmentEnd);
                // Send the controller a message to notify we're still running
                notifyController(elapsedTime, OptimizationStatusMessage.Status.RUNNING);
                env.message("\nALNSLOG, " + elapsedTime + ": segment " + segments + " end cause: " + segmentEndCause + "\n");

                // Reset the StringBuffer that logs the reason why a segment has ended
                segmentEndCause = new StringBuffer();
                
                // Nerf all clusters which have been underperforming for at
                // least nerfBarrier% of the segment, promote the others.
                clusterRoulette.punishNerfCandidatesAndResetOthers(alnsProperties.getNerfBarrier());

                // Let's try a local search run, if we have time for it
                env.message("\nALNSLOG, " + elapsedTime + ": segment " + segments + " local search...\n");
                List<Cluster> xLocalSearch = this.localSearch(
                        xGlobalBest,
                        elapsedTime,
                        alnsProperties.getTimeLimitLocalSearch(),
                        segmentsWithoutImprovement
                );
                double localSearchObjectiveValue = model.get(GRB.DoubleAttr.ObjVal);
// DEBUG: this throws grbexception because the local search said the model was infeasible (which it wasn't)
// DEBUG: xLocalSearch was 9 2 12 4 for instance 1.4.q

                // If the local search was successful, save the new solution as the best in segments
                if (localSearchObjectiveValue >= bestGlobalObjectiveValue) {
                    xGlobalBest = xLocalSearch;
                    bestGlobalObjectiveValue = localSearchObjectiveValue;
                    
                    // If the solution was a real improvement over the best old
                    // one, warm it up
                    if(localSearchObjectiveValue > bestGlobalObjectiveValue)
                        clusterRoulette.warmup(alnsProperties.getWarmupGamma(), xLocalSearch);
                }

                // Anyway, let's log the local search results
                StringBuffer localSearchComment = new StringBuffer();

                // Let's check if the local search was aborted
                // (it's the negation of the same check we do at the beginning of a
                // local search to decide whether to start a local search or not)
                if (!(alnsProperties.getTimeLimitLocalSearch() > 0
                        && alnsProperties.getTimeLimitLocalSearch() + elapsedTime <= alnsProperties.getTimeLimitALNS()
                        && segmentsWithoutImprovement < 1.0)) {
                    localSearchComment.append("ABORT:");
                    if (alnsProperties.getTimeLimitLocalSearch() <= 0) {
                        localSearchComment.append(" Local search disabled by user!");
                    }
                    if (alnsProperties.getTimeLimitLocalSearch() + elapsedTime > alnsProperties.getTimeLimitALNS()) {
                        localSearchComment.append(" No time left for the local search to run!");
                    }
                    if (segmentsWithoutImprovement >= 1.0) {
                        localSearchComment.append(" No improvement since last segment, it's useless to perform a local search!");
                    }
                } else {
                    localSearchComment.append("OK");
                }

                // Update the elapsed time
                elapsedTime = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTimeInNanos);

                // Log outputs
                String[] logLine = {
                    segments + "", "*" + "", elapsedTime + "",
                    "*", "*", "*", "*", "*",
                    temperature + "",
                    simulatedAnnealingBarrier + "",
                    q + "",
                    "*", "*",
                    "*", "*", "*", "*", "*",
                    csvFormatSolution(xBest), bestObjectiveValueInSegment + "",
                    csvFormatSolution(xGlobalBest), bestGlobalObjectiveValue + "",
                    clusterRoulette.toString(),String.valueOf(clusterRoulette.getAverageProbability()),clusterRoulette.nerfOccurrencesString(),
                    "Local search results. " + localSearchComment.toString()
                };
                xlsxLogger.writeRow(logLine);
                // Send the controller a message to notify we're still running
                notifyController(elapsedTime, OptimizationStatusMessage.Status.RUNNING);
                env.message("\nALNSLOG, " + elapsedTime + ": segment " + segments + " local search result: " + localSearchComment + "\n");

                // Prepare solutions for the next segment
                xOld = xGlobalBest;
                xNew = xGlobalBest;
                xBest = xGlobalBest;
                oldObjectiveValue = bestGlobalObjectiveValue;
                newObjectiveValue = bestGlobalObjectiveValue;
                bestObjectiveValueInSegment = bestGlobalObjectiveValue;

                // Update q and the segment counter
                // q will cycle: if it becomes bigger than qMax, it starts again
                q = (q + alnsProperties.getqDelta()) % qMax;

                // Segments will increase
                segments++;
            } // do: Stopping criteria. If not verified, go on with the next segment
            while (elapsedTime <= alnsProperties.getTimeLimitALNS()
                    && q >= qMin
                    && q <= qMax // We should never exit because of this unless qStart was wrong
                    && segments < alnsProperties.getMaxSegments()
                    && segmentsWithoutImprovement < alnsProperties.getMaxSegmentsWithoutImprovement()
                    && !this.isCancelled());

            // Close the excel logger gracefully
            xlsxLogger.close();
            // Send the controller a message to notify we're stopping
            notifyController(elapsedTime, OptimizationStatusMessage.Status.STOPPING);
            env.message("\nALNSLOG, " + elapsedTime + ": ALNS run completed. Final solution test...\n");

            // Final test to set variables in the model and log vehicle paths
            testSolution(this.model, xGlobalBest, true, alnsProperties.getMaxMIPSNodesForFeasibilityCheck());

            // Save the solution to file
            super.writeSolution();

            // If we were interrupted by the user, throw an exception
            if (this.isCancelled()) {
                throw new InterruptedException("optimizeALNS() interrupted in the solver phase.");
            }
        } catch (InterruptedException e) {
            // Send the controller a message to notify we're stopped
            notifyController(elapsedTime, OptimizationStatusMessage.Status.STOPPED);
            super.cancel(true);
            throw new InterruptedException(e.getMessage());
        }
    }

    /**
     * Calculate an integer estimate of the progress of the solver run
     *
     * @param elapsedTime
     * @return an integer between [0,100] representing what percent of the
     * solver run has been completed
     */
    private int progressEstimate(long elapsedTime) {
        int ret = Math.min((int) Math.round(((double) elapsedTime / (double) alnsProperties.getTimeLimitALNS()) * 100.0), 100);
        return ret;
    }

    /**
     * Picks a random destroy method from the available ones.
     * <br>The probability of getting a specific method depends on its weight in
     * the <tt>destroyMethods</tt> distribution.
     *
     * @return a random destroy method
     */
    private BiFunction<List<Cluster>, Integer, List<Cluster>> pickDestroyMethod() {
        return this.destroyMethods.getRandom();
    }

    /**
     * Pick a random repair method from the available ones.
     * <br>The probability of getting a specific method depends on its weight in
     * the <tt>repairMethods</tt> distribution.
     *
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
     *
     * @param simulatedAnnealingBarrier the probability barrier to beat to
     * accept a solution
     * @return <tt>true</tt> if the new solution is accepted.
     * @throws GRBException if there are problems with sending Gurobi
     * environment messages.
     */
    private boolean acceptSolution(double simulatedAnnealingBarrier)
            throws GRBException {
        /**
         * The following text defines simulated annealing for a minimum problem
         *
         * * The probability function P must be positive even when e' is
         * greater than e. This feature prevents the method from becoming stuck
         * at a local minimum that is worse than the global one.
         *
         * * When T tends to zero, the probability P(e,e',T) must tend to zero
         * if e'>e and to a positive value otherwise. For sufficiently small
         * values of T, the system will then increasingly favor moves that go
         * "downhill" (i.e., to lower energy values), and avoid those that go
         * "uphill." With T = 0 the procedure reduces to the greedy algorithm,
         * which makes only the downhill transitions.
         *
         * * In the original description of SA, the probability P(e,e',T) was
         * equal to 1 when e > e' — i.e., the procedure always moved downhill
         * when it found a way to do so, irrespective of the temperature. Many
         * descriptions and implementations of SA still take this condition as
         * part of the method's definition. However, this condition is not
         * essential for the method to work.
         *
         * * The P function is usually chosen so that the probability of
         * accepting a move decreases when the difference e'-e increases —that
         * is, small uphill moves are more likely than large ones. However, this
         * requirement is not strictly necessary, provided that the above
         * requirements are met.
         */

        Random r = new Random();

        /**
         * This is a problem of maximization, so the value of the objective
         * function must always increase when possible and could decrease with a
         * specified probability if a pejorative solution is found.
         *
         * Let's define: DIFF = newObjectiveValue - oldObjectiveValue > 0 T>0
         * So, if we improve our solution we get that DIFF>=0 and P=exp(DIFF/T)
         * >= 1 => the solution is always accepted If the solution is worse
         * DIFF<0 and 0 < P=exp(DIFF/T) < 1 => the solution is accepted with
         * probability P
         */
        if (r.nextDouble() < simulatedAnnealingBarrier) {
            env.message("\nSolution accepted!\n");
            return true;
        } else {
            env.message("\nSolution rejected!\n");
            return false;
        }
    }

    /**
     * Simulated annealing acceptance criterion.
     * <br>The acceptance criterion is a Simulated Annealing process, repurposed
     * for a maximization problem. A new solution that improves the maximization
     * of the objective function is always accepted, while a pejorative solution
     * can be chosen with probability <br>
     * exp((newObjectiveValue - oldObjectiveValue)/temperature)
     *
     * @param oldObjectiveValue old best value of the objective function
     * @param newObjectiveValue new value of the objective function, obtained
     * through the new solution
     * @param temperature a weighting parameter which should be made slowly
     * decreasing by the caller
     */
    private double simulatedAnnealingMaximization(
            double oldObjectiveValue,
            double newObjectiveValue,
            double temperature) {
        return Math.exp((newObjectiveValue - oldObjectiveValue) / temperature);
    }

    // DESTROY HEURISTICS
    /**
     * This is a template for a destroy (insertion) heuristic. It represents a
     * generic greedy insertion where clusters with the best profit are inserted
     * in the solution.
     *
     * @param inputSolution the solution to destroy
     * @param q the number of clusters to insert
     * @return the destroyed solution
     */
    private List<Cluster> destroyHeuristicTemplate(List<Cluster> inputSolution, int q) {
        // Initialize available clusters
        List<Cluster> availableClusters = this.getClustersNotInSolution(inputSolution);

        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);

        // Sort the available clusters
        availableClusters.sort(Cluster.PROFIT_COMPARATOR.reversed());

        // Add q available clusters (or as many clusters as possible) to the solution
        for (int i = 0; i < q && i < availableClusters.size(); i++) {
            output.add(availableClusters.get(i));
        }

        // return the destroyed input
        return output;
    }

    /**
     * Insertion heuristic. Clusters with the best profit are inserted in the
     * solution in a greedy way.
     *
     * @param inputSolution the solution to destroy
     * @param q the number of clusters to insert
     * @return the destroyed solution
     */
    private List<Cluster> destroyGreedyProfitInsertion(List<Cluster> inputSolution, int q) {
        // Initialize available clusters
        List<Cluster> availableClusters = this.getClustersNotInSolution(inputSolution);

        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);

        // Sort the available clusters
        availableClusters.sort(Cluster.PROFIT_COMPARATOR.reversed());

        // Add q available clusters (or as many clusters as possible) to the solution
        for (int i = 0; i < q && i < availableClusters.size(); i++) {
            output.add(availableClusters.get(i));
        }

        // return the destroyed input
        return output;
    }

    /**
     * Insertion heuristic. Clusters with the least cost are inserted in the
     * solution in a greedy way.
     *
     * @param inputSolution the solution to destroy
     * @param q the number of clusters to insert
     * @return the destroyed solution
     */
    private List<Cluster> destroyGreedyCostInsertion(List<Cluster> inputSolution, int q) {
        // Initialize available clusters
        List<Cluster> availableClusters = this.getClustersNotInSolution(inputSolution);

        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);

        // Sort the available clusters
        availableClusters.sort(Cluster.COST_COMPARATOR);

        // Add q available clusters (or as many clusters as possible) to the solution
        for (int i = 0; i < q && i < availableClusters.size(); i++) {
            output.add(availableClusters.get(i));
        }

        // return the destroyed input
        return output;
    }

    /**
     * Insertion heuristic. Clusters with the highest profit to cost ratio are
     * inserted in the solution in a greedy way.
     *
     * @param inputSolution the solution to destroy
     * @param q the number of clusters to insert
     * @return the destroyed solution
     */
    private List<Cluster> destroyGreedyBestInsertion(List<Cluster> inputSolution, int q) {
        // Initialize available clusters
        List<Cluster> availableClusters = this.getClustersNotInSolution(inputSolution);

        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);

        // Sort the available clusters
        availableClusters.sort(Cluster.PROFIT_COST_RATIO_COMPARATOR.reversed());

        // Add q available clusters (or as many clusters as possible) to the solution
        for (int i = 0; i < q && i < availableClusters.size(); i++) {
            output.add(availableClusters.get(i));
        }

        // return the destroyed input
        return output;
    }

    /**
     * Insertion heuristic. A maximum of q clusters are randomly picked and
     * inserted into the solution.
     *
     * @param inputSolution the solution to destroy
     * @param q the number of clusters to insert
     * @return the destroyed solution
     */
    private List<Cluster> destroyRandomInsertion(List<Cluster> inputSolution, int q) {
        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);

        // Create an uniform random distribution of clusters and populate it
        ObjectDistribution<Cluster> clustersToInsert = new ObjectDistribution<>();
        clustersToInsert.addAll(getClustersNotInSolution(output));

        // Insert q clusters not in the solution, picking them randomly
        for (int i = 0; q > 0 && i < q; i++) {
            Cluster c = clustersToInsert.getRandom();
            output.add(c);
            clustersToInsert.remove(c);
        }

        // Return the repaired input
        return output;
    }

    // REPAIR HEURISTICS
    /**
     * This is a repair heuristic template. This is a removal heuristic. It
     * removes from the solution the q clusters with the highest cost.
     *
     * @param inputSolution the solution to repair
     * @param q number of clusters to remove
     * @return the repaired solution
     */
    private List<Cluster> repairHeuristicTemplate(List<Cluster> inputSolution, int q) {
        // Sort the clusters in the input solution
        inputSolution.sort(Cluster.COST_COMPARATOR.reversed());

        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);

        // Remove q clusters from the solution, following the imposed ordering
        int i = 0;
        for (Cluster c : inputSolution) {
            if (i < q && output.size() > 1) {
                output.remove(c);
                i++;
            } else {
                break;
            }
        }

        // Return the repaired input
        return output;
    }

    /**
     * Removal heuristic. It removes from the solution the q clusters with the
     * highest cost.
     *
     * @param inputSolution the solution to repair
     * @param q number of clusters to remove
     * @return the repaired solution
     */
    private List<Cluster> repairHighCostRemoval(List<Cluster> inputSolution, int q) {
        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);

        // Sort the clusters from the input solution
        output.sort(Cluster.COST_COMPARATOR.reversed());

        // Remove q clusters from the solution, following the imposed ordering
        int i = 0;
        for (Cluster c : inputSolution) {
            if (i < q && output.size() > 1) {
                output.remove(c);
                i++;
            } else {
                break;
            }
        }

        // Return the repaired solution
        return output;
    }

    /**
     * Removal heuristic. Removes the first q clusters with the least
     * profit/cost ratio.
     *
     * @param inputSolution the solution to repair
     * @param q number of clusters to remove
     * @return the repaired solution
     */
    private List<Cluster> repairWorstRemoval(List<Cluster> inputSolution, int q) {
        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);

        // Sort the clusters from the input solution
        output.sort(Cluster.PROFIT_COST_RATIO_COMPARATOR);

        // Remove q clusters from the solution, following the imposed ordering
        int i = 0;
        for (Cluster c : inputSolution) {
            if (i < q && output.size() > 1) {
                output.remove(c);
                i++;
            } else {
                break;
            }
        }

        // Return the repaired solution
        return output;
    }

    /**
     * Removal heuristic. Removes the first cluster with the least profit/cost
     * ratio, then removes the other q-1 clusters which are the most similar to
     * the first one. The similarity criterion is computed as follows:
     * <ul>
     * <li>Find v as the vehicle with the longest streak in the first
     * cluster</li>
     * <li>For every node n in the first cluster get the duration of services
     * servable by v. Sum all the values.</li>
     * <li>Divide the previous value by the total duration of services in the
     * clusters</li>
     * <li>Take the absolute value of the difference between the ratio for the
     * first cluster removed and the ratio for every other cluster as the
     * similarity criterion.</li>
     * </ul>
     *
     * @param inputSolution the solution to repair
     * @param q number of clusters to remove
     * @return the repaired solution
     */
    private List<Cluster> repairVehicleTime(List<Cluster> inputSolution, int q) {
        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);

        // Sort the clusters in the input solution
        output.sort(Cluster.PROFIT_COST_RATIO_COMPARATOR);

        if (q > 0 && output.size() > 1) {
            // Remove 1 cluster from the solution, following the imposed ordering
            Cluster firstClusterRemoved = output.remove(0);

            if (q > 1) {
                Vehicle firstVehicle = null;
                int biggestStreakSize = 0;

                // Stores clusters and their ratios
                LinkedHashMap<Cluster, Double> clustersRatios = new LinkedHashMap<>();

                // Find v as the vehicle with the longest streak in the first cluster
                for (Vehicle v : instance.getVehicles()) {
                    if (v.canServe(firstClusterRemoved)) {
                        // Get the biggest streak of v in first
                        List<Streak> streaks = firstClusterRemoved.getStreaks(v);
                        // If there are streaks for vehicle v in the first cluster removed
                        if (!streaks.isEmpty()) {
                            streaks.sort(Streak.SIZE_COMPARATOR.reversed());
                            int streakSize = streaks.get(0).size();

                            // PROBLEMA: sto prendendo la lunghezza dello streak invece della sua durata!
                            // If the streak we've found is bigger than the previous one
                            // update biggestStreakSize and firstVehicle
                            if (streakSize > biggestStreakSize) {
                                biggestStreakSize = streakSize;
                                firstVehicle = v;
                            }
                        }
                    }
                }

                if (firstVehicle != null) {
                    // Calculate the ratio for the first cluster
                    double firstClusterRatio = firstClusterRemoved.getTotalCostForVehicle(firstVehicle) / (1 + firstClusterRemoved.getTotalCost());

                    // Populate the map that holds cluster IDs (in the output) and their ratios
                    for (int i = 0; i < output.size(); i++) {
                        Cluster c = output.get(i);
                        clustersRatios.put(
                                c,
                                Math.abs(firstClusterRatio - c.getTotalCostForVehicle(firstVehicle) / (1 + c.getTotalCost()))
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
                    for (Cluster c : removeOrder) {
                        if (removed < q - 1 && output.size() > 1) {
                            output.remove(c);
                            removed++;
                        } else {
                            break;
                        }
                    }

                } // debug
                else {
                    try {
                        env.message("Error in repairVehicleTime heuristic!\n"
                                + "No serving vehicle found for first cluster " + firstClusterRemoved.getId() + "\n");
                    } catch (GRBException ex) {
                        Logger.getLogger(ALNS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        // Return the repaired input
        return output;
    }

    /**
     * Removal heuristic. Removes the first cluster with the least profit/cost
     * ratio, then removes the other q-1 clusters which are the most similar to
     * the first one. The similarity criterion is computed as follows:
     * <ul>
     * <li> 1/D * (distanceBetween(i,j) + distanceBetween(firstNode,j) +
     * distanceBetween(j,lastNode))
     * <li> D = 3
     * <li> i = first cluster removed from solution
     * <li> j = cluster to evaluate for removal
     * </ul>
     *
     * @param inputSolution the solution to repair
     * @param q number of clusters to remove
     * @return the repaired solution
     */
    private List<Cluster> repairTravelTime(List<Cluster> inputSolution, int q) {
        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);

        // Constant at the denominator
        double D = 3.0;

        // Sort the clusters in the input solution
        output.sort(Cluster.PROFIT_COST_RATIO_COMPARATOR);

        if (q > 0 && output.size() > 1) {
            // Remove 1 cluster from the solution, following the imposed ordering
            Cluster firstClusterRemoved = output.remove(0);

            if (q > 1) {
                // Initialize the firstNode and lastNode of the instance we're working on
                Node firstNode = instance.getNode(0);
                Node lastNode = instance.getNode(instance.getNum_nodes() - 1);

                double firstRatio = (firstClusterRemoved.distance(firstNode) + firstClusterRemoved.distance(lastNode)) / D;

                // Stores clusters and their ratios
                LinkedHashMap<Cluster, Double> clustersRatios = new LinkedHashMap<>();

                // Populate the map that holds cluster IDs (in the output) and their ratios
                for (int i = 0; i < output.size(); i++) {
                    Cluster candidateCluster = output.get(i);
                    clustersRatios.put(
                            candidateCluster,
                            Math.abs(firstRatio - (firstClusterRemoved.distance(candidateCluster) + candidateCluster.distance(firstNode) + candidateCluster.distance(lastNode)) / D)
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
                for (Cluster c : removeOrder) {
                    if (removed < q - 1 && output.size() > 1) { // DEBUG: weird
                        output.remove(c);
                        removed++;
                    } else {
                        break;
                    }
                }
            }
        }
        // Return the repaired input
        return output;
    }

    /**
     * Removal heuristic. Removes at most q randomly picked clusters from the
     * current solution.
     *
     * @param inputSolution the solution to repair
     * @param q number of clusters to remove
     * @return the repaired solution
     */
    private List<Cluster> repairRandomRemoval(List<Cluster> inputSolution, int q) {
        // Initialize the output
        List<Cluster> output = new ArrayList<>(inputSolution);

        // Create an uniform random distribution of clusters and populate it
        ObjectDistribution<Cluster> clustersToRemove = new ObjectDistribution<>();
        clustersToRemove.addAll(output);

        // Remove q clusters from the solution, picking them randomly
        for (int i = 0; q > 0 && i < q && output.size() > 1; i++) {
            Cluster c = clustersToRemove.getRandom();
            output.remove(c);
            clustersToRemove.remove(c);
        }

        // Return the repaired input
        return output;
    }

    /**
     * This is a special repair heuristic to bring back an eventual infeasible
     * solution into feasibility. It operates by removing the minimum number of
     * low gain clusters when their total cost is more than or equal the
     * difference between the maximum cost for the solution (Tmax) and the
     * actual cost of the solution.
     * <br>Fesibility is tested at the end of the method, so you can expect to
     * find interesting information about this solution in the current model.
     *
     * @param inputSolution an infeasible solution
     * @return a feasible solution
     */
    private List<Cluster> repairBackToFeasibility(List<Cluster> inputSolution) throws Exception {
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
        int lastNodeID = instance.getNum_nodes() - 1;
        GRBLinExpr newObj = new GRBLinExpr();
        for (int s = 0; s < instance.getNum_nodes(); s++) {
            newObj.addTerm(1.0, this.z[s][lastNodeID]);
        }

        // Setup the output
        List<Cluster> output = new ArrayList<>(inputSolution);

        // 0. Check feasibility and start cycling until we have a feasible solution
        boolean isFeasible = testSolutionForFeasibility(output, true, alnsProperties.getMaxMIPSNodesForFeasibilityCheck());
        while (!isFeasible && output.size() > 1) {
            // 1. Compute the feasibilityRelaxation so that we can retrieve some information on z and Tmax
            GRBModel clone = new GRBModel(model);

            // Setup the new value of Tmax as the maximum value for a double
            double safeTMax = instance.getTmax() * instance.getNum_vehicles() * instance.getNum_nodes();
            double[] newTMax = new double[this.constraint8.size()];
            for (int t = 0; t < this.constraint8.size(); t++) {
                newTMax[t] = safeTMax;
            }

            // Set the new value of Tmax as the new upper bound for each z
            for (int i = 0; i < instance.getNum_nodes(); i++) {
                for (int j = 0; j < instance.getNum_nodes(); j++) {
                    GRBVar toFix = clone.getVarByName("z_(" + i + "," + j + ")");
                    toFix.set(GRB.DoubleAttr.UB, safeTMax);
                }
            }

            // try removing all constraints8 from the cloned model, then
            // rebuild them with the new tmax instead
            // Remove all old constraints
            for (GRBConstr grbc : this.constraint8) {
                GRBConstr toRemove = clone.getConstrByName(grbc.get(GRB.StringAttr.ConstrName));
                clone.remove(toRemove);
            }
            // Add new constraints
            for (int i = firstNodeID; i < instance.getNum_nodes(); i++) {
                for (int j = firstNodeID; j < instance.getNum_nodes(); j++) {
                    GRBLinExpr expr8 = new GRBLinExpr();
                    for (int v = 0; v < instance.getNum_vehicles(); v++) {
                        expr8.addTerm(safeTMax, x[v][i][j]);
                    }

                    // Add the constraint to the cloned model
                    // This is one constraint for every z[i][j]
                    clone.addConstr(z[i][j], GRB.LESS_EQUAL, expr8, "c8_arc(" + i + "," + j + ")");
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

            //clone.write("feasibilitySubmodel.lp");
            // Let's test the solution on the feasibility relaxation model (clone)
            //if(true){
            if (this.testSolution(clone, output, false, alnsProperties.getMaxMIPSNodesForFeasibilityCheck())) {
                // If it worked, we're very happy because we can start looking for the maximum value of Z
                double maxZ = -1;
                double tempZ;
                for (int i = 0; i < instance.getNum_nodes(); i++) {
                    GRBVar zCurrent = clone.getVarByName(z[i][lastNodeID].get(GRB.StringAttr.VarName));
                    tempZ = zCurrent.get(GRB.DoubleAttr.X);
                    if (tempZ >= maxZ) {
                        maxZ = tempZ;
                    }
                }

                // 3. Compute costDifference = maxZ-Tmax
                double costDifference = maxZ - instance.getTmax();

                // 4. Sort inputSolution clusters by increasing profit/cost ratio
                output.sort(Cluster.PROFIT_COST_RATIO_COMPARATOR);

                // 5. Get the first cluster from sorted inputSolution with cost >= costDifference
                Cluster toRemove = null;
                for (Cluster c : output) {
                    if (c.getTotalCost() >= costDifference) {
                        toRemove = c;
                        break;
                    }
                }
                // if there isn't a cluster that satisfies our criteria, choose
                // the one with the least profit/cost ratio
                if (toRemove == null) {
                    toRemove = output.get(0);
                }

                // 6. Remove the cluster found in 5.
                output.remove(toRemove);

                // 7. Goto 0 (test feasibility)
                isFeasible = testSolutionForFeasibility(output, true, alnsProperties.getMaxMIPSNodesForFeasibilityCheck());
            } // In case the feasibility relaxation did not work, something went VERY wrong
            else {
                throw new Exception("PROBLEM: the feasibility relaxation was infeasible!");
            }

            // Memory cleanup
            clone.dispose();
        }

        return output;
    }

    /**
     * This is a special repair heuristic to bring back an eventual infeasible
     * solution into feasibility. It operates by removing the minimum number of
     * low gain-high cost clusters.
     * <br>Fesibility is tested at the end of the method, so you can expect to
     * find interesting information about this solution in the current model.
     *
     * @param inputSolution an infeasible solution
     * @return a feasible solution
     * @throws Exception if testing the solution breaks somewhere
     */
    private List<Cluster> repairBackToFeasibility2(List<Cluster> inputSolution) throws Exception {
        // Setup the output
        List<Cluster> output = new ArrayList<>(inputSolution);

        while (!testSolutionForFeasibility(output, false, alnsProperties.getMaxMIPSNodesForFeasibilityCheck())) {
            output = this.repairWorstRemoval(output, 1);
        }

        return output;
    }

    /**
     * This is a special repair heuristic to bring back an eventual infeasible
     * solution into feasibility. It operates by removing the minimum number of
     * clusters through the repeated application of the specified repair method
     * with q=1.
     * <br>Fesibility is tested at the end of the method, so you can expect to
     * find interesting information about this solution in the current model.
     *
     * @param inputSolution an infeasible solution
     * @param repairMethod the repair heuristic to use
     * @return a feasible solution
     * @throws Exception if testing the solution breaks somewhere
     */
    private List<Cluster> repairBackToFeasibility3(
            List<Cluster> inputSolution,
            BiFunction<List<Cluster>, Integer, List<Cluster>> repairMethod
    ) throws Exception {
        // Setup the output
        List<Cluster> output = new ArrayList<>(inputSolution);

        while (output.size() > 1 && !testSolutionForFeasibility(output, false, alnsProperties.getMaxMIPSNodesForFeasibilityCheck())) {
            output = repairMethod.apply(output, 1);
        }

        return output;
    }

    /**
     * This is a special repair heuristic to bring back an eventual infeasible
     * solution into feasibility. It operates by removing the minimum number of
     * clusters to bring the starting solution back to feasibility. This
     * implementation is different than the previous ones because it will test
     * different "degrees of destruction" q until it finds the smallest one that
     * works.
     * <br>If the solution is irreparable, the smallest infeasible solution will
     * be returned.
     * <br>Feasibility is tested again at the end of the method, so you can
     * expect to find interesting information about this solution in the current
     * Gurobi model.
     *
     * @param inputSolution an infeasible solution
     * @param repairMethod the repair heuristic to use
     * @param isFeasible the result of the last feasibility check
     * @return the repaired solution, which can be feasible or infeasible.
     * @throws Exception if testing the solution breaks somewhere
     */
    private List<Cluster> repairBackToFeasibility4(
            List<Cluster> inputSolution,
            BiFunction<List<Cluster>, Integer, List<Cluster>> repairMethod,
            boolean isFeasible
    ) throws Exception {
        // Clone the input
        List<Cluster> inputClone = new ArrayList<>(inputSolution);

        // Setup the starting output solution as a clone of the input solution
        List<Cluster> output = new ArrayList<>(inputSolution);

        // Check the initial input size
        int inputSize = inputSolution.size();

        // Setup the initial degree of reparation
        int q = 1;
        
        // Debug: check if we really need to check again for feasibility (WASTE)
        // boolean isFeasible = testSolutionForFeasibility(output, false);

        // Cycle while q is smaller than the size of the input (we don't want to
        // test empty solutions) and
        // the solution is infeasible (we want to exit the cycle when the solution
        // is either too small or feasible)
        while (!isFeasible && q < inputSize) {
            env.message("\nALNSLOG, trying to repair solution with q="+q+"...\n");
            // Try repairing the input solution with the given heuristic and the given q
            output = repairMethod.apply(inputClone, q);
            isFeasible = testSolutionForFeasibility(output, false, alnsProperties.getMaxMIPSNodesForFeasibilityCheck());

            if (!isFeasible) {
                // Increase q
                q++;
            }
        }

        return output;
    }

    /**
     * Given some solution quality flags, update the weights of the selected
     * destroy and repair methods
     *
     * @param destroyMethod the destroy method to update the weight of
     * @param repairMethod the repair method to update the weight of
     * @param solutionIsNewGlobalOptimum true if the new solution is a new
     * global optimum
     * @param solutionIsBetterThanOld true if the new solution is better than
     * the old one
     * @param solutionIsWorseButAccepted true if the new solution is worse than
     * the old one, but is accepted anyway
     * @param solutionIsWorseAndRejected true if the new solution is worse than
     * the old one and is rejected
     * @param repairMethodWasUsed true if the repair method had to be used (so
     * we need to update its weight too)
     */
    private void updateHeuristicMethodsWeight(
            BiFunction<List<Cluster>, Integer, List<Cluster>> destroyMethod,
            BiFunction<List<Cluster>, Integer, List<Cluster>> repairMethod,
            boolean solutionIsNewGlobalOptimum,
            boolean solutionIsBetterThanOld,
            boolean solutionIsWorseButAccepted,
            boolean solutionIsWorseAndRejected,
            boolean repairMethodWasUsed) {

        // Setup the prize that will be given to the selected heuristics
        double prize = 0.1;

        // Check solution quality to decide the prize
        if (solutionIsNewGlobalOptimum) {
            prize = alnsProperties.getHeuristicScores()[0];
        } else if (solutionIsBetterThanOld) {
            prize = alnsProperties.getHeuristicScores()[1];
        } else if (solutionIsWorseButAccepted) {
            prize = alnsProperties.getHeuristicScores()[2];
        } else if (solutionIsWorseAndRejected) {
            prize = alnsProperties.getHeuristicScores()[3];
        }

        // Give the prize to the selected heuristics, with the best compliments from the house
        double repairOldWeight = repairMethods.getWeightOf(repairMethod);
        double destroyOldWeight = destroyMethods.getWeightOf(destroyMethod);

        if (repairMethodWasUsed) {
            repairMethods.updateWeightSafely(repairMethod, repairOldWeight * alnsProperties.getLambda() + (1 - alnsProperties.getLambda()) * prize);
        }
        destroyMethods.updateWeightSafely(destroyMethod, destroyOldWeight * alnsProperties.getLambda() + (1 - alnsProperties.getLambda()) * prize);
    }

    /**
     * Resets the weights (scores) of every repair and destroy heuristic method.
     * The new value will be 1.0.
     */
    private void resetHeuristicMethodsWeight() {
        repairMethods.resetWeights();
        destroyMethods.resetWeights();
    }

    /**
     * Return a list of available clusters which have not been chosen for the
     * given solution.
     * 
     * <br>Clusters are chosen with a probability that varies on
     * their past behaviour through a special "roulette system" (those who bring
     * us to infeasibility have a lesser chance of being chosen).
     * 
     * <br>If no clusters are chosen through this method,
     * there are different fallback alternatives,
     * such as querying for all available clusters which are not nerf
     * candidates, querying for available clusters with a probability above the 
     * average for the current iteration and returning all available clusters in
     * the problem.
     * 
     * <br>In the worst case scenario, an empty list is returned.
     *
     * @param solution the solution to analyze
     * @return a list of available clusters which have not been chosen for the
     * given solution
     */
    private List<Cluster> getClustersNotInSolution(List<Cluster> solution) {
        List<Cluster> availableClusters = clusterRoulette.query();
        availableClusters.removeAll(solution);
        
        if(availableClusters.isEmpty()){
            try {
                env.message(
                        "\nALNSLOG: no clusters have been extracted! "
                                + "\nQuerying for clusters which have been above "
                                + "average more than the "
                                + alnsProperties.getNerfBarrier()*100
                                + "% of the time...\n"
                );
            } catch (GRBException ex) {
                Logger.getLogger(ALNS.class.getName()).log(Level.SEVERE, null, ex);
            }
            availableClusters = clusterRoulette.queryNotNerfCandidates(alnsProperties.getNerfBarrier());
            availableClusters.removeAll(solution);

            if(availableClusters.isEmpty()){
                try {
                    env.message("\nALNSLOG: no clusters have been extracted (again)!"
                            + "\nQuerying for clusters above average in the current iteration...\n");
                } catch (GRBException ex) {
                    Logger.getLogger(ALNS.class.getName()).log(Level.SEVERE, null, ex);
                }
                availableClusters = clusterRoulette.queryHighPass(clusterRoulette.getAverageProbability());
                availableClusters.removeAll(solution);

                if(availableClusters.isEmpty()){
                    try {
                        env.message("\nALNSLOG: no clusters have been extracted (again)!"
                                + "\nUsing all available clusters as a last resort...");
                    } catch (GRBException ex) {
                        Logger.getLogger(ALNS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    availableClusters = instance.cloneClusters();
                    availableClusters.removeAll(solution);
                    if(availableClusters.isEmpty()){
                        try {
                            env.message("\nALNSLOG: no clusters available for insertion! ALNS is giving up!\n");
                        } catch (GRBException ex) {
                            Logger.getLogger(ALNS.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
        return availableClusters;
    }

    /**
     * Converts a proposed solution (a list of clusters) to a string of comma
     * separated values.
     *
     * @param solution the list of Clusters to convert to CSV
     * @return the CSV converted solution
     */
    private String csvFormatSolution(List<Cluster> solution) {
        String SEPARATOR = ", ";
        StringBuffer sb = new StringBuffer();

        for (Cluster c : solution) {
            sb.append(c.toString());
            if (solution.indexOf(c) != solution.size() - 1) {
                sb.append(SEPARATOR);
            }
        }
        return sb.toString();
    }

    /**
     * Perform a local search run.
     *
     * @param inputSolution the solution to start the local search from
     * @param elapsedTime how long the solver has been working on the ALNS for
     * @param timeLimitForLocalSearch how much time we should spend in the local
     * search
     * @param segmentsWithoutImprovement how many segments without improvement
     * have there been before the local search started
     * @return the solution computed by the local search
     * @throws GRBException if there are problems handling Gurobi objects
     * @throws Exception if there are other problems
     */
    private List<Cluster> localSearch(
            List<Cluster> inputSolution,
            long elapsedTime,
            long timeLimitForLocalSearch,
            long segmentsWithoutImprovement
    ) throws GRBException, Exception {
        List<Cluster> output = new ArrayList<>(inputSolution);

        // Perform a local search on the input solution, but only if there is time for it
        // and if it's likely that it will improve the best solution of the this segment.
        // If this segment's solution didn't improve the one at the last segment,
        // this local search run would have the same starting solution as the
        // local search one at the end of the previous segment.
        // Since the run time of these local searches would be the same, there would
        // be no advantage in using the local search with the same parameters
        // (it would yield the same result and waste precious time)
        if (timeLimitForLocalSearch > 0
                && timeLimitForLocalSearch + elapsedTime <= alnsProperties.getTimeLimitALNS()
                && segmentsWithoutImprovement < 1.0) {
            // Log the beginning of the search
            env.message("\nALNSLOG: Local search started at " + elapsedTime + "s - " + LocalDateTime.now().toString() + "\n");

            // Make sure all clusters can be selected by the solver
            this.resetSolution();

            // Reset the model so that it's fresh
            model.reset();
            
            // Remove nerfed clusters from the local search model
            List<Cluster> nerfedClusters = clusterRoulette.queryNerfCandidates(alnsProperties.getNerfBarrier());
            int nerfSize = nerfedClusters.size();
            // ... but keep all the nerfed clusters which are in the current best solution (there should be none, but let's make sure)
            nerfedClusters.removeAll(inputSolution);
            this.removeFromSolution(nerfedClusters);
            
            nerfSize -= nerfedClusters.size();
            env.message("\nALNSLOG: there were "+nerfSize+" nerfed clusters in inputSolution for local search. Local search starting.\n");
            
            // Apply heuristic constraints
            this.toggleHeuristicConstraintsOn();

            // Clone the original model with the heuristic constraints
            GRBModel clone = new GRBModel(this.model);
            GRBModel toSolve = new GRBModel(this.model);
            
            // Test the solution on the clone model so that we can gather informations on the warm solution
            if (this.testSolution(clone, inputSolution, true, alnsProperties.getMaxMIPSNodesForFeasibilityCheck())) { // DEBUG: LOCAL SEARCH CRASHES BECAUSE IT DOESN'T ENTER HERE?
                // Gather informations on the solution
                GRBVar[] cloneVars = clone.getVars();

                // Get the name and the value of the variable and set it as a
                // starting point into our model for a warm start
                for (GRBVar v : cloneVars) {
                    String varName = v.get(GRB.StringAttr.VarName);
                    Double varValue = v.get(GRB.DoubleAttr.X);
                    toSolve.getVarByName(varName).set(GRB.DoubleAttr.Start, varValue);
                }

                // Now the model is ready to be run for the given time using our
                // heuristic constraints
                // model.setCallback(new localSearchCallback(timeLimitForLocalSearch));
                toSolve.set(GRB.DoubleParam.TimeLimit, (double) timeLimitForLocalSearch);

                // Update and optimizeALNS
                toSolve.update();
                toSolve.optimize();
                
                // Now the model should be optimized. If we've found a solution,
                // let's save it to the output variable
                output = this.getClustersInCurrentModelSolution(toSolve);
                
                // Let's remove the callback from the model
                //model.setCallback(null);
            }

            // Let's clear the model from the added constraints
            this.toggleHeuristicConstraintsOff();
            resetSolution();

            // Free the memory
            clone.dispose();
            toSolve.dispose();

            // Log the end of the search
            env.message("ALNSLOG: Local search started at " + elapsedTime + "s ended at " + LocalDateTime.now().toString());
        } else {
            env.message("ALNSLOG: Local search aborted at " + elapsedTime + "s - " + LocalDateTime.now().toString());
        }

        // To finish, we shall test the solution found, so that the model shall
        // come loaded with the new solution
        this.testSolution(this.model, output, true, alnsProperties.getMaxMIPSNodesForFeasibilityCheck());
        return output;
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        try {
            this.optimize();
        } catch (InterruptedException e) {
            env.message("ALNSLOG: Optimization interrupted by user.\n");
            env.message(e.getMessage());
            this.cleanup();
            //DEBUG: uncomment later
            logRedirector.cancel(true);
            return false;
        } finally {
            this.cleanup();
            if (logRedirector == null) {
                System.out.println("Logredirector is null!");
            }
            logRedirector.cancel(true);
        }
        // We're done!
        return true;
    }

    /**
     * Notify the controller of current solver progress
     *
     * @param elapsedTime time elapsed since the start of the current solver
     * process
     * @param alnsStatus current status of the ALNS solver
     */
    protected void notifyController(long elapsedTime, OptimizationStatusMessage.Status alnsStatus) {
        OptimizationStatusMessage.Status realState = OptimizationStatusMessage.Status.RUNNING;
        switch (alnsStatus) {
            case STARTING:
                if (isCancelled()) {
                    realState = OptimizationStatusMessage.Status.STOPPING;
                } else {
                    realState = OptimizationStatusMessage.Status.STARTING;
                }
                break;

            case RUNNING:
                if (isCancelled()) {
                    realState = OptimizationStatusMessage.Status.STOPPING;
                } else {
                    realState = OptimizationStatusMessage.Status.RUNNING;
                }
                break;

            case DONE:
                realState = OptimizationStatusMessage.Status.DONE;
                break;

            case STOPPED:
                realState = OptimizationStatusMessage.Status.STOPPED;
                break;
        }

        // Do an estimate of the progress
        this.setProgress(this.progressEstimate(elapsedTime));

        OptimizationStatusMessage osm = new OptimizationStatusMessage(
                instance.getName(),
                this.getProgress(),
                elapsedTime,
                realState
        );

        controller.setMessageFromALNS(osm);
    }

    @Override
    protected void process(List<OptimizationStatusMessage> messages) {
        if (messages != null && !messages.isEmpty()) {

        }
    }

    @Override
    public void done() {
        if (this.getState() == StateValue.DONE) {
            if (!isCancelled()) {
                notifyController(elapsedTime, OptimizationStatusMessage.Status.DONE);
            } else {
                notifyController(elapsedTime, OptimizationStatusMessage.Status.STOPPED);
            }
        }
        /*
        try {
            boolean cancelLogRedir = this.logRedirector.cancel(true);
            env.message("Did you close the logger door? "+cancelLogRedir+"\n");
        } catch (GRBException ex) {
            Logger.getLogger(ALNS.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch(Exception e){
            System.out.println("Strange error happened: "+e.getMessage());
        }
         */
    }
} // end of class ALNS

/**
 * Inner class to handle callbacks that solve the model for a given amount of
 * time
 *
 * @author Frash
 */
class localSearchCallback extends GRBCallback {

    /**
     * Time limit for the execution of a solver run
     */
    private long timeLimit;

    /**
     * Constructor for class localSearchCallback
     *
     * @param timeLimit
     */
    public localSearchCallback(long timeLimit) {
        this.timeLimit = timeLimit;
    }

    @Override
    protected void callback() {
        try {
            if (where == GRB.CB_MIP) {
                if (getDoubleInfo(GRB.CB_RUNTIME) > timeLimit) {
                    abort();
                }
            } else if (where == GRB.CB_MIPSOL) {
                abort();
            }
        } catch (GRBException ex) {
            Logger.getLogger(Orienteering.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
