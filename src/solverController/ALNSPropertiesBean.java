/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverController;

import java.beans.PropertyChangeSupport;

/**
 * Java Bean to hold all the properties and parameters for the ALNS optimizer.
 *
 * @author Frash
 */
public class ALNSPropertiesBean {

    /**
     * Empty constructor.
     */
    public ALNSPropertiesBean() {

    }
    
    /**
     * Cloning method: clones all attributes from given bean
     * 
     * @param apb the ALNSPropertiesBean to clone from
     */
    public void cloneFrom(ALNSPropertiesBean apb){
        this.maxHistorySize = apb.getMaxHistorySize();
        this.qStart = apb.getqStart();
        this.qDelta = apb.getqDelta();
        this.segmentSize = apb.getSegmentSize();
        this.maxSegments = apb.getMaxSegments();
        this.maxSegmentsWithoutImprovement = apb.getMaxSegmentsWithoutImprovement();
        this.useDestroyCloseToBarycenter = apb.isUseDestroyCloseToBarycenter();
        this.useDestroyGreedyCostInsertion = apb.isUseDestroyGreedyCostInsertion();
        this.useDestroyGreedyBestInsertion = apb.isUseDestroyGreedyBestInsertion();
        this.useDestroyGreedyProfitInsertion = apb.isUseDestroyGreedyProfitInsertion();
        this.useDestroyRandomInsertion = apb.isUseDestroyRandomInsertion();
        this.useRepairHighCostRemoval = apb.isUseRepairHighCostRemoval();
        this.useRepairRandomRemoval = apb.isUseRepairRandomRemoval();
        this.useRepairTravelTime = apb.isUseRepairTravelTime();
        this.useRepairVehicleTime = apb.isUseRepairVehicleTime();
        this.useRepairWorstRemoval = apb.isUseRepairWorstRemoval();
        this.lambda = apb.getLambda();
        this.alpha = apb.getAlpha();
        this.punishmentGamma = apb.getPunishmentGamma();
        this.cooldownGamma = apb.getCooldownGamma();
        this.warmupGamma = apb.getWarmupGamma();
        this.nerfBarrier = apb.getNerfBarrier();
        this.timeLimitALNS = apb.getTimeLimitALNS();
        this.timeLimitLocalSearch = apb.getTimeLimitLocalSearch();
        this.heuristicScores = apb.getHeuristicScores();
        this.rewardForBestSegmentHeuristics = apb.getRewardForBestSegmentHeuristics();
        this.punishmentForWorstSegmentHeuristics = apb.getPunishmentForWorstSegmentHeuristics();
        this.maxMIPSNodesForFeasibilityCheck = apb.getMaxMIPSNodesForFeasibilityCheck();
        this.maxIterationsWithoutImprovement = apb.getMaxIterationsWithoutImprovement();
    }

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
    private long maxSegments = 10000;

    /**
     * Number of segments without improvement before ALNS termination
     */
    private long maxSegmentsWithoutImprovement = 10000;
    
    /**
     * Determines whether to use this heuristic.
     */
    private boolean useDestroyCloseToBarycenter = true;
    
    /**
     * Determines whether to use this heuristic.
     */
    private boolean useDestroyGreedyCostInsertion = true;
    /**
     * Determines whether to use this heuristic.
     */
    private boolean useDestroyGreedyBestInsertion = true;
    /**
     * Determines whether to use this heuristic.
     */
    private boolean useDestroyGreedyProfitInsertion = true;
    /**
     * Determines whether to use this heuristic.
     */
    private boolean useDestroyRandomInsertion = true;

    /**
     * Determines whether to use this heuristic.
     */
    private boolean useRepairHighCostRemoval = true;
    /**
     * Determines whether to use this heuristic.
     */
    private boolean useRepairRandomRemoval = true;
    /**
     * Determines whether to use this heuristic.
     */
    private boolean useRepairTravelTime = true;
    /**
     * Determines whether to use this heuristic.
     */
    private boolean useRepairVehicleTime = true;
    /**
     * Determines whether to use this heuristic.
     */
    private boolean useRepairWorstRemoval = true;

    /**
     * This is the decay parameter of the update process for heuristic method
     * weights.
     * <br>This value should be a double in the interval [0,1].
     * <br>Heuristic method weights are updated following the convex combination
     * <br> newWeight = lambda*oldWeight + (1-lambda)*psi
     * <br>where psi is a value that indicates the relative score to give to an
     * heuristic.
     */
    private double lambda = 0.55;

    /**
     * This is the decay parameter of the update process for Temperature.
     * <br>This value should be a double in the interval [0,1].
     * <br>The temperature is updated at the end of every segment like
     * <br>newTemperature = alpha*oldTemperature
     * <br>so that a slowly decreasing temperature (alpha-&gt;1) will make
     * fluctuations in accepted solutions much stronger
     */
    private double alpha = 0.85;

