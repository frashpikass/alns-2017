/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to describe a cluster
 * @author Frash
 */
public class Cluster {
    
    private double x;
    private double y;
    private List<Node> nodes;
    private double profit;
    private int id;
    
    private List<Vehicle> instanceVehicles;
    private int maxNumberOfVehiclesNeeded;
    private int minNumberOfVehiclesNeeded;
    private double weightedProfit;
    
    /**
     * Comparator to sort clusters by weighted profit.
     */
    public final static Comparator<Cluster> WEIGHTED_PROFIT_COMPARATOR = new Comparator<Cluster>() {
        @Override
        public int compare(Cluster o1, Cluster o2) {
            int ret = 0;
            try {
                if(o1.getWeightedProfit() < o2.getWeightedProfit()){
                    ret=-1;
                }
                else if(o1.getWeightedProfit() > o2.getWeightedProfit()){
                    ret=+1;
                }
            } catch (Exception ex) {
                Logger.getLogger(Cluster.class.getName()).log(Level.SEVERE, null, ex);
            }
            return ret;
        }
    };
    
    /**
     * Comparator to sort clusters by profit.
     */
    public final static Comparator<Cluster> PROFIT_COMPARATOR = new Comparator<Cluster>() {
        @Override
        public int compare(Cluster o1, Cluster o2) {
            int ret = 0;
            try {
                if(o1.getProfit() < o2.getProfit()){
                    ret=-1;
                }
                else if(o1.getProfit() > o2.getProfit()){
                    ret=+1;
                }
            } catch (Exception ex) {
                Logger.getLogger(Cluster.class.getName()).log(Level.SEVERE, null, ex);
            }
            return ret;
        }
    };
    
    /**
     * Comparator to sort clusters by cost.
     */
    public final static Comparator<Cluster> COST_COMPARATOR = new Comparator<Cluster>() {
        @Override
        public int compare(Cluster o1, Cluster o2) {
            int ret = 0;
            try {
                if(o1.getTotalCost() < o2.getTotalCost()){
                    ret=-1;
                }
                else if(o1.getTotalCost() > o2.getTotalCost()){
                    ret=+1;
                }
            } catch (Exception ex) {
                Logger.getLogger(Cluster.class.getName()).log(Level.SEVERE, null, ex);
            }
            return ret;
        }
    };
    
    /**
     * Comparator to sort clusters by the profit/cost ratio.
     */
    public final static Comparator<Cluster> PROFIT_COST_RATIO_COMPARATOR = new Comparator<Cluster>() {
        @Override
        public int compare(Cluster o1, Cluster o2) {
            int ret = 0;
            try {
                if(o1.profit/(1+o1.getTotalCost()) < o2.profit/(1+o2.getTotalCost())){
                    ret=-1;
                }
                else if(o1.profit/(1+o1.getTotalCost()) > o2.profit/(1+o2.getTotalCost())){
                    ret=+1;
                }
            } catch (Exception ex) {
                Logger.getLogger(Cluster.class.getName()).log(Level.SEVERE, null, ex);
            }
            return ret;
        }
    };
    
    /**
     * Comparator to sort clusters by minimum number of vehicles needed.
     */
    public final static Comparator<Cluster> MIN_VEHICLES_COMPARATOR = new Comparator<Cluster>(){
        @Override
        public int compare(Cluster o1, Cluster o2) {
            return o1.getMinNumberOfVehiclesNeeded()-o2.getMinNumberOfVehiclesNeeded();
        }
    };
    
    /**
     * Comparator to sort clusters by maximum number of vehicles needed.
     */
    public final static Comparator<Cluster> MAX_VEHICLES_COMPARATOR = new Comparator<Cluster>(){
        @Override
        public int compare(Cluster o1, Cluster o2) {
            return o1.getMaxNumberOfVehiclesNeeded()-o2.getMaxNumberOfVehiclesNeeded();
        }
    };
    
