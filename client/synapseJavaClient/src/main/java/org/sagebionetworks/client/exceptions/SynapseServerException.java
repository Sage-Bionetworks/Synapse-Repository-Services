/**
 * 
 */
package org.sagebionetworks.client.exceptions;


/**
 * 
 * SynapseException for problems occurring server-side.  Whenever possible methods should 
 * throw typed subclasses of this exception which correspond to specific status codes. 
 * 
 * @author deflaux
 *
 */
public class SynapseServerException extends SynapseException {
	/**
	 * The http response code associated with a server-side exception
	 */
	private int statusCode;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	public int getStatusCode() {return statusCode;}

	/**
	 * 
	 */
	public SynapseServerException(int httpStatus) {
		this.statusCode=httpStatus;
	}

	/**
	 * @param arg0
	 */
	public SynapseServerException(int httpStatus, String arg0) {
		super(arg0);
		this.statusCode=httpStatus;
	}

	/**
	 * @param arg0
	 */
	public SynapseServerException(int httpStatus, Throwable arg0) {
		super(arg0);
		this.statusCode=httpStatus;
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public SynapseServerException(int httpStatus, String arg0, Throwable arg1) {
		super(arg0, arg1);
		this.statusCode=httpStatus;
	}

}
