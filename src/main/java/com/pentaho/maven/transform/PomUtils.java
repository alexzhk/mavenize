package com.pentaho.maven.transform;

import com.pentaho.maven.transform.xml.XmlUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.ElementFilter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Vasilina_Terehova on 1/9/2017.
 */
public class PomUtils {
    public static void updateParent(Path shimPath, String parentName) throws JDOMException, IOException {
        String pomShimReactorFile = Paths.get(shimPath.toString(), MainRunner.POM_XML).toString();
        Document documentFromFile = XmlUtils.getDocumentFromFile(pomShimReactorFile);
        documentFromFile.getRootElement().getContent(new ElementFilter("parent")).get(0)
                .getContent(new ElementFilter("artifactId")).get(0).setText(parentName);
        XmlUtils.outputDoc(documentFromFile, pomShimReactorFile);
    }

    public static void parseAndSavePom(String pomNameInResources, String moduleName, Path whereSave) throws IOException, URISyntaxException {
        PomUtils.parseAndSavePom(pomNameInResources, new HashMap<>(), moduleName, whereSave);
    }

    public static void parseAndSavePom(String pomNameInResources, Map<String, String> vars, String moduleName, Path whereSave) throws IOException, URISyntaxException {
        vars.put("module_name", moduleName);
        String pom = new String(Files.readAllBytes(Paths.get(MainRunner.class.getClassLoader().getResource(pomNameInResources).toURI())));
        FileOutputStream fileOutputStream = new FileOutputStream(Paths.get(whereSave.toString(), MainRunner.POM_XML).toString());
        vars.entrySet().stream().forEach(stringStringEntry -> {
            try {
                fileOutputStream.write(pom.replace("${" + stringStringEntry.getKey() + "}", stringStringEntry.getValue()).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        fileOutputStream.close();
        //new BufferedInputStream()
    }

    public static void addModuleToModuleList(Path shimPath, String moduleName) throws JDOMException, IOException {
        String pomShimReactorFile = Paths.get(shimPath.toString(), MainRunner.POM_XML).toString();
        Document documentFromFile = XmlUtils.getDocumentFromFile(pomShimReactorFile);
        Namespace rootElementScope = documentFromFile.getRootElement().getNamespace();
        List<Element> modules = documentFromFile.getRootElement().getContent(new ElementFilter("modules"));
        if (modules.size() > 0) {
            Optional<Element> first = modules.get(0).getChildren().stream().filter(element -> element.getText().equals(moduleName)).findFirst();
            if (!first.isPresent()) {
                Element moduleElement = new Element("module", rootElementScope);
                moduleElement.setText(moduleName);
                modules.get(0).addContent(moduleElement);
            }
        }
        XmlUtils.outputDoc(documentFromFile, pomShimReactorFile);
    }

}
