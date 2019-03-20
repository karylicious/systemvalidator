package com.clieser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.activation.DataHandler;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 *
 * @author Carla Augusto
 */

//Singleton pattern restricts the instantiation of a class and ensures that only one instance of the class exists in the java virtual machine. 
public class Assistant {
    private static String currentWorkingDirectory, tempDirectoryPath, logDirectoryPath, log;
    private ArrayList<TestResult> resultList;  
    private static Assistant singleInstance = null;
    private ArrayList<String> response;

    private Assistant(){
        currentWorkingDirectory = System.getProperty("user.dir") + "\\src\\main\\java\\com\\clieser";
        tempDirectoryPath = currentWorkingDirectory + "\\Temp";
        logDirectoryPath = currentWorkingDirectory + "\\Logs";
    }
    
    public static Assistant getInstance(){
        if (singleInstance == null)
            singleInstance = new Assistant();        
        return singleInstance;
    }        
    
    public void addResponse(String text){ response.add(text); }
    
    public void addLog(Object text){ log += text; }                        
        
    public ArrayList<TestResult> getResultList(){ return resultList; }  
        
    public void addResults(TestResult result){ resultList.add( result ); }
        
    public String getTempDirectoryPath(){ return tempDirectoryPath; }
    
    public String getLogDirectoryPath(){ return logDirectoryPath; }
    
           
    public void createDirectory(String directory) throws Exception{
        if (!(Files.exists(Paths.get(directory)))) 
            Files.createDirectories(Paths.get(directory));        
    }       
    
    public void createLogAndTemporaryDirectories() throws Exception{
        createDirectory(tempDirectoryPath);        
        createDirectory(logDirectoryPath);
    }        
    
