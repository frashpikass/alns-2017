/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverView.bindingInterfaces;

import org.jdesktop.beansbinding.Validator;

/**
 * Class which validates positive integers and ensures they are greater than 0.
 * @author Frash
 */
public class PositiveIntegerValidator extends Validator<Integer>{

    @Override
    public Validator.Result validate(Integer value) {
        if (!(value > 0 )) {
            return new Validator.Result(value, "This value must be a positive integer!");
        }
        
        return null; 
    }
    
}