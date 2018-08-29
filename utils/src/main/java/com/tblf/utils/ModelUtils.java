package com.tblf.utils;

import com.tblf.model.ModelPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.gmt.modisco.java.*;
import org.eclipse.gmt.modisco.java.Package;
import org.eclipse.gmt.modisco.java.emf.JavaPackage;
import org.eclipse.gmt.modisco.omg.kdm.kdm.KdmPackage;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;
import org.eclipse.modisco.java.composition.javaapplication.JavaapplicationPackage;
import org.eclipse.modisco.kdm.source.extension.ASTNodeSourceRegion;
import org.eclipse.modisco.kdm.source.extension.ExtensionPackage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by Thibault on 18/09/2017.
 */
public class ModelUtils {

    private static final Logger LOGGER = Logger.getAnonymousLogger();

    static {
        JavaPackage.eINSTANCE.eClass();
        JavaapplicationPackage.eINSTANCE.eClass();
        ExtensionPackage.eINSTANCE.eClass();
        KdmPackage.eINSTANCE.eClass();
        ModelPackage.eINSTANCE.eClass();

        Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
        Map<String, Object> m = reg.getExtensionToFactoryMap();
        m.put("xmi", new XMIResourceFactoryImpl());
    }

    /**
     * Load the XMI model contained in the {@link File} f*
     *
     * @param f the {@link File} ending with XMI
     * @return
     * @throws Exception
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
     *
     * @param zipFile
     * @return a {@link Resource} containing the model
     * @throws Exception
     */
    public static Resource loadModelFromZip(File zipFile) throws IOException {

        List<File> files = com.tblf.utils.FileUtils.unzip(zipFile);

        Optional<File> xmiFile = files.stream().filter(file -> file.toString().endsWith(".xmi")).findAny();

        if (xmiFile.isPresent()) {
            return loadModel(xmiFile.get());
        } else {
            return null;
        }
    }

    /**
     * Load the JavaApplication model xmi
     *
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
                        LOGGER.info("Adding the model " + path + " to the resourceSet");
                        resourceSet.getResource(URI.createURI(path.toUri().toURL().toString()), true);
                    } catch (MalformedURLException e) {
                        LOGGER.warning("Cannot load the xmi: " + path);
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
     *
     * @param file the root folder
     * @return the {@link ResourceSet}
     */
    public static ResourceSet addJavaApplicationModelFragments(File file, ResourceSet resourceSet) throws IOException {

        try (Stream<Path> files = Files.walk(file.toPath(), 2)) {
            files.filter(path -> path.toString().endsWith("java2kdm.xmi"))
                    .forEach(path -> {
                        try {
                            LOGGER.fine("Adding the model " + path + " to the resourceSet");
                            resourceSet.getResource(URI.createURI(path.toUri().toURL().toString()), true);
                        } catch (MalformedURLException e) {
                            LOGGER.warning("Cannot load the xmi: " + path);
                        }
                    });
        }

        return resourceSet;
    }

    /**
     * Takes a zipped older of java2kdm fragments and load it into a resourceSet
     *
     * @param zipFile
     * @return
     * @throws IOException
     */
    public static Resource loadJavaApplicationModelFromZip(File zipFile) throws IOException {
        List<File> files = com.tblf.utils.FileUtils.unzip(zipFile);

        File directory = files.get(0).getParentFile();

        return loadJavaApplicationModel(directory);
    }


