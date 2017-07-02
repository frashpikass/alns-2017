package solverController;

import java.beans.PropertyChangeEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/**
 * This class describes a thread which reads a logfile being written in the background
 * and sends all of its new lines to stdout
 * @author Frash
 */
public class LogRedirector
        extends SwingWorker<Void,String>
{
    private volatile boolean finished = false;
    private String inputFilePath;
    private FileInputStream input;
    
    /**
     * Set a flag that should stop the logger
     */
    public void finish(){
        this.finished = true;
        this.firePropertyChange("pleaseFinish", false, true);
    }
    
    public void propertyChange(PropertyChangeEvent evt) {
        if("pleaseFinish".equals(evt.getPropertyName())){
                    LogRedirector lr = (LogRedirector) evt.getSource();
                    lr.cancel(true);
        }
    }
    
    /**
     * Constructor for a new log redirector
     * @param inputFilePath path to the input file to log
     */
    public LogRedirector(String inputFilePath){
        this.inputFilePath = inputFilePath;
        try {
            input = new FileInputStream(inputFilePath);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LogRedirector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    protected Void doInBackground(){
        try{
            try {
                // Skip to the end of the file
                input.skip(input.available());
            } catch (IOException ex) {
                Logger.getLogger(LogRedirector.class.getName()).log(Level.SEVERE, null, ex);
            }
            do{
                // Read some kilobytes
                readSome(10240);
                // Wait some time before reading the next kilobyte
                //this.wait(300);
                Thread.sleep(1000);
            }while(!finished && !isCancelled());
        }
        catch(InterruptedException e){
            // System.out.println("Logger redirector interrupted.");
            readSome(10240);
        }
        finally{
            return null;
        }
    }
    
    /**
     * Read the specified number of bytes from the input file and print them to stdout
     * @param bytesToRead number of bytes to read
     */
    private void readSome(int bytesToRead){
        // Read 10 kilobytes, if available
        byte[] bytes = new byte[bytesToRead];
        int readN = 0;
        try {
            readN = input.read(bytes);
        } catch (IOException ex) {
            Logger.getLogger(LogRedirector.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (readN > 0) {
            // Print the kilobyte we've just read
            byte[] sub = Arrays.copyOf(bytes, readN);
            //System.out.print(new String(sub));
            publish(new String(sub));
        }
    }
    
    @Override
    protected void process(List<String> messages){
        System.out.print(messages.get(messages.size()-1));
    }
    
    @Override
    protected void done(){
        System.out.println("LogRedirector "+this.getState());
        
        if(this.getState() == StateValue.DONE){
            try {
                input.close();
            } catch (IOException ex) {
                Logger.getLogger(LogRedirector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
