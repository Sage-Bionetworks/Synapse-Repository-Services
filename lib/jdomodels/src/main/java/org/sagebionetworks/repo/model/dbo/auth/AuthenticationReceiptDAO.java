package org.sagebionetworks.repo.model.dbo.auth;

public interface AuthenticationReceiptDAO {

	/**
	 * Answer the question, "Has this user logged in and been issue this receipt?"
	 * 
	 * @param userId
	 * @param receipt
	 * @return true if we have a record of this combination in the DB, false otherwise.
	 */
	public boolean isValidReceipt(long userId, String receipt);

	/**
	 * Create a new receipt for this user
	 * 
	 * @param userId
	 * @return the newly created receipt
	 */
	public String createNewReceipt(long userId);

	/**
	 * Replace the old receipt with a new one
	 * 
	 * @param userId
	 * @param oldReceipt
	 * @return the new receipt
	 */
	public String replaceReceipt(long userId, String oldReceipt);
}
