package org.sagebionetworks.repo.model.message;

import java.util.List;
import java.util.Map;

import org.springframework.transaction.support.TransactionSynchronization;

/**
 * The Spring implementation of TransactionSynchronizationManager uses all static methods making 
 * it difficult to apply IoC (Inversion of Control) and therefore test. This provides a simple abstraction from 
 * TransactionSynchronizationManager that is IoC friendly.
 * @author jmhill
 *
 */
public interface TransactionSynchronizationProxy {
	
	/**
	 * @see http://static.springsource.org/spring/docs/3.0.x/javadoc-api/org/springframework/transaction/support/TransactionSynchronizationManager.html#isActualTransactionActive()
	 * @return
	 */
	boolean isActualTransactionActive();

	/**
	 * @see http://static.springsource.org/spring/docs/3.0.x/javadoc-api/org/springframework/transaction/support/TransactionSynchronizationManager.html#isSynchronizationActive()
	 * @return
	 */
	boolean isSynchronizationActive();

	/**
	 * @see http://static.springsource.org/spring/docs/3.0.x/javadoc-api/org/springframework/transaction/support/TransactionSynchronizationManager.html#bindResource(java.lang.Object,%20java.lang.Object)
	 * @param key
	 * @param value
	 */
	void bindResource(String key, Map<?, ?> value);

	/**
	 * @See http://static.springsource.org/spring/docs/3.0.x/javadoc-api/org/springframework/transaction/support/TransactionSynchronizationManager.html#getResource(java.lang.Object)
	 * @param key
	 * @return
	 */
	Map<?, ?> getResource(String key);

	/**
	 * @see http://static.springsource.org/spring/docs/3.0.x/javadoc-api/org/springframework/transaction/support/TransactionSynchronizationManager.html#getSynchronizations()
	 * @return
	 */
	List<TransactionSynchronization> getSynchronizations();

	/**
	 * @see http://static.springsource.org/spring/docs/3.0.x/javadoc-api/org/springframework/transaction/support/TransactionSynchronizationManager.html#registerSynchronization(org.springframework.transaction.support.TransactionSynchronization)
	 * @param synchronizationHandler
	 */
	void registerSynchronization(TransactionSynchronization synchronizationHandler);

	/**
	 * @see http://static.springsource.org/spring/docs/3.0.x/javadoc-api/org/springframework/transaction/support/TransactionSynchronizationManager.html#unbindResourceIfPossible(java.lang.Object)
	 * @param key
	 */
	void unbindResourceIfPossible(String key);

}
