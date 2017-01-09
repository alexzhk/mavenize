package com.pentaho.maven.transform.assembly;

import com.pentaho.maven.transform.MainRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Aliaksandr_Zhuk on 12/14/2016.
 */
public class AntArchiveFolderComparator {

    private Path folder;
    private String shim;

    private List<String> mavenDirtyTree;
    private List<String> mavenTree;
    private List<String> antTree;

    private HashSet<String> onlyInAntTree;

    private List<String> parentArtifats = new ArrayList<>();
    private List<String> gavList = new ArrayList<>();
    private List<Boolean> parentWithChild = new ArrayList<>();

    private List<String> gavAntList = new ArrayList<>();

    private String[] gavArray;

    private List<String> dependencySection = new ArrayList<>();
    private List<String> dependencySectionFalse = new ArrayList<>();

    private StringBuilder tagSection = new StringBuilder();

    private static final Logger LOG = LoggerFactory.getLogger(MainRunner.class);


    public AntArchiveFolderComparator(Path folder, String shim, List antTree, List mavenTree, List mavenDirtyTree) {

        this.folder = folder;
        this.shim = shim;

        this.antTree = antTree;
        this.mavenTree = mavenTree;
        this.mavenDirtyTree = mavenDirtyTree;

        initializeTreeLists();
        DependencySectionGenerator.setAntTreeAndGAVList(this.antTree, gavAntList);

    }

    public List<String> getDependencySection() {
        return dependencySection;
    }

    public StringBuilder getTagSection() {
        return tagSection;
    }

    private void addIncludeTag(List<String> dependencySection, List<String> list, String comment) {

        if (list.size() > 0) {

            if (!comment.equals("")) {
                dependencySection.add(comment);
            }

            for (String include : list) {
                dependencySection.add("        <include>" + include + "</include>\n");
            }
        }

    }

    private void createDependencySection(List<String> mavenIncludes0, List<String> mavenIncludes1, List<String> mavenIncludesFalse, List<String> antIncludes) {

        addIncludeTag(dependencySectionFalse, mavenIncludesFalse, "");
        addIncludeTag(dependencySection, mavenIncludes0, "<!-- Parent MVN Dependencies -->\n");
        addIncludeTag(dependencySection, mavenIncludes1, "<!-- Transitive MVN Dependencies -->\n");
        addIncludeTag(dependencySection, antIncludes, "<!-- Transitive ANT Dependencies -->\n");

    }

    private void createIncludeTagsLists(List<String> inMavenTree, List<String> inAntTree) {

        int index = -1;
        String include = "";
        String[] gav;

        //Transitive MVN Dependencies
        List<String> mavenIncludes0 = new ArrayList<>();
        //Parent MVN Dependencies
        List<String> mavenIncludes1 = new ArrayList<>();
        //Parent Non Transitive MVN Dependencies
        List<String> mavenIncludesFalse = new ArrayList<>();
        //Transitive ANT Dependencies
        List<String> antIncludes = new ArrayList<>();

        for (String mavenArtifact : inMavenTree) {

            index = gavList.indexOf(mavenArtifact);

            if (index != -1) {
                gav = mavenTree.get(index).split(":");
                include = gav[0] + ":" + gav[1];

                if (parentArtifats.get(index).equals("0")) {
                    mavenIncludes0.add(include);
                } else if (parentWithChild.get(index) == true) {
                    mavenIncludes1.add(include);
                } else {
                    mavenIncludesFalse.add(include);
                }
            }
        }

        for (String antArtifact : inAntTree) {

            index = gavAntList.indexOf(antArtifact);

            if (index != -1) {
                gav = antTree.get(index).split("#");
                include = gav[0] + ":" + gav[1];
                antIncludes.add(include);
            }
        }

        createDependencySection(mavenIncludes0, mavenIncludes1, mavenIncludesFalse, antIncludes);

    }


    public List<String> compareWithAntTree(List<String> unresolvedMavenArtifacts) {

        List<String> inAntTree = unresolvedMavenArtifacts.stream().filter(s -> gavAntList.contains(s)).sorted().collect(Collectors.toList());
        List<String> notInAntTree = unresolvedMavenArtifacts.stream().filter(s -> !gavAntList.contains(s)).sorted().collect(Collectors.toList());

        if (notInAntTree != null) {
            LOG.warn("Some artifasts are not found in any tree: " + Arrays.toString(notInAntTree.toArray()));
        }

        return inAntTree;

    }


