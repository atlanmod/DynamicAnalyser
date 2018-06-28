package com.tblf.parsing.queries;

import com.tblf.parsing.queries.Query;
import org.apache.commons.lang.NotImplementedException;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;
import org.eclipse.modisco.kdm.source.extension.ASTNodeSourceRegion;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Method used to compute all the requests using OCL queries
 */
public class OCLStatementQuery implements Query {
    private static final Logger LOGGER = Logger.getLogger("OCLStatementQuery");

/*    private OCLHelper<org.eclipse.emf.ecore.EClassifier, org.eclipse.emf.ecore.EOperation, org.eclipse.emf.ecore.EStructuralFeature, Constraint> OCL_HELPER;
    private OCL ocl;

    private OCL.Query lineExpr;
    private OCL.Query positionExpr;*/

    /**
     * Constructor initializing OCL
     */
    public OCLStatementQuery() {
/*
        EcoreEnvironmentFactory ecoreEnvironmentFactory = new EcoreEnvironmentFactory(EPackage.Registry.INSTANCE);

        ocl = OCL.newInstance(ecoreEnvironmentFactory);
        OCL_HELPER = ocl.createOCLHelper();

        ParsingOptions.setOption(ocl.getEnvironment(),
                ParsingOptions.implicitRootClass(ocl.getEnvironment()),
                EcorePackage.Literals.EOBJECT);

        OCL_HELPER.setContext(JavaapplicationPackage.eINSTANCE.getEClassifier("Java2File"));*/
    }

    @Override
    public Collection<ASTNodeSourceRegion> queryLine(int lineStart, int lineEnd, Java2File java2File) {

/*
        Collection<ASTNodeSourceRegion> results;
        try {

            if (lineExpr == null) {
                OCL_HELPER.defineAttribute("lineStart : Integer = " + lineStart);
                OCL_HELPER.defineAttribute("lineEnd : Integer = " + lineEnd);

                String queryAsString = "self.children -> select (startLine = lineStart) -> select (endLine = lineEnd) -> select (node.oclIsKindOf(java::Statement))";

                OCLExpression<org.eclipse.emf.ecore.EClassifier> oclExpression = OCL_HELPER.createQuery(queryAsString);
                lineExpr = ocl.createQuery(oclExpression);
            } else {
                ((IntegerLiteralExp) ocl.getConstraints().get(0).getSpecification().getBodyExpression()).setIntegerSymbol(lineStart);
                ((IntegerLiteralExp) ocl.getConstraints().get(1).getSpecification().getBodyExpression()).setIntegerSymbol(lineEnd);
            }

            results = (Collection<ASTNodeSourceRegion>) lineExpr.evaluate(java2File);
        } catch (ParserException e) {
            LOGGER.warning("Couldn't create the OCL request to find the statement in the model " + Arrays.toString(e.getStackTrace()));
            results = new ArrayList<>();
        }
        return results;
*/
        throw new NotImplementedException();
    }


    @Override
    public Collection<ASTNodeSourceRegion> queryPosition(int startPos, int endPos, Java2File java2File) {

/*
        Collection<ASTNodeSourceRegion> results;
        try {

            if (positionExpr == null) {
                OCL_HELPER.defineAttribute("posStart : Integer = " + posStart);
                OCL_HELPER.defineAttribute("posEnd : Integer = " + posEnd);

                String queryAsString = "self.children -> select (startPosition = posStart) -> select (endPosition = posEnd) -> select (node.oclIsKindOf(java::Statement))";

                OCLExpression<org.eclipse.emf.ecore.EClassifier> oclExpression = OCL_HELPER.createQuery(queryAsString);
                positionExpr = ocl.createQuery(oclExpression);
            } else {
                ((IntegerLiteralExp) ocl.getConstraints().get(0).getSpecification().getBodyExpression()).setIntegerSymbol(posStart);
                ((IntegerLiteralExp) ocl.getConstraints().get(1).getSpecification().getBodyExpression()).setIntegerSymbol(posEnd);
            }

            results = (Collection<ASTNodeSourceRegion>) positionExpr.evaluate(java2File);
        } catch (ParserException e) {
            LOGGER.warning("Couldn't create the OCL request to find the statement in the model " + Arrays.toString(e.getStackTrace()));
            results = new ArrayList<>();
        }
*/

        throw new NotImplementedException();
    }

}