    public void createLogFile() { 
        try {
            if(!log.isEmpty()) {
                File file = new File(logDirectoryPath + "\\" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".txt");
                FileOutputStream outputStream = new FileOutputStream(file);
                OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream); 

                Writer theWriter = new BufferedWriter(outputWriter);
                theWriter.write((String)log);
                theWriter.close();
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }   
            
    public Reply terminateTest(String userTemporaryDirectoryPath) {   
        response.add("[INFO] Test has finished\n\n");       
        System.gc();
        delete(new File(userTemporaryDirectoryPath));    
        createLogFile();
        return new Reply(response, resultList);
    }
    
    public void uploadFile(DataHandler selectedFile, String fileName, String uploadLocation) throws IOException {               
        InputStream input = selectedFile.getInputStream();
        OutputStream output = new FileOutputStream( new File(uploadLocation + "\\"+ fileName));

        byte[] b = new byte[100000];
        int bytesRead = 0;

        while ((bytesRead = input.read(b)) != -1) {
            output.write(b, 0, bytesRead);
        }         
    }
    
    public void createNewShFile(String intendedFile, ArrayList<String> commands) throws IOException {  
        File file = new File(intendedFile);
        file.createNewFile();
        PrintWriter writer = new PrintWriter(intendedFile, "UTF-8");
        
        for (String currentCommand:  commands){
            writer.println(currentCommand);
        }
        writer.close();
    }        
         
    public boolean hasServerBeenDeployed(String serverDirectoryPath, String userTemporaryDirectoryPath){   
        try{                         
            //ANT BUILD
            ArrayList<String> commandsList = new ArrayList();
            commandsList.add("cd C:\\ant\\bin");
            commandsList.add("ant -f " + serverDirectoryPath);
            
            String antBuildShFile = userTemporaryDirectoryPath + "\\antbuild.bat";
            createNewShFile(antBuildShFile, commandsList);

            Process process1 = Runtime.getRuntime().exec("cmd /c "+antBuildShFile);          
            Thread.currentThread().sleep(7000l);          
            
            //ANT RUN-DEPLOY
            commandsList = new ArrayList();
            commandsList.add("cd C:\\ant\\bin");
            commandsList.add("ant -f " + serverDirectoryPath + " run-deploy");
            
            String antDeployShFile = userTemporaryDirectoryPath + "\\antdeploy.bat";
            createNewShFile(antDeployShFile, commandsList);

            Process process2 = Runtime.getRuntime().exec("cmd /c " + antDeployShFile);  
            Thread.currentThread().sleep(5000l);                        
            
            //GET LIST OF DEPLOYED APPLICATION ON GLASSFISH
            commandsList = new ArrayList();
            commandsList.add("cd C:\\Program Files\\glassfish-4.1.1\\bin");
            commandsList.add("asadmin list-applications --type web");
            
            String deployedListdShFile = userTemporaryDirectoryPath + "\\deployedlist.bat";
            createNewShFile(deployedListdShFile, commandsList);
            Process process3 = Runtime.getRuntime().exec("cmd /c " + deployedListdShFile);
                        
            BufferedReader br = new BufferedReader(new InputStreamReader(process3.getInputStream())); 
            String line;
            ArrayList<String> deployedList = new ArrayList();  
            
            while ((line = br.readLine()) != null) {
                if(line.contains("<webservices, web>")){
                    String[] appName = line.split("<webservices, web>");
                    deployedList.add(appName[0].trim());
                }
            }   
            
            br.close();
            File file = new File(serverDirectoryPath);
            String projectName = file.getName();
            boolean foundApp = false;
            
            for(String app : deployedList){
                if ( app.equals(projectName) ){
                    foundApp = true;
                    break;
                }
            }            
            return foundApp;
        }
        catch(Exception e){            
            log += e.getMessage();
            createLogFile();
        }               
        return false;        
    }         
    
    public void undeplopyServer(String serverDirectoryPath, String userTemporaryDirectoryPath){
        try{
            //ANT RUN-DEPLOY
            ArrayList<String> commandsList = new ArrayList();
            commandsList.add("cd C:\\ant\\bin");
            commandsList.add("ant -f " + serverDirectoryPath + " run-undeploy");
            
            String antUndeployShFile = userTemporaryDirectoryPath + "\\antundeploy.bat";
            createNewShFile(antUndeployShFile, commandsList);

            Process process = Runtime.getRuntime().exec("cmd /c " + antUndeployShFile);           
            Thread.currentThread().sleep(3000l);
        }
        catch(Exception e){            
            log += e.getMessage();
            createLogFile();
        }   
    }
        
    public boolean didRunClientAndSaveOutput(String clientEntryPoint, String pathProjectBeingTested, String userTemporaryDirectoryPath) {   
        try{              
            File classesDirectory = new File(pathProjectBeingTested + "/build/classes");        
            ArrayList<String> commandsList = new ArrayList();
            
            commandsList.add("cd " +  classesDirectory.getPath());
            commandsList.add("java -Dcom.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump=true " + clientEntryPoint); 
            
            final String clientShFile = userTemporaryDirectoryPath + "\\client.bat";
            createNewShFile(clientShFile, commandsList);

            //String[] cmdline = { "/bin/bash", "-c", "bash "+clientShFile+" > "+userTemporaryDirectoryPath + "/client-output.txt"}; 
            //String[] cmdline = {"cmd /c "+clientShFile}; 
            
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
            log += e.getMessage();
            createLogFile();
            return false;
        }               
        return true;        
    }         
       
    public void delete(File file){        
    	if(file.isDirectory()){ 
            //directory is empty, then delete it
            if(file.list().length==0){    			
               file.delete();
            }
            else{
               //list all the directory contents
               String files[] = file.list();
               for (String temp : files) {
                  File fileDelete = new File(file, temp);
                  //recursive delete
                 delete(fileDelete);
               }
               //check the directory again, if empty then delete it
               if(file.list().length==0){
                 file.delete();
               }
            }    		
    	}
        else{
            file.delete();
    	}
    }    
    
    public String[] getSubdirectories(String parentDirectoryPath){
        File file = new File(parentDirectoryPath);
        String[] directories = file.list(new FilenameFilter() {
          @Override
          public boolean accept(File current, String name) {
            return new File(current, name).isDirectory();
          }
        });
        return directories;
    }
    
    public String getClientProjectPath(String unzipLocation, String entryPoint){      
        String[] directories = getSubdirectories(unzipLocation);        
        String theEntryPoint = entryPoint.replace('.', '\\');    
        
        for (int i = 0; i < directories.length; i ++){
            if (new File(unzipLocation+"\\"+directories[i]+ "\\build\\classes\\"+ theEntryPoint+".class").exists()) {
               return unzipLocation+"\\"+directories[i];
            }
        }
        return null;
    }    
       
    public String getServerProjectPath(String unzipLocation){
        File file = new File(unzipLocation);
        String[] directories = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
              return new File(current, name).isDirectory();
            }
        });
        
        for (int i = 0; i < directories.length; i ++){
            //log += unzipLocation+"\\"+directories[i]+ "\\build\\web\n";
            if (new File(unzipLocation+"\\"+directories[i]+ "\\build\\web").exists()) {
               return unzipLocation+"\\"+directories[i];
            }
        }
        return null;
    }    
           
    public ArrayList<String> unzipAndGetTheProjectsToBeTested(final String zipFilePath, String unzipLocation) throws IOException { 
        ArrayList<String> listOfProjectsToBeTested = new ArrayList();
        // Open the zip file
        ZipFile zipFile = new ZipFile(zipFilePath);
        Enumeration<?> enu = zipFile.entries();

        if (!(Files.exists(Paths.get(unzipLocation)))) {
            Files.createDirectories(Paths.get(unzipLocation));
        }            
        
        boolean foundFirstDirectoryInTheZipFile = false;   
        
        while (enu.hasMoreElements()) {
            ZipEntry zipEntry = new ZipEntry((ZipEntry) enu.nextElement());
            String name = zipEntry.getName();                                
            File file = new File(unzipLocation + "\\" + name);
            
            if (name.endsWith("/")) {
                if(!foundFirstDirectoryInTheZipFile){
                    foundFirstDirectoryInTheZipFile = true;    
                    listOfProjectsToBeTested.add(name);
                }
                file.mkdirs();
                continue;
            }
            else if (name.contains(".zip")){
               listOfProjectsToBeTested.add(name);      
            }

            File parent = file.getParentFile();
            if (parent != null){                                  
                parent.mkdirs();
            }

            // Extract the file
            InputStream is = zipFile.getInputStream(zipEntry);
            FileOutputStream  fos = new FileOutputStream(file);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = is.read(bytes)) >= 0) {
                fos.write(bytes, 0, length);
            }                
            fos.close();
            is.close();
        }
        zipFile.close();  
        return listOfProjectsToBeTested;
    }   
    
    public void initilizeGlobalVariables(){    
        log = "";
        response = new ArrayList();        
        resultList = new ArrayList();
    }
        
    public ArrayList<String> getListOfMethodsOnServer(String wsdlFile) {
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
            log += e.getMessage();
            createLogFile();
        }
        return list;
    }
    
    
    
    public ArrayList<String> getMethodsAvailableOnServer(String clientDirectoryPath){
        String[] directories = getSubdirectories(clientDirectoryPath + "\\xml-resources\\web-service-references");
        String serviceName = directories[0];
        String localhostDirPath = clientDirectoryPath + "\\xml-resources\\web-service-references\\"+serviceName +"\\wsdl\\localhost_8080";

        directories = getSubdirectories(localhostDirPath);        
        String wsdlFilePath =  localhostDirPath + "\\" +  directories[0] +"\\" + serviceName + ".wsdl";     
        return getListOfMethodsOnServer(wsdlFilePath);         
    }
            
    public String getServiceName(String pathProjectBeingTested) {        
       String[] directories = getSubdirectories(pathProjectBeingTested + "\\xml-resources\\web-service-references");
       return directories[0];
    }    
    
    public String getServiceNamexx(String wsdlFile) {        
        try{
            Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new FileInputStream(wsdlFile));
            NodeList elements = d.getElementsByTagName("service");
            return elements.item(0).getAttributes().getNamedItem("name").getNodeValue();          
        }
        catch(Exception e){
            log += e.getMessage();
            createLogFile();
        }
        return null;
    }    
    
    public int getNumberOfMethodsInvokedByTheClient(String clientDirectoryPath, ArrayList<String> serverMethodsList){
        int total = 0;
        try{            
            File file = new File(clientDirectoryPath + "\\traced-soap-traffic.txt");   
            BufferedReader br = new BufferedReader(new FileReader(file)); 
            String line;              
            
            while (( line = br.readLine()) != null) {              
                if ( line.contains("SOAPAction:")){
                    for (String method : serverMethodsList){
                        if (line.contains(method)){
                            total++;               
                            break;
                        }
                    }
                }
            }  
            br.close();
        }
        catch(Exception e){
            log += e.getMessage();
            createLogFile();
        }
        return total;
    }
    
    
    public boolean didServerCommunicatedWithClient(String clientDirectoryPath, String serverDirectoryPath){
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
            log += e.getMessage();
            createLogFile();
        }
        return false;
    }
    
    public boolean didClientCommunicatedWithServer(String pathProjectBeingTested){
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
            log += e.getMessage();
            createLogFile();
        }
        return false;
    }
}