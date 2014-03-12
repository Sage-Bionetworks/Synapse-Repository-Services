package org.sagebionetworks.bridge.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * ServiceProvider is a single class which can be autowired to provide access
 * to all Services. This class should be used to support all Controllers.
 * 
 * @author bkng
 */
@Component
public class BridgeServiceProviderImpl implements BridgeServiceProvider {
	
	@Autowired
	private CommunityService communityService;
	@Autowired
	private ParticipantDataService participantDataService;
	@Autowired
	private TimeSeriesService timeSeriesService;

	public CommunityService getCommunityService() {
		return communityService;
	}

	@Override
	public ParticipantDataService getParticipantDataService() {
		return participantDataService;
	}

	@Override
	public TimeSeriesService getTimeSeriesService() {
		return timeSeriesService;
	}
}
