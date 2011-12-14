package org.sagebionetworks.tool.migration.job;

public class CreatorResponse {
	
	int submitedToQueue;
	int pendingDependancies;
	
	
	/**
	 * @param submitedToQueue How many creates were submitted to the queue.
	 * 
	 * @param pendingDependancies How many creates are pending submission due to dependency requirements.
	 */
	public CreatorResponse(int submitedToQueue, int pendingDependancies) {
		super();
		this.submitedToQueue = submitedToQueue;
		this.pendingDependancies = pendingDependancies;
	}
	
	/**
	 * How many creates were submitted to the queue.
	 * @return
	 */
	public int getSubmitedToQueue() {
		return submitedToQueue;
	}

	/**
	 * How many creates are still pending submission due to dependency requirements.
	 * @return
	 */
	public int getPendingDependancies() {
		return pendingDependancies;
	}

	
}