    /**
     * Gather all the test classes from a {@link Model} using OCL queries
     * A {@link ClassDeclaration} is considered as a test class if it had JUnit test annotations
     *
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
     * Return all the {@link ClassDeclaration} from a Model
     *
     * @param model a {@link Resource}
     * @return a {@link Set} of {@link ClassDeclaration}s
     */
    public static Collection<ClassDeclaration> queryForAllClasses(Resource model) {
        Model model1 = (Model) model.getContents().get(0);

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(model1.eAllContents(), Spliterator.ORDERED), false)
                .filter(eObject -> eObject instanceof ClassDeclaration)
                .map(eObject -> (ClassDeclaration) eObject)
                .collect(Collectors.toSet());
    }

    /**
     * Check if a class is a test class by verifying its methods annotations
     *
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
     *
     * @param type a {@link Type}
     * @return true if it is a test class
     */
    public static boolean isATestClassRec(Type type) {
        if (type instanceof ClassDeclaration) {
            ClassDeclaration classDeclaration = (ClassDeclaration) type;
            return classDeclaration.getSuperClass() != null && isATestClassRec(classDeclaration.getSuperClass().getType());
        } else {
            return type.getName().equals("TestCase");
        }
    }

    /**
     * Check if an @{@link Annotation} is a test annotation.
     * Add severals checking in this method if you want to enable other test libraries, such as TestNG etc ...
     *
     * @param annotation the {@link Annotation}
     * @return true if it is an annotation
     */
    private static boolean isATestAnnotation(Annotation annotation) {
        return "Test".equals(annotation.getType().getType().getName());
    }

    /**
     * get the source file of the {@link ClassDeclaration} using its {@link org.eclipse.gmt.modisco.java.CompilationUnit}
     *
     * @param classDeclaration a {@link ClassDeclaration}
     * @return the {@link File}
     */
    public static File getSrcFromClass(ClassDeclaration classDeclaration) {
        if (classDeclaration.getOriginalCompilationUnit() == null)
            return null;
        return new File(classDeclaration.getOriginalCompilationUnit().getOriginalFilePath());
    }

    /**
     * From an {@link EObject} recursively analyse the eContainer in order to build the full qualified name of a
     * {@link ClassDeclaration}. Packages separated with the '.' character, and internal classes using the "$" character
     *
     * @param eObject an {@link EObject}
     * @return the qualifiedName as a {@link String}
     */
    public static String getQualifiedName(EObject eObject) {
        EStructuralFeature eStructuralFeature = eObject.eClass().getEStructuralFeature("name");

        if (eStructuralFeature == null || eObject instanceof Model) {
            return "";
        }

        String currentName = (String) eObject.eGet(eStructuralFeature);

        if (eObject.eContainer() instanceof TypeDeclaration) {
            currentName = "$".concat(currentName);
        }

        if (eObject.eContainer() instanceof Package) {
            currentName = ".".concat(currentName);
        }

        return getQualifiedName(eObject.eContainer()) + currentName;
    }

    /**
     * Iterate over a folder files to add all the xmi inside a single resource set
     *
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
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Cannot load the resource " + path.toString(), e);
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
     *
     * @param pkgQualifiedName the Package qualified name as a {@link String}
     * @param resourceSet      a {@link ResourceSet}
     * @return the {@link Resource}
     */
    public static Resource getPackageResource(String pkgQualifiedName, ResourceSet resourceSet) throws IOException {
        return resourceSet.getResources()
                .stream()
                .filter(resource -> resource.getURI().lastSegment().contains(pkgQualifiedName + "_java2kdm"))
                .findFirst()
                .orElseThrow(() -> new IOException("Cannot find the package " + pkgQualifiedName + " in the Model"));
    }

    /**
     * Check if the MoDisco model has been built in the project {@link File}
     *
     * @param project the {@link File} directory containing the project
     * @return true if the model exists, false otherwise
     */
    public static boolean isModelLoaded(File project) throws IOException {
        try (Stream<Path> stream = Files.walk(project.toPath())) {
            return stream.filter(path -> path.toString().endsWith(".xmi")).count() > 0;
        }
    }

    /**
     * Iterate over a {@link Model} to get all the test methods
     *
     * @param model a {@link Model}
     * @return a {@link Collection} of {@link MethodDeclaration}
     */
    public static Collection<MethodDeclaration> getAllTestMethods(Model model) {

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(model.eAllContents(), Spliterator.ORDERED), false)
                .filter(MethodDeclaration.class::isInstance)
                .map(eObject -> ((MethodDeclaration) eObject)) //get methods
                .filter(eObject -> eObject.getAnnotations().stream().anyMatch(ModelUtils::isATestAnnotation)) //get test methods using @Test
                .collect(Collectors.toList()); //collect

    }

    /**
     * Get the {@link ClassDeclaration} containing the given {@link EObject}
     *
     * @param eObject an {@link EObject} such as {@link MethodDeclaration}
     * @return a {@link ClassDeclaration} containing the {@link EObject}
     */
    public static ClassDeclaration getContainerClassDeclaration(EObject eObject) {
        EObject container = eObject.eContainer();
        if (container instanceof ClassDeclaration || container == null)
            return (ClassDeclaration) container;

        return getContainerClassDeclaration(container);
    }

    /**
     * Get all the {@link MethodDeclaration} in a {@link Java2File}
     *
     * @param java2File a {@link Java2File}
     * @return a {@link Collection} of {@link MethodDeclaration}
     */
    public static Collection<MethodDeclaration> getMethodDeclarationFromJava2File(Java2File java2File) {
        return java2File.getChildren()
                .stream()
                .filter(astNodeSourceRegion -> astNodeSourceRegion.getNode() instanceof MethodDeclaration)
                .map(astNodeSourceRegion -> ((MethodDeclaration) astNodeSourceRegion.getNode()))
                .collect(Collectors.toList());
    }

    /**
     * Return the signature of a method as a String, such as:
     * method(String, Integer, Double)
     *
     * @param methodDeclaration a {@link MethodDeclaration} from a Modisco model
     * @return a {@link String}
     */
    public static String getMethodSignature(AbstractMethodDeclaration methodDeclaration) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(methodDeclaration.getName());
        stringBuilder.append("(");
        if (methodDeclaration.getParameters().size() > 0) {
            stringBuilder.append(methodDeclaration.getParameters().get(0).getType().getType().getName());
            for (int i = 1; i < methodDeclaration.getParameters().size(); ++i) {
                stringBuilder.append(", ");
                stringBuilder.append(methodDeclaration.getParameters().get(i).getType().getType().getName());
            }
        }

        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    /**
     * Get the method with the give name in the super classes of the {@link Java2File}
     *
     * @param java2File                  a {@link Java2File}
     * @param methodDeclarationSignature a {@link MethodDeclaration}
     * @return a {@link MethodDeclaration} with the same signature but in a superclass
     */
    public static AbstractMethodDeclaration getOverridenMethod(Java2File java2File, String methodDeclarationSignature) {

        //The classDeclaration cannot be found in the model
        AbstractTypeDeclaration abstractTypeDeclaration = java2File.getJavaUnit().getTypes().stream()
                .filter(abstractTypeDeclaration1 -> abstractTypeDeclaration1 instanceof ClassDeclaration)
                .findFirst()
                .orElse(null);

        if (abstractTypeDeclaration == null) {
            LOGGER.warning("Cannot find the ClassDeclaration in the file " + java2File.getJavaUnit().getOriginalFilePath());
            return null;
        }

        //Get all the methods in the super classes implementing the method with methodDeclarationSignature name
        return getMethodInParentClass((ClassDeclaration) abstractTypeDeclaration, methodDeclarationSignature);
    }

    /**
     * Get a specific {@link MethodDeclaration} in the parent class of a {@link ClassDeclaration}
     * Is recursive until the class has no parent, or the method is found
     * @param classDeclaration a {@link ClassDeclaration}
     * @param methodDeclarationSignature a {@link String}
     * @return the found {@link AbstractMethodDeclaration}
     */
    public static AbstractMethodDeclaration getMethodInParentClass(ClassDeclaration classDeclaration , String methodDeclarationSignature) {
        if (classDeclaration.getSuperClass() == null || !(classDeclaration.getSuperClass().getType() instanceof ClassDeclaration))
            return null;

        ClassDeclaration parentClass = (ClassDeclaration) classDeclaration.getSuperClass().getType();

        return parentClass.getBodyDeclarations()
                .stream()
                .filter(bodyDeclaration -> bodyDeclaration instanceof AbstractMethodDeclaration)
                .map(bodyDeclaration -> ((AbstractMethodDeclaration) bodyDeclaration))
                .filter(bodyDeclaration -> methodDeclarationSignature.equals(getMethodSignature(bodyDeclaration)))
                .findFirst() //Only one element, signature are unique in a class
                .orElse(getMethodInParentClass(parentClass, methodDeclarationSignature));

    }

    /**
     * Get the {@link Java2File} eObject in the MoDisco model from the path of the Class file.
     * @param resourceSet a {@link ResourceSet}
     * @param path the path to a file, as a String
     * @return the {@link Java2File} containing the class file
     * @throws IOException when the {@link Java2File} cannot be found
     */
    public static Java2File getJava2FileInResourceSetFromPathAsString(ResourceSet resourceSet, String path) throws IOException {

        String pkg = ParserUtils.getPackageQNFromFile(new File(path));

        Resource sutPackage = ModelUtils.getPackageResource(pkg, resourceSet);

        //WONT GET NEW TEST FILES ???

        Java2File java2File = (Java2File) sutPackage.getContents()
                .stream()
                .filter(eObject -> eObject instanceof Java2File
                        && ((Java2File) eObject).getJavaUnit().getOriginalFilePath().endsWith(path))
                .findFirst()
                .orElseThrow(() -> new IOException("Could not find "+path+" in the java model. Is it a Java File"));

        return java2File;
    }

    /**
     * Get the {@link ASTNodeSourceRegion} in the model that contains the given {@link EObject}
     * @param java2File a {@link Java2File}
     * @param eObject an {@link EObject}
     * @return the found {@link ASTNodeSourceRegion} or null
     */
    public static ASTNodeSourceRegion getASTNodeFromJavaElementInJava2File(Java2File java2File, EObject eObject) {
        return java2File.getChildren()
                .stream()
                .filter(astNodeSourceRegion -> astNodeSourceRegion.getNode().equals(eObject))
                .findFirst()
                .orElse(null);
    }
}
