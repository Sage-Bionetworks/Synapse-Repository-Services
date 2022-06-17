package org.sagebionetworks.repo.manager.dataaccess;

public interface DataAccessRequestNotificationManager {

	/**
	 * Called after the given data access request is created or updated.
	 * 
	 * @param dataAccessRequestId
	 */
	void dataAccessRequestCreatedOrUpdated(String dataAccessRequestId);

}
