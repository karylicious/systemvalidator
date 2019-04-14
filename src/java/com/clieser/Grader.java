package com.clieser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.activation.DataHandler;

public class Grader {
          
    public static Grading gradeClientAndServer (String clientEntryPoint, DataHandler selectedFile, String selectedFileName, String exerciseQuestionXML){
      
        ArrayList<String> gradingResponseList = new ArrayList();    
        ArrayList<GradingResult> gradingResultList = new ArrayList();
        
        String userTemporaryDirectoryPath = "";
        double finalGrade = 0;
        
        try{             
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
                gradingResponseList.add ("[INFO] Grading " + (i+1) + " out of " + listOfProjectsToBeTested.size() + " projects\n\n\n");
                gradingResponseList.add ("\n\n");
                                
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
                gradingResponseList.add ( "[INFO] Project selected: "+ NameOfTheProjectBeingTested +"\n");   

                //>>>>>>>>>>>>>>>>>>>>>>>   VERIFYING SERVER   <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                
                gradingResponseList.add ("[INFO] System is searching for the Server project\n\n");    
                
                if( serverDirectoryPath.isEmpty() ){
                    gradingResponseList.add ("[INFO] System did not find the Server project\n\n");
                    gradingResponseList.add ("\n\n");                    
                    continue;
                }
                
                gradingResponseList.add ("[INFO] System did find the Server project\n\n");                
                gradingResponseList.add ("[INFO] System is trying to deploy the Server\n\n");  
                
                if ( !ServerAssistant.hasServerBeenDeployed(serverDirectoryPath, userTemporaryDirectoryPath) ){
                    gradingResponseList.add ("[INFO] System did not manage to deploy the Server. Please check your code.\n\n");
                    gradingResponseList.add ("\n\n");                                       
                    continue;
                }
                
                gradingResponseList.add ("[INFO] System has deployed Server\n\n");
                
                //>>>>>>>>>>>>>>>>>>>>>>>   VERIFYING CLIENT   <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                
                gradingResponseList.add ("[INFO] System is searching for the Client stubs\n\n");                
              
                // Whenever a new web service reference is added on the client, the wsimport command generates 
                // the stubs under the following directories
                
                if ( !new File(clientDirectoryPath + "\\build\\generated-sources\\jax-ws").exists() ){
                    gradingResponseList.add ("[INFO] System did not find the Client stubs\n\n");
                    gradingResponseList.add ("\n\n");
                    continue;
                }          
                
                gradingResponseList.add ("[INFO] System found the Client stubs\n\n");                        
                gradingResponseList.add ("[INFO] System is searching for the main class of the Client\n\n");                
                
                String entryPoint = clientEntryPoint.replace('.', '\\');
                if(!Files.exists(Paths.get(clientDirectoryPath + "\\build\\classes\\" + entryPoint + ".class"))){
                    gradingResponseList.add ("[INFO] System did not find the main class of the Client\n\n");
                    gradingResponseList.add ("\n\n");                    
                    continue;
                }
                               
                gradingResponseList.add ("[INFO] System found the main class of the Client\n\n");
                gradingResponseList.add ("[INFO] System is trying to start the Client\n\n");                      
                boolean hasClientStarted = ClientAssistant.didRunClientAndSaveOutput(clientEntryPoint, clientDirectoryPath, userTemporaryDirectoryPath);

                if(!hasClientStarted){
                    gradingResponseList.add ("[INFO] Client did not start. Please check your code\n\n");
                    gradingResponseList.add ("\n\n");
                    continue;
                }   

                gradingResponseList.add ("[INFO] Client has started\n\n");
                gradingResponseList.add ("[INFO] System is verifying whether Client has communicated with the Server\n\n"); 
                
                if( !ClientAssistant.didClientCommunicatedWithServer(clientDirectoryPath) ){
                    gradingResponseList.add ("[INFO] Client did not communicate with the Server. Please check your code\n\n");
                    gradingResponseList.add ("\n\n");                  
                    continue;
                }
                
                gradingResponseList.add ("[INFO] Client has communicated with the Server\n\n");            
                gradingResponseList.add ("[INFO] System is verifying whether Server has communicated with the Client\n\n"); 
                
                if( !ServerAssistant.didServerCommunicatedWithClient(clientDirectoryPath, serverDirectoryPath) ){
                    gradingResponseList.add ("[INFO] Server did not communicate with the Client. Please check your code\n\n");
                    gradingResponseList.add ("\n\n");                  
                    continue;
                }
                
                gradingResponseList.add ("[INFO] Server has communicated with the Client\n\n");   
                
                ServerAssistant.undeplopyServer(serverDirectoryPath, userTemporaryDirectoryPath);       
                
                gradingResponseList.add ("[INFO] System has undeployed Server\n\n");
                
                ArrayList<String> serverMethodsList = ServerAssistant.getMethodsAvailableOnServer(clientDirectoryPath);   
                ArrayList<String> listOfInvokedMethodsNames = SoapEnvelopeAssistant.getListOfInvokedMethodsNames(clientDirectoryPath, serverMethodsList);                
                gradingResponseList.add ("[INFO] Client has invoked " + listOfInvokedMethodsNames.size() + " out of " + serverMethodsList.size() + " methods\n\n");                
                              
                ArrayList<String> detailsList = SoapEnvelopeAssistant.getListOfInvokedMethodsDetails(clientDirectoryPath, listOfInvokedMethodsNames);
                if ( !detailsList.isEmpty()){
                    gradingResponseList.add ("[INFO] Methods invoked by the Client:");
                    for (String detail : detailsList) {
                        gradingResponseList.add  ( detail);
                        if (detail.equals(""))
                            gradingResponseList.add ("\n");
                    }                    
                }                               
                gradingResponseList.add ("\n\n");                
                
                ArrayList<ExerciseQuestion> exerciseQuestionsList =  SoapEnvelopeAssistant.getExerciseQuestions(exerciseQuestionXML);
                ArrayList<UserAnswer> getUserAnswerList = SoapEnvelopeAssistant.getUserAnswerList(clientDirectoryPath, exerciseQuestionsList);
                
                boolean hasInvokedTheExpectedMethod;
                double grade = 0;
                
                for (ExerciseQuestion question : exerciseQuestionsList){
                    hasInvokedTheExpectedMethod = false;
                    
                    for (UserAnswer answer : getUserAnswerList){
                        
                        if (answer.getInvokedMethod().equals(question.getExpectedInvokedMethod())){
                            
                            hasInvokedTheExpectedMethod = true;
                            
                            if(answer.getActualOutput().equals(question.getExpectedOutput())){
                                finalGrade += question.getPoints();
                                grade = question.getPoints();
                            }
                            else{
                                finalGrade += (question.getPoints() / 2);
                                grade = (question.getPoints() / 2);
                            }                            
                            gradingResultList.add (new GradingResult(NameOfTheProjectBeingTested,"Did invoke " +question.getExpectedInvokedMethod()+ "() method","true", answer.getActualOutput(), Double.toString(grade)));
                        }
                    }
                    if(!hasInvokedTheExpectedMethod)
                        gradingResultList.add (new GradingResult(NameOfTheProjectBeingTested,"Did not invoke " +question.getExpectedInvokedMethod()+ "() method","false", "","0"));                  
                }                
            }
        }
        catch(Exception e){
            FileAssistant.createLogFile ( e.toString());
            FileAssistant.deleteDirectory(new File(userTemporaryDirectoryPath));
        }  
        gradingResponseList.add("[INFO] Grading has finished\n\n");  
        FileAssistant.deleteDirectory(new File(userTemporaryDirectoryPath)); 
        return new Grading(gradingResponseList, gradingResultList, Double.toString(finalGrade));
    }
    
    public static Grading gradeClient (String clientEntryPoint, DataHandler selectedFile, String selectedFileName, String exerciseQuestionXML){
        
        ArrayList<String> gradingResponseList = new ArrayList();    
        ArrayList<GradingResult> gradingResultList = new ArrayList();
        String userTemporaryDirectoryPath = "";
        double finalGrade = 0;
        
        try{             
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
                gradingResponseList.add ("[INFO] Grading " + (i+1) + " out of " + listOfProjectsToBeTested.size() + " projects\n\n\n");
                gradingResponseList.add ("\n\n");
                                
                String NameOfTheProjectBeingTested = selectedFileName + ".zip";                       
                
                String clientDirectoryPath = ClientAssistant.getClientProjectPath(unzipLocation, clientEntryPoint);                
                
                 if(listOfProjectsToBeTested.size() > 1){     
                     
                    //>>>>>>>>>>>>>>>>>>  IN CASE OF MULTIPLE PROJECTS UNZIP ONE BY ONE  <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<    
                    
                    zipFilePath = userTemporaryDirectoryPath+ "\\"+selectedFileName +"\\"+listOfProjectsToBeTested.get(i);                  
                       
                    String[] foundProjectName = listOfProjectsToBeTested.get(i).split(".zip");
                    
                    // unzip inside of a new directory which as the same name as the zip file name but outside the parent directory    
                    
                    unzipLocation = userTemporaryDirectoryPath + "\\" + foundProjectName[0];                     
                    FileAssistant.unzipAndGetTheProjectsToBeTested(zipFilePath, unzipLocation, false);                      
                    
                    clientDirectoryPath = ClientAssistant.getClientProjectPath(unzipLocation, clientEntryPoint);                                        
                    
                    NameOfTheProjectBeingTested = listOfProjectsToBeTested.get(i);                     
                }                                  
                gradingResponseList.add ( "[INFO] Project selected: "+ NameOfTheProjectBeingTested +"\n");  

                
                //>>>>>>>>>>>>>>>>>>>>>>>   VERIFYING CLIENT   <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                
                gradingResponseList.add ("[INFO] System is searching for the Client stubs\n\n");
                
              
                // Whenever a new web service reference is added to the client, the wsimport command generates 
                // the stubs under the following directories
                
                if ( !new File(clientDirectoryPath + "\\build\\generated-sources\\jax-ws").exists() ){
                    gradingResponseList.add ("[INFO] System did not find the Client stubs\n\n");
                    gradingResponseList.add ("\n\n");
                    continue;
                }          
                
                gradingResponseList.add ("[INFO] System found the Client stubs\n\n");                        
                gradingResponseList.add ("[INFO] System is searching for the main class of the Client\n\n");                
                
                String entryPoint = clientEntryPoint.replace('.', '\\');
                if(!Files.exists(Paths.get(clientDirectoryPath + "\\build\\classes\\" + entryPoint + ".class"))){
                    gradingResponseList.add ("[INFO] System did not find the main class of the Client\n\n");
                    gradingResponseList.add ("\n\n");                    
                    continue;
                }
                               
                gradingResponseList.add ("[INFO] System found the main class of the Client\n\n");
                gradingResponseList.add ("[INFO] System is trying to start the Client\n\n");                      
                boolean hasClientStarted = ClientAssistant.didRunClientAndSaveOutput(clientEntryPoint, clientDirectoryPath, userTemporaryDirectoryPath);

                if(!hasClientStarted){
                    gradingResponseList.add ("[INFO] Client did not start. Please check your code\n\n");
                    gradingResponseList.add ("\n\n");
                    continue;
                }   

                gradingResponseList.add ("[INFO] Client has started\n\n");
                gradingResponseList.add ("[INFO] System is verifying whether Client has communicated with the Server\n\n"); 
                
                if( !ClientAssistant.didClientCommunicatedWithServer(clientDirectoryPath) ){
                    gradingResponseList.add ("[INFO] Client did not communicate with the Server. Please check your code\n\n");
                    gradingResponseList.add ("\n\n");                  
                    continue;
                }
                
                gradingResponseList.add ("[INFO] Client has communicated with the Server\n\n");       
                
                ArrayList<String> serverMethodsList = ServerAssistant.getMethodsAvailableOnServer(clientDirectoryPath);   
                ArrayList<String> listOfInvokedMethodsNames = SoapEnvelopeAssistant.getListOfInvokedMethodsNames(clientDirectoryPath, serverMethodsList);                
                gradingResponseList.add ("[INFO] Client has invoked " + listOfInvokedMethodsNames.size() + " out of " + serverMethodsList.size() + " methods\n\n");                
                                                
                ArrayList<String> detailsList = SoapEnvelopeAssistant.getListOfInvokedMethodsDetails(clientDirectoryPath, listOfInvokedMethodsNames);
                if ( !detailsList.isEmpty()){
                    gradingResponseList.add ("[INFO] Methods invoked by the Client:");
                    for (String detail : detailsList) {
                        gradingResponseList.add  ( detail);
                        if (detail.equals(""))
                            gradingResponseList.add ("\n");
                    }                    
                }                               
                gradingResponseList.add ("\n\n");
                                
                ArrayList<ExerciseQuestion> exerciseQuestionsList =  SoapEnvelopeAssistant.getExerciseQuestions(exerciseQuestionXML);
                ArrayList<UserAnswer> getUserAnswerList = SoapEnvelopeAssistant.getUserAnswerList(clientDirectoryPath, exerciseQuestionsList);
                
                boolean hasInvokedTheExpectedMethod;
                double grade = 0;
                
                for (ExerciseQuestion question : exerciseQuestionsList){
                    hasInvokedTheExpectedMethod = false;
                    
                    for (UserAnswer answer : getUserAnswerList){
                        
                        if (answer.getInvokedMethod().equals(question.getExpectedInvokedMethod())){
                            
                            hasInvokedTheExpectedMethod = true;
                            
                            if(answer.getActualOutput().equals(question.getExpectedOutput())){
                                finalGrade += question.getPoints();
                                grade = question.getPoints();
                            }
                            else{
                                finalGrade += (question.getPoints() / 2);
                                grade = (question.getPoints() / 2);
                            }                            
                             gradingResultList.add (new GradingResult(NameOfTheProjectBeingTested,"Did invoke " +question.getExpectedInvokedMethod()+ "() method","true", answer.getActualOutput(), Double.toString(grade)));
                        }
                    }
                    if(!hasInvokedTheExpectedMethod)
                        gradingResultList.add (new GradingResult(NameOfTheProjectBeingTested,"Did not invoke " +question.getExpectedInvokedMethod()+ "() method","false", "","0"));                  
                }                
            }
        }
        catch(Exception e){
            FileAssistant.createLogFile ( e.toString());
        }  
        gradingResponseList.add("[INFO] Grading has finished\n\n");  
        FileAssistant.deleteDirectory(new File(userTemporaryDirectoryPath));  
        return new Grading(gradingResponseList, gradingResultList, Double.toString(finalGrade));    
    }
}