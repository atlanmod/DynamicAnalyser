package com.tblf.parsing.helper;

import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.tools.AbstractTool;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;

public class EOLQueryHelper extends AbstractTool {

    public String getParentQualifiedName(GraphNodeWrapper node) throws EolRuntimeException {
        return getQualifiedName((GraphNodeWrapper) node.getFeature("eContainer"));
    }

    public String getQualifiedName(GraphNodeWrapper node) throws EolRuntimeException {
        Object name = node.getFeature("name");
        GraphNodeWrapper parent = (GraphNodeWrapper) node.getFeature("eContainer");

        if (parent == null || (parent.getTypeName().equals("Model")))
            return name == null ? "." : (String) name;

        return getQualifiedName(parent)+"."+name;
    }
}
