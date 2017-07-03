/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.ToolTipManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import solverController.Controller;
import solverController.Controller.Solvers;
import solverController.ParametersBean;
import solverView.MainWindow;

/**
 * Main class. Should be the one to run when the JAR file is executed.
 *
 * @author Frash
 */
public class Main {

    /**
     * Main method. Calls either the GUI or the Controller from a command line
     * interface.
     *
     * @param args
     * @return
     */
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                launchGui();
            } else {
                // Get the controller from CLI arguments
                Controller cnt = parseArgs(args);

                // Run the controller, if it was created correctly
                if (cnt != null) {
                    cnt.execute();
                    cnt.get();
                }
            }
        } catch (ParseException e) {
            // Bad ending 1
            System.out.println("Parsing Error: " + e.getMessage());
            return;
        } catch (Exception e) {
            // Bad ending 2
            System.out.println("Execution Error: " + e.getMessage());
            return;
        }

        // Good ending
        System.out.println("CTOWSS_alns execution terminated correctly.");
    }

    /**
     * Launches the gui
     */
    public static void launchGui() {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
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
    }

    /**
     * Parse the input arguments of main and return a Controller
     *
     * @param args vector of arguments
     * @return a Controller generated from the arguments
     * @throws org.apache.commons.cli.ParseException if the arguments are broken
     * (also shows an help message)
     */
    public static Controller parseArgs(String[] args) throws ParseException {
        Controller ret = null;

        // Initializing help text
        HelpFormatter hf = new HelpFormatter();
        String appDescription
                = "Solve the given instances for a Clustered Team Orienteering problem With\n"
                + "Services Sequence (CTOWSS) using either an ALNS (Adaptive Large Neighborhood\n"
                + "Search) algorithm, a regular MIPS solver or the MIPS solver on the model's\n"
                + "relaxation."
                + "\n(C) 2017 Francesco Piazza";
        String footer = "Launch with no arguments to run the GUI."
                + "\nRequires an active installation of Gurobi on the current machine to run."
                + "\nSee http://www.gurobi.com";
        String appName = args[0];

        // Building command line options
        Options options = new Options();

        Option helpOpt = Option.builder("h")
                .desc("show help")
                .hasArg(false)
                .longOpt("help")
                .optionalArg(true)
                .required(false)
                .type(Boolean.class)
                .build();

        Option modelPathsOpt = Option.builder("i")
                .argName("pathToInstance1> <pathToInstance2> <...")
                .desc("list of space separated instance paths")
                .longOpt("instances")
                .optionalArg(false)
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .required()
                .type(String.class)
                .valueSeparator(' ')
                .build();

        Option parametersOpt = Option.builder("p")
                .argName("parametersPath")
                .desc("path to the run parameters JSON file")
                .hasArg()
                .longOpt("parameters")
                .numberOfArgs(1)
                .optionalArg(false)
                .required()
                .type(String.class)
                .build();

        Option solverOpt = Option.builder("s")
                .argName("solver")
                .desc("solver to use. Options: SOLVE_ALNS (default), SOLVE_MIPS, SOLVE_RELAXED")
                .hasArg()
                .longOpt("solver")
                .numberOfArgs(1)
                .optionalArg(false)
                .required(false)
                .type(Controller.Solvers.class)
                .build();

        options.addOption(helpOpt);
        options.addOption(modelPathsOpt);
        options.addOption(parametersOpt);
        options.addOption(solverOpt);

        // Parsing command line options
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            // List of possible options
            List<String> modelPaths = new ArrayList<>();
            ParametersBean pb = new ParametersBean();
            Solvers solver = Solvers.SOLVE_ALNS;

            if (cmd.hasOption("h")) {
                hf.printHelp(appName, appDescription, options, footer, true);
            } else {
                if (cmd.hasOption("i")) {
                    String[] modelPathss = cmd.getOptionValues("i");
                    modelPaths = Arrays.asList(modelPathss);
                }
                if (cmd.hasOption("p")) {
                    pb.deserializeFromJSON(cmd.getOptionValue("p")); // Throws IOException
                }
                if (cmd.hasOption("s")) {
                    solver = Solvers.valueOf(cmd.getOptionValue("s")); // Throws IllegalArgumentException - if this enum type has no constant with the specified name
                }

                // Create the new Controller
                ret = new Controller(modelPaths, pb, solver, null, null);
            }
        } catch (ParseException e) {
            hf.printHelp(appName, "", options, footer);
            throw e;
        } catch (IOException e) {
            throw new ParseException("IO error - " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ParseException("Illegal argument error - " + e.getMessage());
        }

        return ret;
    }
}
