package org.sagebionetworks.repo.model.message;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

/**
 * Basic implementation of TransactionalMessenger.  Messages are bound to the current transaction and thread.
 * The messages will be sent when the current transaction commits.  If the transaction rolls back the messages will not be sent.
 * 
 * This class utilizes TransactionSynchronizationManager for all transaction management.
 * 
 * @author John
 *
 */
public class TransactionalMessengerImpl implements TransactionalMessenger {
	
	static private Log log = LogFactory.getLog(TransactionalMessengerImpl.class);
	
	private static final String TRANSACTIONAL_MESSANGER_IMPL_MESSAGES = "TransactionalMessangerImpl.Messages";
	@Autowired
	DataSourceTransactionManager txManager;
	@Autowired
	DBOChangeDAO changeDAO;
	@Autowired
	TransactionSynchronizationProxy transactionSynchronizationManager;
	
	/**
	 * Used by spring.
	 * 
	 */
	public TransactionalMessengerImpl(){}
	
	/**
	 * For IoC
	 * @param txManager
	 * @param changeDAO
	 * @param transactionSynchronizationManager
	 */
	public TransactionalMessengerImpl(
			DataSourceTransactionManager txManager,
			DBOChangeDAO changeDAO,
			TransactionSynchronizationProxy transactionSynchronizationManager) {
		super();
		this.txManager = txManager;
		this.changeDAO = changeDAO;
		this.transactionSynchronizationManager = transactionSynchronizationManager;
	}

	/**
	 * The list of observers that are notified of messages after a commit.
	 */
	private List<TransactionalMessengerObserver> observers = new LinkedList<TransactionalMessengerObserver>();
	
	@Override
	public void sendMessageAfterCommit(ChangeMessage message) {
		if(message == null) throw new IllegalArgumentException("Message cannot be null");
		// Make sure we are in a transaction.
		if(!transactionSynchronizationManager.isSynchronizationActive()) throw new IllegalStateException("Cannot send a transactional message becasue there is no transaction");
		// Bind this message to the transaction
		// Get the bound list of messages if it already exists.
		Map<String, ChangeMessage> currentMessages = getCurrentBoundMessages();
		// If we already have a message going out for this object then we needs replace it with the latest.
		// If an object's etag changes multiple times, only the final etag should be in the message.
		String key = getObjectKey(message);
		currentMessages.put(key, message);
		// Register a handler if needed
		registerHandlerIfNeeded();
	}
	
	/**
	 * The key we use for the map is the <type>-<id> for the case where two different types of objects have the same id.
	 * @param message
	 * @return
	 */
	public String getObjectKey(ChangeMessage message){
		if(message == null) throw new IllegalArgumentException("Message cannot be null");
		if(message.getObjectId() == null) throw new IllegalArgumentException("message.getObjectId() cannot be null");
		if(message.getObjectType() == null) throw new IllegalArgumentException("message.getObjectType() cannot be null");
		StringBuilder builder = new StringBuilder();
		builder.append(message.getObjectType().name());
		builder.append("-");
		builder.append(message.getObjectId());
		return builder.toString();
	}
	
	/**
	 * Get the messages that are currently bound to this transaction.
	 * @return
	 */
	private Map<String, ChangeMessage> getCurrentBoundMessages(){
		Map<String, ChangeMessage> currentMessages = (Map<String, ChangeMessage>) transactionSynchronizationManager.getResource(TRANSACTIONAL_MESSANGER_IMPL_MESSAGES);
		if(currentMessages == null){
			// This is the first time it is called for this thread.
			currentMessages = new HashMap<String, ChangeMessage>();
			// Bind this list to the transaction.
			transactionSynchronizationManager.bindResource(TRANSACTIONAL_MESSANGER_IMPL_MESSAGES, currentMessages);
		}
		return currentMessages;
	}
	
	/**
	 * For each thread we need to add a handler, but we only need to do this if a handler does not already exist.
	 * 
	 */
	private void registerHandlerIfNeeded(){
		// Inspect the current handlers.
		List<TransactionSynchronization> currentList = transactionSynchronizationManager.getSynchronizations();
		if(currentList.size() < 1){
			// Add a new handler
			transactionSynchronizationManager.registerSynchronization(new SynchronizationHandler());
		}else if(currentList.size() == 1){
			// Validate that the handler is what we expected
			TransactionSynchronization ts = currentList.get(0);
			if(ts == null) throw new IllegalStateException("TransactionSynchronization cannot be null");
			if(!(ts instanceof SynchronizationHandler)){
				throw new IllegalStateException("Found an unknow TransactionSynchronization: "+ts.getClass().getName());
			}
		}else{
			throw new IllegalStateException("Expected one and only one TransactionSynchronization for this therad but found: "+currentList.size());
		}
	}
	
	/**
	 * Handles the Synchronization Handler
	 * @author John
	 *
	 */
	private class SynchronizationHandler extends TransactionSynchronizationAdapter {
		@Override
		public void afterCompletion(int status) {
			// Unbind any messages from this transaction.
			// Note: We unbind even if the status was a roll back (status=1) as we will not send
			// messages when a roll back occurs.
			if(log.isTraceEnabled()){
				log.trace("Unbinding resources");
			}
			// Unbind any messages from this transaction.
			transactionSynchronizationManager.unbindResourceIfPossible(TRANSACTIONAL_MESSANGER_IMPL_MESSAGES);
		}

		@Override
		public void afterCommit() {
			// Log the messages
			Map<String, ChangeMessage> currentMessages = getCurrentBoundMessages();
			// For each observer fire the message.
			for(TransactionalMessengerObserver observer: observers){
				// Fire each message.
				Collection<ChangeMessage> collection = currentMessages.values();
				for(ChangeMessage message: collection){
					observer.fireChangeMessage(message);
					if(log.isTraceEnabled()){
						log.trace("Firing a change event: "+message+" for observer: "+observer);
					}
				}
			}
			// Clear the list
			currentMessages.clear();
		}

		@Override
		public void beforeCommit(boolean readOnly) {
			// write the changes to the database
			Map<String, ChangeMessage> currentMessages = getCurrentBoundMessages();
			Collection<ChangeMessage> collection = currentMessages.values();
			List<ChangeMessage> list = new LinkedList<ChangeMessage>(collection);
			// Create the list of DBOS
			List<ChangeMessage> updatedList = changeDAO.replaceChange(list);
			// Now replace each entry on the map with the update message
			for(ChangeMessage message: updatedList){
				currentMessages.put(message.getObjectId(), message);
			}
		}
		
	}
	
	/**
	 * Register an observer that will be notified when there is a message after a commit.
	 * 
	 * @param observer
	 */
	public void registerObserver(TransactionalMessengerObserver observer){
		// Add this to the list of observers.
		observers.add(observer);
	}
	
	/**
	 * Remove an observer.
	 * @param observer
	 * @return true if observer was registered.
	 */
	public boolean removeObserver(TransactionalMessengerObserver observer){
		// remove from the list
		return observers.remove(observer);
	}

	@Override
	public List<TransactionalMessengerObserver> getAllObservers() {
		// Return a copy of the list.
		return new LinkedList<TransactionalMessengerObserver>(observers);
	}

}
