/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverController;

/**
 * This class describes an optimization status message
 * @author Frash
 */
public class OptimizationStatusMessage {
    /**
     * Path to the instance currently being solved
     */
    private String instancePath;
    
    /**
     * Current progress (integer between 0 and 100)
     */
    private int progress;
    
    /**
     * Currently elapsed time
     */
    private double elapsedTime;
    
    /**
     * Current best objective value
     */
    private double bestObj;
    
    /**
     * Number of the instance in the batch
     */
    private int instanceNumber;
    
    /**
     * Size of the batch
     */
    private int batchSize;
    
    /**
     * Enum for optimization statuses
     */
    public enum Status {STARTING, RUNNING, STOPPING, STOPPED, DONE}
    
    /**
     * Optimization status
     */
    private Status status;
    
    /**
     * Constructor.
     * @param instancePath Path to the instance currently being solved
     * @param progress Current progress (an in between [0,100])
     * @param elapsedTime Currently elapsed time
     * @param status solver status
     * @param bestObj current value of the best objective
     */
    public OptimizationStatusMessage(
            String instancePath,
            int progress,
            double elapsedTime,
            Status status,
            double bestObj){
        this.instancePath = instancePath;
        this.progress = progress;
        this.elapsedTime = elapsedTime;
        this.instanceNumber = 0;
        this.batchSize = 1;
        this.status = status;
        this.bestObj = bestObj;
    }
    
    /**
     * Constructor which keeps track of batches.
     * @param instancePath Path to the instance currently being solved
     * @param progress Current progress (an int between [0,100])
     * @param elapsedTime Currently elapsed time
     * @param instanceNumber number of the instance in the batch
     * @param batchSize size of the batch
     * @param status solver status
     * @param bestObj current value of the best objective
     */
    public OptimizationStatusMessage(
            String instancePath,
            int progress,
            double elapsedTime,
            int instanceNumber,
            int batchSize,
            Status status,
            double bestObj){
        this.instancePath = instancePath;
        this.progress = progress;
        this.elapsedTime = elapsedTime;
        this.instanceNumber = instanceNumber;
        this.batchSize = batchSize;
        this.status = status;
        this.bestObj = bestObj;
    }

    /**
     * Path to the instance currently being solved
     * @return Path to the instance currently being solved
     */
    public String getInstancePath() {
        return instancePath;
    }
    
    /**
     * Current progress (an int between [0,100])
     * @return current progress
     */
    public int getProgress() {
        return progress;
    }
    
    /**
     * Currently elapsed time
     * @return Currently elapsed time
     */
    public double getElapsedTime() {
        return elapsedTime;
    }
    
    /**
     * Number of the instance in the batch
     * @return Number of the instance in the batch
     */
    public int getInstanceNumber() {
        return instanceNumber;
    }

    /**
     * Size of the batch
     * @return Size of the batch
     */
    public int getBatchSize() {
        return batchSize;
    }
    
    /**
     * Current best objective value
     * @return the bestObj
     */
    public double getBestObj() {
        return bestObj;
    }

    /**
     * Optimization status
     * @return optimization statis
     */
    public Status getStatus() {
        return status;
    }
    
    public String toString(){
        return elapsedTime
                + "s: Current Instance '"
                + instancePath
                + "' @ "
                + progress
                + "%, batch "
                + instanceNumber+"/"+batchSize
                + " done, status: "
                + status
                + ", BestObj:"
                + bestObj;
    }
}
