package com.pentaho.maven.transform.ivy;

import com.pentaho.maven.transform.BashExecutor;
import org.apache.maven.model.Exclusion;
import org.jdom2.Element;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by Aliaksandr_Zhuk on 1/2/2017.
 */
public class GlobalExcludeAnalyzer {

    Map<Scopes, List<Exclusion>> map = new HashMap<>();
    List<Element> list = new ArrayList<>();
    Path folder;

    public GlobalExcludeAnalyzer(List<Element> list, Path folder) {

        this.list = list;
        this.folder = folder;

    }


    public void parseExcludes() {

        List<Exclusion> defaultList = new ArrayList<>();
        List<Exclusion> clientList = new ArrayList<>();
        List<Exclusion> pmrList = new ArrayList<>();
        List<Exclusion> testList = new ArrayList<>();
        Exclusion exclusion;
        String[] arr;

        for (Element element : list) {

            exclusion = new Exclusion();
            exclusion.setGroupId(element.getAttributeValue("org"));
            exclusion.setArtifactId(element.getAttributeValue("module"));

            arr = element.getAttributeValue("conf").split(",");


            for (String s : arr) {

                if (s.equals(Scopes.DEFAULT.name().toLowerCase())) {
                    defaultList.add(exclusion);
                } else if (s.equals(Scopes.CLIENT.name().toLowerCase())) {
                    clientList.add(exclusion);
                } else if (s.equals(Scopes.PMR.name().toLowerCase())) {
                    pmrList.add(exclusion);
                } else if (s.equals(Scopes.TEST.name().toLowerCase())) {
                    testList.add(exclusion);
                }

            }

        }

        map.put(Scopes.DEFAULT, defaultList);
        map.put(Scopes.CLIENT, clientList);
        map.put(Scopes.PMR, pmrList);
        map.put(Scopes.TEST, pmrList);

    }


    private Map<String, List<String>> getListMavenTree(SubFolder subFolder) throws IOException {

        Path path = Paths.get(folder.toString(), IvuRunner.TEMP_MVN_SHIM_FOLDER, subFolder.name().toLowerCase(), IvuRunner.TEMP_MAVEN_TREE_TXT);
        List<String> mavenDirtyTree;
        List<String> mavenTree;
        Map<String, List<String>> map = new HashMap<>();

        mavenDirtyTree = Files.readAllLines(path);

        String[] tempArr;
        mavenTree = new ArrayList<>();

        for (String s : mavenDirtyTree) {
            tempArr = s.split("\\s");
            tempArr = (tempArr[tempArr.length - 1]).split(":");

            mavenTree.add(tempArr[0] + ":" + tempArr[1]);
        }

        map.put("dirty", mavenDirtyTree);

        map.put("deps", mavenTree);

        return map;

    }

    public Map<String, List<String>> createMavenTree(SubFolder subFolder) throws IOException {

        Map<String, List<String>> map;

        Path path = Paths.get(folder.toString(), IvuRunner.TEMP_MVN_SHIM_FOLDER, subFolder.name().toLowerCase());

        BashExecutor moduleBashExecutor = new BashExecutor(path);
        moduleBashExecutor.runMvnDependencyTree(IvuRunner.TEMP_MAVEN_TREE_TXT);

        map = getListMavenTree(subFolder);

        return map;

    }


    public void findParentElements(SubFolder subFolder, Scopes scope) throws IOException {

        Map<String, List<String>> mapTree;

        List<String> mavenDirtyTree;
        List<String> mavenTree;

        List<Exclusion> list;

        String element;

        mapTree = createMavenTree(subFolder);

        mavenDirtyTree = mapTree.get("dirty");
        mavenTree = mapTree.get("deps");


        Exclusion excl;
        String[] tempArr;
        List<Exclusion> mvnList = new ArrayList<>();

        for (String s : mavenTree) {
            tempArr = s.split(":");
            excl = new Exclusion();
            excl.setGroupId(tempArr[0]);
            excl.setArtifactId(tempArr[1]);

            mvnList.add(excl);
        }


        list = map.get(scope);

        int index;
        HashSet<Integer> indexSet = new HashSet<>();
        Map<Integer, List<Exclusion>> integerListMap = new HashMap<>();
        List<Exclusion> exclusionList;

        for (Exclusion exclusion : list) {
            index = -1;
            for (Exclusion el : mvnList) {

                if (exclusion.getArtifactId() != null) {
                    if (exclusion.getArtifactId().equals(el.getArtifactId()) && exclusion.getGroupId().equals(el.getGroupId())) {
                        index = mvnList.indexOf(el);
                        index = getParentElement(mavenDirtyTree, index);
                        exclusionList = integerListMap.get(index);

                        if (exclusionList != null) {
                            exclusionList.add(exclusion);

                        } else {

                            exclusionList = new ArrayList<>();
                            exclusionList.add(exclusion);
                            integerListMap.put(index, exclusionList);
                        }


                        indexSet.add(getParentElement(mavenDirtyTree, index));
                        break;
                    }
                } else {
                    if (exclusion.getGroupId().equals(el.getGroupId())) {
                        index = mvnList.indexOf(el);
                        indexSet.add(getParentElement(mavenDirtyTree, index));
                        break;
                    }
                }

            }

        }


    }


    private int getParentElement(List<String> list, int index) {

        int ind = -1;

        for (int i = index + 1; i < list.size(); i--) {

            if (list.get(i).startsWith("+")) {
                ind = i;
            }
        }

        return ind;
    }

}
