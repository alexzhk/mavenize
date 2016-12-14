package com.pentaho.maven.transform.xml.insert;

import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;

/**
 * Created by Vasilina_Terehova on 12/9/2016.
 */
public class ToRootNodeInserter implements BaseInsertOperation {

    public ToRootNodeInserter() {
    }

    @Override
    public void insert(Element rootNode, Element node) {
        rootNode.addContent(node);
    }
}
