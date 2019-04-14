package com.clieser;

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