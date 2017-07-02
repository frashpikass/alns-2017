/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverModel;

import solverController.OrienteeringPropertiesBean;
import solverController.ALNSPropertiesBean;
import gurobi.*;
import java.io.File;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import solverController.Controller;
import solverController.Controller.Solvers;
import solverController.LogRedirector;
import solverController.OptimizationStatusMessage;
import sun.rmi.log.ReliableLog;

/**
 * Class to solve an instance of the Orienteering problem.
 * It offers a static main method to run.
 * @author Frash
 */
public class Orienteering extends SwingWorker<Boolean, OptimizationStatusMessage>
{
    /**
     * A Javabean that holds all general parameters for an Orienteering solver.
     */
    protected OrienteeringPropertiesBean orienteeringProperties;
    
    /**
     * Constant to define the default file extension for log files
     */
    private final static String LOG_FILE_EXTESION = ".log";
    
    /**
     * Constant to define the default file extension for object files
     */
    private final static String OBJECT_FILE_EXTESION = ".dat";
    
    /**
     * Constant value for the maximum time limit for the Gurobi solver
     */
    public static final double MAX_DEFAULT_TIMELIMIT = 600.0;
    
    /**
     * Constant value for the maximum number of threads used by the Gurobi solver
     */
    public static final int DEFAULT_NUMTHREADS = 4;
    
    /**
     * Gurobi environment
     */
    protected GRBEnv env;
    /**
     * Gurobi model for the problem
     */
    protected GRBModel model;
    
    /**
     * List of heuristic constraints to tighten the relaxed model
     */
    protected List<GRBConstr> heuristicConstraints;
    
    /**
     * An instance of the Orienteering problem
     */
    protected InstanceCTOPWSS instance;
    
    protected List<GRBConstr> constraint8;
    protected List<List<GRBVar>> constraint8Variables;
    
    /**
     * Hash of the instance file
     */
    private String instanceHash;
    
    /**
     * Path to the log file (where it will be written)
     */
    private String logFilePath;
    /**
     * Path to the model file to read
     */
    private String modelPath;
    
    /**
     * Redirects the output of the log file to console
     */
    protected LogRedirector logRedirector;

    public GRBVar[][][] getX() {
        return x;
    }

    public GRBVar[] getY() {
        return y;
    }

    public GRBVar[][] getZ() {
        return z;
    }
    
    
    
    /**
     * Gurobi variable x_v_i_j.
     * x[v][i][j] determines if arc (i,j) is traversed by vehicle v.
     * It's boolean and plays no role in the objective function.
     */
    protected GRBVar [][][] x; 
    
    /**
     * Gurobi variable y_c.
     * y[c] is boolean and determines whether cluster c has been completely served.
     * y[c] is also in the objective function.
     */
    protected GRBVar [] y ;
    
    /**
     * Gurobi variable z[i][j].
     * z_(i,j) is real non-negative and determines the time of arrival in node j from node i.
     * The upper bound of z is Tmax.
     */
    protected GRBVar [][] z;
    
    
    /**
     * Retrieve the Gurobi environment object for this problem.
     * @return the Gurobi environment object for this problem
     */
    public GRBEnv getEnv() {
        return env;
    }
    
    /**
     * Retrieve the Gurobi model object for this problem.
     * @return the Gurobi model object for this problem
     */
    public GRBModel getModel() {
        return model;
    }

    /**
     * Retrieve the instance object for this problem model.
     * @return the instance object for this problem model.
     */
    public InstanceCTOPWSS getInstance() {
        return instance;
    }
    
    /**
     * Retrieve the hash of the instance file for this problem model.
     * @return the hash string of the instance file
     */
    public String getInstanceHash() {
        return instanceHash;
    }
    
    /**
     * Constructor. Setups the Orienteering problem environment, model and instance.
     * @param outputFolderPath path to the folder that will hold any output files produced by the solvers
     * @param logFilePath name of the gurobi logfile to keep track of the solution process
     * @param modelPath path to the file containing the instance description
     * @param timeLimit maximum time for solving the problem (in seconds)
     * @param numThreads number of threads used to solve the problem.
     * Suggested value is 1 for real core (hyperthreading doesn't improve Gurobi's speed)
     * @throws java.lang.Exception if anything goes wrong
     */
//    public Orienteering(
//            String outputFolderPath,
//            String modelPath,
//            double timeLimit,
//            int numThreads
//    )
//            throws Exception
//    {
////        // Try to deserialize a previously loaded Orienteering model given the path
////        Orienteering loadedOrienteering = this.deserialize(modelPath);
//        
//        // Setup the Javabean that will hold parameters
//        this.orienteeringProperties = new OrienteeringPropertiesBean();
//        orienteeringProperties.s
//        
//        // Compute the hash for the model
//        this.instanceHash = HashUtilities.fileHash(modelPath);
//        
//        orienteeringProperties.setOutputFolderPath(outputFolderPath);
//        this.logFilePath = outputFolderPath+instanceNameFromPath(modelPath)+LOG_FILE_EXTESION;
//        this.modelPath = modelPath;
//        orienteeringProperties.setTimeLimit(timeLimit);
//        orienteeringProperties.setNumThreads(numThreads);
//        
//        this.heuristicConstraints = new ArrayList<>();
//        this.constraint8 = new ArrayList<>();
//        this.constraint8Variables = new ArrayList<>();
//        
//        // Go for preprocessing
//        instancePreprocessing(logFilePath, modelPath);
//    }

