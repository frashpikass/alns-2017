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

public class InstanceCTOPWSSReader
{
	public static InstanceCTOPWSS read(String path)
	{
		List<String> lines = new ArrayList<>();

		try (BufferedReader br = Files.newBufferedReader(Paths.get(path))) 
		{
			lines = br.lines().collect(Collectors.toList());

		} catch (IOException e) 
		{
			e.printStackTrace();
		}
		int numClusters = Integer.parseInt(lines.get(0).split("\\s+")[1].trim());
		int numVehicles = Integer.parseInt(lines.get(1).split("\\s+")[1].trim());
		int numServices = Integer.parseInt(lines.get(2).split("\\s+")[1].trim());
		int numNodes = Integer.parseInt(lines.get(3).split("\\s+")[1].trim());
		double tmax = Double.parseDouble(lines.get(4).split("\\s+")[1].trim());
		InstanceCTOPWSS inst = new InstanceCTOPWSS(path.substring(path.lastIndexOf("\\"), path.lastIndexOf(".")), numClusters, numVehicles, numServices, numNodes, tmax);
		List<Point> points = new ArrayList<>();
		
		for(int i = 5; i < 5+numNodes; i++)
		{
			String [] s = lines.get(i).split("\\s+");
			points.add(new Point(Double.parseDouble(s[0]), Double.parseDouble(s[1])));
			if(s.length > 2)
			{
				inst.setNodeService(i-5, Integer.parseInt(s[2]));
				inst.setServiceDuration(i-5, Double.parseDouble(s[3]));
			}
		}
		
		IntStream.range(0, numNodes)
			     .forEach(i ->IntStream.range(0, numNodes)
			     .filter(j -> i != j)
			     .forEach(j -> inst.setDistance(i, j, points.get(i).distance(points.get(j)))));
		
		Map<Integer, List<Integer>> clusters = new HashMap<>();
		for (int i = 5 + numNodes; i < 5 + numNodes + numClusters; i++)
		{
			String [] s = lines.get(i).split("\\s+");
			List<Integer> elem = new ArrayList<>();
			inst.setProfit(i-5-numNodes, Double.parseDouble(s[0]));
			for (int k = 1; k < s.length; k++)
			{
				elem.add(Integer.parseInt(s[k]));
			}
			clusters.put(i-5-numNodes, elem);
		}
		inst.setClusters(clusters);
		
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
		}
		inst.setSkills(skills);
		
		for (int i = 0; i < numClusters; i++)
		{
			try
			{
				List<Integer> elements = inst.getCluster(i);
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
		return Math.floor((Math.sqrt(Math.pow(this.x - p.x, 2)+ Math.pow(this.y - p.y, 2))*1000))/1000.0;
	}
}
