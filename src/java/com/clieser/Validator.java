package com.clieser;

import java.io.File;
import javax.jws.WebService;
import javax.jws.WebMethod;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.activation.DataHandler;

/**
 *
 * @author Carla Augusto
 */
@WebService(portName = "ValidatorPort", serviceName = "Validator")
public class Validator {  
    
    //@WebMethod(operationName = "gradeClientProject")
    //public GradingOutput gradeClientProject (String clientEntryPoint, DataHandler selectedFile, String selectedFileName, ArrayList<ExerciseQuestion> exerciseQuestionList){
                
        //return new GradingOutput();
    //}
    
    //@WebMethod(operationName = "gradeClientServerProject")
    //public String gradeClientServerProject (DataHandler selectedFile, String selectedFileName){
        
        
        
    //}
    
    @WebMethod(operationName = "deployServer")
    public String deployServer (DataHandler selectedFile, String selectedFileName){
        
        Assistant assistant = Assistant.getInstance();
        assistant.initilizeGlobalVariables();
        String projectName = "";
        String userTemporaryDirectoryPath = "";
        
        try{
            assistant.createLogAndTemporaryDirectories();  
            String exercisesDirectoryPath = assistant.getTempDirectoryPath()  + "\\exercises";                     
            
            userTemporaryDirectoryPath = assistant.getTempDirectoryPath() + "\\" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            assistant.createDirectory(userTemporaryDirectoryPath);
           
            //assistant.addLog("Seelcy  " + selectedFileName);
            
            //assistant.createLogFile();
            assistant.uploadFile(selectedFile, selectedFileName, userTemporaryDirectoryPath);    
            
      
            String zipFilePath = userTemporaryDirectoryPath + "\\" + selectedFileName;
            
            ArrayList<String> project = assistant.unzipAndGetTheProjectsToBeTested(zipFilePath, exercisesDirectoryPath);      
            if(project.isEmpty()){
                assistant.deleteDirectory(new File(userTemporaryDirectoryPath));
                return "There is already a deployed web service with the same name as the one on the zip file! Please try again with a different name.";
            }
            
            projectName = project.get(0);
            String serverDirectoryPath = exercisesDirectoryPath + "\\" + projectName;
            
            boolean isServerDeployed = assistant.hasServerBeenDeployed(serverDirectoryPath, userTemporaryDirectoryPath);
            assistant.deleteDirectory(new File(userTemporaryDirectoryPath));
            
            if(!isServerDeployed){
                String information;
                if ( !new File(serverDirectoryPath + "\\build").exists() )                    
                    information = "The build directory has not been found on your web service! Please before trying to deploy the web service in this system, make sure you have deployed it using NetBeans IDE at least once.";
                else
                    information = "The uploaded project is not a web service! Please upload a valid web service and try again.";
                
                assistant.deleteDirectory(new File(serverDirectoryPath));    
                return information;
            }
        }
        catch(Exception e){
            assistant.addLog( e.toString());
            assistant.createLogFile();
            assistant.deleteDirectory(new File(userTemporaryDirectoryPath));
        }   
        return projectName;
    }
      
    @WebMethod(operationName = "undeployServer")
    public boolean undeployServer (String serverDirectoryName){
        Assistant assistant = Assistant.getInstance();
        assistant.initilizeGlobalVariables();
        
        String exercisesDirectoryPath = assistant.getTempDirectoryPath()  + "\\exercises"; 
        String serverDirectoryPath = exercisesDirectoryPath + "\\" + serverDirectoryName;
        String userTemporaryDirectoryPath = assistant.getTempDirectoryPath() + "\\" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        
        try{
            assistant.createDirectory(userTemporaryDirectoryPath);
            assistant.undeplopyServer(serverDirectoryPath, userTemporaryDirectoryPath);
            assistant.deleteDirectory(new File(userTemporaryDirectoryPath)); //remove comment
            assistant.deleteDirectory(new File(serverDirectoryPath)); //remove comment
        }
        catch(Exception e){
            assistant.addLog( e.toString());
            assistant.createLogFile();
            return false;
        }  
        return true;
    }
    