    /**
     * Comparator to sort clusters by node number.
     */
    public final static Comparator<Cluster> NODE_NUMBER_COMPARATOR = new Comparator<Cluster>(){
        @Override
        public int compare(Cluster o1, Cluster o2) {
            return o1.getNumNodes()-o2.getNumNodes();
        }
    };
    
    
    /**
     * Constructor for class Cluster
     * @param id the ID of the cluster
     * @param nodes the list of nodes of the cluster
     * @param profit the profit value for this cluster
     * @throws Exception if the nodes in the cluster's node list don't have the same coordinates
     */
    public Cluster(int id, List<Node> nodes, double profit) throws Exception {
        this.id = id;
        this.nodes = nodes;
        this.setCoordinatesFromNodes();
        this.profit = profit;
        
        // This is just the initial value: the getters are set up to calculate these
        // as soon as they are required, since they're pretty expensive to compute
        // Also all of these will need instanceVehicles to be initialized
        instanceVehicles = null;
        this.maxNumberOfVehiclesNeeded=-1;
        this.minNumberOfVehiclesNeeded=-1;
        this.weightedProfit = -1;
    }
    
    /**
     * Sets the cluster coordinates from the nodes list in the cluster
     * @throws Exception if the nodes in the cluster's node list don't have the same coordinates
     */
    private void setCoordinatesFromNodes() throws Exception{
        double x;
        double y;
        
        Node prev = this.nodes.get(0);
        for(Node n : this.nodes){
            if(n.compareTo(prev)!=0)
                throw new Exception("ERROR! Nodes in cluster "+this.id+" don't have the same coordinates!");
            else prev = n;
        }
        this.x = prev.getX();
        this.y = prev.getY();
    }
    
    
    /**
     * Get the id of this cluster
     *
     * @return the id of this cluster
     */
    public int getId() {
        return id;
    }

    
    /**
     * Get the profit value for this cluster
     *
     * @return the profit value
     */
    public double getProfit() {
        return profit;
    }

    
    /**
     * Get the list of nodes in the cluster
     *
     * @return the list of nodes in the cluster
     */
    public List<Node> getNodes() {
        return nodes;
    }

    
    /**
     * Get the value of the y coordinate of the cluster
     *
     * @return the value of y
     */
    public double getY() {
        return y;
    }

    /**
     * Get the value of the x coordinate of the cluster
     *
     * @return the value of x
     */
    public double getX() {
        return x;
    }
    
    
    /**
     * Adds a node to the current cluster
     * @param n the node to add to the cluster
     */
    public void addNode(Node n) {
        this.nodes.add(n);
    }
    
    /**
     * Gets the total cost to serve every node in the cluster
     * @return the total cost for the cluster (excluding travel times)
     */
    public double getTotalCost() {
        double ret = 0.0;
        
        for(Node n : this.nodes){
            ret += n.getCost();
        }
        
        return ret;
    }
    
    /**
     * Gets the total cost for the given vehicle to serve every possible node in the cluster
     * @param v the vehicle in question
     * @return the total cost for the given vehicle to serve every possible node in the cluster
     */
    public double getTotalCostForVehicle(Vehicle v) {
        double ret = 0.0;
        
        for(Node n : this.nodes){
            if(v.canServe(n))
                ret += n.getCost();
        }
        
        return ret;
    }
    
    /**
     * Compute the distance between this cluster and the specified cluster.
     * @param c the cluster to calculate the distance from
     * @return the distance between the two clusters
     */
    public double distance(Cluster c) {
        return Math.floor((Math.sqrt(Math.pow(this.x - c.getX(), 2)+ Math.pow(this.y - c.getY(), 2))*1000))/1000.0;
    }
    
    /**
     * Compute the distance between the center of this cluster and the specified node.
     * @param n the node to calculate the distance from
     * @return the distance between the two clusters
     */
    public double distance(Node n) {
        return Math.floor((Math.sqrt(Math.pow(this.x - n.getX(), 2)+ Math.pow(this.y - n.getY(), 2))*1000))/1000.0;
    }
    
