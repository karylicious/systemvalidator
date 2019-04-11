package com.clieser;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Carla Augusto
 */
@XmlRootElement
public class ExerciseQuestionList {
    private ArrayList<ExerciseQuestion> exerciseQuestionList;
    
    ExerciseQuestionList(){}
    
    ExerciseQuestionList (ArrayList<ExerciseQuestion>  _exerciseQuestionList){
        exerciseQuestionList = _exerciseQuestionList;
    }
    @XmlElement
    public ArrayList<ExerciseQuestion> getExerciseQuestionList() {return exerciseQuestionList;}
}