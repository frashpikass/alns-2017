/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.io.IOException;
import solverModel.ALNS;
import solverModel.Orienteering;

/**
 * A controller class to handle an orienteering problem solver
 * @author Frash
 */
public class controller {
    // Constants
    
    /**
     * Constant to define the default file extension for log files
     */
    private final static String LOG_FILE_EXTESION = ".log";
    
    /**
     * Constant value for the maximum time limit for the Gurobi solver
     */
    public static final double MAX_DEFAULT_TIMELIMIT = 1800.0;
    
    /**
     * Constant value for the maximum number of threads used by the Gurobi solver. 0 indicates "Use as many as the number of cores".
     */
    public static final int DEFAULT_NUMTHREADS = 0;
    
    // General Purpose Variables
    
    private String modelPath;
    private String instanceName;
    private final String outputFolderPath;
    private Orienteering o;
    private ALNS a;
    
    /**
     * If true, all heuristic constraints are applied every time the MIP solver
     * is run.
     */
    private boolean forceHeuristicConstraints;
    
    // ALNS specific variables
    
    /**
     * Number of iterations in an optimization segment
     */
    private int ALNSSegmentSize;
    /**
     * Maximum size of the past history
     */
    private int ALNSMaxHistorySize;
    /**
     * The starting value of q (degree of destruction)
     */
    private int ALNSQStart;
    /**
     * This is the decay parameter of the update process for heuristic method weights.
     * <br>This value should be a double in the interval [0,1].
     * <br>Heuristic method weights are updated following the convex combination
     * <br> newWeight = lambda*oldWeight + (1-lambda)*psi
     * <br>where psi is a value that indicates the relative score to give to an heuristic.
     */
    private double ALNSLambda;
    /**
     * This is the decay parameter of the update process for Temperature.
     * <br>This value should be a double in the interval [0,1].
     * <br>The temperature is updated at the end of every segment like
     * <br>newTemperature = alpha*Temperature
     * <br>so that a slowly decreasing temperature (alpha-&gt;1) will make fluctuations
     * in accepted solutions much stronger
     */
    private double ALNSAlpha;
    /**
     * Maximum runtime for the ALNS heuristic algorithm (in seconds)
     */
    private long ALNSTimeLimitALNS;
    /**
     * Maximum runtime for the local search process (in seconds)
     */
    private long ALNSTimeLimitLocalSearch;
    /**
     * A scaling factor that's applied to the weight of the best heuristics at the beginning of every segment
     */
    private double ALNSRewardForBestSegmentHeuristics;
    /**
     * A scaling factor that's applied to the weight of the worst heuristics at the beginning of every segment
     */
    private double ALNSPunishmentForWorstSegmentHeuristics;
    /**
     * Determines the maximum number of mips nodes to check before giving up a
     * feasibility check.
     */
    private double ALNSMaxMIPSNodesForFeasibilityCheck;
    
    /**
     * Determines how many ALNS iterations without improvement should be accepted
     * before the algorithm moves on to a new segment.
     * A good value could be a fraction of the segment size.
     */
    private int ALNSMaxIterationsWithoutImprovement;
    
    /**
     * This parameters holds the values of psi, the function that prizes good
     * heuristics and penalizes the bad ones.
     */
    private double[] ALNSHeuristicScores;
    
    /**
     * ALNS booleans that set whether to use an heuristic or not
     **/
    private boolean useDestroyGreedyCostInsertion;
    private boolean useDestroyGreedyBestInsertion;
    private boolean useDestroyGreedyProfitInsertion;
    private boolean useDestroyRandomInsertion;

    private boolean useRepairHighCostRemoval;
    private boolean useRepairRandomRemoval;
    private boolean useRepairTravelTime;
    private boolean useRepairVehicleTime;
    private boolean useRepairWorstRemoval;
    