    /**
     * Constructor. Setups the Orienteering problem environment, model and instance variables
     * starting from a modelPath and an OrienteeringPropertiesBean containing all
     * the solver parameters.
     * @param modelPath path to the file containing the instance description
     * @param opb an OrienteeringPropertiesBean containing all the solver parameters
     * @throws Exception if anything goes wrong
     */
    public Orienteering(String modelPath, OrienteeringPropertiesBean opb) throws Exception{
        // Setup solver run specific parameters
        this.orienteeringProperties = opb;
        
        // Setup instance specific parameters
        this.modelPath = modelPath;
        this.logFilePath = opb.getOutputFolderPath()+instanceNameFromPath(modelPath)+LOG_FILE_EXTESION;
        this.heuristicConstraints = new ArrayList<>();
        this.constraint8 = new ArrayList<>();
        this.constraint8Variables = new ArrayList<>();
        
        // Redirect the log to stdout
        this.logRedirector = new LogRedirector(logFilePath);
        
        // Go for preprocessing
        instancePreprocessing();
    }
    
    /**
     * Gets the name of the instance from the given instance file path
     * @param path path to the instance file
     * @return the name of the instance file, without any extension
     */
    private static String instanceNameFromPath(String path){
        int firstIndexOfNameInPath;
        if((firstIndexOfNameInPath = path.lastIndexOf("//"))==-1){
            if((firstIndexOfNameInPath = path.lastIndexOf("\\"))==-1){
                firstIndexOfNameInPath=0;
            }
        }
                
        return path.substring(firstIndexOfNameInPath, path.lastIndexOf("."));
    }
    
    /**
     * Constructor. Setups the Orienteering problem environment, model and instance
     * starting from a previously existing Orienteering object.
     * @param o a previously existing orienteering object.
     * @throws Exception if anything goes wrong
     */
    public Orienteering(Orienteering o) throws Exception{
        this.orienteeringProperties = o.orienteeringProperties;
        this.constraint8 = o.constraint8;
        this.constraint8Variables = o.constraint8Variables;
        this.env=o.getEnv();
        this.heuristicConstraints = o.heuristicConstraints;
        this.instance=o.getInstance();
        this.instanceHash=o.getInstanceHash();
        this.logFilePath = o.logFilePath;
        this.model=o.getModel();
        this.modelPath=o.modelPath;
        this.x = o.getX();
        this.y = o.getY();
        this.z = o.getZ();
        this.logRedirector = o.logRedirector;
    }
    
//    /**
//     * Initialization method. Setups this Orienteering problem environment,
//     * model and instance, copying everyhing from another Orienteering object.
//     * @param o an Orienteering object to copy attributes from
//     */
//    private void initializeFromOrienteeringObject(Orienteering o){
//        this.orienteeringProperties = o.orienteeringProperties;
//        this.constraint8 = o.constraint8;
//        this.constraint8Variables = o.constraint8Variables;
//        this.env=o.getEnv();
//        this.heuristicConstraints = o.heuristicConstraints;
//        this.instance=o.getInstance();
//        this.instanceHash=o.getInstanceHash();
//        this.logFilePath = o.logFilePath;
//        this.model=o.getModel();
//        this.modelPath=o.modelPath;
//        this.x = o.getX();
//        this.y = o.getY();
//        this.z = o.getZ();
//    }
    
    /**
     * Preprocess the instance file and serialize the resulting Orienteering object
     * @param logname name of the gurobi logfile to keep track of the solution process
     * @param modelPath path to the file containing the instance description
     * @throws Exception 
     */
    private void instancePreprocessing()
            throws Exception{
        // Read the instance file from text
        this.instance = InstanceCTOPWSSReader.read(modelPath);
        
        // Setup the model's variables, constraints and objective function
        this.setupEnvironment(logFilePath);
        
        //Try to serialize the produced constraints
        model.write(orienteeringProperties.getOutputFolderPath()+instance.getName()+".lp");
    }
    
    /**
     * Returns true if the arc i,j is a noose (a loop on the same node)
     * @param i ID of the first node in the arc
     * @param j ID of the second node in the arc
     * @return true if (i,j) is a noose
     */
    private static boolean isNoose(int i, int j){
        return i==j;
    }
    
