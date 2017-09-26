package com.tblf.util;

import org.apache.commons.io.FileUtils;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.gmt.modisco.java.Annotation;
import org.eclipse.gmt.modisco.java.ClassDeclaration;
import org.eclipse.gmt.modisco.java.Model;
import org.eclipse.gmt.modisco.java.Package;
import org.eclipse.gmt.modisco.java.emf.JavaPackage;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by Thibault on 18/09/2017.
 */
public class ModelUtils {

    private static final Logger LOGGER = Logger.getAnonymousLogger();

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
    public static Resource loadModel(File f) throws IOException {

        if (f.exists()) {

            ResourceSet resourceSet = new ResourceSetImpl();

            return resourceSet.getResource(URI.createURI(f.toURI().toURL().toString()), true);
        } else {
            throw new NoSuchFileException("The file does not exist!");
        }
    }

    /**
     * Unzip a zip file using the {@link ModelUtils}.unzip() function and return the resource contains in the XMI.
     * If the zip contains multiple XMI, this function will only return a random one.
     * @param zipFile
     * @return a {@link Resource} containing the model
     * @throws Exception
     */
    public static Resource loadModelFromZip(File zipFile) throws IOException {

        List<File> files = unzip(zipFile);

        Optional<File> xmiFile = files.stream().filter(file -> file.toString().endsWith(".xmi")).findAny();

        if (xmiFile.isPresent()) {
            return loadModel(xmiFile.get());
        } else {
            return null;
        }
    }

    /**
     * Unzip a model zipped and return the list of all the files it contains.
     * (The zip could contain any kind of files, but we use it for xmi model compressed as zips)
     * @param zip
     * @return
     * @throws IOException
     */
    public static List<File> unzip(File zip) throws IOException {
        BufferedOutputStream bufferedOutputStream;
        FileInputStream fileInputStream = new FileInputStream(zip);
        ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(fileInputStream));
        ZipEntry zipEntry;

        List<File> filesUnzipped = new ArrayList<>();

        final int BUFFER = 2048;

        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            LOGGER.info("Extracting: "+ zipEntry);

            File file = FileUtils.getFile(zip.getParentFile(), zipEntry.toString());
            filesUnzipped.add(file);

            int count;
            byte data[] = new byte[BUFFER];

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream, BUFFER);

            while ((count = zipInputStream.read(data, 0, BUFFER)) != -1) {
                bufferedOutputStream.write(data,0, count);
            }

            bufferedOutputStream.flush();
            bufferedOutputStream.close();
        }
        zipInputStream.close();

        return filesUnzipped;
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