    /**
     * Get the number of nodes in the cluster
     * @return the number of nodes in the cluster
     */
    public int getNumNodes(){
        return this.nodes.size();
    }
    
    /**
     * Gets the set of services required by this cluster
     * @return the set of service IDs required by this cluster
     */
    public Set<Integer> getServices() {
        Set<Integer> services = new TreeSet<Integer>();
        for(Node n : this.nodes){
            services.add(n.getService());
        }
        return services;
    }
    
    /**
     * Gets the number of services required by this cluster
     * @return the number of services required by this cluster
     */
    public int getNumServices() {
        return this.getServices().size();
    }
    
    /**
     * Returns the list of streaks in the cluster for the specified vehicle.
     * A streak is a set of nodes that can be served one after the other by a
     * single vehicle.
     * @param vehicle the vehicle we want to find streaks for
     * @return the ordered list of streaks for the vehicle
     */
    public List<Streak> getStreaks(Vehicle vehicle) {
        List<Streak> streaks = new ArrayList<>();
        
        Streak currStreak = new Streak(vehicle, this);
        for(Node n : this.nodes){
            // If the node can be served by the vehicle, add it to the current streak
            if(vehicle.canServe(n)){
                currStreak.add(n);
            }
            else{
                // Don't add empty streaks!
                if(!currStreak.isEmpty()){
                    // Save the previous streak if it's not empty and start a new one
                    streaks.add(currStreak);
                    currStreak = new Streak(vehicle, this);
                }
            }
        }
        // Add the last streak ending on the last node (and still don't add empty streaks!)
        if(!currStreak.isEmpty()){
            // Save the previous streak if it's not empty and start a new one
            streaks.add(currStreak);
            currStreak = new Streak(vehicle, this);
        }
        
        return streaks;
    }
    
    /**
     * Returns the position in the cluster of the first occurrence of node n.
     * @param n the node to find
     * @return the first occurrence of node n in this Cluster. -1 if n doesn't exist.
     */
    public int positionOfNode(Node n){
        return this.nodes.indexOf(n);
    }
    
    /**
     * Gets the minimum number of vehicles needed to completely serve this cluster.
     * @return the minimum number of vehicles needed to completely serve this cluster
     */
    public int getMinNumberOfVehiclesNeeded(){
        return this.minNumberOfVehiclesNeeded;
    }

