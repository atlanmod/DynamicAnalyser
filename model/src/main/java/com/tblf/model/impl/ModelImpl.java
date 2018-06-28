/**
 */
package com.tblf.model.impl;

import com.tblf.model.Analysis;
import com.tblf.model.Model;
import com.tblf.model.ModelPackage;
import fr.inria.atlanmod.neoemf.core.DefaultPersistentEObject;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Model</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link ModelImpl#getAnalyses <em>Analyses</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ModelImpl extends DefaultPersistentEObject implements Model {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ModelImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ModelPackage.Literals.MODEL;
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
	@SuppressWarnings("unchecked")
	public EList<Analysis> getAnalyses() {
		return (EList<Analysis>)eGet(ModelPackage.Literals.MODEL__ANALYSES, true);
	}

} //ModelImpl
