package com.tblf.instrumentation;

import java.util.Collection;

/**
 * Created by Thibault on 20/09/2017.
 */
public interface Instrumenter {

    public void instrument(Collection<String> targets, Collection<String> tests);
}
