package com.clieser;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Carla Augusto
 */
@XmlRootElement
public class GradingOutput {
    private ArrayList<String> gradingResponseList;
    private ArrayList<Grading> gradingList;
    
    GradingOutput(){}
    
    GradingOutput (ArrayList<String>  _gradingResponseList, ArrayList<Grading> _gradingList){
        gradingResponseList = _gradingResponseList;
        gradingList = _gradingList;
    }
    @XmlElement
    public ArrayList<String> getGradingResponseList() {return gradingResponseList;}
    
    @XmlElement
    public ArrayList<Grading> getGradingList() {return gradingList;}
}