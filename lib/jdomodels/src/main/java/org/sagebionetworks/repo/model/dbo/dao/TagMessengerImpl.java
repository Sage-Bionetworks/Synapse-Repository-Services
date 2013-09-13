package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.ids.ETagGenerator;
import org.sagebionetworks.repo.model.ObservableEntity;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Ensure any etag change also notifies observers.
 * 
 * @author jmhill
 *
 */
public class TagMessengerImpl implements TagMessenger{

	@Autowired
	private ETagGenerator eTagGenerator;
	@Autowired
	private TransactionalMessenger transactionalMessanger;

	@Override
	public void generateEtagAndSendMessage(ObservableEntity observable,
			ChangeType changeType) {
		// Send a message that an entity was created
		String newEtag = eTagGenerator.generateETag();
		observable.seteTag(newEtag);
		// Create the message
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(changeType);
		message.setObjectType(observable.getObjectType());
		message.setObjectId(observable.getIdString());
		message.setParentId(observable.getParentIdString());
		message.setObjectEtag(observable.geteTag());
		transactionalMessanger.sendMessageAfterCommit(message);
	}

	@Override
	public void sendMessage(ObservableEntity observable, ChangeType changeType) {
		// Create the message
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(changeType);
		message.setObjectType(observable.getObjectType());
		message.setObjectId(observable.getIdString());
		message.setParentId(observable.getParentIdString());
		message.setObjectEtag(observable.geteTag());
		transactionalMessanger.sendMessageAfterCommit(message);
	}

	@Override
	public void sendDeleteMessage(String objectId, ObjectType objectType) {
		// Send a message that the entity was deleted
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.DELETE);
		message.setObjectType(objectType);
		message.setObjectId(objectId);
		transactionalMessanger.sendMessageAfterCommit(message);
	}

	@Override
	public void sendMessage(String id, String etag, ObjectType objectType,	ChangeType changeType) {

		if (id == null) {
			throw new IllegalArgumentException("id cannot be null");
		}
		if (etag == null) {
			throw new IllegalArgumentException("etag cannot be null");
		}
		if (objectType == null) {
			throw new IllegalArgumentException("objectType cannot be null");
		}
		if (changeType == null) {
			throw new IllegalArgumentException("changeType cannot be null");
		}

		ChangeMessage message = new ChangeMessage();
		message.setChangeType(changeType);
		message.setObjectType(objectType);
		message.setObjectId(id);
		message.setObjectEtag(etag);
		transactionalMessanger.sendMessageAfterCommit(message);
	}

	@Override
	public void sendMessage(String id, String parentId, String etag, ObjectType objectType,	ChangeType changeType) {

		if (id == null) {
			throw new IllegalArgumentException("id cannot be null");
		}
		if (parentId == null) {
			throw new IllegalArgumentException("parentId cannot be null");
		}
		if (etag == null) {
			throw new IllegalArgumentException("etag cannot be null");
		}
		if (objectType == null) {
			throw new IllegalArgumentException("objectType cannot be null");
		}
		if (changeType == null) {
			throw new IllegalArgumentException("changeType cannot be null");
		}

		ChangeMessage message = new ChangeMessage();
		message.setChangeType(changeType);
		message.setObjectType(objectType);
		message.setObjectId(id);
		message.setParentId(parentId);
		message.setObjectEtag(etag);
		transactionalMessanger.sendMessageAfterCommit(message);
	}
}
