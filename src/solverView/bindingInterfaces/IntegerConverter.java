/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverView.bindingInterfaces;

import org.jdesktop.beansbinding.Converter;

/**
 * <code>Integer</code> to <code>String</code> converter
 * that returns zero when the argument cannot be parsed.
 * 
 * @author Frash
 */
public class IntegerConverter extends Converter<Integer, String> {

    @Override
    public Integer convertReverse(String value) {
        Integer output;
        try{
            output = Integer.parseInt(value);
        }
        catch(Exception e){
            output = 0;
        }
        return output;
    }

    @Override
    public String convertForward(Integer value) {
        return value.toString();
    }
    
}