package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.message.ChangeType;

/**
 * Union of eTag generation and messaging.
 * This enforces that messages are sent whenever there is an etag change.
 * @author jmhill
 *
 */
public interface TagMessenger {
	
	/**
	 * Generate a new etag and send out a message to all observers.
	 * @param taggable - The object that was created or updated.
	 * @param changeType - The type of change that occurred. Typically a CREATE or UPDATE.
	 * @return The new Etag.
	 */
	public void generateEtagAndSendMessage(ObservableEntity observable, ChangeType changeType);
	
	/**
	 * Send out a message to all observers without changing the etag.
	 * @param taggable
	 * @param changeType
	 */
	public void sendMessage(ObservableEntity observable, ChangeType changeType);
	
	/**
	 * Sends a change message without parent ID.
	 * @throws IllegalArgumentException When the object is type ENTITY, parent ID is required.
	 */
	public void sendMessage(String id, String etag, ObjectType objectType, ChangeType changeType) throws IllegalArgumentException;

	/**
	 * Sends a change message.
	 */
	public void sendMessage(String id, String parentId, String etag, ObjectType objectType, ChangeType changeType);

	/**
	 * Send a delete message without an etag.
	 * @param objectId - The ID of the object that has been deleted.
	 * @param objectType - The type of object that was deleted.
	 */
	public void sendDeleteMessage(String objectId, ObjectType objectType);
}
