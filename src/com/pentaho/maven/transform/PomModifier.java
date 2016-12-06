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
public class PomModifier {

    public static final String API_MODULE_FOLDER = "api";
    public static String[] sourceFolderArrayMaven = new String[]{"src/main/java", "src/main/resources", "src/test/java", "src/test/resources"};
    public static String[] shimsToProcess = new String[]{"cdh58"};
    public static String sourceJavaSubfolder = sourceFolderArrayMaven[0];
    public static String resourceJavaSubfolder = sourceFolderArrayMaven[1];
    public static String testJavaSubfolder = sourceFolderArrayMaven[2];
    public static String testResourceJavaSubfolder = sourceFolderArrayMaven[3];

    String folder;

    public static boolean revert = false;

    public PomModifier(String folder) {
        this.folder = folder;
    }

    public static void main(String[] args) throws IOException, JDOMException {
        if (args.length == 0) {
            throw new IllegalArgumentException("No folder specified");
        }

        PomModifier pomModifier = new PomModifier(args[0]);
        pomModifier.moveShimsToMaven();
       // pomModifier.removeTransferGoalForAnt();
        //new PomModifier(args[0]).executeCommand("ping pentaho.com");

        //new PomModifier(args[0]).createPom();
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

    private void runForShim(Path filePath) throws JDOMException, IOException {

        addAssemblySectionForShim(filePath);
        generatePropertyVersionSection(filePath);

    }

    private void runForAnyProjects(Path filePath) throws JDOMException, IOException {
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

    //<version -> < groupId -> List<Artifact> >
    //Artifact -> Dependency
    private void generatePropertyVersionSection(Path folder) throws JDOMException, IOException {
        String pomPath = Paths.get(folder.toString(), "pom.xml").toString();
        gitAdd(pomPath);
        Document documentFromFile = getDocumentFromFile(pomPath);
        Element rootElement = documentFromFile.getRootElement();
        List<Element> dependencyList = rootElement.getContent(new ElementFilter("dependencies")).get(0).getChildren();
        System.out.println();
        Map<String, Map<String, List<Artifact>>> versions = new HashMap<>();
        Map<Artifact, Element> artifactElementMap = new HashMap<>();
        dependencyList.stream().forEach(element -> {
            String version = getTagValue(element, "version");
            Map<String, List<Artifact>> map = versions.get(version) == null ? new HashMap<>() : versions.get(version);
            String classifier = getTagValue(element, "classifier");
            String groupId1 = getTagValue(element, "groupId");
            Artifact artifact = new Artifact(groupId1, getTagValue(element, "artifactId"), version, classifier);
            artifactElementMap.put(artifact, element);
            boolean groupFound = false;
            if (map.isEmpty()) {
                ArrayList<Artifact> value = new ArrayList<>();
                value.add(artifact);
                map.put(groupId1, value);
            } else {
                if (!map.isEmpty()) {
                    for (String groupId : map.keySet()) {
                        if (map.get(groupId).get(0).getGroupId().equalsIgnoreCase(groupId1)) {
                            groupFound = true;
                            map.get(groupId).add(artifact);
                        }
                    }
                    if (!groupFound) {
                        ArrayList<Artifact> value = new ArrayList<>();
                        value.add(artifact);
                        map.put(groupId1, value);
                    }
                }
            }
            versions.put(version, map);
        });


        StringBuffer propertiesBuffer = new StringBuffer("<properties>\n");
        Set<String> groupIds = new HashSet<>();
        for (Map.Entry<String, Map<String, List<Artifact>>> entries : versions.entrySet()) {
            String versionValue = entries.getKey();
            Map<String, List<Artifact>> value = entries.getValue();
            for (Map.Entry<String, List<Artifact>> groupsMap : value.entrySet()) {
                String groupId = groupsMap.getValue().get(0).getGroupId();
                String shortVersionString = groupId + ".version";
                String versionString = "${" + shortVersionString + "}";
                propertiesBuffer.append("<").append(shortVersionString).append(">").append(versionValue).append("</").append(shortVersionString).append(">\r\n");
                for (Artifact artifact : groupsMap.getValue()) {
                    artifactElementMap.get(artifact).getContent(new ElementFilter("version")).get(0).setText(versionString);
                }
                if (groupIds.contains(groupId)) {
                    groupIds.add(groupId);
                    System.out.println("AHTUNG!!!! ERROR!!!! 2 group ids found");
                    throw new IllegalArgumentException("2 equal group ids found");
                }
            }
        }
        propertiesBuffer.append("</properties>");
        System.out.println(propertiesBuffer.toString());
        //Element versionElement = rootElement.getContent(new ElementFilter("version")).stream().findFirst().get();
        addElementToDocument(propertiesBuffer.toString(), null, "version", rootElement, "");
        updateNameSpaceParent(rootElement, "properties");
        outputDoc(documentFromFile, Paths.get(folder.toString(), "pom_new.xml").toString());
    }

    private void updateNameSpaceParent(Element rootElement, String elementName) {
        Element childElement = rootElement.getContent(new ElementFilter(elementName)).stream().findFirst().get();
        updateNameSpaceParent(rootElement, childElement);
    }

    private void updateNameSpaceParent(Element rootElement, Element childElement) {
        Namespace rootNameSpace = rootElement.getNamespace();
        for (Element child : childElement.getChildren()) {
            updateNameSpaceParent(rootElement, child);
        }
        childElement.setNamespace(rootNameSpace);
    }

    private String getTagValue(Element element, String tagName) {
        List<Element> content = element.getContent(new ElementFilter(tagName));
        if (content.size() == 0) {
            return null;
        }

        Element element1 = content.get(0);
        if (element1 == null) {
            return null;
        }
        return element1.getValue();
    }

    private void runScriptAssemblyForming(Path folder) {
        //how run bash script from java? - bash
    }

    private void addAssemblySectionForShim(Path folder) throws JDOMException, IOException {
        String pomPath = Paths.get(folder.toString(), "pom.xml").toString();
        Document documentFromFile = getDocumentFromFile(pomPath);
        Element rootElement = documentFromFile.getRootElement();
        Namespace parentNamespace = rootElement.getNamespace();
        if (!rootElement.getContent(new ElementFilter("build")).stream().findFirst().isPresent()) {
            Element buildElement = new Element("build", parentNamespace);
            rootElement.addContent(buildElement);
            buildElement.addContent(new Element("pluginManagement", parentNamespace));
        }
        addElementToParentNode("<plugin>\n" +
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
                "</plugin>", "artifactId", "build", "pluginManagement", rootElement, "");
        outputDoc(documentFromFile, pomPath);
    }

    private void addAssemblyFileToGit(Path folder) {

    }

    private void removeBuildFiles(Path folder) {

    }

    private void runTransferGoal(Path folder) throws IOException {
        //todo: when not windows us ant.sh for run
        executeCommand("ant.bat transfer", folder.toString());
    }

    public void createPom() throws JDOMException, IOException {
        //transfer ivy to pom
    }

    private void addTransferGoalForAnt(String folder, String shortFileName) throws JDOMException, IOException {
        String fullName = Paths.get(folder, shortFileName).toString();
        System.out.println("build.xml at: " + fullName);
        addElementToDocumentFileIfNotExist(fullName, "<target name=\"transfer\" depends=\"subfloor.resolve-default\">\n" +
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
        Document document = getDocumentFromFile(fullName);
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
        outputDoc(document, fullName);
    }

    private Document getDocumentFromFile(String fullName) throws JDOMException, IOException {
        SAXBuilder jdomBuilder = new SAXBuilder();
        return jdomBuilder.build(fullName);
    }

    private void addElementToDocumentFileIfNotExist(String fullFileName, String toAdd, String attributeName)
            throws JDOMException, IOException {
        Document document = getDocumentFromFile(fullFileName);
        Element rootNode = document.getRootElement();

        addElementToDocument(toAdd, attributeName, null, rootNode, "xmlns:ivy=\"antlib:org.apache.ivy.ant\"");

        outputDoc(document, fullFileName);

    }

    private void addElementToDocument(String toAdd, String attributeName, String nodeNameAfter, Element rootNode, String nameSpace) throws JDOMException, IOException {
        Element targetElement = readElementFromString(toAdd, nameSpace);

        List<Element> targetList = rootNode.getChildren(targetElement.getName());
        Optional<Element> existingOne;
        if (attributeName != null) {
            existingOne = targetList.
                    stream().
                    filter(element -> element.getAttribute(attributeName).getValue().equalsIgnoreCase(targetElement.getAttributeValue(attributeName))).findFirst();
        } else {
            existingOne = targetList.
                    stream().findFirst();
        }

        if (!existingOne.isPresent()) {
            //if not added
            if (nodeNameAfter == null) {
                rootNode.addContent(targetElement);
            } else {
                Element element = rootNode.getContent(new ElementFilter(nodeNameAfter)).stream().findFirst().get();
                Parent parent = element.getParent();
                parent.addContent(parent.indexOf(element), targetElement);
            }
        }
    }

    private void addElementToParentNode(String toAdd, String attributeName, String parentNodeName, String parentNodeName2, Element rootNode, String nameSpace) throws JDOMException, IOException {
        Element targetElement = readElementFromString(toAdd, nameSpace);
        Element attributeChecked = targetElement.getContent(new ElementFilter(attributeName)).get(0);
        Element parentNode = rootNode.getContent(new ElementFilter(parentNodeName)).stream().findFirst().get().getContent(new ElementFilter(parentNodeName2)).get(0);
        List<Element> elementList = parentNode.getContent(new ElementFilter(targetElement.getName()));
        boolean found = false;
        for (Element element : elementList) {
            if (element.getContent(new ElementFilter(attributeName)).stream().findFirst().get().getValue().equals(attributeChecked.getValue())) {
                found = true;
            }
        }
        if (!found) {
            parentNode.addContent(targetElement);
        }
        updateNameSpaceParent(rootNode, targetElement);
    }

    private void outputDoc(Document document, String fullFileName) throws IOException {
        XMLOutputter xmlOutput = new XMLOutputter();

        // display nice nice
        xmlOutput.setFormat(Format.getPrettyFormat());
        xmlOutput.output(document, new FileWriter(Paths.get(fullFileName).toString()));
    }

    private Element readElementFromString(String toAdd, String namespace) throws JDOMException, IOException {
        SAXBuilder jdomBuilder2 = new SAXBuilder(false);
        Document doc = jdomBuilder2.build(new StringReader("<just_wrapper_now "+namespace+">" +
                toAdd +
                "</just_wrapper_now>"));
        Element targetElement = doc.getRootElement().getChildren().stream().findFirst().get();
        targetElement.detach();
        return targetElement;
    }

    public void moveSourceFolder(Path moduleFolder) throws IOException {
        for (String sourceFolder : sourceFolderArrayMaven) {
            Path mavenSourceFolder = Paths.get(moduleFolder.toString(), sourceFolder);
            try {
                Files.createDirectories(mavenSourceFolder);
                gitAdd(mavenSourceFolder);
            } catch (FileAlreadyExistsException e) {
                //do nothing
                System.out.println(e);
            }
        }
        Path mavenSourceFolder = Paths.get(moduleFolder.toString(), sourceFolderArrayMaven[0]);
        //if (Files.exists(mavenSourceFolder)) {
        //we created successfully maven source folders
        if (!revert) {
            moveFolder(moduleFolder, "test-src", testJavaSubfolder, true);
            moveFolder(moduleFolder, "src/org", sourceJavaSubfolder, false);
            moveFolder(moduleFolder, "src/META-INF", resourceJavaSubfolder, false);
        } else {
            //revert
            moveFolder(moduleFolder, testJavaSubfolder, "test-src", true);
            moveFolder(moduleFolder, sourceJavaSubfolder, "src", true);
            moveFolder(moduleFolder, resourceJavaSubfolder, "src", true);
        }
        //}


    }

    private void gitAdd(Path path) throws IOException {
        gitAdd(path.toString());
    }

    private void gitAdd(String path) throws IOException {
        String command = "git add " + path;
        System.out.println(command);
        executeCommand(command);
    }

    private String executeCommand(String command) throws IOException {
        return executeCommand(command, folder);
    }

    private String executeCommand(String command, String folder) throws IOException {
        StringBuffer output = new StringBuffer();
        Process p;
        try {
            System.out.println(command);
            String[] commands = command.split("\\s");
            System.out.println("commands " + Arrays.toString(commands));
            ProcessBuilder builder = new ProcessBuilder(commands);
            System.out.println("folder " + folder);
            builder.directory(new File(folder));
            p = builder.start();
            //p = runtime.exec(command);
            //p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
                System.out.println(line);
            }

            System.out.println("error stream");
            BufferedReader errorReader =
                    new BufferedReader(new InputStreamReader(p.getErrorStream()));

            line = "";
            while ((line = errorReader.readLine()) != null) {
                output.append(line + "\n");
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return output.toString();

    }

    private void gitRemove(Path path) throws IOException {
        gitRemove(path.toString());
    }

    private void gitRemove(String path) throws IOException {
        executeCommand("git rm -r -f " + path);
    }

    private void moveFolder(Path moduleFolder, String from, String to, boolean goInside) throws IOException {
//        if (revert) {
//            String temp = from;
//            from = to;
//            to = temp;
//        }
        Path fromRoot = Paths.get(moduleFolder.toString(), from);
        if (goInside) {
            if (!Files.exists(fromRoot)) {
                System.out.println("folder " + fromRoot.toString() + " doesn't exist");
                return;
            }
            Optional<Path> first = Files.list(fromRoot).findFirst();
            if (!first.isPresent()) {
                System.out.println("from folder is empty " + fromRoot);
                return;
            }
            fromRoot = first.get();
        } else {
            if (!Files.exists(fromRoot)) {
                System.out.println("from folder is empty " + fromRoot);
                return;
            }
        }


        Path antTestSourceFolder = fromRoot;
        String firstTestPackageFolder = antTestSourceFolder.getFileName().toString();
        Path mavenTestSourceFolder = Paths.get(moduleFolder.toString(), to, firstTestPackageFolder);
        System.out.println("moving " + antTestSourceFolder + " to " + mavenTestSourceFolder);
        try {
            Files.move(antTestSourceFolder, mavenTestSourceFolder);
            gitAdd(mavenTestSourceFolder);
            gitRemove(antTestSourceFolder);
        } catch (NoSuchFileException e) {
            System.out.println("nothing to be moved " + antTestSourceFolder + " to " + mavenTestSourceFolder);
        }
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
