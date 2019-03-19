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
                assistant.addResponse("[INFO] Testing " + (i+1) + " out of " + listOfProjectsToBeTested.size() + " projects\n\n\n");
                assistant.addResponse("\n\n");                
                
                String NameOfTheProjectBeingTested = selectedFileName + ".zip";       
                String[] projectName = listOfProjectsToBeTested.get(i).split(".zip");                
                String pathProjectBeingTested = unzipLocation +"\\" + projectName[0];        
                
                if(listOfProjectsToBeTested.size() > 1){   
                    
                    //>>>>>>>>>>>>>>>>>>  IN CASE OF MULTIPLE PROJECTS UNZIP ONE BY ONE  <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                    
                    zipFilePath = userTemporaryDirectoryPath+ "\\"+selectedFileName +"\\"+listOfProjectsToBeTested.get(i);                  
                           
                    //unzip inside of a new directory which as the same name as the zip file name but outside the parent directory
                    unzipLocation = userTemporaryDirectoryPath;//               
                                    
                    pathProjectBeingTested = unzipLocation + "\\" + projectName[0];                     
                    assistant.unzipAndGetTheProjectsToBeTested(zipFilePath, unzipLocation);              
                    NameOfTheProjectBeingTested = listOfProjectsToBeTested.get(i);             
                }                
                
                assistant.addResponse( "[INFO] Project selected: "+ NameOfTheProjectBeingTested +"\n");            
                assistant.addResponse("[INFO] System is trying to find the Client stubs\n\n");
                
                //Whenever a new web service reference is added on the client, the wsimport command generates the stubs under the following directories
                if ( !new File(pathProjectBeingTested + "\\build\\generated-sources\\jax-ws").exists() ){
                    assistant.addResponse("[INFO] System did not find the Client stubs\n\n");
                    assistant.addResponse("\n\n");
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client is not correctly connected to the server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));                    
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    continue;
                }          
                
                assistant.addResponse("[INFO] System found the Client stubs\n\n");                  
                assistant.addResponse("[INFO] System is looking for the main class of the Client\n\n");                               
                assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client is correctly connected to the server","true"));
                
                String entryPoint = clientEntryPoint.replace('.', '\\');
                if(!Files.exists(Paths.get(pathProjectBeingTested + "\\build\\classes\\" + entryPoint + ".class"))){
                    assistant.addResponse("[INFO] System did not find the main class of the Client\n\n");
                    assistant.addResponse("\n\n");                    
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    continue;
                }
                               
                assistant.addResponse("[INFO] System found the main class of the Client\n\n");
                assistant.addResponse("[INFO] System is trying to start the Client\n\n");                                  
                boolean hasClientStarted = assistant.didRunClientAndSaveOutput(clientEntryPoint, pathProjectBeingTested, userTemporaryDirectoryPath);

                if(!hasClientStarted){
                    assistant.addResponse("[INFO] Client did not start. Please check your code\n\n");
                    assistant.addResponse("\n\n");
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));                    
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    continue;
                }   

                assistant.addResponse("[INFO] Client has started\n\n");
                assistant.addResponse("[INFO] System is verifying whether Client has communicated with the Server\n\n");                
                assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did start","true"));    
                
                if( !assistant.didClientCommunicatedWithServer(pathProjectBeingTested) ){
                    assistant.addResponse("[INFO] Client did not communicate with the Server. Please check your code\n\n");
                    assistant.addResponse("\n\n");                  
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    continue;
                }
                
                assistant.addResponse("[INFO] Client has communicated with the Server\n\n");
                assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did communicate with the Server","true")); 
                assistant.addResponse("\n\n");
            }
        }
        catch(Exception e){
            assistant.addLog( e);
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
                       
            for(int i = 0; i < listOfProjectsToBeTested.size(); i++){
                assistant.addResponse("[INFO] Testing " + (i+1) + " out of " + listOfProjectsToBeTested.size() + " projects\n\n\n");
                assistant.addResponse("\n\n");
                                
                String NameOfTheProjectBeingTested = selectedFileName + ".zip";                       
                String serverDirectoryPath = assistant.getServerProjectPath(unzipLocation); // Assuming that this is a Glassfish web service
                String clientDirectoryPath = assistant.getClientProjectPath(unzipLocation, clientEntryPoint);                
                
                 if(listOfProjectsToBeTested.size() > 1){     
                     
                    //>>>>>>>>>>>>>>>>>>  IN CASE OF MULTIPLE PROJECTS UNZIP ONE BY ONE  <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                    
                    zipFilePath = userTemporaryDirectoryPath+ "\\"+selectedFileName +"\\"+listOfProjectsToBeTested.get(i);  
                    
                    //unzip inside of a new directory which as the same name as the zip file name but outside the parent directory
                    unzipLocation = userTemporaryDirectoryPath;               
                                                  
                    assistant.unzipAndGetTheProjectsToBeTested(zipFilePath, unzipLocation);              
                    NameOfTheProjectBeingTested = listOfProjectsToBeTested.get(i);    
                    
                    serverDirectoryPath = assistant.getServerProjectPath(unzipLocation); // Assuming that this is a Glassfish web service
                    clientDirectoryPath = assistant.getClientProjectPath(unzipLocation, clientEntryPoint);
                
                }                                  
                assistant.addResponse( "[INFO] Project selected: "+ NameOfTheProjectBeingTested +"\n");   

                //>>>>>>>>>>>>>>>>>>>>>>>   TESTING SERVER   <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                
                assistant.addResponse("[INFO] System is looking for the Server project\n\n");    
                
                if( serverDirectoryPath.isEmpty() ){
                    assistant.addResponse("[INFO] System did not find the Server project\n\n");
                    assistant.addResponse("\n\n");                    
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did not start","false"));                           
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client is not correctly connected to the server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));                    
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));
                    continue;
                }
                
                assistant.addResponse("[INFO] System did find the Server project\n\n");                
                assistant.addResponse("[INFO] System is trying to deploy the Server\n\n");  
                
                if ( !assistant.hasServerBeenDeployed(serverDirectoryPath, userTemporaryDirectoryPath) ){
                    assistant.addResponse("[INFO] System did not manage to deploy the Server. Please check your code.\n\n");
                    assistant.addResponse("\n\n");                    
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did not start","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client is not correctly connected to the server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));                    
                    continue;
                }
                
                assistant.addResponse("[INFO] System has deployed Server\n\n");
                assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did start","true"));
                
                //>>>>>>>>>>>>>>>>>>>>>>>   TESTING CLIENT   <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                
                assistant.addResponse("[INFO] System is trying to find the Client stubs\n\n");
                
                //Whenever a new web service reference is added on the client, the wsimport command generates the stubs under the following directories
                if ( !new File(clientDirectoryPath + "\\build\\generated-sources\\jax-ws").exists() ){
                    assistant.addResponse("[INFO] System did not find the Client stubs\n\n");
                    assistant.addResponse("\n\n");
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client is not correctly connected to the server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));                    
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));
                    continue;
                }          
                
                assistant.addResponse("[INFO] System found the Client stubs\n\n");                        
                assistant.addResponse("[INFO] System is looking for the main class of the Client\n\n");                
                assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client is correctly connected to the server","true"));
                
                String entryPoint = clientEntryPoint.replace('.', '\\');
                if(!Files.exists(Paths.get(clientDirectoryPath + "\\build\\classes\\" + entryPoint + ".class"))){
                    assistant.addResponse("[INFO] System did not find the main class of the Client\n\n");
                    assistant.addResponse("\n\n");                    
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));
                    continue;
                }
                               
                assistant.addResponse("[INFO] System found the main class of the Client\n\n");
                assistant.addResponse("[INFO] System is trying to start the Client\n\n");                      
                boolean hasClientStarted = assistant.didRunClientAndSaveOutput(clientEntryPoint, clientDirectoryPath, userTemporaryDirectoryPath);

                if(!hasClientStarted){
                    assistant.addResponse("[INFO] Client did not start. Please check your code\n\n");
                    assistant.addResponse("\n\n");
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));                    
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));
                    continue;
                }   

                assistant.addResponse("[INFO] Client has started\n\n");
                assistant.addResponse("[INFO] System is verifying whether Client has communicated with the Server\n\n"); 
                assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did start","true"));    
                
                if( !assistant.didClientCommunicatedWithServer(clientDirectoryPath) ){
                    assistant.addResponse("[INFO] Client did not communicate with the Server. Please check your code\n\n");
                    assistant.addResponse("\n\n");                  
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));
                    continue;
                }
                
                assistant.addResponse("[INFO] Client has communicated with the Server\n\n");
                assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Client did communicate with the Server","true"));                 
                assistant.addResponse("[INFO] System is verifying whether Server has communicated with the Client\n\n"); 
                
                if( !assistant.didServerCommunicatedWithClient(clientDirectoryPath, serverDirectoryPath) ){
                    assistant.addResponse("[INFO] Server did not communicate with the Client. Please check your code\n\n");
                    assistant.addResponse("\n\n");                  
                    assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));
                    continue;
                }
                
                assistant.addResponse("[INFO] Server has communicated with the Client\n\n");
                assistant.addResults(new TestResult(NameOfTheProjectBeingTested,"Server did communicate with the Client","true"));                
                assistant.addResponse("\n\n");                
                
                assistant.undeplopyServer(serverDirectoryPath, userTemporaryDirectoryPath);                
                assistant.addResponse("[INFO] System has undeployed Server\n\n");
            }
        }
        catch(Exception e){ 
            assistant.addLog(e);
            assistant.createLogFile();
        }              
        return assistant.terminateTest(userTemporaryDirectoryPath);
    }   
}