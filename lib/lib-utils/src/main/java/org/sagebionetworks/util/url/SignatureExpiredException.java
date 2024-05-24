package org.sagebionetworks.util.url;

public class SignatureExpiredException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private String signature;

	public SignatureExpiredException() {
	}

	/**
	 * Create a new Exception with the message and signature.
	 * @param message
	 * @param signature
	 */
	public SignatureExpiredException(String message, String signature) {
		super(message);
		this.signature = signature;
	}

	/**
	 * The expired signagure.
	 * @return
	 */
	public String getSignature() {
		return signature;
	}

}
