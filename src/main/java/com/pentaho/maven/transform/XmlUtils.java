package com.pentaho.maven.transform;

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

    public static void addElementToDocumentFileIfNotExist(String fullFileName, String toAdd, String attributeName)
            throws JDOMException, IOException {
        Document document = getDocumentFromFile(fullFileName);
        Element rootNode = document.getRootElement();

        addElementToDocument(toAdd, attributeName, null, rootNode, "xmlns:ivy=\"antlib:org.apache.ivy.ant\"");

        outputDoc(document, fullFileName);

    }

    public static void addElementToDocument(String toAdd, String attributeName, String nodeNameAfter, Element rootNode, String nameSpace) throws JDOMException, IOException {
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
            if (nameSpace == null || nameSpace.trim().isEmpty()) {
                updateNameSpaceParent(rootNode, targetElement);
            }
            if (nodeNameAfter == null) {
                rootNode.addContent(targetElement);
            } else {
                Element element = rootNode.getContent(new ElementFilter(nodeNameAfter)).stream().findFirst().get();
                Parent parent = element.getParent();
                parent.addContent(parent.indexOf(element), targetElement);
            }
        }
    }

    public static void addElementToParentNode(String toAdd, String attributeName, String parentNodeName, String parentNodeName2, Element rootNode, String nameSpace) throws JDOMException, IOException {
        Element targetElement = readElementFromString(toAdd, nameSpace);
        Element attributeChecked = null;
        if (attributeName != null) {
            attributeChecked = targetElement.getContent(new ElementFilter(attributeName)).get(0);
        }
        Element parentNode = rootNode.getContent(new ElementFilter(parentNodeName)).stream().findFirst().get();
        if (parentNodeName2 != null) {
            parentNode = parentNode.getContent(new ElementFilter(parentNodeName2)).get(0);
        }
        List<Element> elementList = parentNode.getContent(new ElementFilter(targetElement.getName()));
        boolean found = false;
        for (Element element : elementList) {
            if (attributeName != null) {
                if (element.getContent(new ElementFilter(attributeName)).stream().findFirst().get().getValue().equals(attributeChecked.getValue())) {
                    found = true;
                }
            }
        }
        if (!found) {
            parentNode.addContent(targetElement);
        }
        updateNameSpaceParent(rootNode, targetElement);
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
