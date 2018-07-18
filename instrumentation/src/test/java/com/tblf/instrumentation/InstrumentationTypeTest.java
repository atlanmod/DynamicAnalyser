package com.tblf.instrumentation;

import org.junit.Assert;
import org.junit.Test;

public class InstrumentationTypeTest {

    @Test
    public void checkCompareString() {
        String s1 = "BYTECODE";
        String s2 = "SOURCECODE";

        Assert.assertEquals(InstrumentationType.valueOf(s1), InstrumentationType.BYTECODE);

        Assert.assertEquals(InstrumentationType.valueOf(s2), InstrumentationType.SOURCECODE);
    }
}
