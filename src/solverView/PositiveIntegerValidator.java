/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverView;

import org.jdesktop.beansbinding.Validator;

/**
 * Class which validates positive integers and ensures they are greater or equal
 * than 0.
 * @author Frash
 */
public class PositiveIntegerValidator extends Validator<Integer>{

    @Override
    public Validator.Result validate(Integer value) {
        if ((value >= 0 )) {
            return new Validator.Result(null, "This value must be a positive integer or zero!");
        }
        
        return null; 
    }
    
}