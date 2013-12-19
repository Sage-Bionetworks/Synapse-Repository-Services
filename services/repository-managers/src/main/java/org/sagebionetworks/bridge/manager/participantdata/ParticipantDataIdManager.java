package org.sagebionetworks.bridge.manager.participantdata;

import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;

public interface ParticipantDataIdManager {
	/**
	 * Returns a list of all participant ids for this user
	 * 
	 * @param user
	 * @return
	 */
	List<String> mapSynapseUserToParticipantIds(UserInfo userInfo);

	String createNewParticipantForUser(UserInfo user);
}
