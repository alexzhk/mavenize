package com.pentaho.maven.transform;

import org.jdom2.JDOMException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import static com.pentaho.maven.transform.MainRunner.*;

/**
 * Created by Vasilina_Terehova on 1/8/2017.
 */
public class MainRunnerCommonSources {

    private final MainRunner mainRunner;

    public MainRunnerCommonSources(MainRunner mainRunner) {
        this.mainRunner = mainRunner;
    }

    public void moveCommonSourceFolders(Path folder) throws IOException, URISyntaxException {
        Path commonFolder = Paths.get(folder.toString(), "common");
        moveCommonSourceFolder(commonFolder, Paths.get(commonFolder.toString(), "common-shim"), "shim", "src", "test-src");
        moveCommonSourceFolder(commonFolder, Paths.get(commonFolder.toString(), "modern"), "modern", "src-modern", "test-src-modern");
        moveCommonSourceFolder(commonFolder, Paths.get(commonFolder.toString(), "mapred"), "mapred", "src-mapred", null);
        moveCommonSourceFolder(commonFolder, Paths.get(commonFolder.toString(), "hadoop-shim-1.0"), "hadoop-shim-1.0", "src-hadoop-shim-1.0", null);
        moveCommonSourceFolder(commonFolder, Paths.get(commonFolder.toString(), "hbase-1.0"), "hbase-1.0", "src-hbase-1.0", null);
        moveCommonSourceFolder(commonFolder, Paths.get(commonFolder.toString(), "hbase-shim-1.1"), "hbase-shim-1.1", "src-hbase-shim-1.1", "test-src-hbase-shim-1.1");
        moveCommonSourceFolder(commonFolder, Paths.get(commonFolder.toString(), "pig-shim-1.0"), "pig-shim-1.0", "src-pig-shim-1.0", null);
        moveCommonSourceFolder(commonFolder, Paths.get(commonFolder.toString(), "pig-shim-1.1"), "pig-shim-1.1", "src-pig-shim-1.1", null);
        try (Stream<Path> paths = Files.list(commonFolder)) {
            paths.forEach(filePath -> {
                if (Files.isDirectory(filePath) && Files.exists(Paths.get(filePath.toString(), POM_XML))) {
                    //we are in common module folder
                    try {
                        VersionGenerator.generatePropertyVersionSection(filePath);
                    } catch (JDOMException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

        }
    }

    public void moveCommonSourceFolder(Path commonFolder, Path newModuleFolder, String moduleName, String srcPath, String testPath) throws IOException, URISyntaxException {
        BashExecutor moduleBashExecutor = new BashExecutor(commonFolder);
        if (!Files.exists(newModuleFolder)) {
            Files.createDirectory(newModuleFolder);
        }
        mainRunner.createSourceDirectories(newModuleFolder);
        FileUtils.copyFolder(commonFolder, srcPath + "/org", newModuleFolder, sourceJavaSubfolder);
        FileUtils.copyFolder(commonFolder, srcPath + "/META-INF", newModuleFolder, resourceJavaSubfolder);
        if (testPath != null) {
            FileUtils.copyAllInsideFolder(commonFolder, testPath, Paths.get(newModuleFolder.toString(), testJavaSubfolder));
        }
        PomUtils.parseAndSavePom("pom_common_folder_artefact_template.xml", moduleName, newModuleFolder);
        mainRunner.addToGithub(newModuleFolder);
        mainRunner.removeGithubCommonSource(commonFolder, srcPath, testPath);
    }

}
