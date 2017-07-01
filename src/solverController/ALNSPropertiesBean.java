/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverController;

import java.io.Serializable;

/**
 * Java Bean to hold all the properties and parameters for the ALNS optimizer.
 * @author Frash
 */
public class ALNSPropertiesBean implements Serializable {

    /**
     * Maximum size of the past history
     */
    private int maxHistorySize = 30;
    
    /**
     * The starting value of q (degree of destruction)
     */
    private int qStart = 1;
    
    /**
     * The increment of q at the end of every segment
     */
    private int qDelta = 5;
    
    /**
     * Number of iterations in an optimization segment
     */
    private int segmentSize = 100;
    
    /**
     * Maximum number of segments in an ALNS run
     */
    private long maxSegments = 1000;
    
    /**
     * Number of segments without improvement before ALNS termination
     */
    private long maxSegmentsWithoutImprovement = 100;
    
    /**
     *  Determines whether to use this heuristic.
     */
    private boolean useDestroyGreedyCostInsertion = true;
    /**
     *  Determines whether to use this heuristic.
     */
    private boolean useDestroyGreedyBestInsertion = true;
    /**
     *  Determines whether to use this heuristic.
     */
    private boolean useDestroyGreedyProfitInsertion = true;
    /**
     *  Determines whether to use this heuristic.
     */
    private boolean useDestroyRandomInsertion = true;

    /**
     *  Determines whether to use this heuristic.
     */
    private boolean useRepairHighCostRemoval = true;
    /**
     *  Determines whether to use this heuristic.
     */
    private boolean useRepairRandomRemoval = true;
    /**
     *  Determines whether to use this heuristic.
     */
    private boolean useRepairTravelTime = true;
    /**
     *  Determines whether to use this heuristic.
     */
    private boolean useRepairVehicleTime = true;
    /**
     *  Determines whether to use this heuristic.
     */
    private boolean useRepairWorstRemoval = true;
    
    /**
     * This is the decay parameter of the update process for heuristic method weights.
     * <br>This value should be a double in the interval [0,1].
     * <br>Heuristic method weights are updated following the convex combination
     * <br> newWeight = lambda*oldWeight + (1-lambda)*psi
     * <br>where psi is a value that indicates the relative score to give to an heuristic.
     */
    private double lambda = 0.5;
    
    /**
     * This is the decay parameter of the update process for Temperature.
     * <br>This value should be a double in the interval [0,1].
     * <br>The temperature is updated at the end of every segment like
     * <br>newTemperature = alpha*oldTemperature
     * <br>so that a slowly decreasing temperature (alpha-&gt;1) will make fluctuations
     * in accepted solutions much stronger
     */
    private double alpha = 0.85;
    
    /**
     * Maximum runtime for the ALNS heuristic algorithm (in seconds)
     */
    private long timeLimitALNS = 70;
    
    /**
     * Maximum runtime for the local search process (in seconds)
     */
    private long timeLimitLocalSearch = 90;
    
    /**
     * A scaling factor that's applied to the weight of the best heuristics at the beginning of every segment
     */
    private double rewardForBestSegmentHeuristics = 1.5;
    
    /**
     * A scaling factor that's applied to the weight of the worst heuristics at the beginning of every segment
     */
    private double punishmentForWorstSegmentHeuristics = 0.5;
    
    /**
     * This constant holds the possible values of psi, the function that prizes
     * good heuristics and penalizes the bad ones.
     * <br>The default value for it is:
     * <br><tt>{3.0, 2.0, 1.0, 0.1}</tt>
     */
    public final static double[] DEFAULT_HEURISTIC_SCORES = {3.0, 2.0, 1.0, 0.0};
    
    /**
     * This constant holds the number of possible output values for function psi,
     * the one that gives a score for every heuristic. It's used for debugging.
     */
    public final static int NUMBER_OF_VALUES_FOR_HEURISTIC_SCORES = 4;
    
    /**
     * This parameters holds the values of psi, the function that prizes good
     * heuristics and penalizes the bad ones.
     */
    private double[] heuristicScores = DEFAULT_HEURISTIC_SCORES;
    
    /**
     * Determines the maximum number of mips nodes to check before giving up a
     * feasibility check.
     */
    private double maxMIPSNodesForFeasibilityCheck = 5000;
    
    /**
     * Determines how many ALNS iterations without improvement should be accepted
     * before the algorithm moves on to a new segment.
     * Ideally it should be the same as the number of iterations per segment.
     */
    private int maxIterationsWithoutImprovement = 50;
    
