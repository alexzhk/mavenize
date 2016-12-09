package com.pentaho.maven.transform.xml.condition;

import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;

import java.util.List;
import java.util.Optional;

/**
 * Created by Vasilina_Terehova on 12/9/2016.
 */
public class ParentPathCondition implements BaseConditionCheck {

    private String[] parentNodes;
    private String childNode;

    public ParentPathCondition(String[] parentNodes) {
        this.parentNodes = parentNodes;
    }

    public ParentPathCondition(String[] parentNodes, String childNode) {
        this.parentNodes = parentNodes;
        this.childNode = childNode;
    }

    @Override
    public boolean isValid(Element rootNode, Element element) {
        Element parent = getParent(rootNode, parentNodes);
        List<Element> children = parent.getContent(new ElementFilter(element.getName()));
        if (childNode != null) {
            return validateChildWithChildExist(element, children);
        }
        return children.size() == 0;
    }

    private boolean validateChildWithChildExist(Element element, List<Element> children) {
        for (Element child : children) {
            if (child.getContent(new ElementFilter(childNode)).stream().findFirst().get().getValue().equals(
                    element.getContent(new ElementFilter(childNode)).stream().findFirst().get().getValue())) {
                return false;
            }
        }
        return true;
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
}
