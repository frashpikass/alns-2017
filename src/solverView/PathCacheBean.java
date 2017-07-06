/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverView;

import java.beans.*;
import java.io.File;
import java.io.Serializable;

/**
 * A bean to use as a cache for the last path seleceted by user
 * @author Frash
 */
public class PathCacheBean implements Serializable {
    
    /**
     * Path to the last directory
     */
    private File pathToLastDirectory;

    public PathCacheBean() {
        this.pathToLastDirectory = new File(System.getProperty("user.home"));
    }

    /**
     * Path to the last directory
     * @return the pathToLastDirectory
     */
    public File getPathToLastDirectory() {
        return pathToLastDirectory;
    }

    /**
     * Path to the last directory
     * @param pathToLastDirectory the pathToLastDirectory to set
     */
    public void setPathToLastDirectory(File pathToLastDirectory) {
        java.io.File oldPathToLastDirectory = this.pathToLastDirectory;
        this.pathToLastDirectory = pathToLastDirectory;
        propertyChangeSupport.firePropertyChange(PROP_PATHTOLASTDIRECTORY, oldPathToLastDirectory, pathToLastDirectory);
    }
    private final transient PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);
    public static final String PROP_PATHTOLASTDIRECTORY = "pathToLastDirectory";
    
}