    public controller(
            String outputFolderPath,
            String modelPath,
            double timeLimit,
            int numThreads,
            boolean forceHeuristicConstraints,
            
            int ALNSSegmentSize,
            int ALNSMaxHistorySize,
            int ALNSQStart,
            double ALNSLambda,
            double ALNSAlpha,
            long ALNSTimeLimitALNS,
            long ALNSTimeLimitLocalSearch,
            double ALNSRewardForBestSegmentHeuristics,
            double ALNSPunishmentForWorstSegmentHeuristics,
            double ALNSMaxMIPSNodesForFeasibilityCheck,
            int ALNSMaxIterationsWithoutImprovement,
            double[] ALNSHeuristicScores,
            
            boolean useDestroyGreedyCostInsertion,
            boolean useDestroyGreedyBestInsertion,
            boolean useDestroyGreedyProfitInsertion,
            boolean useDestroyRandomInsertion,
            
            boolean useRepairHighCostRemoval,
            boolean useRepairRandomRemoval,
            boolean useRepairTravelTime,
            boolean useRepairVehicleTime,
            boolean useRepairWorstRemoval
    ) throws Exception {
        this.forceHeuristicConstraints = forceHeuristicConstraints;
        this.modelPath = modelPath;
        this.outputFolderPath = outputFolderPath;
        
        this.instanceName = instanceNameFromPath(modelPath);
        boolean reload = true;
        
        o = new Orienteering(
                outputFolderPath,
                outputFolderPath+instanceName+LOG_FILE_EXTESION,
                modelPath,
                timeLimit,
                numThreads,
                reload
        );
        
        this.ALNSSegmentSize = ALNSSegmentSize;
        this.ALNSMaxHistorySize = ALNSMaxHistorySize;
        this.ALNSQStart = ALNSQStart;
        this.ALNSLambda = ALNSLambda;
        this.ALNSAlpha = ALNSAlpha;
        this.ALNSTimeLimitALNS = ALNSTimeLimitALNS;
        this.ALNSTimeLimitLocalSearch = ALNSTimeLimitLocalSearch;
        this.ALNSRewardForBestSegmentHeuristics = ALNSRewardForBestSegmentHeuristics;
        this.ALNSPunishmentForWorstSegmentHeuristics = ALNSPunishmentForWorstSegmentHeuristics;
        this.ALNSMaxMIPSNodesForFeasibilityCheck = ALNSMaxMIPSNodesForFeasibilityCheck;
        this.ALNSMaxIterationsWithoutImprovement = ALNSMaxIterationsWithoutImprovement;
        this.ALNSHeuristicScores = ALNSHeuristicScores;
        
        // Heuristic setup
        this.useDestroyGreedyCostInsertion = useDestroyGreedyCostInsertion;
        this.useDestroyGreedyBestInsertion = useDestroyGreedyBestInsertion;
        this.useDestroyGreedyProfitInsertion = useDestroyGreedyProfitInsertion;
        this.useDestroyRandomInsertion = useDestroyRandomInsertion;

        this.useRepairHighCostRemoval = useRepairHighCostRemoval;
        this.useRepairRandomRemoval = useRepairRandomRemoval;
        this.useRepairTravelTime = useRepairTravelTime;
        this.useRepairVehicleTime = useRepairVehicleTime;
        this.useRepairWorstRemoval = useRepairWorstRemoval;        
        
        a = new ALNS(
                o,
                ALNSSegmentSize,
                ALNSMaxHistorySize,
                ALNSQStart,
                ALNSLambda,
                ALNSAlpha, 
                ALNSTimeLimitALNS,
                ALNSTimeLimitLocalSearch,
                ALNSRewardForBestSegmentHeuristics,
                ALNSPunishmentForWorstSegmentHeuristics,
                ALNSMaxMIPSNodesForFeasibilityCheck,
                ALNSMaxIterationsWithoutImprovement,
                ALNSHeuristicScores,
                
                useDestroyGreedyCostInsertion,
                useDestroyGreedyBestInsertion,
                useDestroyGreedyProfitInsertion,
                useDestroyRandomInsertion,

                useRepairHighCostRemoval,
                useRepairRandomRemoval,
                useRepairTravelTime,
                useRepairVehicleTime,
                useRepairWorstRemoval
        );
    }
    
    /**
     * Gets the name of the instance from the given instance file path
     * @param path path to the instance file
     * @return the name of the instance file, without any extension
     */
    private static String instanceNameFromPath(String path){
        int firstIndexOfNameInPath;
        if((firstIndexOfNameInPath = path.lastIndexOf("//"))==-1){
            if((firstIndexOfNameInPath = path.lastIndexOf("\\"))==-1){
                firstIndexOfNameInPath=0;
            }
        }
                
        return path.substring(firstIndexOfNameInPath, path.lastIndexOf("."));
    }
    
    public void csvSaveParameters() throws IOException{
        CSVWriter logger = new CSVWriter(new FileWriter(outputFolderPath+this.instanceName+"_ALNSlog.csv"), '\t');
        String [] generalHeader = {"General parameters",""};
        
        logger.writeNext(generalHeader);
        logger.writeAll(o.csvGetGeneralParameters());
        
        String [] ALNSHeader = {"ALNS parameters",""};
        logger.writeNext(ALNSHeader);
        logger.writeAll(a.csvGetALNSParameters());
        
        logger.close();
    }
    
    public void csvLoadParameters(){
        
    }
    
    public static void main(String[] args) {
    }
    
    /**
     * Optimize the relaxation of the given instance.
     * @throws Exception if anything goes wrong
     */
    public void optimizeRelaxed() throws Exception{
        o.optimizeRelaxed(forceHeuristicConstraints);
    }
    
    /**
     * Optimize the given instance using Gurobi's MIPS solver.
     * @throws Exception  if anything goes wrong
     */
    public void optimizeMIP() throws Exception{
        o.optimizeMIP(forceHeuristicConstraints);
    }
    
    /**
     * Optimize the given instance using the ALNS algorithm.
     * @throws Exception if anything goes wrong
     */
    public void optimizeALNS() throws Exception{
        a.optimize();
    }
}
