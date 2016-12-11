package com.pentaho.maven.transform;

import com.pentaho.maven.transform.xml.XmlUtils;
import com.pentaho.maven.transform.xml.XmlUtilsJDom1;
import com.pentaho.maven.transform.xml.condition.ElementExistCondition;
import com.pentaho.maven.transform.xml.condition.ElementWithAttributeCondition;
import com.pentaho.maven.transform.xml.condition.ParentPathCondition;
import com.pentaho.maven.transform.xml.insert.AfterChildInserter;
import com.pentaho.maven.transform.xml.insert.ToParentInserter;
import com.pentaho.maven.transform.xml.insert.ToParentPathInserter;
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
 * run convert for all project from mvn to ant
 * the only argument to main is the folder where project pentaho-hadoop-shims is checked out
 * for now main.sh, tmp.sh should be there - could be checkout VasilinaTerehova/pentaho-haodop-shims/BAD-570
 * api folder proccessed separately - just folder to maven structure movement, convert pom, generate versions property section, add parent tag
 * for every shim the previous actions take part, assembly plugin section put, also script with generating assembly.xml starts which, after script finishes
 * assembly move to needed folder, add to github
 * Created by Vasilina_Terehova on 12/5/2016.
 */
public class MainRunner {

    public static final String API_MODULE_FOLDER = "api";
    public static final String ASSEMBLY_XML = "assembly.xml";
    public static final String BUILD_XML = "build.xml";
    public static final String ANT_TARGET_FOLDER = "dist";
    public static final String MAVEN_TARGET_FOLDER = "target";
    public static final String ZIP_EXTENSION = "zip";
    public static final String TEMP_EXTRACT_FOLDER = "extract";
    public static final String DEFAULT_FOLDER = "lib";
    public static final String CLIENT_FOLDER = "client";
    public static final String DESCRIPTOR_FOLDER = "src/main/descriptor";
    public static final String PMR_FOLDER = "pmr";
    public static String[] sourceFolderArrayMaven = new String[]{"src/main/java", "src/main/resources", "src/test/java", "src/test/resources"};
    public static String[] shimsToProcess = new String[]{"hdp24", "hdp25", "cdh58", "cdh59", "mapr410", "mapr510", "emr310", "emr41", "emr46"};
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
                    moduleBashExecutor = new BashExecutor(filePath);
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
                        } catch (IOException | JDOMException | ShimCannotBeProcessed e) {
                            e.printStackTrace();
                            LOG.error("Shim can't be processed " + e.getMessage());
                        }
                    }
                }
            });
        }
    }

    private void runCompareForShim(Path modulePath) throws IOException {
        moduleBashExecutor.runAntCleanAllResolveDist();
        moduleBashExecutor.runMvnCleanInstall();
        Optional<Path> zipArchiveAnt = Files.list(Paths.get(modulePath.toString(), ANT_TARGET_FOLDER)).filter(path -> path.getFileName().toString().endsWith(ZIP_EXTENSION)).findFirst();
        Optional<Path> zipArchiveMaven = Files.list(Paths.get(modulePath.toString(), MAVEN_TARGET_FOLDER)).filter(path -> path.getFileName().toString().endsWith(ZIP_EXTENSION)).findFirst();
        //for running this 7z needed installed
        moduleBashExecutor.unArchive(zipArchiveAnt.get(), Paths.get(modulePath.toString(), ANT_TARGET_FOLDER, TEMP_EXTRACT_FOLDER));
        moduleBashExecutor.unArchive(zipArchiveMaven.get(), Paths.get(modulePath.toString(), MAVEN_TARGET_FOLDER, TEMP_EXTRACT_FOLDER));

        Path defaultFolderAnt = Paths.get(modulePath.toString(), ANT_TARGET_FOLDER, TEMP_EXTRACT_FOLDER, modulePath.getFileName().toString(),
                DEFAULT_FOLDER);
        Path defaultFolderMaven = Paths.get(modulePath.toString(), MAVEN_TARGET_FOLDER, TEMP_EXTRACT_FOLDER,
                modulePath.getFileName().toString(), DEFAULT_FOLDER);
        Path clientFolderAnt = Paths.get(defaultFolderAnt.toString(), CLIENT_FOLDER);
        Path clientFolderMaven = Paths.get(defaultFolderMaven.toString(), CLIENT_FOLDER);
        Path pmrFolderAnt = Paths.get(defaultFolderAnt.toString(), PMR_FOLDER);
        Path pmrFolderMaven = Paths.get(defaultFolderMaven.toString(), PMR_FOLDER);
        System.out.println("---------------compare default folders");
        new DirectoryComparator().compare(clientFolderAnt, clientFolderMaven);
        System.out.println("---------------compare client folders");
        new DirectoryComparator().compare(defaultFolderAnt, defaultFolderMaven);
        System.out.println("---------------compare pmr folders");
        new DirectoryComparator().compare(pmrFolderAnt, pmrFolderMaven);
    }

    private void runForApiProject(Path modulePath) throws JDOMException, IOException {
        addTransferGoalForAnt(modulePath.toString(), BUILD_XML);
        moveSourceFolder(modulePath);
        runTransferGoal(modulePath);
        VersionGenerator.generatePropertyVersionSection(modulePath);
        addParentSection(modulePath);
        removeTransferGoalTarget(modulePath);
        addToGithub(modulePath);
    }

    private void runForShim(Path modulePath) throws JDOMException, IOException, ShimCannotBeProcessed {
        prepareActions(modulePath);
        PropertyReader propertyReader = new PropertyReader(modulePath);
        propertyReader.setOssLicenseFalse();
        addTransferGoalForAnt(modulePath.toString(), BUILD_XML);
        XmlUtilsJDom1.fixIvyWithClassifier(modulePath);
        runTransferGoal(modulePath);
//
        addAssemblySectionForShim(modulePath);
        runScriptAssemblyGenerating(modulePath);

        moveSourceFolder(modulePath);

        ShimProperties shimProperties = propertyReader.readShimProperties();
        addCommonSourceFolderInclusions(modulePath, shimProperties);
        //addCommonTestSourceFolderInclusions(modulePath, shimProperties);
        addResourceFolders(modulePath, shimProperties);
        VersionGenerator.generatePropertyVersionSection(modulePath);
        addParentSection(modulePath);

        moveGenerateAssembly(modulePath);

        //cleanup
        removeTransferGoalTarget(modulePath);
        addToGithub(modulePath);
        runCompareForShim(modulePath);
    }

    private void prepareActions(Path modulePath) throws IOException {
        Path assemblyPath = Paths.get(modulePath.toString(), ASSEMBLY_XML);
        if (Files.exists(assemblyPath)) {
            Files.delete(assemblyPath);
        }

    }

    private void addParentSection(Path modulePath) throws JDOMException, IOException {
        String pomPath = Paths.get(modulePath.toString(), VersionGenerator.POM_XML).toString();
        XmlUtils.addElementToDocumentFile(
                pomPath,
                Constants.PARENT_TAG
                , new ElementExistCondition(), new AfterChildInserter("modelVersion"), "");
    }

    private void runScriptAssemblyGenerating(Path modulePath) throws IOException {
        Path shimName = modulePath.getFileName();
        rootFolderExecutor.executeCommand("\"C:\\Program Files\\Git\\git-bash.exe\" .\\main.sh " + shimName);
    }

    private void moveGenerateAssembly(Path modulePath) throws ShimCannotBeProcessed, IOException {
        Path desriptorFolder = Paths.get(modulePath.toString(), DESCRIPTOR_FOLDER);
        Path fullAssemblyPath = Paths.get(modulePath.toString(), ASSEMBLY_XML);
        Path neededFullAssemblyPath = Paths.get(modulePath.toString(), DESCRIPTOR_FOLDER, ASSEMBLY_XML);
        if (!Files.exists(fullAssemblyPath)) {
            String msg = "no assembly generated by some reasons";
            LOG.error(msg);
            // throw new ShimCannotBeProcessed(msg);
        }
        if (!Files.exists(desriptorFolder)) {
            Files.createDirectory(desriptorFolder);
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
            String[] parentNodes = {"build"};
            XmlUtils.addElement(
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
                            "    </resources>", rootElement, new ParentPathCondition(parentNodes), new ToParentPathInserter(parentNodes), "");
            XmlUtils.outputDoc(documentFromFile, pomPath);
        }
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
                        "                   <include>common/src-hbase-" + getHbaseVersion(shimProperties) + "/**/*.java</include>\n" +
                        "                   <include>common/src-hbase-shim-" + shimProperties.getHbaseVersion() + "/**/*.java</include>\n" +
                        "                   <include>common/src-pig-shim-" + shimProperties.getPigVersion() + "/**/*.java</include>\n" +
                        "                   <include>" + folder.getFileName() + "/src/main/java/**/*.java</include>" +
                        "               </includes>\n" +
                        "          </configuration>" +
                        "</plugin>", new ParentPathCondition(parentNodes, "artifactId"), new ToParentPathInserter(parentNodes), "");
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
                        "</plugin>", new ParentPathCondition(parentNodes, "artifactId"), new ToParentPathInserter(parentNodes), "");
    }

    private String getHbaseVersion(ShimProperties shimProperties) {
        return shimProperties.getHbaseGenerationVersion() == null ? "pre1.0" : shimProperties.getHbaseGenerationVersion();
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
        String[] parentNodes = {"build", "plugins"};
        XmlUtils.addElement(Constants.ASSEMBLY_PLUGIN, rootElement, new ParentPathCondition(parentNodes, "artifactId"), new ToParentPathInserter(parentNodes), "");
        XmlUtils.outputDoc(documentFromFile, pomPath);
    }

    private void runTransferGoal(Path folder) throws IOException {
        //todo: when not windows us ant.sh for run
        moduleBashExecutor.executeCommand("ant.bat transfer");
    }

    private void addTransferGoalForAnt(String folder, String shortFileName) throws JDOMException, IOException {
        String fullName = Paths.get(folder, shortFileName).toString();
        LOG.debug("build.xml at: " + fullName);
        XmlUtils.addElementToDocumentFile(fullName, Constants.MAKEPOM_TARGET, new ElementWithAttributeCondition("name"), new ToParentInserter(), "xmlns:ivy=\"antlib:org.apache.ivy.ant\"");
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
        FileUtils.moveAllInsideFolder(moduleFolder, "test-src", testJavaSubfolder);
        FileUtils.moveFolder(moduleFolder, "src/org", sourceJavaSubfolder);
        FileUtils.moveFolder(moduleFolder, "src/META-INF", resourceJavaSubfolder);
        FileUtils.moveAllInsideFolder(moduleFolder, "package-res", resourceJavaSubfolder);
    }


    private void addToGithub(Path modulePath) throws IOException {
        moduleBashExecutor.gitAdd(Paths.get(modulePath.toString(), "src/main"));
        moduleBashExecutor.gitAdd(Paths.get(modulePath.toString(), "src/test"));
        moduleBashExecutor.gitAdd(Paths.get(modulePath.toString(), "pom.xml"));
        moduleBashExecutor.gitRemove(Paths.get(modulePath.toString(), "test-src"));
        moduleBashExecutor.gitRemove(Paths.get(modulePath.toString(), "src/org"));
        moduleBashExecutor.gitRemove(Paths.get(modulePath.toString(), "src/META-INF"));
        moduleBashExecutor.gitRemove(Paths.get(modulePath.toString(), "package-res"));
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

}
