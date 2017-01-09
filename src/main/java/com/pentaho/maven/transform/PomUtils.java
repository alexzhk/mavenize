package com.pentaho.maven.transform;

import com.pentaho.maven.transform.xml.XmlUtils;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        String pom = new String(Files.readAllBytes(Paths.get(MainRunner.class.getClassLoader().getResource(pomNameInResources).toURI())));
        FileOutputStream fileOutputStream = new FileOutputStream(Paths.get(whereSave.toString(), MainRunner.POM_XML).toString());
        fileOutputStream.write(pom.replace("${module_name}", moduleName).getBytes());
        fileOutputStream.close();
        //new BufferedInputStream()
    }

}
