package com.pentaho.maven.transform;

import com.pentaho.maven.transform.xml.XmlUtils;
import com.pentaho.maven.transform.xml.condition.ElementExistCondition;
import com.pentaho.maven.transform.xml.insert.AfterChildInserter;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ContentFilter;
import org.jdom2.filter.ElementFilter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by Vasilina_Terehova on 12/7/2016.
 */
public class VersionGenerator {

    private static String[] excludedGroupIds = new String[]{"org.pentaho", "pentaho", "pentaho-kettle"};
    private static String[] excludedArtifactsIds = new String[]{"hamcrest-core", "mockito-all", "junit", "jug-lgpl", "metrics-core", "commons-lang", "pentaho-hadoop-shims-common-pig-shim-1.1", "pentaho-hadoop-shims-common-pig-shim-1.0"};
    private static String[] makeArtifactNameOfPropertyIds = new String[]{"hadoop2-windows-patch"};
    private static String[] settingProjectVersion = new String[]{"pentaho-hadoop-shims-common-pig-shim-1.1", "pentaho-hadoop-shims-common-pig-shim-1.0"};

    public static final String IVY_XML = "ivy.xml";

    //<version -> < groupId -> List<Artifact> >
    //Artifact -> Dependency
    public static void generatePropertyVersionSection(Path folder) throws JDOMException, IOException {
        String pomPath = Paths.get(folder.toString(), MainRunner.POM_XML).toString();
        Document documentFromFile = XmlUtils.getDocumentFromFile(pomPath);
        Element rootElement = documentFromFile.getRootElement();
        List<String> excludedGroupIdList = Arrays.asList(excludedGroupIds);
        List<String> excludedArtifactList = Arrays.asList(excludedArtifactsIds);
        List<String> makeArtifactNameOfPropertyIdList = Arrays.asList(makeArtifactNameOfPropertyIds);
        List<String> settingProjectVersionList = Arrays.asList(settingProjectVersion);
        Optional<Element> properties = rootElement.getContent(new ElementFilter("properties")).stream().findFirst();
        if (!properties.isPresent()) {
            List<Element> dependencyList = rootElement.getContent(new ElementFilter("dependencies")).get(0).getChildren();
            System.out.println();
            Map<String, Map<String, List<Artifact>>> versions = new HashMap<>();
            Map<Artifact, Element> artifactElementMap = new HashMap<>();
            dependencyList.stream().forEach(element -> {
                String version = XmlUtils.getTagValue(element, "version");
                Map<String, List<Artifact>> map = versions.get(version) == null ? new HashMap<>() : versions.get(version);
                String classifier = XmlUtils.getTagValue(element, "classifier");
                String groupId1 = XmlUtils.getTagValue(element, "groupId");
                String scope = XmlUtils.getTagValue(element, "scope");
                Artifact artifact = new Artifact(groupId1, XmlUtils.getTagValue(element, "artifactId"), version, classifier, scope);
                if (artifact.getArtifactId().equals("pentaho-hadoop-shims-api") || artifact.getArtifactId().equals("pentaho-hadoop-shims-api-test")) {
                    element.getContent(new ElementFilter("groupId")).get(0).setText("org.pentaho");
                }
                if (artifact.getArtifactId().equals("pentaho-hadoop-shims-api-test")) {
                    element.getContent(new ElementFilter("artifactId")).get(0).setText("pentaho-hadoop-shims-api");
                    Element classifier1 = new Element("classifier", "http://maven.apache.org/POM/4.0.0");
                    classifier1.setText("tests");
                    element.addContent(classifier1);
                }
                if (version.startsWith("${")) {
                    return;
                }

                if (makeArtifactNameOfPropertyIdList.contains(artifact.getArtifactId()) || settingProjectVersionList.contains(artifact.getArtifactId())
                        || (!excludedGroupIdList.contains(groupId1) && !excludedArtifactList.contains(artifact.getArtifactId()))) {
                    artifactElementMap.put(artifact, element);
                    boolean groupFound = false;
                    if (map.isEmpty()) {
                        ArrayList<Artifact> value = new ArrayList<>();
                        value.add(artifact);
                        map.put(groupId1, value);
                    } else {
                        if (!map.isEmpty()) {
                            for (String groupId : map.keySet()) {
                                if (map.get(groupId).get(0).getGroupId().equalsIgnoreCase(groupId1)) {
                                    groupFound = true;
                                    map.get(groupId).add(artifact);
                                }
                            }
                            if (!groupFound) {
                                ArrayList<Artifact> value = new ArrayList<>();
                                value.add(artifact);
                                map.put(groupId1, value);
                            }
                        }
                    }
                    versions.put(version, map);
                } else {
                    List<Element> scopeElements = element.getContent(new ElementFilter("scope"));
                    if (scopeElements.size() > 0) {
                        element.removeContent(scopeElements.get(0));
                    }
                    List<Element> exclusionElements = element.getContent(new ElementFilter("exclusions"));
                    if (exclusionElements.size() > 0) {
                        element.removeContent(exclusionElements.get(0));
                    }
                    element.removeContent(new ElementFilter("version")).get(0);
                }
            });


            StringBuffer propertiesBuffer = new StringBuffer("<properties>\n");
            Set<String> artifactIds = new HashSet<>();
            for (Map.Entry<String, Map<String, List<Artifact>>> entries : versions.entrySet()) {
                String versionValue = entries.getKey();
                Map<String, List<Artifact>> value = entries.getValue();
                for (Map.Entry<String, List<Artifact>> groupsMap : value.entrySet()) {
                    String artifactId = groupsMap.getValue().get(0).getArtifactId();
                    String groupId = groupsMap.getValue().get(0).getGroupId();

                    String shortVersionString = artifactId + ".version";

                    if (artifactIds.contains(artifactId) || groupsMap.getValue().size() > 1) {
                        shortVersionString = groupId + ".version";
                    }

                    String versionString = "${" + shortVersionString + "}";

                    if(!settingProjectVersionList.contains(artifactId)) {
                        propertiesBuffer.append("<").append(shortVersionString).append(">").append(versionValue).append("</").append(shortVersionString).append(">\r\n");
                    }else{

                        versionString = "${project.version}";
                    }
                    for (Artifact artifact : groupsMap.getValue()) {
                        artifactElementMap.get(artifact).getContent(new ElementFilter("version")).get(0).setText(versionString);
                    }
                    if (!artifactIds.contains(artifactId)) {
                        artifactIds.add(artifactId);
                    } else {
                        System.out.println("AHTUNG!!!! ERROR!!!! 2 group ids found");
                        throw new IllegalArgumentException("2 equal group ids found");
                    }
                }
            }
            propertiesBuffer.append("</properties>");
            System.out.println(propertiesBuffer.toString());
            //Element versionElement = rootElement.getContent(new ElementFilter("version")).stream().findFirst().get();
            XmlUtils.addElement(propertiesBuffer.toString(), rootElement, new ElementExistCondition(), new AfterChildInserter("version"), "");
            XmlUtils.updateNameSpaceParent(rootElement, "properties");
            XmlUtils.outputDoc(documentFromFile, Paths.get(folder.toString(), MainRunner.POM_XML).toString());
        }
    }
}
