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
    private ArrayList<String> responseList;
    private ArrayList<Grading> gradingResultList;
    
    GradingOutput(){}
    
    GradingOutput (ArrayList<String>  _gradingResponseList, ArrayList<Grading> _gradingResutlList){
        responseList = _gradingResponseList;
        gradingResultList = _gradingResutlList;
    }
    @XmlElement
    public ArrayList<String> getResponseList() {return responseList;}
    
    @XmlElement
    public ArrayList<Grading> getGradingResultList() {return gradingResultList;}
}