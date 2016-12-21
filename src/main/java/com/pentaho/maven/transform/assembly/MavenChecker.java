package com.pentaho.maven.transform.assembly;

import com.pentaho.maven.transform.Artifact;
import com.pentaho.maven.transform.BashExecutor;
import com.pentaho.maven.transform.FileUtils;
import com.pentaho.maven.transform.MainRunner;
import com.pentaho.maven.transform.xml.XmlUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Aliaksandr_Zhuk on 12/14/2016.
 */
public class MavenChecker {

    private Path shimName;
    private Path folder;


    private List<String> antTree = new ArrayList<>();
    private List<String> gavAnt = new ArrayList<>();
    private List<String> gaAnt = new ArrayList<>();
    private List<String> aAnt = new ArrayList<>();
    private List<String> avAnt = new ArrayList<>();

    List<String> tmpList = new ArrayList<>();

    private Map<String, Artifact> antArtifacts = new HashMap<>();

    private HashSet<String> mavenFolders = new HashSet<>();
    private HashSet<String> antFolders = new HashSet<>();

    private HashSet<String> dependencies = new HashSet<>();

    BashExecutor moduleBashExecutor;


    public MavenChecker(Path folder, Path shimName, List antTree) throws IOException {
        this.folder = folder;
        this.shimName = shimName;
        this.antTree = antTree;

        Path archivePath = Paths.get(this.folder.toString(), AssemblyGenerator.TEMP_MVN_SHIM_FOLDER, this.shimName.toString(), MainRunner.MAVEN_TARGET_FOLDER);

        moduleBashExecutor = new BashExecutor(Paths.get(this.folder.toString(), AssemblyGenerator.TEMP_MVN_SHIM_FOLDER, this.shimName.toString()));
        moduleBashExecutor.runMvnCleanInstall();

        Optional<Path> zipArchiveMaven = Files.list(Paths.get(folder.toString(), AssemblyGenerator.TEMP_MVN_SHIM_FOLDER, this.shimName.toString(), MainRunner.MAVEN_TARGET_FOLDER)).filter(path -> path.getFileName().toString().endsWith(MainRunner.ZIP_EXTENSION)).findFirst();

        moduleBashExecutor.unArchive(zipArchiveMaven.get(), archivePath);

        transferListToMap();

    }

    private void transferListToMap() {

        int index = -1;
        List<String> list = new ArrayList<>();
        String[] arr;
        String key;

        for (String el : antTree) {
            arr = el.split("#");
            gavAnt.add(arr[1] + "-" + arr[2] + ".jar");
            gaAnt.add(arr[0] + "-" + arr[1]);
            aAnt.add(arr[1]);
            avAnt.add(arr[1] + "-" + arr[2]);
        }

    }

    /**
     * deletes duplicate pares groupId+artifactId only in lib and client folders
     */
    private HashSet<String> prepareArchArtifacts(Boolean isMaven) throws IOException {

        HashSet<String> set = new HashSet<>();

        Path libFolder;
        Path clientFolder;
        Path pmrFolder;

        Pattern pattern;
        Matcher matcher;

        List<Integer> indexList = new ArrayList<>();

        HashSet<String> fileNames1 = new HashSet<String>();
        HashSet<String> fileNames2 = new HashSet<String>();
        HashSet<String> fileNames3 = new HashSet<String>();


        if (isMaven) {
            libFolder = Paths.get(folder.toString(), AssemblyGenerator.TEMP_MVN_SHIM_FOLDER, shimName.toString(), MainRunner.MAVEN_TARGET_FOLDER, shimName.toString(), MainRunner.DEFAULT_FOLDER);
            clientFolder = Paths.get(folder.toString(), AssemblyGenerator.TEMP_MVN_SHIM_FOLDER, shimName.toString(), MainRunner.MAVEN_TARGET_FOLDER, shimName.toString(), MainRunner.DEFAULT_FOLDER, MainRunner.CLIENT_FOLDER);
            pmrFolder = Paths.get(folder.toString(), AssemblyGenerator.TEMP_MVN_SHIM_FOLDER, shimName.toString(), MainRunner.MAVEN_TARGET_FOLDER, shimName.toString(), MainRunner.DEFAULT_FOLDER, MainRunner.PMR_FOLDER);

        } else {
            libFolder = Paths.get(folder.toString(), shimName.toString(), MainRunner.ANT_TARGET_FOLDER, shimName.toString(), MainRunner.DEFAULT_FOLDER);
            clientFolder = Paths.get(folder.toString(), shimName.toString(), MainRunner.ANT_TARGET_FOLDER, shimName.toString(), MainRunner.DEFAULT_FOLDER, MainRunner.CLIENT_FOLDER);
            pmrFolder = Paths.get(folder.toString(), shimName.toString(), MainRunner.ANT_TARGET_FOLDER, shimName.toString(), MainRunner.DEFAULT_FOLDER, MainRunner.PMR_FOLDER);
        }

        Files.list(libFolder).filter(path -> path.getFileName().toString().endsWith("jar")).forEach(path -> fileNames1.add(path.getFileName().toString()));

        if (Files.exists(clientFolder)) {
            Files.list(clientFolder).filter(path -> path.getFileName().toString().endsWith("jar")).forEach(path -> fileNames2.add(path.getFileName().toString()));
        }

        Files.list(pmrFolder).filter(path -> path.getFileName().toString().endsWith("jar")).forEach(path -> fileNames3.add(path.getFileName().toString()));

        String tmp = "";
        for (String el : fileNames1) {

            if (!gavAnt.contains(tmp) && !tmp.equals("")) {

                tmpList.add(tmp);
                tmp = "";

            }
            set.add(el);
            for (String artifact : avAnt) {
                if (el.equals(artifact + ".jar")) {
                    indexList.add(avAnt.indexOf(artifact));
                    break;
                }
                tmp = el;
            }
        }

        if (Files.exists(clientFolder)) {
            for (String el : fileNames2) {
                set.add(el);
                for (Integer artifact : indexList) {
                    pattern = Pattern.compile("^" + aAnt.get(artifact) + "-[0-9].*");
                    matcher = pattern.matcher("");
                    if (matcher.reset(el).matches() && !(avAnt.get(artifact) + ".jar").equals(el)) {
                        if (isMaven) {
                            dependencies.add(gavAnt.get(artifact));
                        }
                        set.remove(el);
                        break;
                    }

                }
            }
        }

        for (String el : fileNames3) {
            set.add(el);
        }

        return set;
    }

