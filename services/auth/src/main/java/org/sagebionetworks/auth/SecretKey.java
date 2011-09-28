package org.sagebionetworks.auth;

/**
 * Data transfer object for a secret key
 */
public class SecretKey {
	private String secretKey;

	/**
	 * @return the secretKey
	 */
	public String getSecretKey() {
		return secretKey;
	}

	/**
	 * @param secretKey the secretKey to set
	 */
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public SecretKey(String secretKey) {
		super();
		this.secretKey = secretKey;
	}
	
}
