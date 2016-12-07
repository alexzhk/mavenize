package com.pentaho.maven.transform;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.ElementFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by Vasilina_Terehova on 12/5/2016.
 */
public class MainRunner {

    public static final String API_MODULE_FOLDER = "api";
    public static final String ASSEMBLY_XML = "assembly.xml";
    public static final String BUILD_XML = "build.xml";
    public static String[] sourceFolderArrayMaven = new String[]{"src/main/java", "src/main/resources", "src/test/java", "src/test/resources"};
    public static String[] shimsToProcess = new String[]{"cdh58"};
    public static String sourceJavaSubfolder = sourceFolderArrayMaven[0];
    public static String resourceJavaSubfolder = sourceFolderArrayMaven[1];
    public static String testJavaSubfolder = sourceFolderArrayMaven[2];
    public static String testResourceJavaSubfolder = sourceFolderArrayMaven[3];

    BashExecutor rootFolderExecutor;
    BashExecutor moduleBashExecutor;

    private static final Logger LOG = LoggerFactory.getLogger(MainRunner.class);

    String folder;

    public static boolean revert = false;

    public MainRunner(String folder) {
        this.folder = folder;
        rootFolderExecutor = new BashExecutor(Paths.get(folder));
    }

    public static void main(String[] args) throws IOException, JDOMException {
        if (args.length == 0) {
            throw new IllegalArgumentException("No folder specified");
        }

        MainRunner mainRunner = new MainRunner(args[0]);
        mainRunner.moveShimsToMaven();

        //new MainRunner(args[0]).executeCommand("ping pentaho.com");
    }