    public void compareArchArtifacts() throws IOException, JDOMException {

        String[] arr;

        HashSet<String> maven;
        HashSet<String> ant;

        maven = prepareArchArtifacts(true);
        ant = prepareArchArtifacts(false);

        List<String> notInMavenArch = ant.stream().filter(s -> !maven.contains(s)).sorted().collect(Collectors.toList());

        List<String> depList = dependencies.stream().filter(s -> !notInMavenArch.contains(s)).sorted().collect(Collectors.toList());
        notInMavenArch.addAll(depList);

        if (!notInMavenArch.isEmpty()) {

            for (String artifact : gavAnt) {
                for (String el : notInMavenArch) {

                    if (el.equals(artifact)) {
                        arr = antTree.get(gavAnt.indexOf(artifact)).split("#");
                        antArtifacts.put(gaAnt.get(gavAnt.indexOf(artifact)), new Artifact(arr[0], arr[1], arr[2], null, null));
                    }
                }
            }

        }

        deleteDuplicateDeps();
    }

    private void addElementsToPOM(Element rootElement) {

        Element element;
        Element exclusions;
        Element exclusion;

        for (String el : antArtifacts.keySet()) {
            element = new Element("dependency");
            element.addContent(new Element("groupId").setText(antArtifacts.get(el).getGroupId()));
            element.addContent(new Element("artifactId").setText(antArtifacts.get(el).getArtifactId()));
            element.addContent(new Element("version").setText(antArtifacts.get(el).getVersion()));
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

    private void deleteDuplicateDeps() throws JDOMException, IOException {

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
            for (String ant : antArtifacts.keySet()) {
                for (Artifact artifact : artifactElementMap.keySet()) {
                    if (ant.equals(artifact.getGroupId() + "-" + artifact.getArtifactId())) {

                        artifactElementMap.get(artifact).getContent(new ElementFilter("scope")).get(0).setText(MainRunner.DEFAULT_SCOPE);

                        if (antArtifacts.get(ant).getVersion().equalsIgnoreCase(artifact.getVersion())) {
                            removeList.add(ant);
                            break;
                        } else {
                            artifactElementMap.get(artifact).getContent(new ElementFilter("version")).get(0).setText(antArtifacts.get(ant).getVersion());
                            removeList.add(ant);
                            break;
                        }
                    }
                }
            }
        }

        if (!removeList.isEmpty()) {
            for (String el : removeList) {
                antArtifacts.remove(el);
            }
        }

        addElementsToPOM(rootElement);

        XmlUtils.outputDoc(documentFromFile, Paths.get(folder.toString(), shimName.toString(), MainRunner.POM_XML).toString());

        Path filePath = Paths.get(folder.toString(), shimName.toString(), MainRunner.POM_XML);
        Path mvnPath = Paths.get(folder.toString(), AssemblyGenerator.TEMP_MVN_SHIM_FOLDER, shimName.toString(), MainRunner.POM_XML);

        FileUtils.copyFileReplace(filePath, mvnPath);

        //moduleBashExecutor.runMvnCleanInstall();

    }

}
