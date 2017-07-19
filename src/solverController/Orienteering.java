/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverController;

import gurobi.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import solverModel.Cluster;
import solverModel.InstanceCTOPWSS;
import solverModel.InstanceCTOPWSSReader;
import solverModel.Node;
import solverModel.Streak;
import solverModel.Vehicle;

/**
 * Class to solve an instance of the Orienteering problem. It offers a static
 * main method to run.
 *
 * @author Frash
 */
public class Orienteering extends SwingWorker<Boolean, OptimizationStatusMessage> {

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
     * Constant value for the maximum number of threads used by the Gurobi
     * solver
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
     * Gurobi variable x_v_i_j. x[v][i][j] determines if arc (i,j) is traversed
     * by vehicle v. It's boolean and plays no role in the objective function.
     */
    protected GRBVar[][][] x;

    /**
     * Gurobi variable y_c. y[c] is boolean and determines whether cluster c has
     * been completely served. y[c] is also in the objective function.
     */
    protected GRBVar[] y;

    /**
     * Gurobi variable z[i][j]. z_(i,j) is real non-negative and determines the
     * time of arrival in node j from node i. The upper bound of z is Tmax.
     */
    protected GRBVar[][] z;

    /**
     * Retrieve the Gurobi environment object for this problem.
     *
     * @return the Gurobi environment object for this problem
     */
    public GRBEnv getEnv() {
        return env;
    }

    /**
     * Retrieve the Gurobi model object for this problem.
     *
     * @return the Gurobi model object for this problem
     */
    public GRBModel getModel() {
        return model;
    }

    /**
     * Retrieve the instance object for this problem model.
     *
     * @return the instance object for this problem model.
     */
    public InstanceCTOPWSS getInstance() {
        return instance;
    }
    
    /**
     * This value holds the smallest value found for the objective function of
     * the relaxed model, which works as an upper bound for every integer
     * solution (it speeds up certain feasibility checks)
     */
    protected double minimumObjOfRelaxedModel = Double.MAX_VALUE;

    /**
     * Retrieve the hash of the instance file for this problem model.
     *
     * @return the hash string of the instance file
     */
    public String getInstanceHash() {
        return instanceHash;
    }

    /**
     * Constructor. Setups the Orienteering problem environment, model and
     * instance.
     *
     * @param outputFolderPath path to the folder that will hold any output
     * files produced by the solvers
     * @param logFilePath name of the gurobi logfile to keep track of the
     * solution process
     * @param modelPath path to the file containing the instance description
     * @param timeLimit maximum time for solving the problem (in seconds)
     * @param numThreads number of threads used to solve the problem. Suggested
     * value is 1 for real core (hyperthreading doesn't improve Gurobi's speed)
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
     * Constructor. Setups the Orienteering problem environment, model and
     * instance variables starting from a modelPath and an
     * OrienteeringPropertiesBean containing all the solver parameters.
     *
     * @param modelPath path to the file containing the instance description
     * @param opb an OrienteeringPropertiesBean containing all the solver
     * parameters
     * @throws Exception if anything goes wrong
     */
    public Orienteering(String modelPath, OrienteeringPropertiesBean opb) throws Exception {
        // Setup solver run specific parameters
        this.orienteeringProperties = opb;

        // Setup instance specific parameters
        this.modelPath = modelPath;
        this.logFilePath = opb.getOutputFolderPath() + File.separator + instanceNameFromPath(modelPath) + LOG_FILE_EXTESION;
        this.heuristicConstraints = new ArrayList<>();
        this.constraint8 = new ArrayList<>();
        this.constraint8Variables = new ArrayList<>();

        // Go for preprocessing
        instancePreprocessing();
    }

    /**
     * Gets the name of the instance from the given instance file path
     *
     * @param path path to the instance file
     * @return the name of the instance file, without any extension
     */
    private static String instanceNameFromPath(String path) {
        // Try to get filename and instance name from the path
        Path instancePath = Paths.get(path);
        String filename = instancePath.getFileName().toString();
        String instanceName = filename;
        if (filename.lastIndexOf('.') != -1) {
            instanceName = filename.substring(0, filename.lastIndexOf('.'));
        }

        return instanceName;
    }