    @WebMethod(operationName = "testClient")
    public Reply testClient(String clientEntryPoint, DataHandler selectedFile, String selectedFileName){
        //Assuming that the Server is already running...
        Assistant assistant = Assistant.getInstance();
        assistant.initilizeGlobalVariables();         
        String userTemporaryDirectoryPath = ""; 
        try {        
            //>>>>>>>>>>>>>>>>    CREATION OF TEMPORAY DIRECTORIES AND UPLOAD FILE    <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            
            assistant.createLogAndTemporaryDirectories();  
            userTemporaryDirectoryPath = assistant.getTempDirectoryPath() + "\\" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String userUploadDirectoryPath = userTemporaryDirectoryPath + "\\upload";
            
            assistant.createDirectory(userUploadDirectoryPath);
            assistant.uploadFile(selectedFile, selectedFileName, userUploadDirectoryPath);  
            
            //>>>>>>>>>>>>>>>>    UNZIP OF THE FILE AND ITERATION OVER LIST OF PROJECTS FOUND    <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            
            String zipFilePath = userUploadDirectoryPath + "\\" + selectedFileName;
            String unzipLocation = userTemporaryDirectoryPath + "\\"+selectedFileName; 
            
            //   unzip inside of a new directory which as the same name as the zip file name
            ArrayList<String> listOfProjectsToBeTested = assistant.unzipAndGetTheProjectsToBeTested(zipFilePath, unzipLocation);            
                    
            for(int i = 0; i < listOfProjectsToBeTested.size(); i++){
                assistant.addTestResponse("[INFO] Testing " + (i+1) + " out of " + listOfProjectsToBeTested.size() + " projects\n\n\n");
                assistant.addTestResponse("\n\n");                
                
                String NameOfTheProjectBeingTested = selectedFileName + ".zip";       
                String[] projectName = listOfProjectsToBeTested.get(i).split(".zip");                
                String pathProjectBeingTested = unzipLocation +"\\" + projectName[0];        
                
                if(listOfProjectsToBeTested.size() > 1){   
                    
                    //>>>>>>>>>>>>>>>>>>  IN CASE OF MULTIPLE PROJECTS UNZIP ONE BY ONE  <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                    
                    zipFilePath = userTemporaryDirectoryPath+ "\\"+selectedFileName +"\\"+listOfProjectsToBeTested.get(i);                  
                           
                    //unzip inside of a new directory which as the same name as the zip file name but outside the parent directory                           
                                         
                    pathProjectBeingTested = userTemporaryDirectoryPath + "\\" + projectName[0];                     
                    assistant.unzipAndGetTheProjectsToBeTested(zipFilePath, pathProjectBeingTested);  
                    
                    String[] subDirectories = assistant.getSubdirectories(pathProjectBeingTested);
                    
                     //The array will expect to have just one directory with the name 
                    pathProjectBeingTested += "\\" +subDirectories[0];
                    
                    NameOfTheProjectBeingTested = listOfProjectsToBeTested.get(i); 
                }                
                
                assistant.addTestResponse( "[INFO] Project selected: "+ NameOfTheProjectBeingTested +"\n");            
                assistant.addTestResponse("[INFO] System is searching for the Client stubs\n\n");
                
                //Whenever a new web service reference is added on the client, the wsimport command generates the stubs under the following directories
                if ( !new File(pathProjectBeingTested + "\\build\\generated-sources\\jax-ws").exists() ){
                    assistant.addTestResponse("[INFO] System did not find the Client stubs\n\n");
                    assistant.addTestResponse("\n\n");
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client is not correctly connected to the server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));                    
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    continue;
                }          
                
                assistant.addTestResponse("[INFO] System found the Client stubs\n\n");                  
                assistant.addTestResponse("[INFO] System is searching for the main class of the Client\n\n");                               
                assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client is correctly connected to the server","true"));
                
                
                String entryPoint = clientEntryPoint.replace('.', '\\');
                
                if(!Files.exists(Paths.get(pathProjectBeingTested + "\\src\\" + entryPoint + ".java"))){
                    assistant.addTestResponse("[INFO] System did not find the main class of the Client\n\n");
                    assistant.addTestResponse("\n\n");                    
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    continue;
                }
                               
                assistant.addTestResponse("[INFO] System found the main class of the Client\n\n");
                assistant.addTestResponse("[INFO] System is trying to start the Client\n\n");                                  
                boolean hasClientStarted = assistant.didRunClientAndSaveOutput(clientEntryPoint, pathProjectBeingTested, userTemporaryDirectoryPath);

                if(!hasClientStarted){
                    assistant.addTestResponse("[INFO] Client did not start. Please check your code\n\n");
                    assistant.addTestResponse("\n\n");
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));                    
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    continue;
                }   

                assistant.addTestResponse("[INFO] Client has started\n\n");
                assistant.addTestResponse("[INFO] System is verifying whether Client has communicated with the Server\n\n");                
                assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did start","true"));    
                
                if( !assistant.didClientCommunicatedWithServer(pathProjectBeingTested) ){
                    assistant.addTestResponse("[INFO] Client did not communicate with the Server. Please check your code\n\n");
                    assistant.addTestResponse("\n\n");                  
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    continue;
                }
                
                assistant.addTestResponse("[INFO] Client has communicated with the Server\n\n");
                assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did communicate with the Server","true"));                 
                
                ArrayList<String> serverMethodsList = assistant.getMethodsAvailableOnServer(pathProjectBeingTested);                
                ArrayList<String> listOfInvokedMethodsNames = assistant.getListOfInvokedMethodsNames(pathProjectBeingTested, serverMethodsList);                
                assistant.addTestResponse("[INFO] Client has invoked " + listOfInvokedMethodsNames.size() + " out of " + serverMethodsList.size() + " methods\n\n");
                                
                ArrayList<String> detailsList = assistant.getListOfInvokedMethodsDetails(pathProjectBeingTested, listOfInvokedMethodsNames);
                if ( !detailsList.isEmpty()){
                    assistant.addTestResponse("[INFO] Methods invoked by the Client:");
                    for (String details : detailsList) {
                        assistant.addTestResponse ( details);
                        if (details.equals(""))
                            assistant.addTestResponse("\n\n");
                    }
                    
                }                               
                assistant.addTestResponse("\n\n");
            }
        }
        catch(Exception e){
            assistant.addLog( e.toString());
            assistant.createLogFile();
        }     
        return assistant.terminateTest(userTemporaryDirectoryPath);
    }          
   
    @WebMethod(operationName = "testClientAndServer")
    public Reply testClientAndServer(String clientEntryPoint, DataHandler selectedFile, String selectedFileName){
        Assistant assistant = Assistant.getInstance();
        assistant.initilizeGlobalVariables();  
        String userTemporaryDirectoryPath = "";
        
        try {     
            //>>>>>>>>>>>>>>>>    CREATION OF TEMPORAY DIRECTORIES AND UPLOAD FILE    <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            
            assistant.createLogAndTemporaryDirectories();  
            userTemporaryDirectoryPath = assistant.getTempDirectoryPath() + "\\" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String userUploadDirectoryPath = userTemporaryDirectoryPath + "\\upload";
            
            assistant.createDirectory(userUploadDirectoryPath);
            assistant.uploadFile(selectedFile, selectedFileName, userUploadDirectoryPath);  
            String zipFilePath = userUploadDirectoryPath + "\\" + selectedFileName;

            //>>>>>>>>>>>>>>>>    UNZIP OF THE FILE AND ITERATION OVER LIST OF PROJECTS FOUND    <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            
            String unzipLocation = userTemporaryDirectoryPath + "\\"+selectedFileName; 
            
            //unzip inside of a new directory which as the same name as the zip file name
            ArrayList<String> listOfProjectsToBeTested = assistant.unzipAndGetTheProjectsToBeTested(zipFilePath, unzipLocation);      
            //String projectName ="";           
            for(int i = 0; i < listOfProjectsToBeTested.size(); i++){
                assistant.addTestResponse("[INFO] Testing " + (i+1) + " out of " + listOfProjectsToBeTested.size() + " projects\n\n\n");
                assistant.addTestResponse("\n\n");
                                
                String NameOfTheProjectBeingTested = selectedFileName + ".zip";                       
                String serverDirectoryPath = assistant.getServerProjectPath(unzipLocation); // Assuming that this is a Glassfish web service
                String clientDirectoryPath = assistant.getClientProjectPath(unzipLocation, clientEntryPoint);                
                
                //String[] name = serverDirectoryPath.split("\\");
                //projectName=name[1];
                 if(listOfProjectsToBeTested.size() > 1){     
                     
                    //>>>>>>>>>>>>>>>>>>  IN CASE OF MULTIPLE PROJECTS UNZIP ONE BY ONE  <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                    
                   /* zipFilePath = userTemporaryDirectoryPath+ "\\"+selectedFileName +"\\"+listOfProjectsToBeTested.get(i);  
                    
                    //unzip inside of a new directory which as the same name as the zip file name but outside the parent directory
                    unzipLocation = userTemporaryDirectoryPath;               
                                                  
                    assistant.unzipAndGetTheProjectsToBeTested(zipFilePath, unzipLocation);              
                        
                    
                    serverDirectoryPath = assistant.getServerProjectPath(unzipLocation); // Assuming that this is a Glassfish web service
                    clientDirectoryPath = assistant.getClientProjectPath(unzipLocation, clientEntryPoint);
                    
                    NameOfTheProjectBeingTested = listOfProjectsToBeTested.get(i);*/
                    
                    
                    //-==============================
                    zipFilePath = userTemporaryDirectoryPath+ "\\"+selectedFileName +"\\"+listOfProjectsToBeTested.get(i);                  
                       
                    String[] foundProjectName = listOfProjectsToBeTested.get(i).split(".zip");
                    //unzip inside of a new directory which as the same name as the zip file name but outside the parent directory                           
                                         
                    unzipLocation = userTemporaryDirectoryPath + "\\" + foundProjectName[0];                     
                    assistant.unzipAndGetTheProjectsToBeTested(zipFilePath, unzipLocation);  
                    
                    //String[] subDirectories = assistant.getSubdirectories(pathProjectBeingTested);
                    
                    serverDirectoryPath = assistant.getServerProjectPath(unzipLocation); // Assuming that this is a Glassfish web service
                    clientDirectoryPath = assistant.getClientProjectPath(unzipLocation, clientEntryPoint);
                    
                    
                     //The array will expect to have just one directory with the name 
                    //pathProjectBeingTested += "\\" +subDirectories[0];
                    
                    NameOfTheProjectBeingTested = listOfProjectsToBeTested.get(i); 
                    
                    //name = serverDirectoryPath.split("\\");
                    //projectName=name[1];
                }                                  
                assistant.addTestResponse( "[INFO] Project selected: "+ NameOfTheProjectBeingTested +"\n");   

                //>>>>>>>>>>>>>>>>>>>>>>>   TESTING SERVER   <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                
                assistant.addTestResponse("[INFO] System is searching for the Server project\n\n");    
                
                if( serverDirectoryPath.isEmpty() ){
                    assistant.addTestResponse("[INFO] System did not find the Server project\n\n");
                    assistant.addTestResponse("\n\n");                    
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did not start","false"));                           
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client is not correctly connected to the server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));                    
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));
                    continue;
                }
                
                assistant.addTestResponse("[INFO] System did find the Server project\n\n");                
                assistant.addTestResponse("[INFO] System is trying to deploy the Server\n\n");  
                
                if ( !assistant.hasServerBeenDeployed(serverDirectoryPath, userTemporaryDirectoryPath) ){
                    assistant.addTestResponse("[INFO] System did not manage to deploy the Server. Please check your code.\n\n");
                    assistant.addTestResponse("\n\n");                    
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did not start","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client is not correctly connected to the server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));                    
                    continue;
                }
                
                assistant.addTestResponse("[INFO] System has deployed Server\n\n");
                assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did start","true"));
                
                //>>>>>>>>>>>>>>>>>>>>>>>   TESTING CLIENT   <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                
                assistant.addTestResponse("[INFO] System is searching for the Client stubs\n\n");
                
                assistant.addLog(clientDirectoryPath);
                //Whenever a new web service reference is added on the client, the wsimport command generates the stubs under the following directories
                if ( !new File(clientDirectoryPath + "\\build\\generated-sources\\jax-ws").exists() ){
                    assistant.addTestResponse("[INFO] System did not find the Client stubs\n\n");
                    assistant.addTestResponse("\n\n");
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client is not correctly connected to the server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));                    
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));
                    continue;
                }          
                
                assistant.addTestResponse("[INFO] System found the Client stubs\n\n");                        
                assistant.addTestResponse("[INFO] System is searching for the main class of the Client\n\n");                
                assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client is correctly connected to the server","true"));
                
                String entryPoint = clientEntryPoint.replace('.', '\\');
                if(!Files.exists(Paths.get(clientDirectoryPath + "\\build\\classes\\" + entryPoint + ".class"))){
                    assistant.addTestResponse("[INFO] System did not find the main class of the Client\n\n");
                    assistant.addTestResponse("\n\n");                    
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));
                    continue;
                }
                               
                assistant.addTestResponse("[INFO] System found the main class of the Client\n\n");
                assistant.addTestResponse("[INFO] System is trying to start the Client\n\n");                      
                boolean hasClientStarted = assistant.didRunClientAndSaveOutput(clientEntryPoint, clientDirectoryPath, userTemporaryDirectoryPath);

                if(!hasClientStarted){
                    assistant.addTestResponse("[INFO] Client did not start. Please check your code\n\n");
                    assistant.addTestResponse("\n\n");
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));                    
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));
                    continue;
                }   

                assistant.addTestResponse("[INFO] Client has started\n\n");
                assistant.addTestResponse("[INFO] System is verifying whether Client has communicated with the Server\n\n"); 
                assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did start","true"));    
                
                if( !assistant.didClientCommunicatedWithServer(clientDirectoryPath) ){
                    assistant.addTestResponse("[INFO] Client did not communicate with the Server. Please check your code\n\n");
                    assistant.addTestResponse("\n\n");                  
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));
                    continue;
                }
                
                assistant.addTestResponse("[INFO] Client has communicated with the Server\n\n");
                assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did communicate with the Server","true"));                 
                assistant.addTestResponse("[INFO] System is verifying whether Server has communicated with the Client\n\n"); 
                
                if( !assistant.didServerCommunicatedWithClient(clientDirectoryPath, serverDirectoryPath) ){
                    assistant.addTestResponse("[INFO] Server did not communicate with the Client. Please check your code\n\n");
                    assistant.addTestResponse("\n\n");                  
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));
                    continue;
                }
                
                assistant.addTestResponse("[INFO] Server has communicated with the Client\n\n");
                assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did communicate with the Client","true"));                
                                
                
                assistant.undeplopyServer(serverDirectoryPath, userTemporaryDirectoryPath);                
                assistant.addTestResponse("[INFO] System has undeployed Server\n\n");
                
                ArrayList<String> serverMethodsList = assistant.getMethodsAvailableOnServer(clientDirectoryPath);   
                ArrayList<String> listOfInvokedMethodsNames = assistant.getListOfInvokedMethodsNames(clientDirectoryPath, serverMethodsList);                
                assistant.addTestResponse("[INFO] Client has invoked " + listOfInvokedMethodsNames.size() + " out of " + serverMethodsList.size() + " methods\n\n");                
                
                
                
                ArrayList<String> detailsList = assistant.getListOfInvokedMethodsDetails(clientDirectoryPath, listOfInvokedMethodsNames);
                if ( !detailsList.isEmpty()){
                    assistant.addTestResponse("[INFO] Methods invoked by the Client:");
                    for (String detail : detailsList) {
                        assistant.addTestResponse ( detail);
                        if (detail.equals(""))
                            assistant.addTestResponse("\n");
                    }
                    
                }                               
                assistant.addTestResponse("\n\n");
            }
        }
        catch(Exception e){ 
            assistant.addLog(e.toString());
            assistant.createLogFile();
        }              
        return assistant.terminateTest(userTemporaryDirectoryPath);
    }   
}