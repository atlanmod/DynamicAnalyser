package com.tblf.main;

import com.tblf.Model.ModelPackage;
import com.tblf.business.Manager;
import com.tblf.diff.GitCaller;
import com.tblf.parsing.ModelParser;
import com.tblf.runner.JUnitRunner;
import com.tblf.util.Configuration;
import com.tblf.util.ModelUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.gmt.modisco.java.MethodDeclaration;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws Exception {
        Configuration.setProperty("mode", "BYTECODE");
        Configuration.setProperty("sutBinaries", "/target/classes");
        Configuration.setProperty("testBinaries", "/target/test-classes");
        File file = new File("/home/thibault/Documents/git/JsonPullParser/jsonpullparser-core");

        Manager manager = new Manager();
        File trace = manager.buildTraces(file);
        Resource resource = manager.parseTraces(trace);

        //File model = new File("/home/thibault/Documents/git/assertj-core/analysis.xmi");

        Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
        Map<String, Object> m = reg.getExtensionToFactoryMap();
        m.put("xmi", new XMIResourceFactoryImpl());
        m.put(ModelPackage.eNS_URI, ModelPackage.eINSTANCE);

        GitCaller gitCaller = new GitCaller(file, resource.getResourceSet());
        gitCaller.compareCommits("HEAD~1");

        Set<MethodDeclaration> methodDeclarationSet = gitCaller.getTestToRun();
        File tests = new File(file, "target/test-classes");
        File targets = new File(file, "target/classes");

        ClassLoader classLoader = new URLClassLoader(new URL[]{tests.toURI().toURL(), targets.toURI().toURL()});

        Collection<Map.Entry<String, String>> entries = methodDeclarationSet.stream().map(methodDeclaration -> new AbstractMap.SimpleEntry<>(methodDeclaration.getName(), ModelUtils.getQualifiedName(methodDeclaration.eContainer()))).collect(Collectors.toList());
        JUnitRunner jUnitRunner = new JUnitRunner(classLoader);

        jUnitRunner.runTestMethods(entries);

        ModelParser modelParser = new ModelParser();
        modelParser.parse(resource.getResourceSet().getResources().stream().filter(resource1 -> resource1.getURI().toString().contains("_java.xmi")).findFirst().get());

        jUnitRunner.runTests(modelParser.getTests().keySet());
    }
}
