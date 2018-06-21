package com.tblf.parsing;

import com.tblf.parsing.indexer.HawkQuery;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

public class HawkQueryTest {


    @Test
    @Ignore
    public void checkHawkLoadingSimpleProject() {
        Query query = new HawkQuery(new File("src/test/resources/hawk/models/simpleProject"));
        query.queryLine(3, 3, null);
    }

    @Test
    public void checkHawkLoadingComplexProject() {
        try(HawkQuery query = new HawkQuery(new File("src/test/resources/hawk/models/junit"))) {
            query.queryLine(3, 3, null);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

    }
}

