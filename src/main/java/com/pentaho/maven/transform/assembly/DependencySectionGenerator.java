package com.pentaho.maven.transform.assembly;

import com.pentaho.maven.transform.Artifact;
import com.pentaho.maven.transform.FileUtils;
import com.pentaho.maven.transform.MainRunner;
import com.pentaho.maven.transform.xml.XmlUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by Aliaksandr_Zhuk on 12/14/2016.
 */
public class DependencySectionGenerator {

    public static HashSet<String> onlyInAntTree = new HashSet<>();

    public static List<String> antTree;
    public static List<String> gavAntList;

    private Map<String, Artifact> antArtifact = new HashMap<>();


    public static void setAntTreeAndGAVList(List<String> antTree, List<String> gavAntList) {

        DependencySectionGenerator.antTree = antTree;
        DependencySectionGenerator.gavAntList = gavAntList;

    }


    public static void collectAdditionalDeps(List<String> inAntTree) {
        onlyInAntTree.addAll(inAntTree);
    }


    public void createDepSection() {

        int index = -1;
        List<String> list = new ArrayList<>();
        String[] arr;
        String key;

        for (String el : onlyInAntTree) {
            index = gavAntList.indexOf(el);

            if (index != -1) {
                arr = antTree.get(index).split("#");

                key = arr[0] + "-" + arr[1];
                antArtifact.put(key, new Artifact(arr[0], arr[1], arr[2], null, MainRunner.DEFAULT_SCOPE));

            }

        }

    }


    public void findArtifactInPOM(Path folder, Path shimName) throws JDOMException, IOException {

        List<String> removeList = new ArrayList<>();

        String pomPath = Paths.get(folder.toString(), shimName.toString(), MainRunner.POM_XML).toString();

        Document documentFromFile = XmlUtils.getDocumentFromFile(pomPath);
        Element rootElement = documentFromFile.getRootElement();

        Map<Artifact, Element> artifactElementMap = new HashMap<>();

        List<Element> dependencyList = rootElement.getContent(new ElementFilter("dependencies")).get(0).getChildren();
        dependencyList.stream().forEach(element -> {
            String version = XmlUtils.getTagValue(element, "version");
            String classifier = XmlUtils.getTagValue(element, "classifier");
            String groupId1 = XmlUtils.getTagValue(element, "groupId");
            String scope = XmlUtils.getTagValue(element, "scope");
            Artifact artifact = new Artifact(groupId1, XmlUtils.getTagValue(element, "artifactId"), version, classifier, scope);
            artifactElementMap.put(artifact, element);
        });

        if (!artifactElementMap.isEmpty()) {
            for (String ant : antArtifact.keySet()) {
                for (Artifact artifact : artifactElementMap.keySet()) {
                    if (ant.equals(artifact.getGroupId() + "-" + artifact.getArtifactId())) {

                        artifactElementMap.get(artifact).getContent(new ElementFilter("scope")).get(0).setText(MainRunner.DEFAULT_SCOPE);

                        if (antArtifact.get(ant).getVersion().equalsIgnoreCase(artifact.getVersion())) {
                            removeList.add(ant);
                            break;
                        } else {
                            artifactElementMap.get(artifact).getContent(new ElementFilter("version")).get(0).setText(antArtifact.get(ant).getVersion());
                            removeList.add(ant);
                            break;
                        }
                    }
                }
            }
        }

        if (!removeList.isEmpty()) {
            for (String el : removeList) {
                antArtifact.remove(el);
            }
        }

        addElementsToPOM(rootElement);

        XmlUtils.outputDoc(documentFromFile, Paths.get(folder.toString(), shimName.toString(), MainRunner.POM_XML).toString());

        Path filePath = Paths.get(folder.toString(), shimName.toString(), MainRunner.POM_XML);
        Path mvnPath = Paths.get(folder.toString(), AssemblyGenerator.TEMP_MVN_SHIM_FOLDER, shimName.toString(), MainRunner.POM_XML);

        FileUtils.copyFileReplace(filePath, mvnPath);

    }

    private void addElementsToPOM(Element rootElement) {

        Element element;
        Element exclusions;
        Element exclusion;

        for (String el : antArtifact.keySet()) {
            element = new Element("dependency");
            element.addContent(new Element("groupId").setText(antArtifact.get(el).getGroupId()));
            element.addContent(new Element("artifactId").setText(antArtifact.get(el).getArtifactId()));
            element.addContent(new Element("version").setText(antArtifact.get(el).getVersion()));
            element.addContent(new Element("scope").setText(MainRunner.DEFAULT_SCOPE));

            exclusions = new Element("exclusions");

            exclusion = new Element("exclusion");
            exclusion.addContent(new Element("groupId").setText("*"));
            exclusion.addContent(new Element("artifactId").setText("*"));
            exclusions.addContent(exclusion);

            element.addContent(exclusions);

            XmlUtils.updateNameSpaceParent(rootElement, element);

            rootElement.getContent(new ElementFilter("dependencies")).get(0).addContent(element);
        }
    }

}