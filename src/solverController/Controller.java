/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import solverModel.ALNS;
import solverModel.ALNSPropertiesBean;
import solverModel.Orienteering;
import solverModel.OrienteeringPropertiesBean;

/**
 * A Controller class to solve batches of orienteering problem instances using
 * specific parameters and solvers.
 * @author Frash
 */
public class Controller {
    // General Purpose Variables
    
    /**
     * A Javabean storing general solver parameters for Orienteering and ALNS.
     */
    private ParametersBean pb;
    
    /**
     * A list of model paths to solve in a batch.
     */
    private List<String> modelPaths;
    
    /**
     * List of possible choices for solvers.
     */
    public enum Solvers {SOLVE_RELAXED, SOLVE_MIPS, SOLVE_ALNS};
    
    /**
     * The solver chosen for the next batch of instances to solve.
     */
    private Solvers solver = Solvers.SOLVE_ALNS;
    
    /**
     * Constructor for the controller class.
     * @param modelPaths list of model paths to solve in a batch
     * @param opb Javabean that holds all general parameters for an Orienteering solver
     * @param apb Javabean that holds all ALNS parameters
     * @throws Exception 
     */
    public Controller(
            List<String> modelPaths,
            OrienteeringPropertiesBean opb,
            ALNSPropertiesBean apb
    ) throws Exception {
        // Setup all parameters
        pb = new ParametersBean(opb, apb);
        
        this.modelPaths = modelPaths;        
    }
    
    public static void main(String[] args) throws Exception {
        // TEST
        List<String> modelPaths = new ArrayList<>();
        modelPaths.add("Instance0.txt");
        
        Controller c = new Controller(
                modelPaths,
                new OrienteeringPropertiesBean(),
                new ALNSPropertiesBean()
        );
        
        c.optimize();
    }
    
    /**
     * Serialize the current parameters bean to a JSON file.
     * The file name will be the one specified in the parameter <code>outputPath</code>.
     * @param outputPath path to the output file with parameters.
     * @throws IOException if there are problems with the output file.
     */
    public void saveParametersToJSON(String outputPath) throws IOException{
        this.pb.serializeToJSON(outputPath);
    }
    
    /**
     * Deserializes the parameters bean specified at the specific path and uses it
     * to initialize the current one for this controller.
     * @param inputPath path to the input JSON file for properties
     * @throws IOException if there are problems with opening the file
     */
    public void loadParametersFromJSON(String inputPath) throws IOException{
        this.pb.deserializeFromJSON(inputPath);
    }

    /**
     * The solver chosen for the next batch of instances to solve
     * @return the solver
     */
    public Solvers getSolver() {
        return solver;
    }

    /**
     * The solver chosen for the next batch of instances to solve
     * @param solver the solver to set
     */
    public void setSolver(Solvers solver) {
        this.solver = solver;
    }
    
    /**
     * Optimizes the batch of instances using the solver specified through the
     * <code>setSolver</code> method.
     */
    public void optimize() throws Exception{
        // Save the chosen parameters to the output folder
        pb.serializeToJSON();
        
        for(String modelPath : modelPaths){
            // Initialize a new Orienteering object starting from the current modelPath
            Orienteering o = new Orienteering(
                    modelPath,
                    pb.getOrienteeringProperties()
            );
            
            // Initialize a new ALNS object starting from the current modelPath
            // and Orienteering object
            ALNS a = new ALNS(
                    o,
                    pb.getALNSproperties()
            );
            
            // Pick a solver and apply it
            switch(solver){
                case SOLVE_RELAXED:
                    o.optimizeRelaxed();
                    break;
                
                case SOLVE_MIPS:
                    o.optimizeMIPS();
                    break;
                
                case SOLVE_ALNS:
                    a.optimize();
                    break;
            }
            
            // Free memory occupied by Gurobi models
            o.cleanup();
        }
    }
}