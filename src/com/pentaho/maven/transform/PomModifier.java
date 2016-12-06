package com.pentaho.maven.transform;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by Vasilina_Terehova on 12/5/2016.
 */
public class PomModifier {

    public static final String API_MODULE_FOLDER = "api";
    public static String[] sourceFolderArrayMaven = new String[]{"src/main/java", "src/main/resources", "src/test/java", "src/test/resources"};
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
        pomModifier.addTransferGoalForAnt();
        pomModifier.moveShimsToMaven();
        pomModifier.removeTransferGoalForAnt();
        //new PomModifier(args[0]).executeCommand("ping pentaho.com");

        //new PomModifier(args[0]).createPom();
    }

    public void moveShimsToMaven() throws IOException {
        try (Stream<Path> paths = Files.list(Paths.get(folder))) {
            paths.forEach(filePath -> {
                if (Files.isDirectory(filePath)) {
                    //we are in the directory
                    String fileName = filePath.getFileName().toString();
                    if (fileName.equals(API_MODULE_FOLDER)) {
                        //System.out.println(fileName);
                        try {
                            moveSourceFolder(filePath);
                            runTransferGoal(filePath);
                        } catch (IOException e) {
                            System.out.println("for some reasons movement for maven impossible " + e);
                        }
                    }
                }
            });
        }
    }

    private void runTransferGoal(Path folder) throws IOException {
        executeCommand("ant transfer", folder.toString());
    }

    public void createPom() throws JDOMException, IOException {
        //transfer ivy to pom
    }

    public void addTransferGoalForAnt() throws JDOMException, IOException {
        addElementToDocumentIfNotExist(Paths.get(folder, "common-shims-build.xml").toString(), "<target name=\"transfer\" depends=\"subfloor.resolve-default\">\n" +
                "    <ivy:makepom ivyfile=\"${basedir}/ivy.xml\" pomfile=\"${basedir}/pom.xml\">\n" +
                "      <mapping conf=\"default\" scope=\"compile\"/>\n" +
                "      <mapping conf=\"pmr\" scope=\"compile\"/>\n" +
                "      <mapping conf=\"client\" scope=\"compile\"/>\n" +
                "      <mapping conf=\"provided\" scope=\"provided\"/>\n" +
                "      <mapping conf=\"test\" scope=\"test\"/>\n" +
                "    </ivy:makepom>\n" +
                "  </target>", "name");
    }

    public void removeTransferGoalForAnt() throws JDOMException, IOException {
        removeElementToDocumentIfNotExist(Paths.get(folder, "common-shims-build_after.xml").toString(), "target", "name", "transfer");
    }

    private void removeElementToDocumentIfNotExist(String fileName, String elementName, String attributeName, String attributeValue) throws JDOMException, IOException {
        SAXBuilder jdomBuilder = new SAXBuilder();
        Document document = jdomBuilder.build(fileName);
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
        outputDoc(document, "common-shims-build_after.xml");
    }

    private void addElementToDocumentIfNotExist(String fileName, String toAdd, String attributeName)
            throws JDOMException, IOException {
        SAXBuilder jdomBuilder = new SAXBuilder();
        Document document = jdomBuilder.build(fileName);
        Element rootNode = document.getRootElement();

        Element targetElement = readElementFromString(toAdd);

        List<Element> targetList = rootNode.getChildren(targetElement.getName());
        Optional<Element> first = targetList.
                stream().
                filter(element -> element.getAttribute(attributeName).getValue().equalsIgnoreCase(targetElement.getAttributeValue(attributeName))).findFirst();

        if (!first.isPresent()) {
            //if not added
            rootNode.addContent(targetElement);
        }

        outputDoc(document, "common-shims-build_after.xml");

    }

    private void outputDoc(Document document, String path) throws IOException {
        XMLOutputter xmlOutput = new XMLOutputter();

        // display nice nice
        xmlOutput.setFormat(Format.getPrettyFormat());
        xmlOutput.output(document, new FileWriter(Paths.get(folder, path).toString()));
    }

    private Element readElementFromString(String toAdd) throws JDOMException, IOException {
        SAXBuilder jdomBuilder2 = new SAXBuilder(false);
        Document doc = jdomBuilder2.build(new StringReader("<just_wrapper_now xmlns:ivy=\"antlib:org.apache.ivy.ant\">" +
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
            p.waitFor();
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
