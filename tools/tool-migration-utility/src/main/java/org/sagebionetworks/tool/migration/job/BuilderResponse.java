package org.sagebionetworks.tool.migration.job;

public class BuilderResponse {
	
	int submittedToQueue;
	int pendingDependencies;
	
	
	/**
	 * @param submitedToQueue How many creates were submitted to the queue.
	 * 
	 * @param pendingDependancies How many creates are pending submission due to dependency requirements.
	 */
	public BuilderResponse(int submitedToQueue, int pendingDependancies) {
		super();
		this.submittedToQueue = submitedToQueue;
		this.pendingDependencies = pendingDependancies;
	}
	
	/**
	 * How many creates were submitted to the queue.
	 * @return
	 */
	public int getSubmittedToQueue() {
		return submittedToQueue;
	}

	/**
	 * How many creates are still pending submission due to dependency requirements.
	 * @return
	 */
	public int getPendingDependencies() {
		return pendingDependencies;
	}

	
}
