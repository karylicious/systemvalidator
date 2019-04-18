package com.clieser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
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

public class FileAssistant {
    
    private static final String CURRENT_WORKING_DIRECTORY = System.getProperty("user.dir") + "\\src\\main\\java\\com\\clieser";
    private static final String TEMPORARY_DIRECTORY_PATH = CURRENT_WORKING_DIRECTORY + "\\Temp";
    private static final String EXERCISES_DIRECTORY_PATH =  CURRENT_WORKING_DIRECTORY + "\\exercises";
    private static final String LOG_DIRECTORY_PATH = CURRENT_WORKING_DIRECTORY + "\\Logs";
    
    public static String getTempDirectoryPath(){ return TEMPORARY_DIRECTORY_PATH; }
    
    public static String getLogDirectoryPath(){ return LOG_DIRECTORY_PATH; }
    
    public static String getExercisesDirectoryPath(){ return EXERCISES_DIRECTORY_PATH; }    
    
    public static void createDirectory(String directory) throws Exception{
        if (!(Files.exists(Paths.get(directory)))) 
            Files.createDirectories(Paths.get(directory));        
    }       
    
    public static void createLogAndTemporaryAndExercisesDirectories() throws Exception{
        createDirectory(TEMPORARY_DIRECTORY_PATH);        
        createDirectory(LOG_DIRECTORY_PATH);
        createDirectory(EXERCISES_DIRECTORY_PATH);  
    }  
      
    public static void createLogFile(String log) {  
        try {
            File file = new File(LOG_DIRECTORY_PATH + "\\" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".txt");
            FileOutputStream outputStream = new FileOutputStream(file);
            OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream); 

            Writer theWriter = new BufferedWriter(outputWriter);
            theWriter.write(log);
            theWriter.close();            
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    } 
    
    public static void uploadFile(DataHandler selectedFile, String fileName, String uploadLocation) throws IOException {           
        InputStream input = selectedFile.getInputStream();
        OutputStream output = new FileOutputStream( new File(uploadLocation + "\\"+ fileName));

        // Reads at most 100000 bytes from the supplied input stream and
        // returns them as a byte array
        byte[] b = new byte[100000];
        int bytesRead = 0;

        // iterate as long as bytesRead  is not -1 or End Of File is reached
        // write the bytes into the new file
        while ((bytesRead = input.read(b)) != -1) {
            output.write(b, 0, bytesRead);
        }         
    }
    
    public static void createNewBatchFile(String intendedFile, ArrayList<String> commands) throws IOException {  
        File file = new File(intendedFile);
        file.createNewFile();
        PrintWriter writer = new PrintWriter(intendedFile, "UTF-8");
        
        for (String currentCommand:  commands){
            writer.println(currentCommand);
        }
        writer.close();
    }      
     
    public static void deleteDirectory(File directory){
        // System.gc() is used beacause somehow the zip file reader does not get closed after reading the a file. 
        // The System.gc() will suggest that the VM do a garbage collection
        System.gc();
        delete(directory);
    }   
    
    public static void delete(File file){        
    	if(file.isDirectory()){ 
            //directory is empty, then delete it
            if(file.list().length==0)  			
               file.delete();
            
            else{
               //list all the directory contents
               String files[] = file.list();
               for (String temp : files) {
                  File fileDelete = new File(file, temp);
                  //recursive delete
                 delete(fileDelete);
               }
               //check the directory again, if empty then delete it
               if(file.list().length==0)
                 file.delete();               
            }    		
    	}
        else
            file.delete();    	
    }    
    
    public static String[] getFilesInCurrentDirectory(String parentDirectoryPath){
        File file = new File(parentDirectoryPath);
        
        return file.list(new FilenameFilter() {
          @Override
          public boolean accept(File current, String name) {
            return new File(current, name).isFile();
          }
        });        
    }
    
    public static String[] getSubdirectories(String parentDirectoryPath){      
        File file = new File(parentDirectoryPath);
        
        file.list(new FilenameFilter() {
          @Override
          public boolean accept(File current, String name) {
            return new File(current, name).isDirectory();
          }
        });
        
        return file.list();
    }
               
    public static ArrayList<String> unzipAndGetTheProjectsToBeTested(final String zipFilePath, String unzipLocation, boolean isAnExerciseFile) throws IOException { 
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
            name = name.replaceAll("\\s", "");
           
            File file = new File(unzipLocation, name);//new File(unzipLocation + "\\" + name);
             
            if (name.endsWith("/")) {
                if(!foundFirstDirectoryInTheZipFile){
                    if (file.exists()){ // This means that there is already a web service with the same name as the one on the zip file
                        zipFile.close(); 
                        return listOfProjectsToBeTested;
                    }
                    
                    foundFirstDirectoryInTheZipFile = true; 
                    if(isAnExerciseFile)
                        listOfProjectsToBeTested.add(name);
                    else
                        listOfProjectsToBeTested.add(file.getParentFile().getName());
                    
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
}