    /**
     * Empty constructor.
     */
    public ALNSPropertiesBean() {
    }

    /**
     * Maximum size of the past history
     * @return the maxHistorySize
     */
    public int getMaxHistorySize() {
        return maxHistorySize;
    }

    /**
     * Maximum size of the past history
     * @param maxHistorySize the maxHistorySize to set
     */
    public void setMaxHistorySize(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
    }

    /**
     * The starting value of q (degree of destruction)
     * @return the qStart
     */
    public int getqStart() {
        return qStart;
    }

    /**
     * The starting value of q (degree of destruction)
     * @param qStart the qStart to set
     */
    public void setqStart(int qStart) {
        this.qStart = qStart;
    }

    /**
     * The increment of q at the end of every segment
     * @return the qDelta
     */
    public int getqDelta() {
        return qDelta;
    }

    /**
     * The increment of q at the end of every segment
     * @param qDelta the qDelta to set
     */
    public void setqDelta(int qDelta) {
        this.qDelta = qDelta;
    }

    /**
     * Number of iterations in an optimization segment
     * @return the segmentSize
     */
    public int getSegmentSize() {
        return segmentSize;
    }

    /**
     * Number of iterations in an optimization segment
     * @param segmentSize the segmentSize to set
     */
    public void setSegmentSize(int segmentSize) {
        this.segmentSize = segmentSize;
    }

    /**
     * Maximum number of segments in an ALNS run
     * @return the maxSegments
     */
    public long getMaxSegments() {
        return maxSegments;
    }

    /**
     * Maximum number of segments in an ALNS run
     * @param maxSegments the maxSegments to set
     */
    public void setMaxSegments(long maxSegments) {
        this.maxSegments = maxSegments;
    }

    /**
     * Number of segments without improvement before ALNS termination
     * @return the maxSegmentsWithoutImprovement
     */
    public long getMaxSegmentsWithoutImprovement() {
        return maxSegmentsWithoutImprovement;
    }

    /**
     * Number of segments without improvement before ALNS termination
     * @param maxSegmentsWithoutImprovement the maxSegmentsWithoutImprovement to set
     */
    public void setMaxSegmentsWithoutImprovement(long maxSegmentsWithoutImprovement) {
        this.maxSegmentsWithoutImprovement = maxSegmentsWithoutImprovement;
    }

    /**
     * Determines whether to use this heuristic.
     * @return the useDestroyGreedyCostInsertion
     */
    public boolean isUseDestroyGreedyCostInsertion() {
        return useDestroyGreedyCostInsertion;
    }

    /**
     * Determines whether to use this heuristic.
     * @param useDestroyGreedyCostInsertion the useDestroyGreedyCostInsertion to set
     */
    public void setUseDestroyGreedyCostInsertion(boolean useDestroyGreedyCostInsertion) {
        this.useDestroyGreedyCostInsertion = useDestroyGreedyCostInsertion;
    }

    /**
     * Determines whether to use this heuristic.
     * @return the useDestroyGreedyBestInsertion
     */
    public boolean isUseDestroyGreedyBestInsertion() {
        return useDestroyGreedyBestInsertion;
    }

    /**
     * Determines whether to use this heuristic.
     * @param useDestroyGreedyBestInsertion the useDestroyGreedyBestInsertion to set
     */
    public void setUseDestroyGreedyBestInsertion(boolean useDestroyGreedyBestInsertion) {
        this.useDestroyGreedyBestInsertion = useDestroyGreedyBestInsertion;
    }

    /**
     * Determines whether to use this heuristic.
     * @return the useDestroyGreedyProfitInsertion
     */
    public boolean isUseDestroyGreedyProfitInsertion() {
        return useDestroyGreedyProfitInsertion;
    }

    /**
     * Determines whether to use this heuristic.
     * @param useDestroyGreedyProfitInsertion the useDestroyGreedyProfitInsertion to set
     */
    public void setUseDestroyGreedyProfitInsertion(boolean useDestroyGreedyProfitInsertion) {
        this.useDestroyGreedyProfitInsertion = useDestroyGreedyProfitInsertion;
    }

    /**
     * Determines whether to use this heuristic.
     * @return the useDestroyRandomInsertion
     */
    public boolean isUseDestroyRandomInsertion() {
        return useDestroyRandomInsertion;
    }

    /**
     * Determines whether to use this heuristic.
     * @param useDestroyRandomInsertion the useDestroyRandomInsertion to set
     */
    public void setUseDestroyRandomInsertion(boolean useDestroyRandomInsertion) {
        this.useDestroyRandomInsertion = useDestroyRandomInsertion;
    }

