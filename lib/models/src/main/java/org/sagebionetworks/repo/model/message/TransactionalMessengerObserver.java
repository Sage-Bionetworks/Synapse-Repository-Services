package org.sagebionetworks.repo.model.message;

/**
 * The observer pattern applied to Transactional Messenger.
 * @author John
 *
 */
public interface TransactionalMessengerObserver {
	
	/**
	 * This method will be called for every message in the queue after 
	 * the current transaction is committed.  If the transaction is rolled back
	 * then no message will be fired.
	 * @param message
	 */
	public void fireChangeMessage(ChangeMessage message);
}
