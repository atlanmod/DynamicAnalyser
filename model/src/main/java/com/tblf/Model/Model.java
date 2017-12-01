/**
 */
package com.tblf.Model;

import fr.inria.atlanmod.neoemf.core.PersistentEObject;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Model</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link com.tblf.Model.Model#getAnalyses <em>Analyses</em>}</li>
 * </ul>
 *
 * @see com.tblf.Model.ModelPackage#getModel()
 * @model
 * @extends PersistentEObject
 * @generated
 */
public interface Model extends PersistentEObject {
	/**
	 * Returns the value of the '<em><b>Analyses</b></em>' reference list.
	 * The list contents are of type {@link com.tblf.Model.Analysis}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Analyses</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Analyses</em>' reference list.
	 * @see com.tblf.Model.ModelPackage#getModel_Analyses()
	 * @model
	 * @generated
	 */
	EList<Analysis> getAnalyses();

} // Model
