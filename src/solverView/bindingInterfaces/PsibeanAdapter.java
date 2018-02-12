/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverView.bindingInterfaces;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import solverController.ALNSPropertiesBean;

/**
 *
 * @author Frash
 */
public class PsibeanAdapter implements PropertyChangeListener{
//    /**
//     * This parameters holds the values of psi, the function that prizes good
//     * heuristics and penalizes the bad ones. Psi 1
//     */
//    private double psi1;
//    /**
//     * This parameters holds the values of psi, the function that prizes good
//     * heuristics and penalizes the bad ones. Psi 2
//     */
//    private double psi2;
//    /**
//     * This parameters holds the values of psi, the function that prizes good
//     * heuristics and penalizes the bad ones. Psi 3
//     */
//    private double psi3;
//    /**
//     * This parameters holds the values of psi, the function that prizes good
//     * heuristics and penalizes the bad ones. Psi 4
//     */
//    private double psi4;
    
    /**
     * Bean we want to adapt
     */
    private ALNSPropertiesBean alnsPropertiesBean;
    
    /**
     * Constructor for this adapter
     */
    public PsibeanAdapter(){
//        psi1 = ALNSPropertiesBean.DEFAULT_HEURISTIC_SCORES[0];
//        psi2 = ALNSPropertiesBean.DEFAULT_HEURISTIC_SCORES[1];
//        psi3 = ALNSPropertiesBean.DEFAULT_HEURISTIC_SCORES[2];
//        psi4 = ALNSPropertiesBean.DEFAULT_HEURISTIC_SCORES[3];
        
        alnsPropertiesBean = new ALNSPropertiesBean();
    }

    /**
     * This parameters holds the values of psi, the function that prizes good
     * heuristics and penalizes the bad ones. Psi 1
     * @return the psi1
     */
    public double getPsi1() {
        return alnsPropertiesBean.getHeuristicScores(0);
    }

    /**
     * This parameters holds the values of psi, the function that prizes good
     * heuristics and penalizes the bad ones. Psi 1
     * @param psi1 the psi1 to set
     */
    public void setPsi1(double psi1) {
        alnsPropertiesBean.setHeuristicScores(0, psi1);
    }

    /**
     * This parameters holds the values of psi, the function that prizes good
     * heuristics and penalizes the bad ones. Psi 2
     * @return the psi2
     */
    public double getPsi2() {
        return alnsPropertiesBean.getHeuristicScores(1);
    }

    /**
     * This parameters holds the values of psi, the function that prizes good
     * heuristics and penalizes the bad ones. Psi 2
     * @param psi2 the psi2 to set
     */
    public void setPsi2(double psi2) {
        alnsPropertiesBean.setHeuristicScores(1, psi2);
    }

    /**
     * This parameters holds the values of psi, the function that prizes good
     * heuristics and penalizes the bad ones. Psi 3
     * @return the psi3
     */
    public double getPsi3() {
        return alnsPropertiesBean.getHeuristicScores(2);
    }

    /**
     * This parameters holds the values of psi, the function that prizes good
     * heuristics and penalizes the bad ones. Psi 3
     * @param psi3 the psi3 to set
     */
    public void setPsi3(double psi3) {
        alnsPropertiesBean.setHeuristicScores(2, psi3);
    }

    /**
     * This parameters holds the values of psi, the function that prizes good
     * heuristics and penalizes the bad ones. Psi 4
     * @return the psi4
     */
    public double getPsi4() {
        return alnsPropertiesBean.getHeuristicScores(3);
    }

    /**
     * This parameters holds the values of psi, the function that prizes good
     * heuristics and penalizes the bad ones. Psi 4
     * @param psi4 the psi4 to set
     */
    public void setPsi4(double psi4) {
        alnsPropertiesBean.setHeuristicScores(3, psi4);
    }

    /**
     * Bean we want to adapt
     * @return the alnsPropertiesBean
     */
    public ALNSPropertiesBean getAlnsPropertiesBean() {
        return alnsPropertiesBean;
    }

    /**
     * Bean we want to adapt
     * @param alnsPropertiesBean the alnsPropertiesBean to set
     */
    public void setAlnsPropertiesBean(ALNSPropertiesBean alnsPropertiesBean) {
        solverController.ALNSPropertiesBean oldAlnsPropertiesBean = this.alnsPropertiesBean;
        this.alnsPropertiesBean = alnsPropertiesBean;
        alnsPropertiesBean.addPropertyChangeListener(this);
        propertyChangeSupport.firePropertyChange(PROP_ALNSPROPERTIESBEAN, oldAlnsPropertiesBean, alnsPropertiesBean);
    }
    
    private final transient PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);
    public static final String PROP_PSI1 = "psi1";
    public static final String PROP_PSI2 = "psi2";
    public static final String PROP_PSI3 = "psi3";
    public static final String PROP_PSI4 = "psi4";
    public static final String PROP_ALNSPROPERTIESBEAN = "alnsPropertiesBean";

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if(evt.getPropertyName().equals(ALNSPropertiesBean.PROP_HEURISTICSCORES)){
            double[] oldV = (double[]) evt.getOldValue();
            double[] newV = (double[]) evt.getNewValue();
            
            if(oldV[0] != newV[0]){
                propertyChangeSupport.firePropertyChange(PROP_PSI1, oldV[0], newV[0]);
            }
            if(oldV[1] != newV[1]){
                propertyChangeSupport.firePropertyChange(PROP_PSI2, oldV[1], newV[1]);
            }
            if(oldV[2] != newV[2]){
                propertyChangeSupport.firePropertyChange(PROP_PSI3, oldV[2], newV[2]);
            }
            if(oldV[3] != newV[3]){
                propertyChangeSupport.firePropertyChange(PROP_PSI4, oldV[3], newV[3]);
            }
        }
    }
    
    
}
