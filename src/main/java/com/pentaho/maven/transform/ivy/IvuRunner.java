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
public class IvuRunner {

    public static final String IVY_XML = "ivy.xml";
    public static final String POM_XML = "pom.xml";
    public static final String TEMP_MVN_SHIM_FOLDER = "temp";
    public static final String ASSEMBLY_XML = "assembly.xml";
    public static final String BUILD_PROPERTIES = "build.properties";
    public static final String TEMP_MAVEN_TREE_TXT = "temptree.txt";

    public static void main(String[] args) throws JDOMException, IOException {

        String shimName = "mapr510";

        Map<SubTagType, List<Element>> map;
        Map<Scopes, Map<Boolean, List<ComplexArtifact>>> scopesMap = new HashMap<>();

        Path path = Paths.get(args[0]);
        Path ivyPath = Paths.get(args[0], IVY_XML);
        Path propertyPath = Paths.get(args[0], BUILD_PROPERTIES);

        DomManipulator manipulator = new DomManipulator();
        manipulator.getRootElement(ivyPath);

        IvyParser parser = new IvyParser(manipulator);

        map = parser.getSubElements();

        parser.getVersionValues(propertyPath);

        scopesMap = parser.splitElementsByScope(map.get(SubTagType.DEPENDENCY));

        DependencyScope dep = new DependencyScope(scopesMap, Paths.get(args[0]));
        dep.createTempFolder();

        PomCreator pom = new PomCreator(dep, path);

        pom.createPom(Scopes.DEFAULT, SubFolder.DEFAULT);
        pom.createPom(Scopes.CLIENT, SubFolder.CLIENT);
        pom.createPom(Scopes.PMR, SubFolder.PMR);
        pom.createPom(Scopes.TEST, SubFolder.TEST);

        AssemblyCreator assembly = new AssemblyCreator(dep, path);

        assembly.createAssembly(Scopes.DEFAULT, SubFolder.DEFAULT, shimName);
        assembly.createAssembly(Scopes.CLIENT, SubFolder.CLIENT, shimName);
        assembly.createAssembly(Scopes.PMR, SubFolder.PMR, shimName);

    }
}