    /**
     * Updates the minimum number of vehicles needed to completely serve this cluster
     * @param vehicles a list of all the vehicles available for the problem instance
     */
    private void updateMinNumberOfVehiclesNeeded(List<Vehicle> vehicles) {
        /**
         * IDEA: get the minimum number of streaks needed to serve the whole cluster.
         * In order to do so, for each vehicle we get its streaks;
         * then every streak obtained is ordered by decreasing size and precedence;
         * overlapping streaks with lower precedence are then reduced to the minimum
         * lists of services by removing services already served by streaks with
         * a higher precedence and size.
         * At this point, we will have a limited set of streaks which could theoretically
         * serve the whole cluster using the minimum number of vehicles:
         * we shall now count the vehicles involved and that will be our minimum
         * number of vehicles to serve this cluster.
         */

        /**
         * IDEA: the maximum number of vehicles is obtained by sorting the streaks
         * the opposite way: sorting them by precedence and increasing size, we
         * will maximize the number of streaks.
         */

        /**
         * Note:
         * How sorting streaks affects maximum number of vehicles involved
         * and maximum number of streaks:
         * 
         * Unsorted streaks
         *    0 1 2 3 4 5 6 7 8
         * v1  |- - - - -|
         * v2|- - -|
         * v2              |- -|
         * v3          |- - -|
         * v1              |- -|
         * v4|- -|
         * v5|-|
         * 
         * Sorted by decreasing size and precedence
         *    0 1 2 3 4 5 6 7 8
         * v2|- - -|
         * v4|x x|
         * v5|x|
         * v1  |x x - - -|
         * v3          |x - -|
         * v2              |x -|
         * v1              |x x|
         * max v = 3
         * (This is the minimum number of vehicles for the cluster)
         * max streak = 4
         * 
         * Sorted by increasing size and precedence
         *    0 1 2 3 4 5 6 7 8
         * v5|-|
         * v4|x -|
         * v2|x x -|
         * v1  |x x - - -|
         * v3          |x - -|
         * v2              |x -|
         * v1              |x x|
         * max v = 5
         * (This is the maximum number of vehicles for the cluster)
         * max streak = 6
         */
        
        // Get all the streaks for each vehicle in the current cluster
        List<Streak> streaks = new ArrayList<>();
        for(int v=0; v<vehicles.size();v++){
            Vehicle vehicle = vehicles.get(v);
            // Find all streaks for vehicle in cluster
            streaks.addAll(this.getStreaks(vehicle));
        }

        // Sort streaks by size (in descending order) and precedence, in an ascending order
        streaks.sort(Streak.SIZE_COMPARATOR.reversed());
        streaks.sort(Streak.PRECEDENCE_COMPARATOR);

        // Crosscomparison: remove all overlapping streaks in a wise way
        // Now, for each streak in the cluster
        for(Streak sA : streaks){
            // compare it with every other streak in the cluster
            for(Streak sB : streaks){
                // If two streaks are equal, do nothing
                // If they are not equal, follow precedence and remove nodes
                //System.out.println("Streak sA: "+sA+"; Streak sB: "+sB); // DEBUG: remove later
                if(!sA.equals(sB) && sB.isPreceding(sA)){
                    sA.removeAll(sB);
                    //System.out.println(""); // DEBUG remove later
                }
            } // for each Streak sB
        } // for each Streak sA
        
        // Remove empty streaks from the list of streaks
        streaks.removeIf(i -> i.isEmpty());
        
        // Now, let's count how many vehicles are involved in these streaks
        Set<Vehicle> vset = new HashSet<>();
        streaks.forEach(s -> vset.add(s.getVehicle()));
        
        this.minNumberOfVehiclesNeeded = vset.size();
    }
    
    /**
     * Gets the maximum number of vehicles needed to completely serve this cluster.
     * @return the maximum number of vehicles needed to completely serve this cluster
     */
    public int getMaxNumberOfVehiclesNeeded(){
        return this.maxNumberOfVehiclesNeeded;
    }
    
    /**
     * Updates the maximum number of vehicles needed to completely serve this cluster
     * @param vehicles a list of all the vehicles available for the problem instance
     */
    private void updateMaxNumberOfVehiclesNeeded(List<Vehicle> vehicles){
        /**
         * IDEA: get the maximum number of streaks needed to serve the whole cluster.
         * In order to do so, for each vehicle we get its streaks;
         * then every streak obtained is ordered by decreasing size and precedence;
         * overlapping streaks with lower precedence are then reduced to the minimum
         * lists of services by removing services already served by streaks with
         * a higher precedence and size.
         * At this point, we will have a limited set of streaks which could theoretically
         * serve the whole cluster using the minimum number of vehicles:
         * we shall now count the vehicles involved and that will be our minimum
         * number of vehicles to serve this cluster.
         */
        
        // Get all the streaks for each vehicle in the current cluster
        List<Streak> streaks = new ArrayList<>();
        for(int v=0; v<vehicles.size();v++){
            Vehicle vehicle = vehicles.get(v);
            // Find all streaks for vehicle in cluster
            streaks.addAll(this.getStreaks(vehicle));
        }

        // Sort streaks by size (in ascending order) and precedence
        streaks.sort(Streak.SIZE_COMPARATOR);
        streaks.sort(Streak.PRECEDENCE_COMPARATOR);

        // Crosscomparison: remove all overlapping streaks in a wise way
        // Now, for each streak in the cluster
        for(Streak sA : streaks){
            // compare it with every other streak in the cluster
            for(Streak sB : streaks){
                // If two streaks are equal, do nothing
                // If they are not equal, follow precedence and remove nodes
                if(!sA.equals(sB) && sB.isPreceding(sA)){
                    sA.removeAll(sB);
                }
            } // for each Streak sB
        } // for each Streak sA
        
        // Remove empty streaks from the list of streaks
        streaks.removeIf(i -> i.isEmpty());
        
        // Now, let's count how many vehicles are involved in these streaks
        Set<Vehicle> vset = new HashSet<>();
        streaks.forEach(s -> vset.add(s.getVehicle()));
        
        this.maxNumberOfVehiclesNeeded = vset.size();
    }
    
