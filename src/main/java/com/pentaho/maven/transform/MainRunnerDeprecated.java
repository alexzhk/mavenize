package com.pentaho.maven.transform;

import com.pentaho.maven.transform.xml.XmlUtils;
import com.pentaho.maven.transform.xml.condition.ElementWithChildValueInParentPathCondition;
import com.pentaho.maven.transform.xml.insert.ToParentPathInserter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import static com.pentaho.maven.transform.MainRunner.*;

/**
 * Created by Vasilina_Terehova on 1/8/2017.
 */
@Deprecated
public class MainRunnerDeprecated {

    private final MainRunner mainRunner;

    public MainRunnerDeprecated(MainRunner mainRunner) {
        this.mainRunner = mainRunner;
    }

    private void addResourceFolders(Path modulePath, ShimProperties shimProperties) throws JDOMException, IOException {
        String pomPath = Paths.get(modulePath.toString(), MainRunner.POM_XML).toString();
        Document documentFromFile = XmlUtils.getDocumentFromFile(pomPath);
        Element rootElement = documentFromFile.getRootElement();
        Element buildElement = documentFromFile.getRootElement().getContent(new ElementFilter("build")).get(0);
        Optional<Element> first = buildElement.getContent(new ElementFilter("sourceDirectory")).stream().findFirst();
        if (!first.isPresent()) {
            Element sourceDirectory = new Element("sourceDirectory");
            sourceDirectory.setText("../");
            sourceDirectory.setNamespace(rootElement.getNamespace());
            buildElement.addContent(sourceDirectory);
            String[] parentNodes = {"build"};
            XmlUtils.addElement(
                    "<resources>\n" +
                            "      <resource>\n" +
                            "        <directory>src/main/resources</directory>\n" +
                            "      </resource>\n" +
                            "      <resource>\n" +
                            "        <targetPath>${basedir}/target/classes/META-INF</targetPath>\n" +
                            "        <directory>../common/src-hbase-" + MainRunner.getHbaseGenerationVersion(shimProperties) + "/META-INF</directory>\n" +
                            "      </resource>\n" +
                            "      <resource>\n" +
                            "        <targetPath>${basedir}/target/classes/META-INF</targetPath>\n" +
                            "        <directory>../common/src-hbase-shim-" + shimProperties.getHbaseVersion() + "/META-INF</directory>\n" +
                            "      </resource>\n" +
                            "      <resource>\n" +
                            "        <targetPath>${basedir}/target/classes/META-INF</targetPath>\n" +
                            "        <directory>../common/src-mapred/META-INF</directory>\n" +
                            "      </resource>\n" +
                            "      <resource>\n" +
                            "        <targetPath>${basedir}/target/classes/META-INF</targetPath>\n" +
                            "        <directory>../common/src-modern/META-INF</directory>\n" +
                            "      </resource>\n" +
                            "      <resource>\n" +
                            "        <directory>../common/src</directory>\n" +
                            "        <includes>\n" +
                            "          <include>**/*.properties</include>\n" +
                            "        </includes>\n" +
                            "      </resource>\n" +
                            "      <resource>\n" +
                            "        <directory>../common/src-mapred</directory>\n" +
                            "        <includes>\n" +
                            "          <include>**/*.properties</include>\n" +
                            "        </includes>\n" +
                            "      </resource>\n" +
                            "    </resources>", rootElement, new ElementWithChildValueInParentPathCondition(parentNodes), new ToParentPathInserter(parentNodes), "");
            XmlUtils.outputDoc(documentFromFile, pomPath);
        }
    }

    private void runScriptAssemblyGenerating(Path modulePath) throws IOException {
        Path shimName = modulePath.getFileName();
        //rootFolderExecutor.executeCommand("\"C:\\Program Files\\Git\\git-bash.exe\" .\\main.sh " + shimName);
    }

