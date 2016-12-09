package com.pentaho.maven.transform.xml.insert;

import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;

/**
 * Created by Vasilina_Terehova on 12/9/2016.
 */
public class ToParentInserter implements BaseInsertOperation {

    public ToParentInserter() {
    }

    @Override
    public void insert(Element rootNode, Element node) {
        rootNode.addContent(node);
    }
}
