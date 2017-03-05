package com.pentaho.maven.transform.ivy;

import com.pentaho.maven.transform.xml.dom.DomManipulator;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Aliaksandr_Zhuk on 12/25/2016.
 */
public class IvyRunner {

    public static final String IVY_XML = "ivy.xml";
    public static final String POM_XML = "pom.xml";
    public static final String TEMP_MVN_SHIM_FOLDER = "temp";
    public static final String ASSEMBLY_XML = "assembly.xml";
    public static final String BUILD_PROPERTIES = "build.properties";
    public static final String TEMP_MAVEN_TREE_TXT = "temptree.txt";

    public static void main(String[] args) throws JDOMException, IOException {

        String shimName = "hdp25";

        new IvyRunner().generatePomsAssembliesForScopes(Paths.get(args[0], shimName));

    }

    public void generatePomsAssembliesForScopes(Path shimPath) throws JDOMException, IOException {
        String shimName = shimPath.getFileName().toString();
        Map<SubTagType, List<Element>> map;
        Map<Scopes, Map<Boolean, List<ComplexArtifact>>> scopesMap = new HashMap<>();
        Map<Scopes, Map<Boolean, List<ComplexArtifact>>> excludesMap = new HashMap<>();

        DomManipulator manipulator = new DomManipulator();
        Path ivyPath = Paths.get(shimPath.toString(), IVY_XML);
        manipulator.getRootElement(ivyPath);
        Path propertyPath = Paths.get(shimPath.toString(), BUILD_PROPERTIES);
        IvyParser parser = new IvyParser(manipulator);

        map = parser.getSubElements();

        parser.getVersionValues(propertyPath);

        scopesMap = parser.splitElementsByScope(map.get(SubTagType.DEPENDENCY));
        excludesMap = parser.splitExcludeElementsByScope(map.get(SubTagType.EXCLUDE));

        DependencyScope dep = new DependencyScope(scopesMap, excludesMap, shimPath);
        dep.createTempFolder();

        PomCreator pom = new PomCreator(dep, shimPath);

        pom.createPom(Scopes.DEFAULT, SubFolder.DEFAULT);
        pom.createPom(Scopes.CLIENT, SubFolder.CLIENT);
        pom.createPom(Scopes.PMR, SubFolder.PMR);
        pom.createPom(Scopes.TEST, SubFolder.TEST);
        pom.createPom(Scopes.PROVIDED, SubFolder.PROVIDED);

        AssemblyCreator assembly = new AssemblyCreator(dep, shimPath);

        assembly.createAssembly(Scopes.DEFAULT, SubFolder.DEFAULT, shimName);
        assembly.createAssembly(Scopes.CLIENT, SubFolder.CLIENT, shimName);
        assembly.createAssembly(Scopes.PMR, SubFolder.PMR, shimName);
    }
}