    /**
     * Setups the Orienteering problem model and environment.
     * This function will also populate the model with all the necessary constraints,
     * variables, the target function and the objective (maximize or minimize).
     * @param logname name of the gurobi logfile to keep track of the solution process
     */
    private void setupEnvironment(String logname) throws Exception
    {
        try {
            // Start redirecting the log to the output
            logRedirector.execute();
            //SwingUtilities.invokeLater(logRedirector);
            
            this.env = new GRBEnv(logname);
            
            this.env.set(GRB.DoubleParam.TimeLimit, orienteeringProperties.getTimeLimit());
            this.env.set(GRB.IntParam.Threads, orienteeringProperties.getNumThreads());
            this.env.set(GRB.IntParam.LogToConsole, 0);
            this.env.set(GRB.IntParam.OutputFlag, 1);
            this.model = new GRBModel(this.env);
            
            // Some useful constants for constraint definition
            int firstNodeID = 0;
            int lastNodeID = instance.getNum_nodes()-1;
            //Note: getNum_nodes() returns nmax. nmax-1 is the final node. 0 is the initial node.
            
            /*********** 
            * VARIABLES 
            ************/
            // x_v(i,j) determines if arc (i,j) is traversed by vehicle v
            // It's boolean, but plays no role in the objective function
            // Don't include nooses
            // Don't include arcs exiting node lastNodeID
            // Don't include arcs entering node firstNodeID
            this.x = new GRBVar[instance.getNum_vehicles()][instance.getNum_nodes()][instance.getNum_nodes()];
            for (int v=0; v<instance.getNum_vehicles();v++){
                for(int i=firstNodeID; i<instance.getNum_nodes(); i++){
                    for(int j=firstNodeID; j<instance.getNum_nodes(); j++){
                        if(isNoose(i, j) || j==firstNodeID || i==lastNodeID){
                            // Exclude nooses and illegal arcs
                            x[v][i][j] = model.addVar(0.0, 0.0, 0.0, GRB.BINARY, "x_v"+v+"_arc("+i+","+j+")");
                        }
                        else{
                            x[v][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x_v"+v+"_arc("+i+","+j+")");
                        }
                        
                    }
                }
            }
            
            // y_c is boolean and determines whether cluster c has been completely served
            // y_c is also in the objective function (every y_c is multiplied by
            // the profit generated by cluster c upon completion)
            this.y = new GRBVar[instance.getNum_clusters()];
            for(int cluster = 0; cluster<instance.getNum_clusters(); cluster++){
                y[cluster] = model.addVar(0.0, 1.0, instance.getProfit(cluster), GRB.BINARY, "y_c"+cluster);
            }
            
            // z_(i,j) is real non-negative and determines the time of arrival in node j from node i
            this.z = new GRBVar[instance.getNum_nodes()][instance.getNum_nodes()];
            for(int i=0; i<instance.getNum_nodes(); i++){
                for(int j=0; j<instance.getNum_nodes(); j++){
                    //z[i][j] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "z_("+i+","+j+")");
                    z[i][j] = model.addVar(0.0, instance.getTmax(), 0.0, GRB.CONTINUOUS, "z_("+i+","+j+")");
                }
            }
                    
            // Integrate new variables
            model.update();
            
            
            /********************************************
            * EXPRESSIONS (obj function and constraints) 
            *********************************************/
            
            // Expression 1: Set objective function - we did it when we added y_c
            model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);
            
            // Expression 2: ensures that exactly vmax vehicles exit node 0 and
            //               vmax vehicles arrive at node nmax + 1
            
            for(int v=0; v<instance.getNum_vehicles(); v++){
                GRBLinExpr expr2a = new GRBLinExpr();
                GRBLinExpr expr2b = new GRBLinExpr();    
                for(int i=firstNodeID; i<instance.getNum_nodes(); i++){
                    expr2a.addTerm(1.0, x[v][firstNodeID][i]);
                    expr2b.addTerm(1.0, x[v][i][lastNodeID]);
                }    
                model.addConstr(expr2a, GRB.EQUAL, 1.0, "c2a(v"+v+")");
                model.addConstr(expr2b, GRB.EQUAL, 1.0, "c2b(v"+v+")");
            }
            
            // Expression 3: ensures that node i of cluster c can be visited by
            //               a single vehicle and also impose that, if a node
            //               in a cluster is visited by a vehicle, every other
            //               node in the same cluster has to be visited by a
            //               vehicle. If every node in a cluster c is visited,
            //               variable y_c takes value 1
            for(int c=0; c<instance.getNum_clusters(); c++){
                // Retrieve the list of nodes in the cluster
                List<Integer> nodesInCluster = instance.getClusterNodeIDs(c);
                
                for(int i : nodesInCluster){
                    GRBLinExpr expr3a = new GRBLinExpr();
                    GRBLinExpr expr3b = new GRBLinExpr();
                    for(int v=0; v<instance.getNum_vehicles(); v++){
                        for(int j=firstNodeID; j<instance.getNum_nodes(); j++){
                            expr3a.addTerm(1.0, x[v][i][j]);
                            expr3b.addTerm(1.0, x[v][j][i]);
                        }
                    }
                    model.addConstr(expr3a, GRB.EQUAL, y[c], "c3a(c"+c+",i"+i+")");
                    model.addConstr(expr3b, GRB.EQUAL, y[c], "c3b(c"+c+",i"+i+")");
                }
            }
            
            // Expression 4: ensures that if a vehicle v arrives in node i,
            //               the same vehicle leaves node i
            for(int v=0; v<instance.getNum_vehicles(); v++){
                for(int i=firstNodeID+1; i<lastNodeID; i++){
                    GRBLinExpr expr4a = new GRBLinExpr();
                    GRBLinExpr expr4b = new GRBLinExpr();
                    for(int j=firstNodeID; j<instance.getNum_nodes(); j++){
                        expr4a.addTerm(1.0, x[v][i][j]);
                        expr4b.addTerm(1.0, x[v][j][i]);
                    }
                    model.addConstr(expr4a, GRB.EQUAL, expr4b, "c4a(v"+v+",node"+i+")");
                }
            }
            
            // Expression 5: ensure that, if a vehicle v visits node j immediately
            //               after node i, the time elapsed between the arrival
            //               in node i and the arrival in node j is equal to the
            //               time ti needed to provide the service required by
            //               node i plus the travel time t_(i,j)
            //               between node i and node j
            for(int i=firstNodeID+1; i<lastNodeID; i++){
                // Left hand side
                GRBLinExpr expr5a = new GRBLinExpr();
                for(int j=firstNodeID; j<instance.getNum_nodes(); j++){
                    expr5a.addTerm(1.0, z[i][j]);
                    expr5a.addTerm(-1.0, z[j][i]);
                }
                
                // Right hand side
                GRBLinExpr expr5b = new GRBLinExpr();
                for(int v=0; v<instance.getNum_vehicles(); v++){
                    for(int j=firstNodeID; j<instance.getNum_nodes(); j++){
                        expr5b.addTerm(instance.getDistance(i, j)+instance.getServiceDuration(i), x[v][i][j]);
                    }
                }
                // Add constraint
                model.addConstr(expr5a, GRB.EQUAL, expr5b, "c5(node"+i+")");    
            }
            
            // Expression 6: serves the same purpose of constraints (5), but
            //               deal with the special case of the starting node.
            //               In this case, the time z_(i,j) required to reach
            //               any node i from the starting node 0 is equal to the
            //               travel time t_(0,i) between node 0 and node i
            for(int i=firstNodeID+1; i<instance.getNum_nodes(); i++){
                GRBLinExpr expr6 = new GRBLinExpr();
                for(int v=0; v<instance.getNum_vehicles(); v++){
                    expr6.addTerm(instance.getDistance(firstNodeID, i), x[v][firstNodeID][i]);
                }
                model.addConstr(z[firstNodeID][i], GRB.EQUAL, expr6, "c6(node"+i+")");
            }
            
            // Expression 7: ensure that vehicle v can visit node i if and
            //               only if it is able to provide the service required
            //               by the node
            for(int v=0; v<instance.getNum_vehicles(); v++){
                for(int i=firstNodeID+1; i<lastNodeID; i++){
                    
                    // Left hand side
                    GRBLinExpr expr7a = new GRBLinExpr();
                    for(int j=firstNodeID; j<instance.getNum_nodes(); j++){
                        expr7a.addTerm(1.0, x[v][i][j]);
                        expr7a.addTerm(1.0, x[v][j][i]);
                    }
                    
                    // Right hand side
                    GRBLinExpr expr7b = new GRBLinExpr();
                    expr7b.addConstant(2.0*instance.hasSkill(v,instance.getNodeService(i)));
                    
                    // Add constraint 7
                    model.addConstr(expr7a, GRB.LESS_EQUAL, expr7b, "c7(node"+i+",v"+v+")");
                }
            }
            
            // Expression 8: ensure that, given a pair of nodes (i; j),
            //               variable z_(i,j) can take a value greater than 0
            //               if and only if there is a vehicle that travels from
            //               node i to node j
            for(int i=firstNodeID; i<instance.getNum_nodes(); i++){
                for(int j=firstNodeID; j<instance.getNum_nodes(); j++){
                    GRBLinExpr expr8 = new GRBLinExpr();
                    List<GRBVar> localConstr8Var = new ArrayList<>();
                    for(int v=0; v<instance.getNum_vehicles(); v++){
                        expr8.addTerm(instance.getTmax(), x[v][i][j]);
                        localConstr8Var.add(x[v][i][j]);
                    }
                    
                    // Add the constraint to the model and save it for later use
                    // This is one constraint for every z[i][j]
                    this.constraint8.add(model.addConstr(z[i][j], GRB.LESS_EQUAL, expr8, "c8_arc("+i+","+j+")"));

                    // Save the list of variables used by constraint8
                    this.constraint8Variables.add(localConstr8Var);
                    // This will needed in some heuristic methods later, such as
                    // repairBackToFeasibility
                }
            }
            
            
            // Expression 9: introduced to ensure that, given a cluster c,
            //               if a precedence relationship exists between node i
            //               of cluster c and j of cluster c,
            //               then node j can be visited only after node i has
            //               been provided with the service it requires
            for(int c=0; c<instance.getNum_clusters(); c++){
                // Retrieve the list of nodes in the cluster
                List<Integer> nodesInCluster = instance.getClusterNodeIDs(c);
                
                for(int i : nodesInCluster){
                    double d_i = instance.getServiceDuration(i);
                    for(int j : nodesInCluster){
                        // Left hand side
                        GRBLinExpr expr9a = new GRBLinExpr();
                        // Right hand side
                        GRBLinExpr expr9b = new GRBLinExpr();
                        
                        // Stores the precedence between nodes i and j
                        int w_i_j = instance.getPrecedence(i, j);
                        
                        for(int k=firstNodeID; k<instance.getNum_nodes(); k++){
                            // First term of the left hand side    
                            expr9a.addTerm(w_i_j, z[k][i]);
                            for(int v=0; v<instance.getNum_vehicles(); v++){
                                // Second term of the left hand side
                                expr9a.addTerm(w_i_j*d_i, x[v][k][i]);
                            }
                            
                            // Right hand side
                            expr9b.addTerm(1.0, z[k][j]);
                        }
                        // I previosuly added the constraint here
                        model.addConstr(expr9a, GRB.LESS_EQUAL, expr9b, "c9_c"+c+"_arc("+i+","+j+")");
                    }
                }
                // Add the costraint
                // model.addConstr(expr9a, GRB.LESS_EQUAL, expr9b, "c9_c"+c+"");
            }
            
            // Expression 10: impose non negative conditions on the z variables
            // Expression 11: binary conditions on the x variable
            // Expression 11: binary conditions on the y variable
            // ...Those were already specified in the model variable declarations.
            
            
            // OUR CONSTRAINTS
            
            // Expression 12:
            // All arcs that go from the starting deposit to any node of any cluster but the first should be removed
            for(int c=0; c<instance.getNum_clusters(); c++){
                // Retrieve the list of nodes in the cluster
                List<Integer> nodesInCluster = instance.getClusterNodeIDs(c);
                for(int i=1; i<nodesInCluster.size();i++){
                    for(int v=0; v < this.instance.getNum_vehicles(); v++){
                        model.addConstr(x[v][firstNodeID][nodesInCluster.get(i)], GRB.EQUAL, 0.0, "c12_c"+c+"_n"+nodesInCluster.get(i)+"_v"+v);
                    }
                }
            }
            
            // Expression 14:
            // All arcs inside of a cluster that go in any direction but the one
            // specified by precedence should be removed
            for(int c=0; c<instance.getNum_clusters(); c++){
                // Retrieve the list of nodes in the cluster
                List<Integer> nodesInCluster = instance.getClusterNodeIDs(c);
                for(int v=0; v < this.instance.getNum_vehicles(); v++){
                    for(int i=0; i<nodesInCluster.size();i++){
                        for(int j=0; j<nodesInCluster.size();j++){
                            if(i != j && j != i+1){
                                model.addConstr(x[v][nodesInCluster.get(i)][nodesInCluster.get(j)], GRB.EQUAL, 0.0, "c14_c"+c+"_arc("+nodesInCluster.get(i)+","+nodesInCluster.get(j)+")_v"+v);
                                
                            }
                        }
                    }
                }
            }
            
            // Activates all heuristic constraints
            //this.toggleHeuristicConstraintsOn();
            
//            // Expression 16:
//            /**
//             * -Streak optimization heuristic 2-
//             * A streak is a set of nodes in a cluster that can be served all at
//             * once by the same vehicle. We want to impose that if a vehicle enters
//             * a streak in a cluster, it can not exit the cluster until its
//             * current streak has been fully served.
//             * To do so, for each cluster we get its ordered list of services.
//             * Then, for each vehicle, we look for every streak it can serve in that
//             * particular cluster.
//             * At this point, for each streak of a specific vehicle, we remove
//             * all arcs that go from a node in the middle of a streak to another
//             * node which is different from the next one in the streak.
//             * Arcs to the initial node of a streak and from the final node of a
//             * streak are left unaltered.
//             * 
//             * Also we want that if a vehicle has a streak in a cluster, such
//             * vehicle can not enter the streak from a point which is not the
//             * first in the streak, unless that point is also an exit point for
//             * a streak by a different vehicle
//             * 
//             * To do so, for each cluster we get its ordered list of services.
//             * Then, for each vehicle, we look for every streak it can serve in that
//             * particular cluster.
//             * At this point, for each streak of a specific vehicle, we remove
//             * all arcs that go from a node in the middle of a streak to another
//             * node which is different from the next one in the streak.
//             * Arcs to the initial node of a streak and from the final node of a
//             * streak are left unaltered.
//             * 
//             * 
//             * This set of constraints might not work for every instance, thus
//             * it's an heuristic.
//             */
//            
//            // For each cluster
//            for(int c=0; c<instance.getNum_clusters(); c++){
//                Cluster cluster = instance.getCluster(c);
//                
//                // Get all the streaks for each vehicle in the current cluster
//                List<Streak> streaks = new ArrayList<>();
//                for(int v=0; v<instance.getNum_vehicles();v++){
//                    Vehicle vehicle = instance.getVehicle(v);
//                    // Find all streaks for vehicle in cluster
//                    streaks.addAll(cluster.getStreaks(vehicle));
//                }
//                
//                // Sort streaks by precedence and size, in an ascending order
//                Collections.sort(streaks);
//                
//                // Crosscomparison: remove all overlapping streaks in a wise way
//                
//                // Now, for each streak in the cluster
//                for(Streak sA : streaks){
//                    // get the vehicleID for the current streak
//                    int vehicleID = sA.getVehicle().getId();
//                    
//                    // compare it with every other streak in the cluster
//                    for(Streak sB : streaks){
//                        if(!sA.equals(sB)){
//                            if(sB.isPreceding(sA)){
//                                // Clone sA: the clone will retain all removed nodes
//                                Streak sAClone = sA.clone();
//                                // Retain the nodes in common with streak sB
//                                sAClone.retainAll(sB);
//                                // Remove from sA all nodes in common with sB
//                                sA.removeAll(sB);
//                                
//                                // Disconnect all removed nodes: they must not be
//                                // entered nor exited by the streaks's vehicle
//                                for(Node n : sAClone.getNodes()){
//                                    int nodeID = n.getId();
//                                    for(int i=firstNodeID; i<=lastNodeID; i++){
//                                        // Add constraints to remove all the arcs leading to
//                                        // or coming from the removed nodes for the current vehicle
//                                        model.addConstr(x[vehicleID][nodeID][i], GRB.EQUAL, 0.0, "c16A_c"+c+"_arc("+nodeID+","+i+")_v"+vehicleID);
//                                        model.addConstr(x[vehicleID][i][nodeID], GRB.EQUAL, 0.0, "c16A_c"+c+"_arc("+i+","+nodeID+")_v"+vehicleID);
//                                    } //for
//                                } // for
//                            } // if
//                        } // if
//                    } // for
//                    
//                    // Now the Streak sA has been cleaned up from overlapping nodes
//                    // so we just have to make sure that a vehicle can only enter from
//                    // the entry point and exit from the exit point
//                    if(!sA.isEmpty() && sA.size()>1){
//                        // Let's hypothesize entryPoint != exitPoint since sA.size()>1
//                        Node entryPoint = sA.get(0);
//                        Node exitPoint = sA.get(sA.size()-1);
//                        
//                        // For each node in the streak
//                        for(Node n : sA.getNodes()){
//                            
//                            // If n is an entry point
//                            // Remove all arcs that exit an entry point, except for
//                            // the one that goes to the following node in the streak
//                            if(n.equals(entryPoint)){
//                                Node nextNode = sA.get(sA.getNodes().indexOf(n)+1);
//                                for(int i=firstNodeID; i<=lastNodeID; i++){
//                                    if(!instance.getNode(i).equals(nextNode)){
//                                        model.addConstr(x[vehicleID][n.getId()][i], GRB.EQUAL, 0.0, "c16B_c"+c+"_arc("+n.getId()+","+i+")_v"+vehicleID);
//                                    }
//                                }
//                            }
//                            
//                            // If n is an exit point
//                            // Remove all arcs that enter an exit point, except for
//                            // the one that comes from the previous node in the streak
//                            else if(n.equals(exitPoint)){
//                                Node prevNode = sA.get(sA.getNodes().indexOf(n)-1);
//                                for(int i=firstNodeID; i<=lastNodeID; i++){
//                                    if(!instance.getNode(i).equals(prevNode)){
//                                        model.addConstr(x[vehicleID][i][n.getId()], GRB.EQUAL, 0.0, "c16B_c"+c+"_arc("+n.getId()+","+i+")_v"+vehicleID);
//                                    }
//                                }
//                            }
//                            
//                            // If a node is neither an entry nor an exit point for the current
//                            // streak, remove all entering and exiting arcs except
//                            // for the ones that come from the previous node or go to the next node
//                            else{
//                                Node nextNode = sA.get(sA.getNodes().indexOf(n)+1);
//                                Node prevNode = sA.get(sA.getNodes().indexOf(n)-1);
//                                for(int i=firstNodeID; i<=lastNodeID; i++){
//                                    if(!instance.getNode(i).equals(nextNode)){
//                                        model.addConstr(x[vehicleID][n.getId()][i], GRB.EQUAL, 0.0, "c16B_c"+c+"_arc("+n.getId()+","+i+")_v"+vehicleID);
//                                    }
//                                    if(!instance.getNode(i).equals(prevNode)){
//                                        model.addConstr(x[vehicleID][i][n.getId()], GRB.EQUAL, 0.0, "c16B_c"+c+"_arc("+n.getId()+","+i+")_v"+vehicleID);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            } // for each cluster
            
            
            // One last update to the model to make sure everything is ok
            model.update();
            
        } catch (GRBException ex) {
            Logger.getLogger(Orienteering.class.getName()).log(Level.SEVERE, null, ex);
        }    
    }
    
