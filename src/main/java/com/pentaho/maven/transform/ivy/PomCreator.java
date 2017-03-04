package com.pentaho.maven.transform.ivy;

import com.pentaho.maven.transform.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Aliaksandr_Zhuk on 12/29/2016.
 */
public class PomCreator {

    DependencyScope dep;
    Path path;


    public PomCreator(DependencyScope dep, Path path) {

        this.dep = dep;
        this.path = path;

    }


    private List<Dependency> createNonTransitiveDeps(Scopes scope) {

        List<ComplexArtifact> list;

        List<Dependency> dependencyList = new ArrayList<>();
        list = dep.getTransitiveDepsByScope(false, scope);

        for (ComplexArtifact complexArtifact : list) {

            Dependency dependency = new Dependency();
            dependency.setGroupId(complexArtifact.getArtifact().getGroupId());
            dependency.setArtifactId(complexArtifact.getArtifact().getArtifactId());
            dependency.setVersion(complexArtifact.getArtifact().getVersion());

            /*Element element = new Element("dependency");
            element.addContent(new Element("groupId").setText(complexArtifact.getArtifact().getGroupId()));
            element.addContent(new Element("artifactId").setText(complexArtifact.getArtifact().getArtifactId()));
            element.addContent(new Element("version").setText(complexArtifact.getArtifact().getVersion()));*/
            // element.addContent(new Element("scope").setText(complexArtifact.getArtifact().getScope()));

            if (complexArtifact.getArtifact().getClassifier() != null) {


                dependency.setClassifier(complexArtifact.getArtifact().getClassifier());
                //element.addContent(new Element("classifier").setText(complexArtifact.getArtifact().getClassifier()));

            }

            //elementList.add(element);

            dependencyList.add(dependency);

        }

        return dependencyList;


    }

    private List<Dependency> createTransitiveDeps(Scopes scope) {

        List<ComplexArtifact> list;

        List<Dependency> dependencyList = new ArrayList<>();

        List<Exclusion> exclusionList = new ArrayList<>();

        List<Artifact> excludeList = new ArrayList<>();

        list = dep.getTransitiveDepsByScope(true, scope);

        for (ComplexArtifact complexArtifact : list) {


            Dependency dependency = new Dependency();
            dependency.setGroupId(complexArtifact.getArtifact().getGroupId());
            dependency.setArtifactId(complexArtifact.getArtifact().getArtifactId());
            dependency.setVersion(complexArtifact.getArtifact().getVersion());

        /*    Element element = new Element("dependency");
            element.addContent(new Element("groupId").setText(complexArtifact.getArtifact().getGroupId()));
            element.addContent(new Element("artifactId").setText(complexArtifact.getArtifact().getArtifactId()));
            element.addContent(new Element("version").setText(complexArtifact.getArtifact().getVersion()));*/
            //element.addContent(new Element("scope").setText(complexArtifact.getArtifact().getScope()));

            if (complexArtifact.getArtifact().getClassifier() != null) {

                //element.addContent(new Element("classifier").setText(complexArtifact.getArtifact().getClassifier()));

                dependency.setClassifier(complexArtifact.getArtifact().getClassifier());
            }

            // Element excludesElement = new Element("excludes");


            excludeList = complexArtifact.getComplexArtifact().get(complexArtifact.getArtifact());

            if (excludeList != null) {

                for (Artifact artifact : excludeList) {

                    //  Element excludeElement = new Element("exclude");

                    Exclusion exclusion = new Exclusion();

                    if (artifact.getGroupId() != null) {
                        exclusion.setGroupId(artifact.getGroupId());
                        // excludeElement.addContent(new Element("groupId").setText(artifact.getGroupId()));
                    } else {
                        exclusion.setGroupId("*");
                        //  excludeElement.addContent(new Element("groupId").setText("*"));
                    }


                    if (artifact.getArtifactId() != null) {
                        exclusion.setArtifactId(artifact.getArtifactId());
                        //excludeElement.addContent(new Element("artifactId").setText(artifact.getArtifactId()));
                    } else {
                        exclusion.setArtifactId("*");
                        // excludeElement.addContent(new Element("artifactId").setText("*"));
                    }

                    //excludesElement.addContent(excludeElement);

                    exclusionList.add(exclusion);
                }

                dependency.setExclusions(exclusionList);
            }

            //  element.addContent(excludesElement);


            // elementList.add(element);

            dependencyList.add(dependency);

        }

        return dependencyList;

    }

    public Map<Boolean, List<Dependency>> createDependencySection(Scopes scope) {

        Map<Boolean, List<Dependency>> map = new HashMap<>();

        map.put(false, createNonTransitiveDeps(scope));
        map.put(true, createTransitiveDeps(scope));

        return map;

    }


    public void createPom(Scopes scope, SubFolder subFolder) throws FileNotFoundException, UnsupportedEncodingException, IOException {

        Map<Boolean, List<Dependency>> map = new HashMap<>();
        List<Dependency> dependencyList = new ArrayList<>();

        //map = createDependencySection(scope);

        String shimName = subFolder.name().toLowerCase();
        Path path = Paths.get(this.path.toString(), IvyRunner.TEMP_MVN_SHIM_FOLDER, shimName, IvyRunner.POM_XML);

        map = createDependencySection(scope);

        for (List<Dependency> list : map.values()) {

            dependencyList.addAll(list);



/*
            for (Dependency dependency : list) {


            }*/

        }

        if (!dependencyList.isEmpty()) {

            Model model = new Model();
            model.setGroupId("pentaho");
            model.setArtifactId("pentaho-hadoop-shims-" + shimName + "-scope-" + scope.toString().toLowerCase());
            model.setVersion("7.1-SNAPSHOT");
            model.setPackaging("jar");
            model.setModelVersion("4.0.0");
            model.setDependencies(dependencyList);

            File file = new File(path.toString());
            OutputStream stream = new FileOutputStream(file);
            Writer writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));

            new MavenXpp3Writer().write(writer, model);
        }

    }

    public void createPom(Scopes scope, SubFolder subFolder, Object o) throws FileNotFoundException, UnsupportedEncodingException, IOException {

        Map<Boolean, List<Dependency>> map = new HashMap<>();
        List<Dependency> dependencyList = new ArrayList<>();

        Path path = Paths.get(this.path.toString(), IvyRunner.TEMP_MVN_SHIM_FOLDER, subFolder.name().toLowerCase(), IvyRunner.POM_XML);

        map = createDependencySection(scope);

        for (List<Dependency> list : map.values()) {

            dependencyList.addAll(list);

        }

        Model model = new Model();
        model.setGroupId("pentaho");
        model.setArtifactId("pentaho-hadoop-shims-" + scope.toString().toLowerCase());
        model.setVersion("7.1-SNAPSHOT");
        model.setPackaging("jar");
        model.setModelVersion("4.0.0");
        model.setDependencies(dependencyList);

        File file = new File(path.toString());
        OutputStream stream = new FileOutputStream(file);
        Writer writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));

        new MavenXpp3Writer().write(writer, model);

    }


    public void injectGlobalExcludes() {


    }

}
