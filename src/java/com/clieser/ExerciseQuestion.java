package com.clieser;

public class ExerciseQuestion {
    
    private String expectedInvokedMethod;
    private double points;
    private String expectedOutput;
    
    ExerciseQuestion(String _expectedInvokedMethod, String _expectedOutput, double _points){
        expectedInvokedMethod = _expectedInvokedMethod;
        points = _points;
        expectedOutput = _expectedOutput;
    }
    
    public String getExpectedInvokedMethod(){ return expectedInvokedMethod; }
    
    public double getPoints(){ return points; }    
    
    public String getExpectedOutput(){ return expectedOutput; }  
}