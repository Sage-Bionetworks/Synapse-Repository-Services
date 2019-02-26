package org.sagebionetworks.repo.model.dbo.dao.transactions;

import java.util.concurrent.Callable;

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

	public String setStringLevel2(Long id, String value, Throwable toThrow) throws Throwable;
	
	/**
	 * Get the current value for the given ID
	 * @param id
	 * @return
	 */
	public String getString(Long id);

	public void setStringNoTransaction(Long id, String value);

	public String mandatory(Callable<String> callable) throws Exception;

	public String mandatoryReadCommitted(Callable<String> callable) throws Exception;

	public String required(Callable<String> callable) throws Exception;

	public String requiresNew(Callable<String> callable) throws Exception;
	
	public String writeReadCommitted(Callable<String> callable) throws Exception;

	public String NewWriteTransaction(Callable<String> callable) throws Exception;


}
