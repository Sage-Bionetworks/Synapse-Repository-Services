package org.sagebionetworks.bridge.manager.participantdata;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;

public interface ParticipantDataIdMappingManager {
	/**
	 * Returns a list of all participant ids for this user
	 */
	List<String> mapSynapseUserToParticipantIds(UserInfo userInfo) throws IOException, GeneralSecurityException;

	String createNewParticipantForUser(UserInfo user) throws IOException, GeneralSecurityException;
}
