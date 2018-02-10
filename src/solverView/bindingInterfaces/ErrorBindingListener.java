/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverView.bindingInterfaces;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
        String description = "";
        if ((fail != null)) {
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
        }
        
        String msg = "[" + binding.getName() + "] " + description;
        JOptionPane.showMessageDialog(null, msg, "Input error", JOptionPane.ERROR_MESSAGE);
        System.out.println(msg);
//        outputLabel.setText(msg);
//        errorDialog.setVisible(true);
    }

    @Override
    public void synced(Binding binding) {
        String bindName = binding.getName();
        String msg = "[" + bindName + "] Synced";
        System.out.println(msg);        
        outputLabel.setText("");
    }
}
