/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverView;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import solverController.Controller;
import solverController.Solution;

/**
 * Class to visually represent a solution report in the Swing GUI
 * @author Frash
 */
public class SolutionReportPane extends javax.swing.JPanel {
    private final static String NO_SOL = "No solution!";
    
    /**
     * The solution represented by this report panel
     */
    private Solution solution = null;
    
    /**
     * Path to the output folder for this solution report
     */
    private String outputFolderPath;
    
    /**
     * Index which represents the report number
     */
    private int reportNumber;
    
    /**
     * Reference to the parent window hosting this report pane
     */
    private MainWindow parentWindow;
    
    /**
     * Creates new form SolutionReportPanel
     * @param solution the solution to display
     * @param outputFolderPath Path to the output folder
     * @param reportNumber index which represents the report number
     * @param parentWindow reference to the parent window hosting this report pane
     */
    public SolutionReportPane(
            Solution solution,
            String outputFolderPath,
            int reportNumber,
            MainWindow parentWindow
    ) {
        initComponents();
        
        updateSolution(solution);
        this.outputFolderPath = outputFolderPath;
        this.reportNumber = reportNumber;
        this.parentWindow = parentWindow;
        
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
            else{
                this.jTextAreaReport.setText(solution.toString());
                this.jTextAreaReport.setCaretPosition(0);
            }
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
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPaneReport = new javax.swing.JScrollPane();
        jTextAreaReport = new javax.swing.JTextArea();
        jPanelLabels = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabelInstancePath = new javax.swing.JLabel();
        jLabelSolver = new javax.swing.JLabel();
        jLabelTimestamp = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabelBestObjective = new javax.swing.JLabel();
        jPanelButtons = new javax.swing.JPanel();
        jButtonOpenSolutionPath = new javax.swing.JButton();
        jButtonCloseReport = new javax.swing.JButton();
        jButtonReloadParameters = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEtchedBorder());
        setName("Report 1"); // NOI18N
        setLayout(new java.awt.BorderLayout());

        jPanelHeader.setLayout(new javax.swing.BoxLayout(jPanelHeader, javax.swing.BoxLayout.LINE_AXIS));

        jScrollPaneReport.setBorder(javax.swing.BorderFactory.createTitledBorder("Report"));

        jTextAreaReport.setEditable(false);
        jTextAreaReport.setColumns(50);
        jTextAreaReport.setRows(11);
        jTextAreaReport.setTabSize(4);
        jTextAreaReport.setAutoscrolls(false);
        jScrollPaneReport.setViewportView(jTextAreaReport);

        jSplitPane1.setLeftComponent(jScrollPaneReport);

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
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanelLabels.add(jLabelInstancePath, gridBagConstraints);

        jLabelSolver.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabelSolver.setText("jLabel5");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanelLabels.add(jLabelSolver, gridBagConstraints);

        jLabelTimestamp.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabelTimestamp.setText("jLabel6");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
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

        jSplitPane1.setRightComponent(jPanelLabels);

        jPanelHeader.add(jSplitPane1);

        add(jPanelHeader, java.awt.BorderLayout.CENTER);

        java.awt.GridBagLayout jPanelButtonsLayout = new java.awt.GridBagLayout();
        jPanelButtonsLayout.columnWidths = new int[] {0, 10, 0, 10, 0};
        jPanelButtonsLayout.rowHeights = new int[] {0};
        jPanelButtons.setLayout(jPanelButtonsLayout);

        jButtonOpenSolutionPath.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/folder-open.png"))); // NOI18N
        jButtonOpenSolutionPath.setText("Open output folder");
        jButtonOpenSolutionPath.setToolTipText("Open the output folder for this solution report");
        jButtonOpenSolutionPath.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOpenSolutionPathActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanelButtons.add(jButtonOpenSolutionPath, gridBagConstraints);

        jButtonCloseReport.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/close.png"))); // NOI18N
        jButtonCloseReport.setText("Close this report");
        jButtonCloseReport.setToolTipText("Close this report tab");
        jButtonCloseReport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCloseReportActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanelButtons.add(jButtonCloseReport, gridBagConstraints);

        jButtonReloadParameters.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/open-in-app.png"))); // NOI18N
        jButtonReloadParameters.setText("Reload used parameters");
        jButtonReloadParameters.setToolTipText("Reload all parameters used to produce this solution");
        jButtonReloadParameters.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReloadParametersActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanelButtons.add(jButtonReloadParameters, gridBagConstraints);

        add(jPanelButtons, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonOpenSolutionPathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOpenSolutionPathActionPerformed
        try {
            // TODO add your handling code here:
            Desktop d = Desktop.getDesktop();
            d.open(new File(this.outputFolderPath));
        } catch (IOException ex) {
            Logger.getLogger(SolutionReportPane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonOpenSolutionPathActionPerformed

    private void jButtonCloseReportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCloseReportActionPerformed
        // TODO add your handling code here:
        parentWindow.removeSolutionReport(this);
    }//GEN-LAST:event_jButtonCloseReportActionPerformed

    private void jButtonReloadParametersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonReloadParametersActionPerformed
        // TODO add your handling code here:
        parentWindow.loadParametersBean(this.solution.getParameters());
    }//GEN-LAST:event_jButtonReloadParametersActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonCloseReport;
    private javax.swing.JButton jButtonOpenSolutionPath;
    private javax.swing.JButton jButtonReloadParameters;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabelBestObjective;
    private javax.swing.JLabel jLabelInstancePath;
    private javax.swing.JLabel jLabelSolver;
    private javax.swing.JLabel jLabelTimestamp;
    private javax.swing.JPanel jPanelButtons;
    private javax.swing.JPanel jPanelHeader;
    private javax.swing.JPanel jPanelLabels;
    private javax.swing.JScrollPane jScrollPaneReport;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTextArea jTextAreaReport;
    // End of variables declaration//GEN-END:variables

    /**
     * Path to the output folder
     * @return the outputFolderPath
     */
    public String getOutputFolderPath() {
        return outputFolderPath;
    }

    /**
     * Index which represents the report number
     * @return the reportNumber
     */
    public int getReportNumber() {
        return reportNumber;
    }

    /**
     * Index which represents the report number
     * @param reportNumber the reportNumber to set
     */
    public void setReportNumber(int reportNumber) {
        this.reportNumber = reportNumber;
    }
}