    /**
     * This parameter is the scaling factor to change the chance of bad clusters
     * being chosen for insertion heuristics.
     * <br>Must be a double in range [0,1].
     * <br>Probability for bad clusters is downscaled like
     * <br><i>newProbability</i> =
     * <i>punishmentGamma</i>*<i>oldProbability</i>
     */
    private double punishmentGamma = 0.60;

    /**
     * This parameter is the scaling factor used in the cooldown process.
     * <br>Must be a small double in range [0,1].
     * <br>A hot (freshly selected) cluster will have a new probability of being
     * chosen which is cooldownGamma times smaller
     *
     * <br>Probability for hot clusters is downscaled like
     * <br><i>newProbability</i> = (1 -
     * <i>cooldownGamma</i>)*<i>oldProbability</i>
     */
    private double cooldownGamma = 0.1;

    /**
     * This parameter is the scaling factor used in the warmup process.
     * <br>Must be a small double in range [0,1].
     * <br>A cold (not freshly selected) cluster will have a new probability of
     * being chosen which is warmupGamma times bigger
     *
     * <br>Probability for cold clusters is upscaled like
     * <br><i>newProbability</i> = (1 -
     * <i>warmupGamma</i>)*<i>oldProbability</i>+<i>warmupGamma</i>
     */
    private double warmupGamma = 0.001;

    /**
     * Clusters that have had a "chance of being chosen" less than the average
     * for more than nerfBarrier% of the time in a segment will be surely
     * punished to make them less available in the following segment.
     *
     * <br>This will also impact on the local search: clusters that have a
     * probability of selection below the average won't be included in those
     * available for the local search.
     *
     * <br>Must be a double in range [0,1].
     */
    private double nerfBarrier = 0.55;

    /**
     * Maximum runtime for the ALNS heuristic algorithm (in seconds)
     */
    private long timeLimitALNS = 1800;

    /**
     * Maximum runtime for the local search process (in seconds)
     */
    private long timeLimitLocalSearch = 120;

    /**
     * A scaling factor that's applied to the weight of the best heuristics at
     * the beginning of every segment
     */
    private double rewardForBestSegmentHeuristics = 1.3;

    /**
     * A scaling factor that's applied to the weight of the worst heuristics at
     * the beginning of every segment
     */
    private double punishmentForWorstSegmentHeuristics = 0.7;

    /**
     * This constant holds the possible values of psi, the function that prizes
     * good heuristics and penalizes the bad ones.
     * <br>The default value for it is:
     * <br><tt>{3.0, 2.0, 1.0, 0.1}</tt>
     */
    public final static double[] DEFAULT_HEURISTIC_SCORES = {3.0, 2.0, 1.0, 0.0};

    /**
     * This constant holds the number of possible output values for function
     * psi, the one that gives a score for every heuristic. It's used for
     * debugging.
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
     * Determines how many ALNS iterations without improvement should be
     * accepted before the algorithm moves on to a new segment. Ideally it
     * should be the same as the number of iterations per segment.
     */
    private int maxIterationsWithoutImprovement = 50;

    /**
     * Maximum size of the past history
     *
     * @return the maxHistorySize
     */
    public int getMaxHistorySize() {
        return maxHistorySize;
    }

    /**
     * Maximum size of the past history
     *
     * @param maxHistorySize the maxHistorySize to set
     */
    public void setMaxHistorySize(int maxHistorySize) {
        int oldMaxHistorySize = this.maxHistorySize;
        this.maxHistorySize = maxHistorySize;
        propertyChangeSupport.firePropertyChange(PROP_MAXHISTORYSIZE, oldMaxHistorySize, maxHistorySize);
    }

    /**
     * The starting value of q (degree of destruction)
     *
     * @return the qStart
     */
    public int getqStart() {
        return qStart;
    }

    /**
     * The starting value of q (degree of destruction)
     *
     * @param qStart the qStart to set
     */
    public void setqStart(int qStart) {
        int oldqStart = this.qStart;
        this.qStart = qStart;
        propertyChangeSupport.firePropertyChange(PROP_QSTART, oldqStart, qStart);
    }

    /**
     * The increment of q at the end of every segment
     *
     * @return the qDelta
     */
    public int getqDelta() {
        return qDelta;
    }

    /**
     * The increment of q at the end of every segment
     *
     * @param qDelta the qDelta to set
     */
    public void setqDelta(int qDelta) {
        int oldqDelta = this.qDelta;
        this.qDelta = qDelta;
        propertyChangeSupport.firePropertyChange(PROP_QDELTA, oldqDelta, qDelta);
    }

