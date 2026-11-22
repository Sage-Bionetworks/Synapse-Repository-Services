package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

/**
 * Where the requester's notification about a terminal eDUC envelope stands.
 */
public enum EDucNotificationState {
	/**
	 * The terminal state is recorded and the requester has not been dealt with yet.
	 */
	PENDING,
	/**
	 * The requester was sent a message.
	 */
	SENT,
	/**
	 * The requester was deliberately not sent a message, either because the state is not one they are told
	 * about or because delivery was suppressed on this stack.
	 */
	NOT_SENT
}
