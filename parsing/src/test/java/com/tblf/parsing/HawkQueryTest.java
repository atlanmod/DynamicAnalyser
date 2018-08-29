package com.tblf.parsing;

import com.tblf.parsing.indexer.HawkQuery;
import com.tblf.parsing.queries.Query;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class HawkQueryTest {

    @Before
    public void setUp() throws IOException {
        File index = new File("index");
        if (index.exists()) {
            FileUtils.deleteDirectory(index);
        }
    }

    @Test
    public void checkHawkLoadingSimpleProject() throws Exception {
        Query query = new HawkQuery(new File("src/test/resources/hawk/models/simpleProject"));
        query.queryLine(3, 3, null);
        ((HawkQuery)query).close();
    }

    @Test
    public void checkHawkLoadingComplexProject() {
        try(HawkQuery query = new HawkQuery(new File("src/test/resources/hawk/models/junit"))) {
            query.queryLine(3, 3, null);
            query.close();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

    }

    @Test
    public void checkHawkInputQuery() throws Exception {
        HawkQuery hawkQuery = new HawkQuery(new File("src/test/resources/hawk/models/junit"));
        Assert.assertEquals(1297, hawkQuery.queryWithInputEOLQuery("return ClassDeclaration.all.size();"));
        hawkQuery.close();
    }


    @Test
    public void checkHawkMultipleInputQuery() throws Exception {
        HawkQuery hawkQuery = new HawkQuery(new File("src/test/resources/hawk/models/junit"));
        Assert.assertEquals(4241, hawkQuery.queryWithInputEOLQuery("return MethodDeclaration.all.size();"));
        Assert.assertEquals(1297, hawkQuery.queryWithInputEOLQuery("return ClassDeclaration.all.size();"));
        hawkQuery.close();
    }
}

