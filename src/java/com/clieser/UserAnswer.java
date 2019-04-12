/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.clieser;

/**
 *
 * @author Carla-PC
 */
public class UserAnswer {
    private String actualInvokedMethod;
    private String actualOutput;
    
    UserAnswer(String _actualInvokedMethod, String _actualOutput){
        actualInvokedMethod = _actualInvokedMethod;
        actualOutput = _actualOutput;
    }
    
    public String getInvokedMethod(){ return actualInvokedMethod; }    
    
    public String getActualOutput(){ return actualOutput; }  
}