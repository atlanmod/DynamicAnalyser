package com.tblf.parsing;

import com.tblf.Model.ModelPackage;
import com.tblf.utils.ModelUtils;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.gmt.modisco.java.ClassDeclaration;
import org.eclipse.gmt.modisco.java.Model;
import org.eclipse.gmt.modisco.java.Package;
import org.eclipse.gmt.modisco.java.emf.JavaPackage;
import org.eclipse.ocl.OCL;
import org.eclipse.ocl.ParserException;
import org.eclipse.ocl.Query;
import org.eclipse.ocl.ecore.Constraint;
import org.eclipse.ocl.ecore.EcoreEnvironmentFactory;
import org.eclipse.ocl.ecore.StringLiteralExp;
import org.eclipse.ocl.helper.OCLHelper;
import org.eclipse.ocl.options.ParsingOptions;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Thibault on 19/09/2017.
 */
public class ModelParserTest {

    @Test
    public void checkParse() throws Exception {
        Resource model = ModelUtils.loadModelFromZip(new File("src/test/resources/models/junit_java.zip"));

        Map<String, File> tests = new HashMap<>();
        Map<String, File> targets = new HashMap<>();

        model.getAllContents().forEachRemaining(object -> {
            if (object instanceof ClassDeclaration) {
                ClassDeclaration classDeclaration = (ClassDeclaration) object;

                if (classDeclaration.getOriginalCompilationUnit() != null) {
                    File file = ModelUtils.getSrcFromClass(classDeclaration);

                    if (ModelUtils.isATestClass(classDeclaration)) {
                        tests.put(ModelUtils.getQualifiedName(classDeclaration), file);
                    } else {
                        targets.put(ModelUtils.getQualifiedName(classDeclaration), file);
                    }
                }
            }
        });

        ModelParser modelParser = new ModelParser();
        modelParser.parse(model);

        Assert.assertFalse(modelParser.getTargets().isEmpty());
        Assert.assertFalse(modelParser.getTests().isEmpty());

        Assert.assertEquals("Different number of targets", targets.size(), modelParser.getTargets().size());
        Assert.assertEquals("Different number of tests", tests.size(), modelParser.getTests().size());
    }

    @Test
    public void checkRequestParametrized() throws IOException {
        Resource model = ModelUtils.loadModelFromZip(new File("src/test/resources/models/junit_java.zip"));
        Model model1 = (Model) model.getContents().get(0);

        Assert.assertNotNull(model1);

        EcoreEnvironmentFactory ecoreEnvironmentFactory = new EcoreEnvironmentFactory(EPackage.Registry.INSTANCE);

        OCL ocl = OCL.newInstance(ecoreEnvironmentFactory);
        OCLHelper OCL_HELPER = ocl.createOCLHelper();

        ParsingOptions.setOption(ocl.getEnvironment(),
                ParsingOptions.implicitRootClass(ocl.getEnvironment()),
                EcorePackage.Literals.EOBJECT);

        OCL_HELPER.setContext(JavaPackage.eINSTANCE.getEClassifier("Model"));

        try {
            Object o = OCL_HELPER.defineAttribute("value : String = 'blahblah'"); //Defining the value
            String queryAsString = "self.ownedElements -> select ( name = value )";

            Query query1 = ocl.createQuery(OCL_HELPER.createQuery(queryAsString));

            ((StringLiteralExp) ((Constraint) OCL_HELPER.getOCL().getConstraints().get(0)).getSpecification().getBodyExpression()).setStringSymbol("junit");
            Object pkg = ((Set<Package>) query1.evaluate(model1)).stream().findFirst().get();
            Package aPackage = (Package) pkg;
            Assert.assertTrue(aPackage.getName().equals("junit"));

            ((StringLiteralExp) ((Constraint) OCL_HELPER.getOCL().getConstraints().get(0)).getSpecification().getBodyExpression()).setStringSymbol("org");
            pkg = ((Set<Package>) query1.evaluate(model1)).stream().findFirst().get();
            aPackage = (Package) pkg;
            Assert.assertTrue(aPackage.getName().equals("org"));
        } catch (ParserException e) {
            Assert.fail(Arrays.toString(e.getStackTrace()));
        }
    }

    @Ignore
    @Test
    public void checkOperation() throws IOException, ParserException, InvocationTargetException {
        Resource model = ModelUtils.loadModelFromZip(new File("src/test/resources/models/junit_java.zip"));
        Model model1 = (Model) model.getContents().get(0);

        Assert.assertNotNull(model1);

        EcoreEnvironmentFactory ecoreEnvironmentFactory = new EcoreEnvironmentFactory(EPackage.Registry.INSTANCE);

        OCL ocl = OCL.newInstance(ecoreEnvironmentFactory);
        OCLHelper OCL_HELPER = ocl.createOCLHelper();

        ParsingOptions.setOption(ocl.getEnvironment(),
                ParsingOptions.implicitRootClass(ocl.getEnvironment()),
                EcorePackage.Literals.EOBJECT);

        OCL_HELPER.setContext(JavaPackage.eINSTANCE.getEClassifier("Model"));

        EOperation eOperation = (EOperation) OCL_HELPER.defineOperation("operation(a : String) : Set(Package) = self.ownedElements -> select(name = a)");
        model1.eClass().getEOperations().add(eOperation);

        EList eList = new BasicEList();
        eList.add("junit");

        ModelPackage.Literals.MODEL.getEOperations().add(eOperation);
        EcorePackage.Literals.EOBJECT.getEOperations().add(eOperation);

        Assert.assertNotNull(model1.eInvoke(eOperation, eList));

    }

}
