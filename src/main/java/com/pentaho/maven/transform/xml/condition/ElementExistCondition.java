package com.pentaho.maven.transform.xml.condition;

import org.jdom2.Element;
import org.jdom2.filter.ContentFilter;
import org.jdom2.filter.ElementFilter;

/**
 * Created by Vasilina_Terehova on 12/9/2016.
 */
public class ElementExistCondition implements BaseConditionCheck {

    @Override
    public boolean isValid(Element rootNode, Element element) {
        return rootNode.getContent(new ElementFilter(element.getName())).size() == 0;
    }
}
