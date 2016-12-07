package com.pentaho.maven.transform;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Created by Vasilina_Terehova on 12/7/2016.
 */
public class BashExecutor {

    private final Path folderExecute;

    public BashExecutor(Path folderExecute) {
        this.folderExecute = folderExecute;
    }

    public void gitAdd(Path path) throws IOException {
        gitAdd(path.toString());
    }

    public void gitAdd(String path) throws IOException {
        String command = "git add " + path;
        System.out.println(command);
        executeCommand(command);
    }

    public String executeCommand(String command) throws IOException {
        StringBuffer output = new StringBuffer();
        Process p;
        try {
            System.out.println(command);
            String[] commands = command.split("\\s");
            System.out.println("commands " + Arrays.toString(commands));
            ProcessBuilder builder = new ProcessBuilder(commands);
            System.out.println("folder " + folderExecute);
            builder.directory(new File(folderExecute.toString()));
            p = builder.start();
            //p = runtime.exec(command);
            //p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
                System.out.println(line);
            }

            System.out.println("error stream");
            BufferedReader errorReader =
                    new BufferedReader(new InputStreamReader(p.getErrorStream()));

            line = "";
            while ((line = errorReader.readLine()) != null) {
                output.append(line + "\n");
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return output.toString();

    }

    public void gitRemove(Path path) throws IOException {
        gitRemove(path.toString());
    }

    public void gitRemove(String path) throws IOException {
        executeCommand("git rm -r -f " + path);
    }
}