    /**
     * Number of iterations in an optimization segment
     *
     * @return the segmentSize
     */
    public int getSegmentSize() {
        return segmentSize;
    }

    /**
     * Number of iterations in an optimization segment
     *
     * @param segmentSize the segmentSize to set
     */
    public void setSegmentSize(int segmentSize) {
        int oldSegmentSize = this.segmentSize;
        this.segmentSize = segmentSize;
        propertyChangeSupport.firePropertyChange(PROP_SEGMENTSIZE, oldSegmentSize, segmentSize);
    }

    /**
     * Maximum number of segments in an ALNS run
     *
     * @return the maxSegments
     */
    public long getMaxSegments() {
        return maxSegments;
    }

    /**
     * Maximum number of segments in an ALNS run
     *
     * @param maxSegments the maxSegments to set
     */
    public void setMaxSegments(long maxSegments) {
        long oldMaxSegments = this.maxSegments;
        this.maxSegments = maxSegments;
        propertyChangeSupport.firePropertyChange(PROP_MAXSEGMENTS, oldMaxSegments, maxSegments);
    }

    /**
     * Number of segments without improvement before ALNS termination
     *
     * @return the maxSegmentsWithoutImprovement
     */
    public long getMaxSegmentsWithoutImprovement() {
        return maxSegmentsWithoutImprovement;
    }

    /**
     * Number of segments without improvement before ALNS termination
     *
     * @param maxSegmentsWithoutImprovement the maxSegmentsWithoutImprovement to
     * set
     */
    public void setMaxSegmentsWithoutImprovement(long maxSegmentsWithoutImprovement) {
        long oldMaxSegmentsWithoutImprovement = this.maxSegmentsWithoutImprovement;
        this.maxSegmentsWithoutImprovement = maxSegmentsWithoutImprovement;
        propertyChangeSupport.firePropertyChange(PROP_MAXSEGMENTSWITHOUTIMPROVEMENT, oldMaxSegmentsWithoutImprovement, maxSegmentsWithoutImprovement);
    }

    /**
     * Determines whether to use this heuristic.
     *
     * @return the useDestroyGreedyCostInsertion
     */
    public boolean isUseDestroyGreedyCostInsertion() {
        return useDestroyGreedyCostInsertion;
    }

    /**
     * Determines whether to use this heuristic.
     *
     * @param useDestroyGreedyCostInsertion the useDestroyGreedyCostInsertion to
     * set
     */
    public void setUseDestroyGreedyCostInsertion(boolean useDestroyGreedyCostInsertion) {
        boolean oldUseDestroyGreedyCostInsertion = this.useDestroyGreedyCostInsertion;
        this.useDestroyGreedyCostInsertion = useDestroyGreedyCostInsertion;
        propertyChangeSupport.firePropertyChange(PROP_USEDESTROYGREEDYCOSTINSERTION, oldUseDestroyGreedyCostInsertion, useDestroyGreedyCostInsertion);
    }

    /**
     * Determines whether to use this heuristic.
     *
     * @return the useDestroyGreedyBestInsertion
     */
    public boolean isUseDestroyGreedyBestInsertion() {
        return useDestroyGreedyBestInsertion;
    }

    /**
     * Determines whether to use this heuristic.
     *
     * @param useDestroyGreedyBestInsertion the useDestroyGreedyBestInsertion to
     * set
     */
    public void setUseDestroyGreedyBestInsertion(boolean useDestroyGreedyBestInsertion) {
        boolean oldUseDestroyGreedyBestInsertion = this.useDestroyGreedyBestInsertion;
        this.useDestroyGreedyBestInsertion = useDestroyGreedyBestInsertion;
        propertyChangeSupport.firePropertyChange(PROP_USEDESTROYGREEDYBESTINSERTION, oldUseDestroyGreedyBestInsertion, useDestroyGreedyBestInsertion);
    }

    /**
     * Determines whether to use this heuristic.
     *
     * @return the useDestroyGreedyProfitInsertion
     */
    public boolean isUseDestroyGreedyProfitInsertion() {
        return useDestroyGreedyProfitInsertion;
    }

    /**
     * Determines whether to use this heuristic.
     *
     * @param useDestroyGreedyProfitInsertion the
     * useDestroyGreedyProfitInsertion to set
     */
    public void setUseDestroyGreedyProfitInsertion(boolean useDestroyGreedyProfitInsertion) {
        boolean oldUseDestroyGreedyProfitInsertion = this.useDestroyGreedyProfitInsertion;
        this.useDestroyGreedyProfitInsertion = useDestroyGreedyProfitInsertion;
        propertyChangeSupport.firePropertyChange(PROP_USEDESTROYGREEDYPROFITINSERTION, oldUseDestroyGreedyProfitInsertion, useDestroyGreedyProfitInsertion);
    }

