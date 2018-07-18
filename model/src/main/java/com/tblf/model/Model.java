/**
 */
package com.tblf.model;

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
 *   <li>{@link Model#getAnalyses <em>Analyses</em>}</li>
 * </ul>
 *
 * @see ModelPackage#getModel()
 * @model
 * @extends PersistentEObject
 * @generated
 */
public interface Model extends PersistentEObject {
	/**
	 * Returns the value of the '<em><b>Analyses</b></em>' reference list.
	 * The list contents are of type {@link Analysis}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Analyses</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Analyses</em>' reference list.
	 * @see ModelPackage#getModel_Analyses()
	 * @model
	 * @generated
	 */
	EList<Analysis> getAnalyses();

} // Model
