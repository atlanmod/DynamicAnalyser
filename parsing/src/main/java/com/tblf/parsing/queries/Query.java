package com.tblf.parsing.queries;

import org.eclipse.modisco.java.composition.javaapplication.Java2File;
import org.eclipse.modisco.kdm.source.extension.ASTNodeSourceRegion;

import java.util.Collection;

public interface Query {
    /**
     * Query a line in a {@link Java2File} with the linePositions
     *
     * @param lineStart
     * @param lineEnd
     * @param java2File
     * @return the list of the {@link org.eclipse.gmt.modisco.java.ASTNode} corresponding to the lines
     */
    Collection<ASTNodeSourceRegion> queryLine(int lineStart, int lineEnd, Java2File java2File);


    /**
     * Query a line in a {@link Java2File} with the linePositions
     *
     * @param startPos  the starting position in the file of the specified statement
     * @param endPos    the ending position in the file of the specified
     * @param java2File
     * @return the list of the {@link org.eclipse.gmt.modisco.java.ASTNode} corresponding to the lines
     */
    Collection<ASTNodeSourceRegion> queryPosition(int startPos, int endPos, Java2File java2File);

}