    /**
     * Determines whether to use this heuristic.
     *
     * @return the useDestroyRandomInsertion
     */
    public boolean isUseDestroyRandomInsertion() {
        return useDestroyRandomInsertion;
    }

    /**
     * Determines whether to use this heuristic.
     *
     * @param useDestroyRandomInsertion the useDestroyRandomInsertion to set
     */
    public void setUseDestroyRandomInsertion(boolean useDestroyRandomInsertion) {
        boolean oldUseDestroyRandomInsertion = this.useDestroyRandomInsertion;
        this.useDestroyRandomInsertion = useDestroyRandomInsertion;
        propertyChangeSupport.firePropertyChange(PROP_USEDESTROYRANDOMINSERTION, oldUseDestroyRandomInsertion, useDestroyRandomInsertion);
    }

    /**
     * Determines whether to use this heuristic.
     *
     * @return the useRepairHighCostRemoval
     */
    public boolean isUseRepairHighCostRemoval() {
        return useRepairHighCostRemoval;
    }

    /**
     * Determines whether to use this heuristic.
     *
     * @param useRepairHighCostRemoval the useRepairHighCostRemoval to set
     */
    public void setUseRepairHighCostRemoval(boolean useRepairHighCostRemoval) {
        boolean oldUseRepairHighCostRemoval = this.useRepairHighCostRemoval;
        this.useRepairHighCostRemoval = useRepairHighCostRemoval;
        propertyChangeSupport.firePropertyChange(PROP_USEREPAIRHIGHCOSTREMOVAL, oldUseRepairHighCostRemoval, useRepairHighCostRemoval);
    }

    /**
     * Determines whether to use this heuristic.
     *
     * @return the useRepairRandomRemoval
     */
    public boolean isUseRepairRandomRemoval() {
        return useRepairRandomRemoval;
    }

    /**
     * Determines whether to use this heuristic.
     *
     * @param useRepairRandomRemoval the useRepairRandomRemoval to set
     */
    public void setUseRepairRandomRemoval(boolean useRepairRandomRemoval) {
        boolean oldUseRepairRandomRemoval = this.useRepairRandomRemoval;
        this.useRepairRandomRemoval = useRepairRandomRemoval;
        propertyChangeSupport.firePropertyChange(PROP_USEREPAIRRANDOMREMOVAL, oldUseRepairRandomRemoval, useRepairRandomRemoval);
    }

    /**
     * Determines whether to use this heuristic.
     *
     * @return the useRepairTravelTime
     */
    public boolean isUseRepairTravelTime() {
        return useRepairTravelTime;
    }

    /**
     * Determines whether to use this heuristic.
     *
     * @param useRepairTravelTime the useRepairTravelTime to set
     */
    public void setUseRepairTravelTime(boolean useRepairTravelTime) {
        boolean oldUseRepairTravelTime = this.useRepairTravelTime;
        this.useRepairTravelTime = useRepairTravelTime;
        propertyChangeSupport.firePropertyChange(PROP_USEREPAIRTRAVELTIME, oldUseRepairTravelTime, useRepairTravelTime);
    }

    /**
     * Determines whether to use this heuristic.
     *
     * @return the useRepairVehicleTime
     */
    public boolean isUseRepairVehicleTime() {
        return useRepairVehicleTime;
    }

    /**
     * Determines whether to use this heuristic.
     *
     * @param useRepairVehicleTime the useRepairVehicleTime to set
     */
    public void setUseRepairVehicleTime(boolean useRepairVehicleTime) {
        boolean oldUseRepairVehicleTime = this.useRepairVehicleTime;
        this.useRepairVehicleTime = useRepairVehicleTime;
        propertyChangeSupport.firePropertyChange(PROP_USEREPAIRVEHICLETIME, oldUseRepairVehicleTime, useRepairVehicleTime);
    }

    /**
     * Determines whether to use this heuristic.
     *
     * @return the useRepairWorstRemoval
     */
    public boolean isUseRepairWorstRemoval() {
        return useRepairWorstRemoval;
    }

    /**
     * Determines whether to use this heuristic.
     *
     * @param useRepairWorstRemoval the useRepairWorstRemoval to set
     */
    public void setUseRepairWorstRemoval(boolean useRepairWorstRemoval) {
        boolean oldUseRepairWorstRemoval = this.useRepairWorstRemoval;
        this.useRepairWorstRemoval = useRepairWorstRemoval;
        propertyChangeSupport.firePropertyChange(PROP_USEREPAIRWORSTREMOVAL, oldUseRepairWorstRemoval, useRepairWorstRemoval);
    }

