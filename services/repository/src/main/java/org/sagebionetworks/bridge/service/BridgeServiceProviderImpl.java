package org.sagebionetworks.bridge.service;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * ServiceProvider is a single class which can be autowired to provide access
 * to all Services. This class should be used to support all Controllers.
 * 
 * @author bkng
 */
public class BridgeServiceProviderImpl implements BridgeServiceProvider {
	
	@Autowired
	private CommunityService communityService;
	@Autowired
	private ParticipantDataService participantDataService;

	public CommunityService getCommunityService() {
		return communityService;
	}

	@Override
	public ParticipantDataService getParticipantDataService() {
		return participantDataService;
	}
}