    private void collectOnlyInAntTree(List<String> inAntTree) {

        DependencySectionGenerator.collectAdditionalDeps(inAntTree);
    }

    public void compareWithMavenTree(String archDir) throws IOException {

        Path archFolder = Paths.get(folder.toString(), shim, MainRunner.ANT_TARGET_FOLDER, shim, archDir);

        List<String> inAntTree;

        HashSet<String> fileNames = new HashSet<String>();

        Files.list(archFolder).filter(path -> path.getFileName().toString().endsWith("jar")).forEach(path -> fileNames.add(path.getFileName().toString()));

        List<String> inMavenTree = fileNames.stream().filter(s -> gavList.contains(s)).sorted().collect(Collectors.toList());
        List<String> notInMavenTree = fileNames.stream().filter(s -> !gavList.contains(s)).sorted().collect(Collectors.toList());

        inAntTree = compareWithAntTree(notInMavenTree);

        collectOnlyInAntTree(inAntTree);

        createIncludeTagsLists(inMavenTree, inAntTree);

    }


    private void createTagSection(String archFolder) {

        TagSections tag;

        archFolder = archFolder.replace("\\", "/");

        StringBuilder s = new StringBuilder();

        if (dependencySectionFalse.size() > 0) {
            tag = new TagSections(archFolder, dependencySectionFalse);
            s.append(tag.getTransitiveFalse());
        }

        if (dependencySection.size() > 0) {
            tag = new TagSections(archFolder, dependencySection);
            s.append(tag.getFilteringFalse());
        }

        if (!s.toString().isEmpty()) {
            tagSection.append(s.toString());
        }

    }

    private void initializeTreeLists() {

        parentArtifats = new ArrayList<>();
        gavList = new ArrayList<>();
        parentWithChild = new ArrayList<>();
        gavAntList = new ArrayList<>();

        for (String dertyArtifact : mavenDirtyTree) {

            if (dertyArtifact.charAt(0) == '+') {
                parentArtifats.add("1");
            } else {
                parentArtifats.add("0");
            }

        }

        for (String artifact : mavenTree) {

            gavArray = artifact.split(":");

            if (gavArray.length == 6) {
                gavList.add(gavArray[1] + "-" + gavArray[4] + "-" + gavArray[3] + "." + gavArray[2]);
            } else {
                gavList.add(gavArray[1] + "-" + gavArray[3] + "." + gavArray[2]);
            }

        }

        for (int i = 0; i < parentArtifats.size() - 1; i++) {
            String first = parentArtifats.get(i);
            for (int j = i + 1; j < parentArtifats.size(); j++) {
                String second = parentArtifats.get(j);

                if ((second.equals(first) && first.equals("1")) || j == parentArtifats.size() - 1) {
                    parentWithChild.add(false);
                } else {
                    parentWithChild.add(true);
                }
                break;
            }
        }

        if (parentArtifats.get(parentArtifats.size() - 1).equals("1")) {
            parentWithChild.add(false);
        } else {
            parentWithChild.add(true);
        }


        for (String antArtifact : antTree) {
            gavArray = antArtifact.split("#");
            gavAntList.add(gavArray[1] + "-" + gavArray[2] + ".jar");
        }

    }

    public void compareFolderWithTree(String folder) throws IOException {

        dependencySection = new ArrayList<>();
        dependencySectionFalse = new ArrayList<>();

        compareWithMavenTree(folder);

        createTagSection(folder);

    }

    public void compareFolderWithArtifactNames() throws IOException {

        Path archFolder = Paths.get(folder.toString(), shim, MainRunner.ANT_TARGET_FOLDER, shim, MainRunner.DEFAULT_FOLDER, MainRunner.CLIENT_FOLDER);

        compareFolderWithTree(MainRunner.DEFAULT_FOLDER);

        if (Files.exists(archFolder)){
            compareFolderWithTree(MainRunner.DEFAULT_FOLDER + "\\" + MainRunner.CLIENT_FOLDER);
        }

        compareFolderWithTree(MainRunner.DEFAULT_FOLDER + "\\" + MainRunner.PMR_FOLDER);

    }

}
