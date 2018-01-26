/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverView;

import solverController.Controller;
import solverController.Solution;

/**
 *
 * @author Frash
 */
public class SolutionReportPane extends javax.swing.JPanel {
    private final static String NO_SOL = "No solution!";
    
    /**
     * The solution represented by this report panel
     */
    private Solution solution = null;
    
    /**
     * Creates new form SolutionReportPanel
     * @param solution the solution to display
     */
    public SolutionReportPane(Solution solution) {
        initComponents();
        
        updateSolution(solution);
        
        // Set scroll speed for the scroll pane
        jScrollPaneReport.getVerticalScrollBar().setUnitIncrement(16);
        jScrollPaneReport.getHorizontalScrollBar().setUnitIncrement(16);
    }
    
    /**
     * Method to update the current solution and its display.
     * @param solution the new solution to display
     */
    public void updateSolution(Solution solution){
        if(solution != null){
            // Update the solution object
            this.solution = solution;

            // Update all labels
            this.jLabelBestObjective.setText(String.valueOf(solution.getObjectiveValue()));
            this.jLabelInstancePath.setText(solution.getInstancePath());
            this.jLabelInstancePath.setToolTipText(solution.getInstancePath());
            this.jLabelSolver.setText(solution.getSolverName());
            this.jLabelTimestamp.setText(solution.getTimestamp().toString());

            // Update the full report
            if(solution.getSolverName().equals(Controller.Solvers.SOLVE_RELAXED.toString())){
                this.jScrollPaneReport.setVisible(false);
            }
            else this.jTextAreaReport.setText(solution.toString());
        }
        else{
            this.jLabelBestObjective.setText(NO_SOL);
            this.jLabelInstancePath.setText(NO_SOL);
            this.jLabelInstancePath.setToolTipText(NO_SOL);
            this.jLabelSolver.setText(NO_SOL);
            this.jLabelTimestamp.setText(NO_SOL);
        }
        
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanelHeader = new javax.swing.JPanel();
        jPanelLabels = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabelInstancePath = new javax.swing.JLabel();
        jLabelSolver = new javax.swing.JLabel();
        jLabelTimestamp = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabelBestObjective = new javax.swing.JLabel();
        jButtonOpenSolutionPath = new javax.swing.JButton();
        jScrollPaneReport = new javax.swing.JScrollPane();
        jTextAreaReport = new javax.swing.JTextArea();

        setName("Report 1"); // NOI18N
        setLayout(new java.awt.BorderLayout());

        jPanelHeader.setLayout(new java.awt.BorderLayout());

        java.awt.GridBagLayout jPanelHeaderLayout = new java.awt.GridBagLayout();
        jPanelHeaderLayout.columnWidths = new int[] {0, 10, 0};
        jPanelHeaderLayout.rowHeights = new int[] {0, 10, 0, 10, 0, 10, 0};
        jPanelLabels.setLayout(jPanelHeaderLayout);

        jLabel1.setText("Instance:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanelLabels.add(jLabel1, gridBagConstraints);

        jLabel2.setText("Solver:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanelLabels.add(jLabel2, gridBagConstraints);

        jLabel3.setText("Timestamp:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanelLabels.add(jLabel3, gridBagConstraints);

        jLabelInstancePath.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabelInstancePath.setText("jLabel4");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanelLabels.add(jLabelInstancePath, gridBagConstraints);

        jLabelSolver.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabelSolver.setText("jLabel5");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanelLabels.add(jLabelSolver, gridBagConstraints);

        jLabelTimestamp.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabelTimestamp.setText("jLabel6");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanelLabels.add(jLabelTimestamp, gridBagConstraints);

        jLabel4.setText("Best obj:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanelLabels.add(jLabel4, gridBagConstraints);

        jLabelBestObjective.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabelBestObjective.setText("jLabel5");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanelLabels.add(jLabelBestObjective, gridBagConstraints);

        jPanelHeader.add(jPanelLabels, java.awt.BorderLayout.WEST);

        jButtonOpenSolutionPath.setText("Open output folder");
        jButtonOpenSolutionPath.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOpenSolutionPathActionPerformed(evt);
            }
        });
        jPanelHeader.add(jButtonOpenSolutionPath, java.awt.BorderLayout.EAST);

        add(jPanelHeader, java.awt.BorderLayout.PAGE_START);

        jScrollPaneReport.setBorder(javax.swing.BorderFactory.createTitledBorder("Report"));

        jTextAreaReport.setEditable(false);
        jTextAreaReport.setColumns(20);
        jTextAreaReport.setRows(5);
        jTextAreaReport.setAutoscrolls(false);
        jScrollPaneReport.setViewportView(jTextAreaReport);

        add(jScrollPaneReport, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonOpenSolutionPathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOpenSolutionPathActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButtonOpenSolutionPathActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonOpenSolutionPath;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabelBestObjective;
    private javax.swing.JLabel jLabelInstancePath;
    private javax.swing.JLabel jLabelSolver;
    private javax.swing.JLabel jLabelTimestamp;
    private javax.swing.JPanel jPanelHeader;
    private javax.swing.JPanel jPanelLabels;
    private javax.swing.JScrollPane jScrollPaneReport;
    private javax.swing.JTextArea jTextAreaReport;
    // End of variables declaration//GEN-END:variables
}
