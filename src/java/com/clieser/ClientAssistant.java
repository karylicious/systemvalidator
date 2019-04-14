package com.clieser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ClientAssistant {
       
    public static boolean didRunClientAndSaveOutput(String clientEntryPoint, String pathProjectBeingTested, String userTemporaryDirectoryPath) {   
        try{              
            File classesDirectory = new File(pathProjectBeingTested + "\\build\\classes");        
            ArrayList<String> commandsList = new ArrayList();
            
            commandsList.add("cd " +  classesDirectory.getPath());
            
            // This command will trace the soap message
            commandsList.add("java -Dcom.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump=true " + clientEntryPoint); 
                        
            final String clientShFile = userTemporaryDirectoryPath + "\\client-"+new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+".bat";
            FileAssistant.createNewShFile(clientShFile, commandsList);
            
            File file = new File(pathProjectBeingTested + "\\traced-soap-traffic.txt");
            FileOutputStream outputStream = new FileOutputStream(file);
            
            OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream);    
            Writer theWriter = new BufferedWriter(outputWriter);            
            
            Process process = Runtime.getRuntime().exec("cmd /c "+clientShFile);
            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream())); 
            
            boolean foundSoapMessage = false;
            while ((line = br.readLine()) != null) {
                if(line.contains("---["))
                    foundSoapMessage = true;
                
                if (foundSoapMessage){
                    theWriter.write(line);
                    theWriter.write(System.getProperty( "line.separator" ));
                }
            }                        
            theWriter.close(); 
            br.close();
        }
        catch(Exception e){            
            FileAssistant.createLogFile ( e.toString());
            return false;
        }               
        return true;        
    }      
    
    public static String getClientProjectPath(String unzipLocation, String entryPoint){      
        String[] directories = FileAssistant.getSubdirectories(unzipLocation);        
        String theEntryPoint = entryPoint.replace('.', '\\');    
        
        for (int i = 0; i < directories.length; i ++){
            if (new File(unzipLocation+"\\"+directories[i]+ "\\build\\classes\\"+ theEntryPoint+".class").exists()) 
               return unzipLocation+"\\"+directories[i];            
        }
        return null;
    }    
       
     public static boolean didClientCommunicatedWithServer(String pathProjectBeingTested){
        try{
            File file = new File(pathProjectBeingTested + "\\traced-soap-traffic.txt");   
            BufferedReader br = new BufferedReader(new FileReader(file)); 

            String firstLine;            
            while (( firstLine = br.readLine()) != null) { 
                br.close();
                return firstLine.contains("---[HTTP request");     
            }  
        }
        catch(Exception e){
            FileAssistant.createLogFile ( e.toString());
        }
        return false;
    }   
}