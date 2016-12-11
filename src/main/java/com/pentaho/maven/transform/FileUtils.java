package com.pentaho.maven.transform;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

/**
 * Created by Vasilina_Terehova on 12/7/2016.
 */
public class FileUtils {

    public static void moveFolder(Path moduleFolder, String from, String to) throws IOException {
        Path fromRoot = Paths.get(moduleFolder.toString(), from);
        String firstTestPackageFolder = fromRoot.getFileName().toString();
        Path mavenTestSourceFolder = Paths.get(moduleFolder.toString(), to, firstTestPackageFolder);
        System.out.println("moving " + fromRoot + " to " + mavenTestSourceFolder);
        moveFile(fromRoot, mavenTestSourceFolder);
    }

    public static void moveAllInsideFolder(Path moduleFolder, String from, String to) throws IOException {
        Path fromRoot = Paths.get(moduleFolder.toString(), from);
        if (Files.exists(fromRoot)) {
            Stream<Path> list = Files.list(fromRoot);
            list.forEach(path -> {
                Path antFileLocation = Paths.get(moduleFolder.toString(), from, /*firstTestPackageFolder, */path.getFileName().toString());
                Path mavenFileLocation = Paths.get(moduleFolder.toString(), to, /*firstTestPackageFolder, */path.getFileName().toString());
                System.out.println("moving " + antFileLocation + " to " + mavenFileLocation);
                try {
                    moveFile(antFileLocation, mavenFileLocation);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

    }

    public static void moveFile(Path antTestSourceFolder, Path mavenTestSourceFolder) throws IOException {
        try {
            Files.move(antTestSourceFolder, mavenTestSourceFolder);
        } catch (NoSuchFileException e) {
            System.out.println("nothing to be moved " + antTestSourceFolder + " to " + mavenTestSourceFolder);
        }
    }

    public static void moveFileReplace(Path antTestSourceFolder, Path mavenTestSourceFolder) throws IOException {
        try {
            Files.move(antTestSourceFolder, mavenTestSourceFolder, StandardCopyOption.REPLACE_EXISTING);
        } catch (NoSuchFileException e) {
            System.out.println("nothing to be moved " + antTestSourceFolder + " to " + mavenTestSourceFolder);
        }
    }
}