    /**
     * Determines whether to use this heuristic.
     * @return the useRepairHighCostRemoval
     */
    public boolean isUseRepairHighCostRemoval() {
        return useRepairHighCostRemoval;
    }

    /**
     * Determines whether to use this heuristic.
     * @param useRepairHighCostRemoval the useRepairHighCostRemoval to set
     */
    public void setUseRepairHighCostRemoval(boolean useRepairHighCostRemoval) {
        this.useRepairHighCostRemoval = useRepairHighCostRemoval;
    }

    /**
     * Determines whether to use this heuristic.
     * @return the useRepairRandomRemoval
     */
    public boolean isUseRepairRandomRemoval() {
        return useRepairRandomRemoval;
    }

    /**
     * Determines whether to use this heuristic.
     * @param useRepairRandomRemoval the useRepairRandomRemoval to set
     */
    public void setUseRepairRandomRemoval(boolean useRepairRandomRemoval) {
        this.useRepairRandomRemoval = useRepairRandomRemoval;
    }

    /**
     * Determines whether to use this heuristic.
     * @return the useRepairTravelTime
     */
    public boolean isUseRepairTravelTime() {
        return useRepairTravelTime;
    }

    /**
     * Determines whether to use this heuristic.
     * @param useRepairTravelTime the useRepairTravelTime to set
     */
    public void setUseRepairTravelTime(boolean useRepairTravelTime) {
        this.useRepairTravelTime = useRepairTravelTime;
    }

    /**
     * Determines whether to use this heuristic.
     * @return the useRepairVehicleTime
     */
    public boolean isUseRepairVehicleTime() {
        return useRepairVehicleTime;
    }

    /**
     * Determines whether to use this heuristic.
     * @param useRepairVehicleTime the useRepairVehicleTime to set
     */
    public void setUseRepairVehicleTime(boolean useRepairVehicleTime) {
        this.useRepairVehicleTime = useRepairVehicleTime;
    }

    /**
     * Determines whether to use this heuristic.
     * @return the useRepairWorstRemoval
     */
    public boolean isUseRepairWorstRemoval() {
        return useRepairWorstRemoval;
    }

    /**
     * Determines whether to use this heuristic.
     * @param useRepairWorstRemoval the useRepairWorstRemoval to set
     */
    public void setUseRepairWorstRemoval(boolean useRepairWorstRemoval) {
        this.useRepairWorstRemoval = useRepairWorstRemoval;
    }

    /**
     * This is the decay parameter of the update process for heuristic method weights.
     * <br>This value should be a double in the interval [0,1].
     * <br>Heuristic method weights are updated following the convex combination
     * <br> newWeight = lambda*oldWeight + (1-lambda)*psi
     * <br>where psi is a value that indicates the relative score to give to an heuristic.
     * @return the lambda
     */
    public double getLambda() {
        return lambda;
    }

    /**
     * This is the decay parameter of the update process for heuristic method weights.
     * <br>This value should be a double in the interval [0,1].
     * <br>Heuristic method weights are updated following the convex combination
     * <br> newWeight = lambda*oldWeight + (1-lambda)*psi
     * <br>where psi is a value that indicates the relative score to give to an heuristic.
     * @param lambda the lambda to set
     */
    public void setLambda(double lambda) {
        // Lambda setup - values out of range [0,1] clip to range boundaries
        if(lambda < 1.0) this.lambda = lambda;
        else this.lambda = 1.0;
        if(lambda < 0) this.lambda = 0.0;
    }

    /**
     * This is the decay parameter of the update process for Temperature.
     * <br>This value should be a double in the interval [0,1].
     * <br>The temperature is updated at the end of every segment like
     * <br>newTemperature = alpha*Temperature
     * <br>so that a slowly decreasing temperature (alpha-&gt;1) will make fluctuations
     * in accepted solutions much stronger
     * @return the alpha
     */
    public double getAlpha() {
        return alpha;
    }

    /**
     * This is the decay parameter of the update process for Temperature.
     * <br>This value should be a double in the interval [0,1].
     * <br>The temperature is updated at the end of every segment like
     * <br>newTemperature = alpha*Temperature
     * <br>so that a slowly decreasing temperature (alpha-&gt;1) will make fluctuations
     * in accepted solutions much stronger
     * @param alpha the alpha to set
     */
    public void setAlpha(double alpha) {
        // Alpha setup - values out of range [0,1] clip to values close to range boundaries
        // we want the temperature to always decrease if not explicitly stated otherwise
        if(alpha < 1.0) this.alpha = alpha;
        else this.lambda = 1.0;
        if(lambda < 0) this.lambda = 0.0;
    }

