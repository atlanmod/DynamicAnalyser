package com.tblf.parsing;

import org.eclipse.gmt.modisco.java.Statement;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;
import org.eclipse.modisco.kdm.source.extension.ASTNodeSourceRegion;

import java.util.Collection;
import java.util.stream.Collectors;

public class StreamQuery implements Query {

    @Override
    public Collection<ASTNodeSourceRegion> queryLine(int lineStart, int lineEnd, Java2File java2File) {
        return java2File.getChildren().stream().filter(astNodeSourceRegion -> astNodeSourceRegion.getStartLine() == lineStart)
                .filter(astNodeSourceRegion -> astNodeSourceRegion.getEndLine() == lineEnd)
                .filter(astNodeSourceRegion -> astNodeSourceRegion.getNode() instanceof Statement)
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<ASTNodeSourceRegion> queryPosition(int startPos, int endPos, Java2File java2File) {
        return java2File.getChildren().stream().filter(astNodeSourceRegion -> astNodeSourceRegion.getStartPosition() == startPos)
                .filter(astNodeSourceRegion -> astNodeSourceRegion.getEndPosition() == endPos)
                .filter(astNodeSourceRegion -> astNodeSourceRegion.getNode() instanceof Statement)
                .collect(Collectors.toSet());
    }
}
