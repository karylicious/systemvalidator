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
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.activation.DataHandler;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Carla Augusto
 */

//Singleton pattern restricts the instantiation of a class and ensures that only one instance of the class exists in the java virtual machine. 
public class Assistant {
    private static String currentWorkingDirectory, tempDirectoryPath, logDirectoryPath, log, antDirectoryPath;
    private ArrayList<TestResult> resultList;  
    private static Assistant singleInstance = null;
    private ArrayList<String> response, parametersList, responseValueList;
    private boolean hasResponseMultipleValues = false;

    private Assistant(){
        currentWorkingDirectory = System.getProperty("user.dir") + "\\src\\main\\java\\com\\clieser";
        tempDirectoryPath = currentWorkingDirectory + "\\Temp";
        logDirectoryPath = currentWorkingDirectory + "\\Logs";
        antDirectoryPath = "C:\\ant\\bin";
        hasResponseMultipleValues = false;
        parametersList = new ArrayList();
        responseValueList = new ArrayList();
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
        deleteDirectory(new File(userTemporaryDirectoryPath));    
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
            commandsList.add("cd " + antDirectoryPath);
            commandsList.add("ant -f " + serverDirectoryPath);
            
            String antBuildShFile = userTemporaryDirectoryPath + "\\antbuild.bat";
            createNewShFile(antBuildShFile, commandsList);

            Process process1 = Runtime.getRuntime().exec("cmd /c "+antBuildShFile);          
            Thread.currentThread().sleep(7000l);          
            
            //ANT RUN-DEPLOY
            commandsList = new ArrayList();
            commandsList.add("cd " + antDirectoryPath);
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
    
    public String getClientEntryPoint(String clientDirectoryPath){
        //  I DON'T WANT TO DO THIS!!!!!!!!! IT WILL BE SO MUCH PAINFULL!!!!
        String[] directories = getSubdirectories(clientDirectoryPath+ "/build/classes");        
  
        for (String directory : directories){            
            if (directory.equals("META-INF"))                
                continue;          
            if (new File(clientDirectoryPath +"/build/classes/" +  directory + "/ObjectFactory.class").exists())
                continue; // This means this directory contains the client stubs
            
            // AT THIS POINT I'M FINALLY AT THE CLIENT DIRECTORY
            // NOW I HAVE TO SEARCH FOR .CLASS
            getFile();
            
        }
        
        return "";
    }
    
    public String getFile(){
        
        
        return "";
    }
    
    
    public void undeplopyServer(String serverDirectoryPath, String userTemporaryDirectoryPath){
        try{
            //ANT RUN-DEPLOY
            ArrayList<String> commandsList = new ArrayList();
            commandsList.add("cd " + antDirectoryPath);
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
            File classesDirectory = new File(pathProjectBeingTested + "\\build\\classes");        
            ArrayList<String> commandsList = new ArrayList();
            
            commandsList.add("cd " +  classesDirectory.getPath());
            commandsList.add("java -Dcom.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump=true " + clientEntryPoint); 
            
            final String clientShFile = userTemporaryDirectoryPath + "\\client.bat";
            createNewShFile(clientShFile, commandsList);
            
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
    public void deleteDirectory(File directory){
        System.gc();
        delete(directory);
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
    
    public String[] getFilesInCurrentDirectory(String parentDirectoryPath){
        File file = new File(parentDirectoryPath);
        return file.list(new FilenameFilter() {
          @Override
          public boolean accept(File current, String name) {
            return new File(current, name).isFile();
          }
        });
        
    }
    
    public String[] getSubdirectories(String parentDirectoryPath){
        File file = new File(parentDirectoryPath);
        return file.list(new FilenameFilter() {
          @Override
          public boolean accept(File current, String name) {
            return new File(current, name).isDirectory();
          }
        });
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
                    if (file.exists()){ // This means that there is already a web service with the same name as the one on the zip file
                        zipFile.close(); 
                        return listOfProjectsToBeTested;
                    }
                    
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
    
    private static boolean doesContainExactWord(String source, String wordTofind){
         String pattern = ".*"+wordTofind+".*";
         Pattern p=Pattern.compile(pattern);
         Matcher m=p.matcher(source);
         return m.matches();
    }
    
    public ArrayList<String> getListOfInvokedMethodsNames(String clientDirectoryPath, ArrayList<String> serverMethodsList){      
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
            log += e.getMessage();
            createLogFile();
        }
        return invokedMethodsNameList;
    }    
    
    private NodeList getSoapBodyFirstChildNodes(String soapEnvelope) {
        NodeList childNodes = null;
        try {
            //https://stackoverflow.com/questions/32646444/generate-soap-message-from-java-string
            MessageFactory msgFactory = MessageFactory.newInstance();
            SOAPMessage request = msgFactory.createMessage();
            SOAPPart msgPart = request.getSOAPPart();
            StreamSource content = new StreamSource(new StringReader(soapEnvelope));
            msgPart.setContent(content);    
            
            SOAPEnvelope envelopee = msgPart.getEnvelope();            
            
            //Node Body will always have just one child
            childNodes = envelopee.getBody().getFirstChild().getChildNodes();            
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return childNodes;
    }
    
    
    private void populateTheParameterListVariableWithTheNodesValue(NodeList nodeList) {
        for (int count = 0; count < nodeList.getLength(); count++) {
            Node tempNode = nodeList.item(count);

            // make sure it's element node.
            if (tempNode.getNodeType() == Node.ELEMENT_NODE) {    

                //GET THE VALUES ONLY IF THE CURRENT NODE IS A PARENT
                // IN CASE IT IS NOT A PARENT, THE NAME OF THE FIRST CHILD WILL ALWAYS RETURN "#text" 
                // THE "#text" VALUE COMES FROM THE XML SPECIFICATION)
                if ( !tempNode.getFirstChild().getNodeName().equals("#text") ){
                    //the user-defined type 
                    parametersList.add("Parameter Name = " + tempNode.getNodeName() + "  ( This is a user-defined type which contains the following )");
                    parametersList.add("[START]");
                    parametersList.add("\n");
                } 

                // loop again if has child nodes
                if (tempNode.hasChildNodes())
                    populateTheParameterListVariableWithTheNodesValue(tempNode.getChildNodes());

                //GET THE VALUES ONLY IF THE CURRENT NODE IS NOT A PARENT
                // IN CASE IT IS NOT A PARENT, THE NAME OF THE FIRST CHILD WILL ALWAYS RETURN "#text" 
                // THE "#text" VALUE COMES FROM THE XML SPECIFICATION)
                if ( tempNode.getFirstChild().getNodeName().equals("#text") ){
                    parametersList.add("Parameter Name = " + tempNode.getNodeName());
                    parametersList.add("Parameter Value = " + tempNode.getFirstChild().getTextContent());
                    parametersList.add("\n");
                }      
                else{
                    //the user-defined type 
                    parametersList.add("[END]");
                    parametersList.add("\n");
                }                 
            }
        }   
    
    }
    
    
    private void populateTheResponseValueListVariableWithTheNodesValue(NodeList nodeList) {        
        for (int count = 0; count < nodeList.getLength(); count++) {

            Node tempNode = nodeList.item(count);

            // make sure it's element node.
            if (tempNode.getNodeType() == Node.ELEMENT_NODE) {  
                // loop again if has child nodes
                if (tempNode.hasChildNodes())
                    populateTheResponseValueListVariableWithTheNodesValue(tempNode.getChildNodes());

                //GET THE VALUES ONLY IF THE CURRENT NODE IS NOT A PARENT
                // IN CASE IT IS NOT A PARENT, THE NAME OF THE FIRST CHILD WILL ALWAYS RETURN "#text" 
                // THE "#text" VALUE COMES FROM THE XML SPECIFICATION)
                try{
                    if ( tempNode.getFirstChild().getNodeName().equals("#text") ){

                        if (!tempNode.getNodeName().equals("return")){
                            if (!hasResponseMultipleValues){
                                hasResponseMultipleValues = true;
                                responseValueList.add("The Server response includes multiple values:");
                                responseValueList.add("\n");
                            }
                            responseValueList.add("Parameter Name = " + tempNode.getNodeName());
                            responseValueList.add("Parameter Value = " + tempNode.getFirstChild().getTextContent());
                            responseValueList.add("\n");
                        }
                        else
                            responseValueList.add("Server Returned Value = " + tempNode.getFirstChild().getTextContent() + "\n");                        
                    }  
                }
                catch(Exception e){
                    //The execption means that the server has returned an exception as response
                    log += e.getMessage();
                    createLogFile();
                }
            }
        }       
    }        
    
    public ArrayList<String> getListOfInvokedMethodsDetails(String clientDirectoryPath, ArrayList<String> listOfInvokedMethodsNames){
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
                    if ( line.contains("<S:Envelope")){
                        isTheRequestSection = false;
                        
                        int index1=line.indexOf("<S:Envelope"); 
                        int index2=line.indexOf("</S:Envelope>");
                        String envelope = "</S:Envelope>";
                        
                        String soapEnvelope = line.substring(index1, (index2 + envelope.length()));                        
                        NodeList list = getSoapBodyFirstChildNodes(soapEnvelope);
                        
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
                    if ( line.contains("<S:Envelope")){
                        isTheResponseSection = false;
                        
                        int index1=line.indexOf("<S:Envelope");   
                        int index2=line.indexOf("</S:Envelope>");
                        String envelope = "</S:Envelope>";      
                        
                        String soapEnvelope = line.substring(index1, (index2 + envelope.length()));                            
                        NodeList list = getSoapBodyFirstChildNodes(soapEnvelope);

                        responseValueList = new ArrayList();
                        //responseValueList.add("Server:");
                        hasResponseMultipleValues = false;
                        populateTheResponseValueListVariableWithTheNodesValue(list);
                        
                        for ( String response : responseValueList)
                            details.add(response);                        
                    }
                }
            }  
            br.close();
        }
        catch(Exception e){
            log += e.getMessage();
            createLogFile();
        }
        return details;
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