    /**
     * Maximum runtime for the ALNS heuristic algorithm (in seconds)
     * @return the timeLimitALNS
     */
    public long getTimeLimitALNS() {
        return timeLimitALNS;
    }

    /**
     * Maximum runtime for the ALNS heuristic algorithm (in seconds)
     * @param timeLimitALNS the timeLimitALNS to set
     */
    public void setTimeLimitALNS(long timeLimitALNS) {
        this.timeLimitALNS = timeLimitALNS;
    }

    /**
     * Maximum runtime for the local search process (in seconds)
     * @return the timeLimitLocalSearch
     */
    public long getTimeLimitLocalSearch() {
        return timeLimitLocalSearch;
    }

    /**
     * Maximum runtime for the local search process (in seconds)
     * @param timeLimitLocalSearch the timeLimitLocalSearch to set
     */
    public void setTimeLimitLocalSearch(long timeLimitLocalSearch) {
        this.timeLimitLocalSearch = timeLimitLocalSearch;
    }

    /**
     * A scaling factor that's applied to the weight of the best heuristics at the beginning of every segment
     * @return the rewardForBestSegmentHeuristics
     */
    public double getRewardForBestSegmentHeuristics() {
        return rewardForBestSegmentHeuristics;
    }

    /**
     * A scaling factor that's applied to the weight of the best heuristics at the beginning of every segment
     * @param rewardForBestSegmentHeuristics the rewardForBestSegmentHeuristics to set
     */
    public void setRewardForBestSegmentHeuristics(double rewardForBestSegmentHeuristics) {
        this.rewardForBestSegmentHeuristics = rewardForBestSegmentHeuristics;
    }

    /**
     * A scaling factor that's applied to the weight of the worst heuristics at the beginning of every segment
     * @return the punishmentForWorstSegmentHeuristics
     */
    public double getPunishmentForWorstSegmentHeuristics() {
        return punishmentForWorstSegmentHeuristics;
    }

    /**
     * A scaling factor that's applied to the weight of the worst heuristics at the beginning of every segment
     * @param punishmentForWorstSegmentHeuristics the punishmentForWorstSegmentHeuristics to set
     */
    public void setPunishmentForWorstSegmentHeuristics(double punishmentForWorstSegmentHeuristics) {
        this.punishmentForWorstSegmentHeuristics = punishmentForWorstSegmentHeuristics;
    }

    /**
     * This parameters holds the values of psi, the function that prizes good
     * heuristics and penalizes the bad ones.
     * @return the heuristicScores
     */
    public double[] getHeuristicScores() {
        return heuristicScores;
    }

    /**
     * This parameters holds the values of psi, the function that prizes good
     * heuristics and penalizes the bad ones.
     * @param heuristicScores the heuristicScores to set
     */
    public void setHeuristicScores(double[] heuristicScores) {
        this.heuristicScores = heuristicScores;
    }

    /**
     * Determines the maximum number of mips nodes to check before giving up a
     * feasibility check.
     * @return the maxMIPSNodesForFeasibilityCheck
     */
    public double getMaxMIPSNodesForFeasibilityCheck() {
        return maxMIPSNodesForFeasibilityCheck;
    }

    /**
     * Determines the maximum number of mips nodes to check before giving up a
     * feasibility check.
     * @param maxMIPSNodesForFeasibilityCheck the maxMIPSNodesForFeasibilityCheck to set
     */
    public void setMaxMIPSNodesForFeasibilityCheck(double maxMIPSNodesForFeasibilityCheck) {
        this.maxMIPSNodesForFeasibilityCheck = maxMIPSNodesForFeasibilityCheck;
    }

    /**
     * Determines how many ALNS iterations without improvement should be accepted
     * before the algorithm moves on to a new segment.
     * Ideally it should be the same as the number of iterations per segment.
     * @return the maxIterationsWithoutImprovement
     */
    public int getMaxIterationsWithoutImprovement() {
        return maxIterationsWithoutImprovement;
    }

    /**
     * Determines how many ALNS iterations without improvement should be accepted
     * before the algorithm moves on to a new segment.
     * Ideally it should be the same as the number of iterations per segment.
     * @param maxIterationsWithoutImprovement the maxIterationsWithoutImprovement to set
     */
    public void setMaxIterationsWithoutImprovement(int maxIterationsWithoutImprovement) {
        this.maxIterationsWithoutImprovement = maxIterationsWithoutImprovement;
    }
}