    /**
     * Clean up resources by disposing of the model and the environment.
     * @throws GRBException if anything goes wrong with the disposal
     */
    public void cleanup() throws GRBException{
        model.dispose();
        env.dispose();
    }
    
    public static void main(String[] args) {
        int SOLVE_MIPS=0;
        int SOLVE_RELAXED=1;
        int SOLVE_ALNS=2;
        
        System.out.println("Working dir: "+System.getProperty("user.dir"));
        try{
            String modelPath = "Instance0.txt";
            
            OrienteeringPropertiesBean opb = new OrienteeringPropertiesBean();
            opb.setForceHeuristicConstraints(false);
            opb.setNumThreads(DEFAULT_NUMTHREADS);
            opb.setOutputFolderPath("");
            opb.setTimeLimit(1*60.0);
            
            Orienteering o = new Orienteering(
                    modelPath,
                    opb
            );
            
            // Select the solver to use
            int solutionType = SOLVE_ALNS;

            // Must set LazyConstraints parameter when using lazy constraints
            // o.getModel().set(GRB.IntParam.LazyConstraints, 1);
            if(solutionType == SOLVE_RELAXED){
                o.optimizeRelaxed();
            }
            else if(solutionType == SOLVE_MIPS)
            {
                o.optimizeMIPS();
            }
            else if(solutionType == SOLVE_ALNS){
                // Use the controller to test ALNS!
            }
            // Dispose of all used variables
            o.cleanup();
        }
        catch(Exception e){
            System.out.println(e.toString());
        }
    }
    
