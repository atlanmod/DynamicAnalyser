package com.tblf.parsing.indexer;

import com.tblf.parsing.Query;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;
import org.eclipse.modisco.kdm.source.extension.ASTNodeSourceRegion;

import java.util.Collection;

//TODO
public class HawkQuery implements Query {

    public HawkQuery() {

    }

    @Override
    public Collection<ASTNodeSourceRegion> queryLine(int lineStart, int lineEnd, Java2File java2File) {
        return null;
    }

    @Override
    public Collection<ASTNodeSourceRegion> queryPosition(int startPos, int endPos, Java2File java2File) {
        return null;
    }
}
