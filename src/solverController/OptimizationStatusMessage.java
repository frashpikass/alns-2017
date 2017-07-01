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
     * Current progress (a double between [0,1])
     */
    private double progress;
    
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
     * Constructor.
     * @param instancePath Path to the instance currently being solved
     * @param progress Current progress (a double between [0,1])
     * @param elapsedTime Currently elapsed time
     */
    public OptimizationStatusMessage(
            String instancePath,
            double progress,
            double elapsedTime){
        this.instancePath = instancePath;
        this.progress = progress;
        this.elapsedTime = elapsedTime;
        this.instanceNumber = 0;
        this.batchSize = 1;
    }
    
    /**
     * Constructor.
     * @param instancePath Path to the instance currently being solved
     * @param progress Current progress (a double between [0,1])
     * @param elapsedTime Currently elapsed time
     */
    public OptimizationStatusMessage(
            String instancePath,
            double progress,
            double elapsedTime,
            int instanceNumber,
            int batchSize){
        this.instancePath = instancePath;
        this.progress = progress;
        this.elapsedTime = elapsedTime;
        this.instanceNumber = instanceNumber;
        this.batchSize = batchSize;
    }

    /**
     * Path to the instance currently being solved
     */
    public String getInstancePath() {
        return instancePath;
    }
    
    /**
     * Current progress (a double between [0,1])
     */
    public double getProgress() {
        return progress;
    }
    
    /**
     * Currently elapsed time
     */
    public double getElapsedTime() {
        return elapsedTime;
    }
    
    /**
     * Number of the instance in the batch
     */
    public int getInstanceNumber() {
        return instanceNumber;
    }

    /**
     * Size of the batch
     */
    public int getBatchSize() {
        return batchSize;
    }
    
    
    
    
    
}
