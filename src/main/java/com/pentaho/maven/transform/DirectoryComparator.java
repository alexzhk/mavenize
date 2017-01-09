package com.pentaho.maven.transform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import static com.pentaho.maven.transform.MainRunner.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Vasilina_Terehova on 12/9/2016.
 */
public class DirectoryComparator {
    public void compare(Path path1, Path path2) throws IOException {
        HashSet<String> fileNames1 = new HashSet<String>();
        HashSet<String> fileNames2 = new HashSet<String>();
        Files.list(path1).forEach(path -> fileNames1.add(path.getFileName().toString()));
        Files.list(path2).forEach(path -> fileNames2.add(path.getFileName().toString()));
        List<String> weHaveOnlyInPath1 = fileNames1.stream().filter(s -> !fileNames2.contains(s)).sorted().collect(Collectors.toList());
        List<String> weHaveOnlyInPath2 = fileNames2.stream().filter(s -> !fileNames1.contains(s)).sorted().collect(Collectors.toList());
        System.out.println(weHaveOnlyInPath1);
        System.out.println(weHaveOnlyInPath2);
    }

    public void compare(Path path1, Path path2, Path parent, Path parent2) throws IOException {
        HashSet<String> fileNames1 = new HashSet<String>();
        HashSet<String> fileNames2 = new HashSet<String>();
        Files.list(path1).forEach(path -> fileNames1.add(path.getFileName().toString()));
        Files.list(path2).forEach(path -> fileNames2.add(path.getFileName().toString()));
        List<String> weHaveOnlyInPath1 = fileNames1.stream().filter(s -> !fileNames2.contains(s)).sorted().collect(Collectors.toList());
        List<String> weHaveOnlyInPath2 = fileNames2.stream().filter(s -> !fileNames1.contains(s)).sorted().collect(Collectors.toList());
        Iterator<String> iterator = weHaveOnlyInPath2.iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            if (Files.exists(Paths.get(parent.toString(), next))) {
               iterator.remove();
            }
            if (Files.exists(Paths.get(parent2.toString(), next))) {
                iterator.remove();
            }
        }
        System.out.println(weHaveOnlyInPath1);
        System.out.println(weHaveOnlyInPath2);
    }

    public static void compare(String shimName, String antFolder, String mavenFolder) throws IOException {
        Path antPath = Paths.get(antFolder);
        Path mavenPath = Paths.get(mavenFolder);
        Optional<Path> zipArchiveAnt = Files.list(antPath).filter(path -> (path.getFileName().toString().endsWith(ZIP_EXTENSION) && path.getFileName().toString().contains("package"))).findFirst();
        Optional<Path> zipArchiveMaven = Files.list(mavenPath).filter(path -> (path.getFileName().toString().endsWith(ZIP_EXTENSION))).findFirst();
        //for running this 7z needed installed
        BashExecutor antBashExecutor = new BashExecutor(antPath);
        BashExecutor mavenBashExecutor = new BashExecutor(mavenPath);
        antBashExecutor.unArchive(zipArchiveAnt.get(), Paths.get(antPath.toString(), TEMP_EXTRACT_FOLDER));
        mavenBashExecutor.unArchive(zipArchiveMaven.get(), Paths.get(mavenPath.toString(), TEMP_EXTRACT_FOLDER));

        Path defaultFolderAnt = Paths.get(antPath.toString(), TEMP_EXTRACT_FOLDER, shimName,
                DEFAULT_FOLDER);
        Path defaultFolderMaven = Paths.get(mavenPath.toString(), TEMP_EXTRACT_FOLDER,
                shimName, DEFAULT_FOLDER);
        Path clientFolderAnt = Paths.get(defaultFolderAnt.toString(), CLIENT_FOLDER);
        Path clientFolderMaven = Paths.get(defaultFolderMaven.toString(), CLIENT_FOLDER);
        Path pmrFolderAnt = Paths.get(defaultFolderAnt.toString(), PMR_FOLDER);
        Path pmrFolderMaven = Paths.get(defaultFolderMaven.toString(), PMR_FOLDER);
        if (Files.exists(clientFolderAnt)) {
            System.out.println("---------------compare client folders");
            new DirectoryComparator().compare(clientFolderAnt, clientFolderMaven, defaultFolderMaven, pmrFolderMaven);
        }
        System.out.println("---------------compare default folders");
        new DirectoryComparator().compare(defaultFolderAnt, defaultFolderMaven);
        System.out.println("---------------compare pmr folders");
        new DirectoryComparator().compare(pmrFolderAnt, pmrFolderMaven);


    }

    public static void runCompareForShim(Path modulePath, BashExecutor moduleBashExecutor) throws IOException {
        moduleBashExecutor.runAntCleanAllResolveDist();
        moduleBashExecutor.runMvnCleanInstall();
        Optional<Path> zipArchiveAnt = Files.list(Paths.get(modulePath.toString(), ANT_TARGET_FOLDER)).filter(path -> path.getFileName().toString().endsWith(ZIP_EXTENSION)).findFirst();
        Optional<Path> zipArchiveMaven = Files.list(Paths.get(modulePath.toString(), MAVEN_TARGET_FOLDER)).filter(path -> path.getFileName().toString().endsWith(ZIP_EXTENSION)).findFirst();
        //for running this 7z needed installed
        moduleBashExecutor.unArchive(zipArchiveAnt.get(), Paths.get(modulePath.toString(), ANT_TARGET_FOLDER, TEMP_EXTRACT_FOLDER));
        moduleBashExecutor.unArchive(zipArchiveMaven.get(), Paths.get(modulePath.toString(), MAVEN_TARGET_FOLDER, TEMP_EXTRACT_FOLDER));

        Path defaultFolderAnt = Paths.get(modulePath.toString(), ANT_TARGET_FOLDER, TEMP_EXTRACT_FOLDER, modulePath.getFileName().toString(),
                DEFAULT_FOLDER);
        Path defaultFolderMaven = Paths.get(modulePath.toString(), MAVEN_TARGET_FOLDER, TEMP_EXTRACT_FOLDER,
                modulePath.getFileName().toString(), DEFAULT_FOLDER);
        Path clientFolderAnt = Paths.get(defaultFolderAnt.toString(), CLIENT_FOLDER);
        Path clientFolderMaven = Paths.get(defaultFolderMaven.toString(), CLIENT_FOLDER);
        Path pmrFolderAnt = Paths.get(defaultFolderAnt.toString(), PMR_FOLDER);
        Path pmrFolderMaven = Paths.get(defaultFolderMaven.toString(), PMR_FOLDER);
        System.out.println("---------------compare default folders");
        new DirectoryComparator().compare(clientFolderAnt, clientFolderMaven);
        System.out.println("---------------compare client folders");
        new DirectoryComparator().compare(defaultFolderAnt, defaultFolderMaven);
        System.out.println("---------------compare pmr folders");
        new DirectoryComparator().compare(pmrFolderAnt, pmrFolderMaven);
    }


}
