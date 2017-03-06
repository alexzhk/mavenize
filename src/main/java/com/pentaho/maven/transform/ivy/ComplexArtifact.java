package com.pentaho.maven.transform.ivy;

import com.pentaho.maven.transform.Artifact;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Aliaksandr_Zhuk on 12/26/2016.
 */
public class ComplexArtifact {

    Element element;
    Artifact artifact;
    Map<Artifact, List<Artifact>> map = new HashMap<>();


    public ComplexArtifact(Element element) {

        this.element = element;

        initArtifact();
        getChildrenArtifacts();

    }

    public Artifact getArtifact() {
        return artifact;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    public Map<Artifact, List<Artifact>> getComplexArtifact() {

        return map;

    }


    public String getClassifier(Element element) {

        String classifier = null;
        Element artifact;

        artifact = element.getChild("artifact");

        if (artifact != null) {
            classifier = artifact.getAttributeValue("classifier", Namespace.getNamespace("m", "http://ant.apache.org/ivy/maven"));
        }

        return classifier;
    }


    private void initArtifact() {

        String version = element.getAttributeValue("rev");
        String groupId = element.getAttributeValue("org");
        String artifactId = element.getAttributeValue("name");
        if (artifactId == null) {
            artifactId = element.getAttributeValue("module");
        }
        String scope = element.getAttributeValue("conf");
        String classifier = getClassifier(element);

        artifact = new Artifact(groupId, artifactId, version, classifier, scope);

    }

    private void getChildrenArtifacts() {
        List<Element> excludeList = new ArrayList<>();
        List<Artifact> artList = new ArrayList<>();
        excludeList = element.getChildren(SubTagType.EXCLUDE.name().toLowerCase());


        if (!excludeList.isEmpty()) {
            for (Element el : excludeList) {
                artList.add(new Artifact(el.getAttributeValue("org"), el.getAttributeValue("module"), null, null, null));
            }

            map.put(artifact, artList);
        }
    }

}
