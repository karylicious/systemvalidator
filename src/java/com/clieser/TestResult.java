package com.clieser;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TestResult {
    
    private String projectOwner;
    private String title;
    private String hasPassed;
    
    TestResult(){}
    
    TestResult(String _projectOwner, String _title, String _hasPassed){
        projectOwner = _projectOwner;
        title = _title;
        hasPassed = _hasPassed;
    }
    
    @XmlElement
    public String getProjectOwner() {return projectOwner;}
    
    @XmlElement
    public String getTitle() {return title;}
    
    @XmlElement
    public String getHasPassed() {return hasPassed;}
}