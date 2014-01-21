/**
 * 
 */
package org.sagebionetworks.client.exceptions;


/**
 * @author deflaux
 *
 */
public class SynapseUserException extends SynapseException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public SynapseUserException(int httpStatus) {
		super(httpStatus);
	}

	/**
	 * @param arg0
	 */
	public SynapseUserException(int httpStatus, String arg0) {
		super(httpStatus, arg0);
	}

	/**
	 * @param arg0
	 */
	public SynapseUserException(int httpStatus, Throwable arg0) {
		super(httpStatus, arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public SynapseUserException(int httpStatus, String arg0, Throwable arg1) {
		super(httpStatus, arg0, arg1);

	}

}
