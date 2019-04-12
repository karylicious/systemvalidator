package com.clieser;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Carla Augusto
 */
@XmlRootElement
public class Grading {
    private ArrayList<String> responseList;
    private ArrayList<GradingResult> gradingResultList;
    private String finalGrade;
    
    Grading(){}
    
    Grading (ArrayList<String>  _gradingResponseList, ArrayList<GradingResult> _gradingResutlList, String _finalGrade){
        responseList = _gradingResponseList;
        gradingResultList = _gradingResutlList;
        finalGrade = _finalGrade;
    }
    @XmlElement
    public ArrayList<String> getResponseList() {return responseList;}
    
    @XmlElement
    public ArrayList<GradingResult> getGradingResultList() {return gradingResultList;}
    
    @XmlElement
    public String getFinalGrade() {return finalGrade;}
}