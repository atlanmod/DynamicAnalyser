package com.tblf.util;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.EStructuralFeatureImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.gmt.modisco.java.Annotation;
import org.eclipse.gmt.modisco.java.ClassDeclaration;
import org.eclipse.gmt.modisco.java.Model;
import org.eclipse.gmt.modisco.java.Package;
import org.eclipse.gmt.modisco.java.emf.JavaPackage;
import org.eclipse.gmt.modisco.java.emf.impl.ModelImpl;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.NoSuchFileException;
import java.util.Map;

/**
 * Created by Thibault on 18/09/2017.
 */
public class ModelUtils {

    static
    {
        System.out.println("Initializing MoDisco java package");
        JavaPackage.eINSTANCE.eClass();
        Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
        Map<String, Object> m = reg.getExtensionToFactoryMap();
        m.put("xmi", new XMIResourceFactoryImpl());
    }

    /**
     * Load the XMI model contained in the {@link File} f*
     * @param f the {@link File} ending with XMI
     * @return
     * @throws Exception
     *
     * @PreCondition The File is a XMI file using the Java Modisco metamodel
     */
    public static Resource loadModel(File f) throws Exception {

        if (f.exists()) {

            ResourceSet resourceSet = new ResourceSetImpl();

            return resourceSet.getResource(URI.createURI(f.toURI().toURL().toString()), true);
        } else {
            throw new NoSuchFileException("The file does not exist!");
        }
    }

    /**
     * Check if a class is a test class by verifying its methods annotations
     * @param clazz
     * @return true if it is a test class
     */
    public static boolean isATestClass(ClassDeclaration clazz) {
        return clazz.getBodyDeclarations()
                .stream()
                .anyMatch(bodyDeclaration -> bodyDeclaration.getAnnotations()
                        .stream()
                        .anyMatch(ModelUtils::isATestAnnotation));
    }

    /**
     * Check if an @{@link Annotation} is a test annotation.
     * Add severals checking in this method if you want to enable other test libraries, such as TestNG etc ...
     * @param annotation the {@link Annotation}
     * @return true if it is an annotation
     */
    private static boolean isATestAnnotation(Annotation annotation) {
        return "Test".equals(annotation.getType().getType().getName());
    }

    /**
     * get the source file of the {@link ClassDeclaration} using its {@link org.eclipse.gmt.modisco.java.CompilationUnit}
     * @param classDeclaration
     * @return
     */
    public static File getSrcFromClass(ClassDeclaration classDeclaration) {
        return new File(classDeclaration.getOriginalCompilationUnit().getOriginalFilePath());
    }

    /**
     * From an {@link EObject} recursively analyse the eContainer in order to build the full qualified name of a
     * {@link ClassDeclaration}. Packages separated with the '.' character, and internal classes using the "$" character
     * @param eObject
     * @return
     */
    public static String getQualifiedName(EObject eObject) {
        EStructuralFeature eStructuralFeature =  eObject.eClass().getEStructuralFeature("name");

        if (eStructuralFeature == null ||eObject instanceof Model) {
            return "";
        }

        String currentName = (String) eObject.eGet(eStructuralFeature);

        if (eObject.eContainer() instanceof ClassDeclaration) {
            currentName = "$".concat(currentName);
        }

        if (eObject.eContainer() instanceof Package) {
            currentName = ".".concat(currentName);
        }

        return getQualifiedName(eObject.eContainer())+currentName;

    }

}
