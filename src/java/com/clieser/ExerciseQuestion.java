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
public class ExerciseQuestion {
    private String expectedInvokedMethod;
    private double points;
    String expectedOutput;
    
    ExerciseQuestion(String _expectedInvokedMethod, String _expectedOutput, double _points){
        expectedInvokedMethod = _expectedInvokedMethod;
        points = _points;
        expectedOutput = _expectedOutput;
    }
    
    public String getExpectedInvokedMethod(){ return expectedInvokedMethod; }
    
    public double getPoints(){ return points; }    
    
    public String getExpectedOutput(){ return expectedOutput; }  
}
