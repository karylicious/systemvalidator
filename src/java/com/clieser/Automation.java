package com.clieser;

import java.io.File;
import javax.jws.WebService;
import javax.jws.WebMethod;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.activation.DataHandler;

@WebService(portName = "AutomationPort", serviceName = "Automation")
public class Automation {  
    
    @WebMethod(operationName = "gradeClientAndServer")
    public Grading gradeClientAndServer (String clientEntryPoint, DataHandler selectedFile, String selectedFileName, String exerciseQuestionXML){
        return Grader.gradeClientAndServer (clientEntryPoint, selectedFile, selectedFileName, exerciseQuestionXML);
    }
         
    @WebMethod(operationName = "gradeClient")
    public Grading gradeClient (String clientEntryPoint, DataHandler selectedFile, String selectedFileName, String exerciseQuestionXML){
        return Grader.gradeClient (clientEntryPoint, selectedFile, selectedFileName, exerciseQuestionXML);
    }
    
    @WebMethod(operationName = "testClient")
    public Reply testClient (String clientEntryPoint, DataHandler selectedFile, String selectedFileName){
       return Tester.testClient (clientEntryPoint, selectedFile, selectedFileName);
    }          
   
    @WebMethod(operationName = "testClientAndServer")
    public Reply testClientAndServer (String clientEntryPoint, DataHandler selectedFile, String selectedFileName){
        return Tester.testClientAndServer (clientEntryPoint, selectedFile, selectedFileName);
    }    
    
    @WebMethod(operationName = "deployServer")
    public String deployServer (DataHandler selectedFile, String selectedFileName){       
        
        String projectName = "";
        String userTemporaryDirectoryPath = "";
        
        try{
            FileAssistant.createLogAndTemporaryAndExercisesDirectories();                   
            
            userTemporaryDirectoryPath = FileAssistant.getTempDirectoryPath() + "\\" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            FileAssistant.createDirectory(userTemporaryDirectoryPath);      
            
            FileAssistant.uploadFile(selectedFile, selectedFileName, userTemporaryDirectoryPath);        
            String zipFilePath = userTemporaryDirectoryPath + "\\" + selectedFileName;
            
            ArrayList<String> project = FileAssistant.unzipAndGetTheProjectsToBeTested(zipFilePath, FileAssistant.getExercisesDirectoryPath(), true);      
            if(project.isEmpty()){
                FileAssistant.deleteDirectory(new File(userTemporaryDirectoryPath));
                return "There is already a deployed web service with the same name as the one on the zip file! Please try again with a different name.";
            }
            
            projectName = project.get(0); 
            projectName = projectName.replaceAll("/build/", "");
            
            String serverDirectoryPath = FileAssistant.getExercisesDirectoryPath() + "\\" + projectName;                       
                        
            boolean isServerDeployed = ServerAssistant.hasServerBeenDeployed(serverDirectoryPath, userTemporaryDirectoryPath);
            FileAssistant.deleteDirectory(new File(userTemporaryDirectoryPath));
            
            
            if(!isServerDeployed){
                FileAssistant.createLogFile( serverDirectoryPath);
                FileAssistant.deleteDirectory(new File(serverDirectoryPath));    
                return "The uploaded project is not a web service! Please upload a valid web service and try again.";
            }
        }
        catch(Exception e){
            FileAssistant.createLogFile( e.toString());
            FileAssistant.deleteDirectory(new File(userTemporaryDirectoryPath));
        }   
        return projectName + "/";
    }
      
    @WebMethod(operationName = "undeployServer")
    public boolean undeployServer (String serverDirectoryName){
        
        String serverDirectoryPath = FileAssistant.getExercisesDirectoryPath() + "\\" + serverDirectoryName;
        String userTemporaryDirectoryPath = FileAssistant.getTempDirectoryPath() + "\\" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        
        try{
            FileAssistant.createDirectory(userTemporaryDirectoryPath);
            ServerAssistant.undeplopyServer(serverDirectoryPath, userTemporaryDirectoryPath);
            FileAssistant.deleteDirectory(new File(userTemporaryDirectoryPath)); //remove comment
            FileAssistant.deleteDirectory(new File(serverDirectoryPath)); //remove comment
        }
        catch(Exception e){
            FileAssistant.createLogFile( e.toString() );
            return false;
        }  
        return true;
    }      
}