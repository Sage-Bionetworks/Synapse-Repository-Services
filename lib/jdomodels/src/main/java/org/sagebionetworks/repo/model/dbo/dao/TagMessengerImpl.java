package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.ids.ETagGenerator;
import org.sagebionetworks.repo.model.ObservableEntity;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
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
		String newEtag = eTagGenerator.generateETag(null);
		observable.seteTag(newEtag);
		// Create the message
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(changeType);
		message.setObjectType(observable.getObjectType());
		message.setObjectId(observable.getIdString());
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

}
