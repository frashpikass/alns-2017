/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverView.bindingInterfaces;

import org.jdesktop.beansbinding.Converter;

/**
 * <code>Double</code> to <code>String</code> converter
 * that returns zero when the argument cannot be parsed.
 * 
 * @author Frash
 */
public class DoubleConverter extends Converter<Double, String> {

    @Override
    public Double convertReverse(String value) {
        double output;
        try{
            output = Double.parseDouble(value);
        }
        catch(Exception e){
            output = 0.0;
        }
        //System.out.println("Converting Reverse"+value+"->"+output);
        return output;
    }

    @Override
    public String convertForward(Double value) {
        return value.toString();
    }
    
}