    /**
     * This is the decay parameter of the update process for heuristic method
     * weights.
     * <br>This value should be a double in the interval [0,1].
     * <br>Heuristic method weights are updated following the convex combination
     * <br> newWeight = lambda*oldWeight + (1-lambda)*psi
     * <br>where psi is a value that indicates the relative score to give to an
     * heuristic.
     *
     * @return the lambda
     */
    public double getLambda() {
        return lambda;
    }

    /**
     * This is the decay parameter of the update process for heuristic method
     * weights.
     * <br>This value should be a double in the interval [0,1].
     * <br>Heuristic method weights are updated following the convex combination
     * <br> newWeight = lambda*oldWeight + (1-lambda)*psi
     * <br>where psi is a value that indicates the relative score to give to an
     * heuristic.
     *
     * @param lambda the lambda to set
     */
    public void setLambda(double lambda) {
        double oldLambda = this.lambda;
        double newLambda;

        // Lambda setup - values out of range [0,1] clip to values close to range boundaries
        // we want the temperature to always decrease if not explicitly stated otherwise
        if (lambda <= 0) {
            newLambda = 0.0;
        } else if (lambda >= 1) {
            newLambda = 1;
        } else {
            newLambda = lambda;
        }

        this.lambda = lambda;
        propertyChangeSupport.firePropertyChange(PROP_LAMBDA, oldLambda, newLambda);
    }

    /**
     * This is the decay parameter of the update process for Temperature.
     * <br>This value should be a double in the interval [0,1].
     * <br>The temperature is updated at the end of every segment like
     * <br>newTemperature = alpha*oldTemperature
     * <br>so that a slowly decreasing temperature (alpha-&gt;1) will make
     * fluctuations in accepted solutions much stronger
     *
     * @return the alpha
     */
    public double getAlpha() {
        return alpha;
    }

    /**
     * This is the decay parameter of the update process for Temperature.
     * <br>This value should be a double in the interval [0,1].
     * <br>The temperature is updated at the end of every segment like
     * <br>newTemperature = alpha*oldTemperature
     * <br>so that a slowly decreasing temperature (alpha-&gt;1) will make
     * fluctuations in accepted solutions much stronger
     *
     * @param alpha the alpha to set
     */
    public void setAlpha(double alpha) {
        double oldAlpha = this.alpha;
        double newAlpha;
        // Alpha setup - values out of range [0,1] clip to values close to range boundaries
        // we want the temperature to always decrease if not explicitly stated otherwise
        if (alpha <= 0) {
            newAlpha = 0.0;
        } else if (alpha >= 1) {
            newAlpha = 1;
        } else {
            newAlpha = alpha;
        }

        this.alpha = newAlpha;
        propertyChangeSupport.firePropertyChange(PROP_ALPHA, oldAlpha, newAlpha);
    }

    /**
     * This parameter is the scaling factor to change the chance of bad clusters
     * being chosen for insertion heuristics.
     * <br>Must be a double in range [0,1].
     * <br>Probability for bad clusters is downscaled like
     * <br><i>newProbability</i> =
     * <i>punishmentGamma</i>*<i>oldProbability</i>
     *
     * @return the punishmentGamma
     */
    public double getPunishmentGamma() {
        return punishmentGamma;
    }

    /**
     * This parameter is the scaling factor to change the chance of bad clusters
     * being chosen for insertion heuristics.
     * <br>Must be a double in range [0,1].
     * <br>Probability for bad clusters is downscaled like
     * <br><i>newProbability</i> =
     * <i>punishmentGamma</i>*<i>oldProbability</i>
     *
     * @param punishmentGamma the punishmentGamma to set
     */
    public void setPunishmentGamma(double punishmentGamma) {
        double oldGamma = this.punishmentGamma;
        double newGamma;

        // setup - values out of range [0,1] clip to values close to range boundaries
        // we want the temperature to always decrease if not explicitly stated otherwise
        if (punishmentGamma <= 0) {
            newGamma = 0.0;
        } else if (punishmentGamma >= 1) {
            newGamma = 1;
        } else {
            newGamma = punishmentGamma;
        }

        this.punishmentGamma = newGamma;
        propertyChangeSupport.firePropertyChange(PROP_PUNISHMENTGAMMA, oldGamma, newGamma);
    }

    /**
     * This parameter is the scaling factor used in the cooldown process.
     * <br>Must be a small double in range [0,1].
     * <br>A hot (freshly selected) cluster will have a new probability of being
     * chosen which is cooldownGamma times smaller
     *
     * <br>Probability for hot clusters is downscaled like
     * <br><i>newProbability</i> = (1 -
     * <i>cooldownGamma</i>)*<i>oldProbability</i>
     *
     * @return the cooldownGamma
     */
    public double getCooldownGamma() {
        return cooldownGamma;
    }

