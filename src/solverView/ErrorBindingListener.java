/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverView;

import javax.swing.JDialog;
import javax.swing.JLabel;
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
        String description;
        if ((fail != null) && (fail.getType() == Binding.SyncFailureType.VALIDATION_FAILED)) {
            description = fail.getValidationResult().getDescription();
        } else {
            description = "Sync failed!";
        }
        String msg = "[" + binding.getName() + "] " + description;
        System.out.println(msg);
        outputLabel.setText(msg);
        errorDialog.setVisible(true);
    }

    @Override
    public void synced(Binding binding) {
        String bindName = binding.getName();
        String msg = "[" + bindName + "] Synced";
        System.out.println(msg);        
        outputLabel.setText("");
    }
}
