/**
 * 
 */
package org.sagebionetworks.client.exceptions;

import org.apache.http.HttpStatus;

/**
 * @author deflaux
 *
 */
public class SynapseServiceException extends SynapseException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final int STATUS_CODE = HttpStatus.SC_INTERNAL_SERVER_ERROR;

	/**
	 * 
	 */
	public SynapseServiceException() {
		super(STATUS_CODE);
	}

	/**
	 * @param arg0
	 */
	public SynapseServiceException(String arg0) {
		super(STATUS_CODE, arg0);
	}

	/**
	 * @param arg0
	 */
	public SynapseServiceException(Throwable arg0) {
		super(STATUS_CODE, arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public SynapseServiceException(String arg0, Throwable arg1) {
		super(STATUS_CODE, arg0, arg1);
	}

}
