/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.beans.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import solverModel.ALNSPropertiesBean;
import solverModel.OrienteeringPropertiesBean;

/**
 *
 * @author Frash
 */
public class ParametersBean implements Serializable {
    /**
     * A Javabean that holds all general parameters.
     */
    private OrienteeringPropertiesBean OrienteeringProperties;
    
    /**
     * A Javabean that holds all ALNS parameters.
     */
    private ALNSPropertiesBean ALNSproperties;
    
    /**
     * Empty constructor.
     */
    public ParametersBean() {
        OrienteeringProperties = new OrienteeringPropertiesBean();
        ALNSproperties = new ALNSPropertiesBean();
    }
    
    /**
     * Constant that holds the default name for the output file.
     */
    public final static transient String OUTPUT_JSON_NAME = "parameters.json";
    
    /**
     * Constructor to build a new ParametersBean from existing OrienteeringPropertiesBean
     * and ALNSPropertiesBean objects.
     * @param opb an existing OrienteeringPropertiesBean object 
     * @param apb an existing ALNSPropertiesBean object
     */
    public ParametersBean(OrienteeringPropertiesBean opb, ALNSPropertiesBean apb){
        this.OrienteeringProperties = opb;
        this.ALNSproperties = apb;
    }

    /**
     * A Javabean that holds all general parameters.
     * @return the OrienteeringProperties
     */
    public OrienteeringPropertiesBean getOrienteeringProperties() {
        return OrienteeringProperties;
    }

    /**
     * A Javabean that holds all general parameters.
     * @param OrienteeringProperties the OrienteeringProperties to set
     */
    public void setOrienteeringProperties(OrienteeringPropertiesBean OrienteeringProperties) {
        this.OrienteeringProperties = OrienteeringProperties;
    }

    /**
     * A Javabean that holds all ALNS parameters.
     * @return the ALNSproperties
     */
    public ALNSPropertiesBean getALNSproperties() {
        return ALNSproperties;
    }

    /**
     * A Javabean that holds all ALNS parameters.
     * @param ALNSproperties the ALNSproperties to set
     */
    public void setALNSproperties(ALNSPropertiesBean ALNSproperties) {
        this.ALNSproperties = ALNSproperties;
    }
    
    /**
     * Serialize the current parameters bean to a json file.
     * The file name will be the one specified in the parameter <code>outputPath</code>.
     * @param outputPath path to the output file with parameters.
     * @throws IOException if there are problems with the output file.
     */
    public void serializeToJSON(String outputPath)
            throws IOException{
        // Create a new Gson object
        // The setPrettyPrinting option should make the output file more humanly readable
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        // Serialize to JSON
        String json = gson.toJson(this);
        // Write to file
        try (FileWriter fw = new FileWriter(outputPath)) {
            fw.write(json);
        }
    }
    
    /**
     * Serialize the current parameters bean to a json file.
     * The file name will be parameters.json and it will be placed in the
     * output folder defined inside of the <code>OrienteeringPropertiesBean</code>.
     * @throws IOException if there are problems with the output file.
     */
    public void serializeToJSON()
            throws IOException{
        this.serializeToJSON(
            this.OrienteeringProperties.getOutputFolderPath()
            + ParametersBean.OUTPUT_JSON_NAME
        );
    }
    
    
    
    /**
     * Deserializes the parameters bean specified at the specific path and uses it
     * to initialize the current one.
     * @param inputPath path to the input JSON file for properties
     * @throws IOException if there are problems with opening the file.
     */
    public void deserializeFromJSON(String inputPath)
            throws IOException{
        Gson gson = new Gson();
        String content = new String(Files.readAllBytes(Paths.get(inputPath)));
        ParametersBean out = gson.fromJson(content, ParametersBean.class);
        
        this.setOrienteeringProperties(out.getOrienteeringProperties());
        this.setALNSproperties(out.getALNSproperties());
    }
}
