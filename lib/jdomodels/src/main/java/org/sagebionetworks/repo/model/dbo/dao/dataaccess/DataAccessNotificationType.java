package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

/**
 * Type of notifications sent for access approvals.
 */
public enum DataAccessNotificationType {
	/**
	 * Notification sent after revocation
	 */
	REVOCATION, 
	/**
	 * Notification sent 2 months before the expiration date
	 */
	FIRST_RENEWAL_REMINDER, 
	/**
	 * Notification sent 1 month before the expiration date
	 */
	SECOND_RENEWAL_REMINDER;
}