    /**
     * Optimize the relaxation of the model provided.
     * @throws GRBException if any Gurobi operation goes wrong
     * @throws Exception if there are problems while toggling the constraints
     */
    public void optimizeRelaxed() throws GRBException, Exception{
        // Reset the model to an unsolved state
        model.reset();
        
        // Check if the user wants to use heuristic constraints
        if(orienteeringProperties.isForceHeuristicConstraints()) toggleHeuristicConstraintsOn();
        
        GRBModel relaxedModel = model.relax();
        
        // Optimize the relaxed model
        relaxedModel.optimize();
        
        // Save the relaxed solution to file
        relaxedModel.write(orienteeringProperties.getOutputFolderPath()+instance.getName()+"_relaxed.sol");
        
        // Dispose of the relaxed model
        relaxedModel.dispose();
    }
    
    /**
     * Update the normal time limit for the Gurobi solver
     * @param newTimeLimit the new time limit in seconds
     * @throws GRBException if there are problems while updating the parameters
     */
    public void updateTimeLimit(double newTimeLimit) throws GRBException{
        this.env.set(GRB.DoubleParam.TimeLimit, newTimeLimit);
        model.update();
    }
    
    /**
     * Update the number of threads to use with the Gurobi solver
     * @param newNumThreads the new number of threads to use
     * @throws GRBException if there are problems while updating the parameters
     */
    public void updateNumThreads(int newNumThreads) throws GRBException{
        this.env.set(GRB.IntParam.Threads, newNumThreads);
        model.update();
    }
    