    /**
     * This parameter is the scaling factor used in the cooldown process.
     * <br>Must be a small double in range [0,1].
     * <br>A hot (freshly selected) cluster will have a new probability of being
     * chosen which is cooldownGamma times smaller
     *
     * <br>Probability for hot clusters is downscaled like
     * <br><i>newProbability</i> = (1 -
     * <i>cooldownGamma</i>)*<i>oldProbability</i>
     *
     * @param cooldownGamma the cooldownGamma to set
     */
    public void setCooldownGamma(double cooldownGamma) {
        double oldCooldownGamma = this.cooldownGamma;
        double newGamma;

        // setup - values out of range [0,1] clip to values close to range boundaries
        // we want the temperature to always decrease if not explicitly stated otherwise
        if (cooldownGamma <= 0) {
            newGamma = 0.0;
        } else if (cooldownGamma >= 1) {
            newGamma = 1;
        } else {
            newGamma = cooldownGamma;
        }

        this.cooldownGamma = newGamma;
        propertyChangeSupport.firePropertyChange(PROP_COOLDOWNGAMMA, oldCooldownGamma, newGamma);
    }

    /**
     * This parameter is the scaling factor used in the warmup process.
     * <br>Must be a small double in range [0,1].
     * <br>A cold (not freshly selected) cluster will have a new probability of
     * being chosen which is warmupGamma times bigger
     *
     * <br>Probability for cold clusters is upscaled like
     * <br><i>newProbability</i> = (1 -
     * <i>warmupGamma</i>)*<i>oldProbability</i>+<i>warmupGamma</i>
     *
     * @return the warmupGamma
     */
    public double getWarmupGamma() {
        return warmupGamma;
    }

    /**
     * This parameter is the scaling factor used in the warmup process.
     * <br>Must be a small double in range [0,1].
     * <br>A cold (not freshly selected) cluster will have a new probability of
     * being chosen which is warmupGamma times bigger
     *
     * <br>Probability for cold clusters is upscaled like
     * <br><i>newProbability</i> = (1 -
     * <i>warmupGamma</i>)*<i>oldProbability</i>+<i>warmupGamma</i>
     *
     * @param warmupGamma the warmupGamma to set
     */
    public void setWarmupGamma(double warmupGamma) {
        double oldWarmupGamma = this.warmupGamma;
        double newGamma;

        // setup - values out of range [0,1] clip to values close to range boundaries
        // we want the temperature to always decrease if not explicitly stated otherwise
        if (warmupGamma <= 0) {
            newGamma = 0.0;
        } else if (warmupGamma >= 1) {
            newGamma = 1;
        } else {
            newGamma = warmupGamma;
        }

        this.warmupGamma = warmupGamma;
        propertyChangeSupport.firePropertyChange(PROP_WARMUPGAMMA, oldWarmupGamma, newGamma);
    }

    /**
     * Clusters that have had a "chance of being chosen" less than the average
     * for more than nerfBarrier% of the time in a segment will be surely
     * punished to make them less available in the following segment.
     *
     * <br>This will also impact on the local search: clusters that have a
     * probability of selection below the average won't be included in those
     * available for the local search.
     *
     * <br>Must be a double in range [0,1].
     *
     * @return the nerfBarrier
     */
    public double getNerfBarrier() {
        return nerfBarrier;
    }

    /**
     * Clusters that have had a "chance of being chosen" less than the average
     * for more than nerfBarrier% of the time in a segment will be surely
     * punished to make them less available in the following segment.
     *
     * <br>This will also impact on the local search: clusters that have a
     * probability of selection below the average won't be included in those
     * available for the local search.
     *
     * <br>Must be a double in range [0,1].
     *
     * @param nerfBarrier the nerfBarrier to set
     */
    public void setNerfBarrier(double nerfBarrier) {
        double oldNerfBarrier = this.nerfBarrier;
        this.nerfBarrier = nerfBarrier;
        propertyChangeSupport.firePropertyChange(PROP_NERFBARRIER, oldNerfBarrier, nerfBarrier);
    }

    /**
     * Maximum runtime for the ALNS heuristic algorithm (in seconds)
     *
     * @return the timeLimitALNS
     */
    public long getTimeLimitALNS() {
        return timeLimitALNS;
    }

    /**
     * Maximum runtime for the ALNS heuristic algorithm (in seconds)
     *
     * @param timeLimitALNS the timeLimitALNS to set
     */
    public void setTimeLimitALNS(long timeLimitALNS) {
        long oldTimeLimitALNS = this.timeLimitALNS;
        this.timeLimitALNS = timeLimitALNS;
        propertyChangeSupport.firePropertyChange(PROP_TIMELIMITALNS, oldTimeLimitALNS, timeLimitALNS);
    }

