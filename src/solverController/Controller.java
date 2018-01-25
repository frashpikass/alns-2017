/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverController;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import solverView.MainWindow;

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
    
    /**
     * The outputstream where we want to print all the result.
     * If null, the default one will be used.
     */
    private OutputStream stdoutStream=null;

    @Override
    protected Void doInBackground() throws Exception {
        try{
            // Initialize PropertyChangeSupport
            getPropertyChangeSupport().addPropertyChangeListener( this );

            // Initialize the last instance number
            this.lastInstanceNumber = 0;

            // Execute the batch of instances, one at a time
            for(String path : this.modelPaths){
                // Save the current model path
                this.lastModelPath = path;
                
                // Get the total timelimit according to the selected solver
                double timelimit = 1;
                switch(this.getSolver()){
                    case SOLVE_ALNS:
                        timelimit = pb.getALNSproperties().getTimeLimitALNS();
                        break;
                        
                    case SOLVE_MIPS:
                        timelimit = pb.getOrienteeringProperties().getTimeLimit();
                        break;
                }
                
                // Initialize progress to 0
                this.setProgress(0);
                // Publish information about the initial status
                this.publish(new OptimizationStatusMessage(
                        lastModelPath, 0, 0,
                        lastInstanceNumber,
                        modelPaths.size(),
                        OptimizationStatusMessage.Status.STARTING,
                        0.0,
                        timelimit
                    )
                );

                // Proceed with the optimization
                // Save the chosen parameters to the output folder
                pb.serializeToJSON();

                // Initialize a new Orienteering object starting from the current modelPath
                lastOrienteering = new Orienteering(
                        path,
                        pb.getOrienteeringProperties()
                );

                currentALNS = new ALNS(
                            lastOrienteering,
                            pb.getALNSproperties(),
                            this
                );

                currentALNS.execute();
                
                // Wait for job completion (the Controller will wait)
                currentALNS.get();
                // Update the batch number
                this.lastInstanceNumber++;
            }
            return null;
        }
        catch(InterruptedException e){
            System.out.println("Controller was interrupted: "+e.getMessage());
            
            // Update the window
            messageReceived();
            
            // Kill the ALNS thread
            currentALNS.cancel(true);
            currentALNS = null;
            lastOrienteering = null;
            
            return null;
        }
    }
    
    @Override
    public void done(){
        if(isCancelled()){
            if(mainWindow != null){
                messageReceived();
            }
        }
        else if(isDone()){
            if(mainWindow != null){
                messageReceived();
            }
        }
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
     * Values are SOLVE_RELAXED, SOLVE_MIPS, SOLVE_ALNS
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
     * The MainWindow which hosts the gui. This object must be used to update GUI features.
     */
    private MainWindow mainWindow;
    
    /**
     * A message from an ALNS thread
     */
    private OptimizationStatusMessage messageFromALNS;
    
    /**
     * Retrieve the last message set by the ALNS thread
     * @return the last message set by the ALNS thread
     */
    public OptimizationStatusMessage getMessageFromALNS() {
        return messageFromALNS;
    }
    
    /**
     * Method used by the ALNS thread to set a message for this Controller
     * @param messageFromALNS the message from the ALNS thread
     */
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
     * @param stdoutStream the output stream where output should be printed to. If <code>null</code>, the default will be used.
     * @param mainWindow the main window to update the fetures of. Can be null.
     */
    public Controller(
            List<String> modelPaths,
            OrienteeringPropertiesBean opb,
            ALNSPropertiesBean apb,
            Solvers solver,
            OutputStream stdoutStream,
            MainWindow mainWindow
    ){
        // Setup all parameters
        pb = new ParametersBean(opb, apb);
        this.modelPaths = modelPaths;
        this.solver = solver;
        if(stdoutStream != null){
            this.stdoutStream = stdoutStream;
            System.setOut(new PrintStream(stdoutStream));
            System.setErr(new PrintStream(stdoutStream));
        }
        this.mainWindow = mainWindow;
    }
    
    /**
     * Gets the outputstream used by the controller.
     * @return the outputstream used by the controller, <code>null</code> if the default stream is being used.
     */
    public OutputStream getStdoutStream() {
        return stdoutStream;
    }
    
    
    
    /**
     * Constructor for the controller class.
     * @param modelPaths list of model paths to solve in a batch
     * @param pb Javabean that holds all solver parameters
     * @param solver the solver to use with this controller
     * @param stdoutStream the output stream where output should be printed to. If <code>null</code>, the default will be used.
     * @param mainWindow the main window to update the fetures of. Can be null.
     */
    public Controller(
            List<String> modelPaths,
            ParametersBean pb,
            Solvers solver,
            OutputStream stdoutStream,
            MainWindow mainWindow
    ){
        this.pb = pb;
        this.modelPaths = modelPaths;
        this.solver = solver;
        if(stdoutStream != null){
            this.stdoutStream = stdoutStream;
            System.setOut(new PrintStream(stdoutStream));
            System.setErr(new PrintStream(stdoutStream));
        }
        this.mainWindow = mainWindow;
    }

    public static void main(String[] args) throws Exception {
        
        /*
        MainWindow mw = new MainWindow();
        System.setOut(new PrintStream(mw.getTextAreaOutputStream()));
        System.setErr(new PrintStream(mw.getTextAreaOutputStream()));
        
        mw.openWindow(args);
        /*
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                MainWindow mw = new MainWindow();
                // Set the title
                mw.setTitle("ALNS Solver v1.0 (GUI mode)");
                
                // Make the window appear
                mw.setVisible(true);
                
                // Make tooltips appear faster and last longer
                ToolTipManager.sharedInstance().setInitialDelay(250);
                ToolTipManager.sharedInstance().setDismissDelay(15000);
            }
        });
        */
        // TEST
        List<String> modelPaths = new ArrayList<>();
        modelPaths.add("Instance0.txt");
        
        Controller c = new Controller(
                modelPaths,
                new ParametersBean(),
                Solvers.SOLVE_ALNS,
                null,
                null
        );
        c.execute();
        try{
            c.get();
        }
        catch(InterruptedException e){
            System.out.println("Controller Main - Optimization interrupted: "+e.getMessage());
        }
        catch(ExecutionException e){
            System.out.println("ControllerMain - ExecutionException: "+e.getMessage());
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
     * When a message is received from ALNS, add information about the batch
     * progress and publish it as a standard OptimizationStatusMessage.
     */
    private void messageReceived(){
        // Here I should publish the message I've received and add information
        // on the batch size and instance number in the batch
        if(messageFromALNS != null){
            this.setProgress(messageFromALNS.getProgress());

            OptimizationStatusMessage newMessage = new OptimizationStatusMessage(
                    lastModelPath,
                    messageFromALNS.getProgress(),
                    messageFromALNS.getElapsedTime(),
                    this.lastInstanceNumber,
                    this.modelPaths.size(),
                    messageFromALNS.getStatus(),
                    messageFromALNS.getBestObj(),
                    messageFromALNS.getTimelimit()
            );
            publish(newMessage);
        }
    }
    
    @Override
    protected void process(List<OptimizationStatusMessage> messages){
        if(messages != null && !messages.isEmpty() && mainWindow != null){
            OptimizationStatusMessage osm = messages.get(messages.size()-1);
            
            OptimizationStatusMessage.Status realState = OptimizationStatusMessage.Status.RUNNING;
            switch(osm.getStatus()){
                case STARTING:
                    if(isCancelled())
                        realState = OptimizationStatusMessage.Status.STOPPING;
                    else realState = OptimizationStatusMessage.Status.STARTING;
                    break;
                case RUNNING:
                    if(isCancelled())
                        realState = OptimizationStatusMessage.Status.STOPPING;
                    else realState = OptimizationStatusMessage.Status.RUNNING;
                    break;
                case STOPPING:
                    realState = OptimizationStatusMessage.Status.STOPPING;
                    break;
                case DONE:
                    if(isCancelled())
                        realState = OptimizationStatusMessage.Status.STOPPED;
                    else realState = OptimizationStatusMessage.Status.DONE;
                    break;
                case STOPPED:
                    if(isCancelled())
                        realState = OptimizationStatusMessage.Status.STOPPED;
                    else realState = OptimizationStatusMessage.Status.STOPPING;
                    break;
            }
            //System.out.println("Message from ALNS: "+messages.get(messages.size()-1)); // DEBUG
            // here I should update the progress bar and the label in the gui
            mainWindow.updateSolverStatusIndicators(
                    new OptimizationStatusMessage(
                            osm.getInstancePath(),
                            osm.getProgress(),
                            osm.getElapsedTime(),
                            osm.getInstanceNumber(),
                            osm.getBatchSize(),
                            realState,
                            osm.getBestObj(),
                            osm.getTimelimit()
                    )
            );
        }
    }
}