    /**
     * Optimize the provided model using Gurobi's MIPS solver.
     * @throws GRBException if any Gurobi operation goes wrong
     * @throws Exception if there are problems while toggling the constraints
     */
    public void optimizeMIPS() throws GRBException, Exception{
        // Reset the model to an unsolved state
        model.reset();
        
        // Check if you want to use heuristic constraints
        if(orienteeringProperties.isForceHeuristicConstraints()) toggleHeuristicConstraintsOn();
        
        // Optimize the model
        model.optimize();
        
        // Save the solution to file
        writeSolution();
        
        // Get/log the paths of vehicles
        logVehiclePaths();

        // Log visited clusters
        logVisitedClusters();
    }
    
    /**
     * Write the solver solution for the current model to a file.
     * The file will be placed in the output folder path
     * specified.
     * @throws gurobi.GRBException if there are problems while retrieving the solution or writing it to a file
     */
    protected void writeSolution() throws GRBException{
        model.write(orienteeringProperties.getOutputFolderPath()+instance.getName()+".sol");
    }
    
    /**
     * After an integer solution has been computed, this method can get a list
     * of paths for each vehicle from the current solution.
     * @return the list of paths, one per vehicle
     * @throws Exception if a subtour is found
     * @throws GRBException if there are problems when accessing the variable x
     */
    protected List<List<Integer>> logVehiclePaths() throws GRBException, Exception{
        List<List<Integer>> paths = new ArrayList<>();
        int firstNodeID = 0;
        
        // Find the path of every vehicle
        for(int v=0; v<instance.getNum_vehicles();v++){
            List<Integer> path = new ArrayList<>();
            path.add(firstNodeID);
            
            for(int i=firstNodeID; i<instance.getNum_nodes();i++){
                for(int j=firstNodeID;j<instance.getNum_nodes();j++){
                    if(x[v][i][j].get(GRB.DoubleAttr.X)==1.0){
                        // Look for subtours
                        if(path.contains(j)){
                            // We've found a subtour
                            throw new Exception("Subtour in the path for vehicle "
                                    +v+"!\n Path: "+path.toString());
                        }
                        else{
                            path.add(j);
                            i=j;
                            j=firstNodeID;
                        }
                    }
                }
            }
            paths.add(path);
        }
        
        // Log results
        env.message("\nVehicle paths in solution: ");
        for(int v=0; v<paths.size();v++){
            env.message("\nv"+v+": "+paths.get(v).toString()+"\n");
        }
        
        return paths;
    }
    
