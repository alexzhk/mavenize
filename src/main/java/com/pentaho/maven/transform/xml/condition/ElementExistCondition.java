package com.pentaho.maven.transform.xml.condition;

import org.jdom2.Element;
import org.jdom2.filter.ContentFilter;
import org.jdom2.filter.ElementFilter;

import java.util.List;

/**
 * Created by Vasilina_Terehova on 12/9/2016.
 */
public class ElementExistCondition implements BaseConditionCheck, BaseConditionFinder {

    @Override
    public boolean isValid(Element rootNode, Element element) {
        return find(rootNode, element) == null;
    }

    @Override
    public Element find(Element rootNode, Element element) {
        List<Element> content = rootNode.getContent(new ElementFilter(element.getName()));
        return (content.size() > 0 ? content.get(0) : null);
    }
}
