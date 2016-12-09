package com.pentaho.maven.transform.xml.insert;

import org.jdom2.Element;

/**
 * Created by Vasilina_Terehova on 12/9/2016.
 */
public interface BaseInsertOperation {
    void insert(Element rootNode, Element node);
}
