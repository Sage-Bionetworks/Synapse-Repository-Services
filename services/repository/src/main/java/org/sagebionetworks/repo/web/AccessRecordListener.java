package org.sagebionetworks.repo.web;


/**
 * Abstraction for a listener to response accessRecord object.
 * @author John
 *
 */
public interface AccessRecordListener {

	/**
	 * Set the ID of the return object.
	 * @param returnObjectId
	 */
	public void setReturnObjectId(String returnObjectId);

	/**
	 * Set the concrete type of the request body
	 * @param requestConcreteType
	 */
	public void setRequestConcreteType(String requestConcreteType);
	
}
