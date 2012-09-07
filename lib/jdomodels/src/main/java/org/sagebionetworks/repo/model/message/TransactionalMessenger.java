package org.sagebionetworks.repo.model.message;

import java.util.List;

import org.sagebionetworks.repo.model.message.ChangeMessage;

/**
 * Sends messages as part of a transaction.
 * @author John
 *
 */
public interface TransactionalMessenger {
	
	/**
	 * Send the passed message after the current transaction commits.
	 * 
	 * @param message
	 */
	public void sendMessageAfterCommit(ChangeMessage message);
	
	/**
	 * Register an observer that will be notified when there is a message after a commit.
	 * 
	 * @param observer
	 */
	public void registerObserver(TransactionalMessengerObserver observer);
	
	
	/**
	 * Remove an observer.
	 * @param observer
	 * @return true if observer was registered.
	 */
	public boolean removeObserver(TransactionalMessengerObserver observer);
	
	/**
	 * Get an immutable list of all TransactionalMessengerObservers
	 * @return
	 */
	public List<TransactionalMessengerObserver> getAllObservers();

}
