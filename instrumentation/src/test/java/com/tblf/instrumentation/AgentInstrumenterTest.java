package com.tblf.instrumentation;

import com.tblf.utils.FileUtils;
import com.tblf.utils.MavenUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class AgentInstrumenterTest {

    @Test
    public void testInstrument() throws IOException, InterruptedException {
        File projectZipToInstrument = new File("src/test/resources/sources/dummyagent.zip");
        FileUtils.unzip(projectZipToInstrument);

        File projectToInstrument = new File("src/test/resources/sources/dummyagent");

        File agent = new File("src/test/resources/jars/Agent.jar");
        assert agent.exists();

        new InstrumenterBuilder()
                .onDirectory(projectToInstrument)
                .withAgentInstrumenter()
                .build()
                .instrument(Collections.singleton(agent));

        MavenUtils.compilePom(new File(projectToInstrument, "pom.xml"));
        MavenUtils.runTestsOnly(new File(projectToInstrument, "pom.xml"));

        org.apache.commons.io.FileUtils.deleteDirectory(projectToInstrument);
    }


}
