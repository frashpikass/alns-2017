/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverView.bindingInterfaces;

import org.jdesktop.beansbinding.Converter;

/**
 * <code>Long</code> to <code>String</code> converter
 * that returns zero when the argument cannot be parsed.
 * 
 * @author Frash
 */
public class LongConverter extends Converter<Long, String> {

    @Override
    public Long convertReverse(String value) {
        Long output;
        output = Long.parseLong(value);
        return output;
    }

    @Override
    public String convertForward(Long value) {
        return value.toString();
    }
    
}