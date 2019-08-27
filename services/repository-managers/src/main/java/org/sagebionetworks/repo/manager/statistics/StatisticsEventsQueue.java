package org.sagebionetworks.repo.manager.statistics;

public interface StatisticsEventsQueue {

	/**
	 * Will be invoked by a background timer in order to push to kinesis the current event queue
	 */
	void flush();
}
