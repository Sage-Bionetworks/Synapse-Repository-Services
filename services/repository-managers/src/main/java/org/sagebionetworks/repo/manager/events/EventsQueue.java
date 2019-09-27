package org.sagebionetworks.repo.manager.events;

public interface EventsQueue {

	/**
	 * Will be invoked by a background timer in order to push to kinesis the current event queue
	 */
	void flush();
}
