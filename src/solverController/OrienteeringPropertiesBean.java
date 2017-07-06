/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverController;

import java.io.Serializable;

/**
 * Java Bean to hold all the general properties and parameters for the Orienteering solvers.
 * @author Frash
 */
public class OrienteeringPropertiesBean implements Serializable {
    
    /**
     * Path to the output folder.
     */
    private String outputFolderPath = System.getProperty("user.home");
    /**
     * Define the timeLimit for the MIPS solver in MIPS solver mode.
     */
    private double timeLimit = 600.0;
    /**
     * Defines the number of threads to be used by the MIPS solver.
     * A value of 0 indicates "Use as many as the number of cores".
     */
    private int numThreads = 0;
    /**
     * If true, all heuristic constraints are applied every time the MIP solver
     * is run.
     */
    private boolean forceHeuristicConstraints = false;
    
    /**
     * Empty constructor.
     */
    public OrienteeringPropertiesBean() {
    }
    
    /**
     * Cloning method: clones all attributes from given bean
     * 
     * @param opb the OrienteeringPropertiesBean to clone from
     */
    public void cloneFrom(OrienteeringPropertiesBean opb){
        this.setForceHeuristicConstraints(opb.isForceHeuristicConstraints());
        this.setNumThreads(opb.getNumThreads());
        this.setOutputFolderPath(opb.getOutputFolderPath());
        this.setTimeLimit(opb.getTimeLimit());
    }
    
    

    /**
     * Path to the output folder.
     * @return the outputFolderPath
     */
    public String getOutputFolderPath() {
        return outputFolderPath;
    }

    /**
     * Path to the output folder.
     * @param outputFolderPath the outputFolderPath to set
     */
    public void setOutputFolderPath(String outputFolderPath) {
        this.outputFolderPath = outputFolderPath;
    }

    /**
     * Define the timeLimit for the MIPS solver in MIPS solver mode.
     * @return the timeLimit
     */
    public double getTimeLimit() {
        return timeLimit;
    }

    /**
     * Define the timeLimit for the MIPS solver in MIPS solver mode.
     * @param timeLimit the timeLimit to set
     */
    public void setTimeLimit(double timeLimit) {
        this.timeLimit = timeLimit;
    }

    /**
     * Defines the number of threads to be used by the MIPS solver.
     * A value of 0 indicates "Use as many as the number of cores".
     * @return the numThreads
     */
    public int getNumThreads() {
        return numThreads;
    }

    /**
     * Defines the number of threads to be used by the MIPS solver.
     * A value of 0 indicates "Use as many as the number of cores".
     * @param numThreads the numThreads to set
     */
    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    /**
     * If true, all heuristic constraints are applied every time the MIP solver
     * is run.
     * @return the forceHeuristicConstraints
     */
    public boolean isForceHeuristicConstraints() {
        return forceHeuristicConstraints;
    }

    /**
     * If true, all heuristic constraints are applied every time the MIP solver
     * is run.
     * @param forceHeuristicConstraints the forceHeuristicConstraints to set
     */
    public void setForceHeuristicConstraints(boolean forceHeuristicConstraints) {
        this.forceHeuristicConstraints = forceHeuristicConstraints;
    }
}