    /**
     * Maximum runtime for the local search process (in seconds)
     *
     * @return the timeLimitLocalSearch
     */
    public long getTimeLimitLocalSearch() {
        return timeLimitLocalSearch;
    }

    /**
     * Maximum runtime for the local search process (in seconds)
     *
     * @param timeLimitLocalSearch the timeLimitLocalSearch to set
     */
    public void setTimeLimitLocalSearch(long timeLimitLocalSearch) {
        long oldTimeLimitLocalSearch = this.timeLimitLocalSearch;
        this.timeLimitLocalSearch = timeLimitLocalSearch;
        propertyChangeSupport.firePropertyChange(PROP_TIMELIMITLOCALSEARCH, oldTimeLimitLocalSearch, timeLimitLocalSearch);
    }

    /**
     * A scaling factor that's applied to the weight of the best heuristics at
     * the beginning of every segment
     *
     * @return the rewardForBestSegmentHeuristics
     */
    public double getRewardForBestSegmentHeuristics() {
        return rewardForBestSegmentHeuristics;
    }

    /**
     * A scaling factor that's applied to the weight of the best heuristics at
     * the beginning of every segment
     *
     * @param rewardForBestSegmentHeuristics the rewardForBestSegmentHeuristics
     * to set
     */
    public void setRewardForBestSegmentHeuristics(double rewardForBestSegmentHeuristics) {
        double oldRewardForBestSegmentHeuristics = this.rewardForBestSegmentHeuristics;
        this.rewardForBestSegmentHeuristics = rewardForBestSegmentHeuristics;
        propertyChangeSupport.firePropertyChange(PROP_REWARDFORBESTSEGMENTHEURISTICS, oldRewardForBestSegmentHeuristics, rewardForBestSegmentHeuristics);
    }

    /**
     * A scaling factor that's applied to the weight of the worst heuristics at
     * the beginning of every segment
     *
     * @return the punishmentForWorstSegmentHeuristics
     */
    public double getPunishmentForWorstSegmentHeuristics() {
        return punishmentForWorstSegmentHeuristics;
    }

    /**
     * A scaling factor that's applied to the weight of the worst heuristics at
     * the beginning of every segment
     *
     * @param punishmentForWorstSegmentHeuristics the
     * punishmentForWorstSegmentHeuristics to set
     */
    public void setPunishmentForWorstSegmentHeuristics(double punishmentForWorstSegmentHeuristics) {
        double oldPunishmentForWorstSegmentHeuristics = this.punishmentForWorstSegmentHeuristics;
        this.punishmentForWorstSegmentHeuristics = punishmentForWorstSegmentHeuristics;
        propertyChangeSupport.firePropertyChange(PROP_PUNISHMENTFORWORSTSEGMENTHEURISTICS, oldPunishmentForWorstSegmentHeuristics, punishmentForWorstSegmentHeuristics);
    }

    /**
     * This parameters holds the values of psi, the function that prizes good
     * heuristics and penalizes the bad ones.
     *
     * @return the heuristicScores
     */
    public double[] getHeuristicScores() {
        return heuristicScores;
    }

    /**
     * This parameters holds the values of psi, the function that prizes good
     * heuristics and penalizes the bad ones.
     *
     * @param heuristicScores the heuristicScores to set
     */
    public void setHeuristicScores(double[] heuristicScores) {
        double[] oldHeuristicScores = this.heuristicScores;
        this.heuristicScores = heuristicScores;
        propertyChangeSupport.firePropertyChange(PROP_HEURISTICSCORES, oldHeuristicScores, heuristicScores);
    }

    /**
     * Determines the maximum number of mips nodes to check before giving up a
     * feasibility check.
     *
     * @return the maxMIPSNodesForFeasibilityCheck
     */
    public double getMaxMIPSNodesForFeasibilityCheck() {
        return maxMIPSNodesForFeasibilityCheck;
    }

    /**
     * Determines the maximum number of mips nodes to check before giving up a
     * feasibility check.
     *
     * @param maxMIPSNodesForFeasibilityCheck the
     * maxMIPSNodesForFeasibilityCheck to set
     */
    public void setMaxMIPSNodesForFeasibilityCheck(double maxMIPSNodesForFeasibilityCheck) {
        double oldMaxMIPSNodesForFeasibilityCheck = this.maxMIPSNodesForFeasibilityCheck;
        this.maxMIPSNodesForFeasibilityCheck = maxMIPSNodesForFeasibilityCheck;
        propertyChangeSupport.firePropertyChange(PROP_MAXMIPSNODESFORFEASIBILITYCHECK, oldMaxMIPSNodesForFeasibilityCheck, maxMIPSNodesForFeasibilityCheck);
    }

