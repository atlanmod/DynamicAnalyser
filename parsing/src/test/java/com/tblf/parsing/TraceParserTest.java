package com.tblf.parsing;

import com.tblf.Model.Analysis;
import com.tblf.Model.ModelPackage;
import com.tblf.utils.ModelUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.gmt.modisco.java.Statement;
import org.eclipse.gmt.modisco.java.emf.JavaPackage;
import org.eclipse.gmt.modisco.omg.kdm.kdm.KdmPackage;
import org.eclipse.gmt.modisco.omg.kdm.source.SourcePackage;
import org.eclipse.modisco.java.composition.javaapplication.JavaapplicationPackage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by Thibault on 02/10/2017.
 */
public class TraceParserTest {
    private File trace;
    private ResourceSet resourceSet;

    @Before
    public void setUpRealCode() throws IOException {
        resourceSet = new ResourceSetImpl();
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.FINE);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(Level.FINE);
        }

        EPackage.Registry.INSTANCE.put(JavaPackage.eNS_URI, JavaPackage.eINSTANCE);
        EPackage.Registry.INSTANCE.put(JavaapplicationPackage.eNS_URI, JavaapplicationPackage.eINSTANCE);
        EPackage.Registry.INSTANCE.put(KdmPackage.eNS_URI, KdmPackage.eINSTANCE);
        EPackage.Registry.INSTANCE.put(SourcePackage.eNS_URI, SourcePackage.eINSTANCE);
        EPackage.Registry.INSTANCE.put(ModelPackage.eNS_URI, ModelPackage.eINSTANCE);

        Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
        Map<String, Object> m = reg.getExtensionToFactoryMap();
        m.put("xmi", new XMIResourceFactoryImpl());

        Files.walk(new File(".").toPath()).forEach(System.out::println);

        ModelUtils.unzip(new File("./src/test/resources/models/simpleProject.zip"));

        File file = new File("./src/test/resources/models/simpleProject");
        Files.walk(file.toPath())
                .filter(path -> path.toString().endsWith(".xmi") && !path.toString().endsWith("_kdm.xmi"))
                .forEach(path -> {

            try {
                Resource resource = resourceSet.getResource(URI.createURI(path.toUri().toURL().toString()), true);
                System.out.println(resource);
                resourceSet.getResources().add(resource);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

        });

        Assert.assertTrue(resourceSet.getResources().stream().filter(resource -> resource.getContents().get(0) == null).collect(Collectors.toList()).isEmpty());
    }

    @After
    public void tearDown() throws IOException {
        File file = new File("./src/test/resources/models/simpleProject");
        if (file.exists()) {
            FileUtils.deleteDirectory(file);
        }
    }

    @Test
    public void checkParseLineAccuracy() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("&:com.tblf.SimpleProject.AppTest:<init>\n");
        sb.append("&:com.tblf.SimpleProject.AppTest:testApp\n");
        sb.append("%:com.tblf.SimpleProject.App:method\n");
        sb.append("?:5\n");
        sb.append("?:6");

        trace = File.createTempFile("tmpTrace", ".extr");

        Files.write(trace.toPath(), sb.toString().getBytes());

        File file = new File("./src/test/resources/myAnalysisModel.xmi");
        if (file.exists())
            file.delete();

        file.createNewFile();

        TraceParser traceParser = new TraceParser(trace, file, resourceSet);
        Resource resource = traceParser.parse();

        resource.save(Collections.EMPTY_MAP);
        Assert.assertTrue(file.exists());
        Assert.assertEquals(3, resource.getContents().size());
    }

    @Test
    public void checkParsePositionAccuracy() throws IOException {

        StringBuilder sb = new StringBuilder();
        sb.append("&:com.tblf.SimpleProject.AppTest:<init>\n");
        sb.append("&:com.tblf.SimpleProject.AppTest:testApp\n");
        sb.append("%:com.tblf.SimpleProject.App:method\n");
        sb.append("!:97:130\n");
        sb.append("!:134:172");

        trace = File.createTempFile("tmpTrace", ".extr");

        Files.write(trace.toPath(), sb.toString().getBytes());

        File file = new File("./src/test/resources/myAnalysisModel.xmi");
        if (file.exists())
            file.delete();

        file.createNewFile();

        TraceParser traceParser = new TraceParser(trace, file, resourceSet);
        Resource resource = traceParser.parse();

        resource.save(Collections.EMPTY_MAP);
        Assert.assertTrue(file.exists());

        Assert.assertEquals(3, resource.getContents().size());

    }

    @Test
    public void checkParsePositionAccuracyMultipleIdenticalStatements() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("&:com.tblf.SimpleProject.AppTest:<init>\n");
        sb.append("&:com.tblf.SimpleProject.AppTest:testApp\n");
        sb.append("%:com.tblf.SimpleProject.App:method\n");
        sb.append("!:97:130\n");
        sb.append("!:134:172\n");
        sb.append("!:134:172\n");
        sb.append("!:134:172\n");
        sb.append("!:134:172");

        trace = File.createTempFile("tmpTrace", ".extr");

        Files.write(trace.toPath(), sb.toString().getBytes());

        File file = new File("./src/test/resources/myAnalysisModel.xmi");
        if (file.exists())
            file.delete();

        file.createNewFile();

        TraceParser traceParser = new TraceParser(trace, file, resourceSet);
        Resource resource = traceParser.parse();

        resource.save(Collections.EMPTY_MAP);
        Assert.assertTrue(file.exists());

        Assert.assertEquals(3, resource.getContents().size());
    }

    @Test
    public void checkParseLineMultipleAnalysis() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("&:com.tblf.SimpleProject.AppTest:<init>\n");
        sb.append("&:com.tblf.SimpleProject.AppTest:testApp\n");
        sb.append("%:com.tblf.SimpleProject.App:method\n");
        sb.append("?:5\n");
        sb.append("?:6\n");
        sb.append("&:com.tblf.SimpleProject.AppTest:testApp2\n");
        sb.append("%:com.tblf.SimpleProject.App:method\n");
        sb.append("?:5\n");
        sb.append("?:6");

        trace = File.createTempFile("tmpTrace", ".extr");

        Files.write(trace.toPath(), sb.toString().getBytes());

        File file = new File("./src/test/resources/myAnalysisModel.xmi");
        if (file.exists())
            file.delete();

        file.createNewFile();

        TraceParser traceParser = new TraceParser(trace, file, resourceSet);
        Resource resource = traceParser.parse();

        resource.getContents().forEach(eObject -> {
            Analysis analysis = (Analysis) eObject;
            System.out.println(analysis.getSource()+" -> "+analysis.getTarget());
        });

        Map<Statement, Integer> impacts = new HashMap<>();
        resource.getContents().forEach(eObject -> {
            Analysis analysis = (Analysis) eObject;
            if (analysis.getSource() instanceof Statement) {
                Statement statement = (Statement) analysis.getSource();
                impacts.merge(statement, 1, (a, b) -> a + b);
            }
        });

        Map.Entry entry = impacts.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).orElseThrow(() -> new Exception("Couldn't find anything"));

        System.out.println(entry.getKey()+" has "+entry.getValue()+" impacts ");

        Assert.assertEquals(6, resource.getContents().size());
    }

}
