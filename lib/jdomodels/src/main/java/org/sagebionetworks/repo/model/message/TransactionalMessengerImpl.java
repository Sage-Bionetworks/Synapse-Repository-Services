package org.sagebionetworks.repo.model.message;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ObservableEntity;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ThreadLocalProvider;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


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
	
	static private Logger log = LogManager.getLogger(TransactionalMessengerImpl.class);
	
	private static final String TRANSACTIONAL_MESSANGER_IMPL_CHANGE_MESSAGES = "TransactionalMessangerImpl.ChangeMessages";

	private static final ThreadLocal<Long> currentUserIdThreadLocal = ThreadLocalProvider.getInstance(AuthorizationConstants.USER_ID_PARAM, Long.class);

	@Autowired
	DataSourceTransactionManager txManager;
	@Autowired
	DBOChangeDAO changeDAO;
	@Autowired
	TransactionSynchronizationProxy transactionSynchronizationManager;
	@Autowired
	Clock clock;
	
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
	public TransactionalMessengerImpl(DataSourceTransactionManager txManager, DBOChangeDAO changeDAO,
			TransactionSynchronizationProxy transactionSynchronizationManager, Clock clock) {
		this.txManager = txManager;
		this.changeDAO = changeDAO;
		this.transactionSynchronizationManager = transactionSynchronizationManager;
		this.clock = clock;
	}

	/**
	 * The list of observers that are notified of messages after a commit.
	 */
	private List<TransactionalMessengerObserver> observers = new LinkedList<TransactionalMessengerObserver>();
	
	@Override
	public void sendMessageAfterCommit(String objectId, ObjectType objectType, ChangeType changeType) {
		sendMessageAfterCommit(objectId, objectType, null, changeType);
	}
	
	@Override
	public void sendMessageAfterCommit(String objectId, ObjectType objectType, String etag, ChangeType changeType) {
		sendMessageAfterCommit(objectId, objectType, etag, null, changeType);
	}
	
	@Override
	public void sendMessageAfterCommit(String objectId, ObjectType objectType, String etag, String parentId, ChangeType changeType) {
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(changeType);
		message.setObjectType(objectType);
		message.setObjectId(objectId);
		message.setParentId(parentId);
		message.setObjectEtag(etag);
		
		sendMessageAfterCommit(message);
	}
	
	@Override
	public void sendMessageAfterCommit(ObservableEntity entity, ChangeType changeType) {
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(changeType);
		message.setObjectType(entity.getObjectType());
		message.setObjectId(entity.getIdString());
		message.setParentId(entity.getParentIdString());
		message.setObjectEtag(entity.getEtag());
		
		sendMessageAfterCommit(message);
	}
	
	@Override
	public void sendMessageAfterCommit(ChangeMessage message) {
		if(message == null) throw new IllegalArgumentException("Message cannot be null");
		appendToBoundMessages(message);
	}
	
	@Override
	public void sendModificationMessageAfterCommit(String objectId, ObjectType objectType) {
		ValidateArgument.required(objectId, "objectId");
		ValidateArgument.required(objectType, "objectType");

		Long userId = currentUserIdThreadLocal.get();
		if (userId != null && !AuthorizationUtils.isUserAnonymous(userId.longValue())) {
			ChangeMessage message = new ChangeMessage();
			message.setIsModification(true);
			message.setObjectId(objectId);
			message.setObjectType(objectType);
			message.setTimestamp(clock.now());
			ModificationInfo modificationInfo = new ModificationInfo();
			modificationInfo.setUserId(userId);
			message.setModificationInfo(modificationInfo);

			appendToBoundMessages(message);
		}
	}

	private void appendToBoundMessages(ChangeMessage message) {
		// Make sure we are in a transaction.
		if (!transactionSynchronizationManager.isSynchronizationActive())
			throw new IllegalStateException("Cannot send a transactional message becasue there is no transaction");
		// Bind this message to the transaction
		// Get the bound list of messages if it already exists.
		Map<ChangeMessageKey, ChangeMessage> currentMessages = getCurrentBoundChangeMessages();
		// If we already have a message going out for this object then we needs replace it with the latest.
		// If an object's etag changes multiple times, only the final etag should be in the message.
		currentMessages.put(new ChangeMessageKey(message), message);
		// Register a handler if needed
		registerHandlerIfNeeded();
	}

	/**
	 * Get the change messages that are currently bound to this transaction.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Map<ChangeMessageKey, ChangeMessage> getCurrentBoundChangeMessages() {
		String queueKey = TRANSACTIONAL_MESSANGER_IMPL_CHANGE_MESSAGES;
		Map<ChangeMessageKey, ChangeMessage> currentMessages = (Map<ChangeMessageKey, ChangeMessage>) transactionSynchronizationManager
				.getResource(queueKey);
		if(currentMessages == null){
			// This is the first time it is called for this thread.
			currentMessages = Maps.newHashMap();
			// Bind this list to the transaction.
			transactionSynchronizationManager.bindResource(queueKey, currentMessages);
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
			transactionSynchronizationManager.unbindResourceIfPossible(TRANSACTIONAL_MESSANGER_IMPL_CHANGE_MESSAGES);
		}

		@Override
		public void afterCommit() {
			// Log the messages
			Map<ChangeMessageKey, ChangeMessage> currentMessages = getCurrentBoundChangeMessages();
			// For each observer fire the message.
			for(TransactionalMessengerObserver observer: observers){
				// Fire each message.
				for (ChangeMessage message : currentMessages.values()) {
					if (BooleanUtils.isTrue(message.getIsModification())) {
						observer.fireModificationMessage(message);
					} else {
						observer.fireChangeMessage(message);
					}
					if (log.isTraceEnabled()) {
						log.trace("Firing a change event: " + message + " for observer: " + observer);
					}
				}
			}
			// Clear the lists
			currentMessages.clear();
		}

		@Override
		public void beforeCommit(boolean readOnly) {
			// write the changes to the database
			Map<ChangeMessageKey, ChangeMessage> currentMessages = getCurrentBoundChangeMessages();

			// filter out modification message, since this only applies to the non-modifcation only messages
			List<ChangeMessage> list = Lists.newArrayList(Iterators.filter(currentMessages.values().iterator(),
					new Predicate<ChangeMessage>() {
						@Override
						public boolean apply(ChangeMessage input) {
							return BooleanUtils.isNotTrue(input.getIsModification());
						}
					}));

			// Create the list of DBOS
			List<ChangeMessage> updatedList;
			updatedList = changeDAO.replaceChange(list);
			// Now replace each entry on the map with the update message
			for (ChangeMessage message : updatedList) {
				currentMessages.put(new ChangeMessageKey(message), message);
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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean registerMessageSent(ChangeMessage message){
		try {
			return this.changeDAO.registerMessageSent(message);
		} catch (DataAccessException e) {
			throw new IllegalArgumentException("Messages was not registered as sent: "+e.getMessage());
		}
	}

	@Override
	public List<ChangeMessage> listUnsentMessages(long limit) {
		return this.changeDAO.listUnsentMessages(limit);
	}
	

}