    /**
     * Logs for every visited cluster all of its nodes and whether they have been visited or not. 
     * @return the list of strings describing the clusters
     * @throws GRBException if there are problems while retrieving variables
     * @throws Exception if there are problems while retrieving the nodes in the cluster
     */
    protected List<String> logVisitedClusters() throws GRBException, Exception{
        List<String> ret = new ArrayList<>();
        int countVisited = 0;
        
        env.message("\nList of visited clusters (a * indicates that the node has been visited):");
        for(int c=0; c<instance.getNum_clusters();c++){
            StringBuffer line=new StringBuffer("Cluster "+c+": [");
            
            if(y[c].get(GRB.DoubleAttr.X)!=0){
                countVisited++;
                for(int n : instance.getClusterNodeIDs(c)){
                    line.append(n);
                    if(isVisited(n)){
                        line.append("*");
                        line.append(z[findPreviousNodeInSolution(n)][n].get(GRB.DoubleAttr.X));
                    }
                    
                    line.append(" ");
                }
                line.append("]");
                ret.add(line.toString());
                env.message("\n"+line);
            }
        }
        env.message("\nEnd of the list. Visited clusters: "+countVisited+"\n");
        
        return ret;
    }
    
    /**
     * From the current model, get all the clusters currently in the solution
     * @return a list of clusters in solution
     * @throws GRBException if there are problems while retrieving the solution
     */
    protected List<Cluster> getClustersInCurrentModelSolution() throws GRBException{
        List<Cluster> ret = new ArrayList<>();
        
        for(int c=0; c<instance.getNum_clusters();c++){
            if(y[c].get(GRB.DoubleAttr.X)!=0){
                ret.add(instance.getCluster(c));
            }
        }
        
        return ret;
    }
    
    /**
     * From the specified model, get all the clusters currently in the solution
     * @param inputModel the model to retrieve information from
     * @return a list of clusters in solution
     * @throws GRBException if there are problems while retrieving the solution
     */
    protected List<Cluster> getClustersInCurrentModelSolution(GRBModel inputModel) throws GRBException{
        List<Cluster> ret = new ArrayList<>();
        
        for(int c=0; c<instance.getNum_clusters();c++){
            String varName = y[c].get(GRB.StringAttr.VarName);
            if(inputModel.getVarByName(varName).get(GRB.DoubleAttr.X) != 0){
                ret.add(instance.getCluster(c));
            }
        }
        
        return ret;
    }
    
    /**
     * Finds the ID of the previous node in the path described by the solution.
     * @param node the ID of the node we want to find the predecessor of in the solution path
     * @return the ID of the previous node
     * @throws GRBException if there are problems while retrieving the value of the z variable
     */
    protected int findPreviousNodeInSolution(int node) throws GRBException{
        int previousNode = 0;
        
        for(int i=0;i<instance.getNum_nodes();i++){
            if(z[i][node].get(GRB.DoubleAttr.X)!=0){
                previousNode = i;
                break;
            }
        }
        
        return previousNode;
    }
    