    private void addCommonSourceFolderInclusions(Path folder, ShimProperties shimProperties) throws JDOMException, IOException {
        String pomPath = Paths.get(folder.toString(), "pom.xml").toString();
        String[] parentNodes = {"build", "plugins"};
        XmlUtils.addElementToDocumentFile(pomPath,
                " <plugin>" +
                        "          <groupId>org.apache.maven.plugins</groupId>" +
                        "          <artifactId>maven-compiler-plugin</artifactId>" +
                        "          <configuration>" +
                        "<source>1.8</source>\n" +
                        "            <target>1.8</target>" +
                        "               <includes>" +
                        "                   <include>common/src/**/*.java</include>\n" +
                        "                   <include>common/src-mapred/**/*.java</include>\n" +
                        "                   <include>common/src-modern/**/*.java</include>\n" +
                        "                   <include>common/src-hadoop-shim-" + shimProperties.getHadoopVersion() + "/**/*.java</include>" +
                        "                   <include>common/src-hbase-" + MainRunner.getHbaseGenerationVersion(shimProperties) + "/**/*.java</include>\n" +
                        "                   <include>common/src-hbase-shim-" + shimProperties.getHbaseVersion() + "/**/*.java</include>\n" +
                        "                   <include>common/src-pig-shim-" + shimProperties.getPigVersion() + "/**/*.java</include>\n" +
                        "                   <include>" + folder.getFileName() + "/src/main/java/**/*.java</include>" +
                        "               </includes>\n" +
                        "          </configuration>" +
                        "</plugin>", new ElementWithChildValueInParentPathCondition(parentNodes, "artifactId"), new ToParentPathInserter(parentNodes), "");
    }

    private void addCommonTestSourceFolderInclusions(Path folder, ShimProperties shimProperties) throws JDOMException, IOException {
        String pomPath = Paths.get(folder.toString(), "pom.xml").toString();
        String[] parentNodes = {"build", "plugins"};
        XmlUtils.addElementToDocumentFile(pomPath,
                " <plugin>" +
                        "          <groupId>org.apache.maven.plugins</groupId>" +
                        "          <artifactId>maven-surefire-plugin</artifactId>" +
                        "          <configuration>" +
                        "               <includes>" +
                        "                   <include>common/test-src/**/*.java</include>\n" +
                        "                   <include>common/test-src-modern/**/*.java</include>\n" +
                        "                   <include>common/test-src-hadoop-shim-" + shimProperties.getHadoopVersion() + "/**/*.java</include>" +
                        "                   <include>common/test-src-hbase-shim-" + shimProperties.getHbaseVersion() + "/**/*.java</include>\n" +
                        "                   <include>common/test-src-pig-shim-" + shimProperties.getPigVersion() + "/**/*.java</include>\n" +
                        "               </includes>\n" +
                        "          </configuration>" +
                        "</plugin>", new ElementWithChildValueInParentPathCondition(parentNodes, "artifactId"), new ToParentPathInserter(parentNodes), "");
    }

    private void updateDependenciesPropertiesAll(Path shimPath) throws IOException, URISyntaxException, JDOMException, ShimCannotBeProcessed {
        Path fixedPomsAssemblies = Paths.get("D:\\1\\h-s1\\");
        String shimName = shimPath.getFileName().toString();
        Path placeWithFixedPomAssembly = Paths.get(fixedPomsAssemblies.toString(), shimName);
        //replace assembly
        Path newImplArtifactPath = Paths.get(shimPath.toString(), IMPL_ARTIFACT_DIRECTORY);
        Path assembliesDirectory = Paths.get(shimPath.toString(), ASSEMBLIES_ARTIFACT_DIRECTORY);
        Path newAssemblyPath = Paths.get(assembliesDirectory.toString(), shimName + "-shim");

        mainRunner.moveGenerateAssembly(placeWithFixedPomAssembly, newAssemblyPath, true);
        //replace dependencies in pom

        String implArtifactFile = Paths.get(newImplArtifactPath.toString(), POM_XML).toString();
        Document documentFromFile = XmlUtils.getDocumentFromFile(implArtifactFile);
        Element dependencies = documentFromFile.getRootElement().getContent(new ElementFilter("dependencies")).get(0);
        int dependencyIndex = dependencies.getParent().indexOf(dependencies);
        dependencies.detach();

        Element properties = documentFromFile.getRootElement().getContent(new ElementFilter("properties")).get(0);
        int propertiesIndex = properties.getParent().indexOf(properties);
        properties.detach();

        String fixedDependenciesPom = Paths.get(placeWithFixedPomAssembly.toString(), POM_XML).toString();
        Document fixedDependenciesPomDocument = XmlUtils.getDocumentFromFile(fixedDependenciesPom);

        Element dependencies1 = fixedDependenciesPomDocument.getRootElement().getContent(new ElementFilter("dependencies")).get(0);
        dependencies1.detach();
        documentFromFile.getRootElement().addContent(dependencyIndex, dependencies1);

        Element properties1 = fixedDependenciesPomDocument.getRootElement().getContent(new ElementFilter("properties")).get(0);
        properties1.detach();
        documentFromFile.getRootElement().addContent(propertiesIndex, properties1);

        Path antShimFolder = Paths.get("D:\\1\\BAD-570_2\\phs\\", shimName);

        //Element dependenciesNew = documentFromFile.getRootElement().getContent(new ElementFilter("dependencies")).get(0);
        mainRunner.addCommonDependencies(antShimFolder);

        XmlUtils.outputDoc(documentFromFile, Paths.get(newImplArtifactPath.toString(), "pom.xml").toString());
    }

