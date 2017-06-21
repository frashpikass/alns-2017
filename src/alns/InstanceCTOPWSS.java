package alns;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * This class defines the model of problem instances for the Orienteering problem.
 * It's serializable so that it can be easily saved and restored from disk without 
 * having to parse the model file again.
 * 
 * TO IMPLEMENT: Serialization
 * @author Team Mansini
 */
public class InstanceCTOPWSS implements Serializable
{
    /* Appunti: 
    Occhio:
        - fra due nodi non esistono archi da un nodo a se stesso
        - no archi che entrano nel nodo di partenza
        - no archi in uscita dal nodo finale.
    
    */
        
        /**
         * Name of the problem instance.
         */
        private String name;
	
        /**
         * Number of nodes in the problem instance. Includes the starting and the ending nodes.
         */
        private int num_nodes;
	
        /**
         * Number of clusterMap in the problem instance.
         */
        private int num_clusters;
	
        /**
         * Number of vehicles in the problem instance.
         */
        private int num_vehicles;
	
        /**
         * Number of services in the problem instance.
         */
        private int num_services;
	
        /**
         * Maximum time available for each vehicle to reach the last node.
         */
        private double tmax;
	
        /**
         * Distances matrix. Stores the distance between every couple of nodes (arcs).
         */
        private double[][] distances; // NdF. Prone to memory saturation given the problem size (in nodes)
	
        /**
         * Profits vector. Stores the profit offered by a cluster upon service completion.
         */
        private double[] profits;
	
        /**
         * Clusters map. Stores, for every known cluster ID, the list of corresponding node IDs.
         */
        private Map<Integer, List<Integer>> clusterMap; //cluster - nodes in cluster
	
        /**
         * Skills map. Stores, for every known vehicle ID, the list of corresponding service IDs.
         */
        private Map<Integer, List<Integer>> skills; //vehicle - list of services that can be performed
	
        /**
         * Service duration vector. Stores, for every node ID, the duration of the service required by the node.
         */
        private double[] serviceDuration;
	
        /**
         * Node service vector. Stores, for every node ID, the service ID required by that node.
         * Note: every node has one service.
         */
        private int[] nodeService;
        
        /**
         * Precedence matrix. Stores, for every couple of node IDs, 1 if the first node
         * must be served before the second node, 0 otherwise.
         */
	private int[][] precedenceMatrix; //precedenceMatrix[i][j] = 1  if node i has to be visited before node j
        
        /**
         * Attributes that define the data structure for the problem
         */
        private List<Node> nodes;
        private List<Cluster> clusters;
        private List<Vehicle> vehicles;
        
        /**
         * Add a node to the instance
         * @param n the node to add
         */
        public void addNode(Node n){
            nodes.add(n);
        }
        
        /**
         * Gets the node with the specified ID
         * @param id the id of the node to get from the cluster
         * @return the node with the specified ID
         */
        public Node getNode(int id){
            return nodes.get(id);
        }
        
        /**
         * Add a cluster to the instance
         * @param c the cluster to add
         */
        public void addCluster(Cluster c){
            this.clusters.add(c);
        }
        
        /**
         * Gets the cluster with the specified ID
         * @param id the ID of the cluster to get
         * @return the cluster with the specified ID
         */
        public Cluster getCluster(int id){
            return this.clusters.get(id);
        }
        
        /**
         * Add a vehicle to the instance
         * @param v the vehicle to add
         */
        public void addVehicle(Vehicle v){
            this.vehicles.add(v);
        }
        
        /**
         * Gets the vehicle with the specified ID
         * @param id the ID of the vehicle to get
         * @return the vehicle with the specified ID
         */
        public Vehicle getVehicle(int id){
            return this.vehicles.get(id);
        }
        
