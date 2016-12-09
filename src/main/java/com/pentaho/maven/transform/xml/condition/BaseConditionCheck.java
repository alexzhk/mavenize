package com.pentaho.maven.transform.xml.condition;

import org.jdom2.Element;

/**
 * Created by Vasilina_Terehova on 12/9/2016.
 */
public interface BaseConditionCheck {
    boolean isValid(Element rootNode, Element element);
}
