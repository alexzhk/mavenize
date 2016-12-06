package com.pentaho.maven.transform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Created by Vasilina_Terehova on 12/7/2016.
 */
public class FileUtils {

    public static void moveFolder(Path moduleFolder, String from, String to, boolean goInside) throws IOException {
//        if (revert) {
//            String temp = from;
//            from = to;
//            to = temp;
//        }
        Path fromRoot = Paths.get(moduleFolder.toString(), from);
        if (goInside) {
            if (!Files.exists(fromRoot)) {
                System.out.println("folder " + fromRoot.toString() + " doesn't exist");
                return;
            }
            Optional<Path> first = Files.list(fromRoot).findFirst();
            if (!first.isPresent()) {
                System.out.println("from folder is empty " + fromRoot);
                return;
            }
            fromRoot = first.get();
        } else {
            if (!Files.exists(fromRoot)) {
                System.out.println("from folder is empty " + fromRoot);
                return;
            }
        }


        Path antTestSourceFolder = fromRoot;
        String firstTestPackageFolder = antTestSourceFolder.getFileName().toString();
        Path mavenTestSourceFolder = Paths.get(moduleFolder.toString(), to, firstTestPackageFolder);
        System.out.println("moving " + antTestSourceFolder + " to " + mavenTestSourceFolder);
        try {
            Files.move(antTestSourceFolder, mavenTestSourceFolder);
        } catch (NoSuchFileException e) {
            System.out.println("nothing to be moved " + antTestSourceFolder + " to " + mavenTestSourceFolder);
        }
    }
}
