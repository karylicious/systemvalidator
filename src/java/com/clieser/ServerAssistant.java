package com.clieser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class ServerAssistant {
    
    private static final String ANT_DIRECTORY_PATH = "C:\\ant\\bin";
    private static final String GLASSFISH_BIN_DIRECTORY_PATH = "\"C:\\Program Files\\glassfish-4.1.1\\bin\"";
    private static final String NETBEANS_COPY_LIBS_TASK_MODULE_PATH = "\"C:\\Program Files\\NetBeans 8.2\\java\\ant\\extra\\org-netbeans-modules-java-j2seproject-copylibstask\"";
             
    public static boolean hasServerBeenDeployed(String serverDirectoryPath, String userTemporaryDirectoryPath){   
        try{         
            File file = new File(serverDirectoryPath);
            String projectName = file.getName();
                        
            //ANT BUILD COMMAND
            ArrayList<String> commandsList = new ArrayList();
            commandsList.add("cd " + ANT_DIRECTORY_PATH);
            commandsList.add("ant -Dlibs.CopyLibs.classpath=" + NETBEANS_COPY_LIBS_TASK_MODULE_PATH + " -f \"" + serverDirectoryPath+"\"");
            
            String antBuildBatchFile = userTemporaryDirectoryPath + "\\antbuild-"+new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+".bat";
            FileAssistant.createNewBatchFile(antBuildBatchFile, commandsList);

            Process process1 = Runtime.getRuntime().exec(antBuildBatchFile);          
            Thread.currentThread().sleep(7000l);          
            
            //GLASSFISH DEPLOY COMMAND
            commandsList = new ArrayList();
            
            commandsList.add("cd "+ GLASSFISH_BIN_DIRECTORY_PATH);
            
            // There are some cases where the name of the serverDirectoryPath will not include backslah
            String lastSlash = serverDirectoryPath.substring(serverDirectoryPath.length() - 1);
            
            if (!lastSlash.equals("\\"))
                lastSlash ="\\";
            
            commandsList.add(".\\asadmin deploy \""+serverDirectoryPath + lastSlash + "dist\\"+projectName+".war\"");
            
            String glassFishDeploymentBatchFile = userTemporaryDirectoryPath + "\\glassfishdeploy-"+new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+".bat";
            FileAssistant.createNewBatchFile(glassFishDeploymentBatchFile, commandsList);

            Process process2 = Runtime.getRuntime().exec(glassFishDeploymentBatchFile);  
            Thread.currentThread().sleep(3000l);                        
            
            //GET LIST OF DEPLOYED APPLICATION ON GLASSFISH
            commandsList = new ArrayList();
            commandsList.add("cd "+ GLASSFISH_BIN_DIRECTORY_PATH);
            commandsList.add(".\\asadmin list-applications --type web");
            
            String deployedListdShFile = userTemporaryDirectoryPath + "\\deployedapplist-"+new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+".bat";
            FileAssistant.createNewBatchFile(deployedListdShFile, commandsList);
            Process process3 = Runtime.getRuntime().exec(deployedListdShFile);
            Thread.currentThread().sleep(4000l);
            
            BufferedReader br = new BufferedReader(new InputStreamReader(process3.getInputStream())); 
            String line;
            ArrayList<String> deployedList = new ArrayList();  
            
            while ((line = br.readLine()) != null) {
                if(line.contains("<webservices, web>")){
                    String[] appName = line.split("<webservices, web>");
                    deployedList.add(appName[0].trim());
                    FileAssistant.createLogFile( appName[0].trim());
                }
            }   
            
            br.close();
            
            boolean foundApp = false;
            FileAssistant.createLogFile( Integer.toString(deployedList.size()));
            for(String app : deployedList){              
                if ( app.equals(projectName) ){
                    foundApp = true;
                    break;
                }
            }            
            return foundApp;
        }
        catch(Exception e){            
            FileAssistant.createLogFile ( e.toString());
        }               
        return false;        
    }         
        
    public static void undeplopyServer(String serverDirectoryPath, String userTemporaryDirectoryPath){
        try{            
            File file = new File(serverDirectoryPath);
            String projectName = file.getName();
            
            //GLASSSFISH UNDEPLOY COMMAND
            ArrayList<String> commandsList = new ArrayList();
            commandsList.add("cd " + GLASSFISH_BIN_DIRECTORY_PATH);
            
              // There are some cases where the name of the serverDirectoryPath will not include backslah
            String lastSlash = serverDirectoryPath.substring(serverDirectoryPath.length() - 1);
            
            if (!lastSlash.equals("\\"))
                lastSlash ="\\";
            
            commandsList.add(".\\asadmin undeploy \""+serverDirectoryPath + lastSlash + "dist\\"+projectName+"\"");
                        
            String glassFishUndeployBatchFile = userTemporaryDirectoryPath + "\\glassfishundeploy-"+new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+".bat";
            FileAssistant.createNewBatchFile(glassFishUndeployBatchFile, commandsList);

            Process process = Runtime.getRuntime().exec(glassFishUndeployBatchFile);           
            Thread.currentThread().sleep(3000l);
        }
        catch(Exception e){            
            FileAssistant.createLogFile ( e.toString());
        }   
    }
    
    public static String getServerProjectPath(String unzipLocation){
        File file = new File(unzipLocation);
        
        String[] directories = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
              return new File(current, name).isDirectory();
            }
        });
        
        for (int i = 0; i < directories.length; i ++){
            if (new File(unzipLocation+"\\"+directories[i]+ "\\build\\web").exists()) {
               return unzipLocation+"\\"+directories[i];
            }
        }
        return null;
    } 
            
    public static ArrayList<String> getListOfMethodsOnServer(String wsdlFile) {
        ArrayList<String> list = new ArrayList();
        try{
            Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new FileInputStream(wsdlFile));
            NodeList elements = d.getElementsByTagName("operation");

            String operationNameRetrieved;
            boolean foundDuplicate = false;
            
            for (int i = 0; i < elements.getLength(); i++) {
                operationNameRetrieved  = elements.item(i).getAttributes().getNamedItem("name").getNodeValue();
                if( !list.isEmpty() ){
                    for(String method : list){
                        if (method.equals(operationNameRetrieved)){
                            foundDuplicate = true;
                            break;
                        }
                    }
                }
                if( !foundDuplicate )                
                    list.add(elements.item(i).getAttributes().getNamedItem("name").getNodeValue());
                else
                    return list;                
            }        
        }
        catch(Exception e){
            FileAssistant.createLogFile ( e.toString());
        }
        return list;
    }    
    
    public static ArrayList<String> getMethodsAvailableOnServer(String clientDirectoryPath){
        String[] directories = FileAssistant.getSubdirectories(clientDirectoryPath + "\\xml-resources\\web-service-references");
        String serviceName = directories[0];
        String localhostDirPath = clientDirectoryPath + "\\xml-resources\\web-service-references\\"+serviceName +"\\wsdl\\localhost_8080";

        directories = FileAssistant.getSubdirectories(localhostDirPath);        
        String wsdlFilePath =  localhostDirPath + "\\" +  directories[0] +"\\" + serviceName + ".wsdl";     
        return getListOfMethodsOnServer(wsdlFilePath);         
    }
            
    public String getServiceName(String pathProjectBeingTested) {        
       String[] directories = FileAssistant.getSubdirectories(pathProjectBeingTested + "\\xml-resources\\web-service-references");
       return directories[0];
    }   
            
    public static boolean didServerCommunicatedWithClient(String clientDirectoryPath, String serverDirectoryPath){
        try{
            File file = new File(serverDirectoryPath);
            String projectName = file.getName();
            
            file = new File(clientDirectoryPath + "\\traced-soap-traffic.txt");   
            BufferedReader br = new BufferedReader(new FileReader(file)); 

            String line;            
            while (( line = br.readLine()) != null) {              
                if ( line.contains("---[HTTP response - http://localhost:8080/" + projectName)){
                    br.close();
                    return true;
                }
            }  
        }
        catch(Exception e){
            FileAssistant.createLogFile ( e.toString());
        }
        return false;
    }    
}