    public void updateGroupId(Element dependenciesElement) {
        for (Element element : dependenciesElement.getChildren()) {
            String classifier = XmlUtils.getTagValue(element, "classifier");
            String groupId1 = XmlUtils.getTagValue(element, "groupId");
            String scope = XmlUtils.getTagValue(element, "scope");
            String version = XmlUtils.getTagValue(element, "version");
            Artifact artifact = new Artifact(groupId1, XmlUtils.getTagValue(element, "artifactId"), version, classifier, scope);
            if (Objects.equals(artifact.getGroupId(), "org.pentaho") && !Objects.equals(artifact.getArtifactId(), "pentaho-hadoop-shims-api")) {
                Element groupIdElement = element.getContent(new ElementFilter("groupId")).get(0);
                System.out.println("found " + artifact);
                groupIdElement.setText("pentaho");
            }
        }
    }

    public void removeOldSources(Element buildElement) {
        List<Element> sourceDirectoryElementList = buildElement.getContent(new ElementFilter("sourceDirectory"));
        if (sourceDirectoryElementList.size() > 0) {
            buildElement.removeContent(sourceDirectoryElementList.get(0));
        }
        List<Element> resourceElementList = buildElement.getContent(new ElementFilter("resources"));
        if (resourceElementList.size() > 0) {
            buildElement.removeContent(resourceElementList.get(0));
        }
    }

    public void removeOldPluginManagement(String shimName, Element dependenciesElement, String implArtifactFile, Element pluginsElement) throws JDOMException, IOException, URISyntaxException {
        Path antShimFolder = Paths.get("D:\\1\\BAD-570_2\\phs\\", shimName);
        mainRunner.addCommonDependencies(antShimFolder);

        XmlUtils.deleteElement(implArtifactFile, new ElementWithChildValueInParentPathCondition(new String[] {"build", "plugins"}, "artifactId"), " <plugin>" +
                "          <groupId>org.apache.maven.plugins</groupId>" +
                "          <artifactId>maven-compiler-plugin</artifactId></plugin>");


    }

    private void removeAntFiles(Path modulePath, BashExecutor moduleBashExecutor) throws IOException {
        Path buildXmlFile = Paths.get(modulePath.toString(), BUILD_XML);
//        FileUtils.removeFile(buildXmlFile);
        //moduleBashExecutor.gitRemove(buildXmlFile);
        Path ivyXmlFile = Paths.get(modulePath.toString(), IVY_XML);
        FileUtils.removeFile(ivyXmlFile);
        moduleBashExecutor.gitRemove(ivyXmlFile);
        Path buildPropertiesFile = Paths.get(modulePath.toString(), BUILD_PROPERTIES);
        FileUtils.removeFile(buildPropertiesFile);
        moduleBashExecutor.gitRemove(buildPropertiesFile);
        Path ivySettingsXml = Paths.get(modulePath.toString(), IVY_SETTINGS_XML);
        FileUtils.removeFile(ivySettingsXml);
        moduleBashExecutor.gitRemove(ivySettingsXml);
        Path packageIvyXml = Paths.get(modulePath.toString(), PACKAGE_IVY_XML);
        FileUtils.removeFile(packageIvyXml);
        moduleBashExecutor.gitRemove(packageIvyXml);
        Path packagePomXml = Paths.get(modulePath.toString(), PACKAGE_POM_XML);
        FileUtils.removeFile(packagePomXml);
        moduleBashExecutor.gitRemove(packagePomXml);
    }

}
