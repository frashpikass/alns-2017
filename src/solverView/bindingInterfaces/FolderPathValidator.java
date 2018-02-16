/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverView.bindingInterfaces;

import java.io.File;
import org.jdesktop.beansbinding.Validator;

/**
 * Class which validates a folder path and ensures it exists.
 * @author Frash
 */
public class FolderPathValidator extends Validator<String>{

    @Override
    public Validator.Result validate(String value) {
        File file=new File(value);
        
        if (!(file.isDirectory() && file.exists())) {
            return new Validator.Result(null, "This string must be a valid directory!");
        }
        
        return null; 
    }
    
}