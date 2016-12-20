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
public class AntTreeGenerator implements TreeGenerator {

    public static final String TEMP_ANT_TREE_TXT = "temptree.txt";

    private List antTreeList = new ArrayList<>();

    public List getAntTreeList() {
        return antTreeList;
    }

    @Override
    public void createTree(Path shim) throws IOException {
        BashExecutor moduleBashExecutor = new BashExecutor(shim);
        moduleBashExecutor.runAntCleanAll();
        moduleBashExecutor.runAntResolveToFile(TEMP_ANT_TREE_TXT);

        getArtifactsFromAntTree(shim);

    }

    private void getArtifactsFromAntTree(Path shim) throws IOException {

        Pattern pattern;
        Matcher matcher;

        pattern = Pattern.compile(".*found.*");
        matcher = pattern.matcher("");

        Path tempAntTreeFile = Paths.get(shim.toString(), TEMP_ANT_TREE_TXT);

        List<String> list = Files.readAllLines(tempAntTreeFile);

        for (String s : list) {

            if (matcher.reset(s).matches()) {
                antTreeList.add(s.split("\\s")[3].replace(";", "#"));
            }
        }

        Files.deleteIfExists(tempAntTreeFile);

    }

}