	/**
         * Constructor for class InstanceCTOPWSS
         * @param name Name of the problem instance
         * @param num_clusters Number of clusterMap in the instance
         * @param num_vehicles Number of vehicles in the instance
         * @param num_services Number of services in the instance
         * @param num_nodes Number of nodes in the instance
         * @param tmax maximum time available for each vehicle
         */
        public InstanceCTOPWSS(String name, int num_clusters, int num_vehicles, int num_services, int num_nodes, double tmax)
	{
		nodes = new ArrayList<>();
                clusters = new ArrayList<>();
                vehicles = new ArrayList<>();
                
                this.name = name;
		this.num_clusters = num_clusters;
		this.num_vehicles = num_vehicles;
		this.num_services = num_services;
		this.num_nodes = num_nodes;
		this.tmax = tmax;
		profits = new double[num_clusters];
		distances = new double [num_nodes][num_nodes];
		serviceDuration = new double[num_nodes];
		nodeService = new int[num_nodes];
		clusterMap = new HashMap<>();
		skills = new HashMap<>();
		precedenceMatrix = new int[num_nodes][num_nodes];
	}

	/**
         * Saves the distance between two nodes in the distances matrix.
         * @param firstNode the first node
         * @param secondNode the second node
         * @param distance the distance between the two
         */
        public void setDistance(int firstNode, int secondNode, double distance)
	{
		if(distances == null)
			distances = new double [num_nodes][num_nodes];
		
		distances[firstNode][secondNode] = distance;
	}

	/**
         * Retrieves the distance between two nodes in the distances matrix.
         * @param firstNode the first node
         * @param secondNode the second node
         * @return the stored distance between the two
         */
        public double getDistance(int firstNode, int secondNode)
	{
		return distances[firstNode][secondNode];
	}

	/**
         * Sets the profit of a specific cluster in the profits matrix.
         * @param cluster the id of the cluster
         * @param profit the profit obtained by completing all the tasks in the cluster
         */
        public void setProfit(int cluster, double profit)
	{
		profits[cluster] = profit;
	}

	/**
         * Retrieves the profit for a specific cluster as stored in the profits matrix.
         * @param cluster the id of the cluster
         * @return the stored profit value for the specific cluster
         */
        public double getProfit(int cluster)
	{
		return profits[cluster];
	}

	/**
         * Retrieves the name of the problem instance.
         * @return the name of the problem instance
         */
        public String getName()
	{
		return name;
	}
        
        /**
         * Retrieves the number of nodes in the instance (including the starting
         * and the ending nodes).
         * @return the number of nodes in the instance
         */
	public int getNum_nodes()
	{
		return num_nodes;
	}
	
        /**
         * Sets the new number of nodes for this instance.
         * @param num_nodes the new number of nodes for this instance
         */
	public void setNum_nodes(int num_nodes)
	{
		this.num_nodes = num_nodes;
	}

	/**
         * Retrieves the number of vehicles for the instance.
         * @return the number of vehicles for the instance
         */
        public int getNum_vehicles()
	{
		return num_vehicles;
	}
	
	/**
         * Retrieves Tmax, which is the maximum time for any vehicle
         * to reach the last node
         * @return Tmax
         */
        public double getTmax()
	{
		return tmax;
	}
	
	/**
         * Returns the list of node IDs withtin a specific cluster.
         * @param cluster the ID of the cluster to retrieve
         * @return the list of node IDs
         * @throws Exception if the provided cluster ID has not been previously stored
         */
        public List<Integer> getClusterNodeIDs(int cluster) throws Exception
	{
		if(clusterMap.containsKey(cluster))
			return clusterMap.get(cluster);
		else
			throw new Exception("Cluster not found");
	}
	
	/**
         * Checks whether a specific node belongs to a specific cluster.
         * @param cluster the ID of the cluster
         * @param node the ID of the node to look for
         * @return True if node belongs to cluster, False otherwise
         */
        public boolean belongs (int cluster, int node)
	{
            return clusterMap.containsKey(cluster) && clusterMap.get(cluster).contains(node);	
	}
	
	/**
         * Adds a node ID to a cluster.
         * @param cluster ID of the cluster to update
         * @param node ID of the node to add to the specific cluster
         */
        public void addVertex(int cluster, int node)
	{
		if(clusterMap.containsKey(cluster))
			clusterMap.get(cluster).add(node);
	}

