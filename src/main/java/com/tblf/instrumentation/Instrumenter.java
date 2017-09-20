package com.tblf.instrumentation;

import java.util.List;

/**
 * Created by Thibault on 20/09/2017.
 */
public interface Instrumenter {

    public void instrument(List<String> targets, List<String> tests);
}
