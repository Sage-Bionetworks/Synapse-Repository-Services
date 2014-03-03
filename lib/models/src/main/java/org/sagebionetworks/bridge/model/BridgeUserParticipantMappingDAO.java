package org.sagebionetworks.bridge.model;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public interface BridgeUserParticipantMappingDAO {

	List<ParticipantDataId> getParticipantIdsForUser(Long userId) throws IOException, GeneralSecurityException;

	void setParticipantIdsForUser(Long userId, List<ParticipantDataId> participantIds) throws IOException, GeneralSecurityException;
}
