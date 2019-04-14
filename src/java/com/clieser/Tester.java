package com.clieser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.activation.DataHandler;

public class Tester {       
    
    public static Reply testClient(String clientEntryPoint, DataHandler selectedFile, String selectedFileName){
        
        // Assuming that the Server is already running...
        ArrayList<String> testResponseList = new ArrayList();        
        ArrayList<TestResult> testResultList = new ArrayList();      
        String userTemporaryDirectoryPath = ""; 
        
        try {        
            //>>>>>>>>>>>>>>>>    CREATION OF TEMPORAY DIRECTORIES AND UPLOAD FILE    <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            
            FileAssistant.createLogAndTemporaryAndExercisesDirectories();  
            userTemporaryDirectoryPath = FileAssistant.getTempDirectoryPath() + "\\" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String userUploadDirectoryPath = userTemporaryDirectoryPath + "\\upload";
            
            FileAssistant.createDirectory(userUploadDirectoryPath);
            FileAssistant.uploadFile(selectedFile, selectedFileName, userUploadDirectoryPath);  
            
            //>>>>>>>>>>>>>>>>    UNZIP OF THE FILE AND ITERATION OVER LIST OF PROJECTS FOUND    <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            
            String zipFilePath = userUploadDirectoryPath + "\\" + selectedFileName;
            String unzipLocation = userTemporaryDirectoryPath + "\\"+selectedFileName; 
            
            //   unzip inside of a new directory which as the same name as the zip file name
            ArrayList<String> listOfProjectsToBeTested = FileAssistant.unzipAndGetTheProjectsToBeTested(zipFilePath, unzipLocation, false);            
                    
            for(int i = 0; i < listOfProjectsToBeTested.size(); i++){
                testResponseList.add ("[INFO] Testing " + (i+1) + " out of " + listOfProjectsToBeTested.size() + " projects\n\n\n");
                testResponseList.add ("\n\n");                
                
                String NameOfTheProjectBeingTested = selectedFileName + ".zip";       
                String[] projectName = listOfProjectsToBeTested.get(i).split(".zip");                
                String pathProjectBeingTested = unzipLocation +"\\" + projectName[0];        
                
                if(listOfProjectsToBeTested.size() > 1){   
                    
                    //>>>>>>>>>>>>>>>>>>  IN CASE OF MULTIPLE PROJECTS UNZIP ONE BY ONE  <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                    
                    zipFilePath = userTemporaryDirectoryPath+ "\\"+selectedFileName +"\\"+listOfProjectsToBeTested.get(i);                  
                           
                    // unzip inside of a new directory which as the same name as the zip file name but outside the parent directory                           
                                         
                    pathProjectBeingTested = userTemporaryDirectoryPath + "\\" + projectName[0];                     
                    FileAssistant.unzipAndGetTheProjectsToBeTested(zipFilePath, pathProjectBeingTested, false);  
                    
                    String[] subDirectories = FileAssistant.getSubdirectories(pathProjectBeingTested);
                    
                     // The array will expect to have just one directory with the name 
                    pathProjectBeingTested += "\\" +subDirectories[0];
                    
                    NameOfTheProjectBeingTested = listOfProjectsToBeTested.get(i); 
                }                
                
                testResponseList.add ( "[INFO] Project selected: "+ NameOfTheProjectBeingTested +"\n");            
                testResponseList.add ("[INFO] System is searching for the Client stubs\n\n");             
                
                // Whenever a new web service reference is added on the client, the wsimport command generates the stubs under the following directories
                if ( !new File(pathProjectBeingTested + "\\build\\generated-sources\\jax-ws").exists() ){
                    testResponseList.add ("[INFO] System did not find the Client stubs\n\n");
                    testResponseList.add ("\n\n");
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client is not correctly connected to the server","false"));
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));                    
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    continue;
                }          
                
                testResponseList.add ("[INFO] System found the Client stubs\n\n");                  
                testResponseList.add ("[INFO] System is searching for the main class of the Client\n\n");                               
                testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client is correctly connected to the server","true"));
                                
                String entryPoint = clientEntryPoint.replace('.', '\\');
                
                if(!Files.exists(Paths.get(pathProjectBeingTested + "\\src\\" + entryPoint + ".java"))){
                    testResponseList.add ("[INFO] System did not find the main class of the Client\n\n");
                    testResponseList.add ("\n\n");                    
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    continue;
                }
                               
                testResponseList.add ("[INFO] System found the main class of the Client\n\n");
                testResponseList.add ("[INFO] System is trying to start the Client\n\n");                                  
                boolean hasClientStarted = ClientAssistant.didRunClientAndSaveOutput(clientEntryPoint, pathProjectBeingTested, userTemporaryDirectoryPath);

                if(!hasClientStarted){
                    testResponseList.add ("[INFO] Client did not start. Please check your code\n\n");
                    testResponseList.add ("\n\n");
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));                    
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    continue;
                }   

                testResponseList.add ("[INFO] Client has started\n\n");
                testResponseList.add ("[INFO] System is verifying whether Client has communicated with the Server\n\n");                
                testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did start","true"));    
                
                if( !ClientAssistant.didClientCommunicatedWithServer(pathProjectBeingTested) ){
                    testResponseList.add ("[INFO] Client did not communicate with the Server. Please check your code\n\n");
                    testResponseList.add ("\n\n");                  
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    continue;
                }
                
                testResponseList.add ("[INFO] Client has communicated with the Server\n\n");
                testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did communicate with the Server","true"));                 
                
                ArrayList<String> serverMethodsList = ServerAssistant.getMethodsAvailableOnServer(pathProjectBeingTested);                
                ArrayList<String> listOfInvokedMethodsNames = SoapEnvelopeAssistant.getListOfInvokedMethodsNames(pathProjectBeingTested, serverMethodsList);                
                testResponseList.add ("[INFO] Client has invoked " + listOfInvokedMethodsNames.size() + " out of " + serverMethodsList.size() + " methods\n\n");
                                
                ArrayList<String> detailsList = SoapEnvelopeAssistant.getListOfInvokedMethodsDetails(pathProjectBeingTested, listOfInvokedMethodsNames);
                if ( !detailsList.isEmpty()){
                    testResponseList.add ("[INFO] Methods invoked by the Client:");
                    for (String details : detailsList) {
                        testResponseList.add  ( details);
                        if (details.equals(""))
                            testResponseList.add ("\n\n");
                    }                    
                }                               
                testResponseList.add ("\n\n");
            }
        }
        catch(Exception e){
            FileAssistant.createLogFile ( e.toString());
        }     
        testResponseList.add("[INFO] Test has finished\n\n");  
        FileAssistant. deleteDirectory(new File(userTemporaryDirectoryPath));      
        return new Reply(testResponseList, testResultList);
    }          
   
    public static Reply testClientAndServer(String clientEntryPoint, DataHandler selectedFile, String selectedFileName){
        
        ArrayList<String> testResponseList = new ArrayList();        
        ArrayList<TestResult> testResultList = new ArrayList();  
        String userTemporaryDirectoryPath = "";
        
        try {     
            //>>>>>>>>>>>>>>>>    CREATION OF TEMPORAY DIRECTORIES AND UPLOAD FILE    <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            
            FileAssistant.createLogAndTemporaryAndExercisesDirectories();  
            userTemporaryDirectoryPath = FileAssistant.getTempDirectoryPath() + "\\" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String userUploadDirectoryPath = userTemporaryDirectoryPath + "\\upload";
            
            FileAssistant.createDirectory(userUploadDirectoryPath);
            FileAssistant.uploadFile(selectedFile, selectedFileName, userUploadDirectoryPath);  
            String zipFilePath = userUploadDirectoryPath + "\\" + selectedFileName;

            //>>>>>>>>>>>>>>>>    UNZIP OF THE FILE AND ITERATION OVER LIST OF PROJECTS FOUND    <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            
            String unzipLocation = userTemporaryDirectoryPath + "\\"+selectedFileName; 
            
            // unzip inside of a new directory which as the same name as the zip file name
            ArrayList<String> listOfProjectsToBeTested = FileAssistant.unzipAndGetTheProjectsToBeTested(zipFilePath, unzipLocation, false);      
                
            for(int i = 0; i < listOfProjectsToBeTested.size(); i++){
                testResponseList.add ("[INFO] Testing " + (i+1) + " out of " + listOfProjectsToBeTested.size() + " projects\n\n\n");
                testResponseList.add ("\n\n");
                                
                String NameOfTheProjectBeingTested = selectedFileName + ".zip";                       
                String serverDirectoryPath = ServerAssistant.getServerProjectPath(unzipLocation); // Assuming that this is a Glassfish web service
                String clientDirectoryPath = ClientAssistant.getClientProjectPath(unzipLocation, clientEntryPoint);                
                
                if(listOfProjectsToBeTested.size() > 1){     
                     
                    //>>>>>>>>>>>>>>>>>>  IN CASE OF MULTIPLE PROJECTS UNZIP ONE BY ONE  <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                 
                    zipFilePath = userTemporaryDirectoryPath+ "\\"+selectedFileName +"\\"+listOfProjectsToBeTested.get(i);                  
                       
                    String[] foundProjectName = listOfProjectsToBeTested.get(i).split(".zip");
                    
                    // unzip inside of a new directory which as the same name as the zip file name but outside the parent directory   
                    unzipLocation = userTemporaryDirectoryPath + "\\" + foundProjectName[0];                   
                    
                    FileAssistant.unzipAndGetTheProjectsToBeTested(zipFilePath, unzipLocation, false);  
                                     
                    serverDirectoryPath = ServerAssistant.getServerProjectPath(unzipLocation); // Assuming that this is a Glassfish web service
                    clientDirectoryPath = ClientAssistant.getClientProjectPath(unzipLocation, clientEntryPoint);
                                        
                    NameOfTheProjectBeingTested = listOfProjectsToBeTested.get(i);                     
                }                                  
                testResponseList.add ( "[INFO] Project selected: "+ NameOfTheProjectBeingTested +"\n");   

                //>>>>>>>>>>>>>>>>>>>>>>>   TESTING SERVER   <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                
                testResponseList.add ("[INFO] System is searching for the Server project\n\n");    
                
                if( serverDirectoryPath.isEmpty() ){
                    testResponseList.add ("[INFO] System did not find the Server project\n\n");
                    testResponseList.add ("\n\n");                    
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Server did not start","false"));                           
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client is not correctly connected to the server","false"));
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));                    
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));
                    continue;
                }
                
                testResponseList.add ("[INFO] System did find the Server project\n\n");                
                testResponseList.add ("[INFO] System is trying to deploy the Server\n\n");  
                
                if ( !ServerAssistant.hasServerBeenDeployed(serverDirectoryPath, userTemporaryDirectoryPath) ){
                    testResponseList.add ("[INFO] System did not manage to deploy the Server. Please check your code.\n\n");
                    testResponseList.add ("\n\n");                    
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Server did not start","false"));
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client is not correctly connected to the server","false"));
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));                    
                    continue;
                }
                
                testResponseList.add ("[INFO] System has deployed Server\n\n");
                testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Server did start","true"));
                
                //>>>>>>>>>>>>>>>>>>>>>>>   TESTING CLIENT   <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                
                testResponseList.add ("[INFO] System is searching for the Client stubs\n\n");
                
                // Whenever a new web service reference is added on the client, the wsimport command generates the stubs 
                // under the following directories
                
                if ( !new File(clientDirectoryPath + "\\build\\generated-sources\\jax-ws").exists() ){
                    testResponseList.add ("[INFO] System did not find the Client stubs\n\n");
                    testResponseList.add ("\n\n");
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client is not correctly connected to the server","false"));
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));                    
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));
                    continue;
                }          
                
                testResponseList.add ("[INFO] System found the Client stubs\n\n");                        
                testResponseList.add ("[INFO] System is searching for the main class of the Client\n\n");                
                testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client is correctly connected to the server","true"));
                
                String entryPoint = clientEntryPoint.replace('.', '\\');
                if(!Files.exists(Paths.get(clientDirectoryPath + "\\build\\classes\\" + entryPoint + ".class"))){
                    testResponseList.add ("[INFO] System did not find the main class of the Client\n\n");
                    testResponseList.add ("\n\n");                    
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));
                    continue;
                }
                               
                testResponseList.add ("[INFO] System found the main class of the Client\n\n");
                testResponseList.add ("[INFO] System is trying to start the Client\n\n");                      
                boolean hasClientStarted = ClientAssistant.didRunClientAndSaveOutput(clientEntryPoint, clientDirectoryPath, userTemporaryDirectoryPath);

                if(!hasClientStarted){
                    testResponseList.add ("[INFO] Client did not start. Please check your code\n\n");
                    testResponseList.add ("\n\n");
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did not start","false"));                    
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));
                    continue;
                }   

                testResponseList.add ("[INFO] Client has started\n\n");
                testResponseList.add ("[INFO] System is verifying whether Client has communicated with the Server\n\n"); 
                testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did start","true"));    
                
                if( !ClientAssistant.didClientCommunicatedWithServer(clientDirectoryPath) ){
                    testResponseList.add ("[INFO] Client did not communicate with the Server. Please check your code\n\n");
                    testResponseList.add ("\n\n");                  
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did not communicate with the Server","false"));
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));
                    continue;
                }
                
                testResponseList.add ("[INFO] Client has communicated with the Server\n\n");
                testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Client did communicate with the Server","true"));                 
                testResponseList.add ("[INFO] System is verifying whether Server has communicated with the Client\n\n"); 
                
                if( !ServerAssistant.didServerCommunicatedWithClient(clientDirectoryPath, serverDirectoryPath) ){
                    testResponseList.add ("[INFO] Server did not communicate with the Client. Please check your code\n\n");
                    testResponseList.add ("\n\n");                  
                    testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Server did not communicate with the Client","false"));
                    continue;
                }
                
                testResponseList.add ("[INFO] Server has communicated with the Client\n\n");
                testResultList.add (new TestResult(NameOfTheProjectBeingTested,"Server did communicate with the Client","true"));                
                                                
                ServerAssistant.undeplopyServer(serverDirectoryPath, userTemporaryDirectoryPath);                
                testResponseList.add ("[INFO] System has undeployed Server\n\n");
                
                ArrayList<String> serverMethodsList = ServerAssistant.getMethodsAvailableOnServer(clientDirectoryPath);   
                ArrayList<String> listOfInvokedMethodsNames = SoapEnvelopeAssistant.getListOfInvokedMethodsNames(clientDirectoryPath, serverMethodsList);                
                testResponseList.add ("[INFO] Client has invoked " + listOfInvokedMethodsNames.size() + " out of " + serverMethodsList.size() + " methods\n\n");                
                               
                
                ArrayList<String> detailsList = SoapEnvelopeAssistant.getListOfInvokedMethodsDetails(clientDirectoryPath, listOfInvokedMethodsNames);
                if ( !detailsList.isEmpty()){
                    testResponseList.add ("[INFO] Methods invoked by the Client:");
                    for (String detail : detailsList) {
                        testResponseList.add  ( detail);
                        if (detail.equals(""))
                            testResponseList.add ("\n");
                    }                    
                }                               
                testResponseList.add ("\n\n");
            }
        }
        catch(Exception e){ 
            FileAssistant.createLogFile ( e.toString());
        }      
        testResponseList.add("[INFO] Test has finished\n\n");  
        FileAssistant.deleteDirectory(new File(userTemporaryDirectoryPath)); 
        return new Reply(testResponseList, testResultList);
    }   
}