    /**
     * Determines how many ALNS iterations without improvement should be
     * accepted before the algorithm moves on to a new segment. Ideally it
     * should be the same as the number of iterations per segment.
     *
     * @return the maxIterationsWithoutImprovement
     */
    public int getMaxIterationsWithoutImprovement() {
        return maxIterationsWithoutImprovement;
    }

    /**
     * Determines how many ALNS iterations without improvement should be
     * accepted before the algorithm moves on to a new segment. Ideally it
     * should be the same as the number of iterations per segment.
     *
     * @param maxIterationsWithoutImprovement the
     * maxIterationsWithoutImprovement to set
     */
    public void setMaxIterationsWithoutImprovement(int maxIterationsWithoutImprovement) {
        int oldMaxIterationsWithoutImprovement = this.maxIterationsWithoutImprovement;
        this.maxIterationsWithoutImprovement = maxIterationsWithoutImprovement;
        propertyChangeSupport.firePropertyChange(PROP_MAXITERATIONSWITHOUTIMPROVEMENT, oldMaxIterationsWithoutImprovement, maxIterationsWithoutImprovement);
    }

    private final transient PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);
    public static final String PROP_MAXHISTORYSIZE = "maxHistorySize";
    public static final String PROP_QSTART = "qStart";
    public static final String PROP_QDELTA = "qDelta";
    public static final String PROP_SEGMENTSIZE = "segmentSize";
    public static final String PROP_MAXSEGMENTS = "maxSegments";
    public static final String PROP_MAXSEGMENTSWITHOUTIMPROVEMENT = "maxSegmentsWithoutImprovement";
    public static final String PROP_USEDESTROYCLOSETOBARYCENTER = "useDestroyCloseToBarycenter";
    public static final String PROP_USEDESTROYGREEDYCOSTINSERTION = "useDestroyGreedyCostInsertion";
    public static final String PROP_USEDESTROYGREEDYBESTINSERTION = "useDestroyGreedyBestInsertion";
    public static final String PROP_USEDESTROYGREEDYPROFITINSERTION = "useDestroyGreedyProfitInsertion";
    public static final String PROP_USEDESTROYRANDOMINSERTION = "useDestroyRandomInsertion";
    public static final String PROP_USEREPAIRHIGHCOSTREMOVAL = "useRepairHighCostRemoval";
    public static final String PROP_USEREPAIRRANDOMREMOVAL = "useRepairRandomRemoval";
    public static final String PROP_USEREPAIRTRAVELTIME = "useRepairTravelTime";
    public static final String PROP_USEREPAIRVEHICLETIME = "useRepairVehicleTime";
    public static final String PROP_USEREPAIRWORSTREMOVAL = "useRepairWorstRemoval";
    public static final String PROP_LAMBDA = "lambda";
    public static final String PROP_ALPHA = "alpha";
    public static final String PROP_PUNISHMENTGAMMA = "punishmentGamma";
    public static final String PROP_COOLDOWNGAMMA = "cooldownGamma";
    public static final String PROP_WARMUPGAMMA = "warmupGamma";
    public static final String PROP_NERFBARRIER = "nerfBarrier";
    public static final String PROP_TIMELIMITALNS = "timeLimitALNS";
    public static final String PROP_TIMELIMITLOCALSEARCH = "timeLimitLocalSearch";
    public static final String PROP_REWARDFORBESTSEGMENTHEURISTICS = "rewardForBestSegmentHeuristics";
    public static final String PROP_PUNISHMENTFORWORSTSEGMENTHEURISTICS = "punishmentForWorstSegmentHeuristics";
    public static final String PROP_HEURISTICSCORES = "heuristicScores";
    public static final String PROP_MAXMIPSNODESFORFEASIBILITYCHECK = "maxMIPSNodesForFeasibilityCheck";
    public static final String PROP_MAXITERATIONSWITHOUTIMPROVEMENT = "maxIterationsWithoutImprovement";

    /**
     * Determines whether to use this heuristic.
     * @return the useDestroyCloseToBarycenter
     */
    public boolean isUseDestroyCloseToBarycenter() {
        return useDestroyCloseToBarycenter;
    }

    /**
     * Determines whether to use this heuristic.
     * @param useDestroyCloseToBarycenter the useDestroyCloseToBarycenter to set
     */
    public void setUseDestroyCloseToBarycenter(boolean useDestroyCloseToBarycenter) {
        boolean oldUseDestroyCloseToBarycenter = this.useDestroyCloseToBarycenter;
        this.useDestroyCloseToBarycenter = useDestroyCloseToBarycenter;
        propertyChangeSupport.firePropertyChange(PROP_USEDESTROYCLOSETOBARYCENTER, oldUseDestroyCloseToBarycenter, useDestroyCloseToBarycenter);
    }
    
}
