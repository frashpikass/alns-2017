package alns;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Parser class for the Orienteering problem model files. 
 * @author Team Mansini
 */
public class InstanceCTOPWSSReader
{   
        /**
         * Given the path to the model file, generate a problem instance
         * compatible with the Orienteering problem class.
         * @param path path to the model file
         * @return an Orienteering problem instance
         * @todo implement serialization to optimize loading operations.
         * Idea: if the path leads to a dat file, check whether it's a good
         * serialization of an InstanceCTOPWSS object and eventually load/return it.
         */
        public static InstanceCTOPWSS read(String path) throws Exception
	{
		List<String> lines = new ArrayList<>();

		try (BufferedReader br = Files.newBufferedReader(Paths.get(path))) 
		{
			lines = br.lines().collect(Collectors.toList());

		} catch (IOException e) 
		{
			e.printStackTrace();
		}
		// The first 5 lines in the file define general parameters
                
                int numClusters = Integer.parseInt(lines.get(0).split("\\s+")[1].trim());
		int numVehicles = Integer.parseInt(lines.get(1).split("\\s+")[1].trim());
		int numServices = Integer.parseInt(lines.get(2).split("\\s+")[1].trim());
		int numNodes = Integer.parseInt(lines.get(3).split("\\s+")[1].trim());
		double tmax = Double.parseDouble(lines.get(4).split("\\s+")[1].trim());
		
                /* PROBLEM: tell the professor!
                Throws:
                    java.lang.StringIndexOutOfBoundsException: String index out of range: -1
                path.lastIndexOf("\\") returns -1 if no \ are present in the path, so we'd better
                place a 0 instead or something
                
                */
                
                // Fix: get first character for name
                int firstIndexOfNameInPath;
                if((firstIndexOfNameInPath = path.lastIndexOf("//"))==-1){
                    if((firstIndexOfNameInPath = path.lastIndexOf("\\"))==-1){
                        firstIndexOfNameInPath=0;
                    }
                }
                
                InstanceCTOPWSS inst = new InstanceCTOPWSS(path.substring(firstIndexOfNameInPath, path.lastIndexOf(".")), numClusters, numVehicles, numServices, numNodes, tmax);
		List<Point> points = new ArrayList<>();
		
                // Get node coordinates, service required and service duration
                // The first/last nodes in the sequence are the starting/ending points
		for(int i = 5; i < 5+numNodes; i++)
		{
                        double x;
                        double y;
                        String [] s = lines.get(i).split("\\s+");
			x = Double.parseDouble(s[0]);
			y = Double.parseDouble(s[1]);
                        
                        points.add(new Point(x, y));
			Node n = new Node(i-5,x,y);
                        
                        if(s.length > 2)
			{
				int service = Integer.parseInt(s[2]);
                                double cost = Double.parseDouble(s[3]);
                            
                                inst.setNodeService(i-5, service);
				inst.setServiceDuration(i-5, cost);
                                
                                n.setService(service);
                                n.setCost(cost);
			}
                        
                        inst.addNode(n);
		}
		
                // Calculates distances between two nodes
		IntStream.range(0, numNodes)
			     .forEach(i ->IntStream.range(0, numNodes)
			     .filter(j -> i != j)
			     .forEach(j -> inst.setDistance(i, j, points.get(i).distance(points.get(j)))));
		
		// Gets for each cluster: profit value, nodes in the same cluster
                // The node order indicates precedence between different nodes
                Map<Integer, List<Integer>> clusters = new HashMap<>();
		for (int i = 5 + numNodes; i < 5 + numNodes + numClusters; i++)
		{
			String [] s = lines.get(i).split("\\s+");
			List<Integer> elem = new ArrayList<>();
			List<Node> nodes = new ArrayList<>();
                        double profit = Double.parseDouble(s[0]);
                        inst.setProfit(i-5-numNodes, profit);
			for (int k = 1; k < s.length; k++)
			{
				int nodeID = Integer.parseInt(s[k]);
                                elem.add(nodeID);
                                nodes.add(inst.getNode(nodeID));
			}
			clusters.put(i-5-numNodes, elem);
                        inst.addCluster(new Cluster(i-5-numNodes,nodes,profit));
		}
		inst.setClusterMap(clusters);
		
                /*
                * Finally, track the vehicles. For each vehicle (identified by its
                * row number) save the skills (services) it can satisfy
                */
		Map<Integer, List<Integer>> skills = new HashMap<>();
		for (int i = 5 + numNodes + numClusters; i < lines.size(); i++)
		{
			String [] s = lines.get(i).split("\\s+");
			List<Integer> skill = new ArrayList<>();
                        
			for (int k = 0; k < s.length; k++)
			{
				skill.add(Integer.parseInt(s[k]));
			}
			skills.put(i-5-numNodes-numClusters, skill);
                        Vehicle v = new Vehicle(i-5-numNodes-numClusters,skill);
                        inst.addVehicle(v);
		}
		inst.setSkills(skills);
		
                /* 
                Set the precedence for the clusters
                POSSIBLE ERROR: this cycle doesn't set precedences for the first and the last clusters!
                Fixed by changing the boundaries for the next cycle to i=0 (instead of 1)
                and i<numClusters (instead of numClusters -1)
                POSSIBLE ERROR2: precedences aren't set for each couple of nodes
                */
		for (int i = 0; i < numClusters; i++)
		{
			try
			{
				// Salva i nodi del cluster in elements
                                List<Integer> elements = inst.getClusterNodeIDs(i);
				for (int k = 0; k < elements.size()-1; k++)
				{
					inst.setPrecedence(elements.get(k), elements.get(k+1));
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			
		}
		return inst;
	}
}

class Point
{
	double x;
	double y;
	
	Point(double x, double y)
	{
		this.x = x;
		this.y = y;
	}
	
	double distance(Point p)
	{
		return Math.sqrt(Math.pow(this.x - p.x, 2)+ Math.pow(this.y - p.y, 2));
	}
}
