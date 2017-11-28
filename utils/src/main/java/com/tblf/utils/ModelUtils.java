package com.tblf.utils;

import org.apache.commons.io.FileUtils;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.gmt.modisco.java.*;
import org.eclipse.gmt.modisco.java.Package;
import org.eclipse.gmt.modisco.java.emf.JavaPackage;
import org.eclipse.gmt.modisco.omg.kdm.kdm.KdmPackage;
import org.eclipse.modisco.java.composition.javaapplication.JavaapplicationPackage;
import org.eclipse.modisco.kdm.source.extension.ExtensionPackage;
import org.eclipse.ocl.OCL;
import org.eclipse.ocl.ParserException;
import org.eclipse.ocl.Query;
import org.eclipse.ocl.ecore.EcoreEnvironmentFactory;
import org.eclipse.ocl.expressions.OCLExpression;
import org.eclipse.ocl.helper.OCLHelper;
import org.eclipse.ocl.options.ParsingOptions;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by Thibault on 18/09/2017.
 */
public class ModelUtils {

    private static final Logger LOGGER = Logger.getAnonymousLogger();
    private static final OCLHelper OCL_HELPER;
    private static final OCL ocl;

    static
    {
        JavaPackage.eINSTANCE.eClass();
        JavaapplicationPackage.eINSTANCE.eClass();
        ExtensionPackage.eINSTANCE.eClass();
        KdmPackage.eINSTANCE.eClass();

        Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
        Map<String, Object> m = reg.getExtensionToFactoryMap();
        m.put("xmi", new XMIResourceFactoryImpl());

        ocl = OCL.newInstance(EcoreEnvironmentFactory.INSTANCE);
        OCL_HELPER = ocl.createOCLHelper();
        OCL_HELPER.setContext(JavaPackage.eINSTANCE.getEClassifier("Model"));

        ParsingOptions.setOption(ocl.getEnvironment(),
                ParsingOptions.implicitRootClass(ocl.getEnvironment()),
                EcorePackage.Literals.EOBJECT);

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
     * Load the JavaApplication model xmi
     * @param f the directory containing the fragments
     * @return the created {@link Resource} containing the model
     * @throws IOException if the file is incorrect, or the xmi cannot be found
     */
    public static Resource loadJavaApplicationModel(File f) throws IOException {
        if (f.exists() && f.isDirectory()) {
            ResourceSet resourceSet = new ResourceSetImpl();

            try (Stream<Path> stream = Files.walk(f.toPath())) {
                stream.filter(path -> path.toString().endsWith(".xmi")).forEach(path -> {
                    try {
                        LOGGER.info("Adding the model "+path+" to the resourceSet");
                        resourceSet.getResource(URI.createURI(path.toUri().toURL().toString()), true);
                    } catch (MalformedURLException e) {
                        LOGGER.warning("Cannot load the xmi: "+path);
                    }
                });
            }

            return resourceSet.getResources().stream().filter(resource -> resource.getURI().toString().endsWith("Package2Directory_java2kdm.xmi")).findFirst().orElseThrow(() -> new FileNotFoundException("Cannot load the resource"));
        } else {
            throw new NoSuchFileException("The file does not exist");
        }
    }

    /**
     * Load all the xmi of a folder except the _kdm model. Indeed, This model is enormous and
     * @param file the root folder
     * @return the {@link ResourceSet}
     */
    public static ResourceSet addJavaApplicationModelFragments(File file, ResourceSet resourceSet) throws IOException {

        try (Stream<Path> files = Files.walk(file.toPath(), 2)) {
            files.filter(path -> path.toString().endsWith("java2kdm.xmi"))
                .forEach(path -> {
                    try {
                        LOGGER.fine("Adding the model "+path+" to the resourceSet");
                        resourceSet.getResource(URI.createURI(path.toUri().toURL().toString()), true);
                    } catch (MalformedURLException e) {
                        LOGGER.warning("Cannot load the xmi: "+path);
                    }
                });
        }

        return resourceSet;
    }

    /**
     * Takes a zipped older of java2kdm fragments and load it into a resourceSet
     * @param zipFile
     * @return
     * @throws IOException
     */
    public static Resource loadJavaApplicationModelFromZip(File zipFile) throws IOException {
        List<File> files = unzip(zipFile);

        File directory = files.get(0).getParentFile();

        return loadJavaApplicationModel(directory);
    }

    /**
     * Unzip a model zipped and return the list of all the files it contains.
     * (The zip could contain any kind of files, but we use it for xmi model compressed as zips)
     * @param zip
     * @return
     * @throws IOException
     */
    public static List<File> unzip(File zip) throws IOException {

        ZipEntry zipEntry;
        List<File> filesUnzipped = new ArrayList<>();

        FileInputStream fileInputStream = new FileInputStream(zip);
        try (ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(fileInputStream))) {

            final int BUFFER = 2048;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                LOGGER.fine("Extracting: " + zipEntry);

                File file = FileUtils.getFile(zip.getParentFile(), zipEntry.toString());

                if (zipEntry.toString().endsWith("/")) {
                    file.mkdir();
                } else {
                    filesUnzipped.add(file);

                    int count;
                    byte data[] = new byte[BUFFER];

                    FileOutputStream fileOutputStream = new FileOutputStream(file);

                    try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream, BUFFER)) {
                        while ((count = zipInputStream.read(data, 0, BUFFER)) != -1) {
                            bufferedOutputStream.write(data, 0, count);
                        }

                        bufferedOutputStream.flush();
                        bufferedOutputStream.close();
                    }
                }
            }
            zipInputStream.close();
        }

        return filesUnzipped;
    }


    /**
     * Gather all the test classes from a {@link Model} using OCL queries
     * A {@link ClassDeclaration} is considered as a test class if it had JUnit test annotations
     * @param model
     * @return
     */
    public static Collection<ClassDeclaration> queryForTestClasses(Resource model) {
        Collection<ClassDeclaration> classDeclarations = new HashSet<>();

        model.getAllContents().forEachRemaining(eObject -> {
            if (eObject instanceof ClassDeclaration && isATestClass((ClassDeclaration) eObject)) {
                classDeclarations.add((ClassDeclaration) eObject);
            }
        });

        return classDeclarations;
    }

    /**
     * Return all the {@link ClassDeclaration} from a Model using an OCL query
     * @param model
     * @return
     */
    public static Collection<ClassDeclaration> queryForAllClasses(Resource model) {

        Model model1 = (Model) model.getContents().get(0);
        OCLExpression query = null;
        try {
            query = OCL_HELPER.createQuery("ClassDeclaration.allInstances()");
        } catch (ParserException e) {
            LOGGER.warning("Error in the OCL query" + e);
        }
        return ((Collection<ClassDeclaration>) ocl.createQuery(query).evaluate(model1)).stream().collect(Collectors.toSet());
    }

    /**
     * Check if a class is a test class by verifying its methods annotations
     * @param clazz a {@link ClassDeclaration}
     * @return true if it is a test class
     */
    public static boolean isATestClass(ClassDeclaration clazz) {

        return clazz.getBodyDeclarations()
                .stream()
                .anyMatch(bodyDeclaration -> bodyDeclaration.getAnnotations()
                        .stream()
                        .anyMatch(ModelUtils::isATestAnnotation))
                || isATestClassRec(clazz);
    }

    /**
     * Check if a class is a test class by verifying if it extends the class TestCase, or a class extending TestCase recursively
     * @param type a {@link Type}
     * @return true if it is a test class
     */
    public static boolean isATestClassRec(Type type) {
        if (type instanceof ClassDeclaration) {
            ClassDeclaration classDeclaration = (ClassDeclaration) type;
            if (classDeclaration.getSuperClass() != null) {
                return isATestClassRec(classDeclaration.getSuperClass().getType());
            } else {
                return false;
            }
        } else {
            return type.getName().equals("TestCase");
        }
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
     * @param classDeclaration a {@link ClassDeclaration}
     * @return the {@link File}
     */
    public static File getSrcFromClass(ClassDeclaration classDeclaration) {
        return new File(classDeclaration.getOriginalCompilationUnit().getOriginalFilePath());
    }

    /**
     * From an {@link EObject} recursively analyse the eContainer in order to build the full qualified name of a
     * {@link ClassDeclaration}. Packages separated with the '.' character, and internal classes using the "$" character
     * @param eObject an {@link EObject}
     * @return the qualifiedName as a {@link String}
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

    /**
     * Iterate over a folder files to add all the xmi inside a single resource set
     * @param folder the folder as a {@link File} where isDirectory = true
     * @return the {@link ResourceSet}
     */
    public static ResourceSet buildResourceSet(File folder) {
        ResourceSet resourceSet = new ResourceSetImpl();
        try {
            try (Stream<Path> stream = Files.walk(folder.toPath())) {
                stream.filter(path -> path.toString().endsWith(Configuration.getProperty("modelFormat"))).forEach(path -> {
                    try {
                        resourceSet.getResource(URI.createURI(path.toUri().toURL().toString()), true);
                    } catch (MalformedURLException e) {
                        LOGGER.log(Level.WARNING, "Cannot load the resource "+path.toString(), e);
                    }
                });
            }

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't load the resources", e);
        }

        return resourceSet;
    }

    /**
     * Iterate over a resourceSet to find the JavaApplication model corresponding to the package qualified name
     * @param pkgQualifiedName the Package qualified name as a {@link String}
     * @param resourceSet a {@link ResourceSet}
     * @return the {@link Resource}
     */
    public static Resource getPackageResource(String pkgQualifiedName, ResourceSet resourceSet) throws IOException {
        return resourceSet.getResources()
                .stream()
                .filter(resource -> resource.getURI().lastSegment().contains(pkgQualifiedName+"_java2kdm"))
                .findFirst()
                .orElseThrow(() -> new IOException("Cannot find the package "+pkgQualifiedName+" in the Model"));
    }

    /**
     * Check if the MoDisco model has been built in the project {@link File}
     * @param project the {@link File} directory containing the project
     * @return true if the model exists, false otherwise
     */
    public static boolean isModelLoaded(File project) throws IOException {
        try(Stream<Path> stream = Files.walk(project.toPath())) {
            return stream.filter(path -> path.toString().endsWith(".xmi")).count() > 0;
        }
    }

}
