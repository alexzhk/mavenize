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

    @Override
    public void createTree(Path shim) throws IOException {
        BashExecutor moduleBashExecutor = new BashExecutor(shim);
        moduleBashExecutor.runMvnDependencyTree(TEMP_MAVEN_TREE_TXT);

        getArtifactsFromMavenTree(shim);

    }

    private List getArtifactsFromMavenTree(Path shim) throws IOException {

        Pattern pattern;
        Matcher matcher;

        List<String> mavenTreeList = new ArrayList<>();

        //pattern = Pattern.compile(".*found.*");
        //matcher = pattern.matcher("");

        Path tempMavenTreeFile = Paths.get(shim.toString(), TEMP_MAVEN_TREE_TXT);

        List<String> list = Files.readAllLines(tempMavenTreeFile);

        for (String s : list) {

           // if (matcher.reset(s).matches()) {
                //System.out.println(s.split("\\s")[3].replace(";", "#"));
                mavenTreeList.add(s.split("\\s")[s.length() - 1]);
           // }
        }

        Files.deleteIfExists(tempMavenTreeFile);

        return mavenTreeList;
    }

}
