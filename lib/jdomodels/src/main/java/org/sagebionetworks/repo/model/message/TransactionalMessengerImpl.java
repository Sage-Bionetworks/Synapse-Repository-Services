package org.sagebionetworks.repo.model.message;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.collections.Transform;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ObservableEntity;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ThreadLocalProvider;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
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
	
	private static final String TRANSACTIONAL_MESSANGER_IMPL_MESSAGES = "TransactionalMessangerImpl.Messages";

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
		sendMessageAfterCommit(objectId, objectType, etag, changeType, null);
	}

	@Deprecated
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
	public void sendMessageAfterCommit(String objectId, ObjectType objectType, ChangeType changeType, Long userId) {
		sendMessageAfterCommit(objectId, objectType, null, changeType, userId);
	}

	@Override
	public void sendMessageAfterCommit(String objectId, ObjectType objectType, String etag, ChangeType changeType, Long userId) {
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(changeType);
		message.setObjectType(objectType);
		message.setObjectId(objectId);
		message.setObjectEtag(etag);
		message.setUserId(userId);
		sendMessageAfterCommit(message);
	}

	@Override
	public void sendModificationMessageAfterCommit(String objectId, ObjectType objectType) {
		DefaultModificationMessage message = new DefaultModificationMessage();
		message.setObjectId(objectId);
		message.setObjectType(objectType);
		sendModificationMessageAfterCommit(message);
	}

	@Override
	public void sendModificationMessageAfterCommit(ModificationMessage message) {
		ValidateArgument.required(message.getObjectId(), "objectId");
		ValidateArgument.required(message.getObjectType(), "objectType");

		Long userId = currentUserIdThreadLocal.get();
		if (userId != null && !AuthorizationUtils.isUserAnonymous(userId.longValue())) {
			message.setTimestamp(clock.now());
			message.setUserId(userId);

			appendToBoundMessages(message);
		}
	}

	private <T extends Message> void appendToBoundMessages(T message) {
		// Make sure we are in a transaction.
		if (!transactionSynchronizationManager.isSynchronizationActive())
			throw new IllegalStateException("Cannot send a transactional message becasue there is no transaction");
		// Bind this message to the transaction
		// Get the bound list of messages if it already exists.
		Map<MessageKey, Message> currentMessages = getCurrentBoundMessages();
		// If we already have a message going out for this object then we needs replace it with the latest.
		// If an object's etag changes multiple times, only the final etag should be in the message.
		currentMessages.put(new MessageKey(message), message);
		// Register a handler if needed
		registerHandlerIfNeeded();
	}

	/**
	 * Get the change messages that are currently bound to this transaction.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Map<MessageKey, Message> getCurrentBoundMessages() {
		String queueKey = TRANSACTIONAL_MESSANGER_IMPL_MESSAGES;
		Map<MessageKey, Message> currentMessages = (Map<MessageKey, Message>) transactionSynchronizationManager
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
		for (TransactionSynchronization sync : currentList) {
			if (sync instanceof SynchronizationHandler) {
				return;
			}
		}
		transactionSynchronizationManager.registerSynchronization(new SynchronizationHandler());
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
			Map<MessageKey, Message> currentMessages = getCurrentBoundMessages();
			// For each observer fire the message.
			for(TransactionalMessengerObserver observer: observers){
				// Fire each message.
				for (Message message : currentMessages.values()) {
					if (message instanceof ChangeMessage) {
						observer.fireChangeMessage((ChangeMessage) message);
					} else if (message instanceof ModificationMessage) {
						observer.fireModificationMessage((ModificationMessage) message);
					} else {
						throw new IllegalArgumentException("Unknown message type " + message.getClass().getName());
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
			Map<MessageKey, Message> currentMessages = getCurrentBoundMessages();

			// filter out modification message, since this only applies to the non-modifcation only messages
			List<ChangeMessage> list = Transform.toList(Iterables.filter(currentMessages.values(), new Predicate<Message>() {
				@Override
				public boolean apply(Message input) {
					return input instanceof ChangeMessage;
				}
			}), new Function<Message, ChangeMessage>() {
				@Override
				public ChangeMessage apply(Message input) {
					return (ChangeMessage) input;
				}
			});

			// Create the list of DBOS
			List<ChangeMessage> updatedList;
			updatedList = changeDAO.replaceChange(list);
			// Now replace each entry on the map with the update message
			for (ChangeMessage message : updatedList) {
				currentMessages.put(new MessageKey(message), message);
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
	
	@WriteTransaction
	@Override
	public void registerMessagesSent(ObjectType type, List<ChangeMessage> page) {
		this.changeDAO.registerMessageSent(type, page);
	}

	@Override
	public List<ChangeMessage> listUnsentMessages(long limit) {
		return this.changeDAO.listUnsentMessages(limit);
	}
}
