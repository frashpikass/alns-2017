/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverModel;

import java.util.List;

/**
 * Class to describe a vehicle and its properties for the orienteering problem.
 * @author Frash
 */
public class Vehicle {
    
    private int id;
    private List<Integer> skills;

    /**
     * Constructor for the class vehicle.
     * @param id the id of the vehicle
     * @param services the list of skills available
     */
    public Vehicle(int id, List<Integer> services) {
        this.id = id;
        this.skills = services;
    }
    
    /**
     * Get the value of skills
     *
     * @return the value of skills
     */
    public List<Integer> getSkills() {
        return skills;
    }


    /**
     * Get the value of id
     *
     * @return the value of id
     */
    public int getId() {
        return id;
    }
    
    /**
     * Returns true if this vehicle can fulfill the service required by the specified node.
     * @param n the node to check if it can be served by this vehicle
     * @return true if the node can be served by this vehicle
     */
    boolean canServe(Node n) {
        return this.skills.contains(n.getService());
    }
    
    /**
     * Returns true if this vehicle can fulfill at least one service required by the specified cluster.
     * @param c the cluster to check if it can be served by this vehicle
     * @return true if this vehicle can serve the cluster.
     */
    boolean canServe(Cluster c){
        boolean ret = false;
        
        for(int service : c.getServices()){
            if(this.skills.contains(service)){
                ret = true;
                break;
            } 
        }
        return ret;
    }

}
