package org.sagebionetworks.repo.model.message;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
	
	/**
	 * The list of observers that are notified of messages after a commit.
	 */
	private List<TransactionalMessengerObserver> observers = new LinkedList<TransactionalMessengerObserver>();
	
	@Override
	public void sendMessageAfterCommit(ChangeMessage message) {
		// Make sure we are in a transaction.
		if(!TransactionSynchronizationManager.isSynchronizationActive()) throw new IllegalStateException("Cannot send a transactional message becasue there is no transaction");
		// Bind this message to the transaction
		@SuppressWarnings("unchecked")
		// Get the bound list of messages if it already exists.
		List<ChangeMessage> currentMessages = (List<ChangeMessage>) TransactionSynchronizationManager.getResource(TRANSACTIONAL_MESSANGER_IMPL_MESSAGES);
		if(currentMessages == null){
			// This is the first time it is called for this thread.
			currentMessages = new LinkedList<ChangeMessage>();
			// Bind this list to the transaction.
			TransactionSynchronizationManager.bindResource(TRANSACTIONAL_MESSANGER_IMPL_MESSAGES, currentMessages);
		}
		// Add this message to the list
		currentMessages.add(message);
		// Register a handler if needed
		registerHandlerIfNeeded();
	}
	
	/**
	 * For each thread we need to add a handler, but we only need to do this if a handler does not already exist.
	 * 
	 */
	private void registerHandlerIfNeeded(){
		// Inspect the current handlers.
		List<TransactionSynchronization> currentList = TransactionSynchronizationManager.getSynchronizations();
		if(currentList.size() < 1){
			// Add a new handler
			TransactionSynchronizationManager.registerSynchronization(new SynchronizationHandler());
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
			TransactionSynchronizationManager.unbindResourceIfPossible(TRANSACTIONAL_MESSANGER_IMPL_MESSAGES);
		}

		@Override
		public void afterCommit() {
			// Log the messages
			@SuppressWarnings("unchecked")
			List<ChangeMessage> currentMessages = (List<ChangeMessage>) TransactionSynchronizationManager.getResource(TRANSACTIONAL_MESSANGER_IMPL_MESSAGES);
			if(currentMessages == null) throw new IllegalStateException("No messages bound to this transaction");
			if(currentMessages.isEmpty()) throw new IllegalStateException("Called on an empty list");
			// For each observer fire the message.
			for(TransactionalMessengerObserver observer: observers){
				// Fire each message.
				for(ChangeMessage message: currentMessages){
					observer.fireChangeMessage(message);
					if(log.isTraceEnabled()){
						log.trace("Firing a change event: "+message+" for observer: "+observer);
					}
				}
			}
			// Clear the list
			currentMessages.clear();
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

}