    /**
     * Updates the weighted profit value for this cluster. Must be called before
     * <code>getWeightedProfit</code> is called.
     */
    public void updateWeightedProfit(){
        this.weightedProfit = profit/(1+getMinNumberOfVehiclesNeeded()*getTotalCost());
    }
    
    /**
     * Gets the weighted profit value for this cluster.
     * <br>The weighted profit is calculated as
     * <br><code>profit/(getMinNumberOfVehiclesNeeded()*getTotalCost());</code>
     * <br>in the <code>updateWeightedProfit</code> method, which must be called
     * before this method is called.
     * Making sure <code>setInstanceVehicles</code> is called is enough to be sure
     * that the weighted profit value is updated.
     * @return the weighted profit value for this cluster.
     * @throws Exception if the weighted profit wasn't updated at least once by the
     * <code>updateWeightedProfit</code> method before this method was called.
     */
    public double getWeightedProfit() throws Exception {
        if(this.weightedProfit<0){
            throw new Exception("Cluster "+this.getId()+"needs to have its weighted profit updated before it can be retrieved!");
        }
        return this.weightedProfit;
    }
    
    /**
     * Computes the minimum cost (service time) of any vehicle that serves this
     * cluster.
     * If no vehicle serves the cluster, Double.MAX_VALUE is returned.
     * 
     * @param vehicles the vehicles to find the minimum cost of
     * @return the minimum cost of any vehicle that serves this cluster.
     */
    public double getMinCostForVehicle(List<Vehicle> vehicles){
        double ret = Double.MAX_VALUE;
        for(Vehicle v : vehicles){
            if(v.canServe(this)){
                double cost = getTotalCostForVehicle(v);
                if(cost<ret)
                    ret=cost;
            }
        }
        return ret;
    }
    
    /**
     * Computes the maximum cost (service time) for any vehicle to serve this
     * cluster.
     * If no vehicle serves the cluster, Double.MIN_VALUE is returned.
     * 
     * @param vehicles the vehicles to find the maximum cost of
     * @return the maximum cost of any vehicle that serves this cluster
     */
    public double getMaxCostForVehicle(List<Vehicle> vehicles){
        double ret = Double.MIN_VALUE;
        for(Vehicle v : vehicles){
            double cost = getTotalCostForVehicle(v);
            if(cost>ret)
                ret=cost;
        }
        return ret;
    }

    /**
     * Sets the instanceVehicles for this cluster and updates all cluster parameters
     * which need the instance vehicles to be calculated, such as
     * <code>maxNumberOfVehiclesNeeded</code>, <code>minNumberOfVehiclesNeeded</code>
     * and <code>weightedProfit</code>.
     * @param instanceVehicles the list of vehicles provided by the problem instance
     */
    public void setInstanceVehicles(List<Vehicle> instanceVehicles) {
        this.instanceVehicles = instanceVehicles;
        this.updateMaxNumberOfVehiclesNeeded(instanceVehicles);
        this.updateMinNumberOfVehiclesNeeded(instanceVehicles);
        this.updateWeightedProfit();
    }
    
    @Override
    public String toString(){
        return ""+this.id;
    }
    
    
    

}
