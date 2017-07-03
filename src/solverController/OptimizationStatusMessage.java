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
     */
    public OptimizationStatusMessage(
            String instancePath,
            int progress,
            double elapsedTime,
            Status status){
        this.instancePath = instancePath;
        this.progress = progress;
        this.elapsedTime = elapsedTime;
        this.instanceNumber = 0;
        this.batchSize = 1;
        this.status = status;
    }
    
    /**
     * Constructor.
     * @param instancePath Path to the instance currently being solved
     * @param progress Current progress (an int between [0,100])
     * @param elapsedTime Currently elapsed time
     * @param instanceNumber number of the instance in the batch
     * @param batchSize size of the batch
     */
    public OptimizationStatusMessage(
            String instancePath,
            int progress,
            double elapsedTime,
            int instanceNumber,
            int batchSize,
            Status status){
        this.instancePath = instancePath;
        this.progress = progress;
        this.elapsedTime = elapsedTime;
        this.instanceNumber = instanceNumber;
        this.batchSize = batchSize;
        this.status = status;
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
     * Optimization status
     * @return optimization statis
     */
    public Status getStatus() {
        return status;
    }
    
    public String toString(){
        return elapsedTime+"s: Current Instance '"+instancePath+"' @ "+progress+"%, batch "+instanceNumber+"/"+batchSize+" done, status: "+status;
    }
}
