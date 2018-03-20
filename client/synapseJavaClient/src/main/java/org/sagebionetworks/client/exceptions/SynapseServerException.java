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
public abstract class SynapseServerException extends SynapseException {

	private static final long serialVersionUID = 1L;

	public SynapseServerException() {
		super();
	}
	
	public SynapseServerException(String message) {
		super(message);
	}
	
	public SynapseServerException(String message, Throwable cause) {
		super(message, cause);
	}

	public SynapseServerException(Throwable cause) {
		super(cause);
	}

}
