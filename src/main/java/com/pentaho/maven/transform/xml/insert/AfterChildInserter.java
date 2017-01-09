package com.pentaho.maven.transform.xml.insert;

import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;

/**
 * Created by Vasilina_Terehova on 12/9/2016.
 */
public class AfterChildInserter implements BaseInsertOperation {
    private String childNode;

    public AfterChildInserter(String childNode) {
        this.childNode = childNode;
    }

    @Override
    public void insert(Element rootNode, Element node) {
        Element childAfter = rootNode.getContent(new ElementFilter(childNode)).stream().findFirst().get();
        rootNode.addContent(rootNode.indexOf(childAfter)+1, node);
    }
}
