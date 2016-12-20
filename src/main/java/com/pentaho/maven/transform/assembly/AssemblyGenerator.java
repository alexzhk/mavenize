package com.pentaho.maven.transform.assembly;

import com.pentaho.maven.transform.BashExecutor;
import com.pentaho.maven.transform.FileUtils;
import com.pentaho.maven.transform.MainRunner;
import org.jdom2.JDOMException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Created by Aliaksandr_Zhuk on 12/12/2016.
 */
public class AssemblyGenerator {

    public static final String TEMP_MVN_SHIM_FOLDER = "temp";


    private Path shimName;

    private Path folder;

    private List<String> mavenDirtyTree;
    private List<String> mavenTree;
    private List<String> antTree;

    public AssemblyGenerator(Path folder) {

        this.shimName = folder.getFileName();
        this.folder = folder.getParent();

    }

    public void createTempFolder() throws IOException {

        Path temp = Paths.get(folder.toString(), TEMP_MVN_SHIM_FOLDER);

        FileUtils.deleteFolderWithFiles(temp);
        FileUtils.createFolder(temp);

    }

    private void createMavenStructure(Path mavenResourceFolder) throws IOException {

        FileUtils.createFolders(mavenResourceFolder);

    }


    public void createMavenTempProject() throws IOException {

        Path mavenDescriptorFolder = Paths.get(folder.toString(), TEMP_MVN_SHIM_FOLDER, shimName.toString(), MainRunner.DESCRIPTOR_FOLDER);

        createMavenStructure(mavenDescriptorFolder);

    }


    public void createMavenAndAntTree(Path shim) throws IOException {

        List<TreeGenerator> treeList = new ArrayList<>();

        treeList.add(new AntTreeGenerator());
        treeList.add(new MavenTreeGenerator());

        for (TreeGenerator tree : treeList) {
            tree.createTree(shim);
        }

        antTree = ((AntTreeGenerator) treeList.get(0)).getAntTreeList();
        mavenDirtyTree = ((MavenTreeGenerator) treeList.get(1)).getMavenDirtyTree();
        mavenTree = ((MavenTreeGenerator) treeList.get(1)).getMavenTree();

    }


    private String getArchiveName(String partName) {

        String archiveName = "";
        String[] artifact = mavenTree.get(0).split(":");

        if (partName != "") {
            partName = "-" + partName + "-";
        } else {

            partName = "-";
        }

        archiveName = artifact[1] + partName + artifact[3] + ".zip";

        return archiveName;
    }


    public void unzipAntArchive() throws IOException {

        Path shim = Paths.get(folder.toString(), shimName.toString());
        Path archivePath = Paths.get(folder.toString(), shimName.toString(), MainRunner.ANT_TARGET_FOLDER);

        BashExecutor moduleBashExecutor = new BashExecutor(shim);
        moduleBashExecutor.runAntDist();

        Optional<Path> zipArchiveAnt = Files.list(Paths.get(shim.toString(), MainRunner.ANT_TARGET_FOLDER)).filter(path -> path.getFileName().toString().endsWith(MainRunner.ZIP_EXTENSION)).findFirst();

        moduleBashExecutor.unArchive(zipArchiveAnt.get(), archivePath);

    }


    public void createTrees() throws JDOMException, IOException {

        Path shim = Paths.get(folder.toString(), shimName.toString());

        this.createTempFolder();
        this.createMavenTempProject();

        createMavenAndAntTree(shim);

        unzipAntArchive();

        createMavenFiles();

    }

    private void saveAssemblyTagsToFile(String assembly) throws IOException {

        Path filePath = Paths.get(folder.toString(), shimName.toString(), MainRunner.ASSEMBLY_XML);
        Path mvnPath = Paths.get(folder.toString(), TEMP_MVN_SHIM_FOLDER, shimName.toString(), MainRunner.DESCRIPTOR_FOLDER, MainRunner.ASSEMBLY_XML);

        FileUtils.deleteFile(filePath);

        FileUtils.createFile(filePath);
        FileUtils.writeTextToFile(filePath, assembly);

        FileUtils.copyFileReplace(filePath, mvnPath);

    }

    public void createAssembly() throws IOException {

        AntArchiveFolderComparator antComparator = new AntArchiveFolderComparator(folder, shimName.toString(), antTree, mavenTree, mavenDirtyTree);

        antComparator.compareFolderWithArtifactNames();

        String dependencySetSection = antComparator.getTagSection().toString();

        String assembly = new TagSections(shimName.toString(), dependencySetSection).getAssemblyPlugin();

        saveAssemblyTagsToFile(assembly);

    }

    private void resetSectionGenerator() {

        DependencySectionGenerator.onlyInAntTree = null;
        DependencySectionGenerator.antTree = null;
        DependencySectionGenerator.gavAntList = null;

    }

    public void addNewDependenciesInPOM() throws JDOMException, IOException {

        DependencySectionGenerator dep = new DependencySectionGenerator();

        dep.createDepSection();
        dep.findArtifactInPOM(folder, shimName);

        resetSectionGenerator();
    }


    public void createMavenFiles() throws JDOMException, IOException {

        createAssembly();
        addNewDependenciesInPOM();

        MavenChecker mavenChecker = new MavenChecker(folder, shimName, antTree);
        mavenChecker.compareArchArtifacts();

    }

}
