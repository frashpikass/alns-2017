/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package alns;

import com.sun.javafx.scene.control.skin.VirtualFlow;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class to represent a streak of nodes for a vehicle in a cluster
 * @author Frash
 */
public class Streak implements Comparable{
    private List<Node> nodes;
    private Vehicle vehicle;
    private Cluster cluster;
    
    /**
     * Comparator to sort streaks by size.
     */
    public final static Comparator<Streak> SIZE_COMPARATOR = new Comparator<Streak>() {
        @Override
        public int compare(Streak o1, Streak o2) {
            return o1.size()-o2.size();
        }
    };
    
    /**
     * Comparator to sort streaks by precedence.
     */
    public final static Comparator<Streak> PRECEDENCE_COMPARATOR = new Comparator<Streak>() {
        @Override
        public int compare(Streak o1, Streak o2) {
            return o1.precedenceOn(o2);
        }
    };
    
    /**
     * Gets the vehicle associated with this streak.
     * @return the vehicle associated with this streak
     */
    public Vehicle getVehicle() {
        return vehicle;
    }
    
    /**
     * Gets the cluster associated with this streak.
     * @return the cluster associated with this streak
     */
    public Cluster getCluster() {
        return cluster;
    }
    
    /**
     * Constructor for the class Streak
     * @param nodes a list of nodes
     * @param vehicle the vehicle that serves the streak
     * @param cluster the cluster within which the streak resides
     */
    public Streak(List<Node> nodes, Vehicle vehicle, Cluster cluster){
        this.nodes = nodes;
        this.vehicle = vehicle;
        this.cluster = cluster;
    }
    
    /**
     * Constructor for the class Streak
     * @param vehicle the vehicle that serves the streak
     * @param cluster the cluster within which the streak resides
     */
    public Streak(Vehicle vehicle, Cluster cluster){
        this.nodes = new ArrayList<>();
        this.vehicle = vehicle;
        this.cluster = cluster;
    }
    
    /**
     * Returns the number of nodes in this streak
     * @return the number of nodes in this streak
     */
    public int size(){
        return this.nodes.size();
    }
    
    /**
     * Get a deep clone of this Streak
     * @return a deep clone of this Streak
     * @throws java.lang.CloneNotSupportedException
     */
    @Override
    public Streak clone() throws CloneNotSupportedException{
        ArrayList<Node> nodesCopy = new ArrayList<>(this.nodes);
        return new Streak(nodesCopy, this.vehicle, this.cluster);
    }
    
    /**
     * Removes the first occurrence of the specified Node from this Streak, if
     * it is present. If this Streak does not contain the Node, it is unchanged.
     * More formally, removes the Node with the lowest index i in the Streak 
     * if such an element exists.
     * Returns true if this list contained the specified element (or
     * equivalently, if this list changed as a result of 
     * the call).
     * @param n the Node to remove
     * @return True if the Streak was changed
     */
    public boolean remove(Node n){
        return nodes.remove(n);
    }
    
    /**
     * Removes the Node at the specified position in this Streak.
     * Shifts any subsequent Nodes to the left (subtracts one from their
     * indices). Returns the Node that was removed from the Streak.
     * @param index
     * @return 
     */
    public Node remove(int index){
        return nodes.remove(index);
    }
    
    
    
    /**
     * Compares this streak with another for precedence.
     * Returns -1 if this streak precedes the other streak,
     * 0 if there is no precedence,
     * +1 if this streak comes after the other.
     * @param s the streak to compare this streak with
     * @return the precedence value
     */
    public int precedenceOn(Streak s){
        int ret = 0;
        if(this.cluster.equals(s.cluster)){
            // If the first node of this streak comes before the first node of
            // the other streak s, this streak has precedence on s
            int startOfThisStreak = this.cluster.positionOfNode(this.nodes.get(0));
            int startOfStreakS = this.cluster.positionOfNode(s.nodes.get(0));
            if( startOfThisStreak < startOfStreakS ){
                ret = -1;
            }
            else if(startOfThisStreak > startOfStreakS){
                ret = +1;
            }
        }
        
        return ret;
    }
    
    /**
     * Returns true if this streak is preceding the given streak.
     * @param s the streak to compare this with
     * @return true if this streak is preceding the given streak
     */
    public boolean isPreceding(Streak s){
        return (this.precedenceOn(s)<0);
    }

    @Override
    public int compareTo(Object o) {
        Streak s = (Streak) o;
        int ret = this.precedenceOn(s);
        if(ret == 0){
            if(this.nodes.size() < s.nodes.size()){
                ret = -1;
            }
            else if(this.nodes.size() > s.nodes.size()){
                ret = +1;
            }
        }
        return ret;     
    }

    /**
     * Appends the specified Node to the end of the Streak
     * @param e the node to append
     * @return true if the operation was successful
     */
    public boolean add(Node e) {
        return this.nodes.add(e);
    }

    /**
     * Removes from this Streak all of its nodes that are contained in the specified streak
     * @param c the streak we want to remove the nodes of
     * @return true if it worked and this streak changed
     */
    public boolean removeAll(Streak c) {
        return this.nodes.removeAll(c.nodes);
    }
    
    /**
     * Retains only the nodes in this streak that are contained in the
     * specified streak. In other words, removes from
     * this streak all of its nodes that are not contained in the specified streak.
     * @param c the streak containing nodes to be retained in this streak
     * @return true if it worked and this streak changed
     */
    public boolean retainAll(Streak c) {
        return this.nodes.retainAll(c.nodes);
    }
    
    /**
     * Return the nodes of this Streak
     * @return the nodes in this streak
     */
    public List<Node> getNodes(){
        return this.nodes;
    }
    
    /**
     * Returns true if this Streak contains no nodes.
     * @return true if this Streak contains no nodes
     */
    public boolean isEmpty() {
        return this.nodes.isEmpty();
    }
    
    /**
     * Returns the node at the specified position in this streak.
     * @param index index of the node in the streak
     * @return the node at position index in this streak
     */
    public Node get(int index){
        return this.nodes.get(index);
    }
    
    /**
     * Compares the specified streak with this streak for equality.  Returns
     * <tt>true</tt> if and only if both
     * streaks have the same vehicle, the same cluster, the same size and all
     * corresponding pairs of elements in
     * the two streaks are <i>equal</i>.  (Two nodes <tt>e1</tt> and
     * <tt>e2</tt> are <i>equal</i> if <tt>(e1==null ? e2==null :
     * e1.equals(e2))</tt>.)  In other words, two streaks are defined to be
     * equal if they contain the same nodes in the same order, they refer to the
     * same cluster and are served by the same vehicle.
     *
     * @param s the Streak to be compared for equality with this Streak
     * @return <tt>true</tt> if the specified Streak is equal to this Streak
     */
    public boolean equals(Streak s){
        return this.nodes.equals(s.nodes) && this.cluster==s.cluster && this.vehicle==s.vehicle;
    }
    

}
