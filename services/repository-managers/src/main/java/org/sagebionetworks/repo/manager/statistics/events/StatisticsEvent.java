package org.sagebionetworks.repo.manager.statistics.events;

/**
 * Marker interface for an event for collecting statistics
 * 
 * @author Marco
 *
 */
public interface StatisticsEvent {
	
	/**
	 * @return The timestamp when the event occurred
	 */
	Long getTimestamp();

	/**
	 * @return The id of the user that performed the action if present
	 */
	Long getUserId();
	

}
