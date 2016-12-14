package com.pentaho.maven.transform.xml.condition;

import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;

import java.util.List;
import java.util.Optional;

/**
 * Created by Vasilina_Terehova on 12/9/2016.
 */
public class ElementWithChildValueInParentPathCondition implements BaseConditionCheck, BaseConditionFinder {

    private String[] parentNodes;
    private String childNode;

    public ElementWithChildValueInParentPathCondition(String[] parentNodes) {
        this.parentNodes = parentNodes;
    }

    public ElementWithChildValueInParentPathCondition(String[] parentNodes, String childNode) {
        this.parentNodes = parentNodes;
        this.childNode = childNode;
    }

    @Override
    public boolean isValid(Element rootNode, Element element) {
        return find(rootNode, element) == null;
    }

    public static Element getParent(Element rootNode, String[] parentNodes) {
        Element parent = rootNode;
        for (String findParent : parentNodes) {
            Optional<Element> first = parent.getContent(new ElementFilter(findParent)).stream().findFirst();
            if (!first.isPresent()) {
                throw new IllegalArgumentException("parent doesn't exist " + findParent);
            }
            parent = first.get();
        }
        return parent;
    }

    @Override
    public Element find(Element rootNode, Element element) {
        Element parent = getParent(rootNode, parentNodes);
        List<Element> children = parent.getContent(new ElementFilter(element.getName()));
        if (childNode != null) {
            for (Element child : children) {
                if (child.getContent(new ElementFilter(childNode)).stream().findFirst().get().getValue().equals(
                        element.getContent(new ElementFilter(childNode)).stream().findFirst().get().getValue())) {
                    return child;
                }
            }
        }
        return null;
    }
}
