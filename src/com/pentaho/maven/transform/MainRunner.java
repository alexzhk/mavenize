package com.pentaho.maven.transform;

import org.jdom2.*;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by Vasilina_Terehova on 12/5/2016.
 */
public class MainRunner {

    public static final String API_MODULE_FOLDER = "api";
    public static String[] sourceFolderArrayMaven = new String[]{"src/main/java", "src/main/resources", "src/test/java", "src/test/resources"};
    public static String[] shimsToProcess = new String[]{"cdh58"};
    public static String sourceJavaSubfolder = sourceFolderArrayMaven[0];
    public static String resourceJavaSubfolder = sourceFolderArrayMaven[1];
    public static String testJavaSubfolder = sourceFolderArrayMaven[2];
    public static String testResourceJavaSubfolder = sourceFolderArrayMaven[3];

    BashExecutor rootFolderExecutor;
    BashExecutor moduleBashExecutor;

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


       // mainRunner.removeTransferGoalForAnt();
        //new MainRunner(args[0]).executeCommand("ping pentaho.com");

        //new MainRunner(args[0]).createPom();
    }

    public void moveShimsToMaven() throws IOException {
        List<String> shimList = Arrays.asList(shimsToProcess);
        try (Stream<Path> paths = Files.list(Paths.get(folder))) {
            paths.forEach(filePath -> {
                if (Files.isDirectory(filePath)) {
                    //we are in the directory
                    String shortFileName = filePath.getFileName().toString();
                    if (shortFileName.equals(API_MODULE_FOLDER)) {
                        //System.out.println(shortFileName);
//                        try {
//                            runForAnyProjects(filePath);
//                            generatePropertyVersionSection(filePath);
//                            //addAssemblySectionForShim(filePath);
//                        } catch (IOException e) {
//                            System.out.println("for some reasons movement for maven impossible " + e);
//                        } catch (JDOMException e) {
//                            e.printStackTrace();
//                        }
                    } else if (shimList.contains(shortFileName)) {
                        //one of shim
                        try {
                            runForAnyProjects(filePath);
                            runForShim(filePath);
                            //add assembly section to plugin management
                        } catch (JDOMException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    private void runForShim(Path modulePath) throws JDOMException, IOException {

        addAssemblySectionForShim(modulePath);
        VersionGenerator.generatePropertyVersionSection(modulePath);

        ShimProperties shimProperties = new PropertyReader(modulePath).readShimProperties();
        addCommonSourceFolderInclusions(modulePath, shimProperties);
        addResourceFolders(modulePath, shimProperties);
        addForSubversionControl(modulePath);

        //add common source properties

    }

    private void addResourceFolders(Path modulePath, ShimProperties shimProperties) throws JDOMException, IOException {
        String pomPath = Paths.get(modulePath.toString(), "pom.xml").toString();
        Document documentFromFile = XmlUtils.getDocumentFromFile(pomPath);
        Element rootElement = documentFromFile.getRootElement();
        XmlUtils.addElementToParentNode(
        "<sourceDirectory>../</sourceDirectory><resources>\n" +
                "      <resource>\n" +
                "        <directory>src/main/resources</directory>\n" +
                "      </resource>\n" +
                "      <resource>\n" +
                "        <targetPath>${basedir}/target/classes/META-INF</targetPath>\n" +
                "        <directory>../common/src-hbase-"+getHbaseVersion(shimProperties)+"/META-INF</directory>\n" +
                "      </resource>\n" +
                "      <resource>\n" +
                "        <targetPath>${basedir}/target/classes/META-INF</targetPath>\n" +
                "        <directory>../common/src-hbase-shim-"+shimProperties.getHbaseVersion()+"/META-INF</directory>\n" +
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
                            "            <target>1.8</target>"+
                    "               <includes>" +
                    "                   <include>common/src/**/*.java</include>\n" +
                    "                   <include>common/src-mapred/**/*.java</include>\n" +
                    "                   <include>common/src-modern/**/*.java</include>\n" +
                    "                   <include>common/src-hadoop-shim-"+shimProperties.getHadoopVersion()+"/**/*.java</include>" +
                    "                   <include>common/src-hbase-"+ getHbaseVersion(shimProperties) +"/**/*.java</include>\n" +
                    "                   <include>common/src-hbase-shim-"+shimProperties.getHbaseVersion()+"/**/*.java</include>\n" +
                    "                   <include>common/src-pig-shim-"+shimProperties.getPigVersion()+"/**/*.java</include>\n" +
                            "                   <include>"+folder.getFileName()+"/src/main/java/**/*.java</include>"+
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

    private void runForAnyProjects(Path filePath) throws JDOMException, IOException {
        moduleBashExecutor = new BashExecutor(filePath);
        System.out.println("current dir : " + filePath.toString() );
        addTransferGoalForAnt(filePath.toString(), "build.xml");
        moveSourceFolder(filePath);
        runTransferGoal(filePath);
        modifyPom(filePath);
        removeTransferGoalTarget(filePath);
    }

    private void modifyPom(Path folder) throws JDOMException, IOException {
        //for api just property section
        //for shim add assembly section
        //after that call assembly goal
        //after that put plugin.xml to proper place, add to git

        //remove build.files
    }

    private void runScriptAssemblyForming(Path folder) {
        //how run bash script from java? - bash
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

    private void addAssemblyFileToGit(Path folder) {

    }

    private void removeBuildFiles(Path folder) {

    }

    private void runTransferGoal(Path folder) throws IOException {
        //todo: when not windows us ant.sh for run
        moduleBashExecutor.executeCommand("ant.bat transfer");
    }

    private void addTransferGoalForAnt(String folder, String shortFileName) throws JDOMException, IOException {
        String fullName = Paths.get(folder, shortFileName).toString();
        System.out.println("build.xml at: " + fullName);
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

//    public void addTransferGoalForAnt() throws JDOMException, IOException {
//        addTransferGoalForAnt(folder, "common-shims-build.xml");
//    }

//    public void removeTransferGoalForAnt() throws JDOMException, IOException {
//        removeTransferGoalTarget(Paths.get(folder, "common-shims-build.xml").toString(), "target", "name", "transfer");
//    }

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
            System.out.println("element target transfer successfully deleted");
        } else {
            System.out.println("element target not found and can't be deleted");
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
                System.out.println(e);
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
}
