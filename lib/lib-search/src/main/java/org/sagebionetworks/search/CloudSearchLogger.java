package org.sagebionetworks.search;

import org.sagebionetworks.repo.model.message.ChangeMessage;

/**
 * Abstraction for logging interactions with cloud search.
 * 
 *
 */
public interface CloudSearchLogger {

	/**
	 * Start a new record for the given ChangeMessage.
	 * 
	 * @param change
	 * @return
	 */
	public CloudSearchDocumentLogRecord startRecordForChangeMessage(ChangeMessage change);

	/**
	 * Call to indicate the current batch of records finished with the provided
	 * status. Note: Only records with actions of DELETE or CREATE_OR_UPDATE will be
	 * linked to this status.
	 * 
	 * @param status
	 */
	void currentBatchFinshed(String status);

	/**
	 * Push all of the current records to AWS and and resest the logger.
	 */
	public void pushAllRecordsAndReset();
}
