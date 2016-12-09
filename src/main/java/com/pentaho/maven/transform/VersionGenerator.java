package com.pentaho.maven.transform;

import com.pentaho.maven.transform.xml.XmlUtils;
import com.pentaho.maven.transform.xml.condition.ElementExistCondition;
import com.pentaho.maven.transform.xml.insert.AfterChildInserter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by Vasilina_Terehova on 12/7/2016.
 */
public class VersionGenerator {

    public static final String POM_XML = "pom.xml";

    //<version -> < groupId -> List<Artifact> >
    //Artifact -> Dependency
    public static void generatePropertyVersionSection(Path folder) throws JDOMException, IOException {
        String pomPath = Paths.get(folder.toString(), POM_XML).toString();
        Document documentFromFile = XmlUtils.getDocumentFromFile(pomPath);
        Element rootElement = documentFromFile.getRootElement();
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
                Artifact artifact = new Artifact(groupId1, XmlUtils.getTagValue(element, "artifactId"), version, classifier);
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
            });


            StringBuffer propertiesBuffer = new StringBuffer("<properties>\n");
            Set<String> groupIds = new HashSet<>();
            for (Map.Entry<String, Map<String, List<Artifact>>> entries : versions.entrySet()) {
                String versionValue = entries.getKey();
                Map<String, List<Artifact>> value = entries.getValue();
                for (Map.Entry<String, List<Artifact>> groupsMap : value.entrySet()) {
                    String groupId = groupsMap.getValue().get(0).getGroupId();
                    int i =1;
                    while (groupIds.contains(groupId)) {
                        groupId = groupId + (i + 1);
                        System.out.println(groupId);
                    }
                    String shortVersionString = groupId + ".version";
                    String versionString = "${" + shortVersionString + "}";
                    propertiesBuffer.append("<").append(shortVersionString).append(">").append(versionValue).append("</").append(shortVersionString).append(">\r\n");
                    for (Artifact artifact : groupsMap.getValue()) {
                        artifactElementMap.get(artifact).getContent(new ElementFilter("version")).get(0).setText(versionString);
                    }
                    if (!groupIds.contains(groupId)) {
                        groupIds.add(groupId);
                    } else {
                        System.out.println("AHTUNG!!!! ERROR!!!! 2 group ids found");
                        throw new IllegalArgumentException("2 equal group ids found");
                    }
                }
            }
            propertiesBuffer.append("</properties>");
            System.out.println(propertiesBuffer.toString());
            //Element versionElement = rootElement.getContent(new ElementFilter("version")).stream().findFirst().get();
            XmlUtils.addElement(propertiesBuffer.toString(), rootElement, new ElementExistCondition(), new AfterChildInserter("artifactId"), "");
            XmlUtils.updateNameSpaceParent(rootElement, "properties");
            XmlUtils.outputDoc(documentFromFile, Paths.get(folder.toString(), POM_XML).toString());
        }
    }
}
