package com.tblf.parsing;

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
}
