/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverController;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * A JavaBean that holds parameters for both ALNS and Orienteering
 * @author Frash
 */
public class ParametersBean implements Serializable {
    static final long serialVersionUID = 0;
    
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
        OrienteeringPropertiesBean oldOrienteeringProperties = this.OrienteeringProperties;
        this.OrienteeringProperties = OrienteeringProperties;
        this.propertyChangeSupport.firePropertyChange(PROP_ORIENTEERINGPROPERTIES, oldOrienteeringProperties, OrienteeringProperties);
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
        ALNSPropertiesBean oldALNSProperties = this.ALNSproperties;
        this.ALNSproperties = ALNSproperties;
        this.propertyChangeSupport.firePropertyChange(PROP_ALNSPROPERTIES, oldALNSProperties, ALNSproperties);
    }
    
    /**
     * Serialize this bean to a humanly readable JSON string.
     * @return a humanly readable JSON string representing this bean
     */
    public String toJSON(){
        // Create a new Gson object
        // The setPrettyPrinting option should make the output file more humanly readable
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        // Serialize to JSON
        String json = gson.toJson(this);
        return json;
    }
    
    /**
     * Serialize the current parameters bean to a json file.
     * The file name will be the one specified in the parameter <code>outputPath</code>.
     * @param outputPath path to the output file with parameters.
     * @throws IOException if there are problems with the output file.
     */
    public void serializeToJSON(String outputPath)
            throws IOException{
        // Serialize to JSON
        String json = this.toJSON();
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
            + "/"+ParametersBean.OUTPUT_JSON_NAME
        );
    }
    
    
    
    /**
     * Deserializes the parameters bean specified at the specific path and uses it
     * to initialize the current one.
     * 
     * @param inputPath path to the input JSON file for properties
     * @throws IOException if there are problems with opening the file.
     */
    public void deserializeFromJSON(String inputPath)
            throws IOException{
        Gson gson = new Gson();
        String content = new String(Files.readAllBytes(Paths.get(inputPath)));
        ParametersBean out = gson.fromJson(content, ParametersBean.class);
        
        this.OrienteeringProperties.cloneFrom(out.getOrienteeringProperties());
        this.ALNSproperties.cloneFrom(out.getALNSproperties());
    }
    
    public static final String PROP_ALNSPROPERTIES = "ALNSProperties";
    public static final String PROP_ORIENTEERINGPROPERTIES = "OrienteeringProperties";
    
    // <editor-fold defaultstate="collapsed" desc="PropertyChange Stuff">
    /**
     * Property change support object.
     */
    private final transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    
    /**
     * Adds a PropertyChangeListener to start listening to events
     * @param listener the listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }
    
    /**
     * Removes a PropertyChangeListener to stop listening to events
     * @param listener the listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
    // </editor-fold>
}
