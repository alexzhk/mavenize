package com.pentaho.maven.transform.assembly;

import com.pentaho.maven.transform.MainRunner;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Aliaksandr_Zhuk on 12/12/2016.
 */
public class Test {

    public static final String TEMP_MVN_SHIM_FOLDER = "temp";

    String folder;




    public static void main(String[] args) throws IOException {

   Test test = new Test();



        test.folder = "D://hadoop-shims";

        MainRunner runner = new MainRunner(test.folder);

        System.out.println("Test");


        AssemblyGenerator generateAssembly = new AssemblyGenerator(test.folder);

        generateAssembly.createTempFolder(test.folder);

        test.folder = "D://hadoop-shims/cdh58";

        Path cdh = Paths.get(test.folder);

        generateAssembly.createMavenTempProject(cdh);


        TreeGenerator antTree = new AntTreeGenerator();

        antTree.createTree(cdh);

    }

}
