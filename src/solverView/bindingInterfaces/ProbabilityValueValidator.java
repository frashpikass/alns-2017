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
public class ProbabilityValueValidator extends Validator<Double>{

    @Override
    public Validator.Result validate(Double value) {
        if ((value < 0) || (value > 1)) {
            return new Validator.Result(null, "This value must be floating point within range [0, 1]");
        }
        
        return null; 
    }
    
}