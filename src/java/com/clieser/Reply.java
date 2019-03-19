package com.clieser;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Carla Augusto
 */
@XmlRootElement
public class Reply {
    private ArrayList<String> responseList;
    private ArrayList<TestResult> testResultList;
    
    Reply(){}
    
    Reply (ArrayList<String>  _responseList, ArrayList<TestResult> _testResultList){
        responseList = _responseList;
        testResultList = _testResultList;
    }
    @XmlElement
    public ArrayList<String> getResponseList() {return responseList;}
    
    @XmlElement
    public ArrayList<TestResult> getTestResultList() {return testResultList;}
}