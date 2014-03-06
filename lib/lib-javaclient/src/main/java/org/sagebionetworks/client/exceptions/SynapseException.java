package org.sagebionetworks.client.exceptions;


/**
 * This is the base class for exceptions thrown by the Synapse Client. There are subclasses for
 * client-side and server-side exceptions.  We make this class abstract so that methods within
 * the Synapse Client are required to be explicit about the reason for the exception.
 * 
 * @author brucehoff
 *
 */
abstract public class SynapseException extends Exception {

	private static final long serialVersionUID = 1L;

	public SynapseException() {
		super();
	}

	public SynapseException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public SynapseException(String arg0) {
		super(arg0);
	}

	public SynapseException(Throwable arg0) {
		super(arg0);
	}
	
}
