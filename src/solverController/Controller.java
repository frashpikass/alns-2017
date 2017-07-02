/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverController;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import solverModel.ALNS;
import solverModel.Orienteering;

/**
 * A Controller class to solve batches of orienteering problem instances using
 * specific parameters and solvers.
 * @author Frash
 */
public class Controller
        extends SwingWorker<Void, OptimizationStatusMessage>
        implements PropertyChangeListener
{
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
     * Reference to the current ALNS task being solved
     */
    private ALNS currentALNS;

    @Override
    protected Void doInBackground() throws Exception {
        // Initialize PropertyChangeSupport
        getPropertyChangeSupport().addPropertyChangeListener( this );
        
        // Initialize the last instance number
        this.lastInstanceNumber = 0;
        
        // Execute the batch of instances, one at a time
        for(String path : this.modelPaths){
            // Save the current model path
            this.lastModelPath = path;
            
            // Initialize progress to 0
            this.setProgress(0);
            // Publish information about the initial status
            this.publish(new OptimizationStatusMessage(lastModelPath, 0, 0, lastInstanceNumber, modelPaths.size()));
            
            // Proceed with the optimization
            this.optimize(path);
            
            currentALNS.execute();
            
            try{
                currentALNS.get();
            }
            catch(InterruptedException e){
                System.out.println("ALNS was interrupted: "+e.getMessage());
            }
            catch(ExecutionException e){
                System.out.println("What happened to ALNS? "+e.getMessage());
            }
            
            // Update the batch number
            this.lastInstanceNumber++;
        }
        return null;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if("messageFromALNS".equals(evt.getPropertyName())){
                    Controller c = (Controller) evt.getSource();
                    c.messageReceived();
        }
    }
  
    /**
     * List of possible choices for solvers.
     */
    public enum Solvers {SOLVE_RELAXED, SOLVE_MIPS, SOLVE_ALNS};
    
    /**
     * The solver chosen for the next batch of instances to solve.
     */
    private Solvers solver = Solvers.SOLVE_ALNS;
    
    /**
     * Path to the last model set to be solved in the batch.
     */
    private String lastModelPath = "";
    
    /**
     * Index of the current model being solved in the batch
     */
    private int lastInstanceNumber = 0;
    
    /**
     * Reference to the last orienteering object created. It must be cleaned when the ALNS
     * solver has finished processing the last model path.
     */
    private Orienteering lastOrienteering = null;
    
    /**
     * A message from an ALNS thread
     */
    private OptimizationStatusMessage messageFromALNS;
    
    public OptimizationStatusMessage getMessageFromALNS() {
        return messageFromALNS;
    }

    public void setMessageFromALNS(OptimizationStatusMessage messageFromALNS) {
        OptimizationStatusMessage old = this.messageFromALNS;
        this.messageFromALNS = messageFromALNS;
        this.firePropertyChange("messageFromALNS", old, messageFromALNS);
    }
    
    /**
     * Constructor for the controller class.
     * @param modelPaths list of model paths to solve in a batch
     * @param opb Javabean that holds all general parameters for an Orienteering solver
     * @param apb Javabean that holds all ALNS parameters 
     * @param solver the solver to use with this controller
     */
    public Controller(
            List<String> modelPaths,
            OrienteeringPropertiesBean opb,
            ALNSPropertiesBean apb,
            Solvers solver
    ){
        // Setup all parameters
        pb = new ParametersBean(opb, apb);
        this.modelPaths = modelPaths;        
        this.solver = solver;
        
    }
    
    /**
     * Constructor for the controller class.
     * @param modelPaths list of model paths to solve in a batch
     * @param pb Javabean that holds all solver parameters
     * @param solver the solver to use with this controller
     */
    public Controller(
            List<String> modelPaths,
            ParametersBean pb,
            Solvers solver
    ){
        this.pb = pb;
        this.modelPaths = modelPaths;
        this.solver = solver;
    }

    public static void main(String[] args) throws Exception {
        // TEST
        List<String> modelPaths = new ArrayList<>();
        modelPaths.add("Instance0.txt");
        
        Controller c = new Controller(
                modelPaths,
                new OrienteeringPropertiesBean(),
                new ALNSPropertiesBean(),
                Solvers.SOLVE_ALNS
        );
        
        /*
        c.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if("messageFromALNS".equals(evt.getPropertyName())){
                    Controller c = (Controller) evt.getSource();
                    c.messageReceived();
                }
            }
        });
        */
        //SwingUtilities.invokeLater(c);
        c.execute();
        try{
            c.get();
        }
        catch(InterruptedException e){
            System.out.println("Optimization interrupted. "+e.getMessage());
        }
        catch(ExecutionException e){
            System.out.println("Hmm... "+e.getMessage());
        }
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
        
        this.lastInstanceNumber = 0;
        for(String modelPath : modelPaths){
            this.lastModelPath = modelPath;
            // Initialize a new Orienteering object starting from the current modelPath
            Orienteering o = new Orienteering(
                    modelPath,
                    pb.getOrienteeringProperties()
            );
            
            // Initialize a new ALNS object starting from the current modelPath
            // and Orienteering object
            ALNS a = new ALNS(
                    o,
                    pb.getALNSproperties(),
                    this
            );
            
            // Pick a solver and apply it
            switch(solver){
                case SOLVE_RELAXED:
                    a.optimizeRelaxed();
                    break;
                
                case SOLVE_MIPS:
                    a.optimizeMIPS();
                    break;
                
                case SOLVE_ALNS:
                    a.optimizeALNS();
                    break;
            }
            
            // Free memory occupied by Gurobi models
            o.cleanup();
        }
    }
    
    
    
    /**
     * Optimizes a single instance using the solver specified through the
     * <code>setSolver</code> method.
     */
    public void optimize(String modelPath) throws Exception{
        // Save the chosen parameters to the output folder
        pb.serializeToJSON();
        
        // Initialize a new Orienteering object starting from the current modelPath
        lastOrienteering = new Orienteering(
                modelPath,
                pb.getOrienteeringProperties()
        );
        
        currentALNS = new ALNS(
                    lastOrienteering,
                    pb.getALNSproperties(),
                    this
        );

        // Initialize a new ALNS object starting from the current modelPath
        // and Orienteering object
        // SwingUtilities.invokeLater(currentALNS);
        
        // Now go back to "doInBackground()" and wait for the optimization to complete
    }
    
    private void messageReceived(){
        // Here I should publish the message I've received and add information
        // on the batch size and instance number in the batch
        this.setProgress(messageFromALNS.getProgress());
        
        OptimizationStatusMessage newMessage = new OptimizationStatusMessage(
                lastModelPath,
                messageFromALNS.getProgress(),
                messageFromALNS.getElapsedTime(),
                this.lastInstanceNumber,
                this.modelPaths.size()
        );
        publish(newMessage);
    }
    
    @Override
    protected void process(List<OptimizationStatusMessage> messages){
        if(messages != null && !messages.isEmpty())
            System.out.println("Message from ALNS: "+messages.get(messages.size()-1)); // DEBUG
        // here I should update the progress bar and the label in the gui
    }
}