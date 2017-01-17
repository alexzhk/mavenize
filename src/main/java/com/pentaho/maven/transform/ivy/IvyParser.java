package com.pentaho.maven.transform.ivy;

import com.pentaho.maven.transform.xml.dom.DomManipulator;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by Aliaksandr_Zhuk on 12/25/2016.
 */
public class IvyParser {

    private DomManipulator manipulator;
    Map<String, String> versionMap = new HashMap<>();

    public IvyParser(DomManipulator manipulator) {

        this.manipulator = manipulator;

    }

    public void getVersionValues(Path propertyPath) {

        Properties properties = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream(propertyPath.toString());
            properties.load(input);
            for (String key : properties.stringPropertyNames()) {
                String keyValue = "${" + key + "}";
                versionMap.put(keyValue, properties.getProperty(key));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    public Map<SubTagType, List<Element>> getSubElements() {

        Map<SubTagType, List<Element>> map = new HashMap<>();
        String tag = TagType.DEPENDENCIES.name().toLowerCase();

        for (SubTagType subTag : SubTagType.values()) {
            map.put(subTag, manipulator.getDependencies(tag, subTag.toString().toLowerCase()));
        }

        return map;

    }

    public void replaceVersionWithValue(List<Element> dependencyList) {
        String version;
        for (Element element : dependencyList) {
            version = element.getAttributeValue("rev");
            if (versionMap.containsKey(version)) {
                element.setAttribute("rev", versionMap.get(version));
            }
        }
    }

    private List<String> splitString(String[] arr, String delimiter) {

        String scope;
        List<String> list = new ArrayList<>();

        for (String s : arr) {
            scope = s.split(delimiter)[0];
            list.add(scope);
        }

        return list;
    }


    public void replaceScopeWithValue(List<Element> dependencyList) {
        String scope;
        String[] arr;

        List<String> list;
        Element el;
        List<Element> elementList = new ArrayList<>();

        for (Element element : dependencyList) {
            scope = element.getAttributeValue("conf");

            if (scope != null) {

                arr = scope.split(";");
                list = splitString(arr, "-");

                String str;

                for (int i = 0; i < list.size(); i++) {
                    str = list.get(i);

                    for (Scopes s : Scopes.values()) {
                        if (s.name().toLowerCase().equals(str)) {

                            if (i == 0) {
                                element.setAttribute("conf", str);
                            } else {
                                el = element.clone();
                                el.setAttribute("conf", str);
                                elementList.add(el);
                            }
                            break;
                        }
                    }

                }
            } else {
                element.setAttribute("conf", Scopes.DEFAULT.name().toLowerCase());
            }
        }

        dependencyList.addAll(elementList);
    }

  /*  public void replaceScopeWithValue(List<Element> dependencyList) {
        String scope;
        for (Element element : dependencyList) {
            scope = element.getAttributeValue("conf");

            if (scope != null) {
                scope = scope.split("-")[0];
                for (Scopes s : Scopes.values()) {
                    if (s.name().toLowerCase().equals(scope)) {
                        element.setAttribute("conf", scope);
                    }
                }
            } else {
                element.setAttribute("conf", Scopes.DEFAULT.name().toLowerCase());
            }
        }
    }*/


    public String getClassifier(Element element) {

        String classifier = null;
        Element artifact;

        artifact = element.getChild("artifact");

        if (artifact != null) {
            classifier = artifact.getAttributeValue("classifier", Namespace.getNamespace("m", "http://ant.apache.org/ivy/maven"));
        }

        return classifier;
    }


    private Map<Scopes, List<ComplexArtifact>> splitByScopes(List<ComplexArtifact> list) {

        String scope;

        List<ComplexArtifact> defScope = new ArrayList<>();
        List<ComplexArtifact> clientScope = new ArrayList<>();
        List<ComplexArtifact> pmrScope = new ArrayList<>();
        List<ComplexArtifact> testScope = new ArrayList<>();

        Map<Scopes, List<ComplexArtifact>> map = new HashMap<>();

        for (ComplexArtifact artifact : list) {

            scope = artifact.getArtifact().getScope();

            if (scope.equals(Scopes.DEFAULT.name().toLowerCase())) {
                defScope.add(artifact);
            } else if (scope.equals(Scopes.CLIENT.name().toLowerCase())) {
                clientScope.add(artifact);
            } else if (scope.equals(Scopes.PMR.name().toLowerCase())) {
                pmrScope.add(artifact);
            } else if (scope.equals(Scopes.TEST.name().toLowerCase())) {
                testScope.add(artifact);
            }
        }

        map.put(Scopes.DEFAULT, defScope);
        map.put(Scopes.CLIENT, clientScope);
        map.put(Scopes.PMR, pmrScope);
        map.put(Scopes.TEST, testScope);

        return map;
    }


    private Map<Boolean, List<ComplexArtifact>> groupScopeByTransitive(Map<Scopes, List<ComplexArtifact>> mapTrue, Map<Scopes, List<ComplexArtifact>> mapFalse, Scopes scope) {

        Map<Boolean, List<ComplexArtifact>> map = new HashMap<>();

        map.put(true, mapTrue.get(scope));
        map.put(false, mapFalse.get(scope));

        return map;
    }


    public Map<Scopes, Map<Boolean, List<ComplexArtifact>>> splitElementsByScope(List<Element> dependencyList) {

        replaceVersionWithValue(dependencyList);
        replaceScopeWithValue(dependencyList);

        List<ComplexArtifact> transitiveTrue = new ArrayList<>();
        List<ComplexArtifact> transitiveFalse = new ArrayList<>();

        Map<Scopes, List<ComplexArtifact>> mapTrue;
        Map<Scopes, List<ComplexArtifact>> mapFalse;

        Map<Scopes, Map<Boolean, List<ComplexArtifact>>> mapResult = new HashMap<>();

        dependencyList.stream().forEach(element -> {

            ComplexArtifact complexArtifact = new ComplexArtifact(element);

            String transitive = element.getAttributeValue("transitive");

            if (transitive != null && transitive.equals("false")) {
                transitiveFalse.add(complexArtifact);
            } else {
                transitiveTrue.add(complexArtifact);
            }
        });

        mapTrue = splitByScopes(transitiveTrue);
        mapFalse = splitByScopes(transitiveFalse);

        mapResult.put(Scopes.DEFAULT, groupScopeByTransitive(mapTrue, mapFalse, Scopes.DEFAULT));
        mapResult.put(Scopes.CLIENT, groupScopeByTransitive(mapTrue, mapFalse, Scopes.CLIENT));
        mapResult.put(Scopes.PMR, groupScopeByTransitive(mapTrue, mapFalse, Scopes.PMR));
        mapResult.put(Scopes.TEST, groupScopeByTransitive(mapTrue, mapFalse, Scopes.TEST));

        return mapResult;

    }

   /* public Map<Scopes, Map<Boolean, ComplexArtifact>> splitElementsByScope(List<Element> dependencyList) {


        replaceVersionWithValue(dependencyList);
        replaceScopeWithValue(dependencyList);

        //Map<Boolean, ComplexArtifact> transitiveMap = new HashMap<>();
        Map<Scopes, Map<Boolean, ComplexArtifact>> artifactElementMap = new HashMap<>();

        List<ComplexArtifact> transitiveTrue = new ArrayList<>();
        List<ComplexArtifact> transitiveFalse = new ArrayList<>();

        List<ComplexArtifact> defScope = new ArrayList<>();
        List<ComplexArtifact> clientScope = new ArrayList<>();
        List<ComplexArtifact> pmrScope = new ArrayList<>();



        dependencyList.stream().forEach(element -> {
            *//*String version = element.getAttributeValue("rev");
            String groupId = element.getAttributeValue("org");
            String artifactId = element.getAttributeValue("name");
            String scope = element.getAttributeValue("conf");
            String classifier = getClassifier(element);*//*
            Map<Boolean, ComplexArtifact> transitiveMap; //= new HashMap<>();

            ComplexArtifact complexArtifact = new ComplexArtifact(element);

            String transitive = element.getAttributeValue("transitive");

            String scope = element.getAttributeValue("conf");

            //Artifact artifact = new Artifact(groupId, artifactId, version, classifier, scope);

            for (Scopes s : Scopes.values()) {
                if (s.name().toLowerCase().equals(scope)) {
                    transitiveMap = new HashMap<>();
                    if (transitive != null && transitive.equals("false")) {
                        transitiveMap.put(false, complexArtifact);
                    } else {
                        transitiveMap.put(true, complexArtifact);
                    }
                    artifactElementMap.put(s, transitiveMap);
                    break;
                }
            }

        });

        return artifactElementMap;
        //artifactElementMap.get(artifact).getContent(new ElementFilter("scope")).get(0).setText(MainRunner.DEFAULT_SCOPE);
    }*/

}
