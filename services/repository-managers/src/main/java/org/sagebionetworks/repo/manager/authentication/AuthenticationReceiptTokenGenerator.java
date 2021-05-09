package org.sagebionetworks.repo.manager.authentication;

public interface AuthenticationReceiptTokenGenerator {

	/**
	 * Is the given authentication receipt valid?
	 * @param authenticationReceipt
	 * @return
	 */
	boolean isReceiptValid(long principalId, String authenticationReceipt);

	/**
	 * Generate a new authentication receipt for the given user.
	 * @param principalId
	 * @return
	 */
	String createNewAuthenticationReciept(long principalId);

}