	/**
         * Retrieves the number of clusterMap for this problem instance.
         * @return the number of clusterMap for this problem instance
         */
        public int getNum_clusters()
	{
		return num_clusters;
	}
	
	/**
         * Sets the list of clusterMap for this problem instance.
         * @param clusterMap The list of clusterMap.
         */
        public void setClusterMap(Map<Integer, List<Integer>> clusterMap)
	{
		this.clusterMap = clusterMap;
	}
	
	/**
         * Sets the list of skills for each vehicle in this problem instance.
         * @param skills a complete skill list
         */
        public void setSkills(Map<Integer, List<Integer>> skills )
	{
		this.skills = skills;
	}
	
	/**
         * Sets the precedence of node i over node j within a cluster.
         * @param i the node which has precedence
         * @param j the node which has to give precedence
         */
        public void setPrecedence(int i, int j)
	{
		precedenceMatrix[i][j] = 1;
	}
	
	/**
         * Returns the precedence value for node i over node j within a cluster.
         * @param i a node in a cluster
         * @param j another node in a cluster
         * @return the precedence of i over j (1 if i has precedence, 0 otherwise)
         * @throws Exception if i or j are undefined in the precedence matrix
         */
        public int getPrecedence(int i, int j) throws Exception
	{
		return precedenceMatrix[i][j];
	}
	
	/**
         * Retrieves the list of skills for a specific vehicle.
         * @param vehicle the ID of the vehicle
         * @return the list of skills of vehicle
         * @throws Exception if the vehicle ID isn't found
         */
        public List<Integer> getSkills(int vehicle) throws Exception
	{
		if(skills.containsKey(vehicle))
			return skills.get(vehicle);
		else
			throw new Exception("Vehicle not found");
	}
	
	/**
         * Checks whether a vehicle has a particular skill.
         * @param vehicle the ID of the vehicle
         * @param service the ID of the skill
         * @return 1 if the vehicle exists and can offer the specific service, 0 otherwise
         */
        public int hasSkill (int vehicle, int service)
	{
            // NOTE: changed this from boolean to integer to write constraint 7
            return (skills.containsKey(vehicle) && skills.get(vehicle).contains(service)) ? 1 : 0;	
	}
	
	/**
         * Sets the duration of a service required by a specific node
         * @param node the ID of the node
         * @param duration the duration of the service required by the node
         */
        public void setServiceDuration(int node, double duration)
	{
		if(serviceDuration == null)
			serviceDuration = new double[num_nodes];
		
		serviceDuration[node] = duration;
	}
	
	/**
         * Gets the duration of a service required by a specific node.
         * @param node the ID of the node
         * @return the duration of the service required by the node
         */
        public double getServiceDuration(int node)
	{
		return serviceDuration[node];
	}
	
	/**
         * Sets the service ID of the service required by the specified node.
         * @param node the ID of the node
         * @param service the ID of the service 
         */
        public void setNodeService(int node, int service)
	{
		if(nodeService == null)
			nodeService = new int[num_nodes];
		
		nodeService[node] = service;
	}
	
	/**
         * Gets the service ID of the service required by the specified node.
         * @param node the ID of the node
         * @return the ID of the service
         */
        public int getNodeService(int node)
	{
		return nodeService[node];
	}
	
	/**
         * Gets the total number of services specified by the problem instance.
         * @return the total number of services specified by the problem instance
         */
        public int getNum_services()
	{
		return num_services;
	}
        
        /**
         * Clone the list of clusters used in this instance and return it
         * @return the list of clusters used in this instance
         */
        public List<Cluster> cloneClusters(){
            ArrayList<Cluster> ret = new ArrayList<>(this.clusters);
            return ret;
        }
    
        /**
         * Gets the list of vehicles described by this instance
         * @return the list of vehicles described by thi instance
         */
        public List<Vehicle> getVehicles() {
            return vehicles;
        }
}