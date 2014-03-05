/**
 * 
 */
package org.sagebionetworks.client.exceptions;


/**
 * 
 * This exception is thrown by the Synapse Client for problems that occur client side
 * (e.g. parsing returned JSON) rather than ones that occur server-side and associated
 * with an http status code.
 * 
 * @author deflaux
 *
 */
public class SynapseClientException extends SynapseException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public SynapseClientException() {
	}

	/**
	 * @param arg0
	 */
	public SynapseClientException(String arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public SynapseClientException(Throwable arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public SynapseClientException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}
