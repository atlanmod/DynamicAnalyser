/**
 */
package com.tblf.Model.impl;

import com.tblf.Model.Analysis;
import com.tblf.Model.ModelPackage;

import fr.inria.atlanmod.neoemf.core.DefaultPersistentEObject;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Analysis</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link com.tblf.Model.impl.AnalysisImpl#getName <em>Name</em>}</li>
 *   <li>{@link com.tblf.Model.impl.AnalysisImpl#getSource <em>Source</em>}</li>
 *   <li>{@link com.tblf.Model.impl.AnalysisImpl#getTarget <em>Target</em>}</li>
 *   <li>{@link com.tblf.Model.impl.AnalysisImpl#getValue <em>Value</em>}</li>
 * </ul>
 *
 * @generated
 */
public class AnalysisImpl extends DefaultPersistentEObject implements Analysis {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected AnalysisImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ModelPackage.Literals.ANALYSIS;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected int eStaticFeatureCount() {
		return 0;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getName() {
		return (String)eGet(ModelPackage.Literals.ANALYSIS__NAME, true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setName(String newName) {
		eSet(ModelPackage.Literals.ANALYSIS__NAME, newName);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EObject getSource() {
		return (EObject)eGet(ModelPackage.Literals.ANALYSIS__SOURCE, true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSource(EObject newSource) {
		eSet(ModelPackage.Literals.ANALYSIS__SOURCE, newSource);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	public EList<EObject> getTarget() {
		return (EList<EObject>)eGet(ModelPackage.Literals.ANALYSIS__TARGET, true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getValue() {
		return (String)eGet(ModelPackage.Literals.ANALYSIS__VALUE, true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setValue(String newValue) {
		eSet(ModelPackage.Literals.ANALYSIS__VALUE, newValue);
	}

} //AnalysisImpl
