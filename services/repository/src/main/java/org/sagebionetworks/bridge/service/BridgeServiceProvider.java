package org.sagebionetworks.bridge.service;


/**
 * Abstraction for the service providers.
 *
 */
public interface BridgeServiceProvider {

	public CommunityService getCommunityService();

	public ParticipantDataService getParticipantDataService();

	public TimeSeriesService getTimeSeriesService();
}
