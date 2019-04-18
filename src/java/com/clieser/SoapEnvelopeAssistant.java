package com.clieser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class SoapEnvelopeAssistant {
    
    private static ArrayList<UserAnswer> userAnswerList;
    private static ArrayList<String> parametersList, testResponseValue;
    private static boolean hasResponseMultipleValues;
    
    private static boolean doesContainExactWord(String source, String wordTofind){
        String regularExpression = ".*"+wordTofind+".*";
        Pattern p=Pattern.compile(regularExpression);
        
        // Creates a matcher that will match the given input against this pattern.
        Matcher m=p.matcher(source);
        return m.matches();
    }
    
    public static ArrayList<String> getListOfInvokedMethodsNames(String clientDirectoryPath, ArrayList<String> serverMethodsList){      
        ArrayList<String> invokedMethodsNameList = new ArrayList();
        try{            
            File file = new File(clientDirectoryPath + "\\traced-soap-traffic.txt");   
            BufferedReader br = new BufferedReader(new FileReader(file)); 
            String line;              
            
            boolean foundDuplicate = false;
            while (( line = br.readLine()) != null) {              
                if ( line.contains("SOAPAction:")){
                    for (String method : serverMethodsList){                        
                        if (doesContainExactWord(line, method+"Request")){
                            if ( invokedMethodsNameList.isEmpty())
                                invokedMethodsNameList.add(method);
                            else{
                                for (String methodInvoked : invokedMethodsNameList){
                                    if (methodInvoked.equals(method)){
                                        foundDuplicate = true;
                                        break;
                                    }                                    
                                }
                                if ( !foundDuplicate)
                                    invokedMethodsNameList.add(method);
                                foundDuplicate = false;
                            }              
                            break;
                        }
                    }
                }
            }  
            br.close();            
        }
        catch(Exception e){
            FileAssistant.createLogFile ( e.toString());
        }
        return invokedMethodsNameList;
    }    
    
    private static NodeList getSoapBodyOfFirstChildNodes(String soapEnvelope) {
        NodeList childNodes = null;
        try {
            // Generate soap message from soapEnvelope string
            
            // A factory for creating SOAPMessage objects
            MessageFactory messageFactory = MessageFactory.newInstance();
            
            //Creates a new SOAPMessage object with the default SOAPPart, SOAPEnvelope, SOAPBody, and SOAPHeader objects. 
            SOAPMessage soapMessageObject = messageFactory.createMessage();
            
            SOAPPart messagePart = soapMessageObject.getSOAPPart();
            
            StreamSource content = new StreamSource(new StringReader(soapEnvelope));
            messagePart.setContent(content);    
            
            SOAPEnvelope envelope = messagePart.getEnvelope();            
            
            // The Node Body will always have just one child
            childNodes = envelope.getBody().getFirstChild().getChildNodes();            
            
        } 
        catch (Exception e) {
            FileAssistant.createLogFile ( e.toString());
        }
        return childNodes;
    }    
    
    private static void populateTheParameterListVariableWithTheNodesValue(NodeList nodeList) {
        for (int count = 0; count < nodeList.getLength(); count++) {
            Node tempNode = nodeList.item(count);
            
            if (tempNode.getNodeType() == Node.ELEMENT_NODE) {    

                // GET THE VALUES ONLY IF THE CURRENT NODE IS A PARENT
                // IN CASE IT IS A PARENT, THE NAME OF THE FIRST CHILD WILL NEVER RETURN "#text" 
                // THE "#text" VALUE COMES FROM THE XML SPECIFICATION)
                
                if ( !tempNode.getFirstChild().getNodeName().equals("#text") ){
                    // Start of the node that contains the user-defined type 
                    parametersList.add("Parameter Name = " + tempNode.getNodeName() + "  ( This is a user-defined type which contains the following )");
                    parametersList.add("[START]");
                    parametersList.add("\n");
                } 

                // loop again if has child nodes
                
                if (tempNode.hasChildNodes())
                    populateTheParameterListVariableWithTheNodesValue(tempNode.getChildNodes());

                // GET THE VALUES ONLY IF THE CURRENT NODE IS NOT A PARENT
                // IN CASE IT IS NOT A PARENT, THE NAME OF THE FIRST CHILD WILL ALWAYS RETURN "#text" 
                // THE "#text" VALUE COMES FROM THE XML SPECIFICATION)
                
                if ( tempNode.getFirstChild().getNodeName().equals("#text") ){
                    parametersList.add("Parameter Name = " + tempNode.getNodeName());
                    parametersList.add("Parameter Value = " + tempNode.getFirstChild().getTextContent());
                    parametersList.add("\n");
                }      
                else{
                    //End of the node that contains the user-defined type 
                    parametersList.add("[END]");
                    parametersList.add("\n");
                }                 
            }
        }       
    }    
    
    private static void populateTheResponseValueListVariableWithTheNodesValue(NodeList nodeList) {        
        for (int count = 0; count < nodeList.getLength(); count++) {

            Node tempNode = nodeList.item(count);

            if (tempNode.getNodeType() == Node.ELEMENT_NODE) {  
                
                // loop again if has child nodes
                if (tempNode.hasChildNodes())
                    populateTheResponseValueListVariableWithTheNodesValue(tempNode.getChildNodes());

                // GET THE VALUES ONLY IF THE CURRENT NODE IS NOT A PARENT
                // IN CASE IT IS NOT A PARENT, THE NAME OF THE FIRST CHILD WILL ALWAYS RETURN "#text" 
                // THE "#text" VALUE COMES FROM THE XML SPECIFICATION)
                
                try{
                    if ( tempNode.getFirstChild().getNodeName().equals("#text") ){

                        if (!tempNode.getNodeName().equals("return")){
                            if (!hasResponseMultipleValues){
                                hasResponseMultipleValues = true;
                                testResponseValue.add("The Server response includes multiple values:");
                                testResponseValue.add("\n");
                            }
                            testResponseValue.add("Parameter Name = " + tempNode.getNodeName());
                            testResponseValue.add("Parameter Value = " + tempNode.getFirstChild().getTextContent());
                            testResponseValue.add("\n");
                        }
                        else
                            testResponseValue.add("Server Returned Value = " + tempNode.getFirstChild().getTextContent() + "\n");                        
                    }  
                }
                catch(Exception e){
                    // The exeception means that the server has returned an exception as response
                    FileAssistant.createLogFile ( e.toString());
                }
            }
        }       
    }        
    
    public static ArrayList<String> getListOfInvokedMethodsDetails(String clientDirectoryPath, ArrayList<String> listOfInvokedMethodsNames){
        ArrayList<String> details = new ArrayList();
        
        try{            
            File file = new File(clientDirectoryPath + "\\traced-soap-traffic.txt");   
            BufferedReader br = new BufferedReader(new FileReader(file)); 
            String line;              
            
            boolean isTheRequestSection = false;
            boolean isTheResponseSection = false;
            
            while (( line = br.readLine()) != null) {              
                if ( line.contains("SOAPAction:")){
                    isTheRequestSection = true;
                    for (String method : listOfInvokedMethodsNames){
                        if (doesContainExactWord(line, method+"Request")){
                            details.add("\n");
                            details.add("Method Name - " + method);
                            break;
                        }
                    }
                }
                else if (isTheRequestSection){  
                    // Get the line that contains the Soap Envelope to retrive the methods invoked by the client
                    // Then retrieve the parameters and values passed by the Client
                    
                    if ( line.contains("<S:Envelope")){
                        isTheRequestSection = false;
                        
                        int index1=line.indexOf("<S:Envelope"); 
                        int index2=line.indexOf("</S:Envelope>");
                        String envelope = "</S:Envelope>";
                        
                        String soapEnvelope = line.substring(index1, (index2 + envelope.length()));                        
                        NodeList list = getSoapBodyOfFirstChildNodes(soapEnvelope);
                        
                        parametersList = new ArrayList();
                        populateTheParameterListVariableWithTheNodesValue(list);
                        
                        for ( String parameter : parametersList)
                            details.add(parameter);                        
                    }
                }
                else if ( line.contains("---[HTTP response")){
                    isTheResponseSection = true;                    
                }
                else if (isTheResponseSection) {
                    // Get the line that contains the Soap Envelope to retrive the methods invoked by the client
                    // Then retrieve the response of the Server
                    
                    if ( line.contains("<S:Envelope")){
                        isTheResponseSection = false;
                        
                        int index1=line.indexOf("<S:Envelope");   
                        int index2=line.indexOf("</S:Envelope>");
                        String envelope = "</S:Envelope>";      
                        
                        String soapEnvelope = line.substring(index1, (index2 + envelope.length()));                            
                        NodeList list = getSoapBodyOfFirstChildNodes(soapEnvelope);

                        testResponseValue = new ArrayList();
                        hasResponseMultipleValues = false;
                        populateTheResponseValueListVariableWithTheNodesValue(list);
                        
                        for ( String response : testResponseValue)
                            details.add(response);                        
                    }
                }
            }  
            br.close();
        }
        catch(Exception e){
            FileAssistant.createLogFile ( e.toString());
        }
        return details;
    }
        
    public static ArrayList<ExerciseQuestion> getExerciseQuestions(String exerciseQuestionXML){
        //Example of the content on the exerciseQuestionXML variable
        
        // This structure is defined in the GradeClientServer class on Flask when the get() method of the GradeClientServer class is called
       
        /*<?xml version="1.0" encoding="UTF-8"?>
        <questions>
            <question>
                <expectedinvokedmethod>isConnected</expectedinvokedmethod>
                <points>20.0</points>
                <expectedoutput>true</expectedoutput>
            </question>
        </questions>
        */
             
        ArrayList<ExerciseQuestion> list = new ArrayList();
        try{
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(exerciseQuestionXML));
            Document d = builder.parse(is);

            NodeList elements = d.getElementsByTagName("questions");
            NodeList questionNodes = elements.item(0).getChildNodes();         
            
            for (int i = 0; i < questionNodes.getLength(); i++) {
                
                NodeList questionChildNodes = questionNodes.item(i).getChildNodes();   
                
                String expectedinvokedmethod = questionChildNodes.item(0).getFirstChild().getTextContent();
                String points = questionChildNodes.item(1).getFirstChild().getTextContent(); 
                String expectedoutput = questionChildNodes.item(2).getFirstChild().getTextContent();                
                
                list.add(new ExerciseQuestion(expectedinvokedmethod, expectedoutput, Double.parseDouble(points)));                   
            }        
        }
        catch(Exception e){
            FileAssistant.createLogFile ( e.toString());
        }
        return list;
    }    
    
    public static ArrayList<UserAnswer> getUserAnswerList(String clientDirectoryPath,ArrayList<ExerciseQuestion> exerciseQuestionsList){
        try{            
            File file = new File(clientDirectoryPath + "\\traced-soap-traffic.txt");   
            BufferedReader br = new BufferedReader(new FileReader(file)); 
            String line;              
            
            boolean isTheResponseSection = false;
            String currentMethodName = "";
            userAnswerList = new ArrayList();
            
            while (( line = br.readLine()) != null) {              
                if ( line.contains("SOAPAction:")){
                    for (ExerciseQuestion question : exerciseQuestionsList){
                        if (doesContainExactWord(line, question.getExpectedInvokedMethod()+"Request")){
                            currentMethodName = question.getExpectedInvokedMethod();
                            break;
                        }
                    }
                }                
                else if ( line.contains("---[HTTP response")){
                    isTheResponseSection = true;                    
                }
                else if (isTheResponseSection && !currentMethodName.isEmpty()) {
                    // Get the line that contains the Soap Envelope to retrive the methods invoked by the client
                    // Then retrieve the response of the Server
                    
                    if ( line.contains("<S:Envelope")){
                        isTheResponseSection = false;
                        
                        int index1=line.indexOf("<S:Envelope");   
                        int index2=line.indexOf("</S:Envelope>");
                        String envelope = "</S:Envelope>";      
                        
                        String soapEnvelope = line.substring(index1, (index2 + envelope.length()));                            
                        NodeList list = getSoapBodyOfFirstChildNodes(soapEnvelope);
                        
                        populateTheUserAnswerListVariableIncludingServerOutput(list, currentMethodName);                       
                        currentMethodName ="";                                              
                    }
                }
            }  
            br.close();
        }
        catch(Exception e){
            FileAssistant.createLogFile ( e.toString());
        }
        return userAnswerList;
    }    
    
    private static void populateTheUserAnswerListVariableIncludingServerOutput(NodeList nodeList, String userInvokedMethod) {        
        for (int count = 0; count < nodeList.getLength(); count++) {

            Node tempNode = nodeList.item(count);

            if (tempNode.getNodeType() == Node.ELEMENT_NODE) {  
                
                // loop again if has child nodes
                if (tempNode.hasChildNodes())
                    populateTheResponseValueListVariableWithTheNodesValue(tempNode.getChildNodes());

                // GET THE VALUES ONLY IF THE CURRENT NODE IS NOT A PARENT
                // IN CASE IT IS NOT A PARENT, THE NAME OF THE FIRST CHILD WILL ALWAYS RETURN "#text" 
                // THE "#text" VALUE COMES FROM THE XML SPECIFICATION)
                
                try{
                    if ( tempNode.getFirstChild().getNodeName().equals("#text") ){
                        if (tempNode.getNodeName().equals("return")) {                            
                            userAnswerList.add(new UserAnswer(userInvokedMethod, tempNode.getFirstChild().getTextContent()));     
                        }
                    }  
                }
                catch(Exception e){
                    //The execption means that the server has returned an exception as response
                    userAnswerList.add(new UserAnswer(userInvokedMethod, "[Server has generated an exception]"));
                    FileAssistant.createLogFile ( e.toString());
                }
            }
        }       
    }        
}