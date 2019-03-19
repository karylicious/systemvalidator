package com.clieser;

import javax.xml.bind.annotation.*;
/**
 *
 * @author Carla Augusto
 */
@XmlRootElement
public class TestResult {
    private String projectOwner;
    private String title;
    private String result;
    
    TestResult(){}
    
    TestResult(String _projectOwner, String _title, String _result){
        projectOwner = _projectOwner;
        title = _title;
        result = _result;
    }
    
    @XmlElement
    public String getProjectOwner() {return projectOwner;}
    
    @XmlElement
    public String getTitle() {return title;}
    
    @XmlElement
    public String getResult() {return result;}
}
