package org.sagebionetworks.bridge.model;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatus;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface BridgeUserParticipantMappingDAO {

	List<String> getParticipantIdsForUser(Long userId) throws IOException, GeneralSecurityException;

	void setParticipantIdsForUser(Long userId, List<String> participantIds) throws IOException, GeneralSecurityException;
}