    /**
     * Constructor. Setups the Orienteering problem environment, model and
     * instance starting from a previously existing Orienteering object.
     *
     * @param o a previously existing orienteering object.
     * @throws Exception if anything goes wrong
     */
    public Orienteering(Orienteering o) throws Exception {
        this.orienteeringProperties = o.orienteeringProperties;
        this.constraint8 = o.constraint8;
        this.constraint8Variables = o.constraint8Variables;
        this.env = o.getEnv();
        this.heuristicConstraints = o.heuristicConstraints;
        this.instance = o.getInstance();
        this.instanceHash = o.getInstanceHash();
        this.logFilePath = o.logFilePath;
        this.model = o.getModel();
        this.modelPath = o.modelPath;
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
     * Preprocess the instance file and serialize the resulting Orienteering
     * object
     *
     * @param logname name of the gurobi logfile to keep track of the solution
     * process
     * @param modelPath path to the file containing the instance description
     * @throws Exception
     */
    private void instancePreprocessing()
            throws Exception {
        // Read the instance file from text
        this.instance = InstanceCTOPWSSReader.read(modelPath);

        // Setup the model's variables, constraints and objective function
        this.setupEnvironment(logFilePath);

        // Setup the log redirector to redirect the log to stdout
        this.logRedirector = new LogRedirector(logFilePath);

        // Start redirecting the log to the output
        logRedirector.execute();
        //SwingUtilities.invokeLater(logRedirector);

        //Try to serialize the produced constraints
        model.write(orienteeringProperties.getOutputFolderPath() + File.separator + instance.getName() + ".lp");
    }

    /**
     * Returns true if the arc i,j is a noose (a loop on the same node)
     *
     * @param i ID of the first node in the arc
     * @param j ID of the second node in the arc
     * @return true if (i,j) is a noose
     */
    private static boolean isNoose(int i, int j) {
        return i == j;
    }

    /**
     * Setups the Orienteering problem model and environment. This function will
     * also populate the model with all the necessary constraints, variables,
     * the target function and the objective (maximize or minimize).
     *
     * @param logname name of the gurobi logfile to keep track of the solution
     * process
     * @throws Exception if there are general problems
     */
    private void setupEnvironment(String logname)
            throws Exception {
        try {
            this.env = new GRBEnv(logname);

            this.env.set(GRB.DoubleParam.TimeLimit, orienteeringProperties.getTimeLimit());
            this.env.set(GRB.IntParam.Threads, orienteeringProperties.getNumThreads());
            this.env.set(GRB.IntParam.LogToConsole, 0);
            this.env.set(GRB.IntParam.OutputFlag, 1);
            this.model = new GRBModel(this.env);

            // Some useful constants for constraint definition
            int firstNodeID = 0;
            int lastNodeID = instance.getNum_nodes() - 1;
            //Note: getNum_nodes() returns nmax. nmax-1 is the final node. 0 is the initial node.

            /**
             * *********
             * VARIABLES 
            ***********
             */
            // x_v(i,j) determines if arc (i,j) is traversed by vehicle v
            // It's boolean, but plays no role in the objective function
            // Don't include nooses
            // Don't include arcs exiting node lastNodeID
            // Don't include arcs entering node firstNodeID
            this.x = new GRBVar[instance.getNum_vehicles()][instance.getNum_nodes()][instance.getNum_nodes()];
            for (int v = 0; v < instance.getNum_vehicles(); v++) {
                for (int i = firstNodeID; i < instance.getNum_nodes(); i++) {
                    for (int j = firstNodeID; j < instance.getNum_nodes(); j++) {
                        if (isNoose(i, j) || j == firstNodeID || i == lastNodeID) {
                            // Exclude nooses and illegal arcs
                            x[v][i][j] = model.addVar(0.0, 0.0, 0.0, GRB.BINARY, "x_v" + v + "_arc(" + i + "," + j + ")");
                        } else {
                            x[v][i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x_v" + v + "_arc(" + i + "," + j + ")");
                        }

                    }
                }
            }

            // y_c is boolean and determines whether cluster c has been completely served
            // y_c is also in the objective function (every y_c is multiplied by
            // the profit generated by cluster c upon completion)
            this.y = new GRBVar[instance.getNum_clusters()];
            for (int cluster = 0; cluster < instance.getNum_clusters(); cluster++) {
                y[cluster] = model.addVar(0.0, 1.0, instance.getProfit(cluster), GRB.BINARY, "y_c" + cluster);
            }

            // z_(i,j) is real non-negative and determines the time of arrival in node j from node i
            this.z = new GRBVar[instance.getNum_nodes()][instance.getNum_nodes()];
            for (int i = 0; i < instance.getNum_nodes(); i++) {
                for (int j = 0; j < instance.getNum_nodes(); j++) {
                    //z[i][j] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "z_("+i+","+j+")");
                    z[i][j] = model.addVar(0.0, instance.getTmax(), 0.0, GRB.CONTINUOUS, "z_(" + i + "," + j + ")");
                }
            }

            // Integrate new variables
            model.update();

            /**
             * ******************************************
             * EXPRESSIONS (obj function and constraints) 
            ********************************************
             */
            // Expression 1: Set objective function - we did it when we added y_c
            model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);

            // Expression 2: ensures that exactly vmax vehicles exit node 0 and
            //               vmax vehicles arrive at node nmax + 1
            for (int v = 0; v < instance.getNum_vehicles(); v++) {
                GRBLinExpr expr2a = new GRBLinExpr();
                GRBLinExpr expr2b = new GRBLinExpr();
                for (int i = firstNodeID; i < instance.getNum_nodes(); i++) {
                    expr2a.addTerm(1.0, x[v][firstNodeID][i]);
                    expr2b.addTerm(1.0, x[v][i][lastNodeID]);
                }
                model.addConstr(expr2a, GRB.EQUAL, 1.0, "c2a(v" + v + ")");
                model.addConstr(expr2b, GRB.EQUAL, 1.0, "c2b(v" + v + ")");
            }

            // Expression 3: ensures that node i of cluster c can be visited by
            //               a single vehicle and also impose that, if a node
            //               in a cluster is visited by a vehicle, every other
            //               node in the same cluster has to be visited by a
            //               vehicle. If every node in a cluster c is visited,
            //               variable y_c takes value 1
            for (int c = 0; c < instance.getNum_clusters(); c++) {
                // Retrieve the list of nodes in the cluster
                List<Integer> nodesInCluster = instance.getClusterNodeIDs(c);

                for (int i : nodesInCluster) {
                    GRBLinExpr expr3a = new GRBLinExpr();
                    GRBLinExpr expr3b = new GRBLinExpr();
                    for (int v = 0; v < instance.getNum_vehicles(); v++) {
                        for (int j = firstNodeID; j < instance.getNum_nodes(); j++) {
                            expr3a.addTerm(1.0, x[v][i][j]);
                            expr3b.addTerm(1.0, x[v][j][i]);
                        }
                    }
                    model.addConstr(expr3a, GRB.EQUAL, y[c], "c3a(c" + c + ",i" + i + ")");
                    model.addConstr(expr3b, GRB.EQUAL, y[c], "c3b(c" + c + ",i" + i + ")");
                }
            }

            // Expression 4: ensures that if a vehicle v arrives in node i,
            //               the same vehicle leaves node i
            for (int v = 0; v < instance.getNum_vehicles(); v++) {
                for (int i = firstNodeID + 1; i < lastNodeID; i++) {
                    GRBLinExpr expr4a = new GRBLinExpr();
                    GRBLinExpr expr4b = new GRBLinExpr();
                    for (int j = firstNodeID; j < instance.getNum_nodes(); j++) {
                        expr4a.addTerm(1.0, x[v][i][j]);
                        expr4b.addTerm(1.0, x[v][j][i]);
                    }
                    model.addConstr(expr4a, GRB.EQUAL, expr4b, "c4a(v" + v + ",node" + i + ")");
                }
            }

            // Expression 5: ensure that, if a vehicle v visits node j immediately
            //               after node i, the time elapsed between the arrival
            //               in node i and the arrival in node j is equal to the
            //               time ti needed to provide the service required by
            //               node i plus the travel time t_(i,j)
            //               between node i and node j
            for (int i = firstNodeID + 1; i < lastNodeID; i++) {
                // Left hand side
                GRBLinExpr expr5a = new GRBLinExpr();
                for (int j = firstNodeID; j < instance.getNum_nodes(); j++) {
                    expr5a.addTerm(1.0, z[i][j]);
                    expr5a.addTerm(-1.0, z[j][i]);
                }

                // Right hand side
                GRBLinExpr expr5b = new GRBLinExpr();
                for (int v = 0; v < instance.getNum_vehicles(); v++) {
                    for (int j = firstNodeID; j < instance.getNum_nodes(); j++) {
                        expr5b.addTerm(instance.getDistance(i, j) + instance.getServiceDuration(i), x[v][i][j]);
                    }
                }
                // Add constraint
                model.addConstr(expr5a, GRB.EQUAL, expr5b, "c5(node" + i + ")");
            }

            // Expression 6: serves the same purpose of constraints (5), but
            //               deal with the special case of the starting node.
            //               In this case, the time z_(i,j) required to reach
            //               any node i from the starting node 0 is equal to the
            //               travel time t_(0,i) between node 0 and node i
            for (int i = firstNodeID + 1; i < instance.getNum_nodes(); i++) {
                GRBLinExpr expr6 = new GRBLinExpr();
                for (int v = 0; v < instance.getNum_vehicles(); v++) {
                    expr6.addTerm(instance.getDistance(firstNodeID, i), x[v][firstNodeID][i]);
                }
                model.addConstr(z[firstNodeID][i], GRB.EQUAL, expr6, "c6(node" + i + ")");
            }

            // Expression 7: ensure that vehicle v can visit node i if and
            //               only if it is able to provide the service required
            //               by the node
            for (int v = 0; v < instance.getNum_vehicles(); v++) {
                for (int i = firstNodeID + 1; i < lastNodeID; i++) {

                    // Left hand side
                    GRBLinExpr expr7a = new GRBLinExpr();
                    for (int j = firstNodeID; j < instance.getNum_nodes(); j++) {
                        expr7a.addTerm(1.0, x[v][i][j]);
                        expr7a.addTerm(1.0, x[v][j][i]);
                    }

                    // Right hand side
                    GRBLinExpr expr7b = new GRBLinExpr();
                    expr7b.addConstant(2.0 * instance.hasSkill(v, instance.getNodeService(i)));

                    // Add constraint 7
                    model.addConstr(expr7a, GRB.LESS_EQUAL, expr7b, "c7(node" + i + ",v" + v + ")");
                }
            }

            // Expression 8: ensure that, given a pair of nodes (i; j),
            //               variable z_(i,j) can take a value greater than 0
            //               if and only if there is a vehicle that travels from
            //               node i to node j
            for (int i = firstNodeID; i < instance.getNum_nodes(); i++) {
                for (int j = firstNodeID; j < instance.getNum_nodes(); j++) {
                    GRBLinExpr expr8 = new GRBLinExpr();
                    List<GRBVar> localConstr8Var = new ArrayList<>();
                    for (int v = 0; v < instance.getNum_vehicles(); v++) {
                        expr8.addTerm(instance.getTmax(), x[v][i][j]);
                        localConstr8Var.add(x[v][i][j]);
                    }

                    // Add the constraint to the model and save it for later use
                    // This is one constraint for every z[i][j]
                    this.constraint8.add(model.addConstr(z[i][j], GRB.LESS_EQUAL, expr8, "c8_arc(" + i + "," + j + ")"));

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
            for (int c = 0; c < instance.getNum_clusters(); c++) {
                // Retrieve the list of nodes in the cluster
                List<Integer> nodesInCluster = instance.getClusterNodeIDs(c);

                for (int i : nodesInCluster) {
                    double d_i = instance.getServiceDuration(i);
                    for (int j : nodesInCluster) {
                        // Left hand side
                        GRBLinExpr expr9a = new GRBLinExpr();
                        // Right hand side
                        GRBLinExpr expr9b = new GRBLinExpr();

                        // Stores the precedence between nodes i and j
                        int w_i_j = instance.getPrecedence(i, j);

                        for (int k = firstNodeID; k < instance.getNum_nodes(); k++) {
                            // First term of the left hand side    
                            expr9a.addTerm(w_i_j, z[k][i]);
                            for (int v = 0; v < instance.getNum_vehicles(); v++) {
                                // Second term of the left hand side
                                expr9a.addTerm(w_i_j * d_i, x[v][k][i]);
                            }

                            // Right hand side
                            expr9b.addTerm(1.0, z[k][j]);
                        }
                        // I previosuly added the constraint here
                        model.addConstr(expr9a, GRB.LESS_EQUAL, expr9b, "c9_c" + c + "_arc(" + i + "," + j + ")");
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
            for (int c = 0; c < instance.getNum_clusters(); c++) {
                // Retrieve the list of nodes in the cluster
                List<Integer> nodesInCluster = instance.getClusterNodeIDs(c);
                for (int i = 1; i < nodesInCluster.size(); i++) {
                    for (int v = 0; v < this.instance.getNum_vehicles(); v++) {
                        model.addConstr(x[v][firstNodeID][nodesInCluster.get(i)], GRB.EQUAL, 0.0, "c12_c" + c + "_n" + nodesInCluster.get(i) + "_v" + v);
                    }
                }
            }

            // Expression 14:
            // All arcs inside of a cluster that go in any direction but the one
            // specified by precedence should be removed
            for (int c = 0; c < instance.getNum_clusters(); c++) {
                // Retrieve the list of nodes in the cluster
                List<Integer> nodesInCluster = instance.getClusterNodeIDs(c);
                for (int v = 0; v < this.instance.getNum_vehicles(); v++) {
                    for (int i = 0; i < nodesInCluster.size(); i++) {
                        for (int j = 0; j < nodesInCluster.size(); j++) {
                            if (i != j && j != i + 1) {
                                model.addConstr(x[v][nodesInCluster.get(i)][nodesInCluster.get(j)], GRB.EQUAL, 0.0, "c14_c" + c + "_arc(" + nodesInCluster.get(i) + "," + nodesInCluster.get(j) + ")_v" + v);

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
     *
     * @throws GRBException if anything goes wrong with the disposal
     */
    public void cleanup() throws GRBException {
        model.dispose();
        env.dispose();
    }

    public static void main(String[] args) {
        int SOLVE_MIPS = 0;
        int SOLVE_RELAXED = 1;
        int SOLVE_ALNS = 2;

        System.out.println("Working dir: " + System.getProperty("user.dir"));
        try {
            String modelPath = "Instance0.txt";

            OrienteeringPropertiesBean opb = new OrienteeringPropertiesBean();
            opb.setForceHeuristicConstraints(false);
            opb.setNumThreads(DEFAULT_NUMTHREADS);
            opb.setOutputFolderPath("");
            opb.setTimeLimit(1 * 60.0);

            Orienteering o = new Orienteering(
                    modelPath,
                    opb
            );

            // Select the solver to use
            int solutionType = SOLVE_ALNS;

            // Must set LazyConstraints parameter when using lazy constraints
            // o.getModel().set(GRB.IntParam.LazyConstraints, 1);
            if (solutionType == SOLVE_RELAXED) {
                o.optimizeRelaxed();
            } else if (solutionType == SOLVE_MIPS) {
                o.optimizeMIPS();
            } else if (solutionType == SOLVE_ALNS) {
                // Use the controller to test ALNS!
            }
            // Dispose of all used variables
            o.cleanup();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    /**
     * Optimize the relaxation of the model provided.
     *
     * @throws GRBException if any Gurobi operation goes wrong
     * @throws Exception if there are problems while toggling the constraints
     */
    public void optimizeRelaxed() throws GRBException, Exception {
        // Reset the model to an unsolved state
        model.reset();

        // Check if the user wants to use heuristic constraints
        if (orienteeringProperties.isForceHeuristicConstraints()) {
            toggleHeuristicConstraintsOn();
        }

        GRBModel relaxedModel = model.relax();

        // Optimize the relaxed model
        relaxedModel.optimize();

        // Save the relaxed solution to file
        relaxedModel.write(orienteeringProperties.getOutputFolderPath() + File.separator + instance.getName() + "_relaxed.sol");

        // Dispose of the relaxed model
        relaxedModel.dispose();
    }

    /**
     * Update the normal time limit for the Gurobi solver
     *
     * @param newTimeLimit the new time limit in seconds
     * @throws GRBException if there are problems while updating the parameters
     */
    public void updateTimeLimit(double newTimeLimit) throws GRBException {
        this.env.set(GRB.DoubleParam.TimeLimit, newTimeLimit);
        model.update();
    }

    /**
     * Update the number of threads to use with the Gurobi solver
     *
     * @param newNumThreads the new number of threads to use
     * @throws GRBException if there are problems while updating the parameters
     */
    public void updateNumThreads(int newNumThreads) throws GRBException {
        this.env.set(GRB.IntParam.Threads, newNumThreads);
        model.update();
    }

    /**
     * Optimize the provided model using Gurobi's MIPS solver.
     *
     * @throws GRBException if any Gurobi operation goes wrong
     * @throws Exception if there are problems while toggling the constraints
     */
    public void optimizeMIPS() throws GRBException, Exception {
        // Reset the model to an unsolved state
        model.reset();

        // Check if you want to use heuristic constraints
        if (orienteeringProperties.isForceHeuristicConstraints()) {
            toggleHeuristicConstraintsOn();
        }

        // Optimize the model
        model.optimize();

        // Save the solution to file
        writeSolution(model);

        // Get/log the paths of vehicles
        logVehiclePaths(model);

        // Log visited clusters
        logVisitedClusters(model);
    }

    /**
     * Write the solver solution for the current model to a file. The file will
     * be placed in the output folder path specified.
     *
     * @param model the freshly solved model to write the solution of
     * @throws gurobi.GRBException if there are problems while retrieving the
     * solution or writing it to a file
     */
    protected void writeSolution(GRBModel model) throws GRBException {
        model.write(orienteeringProperties.getOutputFolderPath() + File.separator + instance.getName() + ".sol");
    }

    /**
     * Analyzes a freshly solved model and logs, a list
     * of paths for each vehicle from the current solution.
     *
     * @param model the freshly solved model to log the paths of
     * @return the list of paths, one per vehicle
     * @throws Exception if a subtour is found
     * @throws GRBException if there are problems when accessing the variable x
     */
    protected List<List<Integer>> logVehiclePaths(GRBModel model) throws GRBException, Exception {
        List<List<Integer>> paths = new ArrayList<>();
        int firstNodeID = 0;

        // Find the path of every vehicle
        for (int v = 0; v < instance.getNum_vehicles(); v++) {
            List<Integer> path = new ArrayList<>();
            path.add(firstNodeID);

            for (int i = firstNodeID; i < instance.getNum_nodes(); i++) {
                for (int j = firstNodeID; j < instance.getNum_nodes(); j++) {
                    if (model.getVarByName(x[v][i][j].get(GRB.StringAttr.VarName)).get(GRB.DoubleAttr.X) == 1.0) {
                        // Look for subtours
                        if (path.contains(j)) {
                            // We've found a subtour
                            throw new Exception("Subtour in the path for vehicle "
                                    + v + "!\n Path: " + path.toString());
                        } else {
                            path.add(j);
                            i = j;
                            j = firstNodeID;
                        }
                    }
                }
            }
            paths.add(path);
        }

        // Log results
        env.message("\nVehicle paths in solution: ");
        for (int v = 0; v < paths.size(); v++) {
            env.message("\nv" + v + ": " + paths.get(v).toString() + "\n");
        }

        return paths;
    }

    /**
     * Analyzes a freshly solved model and logs, for every visited cluster, all
     * of its nodes and whether they have been visited or not.
     *
     * @param model the freshly solved model to log the paths of
     * @return the list of strings describing the clusters
     * @throws GRBException if there are problems while retrieving variables
     * @throws Exception if there are problems while retrieving the nodes in the
     * cluster
     */
    protected List<String> logVisitedClusters(GRBModel model) throws GRBException, Exception {
        List<String> ret = new ArrayList<>();
        int countVisited = 0;

        env.message("\nList of visited clusters (a * indicates that the node has been visited):");
        for (int c = 0; c < instance.getNum_clusters(); c++) {
            StringBuffer line = new StringBuffer("Cluster " + c + ": [");
            
            if (model.getVarByName(y[c].get(GRB.StringAttr.VarName)).get(GRB.DoubleAttr.X) != 0) {
                countVisited++;
                for (int n : instance.getClusterNodeIDs(c)) {
                    line.append(n);
                    if (isVisited(model, n)) {
                        line.append("*");
                        line.append(
                                model.getVarByName(
                                        z[findPreviousNodeInSolution(model, n)][n].get(GRB.StringAttr.VarName)
                                ).get(GRB.DoubleAttr.X)
                        );
                    }
                    
                    line.append(" ");
                }
                line.append("]");
                ret.add(line.toString());
                env.message("\n" + line);
            }
        }
        env.message("\nEnd of the list. Visited clusters: " + countVisited + "\n");

        return ret;
    }

    /**
     * From the current model, get all the clusters currently in the solution
     *
     * @return a list of clusters in solution
     * @throws GRBException if there are problems while retrieving the solution
     */
    protected List<Cluster> getClustersInCurrentModelSolution() throws GRBException {
        List<Cluster> ret = new ArrayList<>();

        for (int c = 0; c < instance.getNum_clusters(); c++) {
            if (y[c].get(GRB.DoubleAttr.X) != 0) {
                ret.add(instance.getCluster(c));
            }
        }

        return ret;
    }

    /**
     * From the specified model, get all the clusters currently in the solution
     *
     * @param inputModel the model to retrieve information from
     * @return a list of clusters in solution
     * @throws GRBException if there are problems while retrieving the solution
     */
    protected List<Cluster> getClustersInCurrentModelSolution(GRBModel inputModel) throws GRBException {
        List<Cluster> ret = new ArrayList<>();

        for (int c = 0; c < instance.getNum_clusters(); c++) {
            String varName = y[c].get(GRB.StringAttr.VarName);
            if (inputModel.getVarByName(varName).get(GRB.DoubleAttr.X) != 0) {
                ret.add(instance.getCluster(c));
            }
        }

        return ret;
    }

    /**
     * Finds the ID of the previous node in the path described by the solution.
     *
     * @param model the solved model to analyze
     * @param node the ID of the node we want to find the predecessor of in the
     * solution path
     * @return the ID of the previous node
     * @throws GRBException if there are problems while retrieving the value of
     * the z variable
     */
    protected int findPreviousNodeInSolution(GRBModel model, int node) throws GRBException {
        int previousNode = 0;

        for (int i = 0; i < instance.getNum_nodes(); i++) {
            if (model.getVarByName(z[i][node].get(GRB.StringAttr.VarName)).get(GRB.DoubleAttr.X) != 0) {
                previousNode = i;
                break;
            }
        }

        return previousNode;
    }

    /**
     * Returns true if the given node has been visited in the solution for the
     * given model.
     *
     * @param model the solved model to check
     * @param node the node to check
     * @return true if the node has been visited
     * @throws GRBException if there are problems while retrieving the value of
     * the variable x
     */
    protected boolean isVisited(GRBModel model, int node) throws GRBException {
        boolean ret = false;

        for (int v = 0; v < instance.getNum_vehicles(); v++) {
            for (int i = 0; i < instance.getNum_nodes(); i++) {
                if (model.getVarByName(x[v][i][node].get(GRB.StringAttr.VarName)).get(GRB.DoubleAttr.X) != 0) {
                    ret = true;
                    break;
                }
            }
            if (ret) {
                break;
            }
        }

        return ret;
    }

    /**
     * A counter that keeps track of how many solutions have been excluded from
     * the model.
     */
    protected int excludedSolutionsCounter = 0;

    /**
     * If a solution is thought to be infeasible or bad for the model, we can
     * make sure it's not tested for again by adding constraints that exclude
     * that solution from available ones.
     *
     * @param toExclude the solution to remember not to test for again
     * @return the constraint that was added to the model
     * @throws gurobi.GRBException if updating the model goes wrong
     */
    protected GRBConstr excludeSolutionFromModel(List<Cluster> toExclude) throws GRBException {
        GRBConstr constraint = null;
        
        if (toExclude != null && !toExclude.isEmpty()) {
            int size = toExclude.size();

            // Create the left hand side of the expression
            GRBLinExpr lhs = new GRBLinExpr();
            for (Cluster c : toExclude) {
                lhs.addTerm(1.0, y[c.getId()]);
            }
            
            constraint = model.addConstr(lhs, GRB.LESS_EQUAL, (double) size - 1.0, "Supposedly_Infeasible_" + excludedSolutionsCounter++);
            model.update();
            model.write(orienteeringProperties.getOutputFolderPath() + File.separator + instance.getName() + "_constrained.lp");
        }
        
        return constraint;
    }
    
    /**
     * Add a constraint to exclude a specific solution from a given model.
     *
     * @param toExclude the solution to remember not to test for again
     * @param model the model to remove the solution from
     * @return the constraint that was added to the model
     * @throws gurobi.GRBException if updating the model goes wrong
     */
    protected GRBConstr excludeSolutionFromModel(List<Cluster> toExclude, GRBModel model) throws GRBException {
        GRBConstr constraint = null;
        
        if (toExclude != null && !toExclude.isEmpty()) {
            int size = toExclude.size();

            // Create the left hand side of the expression
            GRBLinExpr lhs = new GRBLinExpr();
            for (Cluster c : toExclude) {
                lhs.addTerm(1.0, y[c.getId()]);
            }
            
            constraint = model.addConstr(lhs, GRB.LESS_EQUAL, (double) size - 1, "Excluded_Solution_" + System.currentTimeMillis());
            model.update();
        }
        
        return constraint;
    }

    /**
     * This method adds all heuristic constraints that tighten the relaxed
     * model.
     *
     * @throws GRBException if anything goes wrong
     */
    protected void toggleHeuristicConstraintsOn() throws GRBException, Exception {
        setSpecificHeuristicConstraints(allHeuristicConstraints);
    }

    /**
     * This method removes all heuristic constraints that tighten the relaxed
     * model.
     *
     * @throws GRBException if anything goes wrong
     */
    protected void toggleHeuristicConstraintsOff() throws GRBException {
        for (GRBConstr c : heuristicConstraints) {
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

    /**
     * This variable holds the objective value as computed by the last
     * feasibility check. Value is -1 if the last check was infeasible.
     */
    protected double objectiveValueFromLastFeasibilityCheck = -1.0;

    /**
     *
     * Use Gurobi to check whether the proposed solution is feasible or not for
     * this model. If the solution is infeasible, a constraint will be added to
     * remove this solution from the pool, but solution information data might
     * (such as variable state) might not be available for future calls to the
     * model.
     *
     * <br>The last objective value is however available in the variable
     * <code>objectiveValueFromLastFeasibilityCheck</code>.
     *
     * @param proposedSolution the solution we want to test
     * @param log true will produce a visible log
     * @param maxMIPSNodes maximum number of MIPS nodes to solve in a
     * feasibility check
     * @return true is the solution is feasible
     * @throws gurobi.GRBException if there are problems while handling the
     * model
     * @throws Exception if there are other problems
     */
    protected boolean testSolutionForFeasibility(
            List<Cluster> proposedSolution,
            boolean log,
            double maxMIPSNodes
    ) throws GRBException, Exception {
        boolean isFeasible = testSolution(this.model, proposedSolution, log, maxMIPSNodes);

        // If the solution was infeasible for the current model, remove it
        // by adding new constraints to this model
        if (isFeasible) {
            // Save the objective value for later use by other methods.
            objectiveValueFromLastFeasibilityCheck = model.get(GRB.DoubleAttr.ObjVal);
        } else {
            excludeSolutionFromModel(proposedSolution); //DEBUG: to test
            
            // Set an "error" objective value
            objectiveValueFromLastFeasibilityCheck = -1.0;
        }

        return isFeasible;
    }

    /**
     * Use Gurobi to check whether the proposed solution is feasible or not for
     * the specified model.
     *
     * @param model the model to test the solution on
     * @param proposedSolution the solution we want to test
     * @param log true will produce a visible log
     * @param maxMIPSNodes maximum number of MIPS nodes to solve in a
     * feasibility check
     * @return true is the solution is feasible
     * @throws gurobi.GRBException if there are problems while handling the
     * model
     * @throws Exception if there are other general problems
     */
    protected boolean testSolution(
            GRBModel model,
            List<Cluster> proposedSolution,
            boolean log,
            double maxMIPSNodes
    ) throws GRBException, Exception {

        boolean isFeasible = false;
        
        // See if the sum of profits in the proposed solution is above the
        // objective of the relaxed. If it is, the solution is clearly
        // infeasible
        double profitForSolution = proposedSolution.stream().mapToDouble(c -> c.getProfit()).sum();
        if(profitForSolution <= this.minimumObjOfRelaxedModel){
            // If the basic check succeeds, proceed with the Gurobi check
            
            // Reset the model to an unsolved state, this will allow us to test our solutions freely
            model.reset();

            // Clear the solution in the model: no cluster will be choseable at the beginning
            clearSolution(model);

            // Place the selected clusters in solution
            putInSolution(model, proposedSolution);

            // Setting up the callback
            model.setCallback(new FeasibilityCallback(maxMIPSNodes));

            // Test the solution
            model.optimize();
            if (model.get(GRB.IntAttr.SolCount) > 0) {
                isFeasible = true;
            }
        }
        
        if (log) {
            env.message("\nTesting solution with clusters: [");

            proposedSolution.forEach(c -> {
                try {
                    env.message(c.getId() + " ");
                } catch (GRBException ex) {
                    Logger.getLogger(Orienteering.class.getName()).log(Level.SEVERE, null, ex);
                }
            });

            if (isFeasible) {
                env.message("]: FEASIBLE integer solution found!");
                this.logVisitedClusters(model);
                this.logVehiclePaths(model);
            } else {
                env.message("]: INFEASIBLE.\n\n");
            }
        }

        // Resetting the callback
        model.setCallback(null);

        return isFeasible;
    }
    
    /**
     * A list containing all available heuristic IDs
     * NOTE: Update it every time you add an heuristic contraint!
     */
    protected List<Integer> allHeuristicConstraints = Arrays.asList(0, 1, 2, 3);
    
    /**
     * Sets all the heuristic constraints specified in the list of constraints for
     * the current model.
     * 
     * @param toSet a list of constraint IDs
     * @throws gurobi.GRBException if problems arise while handling the model
     * @throws Exception if there problems arise while handling the instance
     */
    public void setSpecificHeuristicConstraints(List<Integer> toSet)
            throws GRBException, Exception{
        // Let's start by eventually removing leftover constraints
        toggleHeuristicConstraintsOff();

        // Some useful constants for constraint definition
        int firstNodeID = 0;
        int lastNodeID = instance.getNum_nodes() - 1;
        //Note: getNum_nodes() returns nmax. nmax-1 is the final node. 0 is the initial node.
        
        // Let's set up constraints, one by one
        for(int constraintId : toSet){
            switch(constraintId){
                case 0:
                    // add all heuristic constraints
                    // Expression 13 (HEURISTIC):
                    // All arcs that go from any node (but the last) of any cluster to
                    // the final deposit should be removed (this makes it impossible for
                    // a vehicle to "wait" inside of a cluster).
                    for (int c = 0; c < instance.getNum_clusters(); c++) {
                        // Retrieve the list of nodes in the cluster
                        List<Integer> nodesInCluster = instance.getClusterNodeIDs(c);
                        for (int i = 0; i < nodesInCluster.size() - 1; i++) {
                            for (int v = 0; v < this.instance.getNum_vehicles(); v++) {
                                heuristicConstraints.add(model.addConstr(x[v][nodesInCluster.get(i)][lastNodeID], GRB.EQUAL, 0.0, "hc13_c" + c + "_n" + nodesInCluster.get(i) + "_v" + v));
                            }
                        }
                    }
                    break;
                    
                case 1:
                    // Expression 15:
                    /**
                     * -Streak optimization heuristic- A streak is a set of nodes in a
                     * cluster that can be served all at once by the same vehicle. We want
                     * to impose that if a vehicle enters a streak in a cluster, it can not
                     * exit the cluster until its current streak has not been fully served.
                     * To do so, for each cluster we get its ordered list of services. Then,
                     * for each vehicle, we look for every streak it can serve in that
                     * particular cluster. At this point, for each streak of a specific
                     * vehicle, we remove all arcs that go from a node in the middle of a
                     * streak to another node which is different from the next one in the
                     * streak. Arcs to the initial node of a streak and from the final node
                     * of a streak are left unaltered. This set of constraints might not
                     * work for every instance, thus it's an heuristic.
                     */
                    // For each cluster
                    for (int c = 0; c < instance.getNum_clusters(); c++) {
                        Cluster cluster = instance.getCluster(c);
                        // For each vehicle
                        for (int v = 0; v < instance.getNum_vehicles(); v++) {
                            Vehicle vehicle = instance.getVehicle(v);
                            // Find all streaks for vehicle in cluster
                            List<Streak> streaks = cluster.getStreaks(vehicle);

                            // For each streak for this vehicle in the cluster
                            for (Streak streak : streaks) {
                                // If the streak has more than two nodes
                                if (streak.size() > 1) {
                                    // For each node in the streak but the last one
                                    for (int s = 0; s < streak.size() - 1; s++) {
                                        Node currentNode = streak.get(s);
                                        Node nextNode = streak.get(s + 1);

                                        // Remove all arcs that go from the current node
                                        // to a node which is not the next one in the
                                        // streak, for the current vehicle
                                        // Note: less-than-or equal makes us avoid exiting
                                        // a streak, not even to go to the last node
                                        for (int i = 0; i <= lastNodeID; i++) {
                                            if (i != nextNode.getId()) {
                                                heuristicConstraints.add(model.addConstr(x[v][currentNode.getId()][i], GRB.EQUAL, 0.0, "hc15_c" + c + "_arc(" + currentNode.getId() + "," + i + ")_v" + v));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
                    
                case 2:
                    // Expression 17
                    /**
                     * Since we want to put to good use all of our vehicles,
                     * let's make sure that we get at least one cluster for each
                     * vehicle in the instance.
                     */
                    GRBLinExpr lhs17 = new GRBLinExpr();
                    for (int c = 0; c < instance.getNum_clusters(); c++) {
                        lhs17.addTerm(1.0, y[c]);
                    }
                    heuristicConstraints.add(model.addConstr(lhs17, GRB.GREATER_EQUAL, (double) instance.getNum_vehicles(), "hc17"));
                    break;
                
                case 3:
                    // Expression 18
                    /**
                     * By looking at relaxed solutions, we've noticed that they
                     * often feature some arcs for some vehicles which are taken
                     * even if their z value happens to be 0.
                     * To avoid this, we've thought of this constraint.
                     * 
                     * It means that for every arc, the x value of the vehicle
                     * crossing that arc must be less than the z for that arc,
                     * divided by the service with the smallest (non null)
                     * service time in the instance (or a very small number)
                     */
                    double scaleFactor = 1 / model.get(GRB.DoubleParam.OptimalityTol);
                    for(int i = firstNodeID; i <= lastNodeID; i++){
                        for(int j = firstNodeID; j <= lastNodeID; j++){
                            // For each arc
                            GRBLinExpr lhs18 = new GRBLinExpr();
                            for(int v = 0; v< instance.getNum_vehicles(); v++){
                                lhs18.addTerm(1.0,x[v][i][j]);
                            }
                            GRBLinExpr rhs18 = new GRBLinExpr();
                            rhs18.addTerm(scaleFactor, z[i][j]);
                            
                            heuristicConstraints.add(model.addConstr(lhs18, GRB.LESS_EQUAL, rhs18, "hc18_"+i+"_"+j));
                        }
                    }
            }

        }
        model.update();
    }
    
    /**
     * Tests the given heuristic constraints on the current model to see if the
     * constrained model is still feasible or not.
     * 
     * @param toTest a list of heuristic constraint IDs to test
     * @param guineaPigSolution a solution to test the model for feasibility with
     * @param maxMIPSNodes maximum number of MIPS nodes to solve in a
     * feasibility check
     * @return true if the constrained model is still feasible
     * @throws gurobi.GRBException if there are problems while handling the model
     * @throws Exception if other problems arise
     */
    public boolean testConstraints(List<Integer> toTest, List<Cluster> guineaPigSolution, double maxMIPSNodes)
            throws GRBException, Exception{
        toggleHeuristicConstraintsOff();
        setSpecificHeuristicConstraints(toTest);
        boolean isFeasible = testSolution(model, guineaPigSolution, false, maxMIPSNodes);
        return isFeasible;
    }
    
    /**
     * Returns the smallest list of feasible heuristic constraints for the current model.
     * 
     * @param toTest a list of heuristic constraint IDs to test
     * @param guineaPigSolution a FEASIBLE solution to test the model for feasibility with.
     * <b>IMPORTANT:</b> It <b>MUST</b> be feasible, otherwise the results of this method might be problematic!!!
     * @param maxMIPSNodes maximum number of MIPS nodes to solve in a
     * feasibility check
     * @return the smallest list of feasible heuristic constraints for the current model. (Could be an empty list)
     * @throws Exception if anything goes wrong
     */
    public List<Integer> getLargestFeasibleCombinationOfHeuristicConstraints(
            List<Integer> toTest,
            List<Cluster> guineaPigSolution,
            double maxMIPSNodes
    ) throws Exception {
        List<Integer> newConstraints = new ArrayList<>();
        
        // If we're testing an empty set of constraints, return an empty set
        if(toTest != null && !toTest.isEmpty()){
            // Check if the given constraints, all together, are feasible
            boolean areFeasible = testConstraints(toTest, guineaPigSolution, maxMIPSNodes);

            if(!areFeasible){
                // if constraints toTest are not feasible, remove them
                // one by one and keep the largest feasible set of them
                for(Integer c : toTest){
                    List<Integer> toTestNew = new ArrayList<>(toTest);
                    toTestNew.remove(c);
                    List<Integer> subset = getLargestFeasibleCombinationOfHeuristicConstraints(toTestNew, guineaPigSolution, maxMIPSNodes);
                    if(subset.size() > newConstraints.size()){
                        newConstraints = subset;
                    }
                }
            }
            // in case of feasible constraints, return them
            else newConstraints = new ArrayList<>(toTest);
        } // else, if they're empty, return an empty list
        
        return newConstraints;
    }
    
//    /**
//     * Returns the smallest list of feasible heuristic constraints for the current model.
//     * 
//     * @param toTest a list of heuristic constraint IDs to test
//     * @param guineaPigSolution a FEASIBLE solution to test the model for feasibility with.
//     * <b>IMPORTANT:</b> It <b>MUST</b> be feasible, otherwise the results of this method might be problematic!!!
//     * @param maxMIPSNodes maximum number of MIPS nodes to solve in a
//     * feasibility check
//     * @return the smallest list of feasible heuristic constraints for the current model. (Could be an empty list)
//     * @throws GRBException if anything goes wrong
//     * @throws Exception if anything goes wrong
//     */
//    public List<Integer> getLargestFeasibleCombinationOfHeuristicConstraints2(
//            List<Integer> toTest,
//            List<Cluster> guineaPigSolution,
//            double maxMIPSNodes
//    ) throws GRBException, Exception
//    {
//        List<Integer> newConstraints = new ArrayList<>();
//        
//        // If we're testing an empty set of constraints, return an empty set
//        if(toTest != null && !toTest.isEmpty()){
//            // Check if the given constraints, all together, are feasible
//            boolean areFeasible = testConstraints(toTest, guineaPigSolution, maxMIPSNodes);
//            
//            // Test every constraint by itself
//            for(int constr : toTest){
//                List<Integer> combination = new ArrayList<>();
//                combination.add(constr);
//                boolean isFeasible = testConstraints(combination, guineaPigSolution, maxMIPSNodes);
//                if(isFeasible) newConstraints.add(constr);
//            }
//        }
//        
//        boolean areFeasible = testConstraints(newConstraints, guineaPigSolution, maxMIPSNodes);
//        
//        // If the combination is infeasible, signal it with -1
//        // TODO: change -1 to an empty arraylist, this is for debugging purposes
//        if(!areFeasible) newConstraints.add(-1);
//     
//        return newConstraints;
//    }

    /**
     * Adds the selected cluster to the solution.
     *
     * @param c the cluster to put into solution.
     * @throws GRBException if anything goes wrong with setting the upper bound.
     */
    protected void putInSolution(Cluster c) throws GRBException {
        y[c.getId()].set(GRB.DoubleAttr.LB, 1.0);
        y[c.getId()].set(GRB.DoubleAttr.UB, 1.0);
        model.update();
    }

    /**
     * Remove the selected cluster from the solution.
     *
     * @param c the cluster to remove from solution.
     * @throws GRBException
     */
    protected void removeFromSolution(Cluster c) throws GRBException {
        y[c.getId()].set(GRB.DoubleAttr.LB, 0.0);
        y[c.getId()].set(GRB.DoubleAttr.UB, 0.0);
        model.update();
    }

    /**
     * Adds the selected clusters to the solution.
     *
     * @param l the list of clusters to put into solution.
     * @throws GRBException if anything goes wrong with updating the model.
     */
    protected void putInSolution(List<Cluster> l) throws GRBException {
        l.forEach(c -> {
            try {
                y[c.getId()].set(GRB.DoubleAttr.LB, 1.0);
                y[c.getId()].set(GRB.DoubleAttr.UB, 1.0);
            } catch (GRBException ex) {
                Logger.getLogger(Orienteering.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        model.update();
    }

    /**
     * Adds the selected clusters to the solution for the model specified.
     *
     * @param model the model to update
     * @param l the list of clusters to put into solution.
     * @throws GRBException if anything goes wrong with updating the model.
     */
    protected void putInSolution(GRBModel model, List<Cluster> l) throws GRBException {
        l.forEach(c -> {
            try {
                GRBVar var = model.getVarByName("y_c" + c);
                var.set(GRB.DoubleAttr.LB, 1.0);
                var.set(GRB.DoubleAttr.UB, 1.0);
            } catch (GRBException ex) {
                Logger.getLogger(Orienteering.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        model.update();
    }

    /**
     * Remove the selected clusters from the solution.
     *
     * @param l the list of clusters to remove from the solution.
     * @throws GRBException if anything goes wrong with updating the model.
     */
    protected void removeFromSolution(List<Cluster> l) throws GRBException {
        l.forEach(c -> {
            try {
                y[c.getId()].set(GRB.DoubleAttr.LB, 0.0);
                y[c.getId()].set(GRB.DoubleAttr.UB, 0.0);
            } catch (GRBException ex) {
                Logger.getLogger(Orienteering.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        model.update();
    }

    /**
     * Reset the solution: all clusters will be free to be chosen
     *
     * @throws GRBException if setting the bounds goes wrong
     */
    protected void resetSolution() throws GRBException {
        for (int c = 0; c < instance.getNum_clusters(); c++) {
            y[c].set(GRB.DoubleAttr.LB, 0.0);
            y[c].set(GRB.DoubleAttr.UB, 1.0);
        }

        model.update();
    }

    /**
     * Clear the solution: no clusters will be selectable by the solver
     *
     * @throws GRBException if setting the bounds goes wrong
     */
    protected void clearSolution() throws GRBException {
        for (int c = 0; c < instance.getNum_clusters(); c++) {
            y[c].set(GRB.DoubleAttr.LB, 0.0);
            y[c].set(GRB.DoubleAttr.UB, 0.0);
        }
        model.update();
    }

    /**
     * Clear the solution of a specific model: no clusters will be selectable by
     * the solver
     *
     * @param model the model we want to update
     * @throws GRBException if setting the bounds goes wrong
     */
    protected void clearSolution(GRBModel model) throws GRBException {
        for (int c = 0; c < instance.getNum_clusters(); c++) {
            GRBVar var = model.getVarByName("y_c" + c);
            var.set(GRB.DoubleAttr.LB, 0.0);
            var.set(GRB.DoubleAttr.UB, 0.0);
        }
        model.update();
    }
}
