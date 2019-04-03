package com.tblf.instrumentation.agent;

import com.tblf.instrumentation.Instrumenter;
import com.tblf.utils.MavenUtils;
import org.apache.commons.lang3.NotImplementedException;

import java.io.File;
import java.util.Collection;
import java.util.stream.Collectors;

public class AgentInstrumenter extends Instrumenter {
    @Override
    public void instrument(Collection<String> targets, Collection<String> tests) {
        throw new NotImplementedException("");
    }

    /**
     * add as javaagent the given objects
     * @param processors a {@link Collection} of {@link java.io.File}
     */
    @Override
    public void instrument(Collection<Object> processors) {
        String commandLine = processors.stream().filter(o -> o instanceof File)
                .map(o -> (File) o)
                .map(f -> "-javaagent:"+f.getAbsolutePath())
                .collect(Collectors.joining(" "));

        File pomToAddAgents = new File(this.directory, "pom.xml");
        MavenUtils.addJVMOptionsToSurefireConfig(pomToAddAgents, commandLine);
    }

    @Override
    public ClassLoader getClassLoader() {
        return Instrumenter.class.getClassLoader();
    }
}
