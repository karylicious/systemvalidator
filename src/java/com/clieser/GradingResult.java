package com.clieser;

import javax.xml.bind.annotation.*;
/**
 *
 * @author Carla Augusto
 */
@XmlRootElement
public class GradingResult {
    private String projectOwner;
    private String title;
    private String hasPassed;
    private String actualTestOutput;
    private String grade;
    
    GradingResult(){}
    
    GradingResult(String _projectOwner, String _title, String _hasPassed, String _actualTestOutput, String _grade){
        projectOwner = _projectOwner;
        title = _title;
        hasPassed = _hasPassed;
        actualTestOutput = _actualTestOutput;
        grade = _grade;
    }
    
    @XmlElement
    public String getProjectOwner() {return projectOwner;}
    
    @XmlElement
    public String getTitle() {return title;}
    
    @XmlElement
    public String getHasPassed() {return hasPassed;}
    
    @XmlElement
    public String getActualTestOutput() {return actualTestOutput;}
    
    @XmlElement
    public String getGrade() {return grade;}
}
