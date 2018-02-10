/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverView.bindingInterfaces;

import org.jdesktop.beansbinding.Validator;

/**
 * Class which validates positive doubles and ensures they are greater or equal
 * than 0.
 * @author Frash
 */
public class PositiveDoubleValidator extends Validator<Double>{

    @Override
    public Result validate(Double value) {
        if ((value <= 0)) {
            return new Result(null, "This value must be a positive number!");
        }
        
        return null; 
    }
    
}
