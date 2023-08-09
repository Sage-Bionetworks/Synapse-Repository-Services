package org.sagebionetworks.repo.web;


/**
 * The Listener is used to provide additional information to an access record, from the request body and response body.
 * @author John
 *
 */
public interface AccessRecordDataListener {

	/**
	 * Set the ID of the returned response object.
	 * @param returnObjectId
	 */
	public void setReturnObjectId(String returnObjectId);

	/**
	 * Set the concrete type of the request body
	 * @param requestConcreteType
	 */
	public void setRequestConcreteType(String requestConcreteType);
	
}
