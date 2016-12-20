package com.pentaho.maven.transform.assembly;

import com.pentaho.maven.transform.BashExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Aliaksandr_Zhuk on 12/14/2016.
 */
public class MavenTreeGenerator implements TreeGenerator {

    public static final String TEMP_MAVEN_TREE_TXT = "temptree.txt";

    private List mavenDirtyTree;
    private List mavenTree;

    public List getMavenDirtyTree() {
        return mavenDirtyTree;
    }

    public List getMavenTree() {
        return mavenTree;
    }

    @Override
    public void createTree(Path shim) throws IOException {
        BashExecutor moduleBashExecutor = new BashExecutor(shim);
        moduleBashExecutor.runMvnDependencyTree(TEMP_MAVEN_TREE_TXT);

        getArtifactsFromMavenTree(shim);

    }

    private void getArtifactsFromMavenTree(List<String> artifacts) {

        String[] tempArr;
        mavenTree = new ArrayList<>();

        for (String s : artifacts) {
            tempArr = s.split("\\s");
            mavenTree.add(tempArr[tempArr.length - 1]);
        }

    }

    private void getArtifactsFromMavenTree(Path shim) throws IOException {

        Path tempMavenTreeFile = Paths.get(shim.toString(), TEMP_MAVEN_TREE_TXT);
        mavenDirtyTree = Files.readAllLines(tempMavenTreeFile);
        Files.deleteIfExists(tempMavenTreeFile);

        getArtifactsFromMavenTree(mavenDirtyTree);

    }
    
}