    /**
     * Returns true if the given node has been visited.
     * @param node the node to check
     * @return true if the node has been visited
     * @throws GRBException if there are problems while retrieving the value of the variable x 
     */
    protected boolean isVisited(int node) throws GRBException{
        boolean ret = false;
        
        for(int v=0; v<instance.getNum_vehicles(); v++){
            for(int i=0; i<instance.getNum_nodes(); i++){
                if(x[v][i][node].get(GRB.DoubleAttr.X)!=0){
                    ret=true;
                    break;
                }
            }
            if(ret) break;
        }
        
        return ret;
    }
    
    /**
     * This method adds all heuristic constraints that tighten the relaxed model.
     * @throws GRBException if anything goes wrong
     */
    protected void toggleHeuristicConstraintsOn() throws GRBException, Exception{
        // Some useful constants for constraint definition
        int firstNodeID = 0;
        int lastNodeID = instance.getNum_nodes()-1;
        //Note: getNum_nodes() returns nmax. nmax-1 is the final node. 0 is the initial node.
        
        // add all heuristic constraints
        
        // Expression 13 (HEURISTIC):
        // All arcs that go from any node (but the last) of any cluster to
        // the final deposit should be removed (this makes it impossible for
        // a vehicle to "wait" inside of a cluster).
        for(int c=0; c<instance.getNum_clusters(); c++){
            // Retrieve the list of nodes in the cluster
            List<Integer> nodesInCluster = instance.getClusterNodeIDs(c);
            for(int i=0; i<nodesInCluster.size()-1;i++){
                for(int v=0; v < this.instance.getNum_vehicles(); v++){
                    heuristicConstraints.add( model.addConstr(x[v][nodesInCluster.get(i)][lastNodeID], GRB.EQUAL, 0.0, "c13_c"+c+"_n"+nodesInCluster.get(i)+"_v"+v));
                }
            }
        }
        
        // Expression 15:
        /**
         * -Streak optimization heuristic-
         * A streak is a set of nodes in a cluster that can be served all at
         * once by the same vehicle. We want to impose that if a vehicle enters
         * a streak in a cluster, it can not exit the cluster until its
         * current streak has not been fully served.
         * To do so, for each cluster we get its ordered list of services.
         * Then, for each vehicle, we look for every streak it can serve in that
         * particular cluster.
         * At this point, for each streak of a specific vehicle, we remove
         * all arcs that go from a node in the middle of a streak to another
         * node which is different from the next one in the streak.
         * Arcs to the initial node of a streak and from the final node of a
         * streak are left unaltered.
         * This set of constraints might not work for every instance, thus
         * it's an heuristic.
         */

        // For each cluster
        for(int c=0; c<instance.getNum_clusters(); c++){
            Cluster cluster = instance.getCluster(c);
            // For each vehicle
            for(int v=0; v<instance.getNum_vehicles();v++){
                Vehicle vehicle = instance.getVehicle(v);
                // Find all streaks for vehicle in cluster
                List<Streak> streaks = cluster.getStreaks(vehicle);

                // For each streak for this vehicle in the cluster
                for(Streak streak : streaks){
                    // If the streak has more than two nodes
                    if(streak.size()>1){
                        // For each node in the streak but the last one
                        for(int s=0;s<streak.size()-1;s++){
                            Node currentNode = streak.get(s);
                            Node nextNode = streak.get(s+1);

                            // Remove all arcs that go from the current node
                            // to a node which is not the next one in the
                            // streak, for the current vehicle
                            // Note: less-than-or equal makes us avoid exiting
                            // a streak, not even to go to the last node
                            for(int i=0;i<=lastNodeID; i++){
                                    if(i!=nextNode.getId()){
                                        heuristicConstraints.add(model.addConstr(x[v][currentNode.getId()][i], GRB.EQUAL, 0.0, "c15_c"+c+"_arc("+currentNode.getId()+","+i+")_v"+v));
                                    }
                            }
                        }
                    }
                }
            }
        }
        
        model.update();
    }
    
    /**
     * This method removes all heuristic constraints that tighten the relaxed model.
     * @throws GRBException if anything goes wrong
     */
    protected void toggleHeuristicConstraintsOff() throws GRBException{
        for(GRBConstr c : heuristicConstraints){
            model.remove(c);
        }
        heuristicConstraints.clear();
        model.update();
    }
    
//    public List<String[]> csvGetGeneralParameters(){
//        List<String[]> output = new ArrayList<>();
//        
//        // This is an Orienteering parameter
//        String [] runName = {this.instance.getName()+"",LocalDateTime.now().toString()};
//        String [] outputFolderPath = {"Path to the output folder",this.outputFolderPath};
//        String [] logFilePath = {"Path to the log file",this.logFilePath};
//        String [] modelPath = {"Path to the model (instance) file",this.modelPath};
//        String [] timeLimit = {"Global time limit for MIPS",this.timeLimit+""};
//        String [] numThreads = {"Number of threads",this.numThreads+""};
//        
//        output.add(runName);
//        output.add(outputFolderPath);
//        output.add(logFilePath);
//        output.add(modelPath);
//        output.add(timeLimit);
//        output.add(numThreads);
//        
//        return output;
//    }

    @Override
    protected Boolean doInBackground() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}