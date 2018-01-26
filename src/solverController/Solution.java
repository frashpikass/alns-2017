/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverController;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class describes a solution in a human-readable form and allows to
 * easily update it and print it.
 * @author Frash
 */
public class Solution {
    /**
     * List of vehicle paths in a ordered, human-readable form.
     */
    private List<List<Integer>> vehiclePaths;
    
    /**
     * List of visited clusters in a ordered, human-readable form.
     */
    private List<String> visitedClusters;
    
    /**
     * Objective function value of this solution.
     */
    private double objectiveValue;
    
    /**
     * Path to the instance this solution refers to
     */
    private String instancePath;
    
    /**
     * Name of the solver used for this solution
     */
    private String solverName;
    
    /**
     * Timestamp of when this solution was last updated
     */
    private Timestamp timestamp;
    
    /**
     * Constructor for the class Solution.
     * @param solverName Name of the solver used to generate this solution
     * @param instancePath Path to the instance this solution refers to
     */
    public Solution(String solverName, String instancePath){
        this.solverName = solverName;
        this.instancePath = instancePath;
        this.timestamp = new Timestamp(System.currentTimeMillis());
        this.vehiclePaths = new ArrayList<>();
        this.visitedClusters = new ArrayList<>();
    }
    
    /**
     * Update the solution with the new results.
     * @param vehiclePaths new list of vehicle paths in a ordered, human-readable form
     * @param visitedClusters new list of visited clusters in a ordered, human-readable form
     * @param objectiveValue new objective function value of this solution
     */
    public void update(
            List<List<Integer>> vehiclePaths,
            List<String> visitedClusters,
            double objectiveValue
    ){
        this.vehiclePaths = vehiclePaths;
        this.visitedClusters = visitedClusters;
        this.objectiveValue = objectiveValue;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }
    
    /**
     * Update the solution with the new results. Use this method to store the
     * solution of a relaxed model.
     * @param objectiveValue new objective function value of this solution
     */
    public void update(
            double objectiveValue
    ){
        this.objectiveValue = objectiveValue;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }
    
    @Override
    public String toString(){
        StringBuffer output = new StringBuffer();
        
        output.append("Solution report.\n\n");
        output.append("Instance: "+instancePath+"\n");
        output.append("Solver: "+solverName+"\n");
        output.append("Timestamp: "+timestamp.toString()+"\n");
        output.append("Objective function value: "+objectiveValue+"\n");
        
        output.append("\nList of visited clusters (a * indicates that the node has been visited):");
        for(String s : visitedClusters){
            output.append("\n"+s);
        }
        output.append("\nEnd of the list. Visited clusters: " + visitedClusters.size() + "\n");
        
        output.append("\nVehicle paths in solution: ");
        for (int v = 0; v < vehiclePaths.size(); v++) {
            output.append("\nv" + v + ": " + vehiclePaths.get(v).toString() + "\n");
        }
        
        return output.toString();
    }

    /**
     * List of vehicle paths in a ordered, human-readable form.
     * @return the vehiclePaths
     */
    public List<List<Integer>> getVehiclePaths() {
        return vehiclePaths;
    }

    /**
     * List of visited clusters in a ordered, human-readable form.
     * @return the visitedClusters
     */
    public List<String> getVisitedClusters() {
        return visitedClusters;
    }

    /**
     * Objective function value of this solution.
     * @return the objectiveValue
     */
    public double getObjectiveValue() {
        return objectiveValue;
    }
    
    /**
     * Path to the instance this solution refers to
     * @return the instancePath
     */
    public String getInstancePath() {
        return instancePath;
    }

    /**
     * Name of the solver used for this solution
     * @return the solverName
     */
    public String getSolverName() {
        return solverName;
    }

    /**
     * Timestamp of when this solution was last updated
     * @return the timestamp
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }
    
    /**
     * Saves this solution to a human readable text file.
     * @param path path to the destination text file
     * @throws IOException if there were problems while writing the solution to file
     */
    public void saveToTextFile(String path) throws IOException{
        try (FileWriter fw = new FileWriter(path))
        {
            fw.write(this.toString());
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(Solution.class.getName()).log(Level.SEVERE, "Problem while trying to write solution output to "+path, ex);
            throw ex;
        }
    }
}
