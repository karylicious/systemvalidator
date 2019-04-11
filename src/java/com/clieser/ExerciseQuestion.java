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
    private String methodName;
    private String points;
    
    
    ExerciseQuestion(String _methodName, String _points){
        methodName = _methodName;
        points = _points;
    }
    
    public String getMethodName(){ return methodName; }
    
    public String getPoints(){ return points; }    
}
