package com.pentaho.maven.transform.xml;

import com.pentaho.maven.transform.xml.condition.BaseConditionCheck;
import com.pentaho.maven.transform.xml.condition.ConditionToCheck;
import com.pentaho.maven.transform.xml.condition.ElementExistCondition;
import com.pentaho.maven.transform.xml.condition.ElementWithAttributeCondition;
import com.pentaho.maven.transform.xml.insert.BaseInsertOperation;
import com.pentaho.maven.transform.xml.insert.ToParentInserter;
import org.jdom2.*;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Created by Vasilina_Terehova on 12/7/2016.
 */
public class XmlUtils {

    public static Document getDocumentFromFile(String fullName) throws JDOMException, IOException {
        SAXBuilder jdomBuilder = new SAXBuilder();
        return jdomBuilder.build(fullName);
    }

    public static void addElement(String toAdd, Element rootNode, BaseConditionCheck conditionToCheck, BaseInsertOperation baseInsertOperation, String nameSpace) throws JDOMException, IOException {
        Element targetElement = readElementFromString(toAdd, nameSpace);
        if (nameSpace.trim().isEmpty()) {
            updateNameSpaceParent(rootNode, targetElement);
        }
        if (conditionToCheck.isValid(rootNode, targetElement)) {
            baseInsertOperation.insert(rootNode, targetElement);
        }
    }

    public static void addElementToDocumentFile(String fullFileName, String toAdd, BaseConditionCheck conditionToCheck, BaseInsertOperation baseInsertOperation, String nameSpace)
            throws JDOMException, IOException {
        Document document = getDocumentFromFile(fullFileName);
        Element rootNode = document.getRootElement();

        addElement(toAdd, rootNode, conditionToCheck, baseInsertOperation, nameSpace);

        outputDoc(document, fullFileName);

    }

    public static void outputDoc(Document document, String fullFileName) throws IOException {
        XMLOutputter xmlOutput = new XMLOutputter();

        // display nice nice
        xmlOutput.setFormat(Format.getPrettyFormat());
        xmlOutput.output(document, new FileWriter(Paths.get(fullFileName).toString()));
    }

    public static Element readElementFromString(String toAdd, String namespace) throws JDOMException, IOException {
        SAXBuilder jdomBuilder2 = new SAXBuilder(false);
        Document doc = jdomBuilder2.build(new StringReader("<just_wrapper_now "+namespace+">" +
                toAdd +
                "</just_wrapper_now>"));
        Element targetElement = doc.getRootElement().getChildren().stream().findFirst().get();
        targetElement.detach();
        return targetElement;
    }

    public static void updateNameSpaceParent(Element rootElement, String elementName) {
        Element childElement = rootElement.getContent(new ElementFilter(elementName)).stream().findFirst().get();
        updateNameSpaceParent(rootElement, childElement);
    }

    public static void updateNameSpaceParent(Element rootElement, Element childElement) {
        Namespace rootNameSpace = rootElement.getNamespace();
        for (Element child : childElement.getChildren()) {
            updateNameSpaceParent(rootElement, child);
        }
        childElement.setNamespace(rootNameSpace);
    }

    public static String getTagValue(Element element, String tagName) {
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
}
