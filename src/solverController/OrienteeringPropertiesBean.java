/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverController;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

/**
 * Java Bean to hold all the general properties and parameters for the Orienteering solvers.
 * @author Frash
 */
public class OrienteeringPropertiesBean implements Serializable {
    static final long serialVersionUID = 0;
    
    /**
     * Path to the output folder.
     */
    private String outputFolderPath = System.getProperty("user.home");
    /**
     * Define the timeLimit for the MIPS solver in MIPS solver mode.
     */
    private double timeLimit = 1800.0;
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
        String oldOutputFolderPath = this.outputFolderPath;
        this.outputFolderPath = outputFolderPath;
        this.propertyChangeSupport.firePropertyChange(PROP_OUTPUTFOLDERPATH, oldOutputFolderPath, outputFolderPath);
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
        double oldTimeLimit = this.timeLimit;
        this.timeLimit = timeLimit;
        this.propertyChangeSupport.firePropertyChange(PROP_TIMELIMIT, oldTimeLimit, timeLimit);
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
        int oldNumThreads = this.numThreads;
        this.numThreads = numThreads;
        this.propertyChangeSupport.firePropertyChange(PROP_NUMTHREADS, oldNumThreads, numThreads);
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
        boolean oldForceHeuristicConstraints = this.forceHeuristicConstraints;
        this.forceHeuristicConstraints = forceHeuristicConstraints;
        this.propertyChangeSupport.firePropertyChange(PROP_FORCEHEURISTICCONSTRAINTS, oldForceHeuristicConstraints, forceHeuristicConstraints);
    }
    
    public static final String PROP_FORCEHEURISTICCONSTRAINTS = "forceHeuristicConstraints";
    public static final String PROP_NUMTHREADS = "numThreads";
    public static final String PROP_OUTPUTFOLDERPATH = "outputFolderPath";
    public static final String PROP_TIMELIMIT = "timeLimit";
    
    // <editor-fold defaultstate="collapsed" desc="PropertyChange Stuff">
    /**
     * Property change support object.
     */
    private final transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    
    /**
     * Adds a PropertyChangeListener to start listening to events
     * @param listener the listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }
    
    /**
     * Removes a PropertyChangeListener to stop listening to events
     * @param listener the listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
    // </editor-fold>
}