    public void moveShimsToMaven() throws IOException {
        List<String> shimList = Arrays.asList(shimsToProcess);
        try (Stream<Path> paths = Files.list(Paths.get(folder))) {
            paths.forEach(filePath -> {
                if (Files.isDirectory(filePath)) {
                    //we are in the directory
                    String shortFileName = filePath.getFileName().toString();
                    if (shortFileName.equals(API_MODULE_FOLDER)) {
                        LOG.debug("run for api");
                        try {
                            runForApiProject(filePath);
                        } catch (JDOMException | IOException e) {
                            LOG.error("Api module can't be processed " + e.getMessage());
                        }
                    } else if (shimList.contains(shortFileName)) {
                        //one of shim
                        try {
                            LOG.info("shim " + shortFileName + " started");
                            runForShim(filePath);
                            LOG.info("shim " + shortFileName + " finished successfully");
                        } catch (JDOMException | IOException | ShimCannotBeProcessed e) {
                            e.printStackTrace();
                            LOG.error("Shim can't be processed " + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    private void runForApiProject(Path modulePath) throws JDOMException, IOException {
        moduleBashExecutor = new BashExecutor(modulePath);
        addTransferGoalForAnt(modulePath.toString(), BUILD_XML);
        moveSourceFolder(modulePath);
        runTransferGoal(modulePath);
        VersionGenerator.generatePropertyVersionSection(modulePath);
        addParentSection(modulePath);
        removeTransferGoalTarget(modulePath);
    }

    private void runForShim(Path modulePath) throws JDOMException, IOException, ShimCannotBeProcessed {
        moduleBashExecutor = new BashExecutor(modulePath);

        addTransferGoalForAnt(modulePath.toString(), BUILD_XML);
        runTransferGoal(modulePath);

        addAssemblySectionForShim(modulePath);
        runScriptAssemblyGenerating(modulePath);

        moveSourceFolder(modulePath);
        ShimProperties shimProperties = new PropertyReader(modulePath).readShimProperties();
        addCommonSourceFolderInclusions(modulePath, shimProperties);
        addResourceFolders(modulePath, shimProperties);
        VersionGenerator.generatePropertyVersionSection(modulePath);
        addParentSection(modulePath);

        moveGenerateAssembly(modulePath);

        //cleanup
        removeTransferGoalTarget(modulePath);
    }

    private void addParentSection(Path modulePath) throws JDOMException, IOException {
        String pomPath = Paths.get(modulePath.toString(), VersionGenerator.POM_XML).toString();
        Document documentFromFile = XmlUtils.getDocumentFromFile(pomPath);
        Element rootElement = documentFromFile.getRootElement();
        XmlUtils.addElementToDocument(
                "<parent>\n" +
                        "    <groupId>pentaho</groupId>\n" +
                        "    <artifactId>pentaho-hadoop-shims-parent</artifactId>\n" +
                        "    <version>7.1-SNAPSHOT</version>\n" +
                        "  </parent>"
               , null, "modelVersion", rootElement, "");
        XmlUtils.outputDoc(documentFromFile, pomPath);
    }

    private void runScriptAssemblyGenerating(Path modulePath) throws IOException {
        Path shimName = modulePath.getFileName();
        rootFolderExecutor.executeCommand("\"C:\\Program Files\\Git\\git-bash.exe\" .\\main.sh " + shimName);
    }

    private void moveGenerateAssembly(Path modulePath) throws ShimCannotBeProcessed, IOException {
        Path fullAssemblyPath = Paths.get(modulePath.toString(), ASSEMBLY_XML);
        Path neededFullAssemblyPath = Paths.get(modulePath.toString(), "src/main/descriptor", ASSEMBLY_XML);
        if (!Files.exists(fullAssemblyPath)) {
            String msg = "no assembly generated by some reasons";
            LOG.error(msg);
           // throw new ShimCannotBeProcessed(msg);
        }
        FileUtils.moveFileReplace(fullAssemblyPath, neededFullAssemblyPath);
        rootFolderExecutor.gitAdd(neededFullAssemblyPath);
    }

    private void addResourceFolders(Path modulePath, ShimProperties shimProperties) throws JDOMException, IOException {
        String pomPath = Paths.get(modulePath.toString(), VersionGenerator.POM_XML).toString();
        Document documentFromFile = XmlUtils.getDocumentFromFile(pomPath);
        Element rootElement = documentFromFile.getRootElement();
        Element buildElement = documentFromFile.getRootElement().getContent(new ElementFilter("build")).get(0);
        Optional<Element> first = buildElement.getContent(new ElementFilter("sourceDirectory")).stream().findFirst();
        if (!first.isPresent()) {
            Element sourceDirectory = new Element("sourceDirectory");
            sourceDirectory.setText("../");
            sourceDirectory.setNamespace(rootElement.getNamespace());
            buildElement.addContent(sourceDirectory);
            XmlUtils.addElementToParentNode(
                            "<resources>\n" +
                            "      <resource>\n" +
                            "        <directory>src/main/resources</directory>\n" +
                            "      </resource>\n" +
                            "      <resource>\n" +
                            "        <targetPath>${basedir}/target/classes/META-INF</targetPath>\n" +
                            "        <directory>../common/src-hbase-" + getHbaseVersion(shimProperties) + "/META-INF</directory>\n" +
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
                            "    </resources>", null, "build", null, rootElement, "");
            XmlUtils.outputDoc(documentFromFile, pomPath);
        }
    }

    private void addCommonSourceFolderInclusions(Path folder, ShimProperties shimProperties) throws JDOMException, IOException {
        String pomPath = Paths.get(folder.toString(), "pom.xml").toString();
        Document documentFromFile = XmlUtils.getDocumentFromFile(pomPath);
        Element rootElement = documentFromFile.getRootElement();
        XmlUtils.addElementToParentNode(
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
                        "                   <include>common/src-hbase-" + getHbaseVersion(shimProperties) + "/**/*.java</include>\n" +
                        "                   <include>common/src-hbase-shim-" + shimProperties.getHbaseVersion() + "/**/*.java</include>\n" +
                        "                   <include>common/src-pig-shim-" + shimProperties.getPigVersion() + "/**/*.java</include>\n" +
                        "                   <include>" + folder.getFileName() + "/src/main/java/**/*.java</include>" +
                        "               </includes>\n" +
                        "          </configuration>" +
                        "</plugin>", "artifactId", "build", "plugins", rootElement, "");
        XmlUtils.outputDoc(documentFromFile, pomPath);
    }

    private String getHbaseVersion(ShimProperties shimProperties) {
        return shimProperties.getHbaseVersion() == null ? "pre-1.0" : shimProperties.getHbaseVersion();
    }

    private void addForSubversionControl(Path folder) throws IOException {
        String pomPath = Paths.get(folder.toString(), "pom.xml").toString();
        rootFolderExecutor.gitAdd(pomPath);
        for (String sourceFolder : sourceFolderArrayMaven) {
            rootFolderExecutor.gitAdd(Paths.get(folder.toString(), sourceFolder));
        }
        //todo: return git remove
//        rootFolderExecutor.gitRemove(Paths.get(folder.toString(), "test-src"));
//        rootFolderExecutor.gitRemove(Paths.get(folder.toString(), "src/META-INF"));
//        rootFolderExecutor.gitRemove(Paths.get(folder.toString(), "src/org"));
    }

    private void addAssemblySectionForShim(Path folder) throws JDOMException, IOException {
        String pomPath = Paths.get(folder.toString(), "pom.xml").toString();
        Document documentFromFile = XmlUtils.getDocumentFromFile(pomPath);
        Element rootElement = documentFromFile.getRootElement();
        Namespace parentNamespace = rootElement.getNamespace();
        if (!rootElement.getContent(new ElementFilter("build")).stream().findFirst().isPresent()) {
            Element buildElement = new Element("build", parentNamespace);
            rootElement.addContent(buildElement);
            buildElement.addContent(new Element("plugins", parentNamespace));
        }
        XmlUtils.addElementToParentNode("<plugin>\n" +
                "<artifactId>maven-assembly-plugin</artifactId>\n" +
                "<version>2.6</version>\n" +
                "<executions>\n" +
                "<execution>\n" +
                "<id>pkg</id>\n" +
                "<phase>package</phase>\n" +
                "<goals>\n" +
                "<goal>single</goal>\n" +
                "</goals>\n" +
                "</execution>\n" +
                "</executions>\n" +
                "<configuration>\n" +
                "<descriptor>${basedir}/src/main/descriptor/assembly.xml</descriptor>\n" +
                "<appendAssemblyId>false</appendAssemblyId>\n" +
                "</configuration>\n" +
                "</plugin>", "artifactId", "build", "plugins", rootElement, "");
        XmlUtils.outputDoc(documentFromFile, pomPath);
    }

    private void runTransferGoal(Path folder) throws IOException {
        //todo: when not windows us ant.sh for run
        moduleBashExecutor.executeCommand("ant.bat transfer");
    }

    private void addTransferGoalForAnt(String folder, String shortFileName) throws JDOMException, IOException {
        String fullName = Paths.get(folder, shortFileName).toString();
        LOG.debug("build.xml at: " + fullName);
        XmlUtils.addElementToDocumentFileIfNotExist(fullName, "<target name=\"transfer\" depends=\"subfloor.resolve-default\">\n" +
                "    <ivy:makepom ivyfile=\"${basedir}/ivy.xml\" pomfile=\"${basedir}/pom.xml\">\n" +
                "      <mapping conf=\"default\" scope=\"compile\"/>\n" +
                "      <mapping conf=\"pmr\" scope=\"compile\"/>\n" +
                "      <mapping conf=\"client\" scope=\"compile\"/>\n" +
                "      <mapping conf=\"provided\" scope=\"provided\"/>\n" +
                "      <mapping conf=\"test\" scope=\"test\"/>\n" +
                "    </ivy:makepom>\n" +
                "  </target>", "name");
    }

    private void removeTransferGoalTarget(Path filePath) throws JDOMException, IOException {
        removeTransferGoalTarget(Paths.get(filePath.toString(), "build.xml").toString(), "target", "name", "transfer");
    }

    private void removeTransferGoalTarget(String fullName, String elementName, String attributeName, String attributeValue) throws JDOMException, IOException {
        Document document = XmlUtils.getDocumentFromFile(fullName);
        Element rootNode = document.getRootElement();
        List<Element> targetList = rootNode.getChildren(elementName);
        Optional<Element> first = targetList.
                stream().
                filter(element -> element.getAttribute(attributeName).getValue().equalsIgnoreCase(attributeValue))
                .findFirst();

        if (first.isPresent()) {
            //if not added
            rootNode.removeContent(first.get());
            LOG.info("element target transfer successfully deleted");
        } else {
            LOG.error("element target not found and can't be deleted");
        }
        XmlUtils.outputDoc(document, fullName);
    }

    public void moveSourceFolder(Path moduleFolder) throws IOException {
        for (String sourceFolder : sourceFolderArrayMaven) {
            Path mavenSourceFolder = Paths.get(moduleFolder.toString(), sourceFolder);
            try {
                Files.createDirectories(mavenSourceFolder);
                rootFolderExecutor.gitAdd(mavenSourceFolder);
            } catch (FileAlreadyExistsException e) {
                //do nothing
                LOG.error("can't create folder, already exists " + e.getMessage());
            }
        }
        Path mavenSourceFolder = Paths.get(moduleFolder.toString(), sourceFolderArrayMaven[0]);
        //if (Files.exists(mavenSourceFolder)) {
        //we created successfully maven source folders
        if (!revert) {
            FileUtils.moveFolder(moduleFolder, "test-src", testJavaSubfolder, true);
            FileUtils.moveFolder(moduleFolder, "src/org", sourceJavaSubfolder, false);
            FileUtils.moveFolder(moduleFolder, "src/META-INF", resourceJavaSubfolder, false);
        } else {
            //revert
            FileUtils.moveFolder(moduleFolder, testJavaSubfolder, "test-src", true);
            FileUtils.moveFolder(moduleFolder, sourceJavaSubfolder, "src", true);
            FileUtils.moveFolder(moduleFolder, resourceJavaSubfolder, "src", true);
        }
        //}
    }

    //function process main folder
    //  read all structure pentaho-hadoop-shims
    //      run for api
    //      run for all shims
    //          read properties file
    //          read pom file if not exists run transfer
    //          if exists add properties run
    //              (modify properties run)
    //              //git add pom.xml, move folder of sources - git modification of folder
    //                  remove build.properties,build.xml,ivy.xml
    //              add 2 sections - META-INF, sources; tests? according to build.properties

    //function for one folder
    //  transfer
    //  properties
    //

    //    public void addTransferGoalForAnt() throws JDOMException, IOException {
//        addTransferGoalForAnt(folder, "common-shims-build.xml");
//    }

//    public void removeTransferGoalForAnt() throws JDOMException, IOException {
//        removeTransferGoalTarget(Paths.get(folder, "common-shims-build.xml").toString(), "target", "name", "transfer");
//    }

}
