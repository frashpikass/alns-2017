/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverController;

import gurobi.GRB;
import gurobi.GRBCallback;
import gurobi.GRBException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Inner class to handle callbacks that check for the first feasible solution
 *
 * @author Frash
 */
class FeasibilityCallback extends GRBCallback {

    /**
     * Number of solution nodes to visit before the solver gives up on searching
     * for a feasible solution
     */
    private final static double NODES_BEFORE_ABORT = 5000;

    /**
     * Determines the maximum number of mips nodes to check before giving up a
     * feasibility check.
     */
    private double maxMIPSNodesForFeasibilityCheck;

    /**
     * Constructor for class feasibilityCallback
     *
     * @param maximumMIPSNodesForFeasibilityCheck maximum number of mips nodes
     * to check before giving up a feasibility check.
     */
    public FeasibilityCallback(double maximumMIPSNodesForFeasibilityCheck) {
        super();
        this.maxMIPSNodesForFeasibilityCheck = maximumMIPSNodesForFeasibilityCheck;
    }

    /**
     * Constructor for class feasibilityCallback
     */
    public FeasibilityCallback() {
        super();
        this.maxMIPSNodesForFeasibilityCheck = FeasibilityCallback.NODES_BEFORE_ABORT;
    }

    @Override
    protected void callback() {
        try {
            if (where == GRB.CB_MIP) {
                if (getIntInfo(GRB.CB_MIP_SOLCNT) > 0 || getDoubleInfo(GRB.CB_MIP_NODCNT) > maxMIPSNodesForFeasibilityCheck) {
                    abort();
                }
            } else if (where == GRB.CB_MIPSOL) {
                abort();
            }
        } catch (GRBException ex) {
            Logger.getLogger(Orienteering.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}