package com.pentaho.maven.transform.ivy;

import com.pentaho.maven.transform.Artifact;
import com.pentaho.maven.transform.xml.XmlUtils;
import org.jdom2.Document;
import org.jdom2.Element;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Aliaksandr_Zhuk on 12/29/2016.
 */
public class AssemblyCreator {

    DependencyScope dep;
    Path path;

    public AssemblyCreator(DependencyScope dep, Path path) {

        this.dep = dep;
        this.path = path;

    }

    private List<Element> putIncludes(boolean isTransitive, Scopes scope) {

        List<ComplexArtifact> list;

        List<Element> includeList = new ArrayList<>();

        if (isTransitive) {
            list = dep.getTransitiveDepsByScope(true, scope);
        } else {
            list = dep.getTransitiveDepsByScope(false, scope);
        }

        for (ComplexArtifact complexArtifact : list) {

            Element element = new Element("include").setText(complexArtifact.getArtifact().getGroupId() + ":" + complexArtifact.getArtifact().getArtifactId());
            includeList.add(element);
        }

        return includeList;

    }

   /* private List<List<Element>> putTransitiveIncludes(Scopes scope) {

        List<ComplexArtifact> list;

        List<Element> includeList = new ArrayList<>();

        List<Element> excludeElementList = new ArrayList<>();

        List<List<Element>> resultList = new ArrayList<>();

        list = dep.getTransitiveDepsByScope(true, scope);

        for (ComplexArtifact complexArtifact : list) {

            Element element = new Element("include").setText(complexArtifact.getArtifact().getGroupId() + ":" + complexArtifact.getArtifact().getArtifactId());

            includeList.add(element);

        }

        resultList.add(includeList);
        resultList.add(excludeElementList);

        return resultList;

    }*/


    private List<Element> putExcludes(Scopes scope) {

        List<ComplexArtifact> list;
        Artifact artifact;
        List<Element> excludeElementList = new ArrayList<>();

        list = dep.getExcludesByScope(scope);

        for (ComplexArtifact complexArtifact : list) {

            artifact = complexArtifact.getArtifact();

                    Element excludeElement = new Element("exclude");

                    if (artifact.getGroupId() != null && artifact.getArtifactId() != null) {
                        excludeElement.setText(artifact.getGroupId() + ":" + artifact.getArtifactId());
                    } else if (artifact.getGroupId() != null) {
                        excludeElement.setText(artifact.getGroupId() + ":*");
                    } else if (artifact.getArtifactId() != null) {
                        excludeElement.setText("*:" + artifact.getArtifactId() + ":*");
                    }

                    excludeElementList.add(excludeElement);
        }

        Element excludeElement = new Element("exclude");
        excludeElement.setText("*:tests:*");
        excludeElementList.add(excludeElement);

        return excludeElementList;
    }

    public Map<String, List<Element>> createIncludesSection(Scopes scope) {

        Map<String, List<Element>> map = new HashMap<>();

        map.put("false", putIncludes(false, scope));
        map.put("true", putIncludes(true, scope));
        map.put("exclude", putExcludes(scope));

        return map;

    }

    private Element createDependencySet(boolean isTransitive, List<Element> includeList, List<Element> excludeList, Scopes scope) {

        String dir = "";

        if (scope.name().toLowerCase().equals("default")) {
            dir = "lib";
        } else if (scope.name().toLowerCase().equals("client")) {
            dir = "lib/client";
        } else if (scope.name().toLowerCase().equals("pmr")) {
            dir = "lib/pmr";
        }

        Element dependencySet = new Element("dependencySet");
        dependencySet.addContent(new Element("outputDirectory").setText(dir));

        if (isTransitive) {
            dependencySet.addContent(new Element("useTransitiveDependencies").setText("true"));
            dependencySet.addContent(new Element("useTransitiveFiltering").setText("true"));
        } else {
            dependencySet.addContent(new Element("useTransitiveDependencies").setText("true"));
            dependencySet.addContent(new Element("useTransitiveFiltering").setText("false"));
        }


        Element includes = new Element("includes");

        includes.addContent(includeList);

        dependencySet.addContent(includes);

        if (excludeList != null) {
            Element excludes = new Element("excludes");

            excludes.addContent(excludeList);

            dependencySet.addContent(excludes);

        }

        return dependencySet;
    }

    public void createAssembly(Scopes scope, SubFolder subFolder, String shimName) throws IOException {

        Map<String, List<Element>> map;

        Element dependencySetFalse;
        Element dependencySetTrue;

        String path = Paths.get(this.path.toString(), IvyRunner.TEMP_MVN_SHIM_FOLDER, subFolder.name().toLowerCase(), IvyRunner.ASSEMBLY_XML).toString();

        map = createIncludesSection(scope);

        Element assembly = new Element("assembly");

        Document doc = new Document(assembly);

        assembly.addContent(new Element("id").setText("plugin"));
        assembly.addContent(new Element("baseDirectory").setText(shimName));

        Element formats = new Element("formats");
        formats.addContent(new Element("format").setText("zip"));

        assembly.addContent(formats);

        Element dependencySets = new Element("dependencySets");

        assembly.addContent(dependencySets);

        if (!map.get("false").isEmpty()) {

            dependencySetFalse = createDependencySet(false, map.get("false"), null, scope);
            dependencySets.addContent(dependencySetFalse);
        }

        if (!map.get("true").isEmpty()) {
            dependencySetTrue = createDependencySet(true, map.get("true"), map.get("exclude"), scope);
            dependencySets.addContent(dependencySetTrue);
        }

        if (!map.get("false").isEmpty() || !map.get("true").isEmpty()) {
            XmlUtils.outputDoc(doc, path);
        }

    }

}
