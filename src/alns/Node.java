/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package alns;

/**
 * Class to describe a node and its properties for the orienteering problem.
 * @author Frash
 */
public class Node implements Comparable<Node>{
    
    private int id;
    private double x;
    private double y;
    private double cost;
    private int service;
    
    /**
     * Constructor for the class Node
     * @param id id of the Node in the model
     * @param x x coordinate of the Node
     * @param y y coordinate of the Node
     */
    public Node(int id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.cost = -1.0;
        this.service = -1;
    }
    
    /**
     * Sets the cost value for this node (default is -1)
     * @param cost 
     */
    public void setCost(double cost) {
        this.cost = cost;
    }
    
    /**
     * Sets the service number for this node (default is -1)
     * @param service 
     */
    public void setService(int service) {
        this.service = service;
    }
    
    /**
     * Get the ID of the service required by this node
     *
     * @return the ID of the service
     */
    public int getService() {
        return service;
    }

    
    /**
     * Get the cost to serve this node
     *
     * @return the cost for the node
     */
    public double getCost() {
        return cost;
    }

    
    /**
     * Get the value of the y coordinate
     *
     * @return the value of y
     */
    public double getY() {
        return y;
    }

    
    /**
     * Get the value of the x coordinate
     *
     * @return the value of x
     */
    public double getX() {
        return x;
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
     * Compute the Euclidean norm for the coordinates of this node
     * @return 
     */
    public double norm(){
        return Math.sqrt(Math.pow(this.x, 2.)+Math.pow(this.y, 2.));
    }
    
    /**
     * Compare this node to another node
     * @param o the node to compare this node with
     * @return 0 if the nodes are equal, -1 if the norm of this node is less than the norm of o, +1 otherwise
     */
    @Override
    public int compareTo(Node o) {
        if(this.x == o.getX() && this.y == o.getY())
            return 0;
        else{
            if(this.norm() <= o.norm())
                return -1;
            else return +1;
        }
    }
    
    /**
     * Compares two nodes on the id value
     * @param n the node to compare this node with
     * @return true if the two nodes are equal
     */
    public boolean equals(Node n){
        return this.id == n.id;
    }

}
