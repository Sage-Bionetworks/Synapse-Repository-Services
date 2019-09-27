package org.sagebionetworks.repo.manager.events;

/**
 * Marker interface for an event to be sent to a firehose stream
 * 
 * @author Marco
 *
 */
public interface SynapseEvent {
	
	/**
	 * @return The timestamp when the event occurred
	 */
	Long getTimestamp();

	/**
	 * @return The id of the user that performed the action if present
	 */
	Long getUserId();
	

}
