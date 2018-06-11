package com.tblf.parsing;

import com.tblf.parsing.indexer.HawkQuery;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;
import org.eclipse.modisco.java.composition.javaapplication.JavaapplicationFactory;
import org.junit.Test;

import java.io.File;

public class HawkQueryTest {


    @Test
    public void checkQueryTestSetup() {
        Java2File java2File = JavaapplicationFactory.eINSTANCE.createJava2File();

        Query query = new HawkQuery(new File("src/test/resources/hawk/models/simpleProject"));
        query.queryLine(3, 3, java2File);
    }
}

