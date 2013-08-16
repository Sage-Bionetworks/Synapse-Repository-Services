package org.sagebionetworks.repo.model.dbo.dao.transactions;

/**
 * 
 * @author John
 *
 */
interface TransactionValidator {
	
	/**
	 * This is a test method that allows us to test the Spring transaction settings.
	 * 
	 * @param id The ID to set.
	 * @param value
	 * @param toThrow
	 * @return
	 * @throws Exception
	 */
	public String setString(Long id, String value, Throwable toThrow) throws Throwable;
	
	/**
	 * Get the current value for the given ID
	 * @param id
	 * @return
	 */
	public String getString(Long id);
}
