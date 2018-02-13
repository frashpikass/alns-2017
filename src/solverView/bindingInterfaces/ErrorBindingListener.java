/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverView.bindingInterfaces;

import java.awt.Color;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
    /** Label used to display warnings. */
    private JDialog errorDialog;
    private JLabel outputLabel;

    public ErrorBindingListener(JDialog errorDialog, JLabel outputLabel) {
        if (outputLabel == null || errorDialog == null) throw new IllegalArgumentException();
        this.errorDialog = errorDialog;
        this.outputLabel = outputLabel;
    }

    @Override
    public void syncFailed(Binding binding, SyncFailure fail) {
        Object source = binding.getTargetObject();
        
        String description = "";
        if ((fail != null)) {
            // error messages
            
            if((fail.getType() == Binding.SyncFailureType.VALIDATION_FAILED)){
                description = fail.getValidationResult().getDescription();
            }
            if((fail.getType() == Binding.SyncFailureType.CONVERSION_FAILED)){
                description = "The inserted string is not a number.";
            }
            if((fail.getType() == Binding.SyncFailureType.SOURCE_UNREADABLE)){
                description = "Source unreadable";
            }
            if((fail.getType() == Binding.SyncFailureType.SOURCE_UNWRITEABLE)){
                description = "Source unwriteable";
            }
            if((fail.getType() == Binding.SyncFailureType.TARGET_UNREADABLE)){
                description = "Target unreadable";
            }
            if((fail.getType() == Binding.SyncFailureType.TARGET_UNWRITEABLE)){
                description = "Target unwriteable";
            }
            
            // background colour change to display error
            if(source instanceof JTextField){
                JTextField jtf = (JTextField) source;
                jtf.setBackground(Color.PINK);
            }
            
        }
        
        String msg = "[" + binding.getName() + "] " + description;
        JOptionPane.showMessageDialog(null, msg, "Input error", JOptionPane.ERROR_MESSAGE);
        System.out.println(msg);
//        outputLabel.setText(msg);
//        errorDialog.setVisible(true);
    }

    @Override
    public void synced(Binding binding) {
        Object source = binding.getTargetObject();
        // background colour change to show validation
        if(source instanceof JTextField){
            JTextField jtf = (JTextField) source;
            jtf.setBackground(null);
        }
        
        String bindName = binding.getName();
        String msg = "[" + bindName + "] Synced";
        System.out.println(msg);        
        outputLabel.setText("");
    }
}
