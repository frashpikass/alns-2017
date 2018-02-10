/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverView.bindingInterfaces;

import org.jdesktop.beansbinding.Validator;

/**
 * Class which validates positive longs and ensures they are greater than 0.
 * @author Frash
 */
public class PositiveLongValidator extends Validator<Long>{

    @Override
    public Validator.Result validate(Long value) {
        if ((value <= 0)) {
            return new Validator.Result(null, "This value must be a positive number!");
        }
        
        return null; 
    }
    
}
