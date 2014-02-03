package org.sagebionetworks.bridge.model;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatus;
import org.sagebionetworks.repo.model.DatastoreException;

public interface ParticipantDataStatusDAO {

	void update(List<ParticipantDataStatus> statusUpdates, Map<String, ParticipantDataDescriptor> participantDataDescriptors);

	void getParticipantStatuses(Map<String, ParticipantDataDescriptor> participantDataDescriptors);

	ParticipantDataStatus getParticipantStatus(String participantDataId, ParticipantDataDescriptor participantDataDescriptor)
			throws DatastoreException;
}
