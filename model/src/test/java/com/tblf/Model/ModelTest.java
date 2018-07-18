/**
 */
package com.tblf.Model;

import com.tblf.model.Model;
import com.tblf.model.ModelFactory;
import junit.framework.TestCase;
import junit.textui.TestRunner;
import org.junit.Ignore;

/**
 * <!-- begin-user-doc -->
 * A test case for the model object '<em><b>Model</b></em>'.
 * <!-- end-user-doc -->
 * @generated
 */
@Ignore
public class ModelTest extends TestCase {

	/**
	 * The fixture for this Model test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected Model fixture = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static void main(String[] args) {
		TestRunner.run(ModelTest.class);
	}

	/**
	 * Constructs a new Model test case with the given name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ModelTest(String name) {
		super(name);
	}

	/**
	 * Sets the fixture for this Model test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void setFixture(Model fixture) {
		this.fixture = fixture;
	}

	/**
	 * Returns the fixture for this Model test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected Model getFixture() {
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
		setFixture(ModelFactory.eINSTANCE.createModel());
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

} //ModelTest
