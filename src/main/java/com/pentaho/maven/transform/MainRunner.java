package com.pentaho.maven.transform;

import com.pentaho.maven.transform.assembly.AssemblyGenerator;
import com.pentaho.maven.transform.xml.XmlUtils;
import com.pentaho.maven.transform.xml.XmlUtilsJDom1;
import com.pentaho.maven.transform.xml.condition.ElementExistCondition;
import com.pentaho.maven.transform.xml.condition.ElementWithAttributeCondition;
import com.pentaho.maven.transform.xml.condition.ElementWithChildValueInParentPathCondition;
import com.pentaho.maven.transform.xml.insert.AfterChildInserter;
import com.pentaho.maven.transform.xml.insert.ToRootNodeInserter;
import com.pentaho.maven.transform.xml.insert.ToParentPathInserter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.ElementFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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
    public static final String SHIMS_FOLDER = "shims";
    public static final String ASSEMBLY_XML = "assembly.xml";
    public static final String BUILD_XML = "build.xml";
    public static final String BUILD_PROPERTIES = "build.properties";
    public static final String ANT_TARGET_FOLDER = "dist";
    public static final String MAVEN_TARGET_FOLDER = "target";
    public static final String ZIP_EXTENSION = "zip";
    public static final String TEMP_EXTRACT_FOLDER = "extract";
    public static final String DEFAULT_FOLDER = "lib";
    public static final String CLIENT_FOLDER = "client";
    public static final String DESCRIPTOR_FOLDER = "src/assembly";
    public static final String PMR_FOLDER = "pmr";
    public static final String POM_XML = "pom.xml";
    public static final String IVY_XML = "ivy.xml";
    public static final String IVY_SETTINGS_XML = "ivysettings.xml";
    public static final String PACKAGE_IVY_XML = "package-ivy.xml";
    public static final String PACKAGE_POM_XML = "package-pom.xml";
    //public static final String JAR_ARTIFACT_DIRECTORY = "jar";
    public static final String IMPL_ARTIFACT_DIRECTORY = "impl";
    //public static final String ASSEMBLY_ARTIFACT_DIRECTORY = "assembly";
    public static final String ASSEMBLIES_ARTIFACT_DIRECTORY = "assemblies";
    public static final String DEFAULT_SCOPE = "compile";
    public static String[] sourceFolderArrayMaven = new String[]{"src/main/java", "src/main/resources", "src/test/java", "src/test/resources"};
    public static String[] shimsToProcess = new String[]{/*"hdp24", "hdp25", "cdh58", "cdh59", "mapr510", "emr46",*/"emr52"};
    public static String sourceJavaSubfolder = sourceFolderArrayMaven[0];
    public static String resourceJavaSubfolder = sourceFolderArrayMaven[1];
    public static String testJavaSubfolder = sourceFolderArrayMaven[2];
    public static String testResourceJavaSubfolder = sourceFolderArrayMaven[3];
    public static String descriptorJavaSubfolder = "src/assembly";

    BashExecutor rootFolderExecutor;
    BashExecutor moduleBashExecutor;

    private static final Logger LOG = LoggerFactory.getLogger(MainRunner.class);

    String folder;
    String outputFolder;

    public MainRunner(String folder, String outputFolder) {
        this.folder = folder;
        this.outputFolder = outputFolder;
        rootFolderExecutor = new BashExecutor(Paths.get(folder));
    }

    public static void main(String[] args) throws IOException, JDOMException, ShimCannotBeProcessed, URISyntaxException {
        if (args.length < 2) {
            throw new IllegalArgumentException("Input and output folders specified");
        }

        MainRunner mainRunner = new MainRunner(args[0], args[1]);
        String shimName = "emr52";
        mainRunner.runForShim(shimName);
    }

    public void runForShim(String shimName) throws JDOMException, URISyntaxException, ShimCannotBeProcessed, IOException {
        runForShim(Paths.get(folder, shimName));
    }

    private void runForShimsInNewStructure() throws IOException {
        List<String> shimList = Arrays.asList(shimsToProcess);
        Path folder = Paths.get(this.folder);
        try (Stream<Path> paths = Files.list(folder)) {
            paths.forEach(filePath -> {
                if (Files.isDirectory(filePath)) {
                    String shortFileName = filePath.getFileName().toString();
                    if (shortFileName.contains(SHIMS_FOLDER)) {
                        try (Stream<Path> shims = Files.list(filePath)) {

                            shims.forEach(shimPath -> {
                                String shimName = shimPath.getFileName().toString();
                                if (Files.isDirectory(shimPath) && shimList.contains(shimName)) {
                                    try {
                                        runForShim(shimPath);
                                    } catch (IOException | URISyntaxException | JDOMException | ShimCannotBeProcessed e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    void addCommonDependencies(Path antShimFolder) throws IOException, URISyntaxException, JDOMException {
        String shimName = antShimFolder.getFileName().toString();
        String implArtifactFile = Paths.get(outputFolder, SHIMS_FOLDER, shimName, IMPL_ARTIFACT_DIRECTORY, POM_XML).toString();
        Document documentFromFile = XmlUtils.getDocumentFromFile(implArtifactFile);
        Element dependenciesElement = documentFromFile.getRootElement().getContent(new ElementFilter("dependencies")).get(0);
        Element buildElement = documentFromFile.getRootElement().getContent(new ElementFilter("build")).get(0);
        Element pluginsElement = buildElement.getContent(new ElementFilter("plugins")).get(0);
        PropertyReader propertyReader = new PropertyReader(antShimFolder);

        String pomWithCommonDependencies = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource("common-dependecies-impl.xml").toURI())));
        ShimProperties shimProperties = propertyReader.readShimProperties();

        pomWithCommonDependencies = pomWithCommonDependencies.replace("${pig.version}", shimProperties.getPigVersion());

        Element pomWithDependenciesDocument = XmlUtils.readElementFromStringFull(pomWithCommonDependencies);
        List<Element> additionalDependencies = pomWithDependenciesDocument.getContent(new ElementFilter("dependencies")).get(0).getChildren();
        Element fixedPluginsElement = pomWithDependenciesDocument.getContent(new ElementFilter("build")).get(0).getContent(new ElementFilter("plugins")).get(0);
        fixedPluginsElement.detach();
        ArrayList<Element> elements = new ArrayList<>(additionalDependencies);
        for (Element dependency : elements) {
            dependency.detach();
        }
        buildElement.removeContent(pluginsElement);
        buildElement.addContent(fixedPluginsElement);
        dependenciesElement.addContent(elements);
        XmlUtils.outputDoc(documentFromFile, implArtifactFile);
    }

    private void runForApiProject(Path modulePath) throws JDOMException, IOException {
        addTransferGoalForAnt(modulePath.toString(), BUILD_XML);
        moveSourceFolder(modulePath, modulePath);
        runTransferGoal(modulePath);
        VersionGenerator.generatePropertyVersionSection(modulePath);
        //addParentSection(modulePath, "");
        //todo: parent section for api should be different
        removeTransferGoalTarget(modulePath);
        addToGithub(modulePath);
        removeGithub(modulePath);
    }

    private void createStructureShim(Path shimPath, Path folderWithPreviousSourceFolder) throws JDOMException, IOException, ShimCannotBeProcessed, URISyntaxException {
        String shimName = shimPath.getFileName().toString();
        Path shimReactorDir = Paths.get(outputFolder, SHIMS_FOLDER, shimName);
        Path jarDirectory = Paths.get(shimReactorDir.toString(), IMPL_ARTIFACT_DIRECTORY);
        moveSourceFolder(Paths.get(folderWithPreviousSourceFolder.toString(), shimPath.getFileName().toString()), jarDirectory);
        Path assembliesDirectory = Paths.get(outputFolder, SHIMS_FOLDER, shimName, ASSEMBLIES_ARTIFACT_DIRECTORY);
        Path assemblyDirectory = Paths.get(assembliesDirectory.toString(), shimName + "-shim");
        if (!Files.exists(assemblyDirectory)) {
            Files.createDirectory(assembliesDirectory);
            Files.createDirectory(assemblyDirectory);
            //FileUtils.copyFile(oldAssemblyDirectory, newAssemblyPath);
            PomUtils.parseAndSavePom("pom_shim_assemblies_reactor_template.xml", shimName, assembliesDirectory);

            PomUtils.parseAndSavePom("pom_shim_reactor_template.xml", shimName, shimReactorDir);

            PomUtils.parseAndSavePom("pom_shim_assembly_template.xml", shimName, assemblyDirectory);

        }
        addToGithub(assembliesDirectory);
        addToGithub(assembliesDirectory);
        addToGithub(jarDirectory);


        if (!Files.exists(jarDirectory)) {
            Files.createDirectory(jarDirectory);
        }
        movePackageRes(Paths.get(folderWithPreviousSourceFolder.toString(), shimPath.getFileName().toString()), assemblyDirectory);
        if (!Files.exists(assemblyDirectory)) {
            Files.createDirectory(assemblyDirectory);
        }
        createSourceDirectories(assemblyDirectory);
    }


    private void removeFirstComment(Path modulePath) throws IOException, JDOMException {
        Path pomPath = Paths.get(modulePath.toString(), MainRunner.POM_XML);
        Document documentFromFile = XmlUtils.getDocumentFromFile(pomPath.toString());
        if (documentFromFile.getContent(0).getCType().name().toLowerCase().equals("comment")) {
            documentFromFile.removeContent(0);
            XmlUtils.outputDoc(documentFromFile, Paths.get(modulePath.toString(), MainRunner.POM_XML).toString());
        }
    }

    private Integer convertVersionToInt(Element element) {

        String versionValue = "";
        Integer numVersion = 0;
        String version[];

        version = XmlUtils.getTagValue(element, "version").split("\\.");
        for (String s : version) {
            versionValue = versionValue + s;
        }

        try {
            numVersion = Integer.parseInt(versionValue);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return numVersion;
    }

    private void removeDuplicateDeps(Path modulePath) throws IOException, JDOMException {

        Path pomPath = Paths.get(modulePath.toString(), MainRunner.POM_XML);
        Document documentFromFile = XmlUtils.getDocumentFromFile(pomPath.toString());
        Element rootElement = documentFromFile.getRootElement();
        List<Element> dependencies = rootElement.getContent(new ElementFilter("dependencies")).get(0).getChildren();
        List<Element> uniqueList = new ArrayList<>();
        List<Element> duplicates = new ArrayList<>();

        dependencies.stream().forEach(element -> {
            if (uniqueList.isEmpty()) {
                uniqueList.add(element);
            } else {
                String artifact = XmlUtils.getTagValue(element, "groupId") + ":" + XmlUtils.getTagValue(element, "artifactId");
                Integer numVersion = 0;
                Integer numVersion1 = 0;

                for (int i = 0; i < uniqueList.size(); i++) {
                    Element el = uniqueList.get(i);
                    String artifact1 = XmlUtils.getTagValue(el, "groupId") + ":" + XmlUtils.getTagValue(el, "artifactId");

                    if (artifact.equals(artifact1)) {
                        if (XmlUtils.getTagValue(el, "scope").equals("provided")) {
                            el.getContent(new ElementFilter("scope")).get(0).setText(XmlUtils.getTagValue(element, "scope"));
                        }

                        if (XmlUtils.getTagValue(element, "scope").equals("compile")) {
                            el.getContent(new ElementFilter("scope")).get(0).setText(XmlUtils.getTagValue(element, "scope"));
                        }

                        numVersion = convertVersionToInt(element);
                        numVersion1 = convertVersionToInt(el);

                        if (numVersion != 0 && numVersion1 != 0) {
                            if (numVersion > numVersion1) {
                                el.getContent(new ElementFilter("version")).get(0).setText(XmlUtils.getTagValue(element, "version"));
                            }
                        }

                        duplicates.add(element);
                        break;
                    } else {
                        if (i == uniqueList.size() - 1) {
                            uniqueList.add(element);
                            break;
                        }
                    }
                }
            }
        });

        if (!uniqueList.isEmpty()) {
            rootElement.getContent(new ElementFilter("dependencies")).get(0).setContent(uniqueList);
            XmlUtils.outputDoc(documentFromFile, Paths.get(modulePath.toString(), MainRunner.POM_XML).toString());
        }

    }

    private void runForShim(Path modulePath) throws JDOMException, IOException, ShimCannotBeProcessed, URISyntaxException {
        String shimName = modulePath.getFileName().toString();
        moduleBashExecutor = new BashExecutor(modulePath);
        prepareActions(modulePath);
        PropertyReader propertyReader = new PropertyReader(modulePath);
        propertyReader.setOssLicenseFalse();
        addTransferGoalForAnt(modulePath.toString(), BUILD_XML);
        XmlUtilsJDom1.fixIvyWithClassifier(modulePath);
        runTransferGoal(modulePath);

        removeFirstComment(modulePath);
        removeDuplicateDeps(modulePath);

        addAssemblySectionForShim(modulePath);
        //runScriptAssemblyGenerating(modulePath);
//
        AssemblyGenerator assembly = new AssemblyGenerator(modulePath);
        assembly.createTrees();
        createStructureShim(modulePath, modulePath);
        Path shimReactorDir = Paths.get(outputFolder, SHIMS_FOLDER, shimName);
        //move assembly to assembly artifact
        Path assemblyDirectory = Paths.get(shimReactorDir.toString(), ASSEMBLIES_ARTIFACT_DIRECTORY, shimName + "-shim");
        moveGenerateAssembly(modulePath, assemblyDirectory);
        Path jarDirectory = Paths.get(outputFolder, SHIMS_FOLDER, shimName, IMPL_ARTIFACT_DIRECTORY);
        movePomToJarArtifact(modulePath, jarDirectory);

        //create pom for cdh57 reactor project - some pom prepared - some vars
        PomUtils.parseAndSavePom("pom_shim_reactor_template.xml", modulePath.getFileName().toString(), shimReactorDir);

        //here remove assembly section from pom in jar artifact - make function to find by condition, remove
        XmlUtils.deleteElement(Paths.get(jarDirectory.toString(), POM_XML).toString(), new ElementWithChildValueInParentPathCondition(new String[]{"build", "plugins"}, "artifactId"), Constants.ASSEMBLY_PLUGIN);

        //add element

        //create pom file assembly from some template constant with parent to cdh57 - velocity parsing or replace some vars
        PomUtils.parseAndSavePom("pom_shim_assembly_template.xml", modulePath.getFileName().toString(), assemblyDirectory);

        //add structure to git
        addToGithub(jarDirectory);
        removeGithub(modulePath);
        addToGithub(assemblyDirectory);
        removeGithub(assemblyDirectory);
        moveSourceFolder(modulePath, jarDirectory);
        movePackageRes(modulePath, assemblyDirectory);

        //ShimProperties shimProperties = propertyReader.readShimProperties();
        addCommonDependencies(modulePath);
        VersionGenerator.generatePropertyVersionSection(jarDirectory);
        addParentSection(assemblyDirectory, shimName);
        removeGroupIdFromImplArtifact(jarDirectory);
        addParentSection(jarDirectory, shimName);

        //cleanup
        removeTransferGoalTarget(modulePath);
        addToGithub(modulePath);
        removeGithub(modulePath);
//        DirectoryComparator.runCompareForShim(modulePath, moduleBashExecutor);
    }

    private Path movePomToJarArtifact(Path modulePath, Path jarDirectory) throws IOException {
        //move pom to jar artifact
        Path targetPomPath = Paths.get(jarDirectory.toString(), POM_XML);
        if (!Files.exists(targetPomPath)) {
            FileUtils.copyFileReplace(Paths.get(modulePath.toString(), POM_XML), targetPomPath);
        }
        return jarDirectory;
    }

    private void removeMainFolder(Path modulePath) throws IOException {
        Path commonShimBuildXml = Paths.get(modulePath.toString(), "common-shims-build.xml");
        FileUtils.removeFile(commonShimBuildXml);
        moduleBashExecutor.gitRemove(commonShimBuildXml);
    }

    private void prepareActions(Path modulePath) throws IOException {
        Path assemblyPath = Paths.get(modulePath.toString(), ASSEMBLY_XML);
        if (Files.exists(assemblyPath)) {
            Files.delete(assemblyPath);
        }

    }

    private void removeGroupIdFromImplArtifact(Path modulePath) throws JDOMException, IOException {
        String pomPath = Paths.get(modulePath.toString(), POM_XML).toString();
        Document documentFromFile = XmlUtils.getDocumentFromFile(pomPath);
        List<Element> groupIdElements = documentFromFile.getRootElement().getContent(new ElementFilter("groupId"));
        if (groupIdElements.size() > 0) {
            groupIdElements.get(0).detach();
            XmlUtils.outputDoc(documentFromFile, pomPath);
        }
    }

    private void addParentSection(Path modulePath, String shimName) throws JDOMException, IOException {
        String pomPath = Paths.get(modulePath.toString(), POM_XML).toString();
        XmlUtils.addElementToDocumentFile(
                pomPath,
                Constants.PARENT_TAG.replace("${module_name}", shimName)
                , new ElementExistCondition(), new AfterChildInserter("modelVersion"), "");
    }

    void moveGenerateAssembly(Path modulePath, Path where) throws ShimCannotBeProcessed, IOException {
        String shimName = modulePath.getFileName().toString();
        Path fullAssemblyPath = Paths.get(modulePath.toString(), ASSEMBLY_XML);
        Path targetDescriptorFolder = Paths.get(outputFolder, SHIMS_FOLDER, modulePath.getFileName().toString(), ASSEMBLIES_ARTIFACT_DIRECTORY, shimName + "-shim", DESCRIPTOR_FOLDER);
        Path neededFullAssemblyPath = Paths.get(where.toString(), DESCRIPTOR_FOLDER, ASSEMBLY_XML);
        if (!Files.exists(fullAssemblyPath)) {
            String msg = "no assembly generated by some reasons";
            LOG.error(msg);
            // throw new ShimCannotBeProcessed(msg);
        }
        if (!Files.exists(targetDescriptorFolder)) {
            Files.createDirectory(targetDescriptorFolder);
        }
        System.out.println(fullAssemblyPath);
        System.out.println(neededFullAssemblyPath);
        FileUtils.moveFileReplace(fullAssemblyPath, neededFullAssemblyPath);
        rootFolderExecutor.gitAdd(neededFullAssemblyPath);
    }

    public static String getHbaseGenerationVersion(ShimProperties shimProperties) {
        return shimProperties.getHbaseGenerationVersion() == null ? "pre1.0" : shimProperties.getHbaseGenerationVersion();
    }

    private void addForSubversionControl(Path folder) throws IOException {
        String pomPath = Paths.get(folder.toString(), "pom.xml").toString();
        rootFolderExecutor.gitAdd(pomPath);
        for (String sourceFolder : sourceFolderArrayMaven) {
            rootFolderExecutor.gitAdd(Paths.get(folder.toString(), sourceFolder));
        }
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
        XmlUtils.addElement(Constants.ASSEMBLY_PLUGIN, rootElement, new ElementWithChildValueInParentPathCondition(parentNodes, "artifactId"), new ToParentPathInserter(parentNodes), "");
        XmlUtils.outputDoc(documentFromFile, pomPath);
    }

    private void runTransferGoal(Path folder) throws IOException {
        //todo: when not windows us ant.sh for run
        moduleBashExecutor.executeCommand("ant.bat transfer");
    }

    private void addTransferGoalForAnt(String folder, String shortFileName) throws JDOMException, IOException {
        String fullName = Paths.get(folder, shortFileName).toString();
        LOG.debug("build.xml at: " + fullName);
        XmlUtils.addElementToDocumentFile(fullName, Constants.MAKEPOM_TARGET, new ElementWithAttributeCondition("name"), new ToRootNodeInserter(), "xmlns:ivy=\"antlib:org.apache.ivy.ant\"");
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

    public void createSourceDirectories(Path where) throws IOException {
        for (String sourceFolder : sourceFolderArrayMaven) {
            Path mavenSourceFolder = Paths.get(where.toString(), sourceFolder);
            try {
                Files.createDirectories(mavenSourceFolder);
                rootFolderExecutor.gitAdd(mavenSourceFolder);
            } catch (FileAlreadyExistsException e) {
                //do nothing
                LOG.error("can't create folder, already exists " + e.getMessage());
            }
        }
    }

    public void moveSourceFolder(Path moduleFolder, Path where) throws IOException {
        createSourceDirectories(where);
        FileUtils.copyAllInsideFolder(moduleFolder, "test-src", Paths.get(where.toString(), testJavaSubfolder));
        System.out.println(moduleFolder);
        System.out.println(Paths.get(where.toString(), sourceJavaSubfolder).toString());
        FileUtils.copyFolder(moduleFolder, "src/org", where, sourceJavaSubfolder);
        FileUtils.copyFolder(moduleFolder, "src/META-INF", where, resourceJavaSubfolder);
    }

    public void movePackageRes(Path moduleFolder, Path where) throws IOException {
        System.out.println("package-res " + moduleFolder);
        FileUtils.copyAllInsideFolder(moduleFolder, "package-res", Paths.get(where.toString(), resourceJavaSubfolder));
    }


    void addToGithub(Path modulePath) throws IOException {
//        moduleBashExecutor.gitAdd(Paths.get(modulePath.toString(), "src/main"));
//        moduleBashExecutor.gitAdd(Paths.get(modulePath.toString(), "src/test"));
//        moduleBashExecutor.gitAdd(Paths.get(modulePath.toString(), "pom.xml"));
    }

    private void removeGithub(Path modulePath) throws IOException {
//        moduleBashExecutor.gitRemove(Paths.get(modulePath.toString(), "test-src"));
//        moduleBashExecutor.gitRemove(Paths.get(modulePath.toString(), "src/org"));
//        moduleBashExecutor.gitRemove(Paths.get(modulePath.toString(), "src/META-INF"));
//        moduleBashExecutor.gitRemove(Paths.get(modulePath.toString(), "package-res"));

    }

    public void removeGithubCommonSource(Path modulePath, String src, String testSrc) throws IOException {
        if (testSrc != null) {
            moduleBashExecutor.gitRemove(Paths.get(modulePath.toString(), testSrc));
        }
        moduleBashExecutor.gitRemove(Paths.get(modulePath.toString(), src));

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

    public void moveShimsToMaven() throws IOException {
        List<String> shimList = Arrays.asList(shimsToProcess);
        Path folder = Paths.get(this.folder);
        try (Stream<Path> paths = Files.list(folder)) {
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
                        } catch (IOException | URISyntaxException | JDOMException | ShimCannotBeProcessed e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
        try {
            new MainRunnerCommonSources(this).moveCommonSourceFolders(folder);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        removeMainFolder(folder);
    }

}
