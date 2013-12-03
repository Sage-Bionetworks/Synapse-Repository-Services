package org.sagebionetworks.repo.model.message;

import java.util.List;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ObservableEntity;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Sends messages as part of a transaction.
 * @author John
 *
 */
public interface TransactionalMessenger {
	
	/**
	 * Send a change message for the given object
	 * after the current transaction commits.
	 */
	public void sendMessageAfterCommit(String objectId, ObjectType objectType, ChangeType changeType);
	public void sendMessageAfterCommit(String objectId, ObjectType objectType, String etag, ChangeType changeType);
	public void sendMessageAfterCommit(String objectId, ObjectType objectType, String etag, String parentId, ChangeType changeType);
	
	/**
	 * Send a change message fashioned after the passed entity
	 * after the current transaction commits.
	 */
	public void sendMessageAfterCommit(ObservableEntity entity, ChangeType changeType);
	
	/**
	 * Send the passed message after the current transaction commits.
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
	
	/**
	 * Register that a message has been sent.  Any message that has been created but not registered as sent
	 * will be returned by {@link #listUnsentMessages(long)}.  This is used to detect messages that need to be sent
	 * either for the first time or re-sent on a new stacks.
	 * 
	 * @param changeNumber
	 * @throws NotFoundException 
	 */
	public void registerMessageSent(long changeNumber) throws NotFoundException;
	
	/**
	 * List messages that have been created but not registered as sent (see {@link #registerMessageSent(long)}).
	 * This is used to detect messages that need to be sent either for the first time or re-sent on a new stacks.
	 * 
	 * @param limit
	 * @return
	 */
	public List<ChangeMessage> listUnsentMessages(long limit);

}
