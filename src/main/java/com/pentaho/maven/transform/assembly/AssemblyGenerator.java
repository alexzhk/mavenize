package com.pentaho.maven.transform.assembly;

import com.pentaho.maven.transform.FileUtils;
import com.pentaho.maven.transform.MainRunner;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Aliaksandr_Zhuk on 12/12/2016.
 */
public class AssemblyGenerator {

    public static final String TEMP_MVN_SHIM_FOLDER = "temp";


    private static String shimName = "";

    private String folder;


    public AssemblyGenerator(String folder) {

        this.folder = folder;

    }


    public void createTempFolder(String folder) throws IOException {

        Path temp = Paths.get(folder.toString(), TEMP_MVN_SHIM_FOLDER);

        FileUtils.deleteFolderWithFiles(temp);
        FileUtils.createFolder(temp);

    }


    private void createMavenStructure(Path mavenResourceFolder) throws IOException {

        //FileUtils.createFolders(mavenSourceFolder);
        FileUtils.createFolders(mavenResourceFolder);

    }


   /* private void copyFilesFromAntToMaven(Path shim) throws IOException{

        MainRunner runner = new MainRunner(folder);
        runner.moveSourceFolder(shim);

    }*/


    public void createMavenTempProject(Path shim) throws IOException {

        shimName = shim.getFileName().toString();

        //Path mavenResourceFolder = Paths.get(folder, TEMP_MVN_SHIM_FOLDER, shimName, MainRunner.resourceJavaSubfolder);
        Path mavenDescriptorFolder = Paths.get(folder, TEMP_MVN_SHIM_FOLDER, shimName, MainRunner.descriptorJavaSubfolder);

        createMavenStructure(mavenDescriptorFolder);
        //  copyFilesFromAntToMaven(shim);

    }

}
