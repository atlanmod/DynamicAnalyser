/**
 */
package com.tblf.Model;

import com.tblf.model.Analysis;
import com.tblf.model.ModelFactory;

import junit.framework.TestCase;

import junit.textui.TestRunner;
import org.junit.Ignore;

/**
 * <!-- begin-user-doc -->
 * A test case for the model object '<em><b>Analysis</b></em>'.
 * <!-- end-user-doc -->
 * @generated
 */
@Ignore
public class AnalysisTest extends TestCase {

	/**
	 * The fixture for this Analysis test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected Analysis fixture = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static void main(String[] args) {
		TestRunner.run(AnalysisTest.class);
	}

	/**
	 * Constructs a new Analysis test case with the given name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public AnalysisTest(String name) {
		super(name);
	}

	/**
	 * Sets the fixture for this Analysis test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void setFixture(Analysis fixture) {
		this.fixture = fixture;
	}

	/**
	 * Returns the fixture for this Analysis test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected Analysis getFixture() {
		return fixture;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see TestCase#setUp()
	 * @generated
	 */
	@Override
	protected void setUp() throws Exception {
		setFixture(ModelFactory.eINSTANCE.createAnalysis());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see TestCase#tearDown()
	 * @generated
	 */
	@Override
	protected void tearDown() throws Exception {
		setFixture(null);
	}

} //AnalysisTest
