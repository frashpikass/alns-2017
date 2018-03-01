/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverView.bindingInterfaces;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JTextField;
import org.jdesktop.beansbinding.AbstractBindingListener;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.Binding.SyncFailure;

/**
 * Binding listener used to log synchronization events. It displays
 * (in given label) warnings for failed synchronizations.
 * @author Frash
 */
public class ErrorBindingListener extends AbstractBindingListener{
    /**
     * Format for error messages
     */
    private final static String ERROR_MSG_FORMAT = "<html><b><font size=\"5\" color=\"red\">%s</font></b></html>";
    
    /** Label used to display warnings. */
    private JLabel outputLabel;
    
    /**
     * Map to store previous binding errors
     */
    private HashMap<Binding, String> errorMap;
    

    public ErrorBindingListener(JLabel outputLabel) {
        if (outputLabel == null) throw new IllegalArgumentException();
        this.outputLabel = outputLabel;
        errorMap = new HashMap<>();
    }

    @Override
    public void syncFailed(Binding binding, SyncFailure fail) {
        if ((fail != null)) {
            if(fail.getType() == Binding.SyncFailureType.VALIDATION_FAILED){
                handleFailure(binding, fail.getType(), fail.getValidationResult().getDescription());
            }
            else {
                handleFailure(binding, fail.getType(), null);
            }
        }
    }
    
    /**
     * This method handles vwerious type of failure by setting and displaying
     * an error message in the stack
     * @param binding the binding source
     * @param failureType the failure type
     * @param validationFailedDescription description to display in case the failure is "validation failed"
     */
    public void handleFailure(Binding binding, Binding.SyncFailureType failureType, String validationFailedDescription){
        Object source = binding.getTargetObject();
        
        String description = "";
            
        if((failureType == Binding.SyncFailureType.VALIDATION_FAILED)){
            description = validationFailedDescription;
        }
        if((failureType == Binding.SyncFailureType.CONVERSION_FAILED)){
            description = "The inserted string is not a number.";
        }
        if((failureType == Binding.SyncFailureType.SOURCE_UNREADABLE)){
            description = "Source unreadable";
        }
        if((failureType == Binding.SyncFailureType.SOURCE_UNWRITEABLE)){
            description = "Source unwriteable";
        }
        if((failureType == Binding.SyncFailureType.TARGET_UNREADABLE)){
            description = "Target unreadable";
        }
        if((failureType == Binding.SyncFailureType.TARGET_UNWRITEABLE)){
            description = "Target unwriteable";
        }

        // background colour change to display error
        if(source instanceof JTextField){
            JTextField jtf = (JTextField) source;
            jtf.setBackground(Color.PINK);
        }
        
        String msg = "[" + binding.getName() + "] " + description;
        //JOptionPane.showMessageDialog(null, msg, "Input error", JOptionPane.ERROR_MESSAGE);
        
        // Show error message and save it to the error list
        outputLabel.setText(String.format(ERROR_MSG_FORMAT, msg));
        errorMap.put(binding, msg);
        
        System.out.println(msg);
        
    }

    @Override
    public void synced(Binding binding) {
        Object source = binding.getTargetObject();
        // background colour change to show validation
        if(source instanceof JTextField){
            JTextField jtf = (JTextField) source;
            jtf.setBackground(null);
        }
        
//        String bindName = binding.getName();
//        String msg = "[" + bindName + "] Synced";
//        System.out.println(msg);
        
        
        // If the binding is in the error map, remove it and show the last
        // error available or a "Ready." message
        if(errorMap.containsKey(binding)){
            errorMap.remove(binding);
            if(errorMap.isEmpty()){
                outputLabel.setText("Ready.");
            }
            else{
                // Get the first next error to fix and show it
                for(Binding b : errorMap.keySet()){
                    outputLabel.setText(String.format(ERROR_MSG_FORMAT, errorMap.get(b)));
                    break;
                }
            }
        }
    }
    
    /**
     * Displays an error message in a way that is coherent with other errors.
     * The message is correctly formatted but it's not saved in the stack.
     * @param msg the error message to to display
     */
    public void displayErrorMessage(String msg) {        
        outputLabel.setText(String.format(ERROR_MSG_FORMAT, msg));
    
    }
    
    /**
     * Removes all errors from the stack
     */
    public void resetAllErrors(){
        List<Binding> l = new ArrayList<>(errorMap.keySet());
        for(Binding b : l){
            // For each binding, if it's represented as a textfield
            if(b.getTargetObject() instanceof JTextField){
                // get the textfield
                JTextField jtf = (JTextField) b.getTargetObject();
                
                // reset the text to the safe stored value
                jtf.setText(b.getSourceValueForTarget().getValue().toString());
            }
        }
        
        // The error stack is automatically cleared as the values are reset
        
        // Reset the status bar to "Ready."
        if(errorMap.isEmpty()){
            outputLabel.setText("Ready.");
        }
    }
    
    /**
     * Resets a single error from its corresponding binding
     * @param b the binding to reset an error for
     */
    public void resetError(Binding b){
        if(b.getTargetObject() instanceof JTextField){
            // get the textfield
            JTextField jtf = (JTextField) b.getTargetObject();

            // reset the text to the safe stored value
            jtf.setText(b.getSourceValueForTarget().getValue().toString());
        }
    }
}
