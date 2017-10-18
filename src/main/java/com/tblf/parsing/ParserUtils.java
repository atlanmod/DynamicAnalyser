package com.tblf.parsing;

import com.github.javaparser.ast.stmt.Statement;
import com.tblf.util.Configuration;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Thibault on 02/10/2017.
 */
public class ParserUtils {

    /**
     * Parse the qualified name (QN) of a class and return the package it is contained in
     * @param classQualifiedName the qualified name of the class
     * @return the package qualifiedName
     */
    public static String getPackageQNFromClassQN(String classQualifiedName) {
        String[] split = classQualifiedName.split("\\.");
        return classQualifiedName.replace(".".concat(split[split.length-1]), "");
    }

    /**
     * Parse the URI of a Class file and return the package QN of its container. Uses the {@link Configuration} file
     * @param file a {@link File} such as "src/main/java/com/tblf/SimpleProject/App.java"
     * @return the package qualified name (e.g.) "com.tblf.SimpleProject"
     */
    public static String getPackageQNFromSUTFile(File file){
        String sutUri = Configuration.getProperty("sut");
        String filePath = file.getPath();

        String[] path = filePath.split("/");
        String[] sut = sutUri.split("/");

        List<String> pathList = new LinkedList<>(Arrays.asList(path));

        pathList.remove(pathList.size()-1);
        for (String repo : sut) {
            int item = pathList.indexOf(repo);
            if (item != -1) {
                pathList.remove(item);
            }
        }

        return pathList.stream().collect(Collectors.joining("."));
    }

    /**
     * Parse a statement to create a string to display out of it
     * @param statement the {@link Statement}
     * @return a {@link String}
     */
    public static String statementToString(Statement statement) {
        String toString;
        if (statement.getRange().isPresent()) {
            toString = String.format(" %s : line %s, from %s, to %s", statement, statement.getRange().get().begin.line,statement.getRange().get().begin.column, statement.getRange().get().end.column);
        } else {
            toString = statement.toString();
        }

        return toString;
    }
}
