package com.pentaho.maven.transform.xml.dom;

import com.pentaho.maven.transform.xml.XmlUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Created by Aliaksandr_Zhuk on 12/25/2016.
 */
public class DomManipulator {

    private Element rootElement;

    public Element getRootElement(Path ivyPath) throws JDOMException, IOException {

        Document documentFromFile = XmlUtils.getDocumentFromFile(ivyPath.toString());
        rootElement = documentFromFile.getRootElement();

        return rootElement;
    }
    
    public List<Element> getDependencies(String tag, String childTag) {

        return rootElement.getChild(tag).getChildren(childTag);

    }

}
