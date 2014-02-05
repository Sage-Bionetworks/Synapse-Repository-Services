package org.sagebionetworks.bridge.model;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatus;
import org.sagebionetworks.repo.model.DatastoreException;

public interface ParticipantDataStatusDAO {

	void update(List<ParticipantDataStatus> statusUpdates, Map<String, ParticipantDataId> participantDataDescriptorToDataIdMap);

	void getParticipantStatuses(List<ParticipantDataDescriptor> participantDataDescriptors,
			Map<String, ParticipantDataId> participantDataDescriptorToDataIdMap);

	ParticipantDataStatus getParticipantStatus(ParticipantDataId participantDataId, ParticipantDataDescriptor participantDataDescriptor)
			throws DatastoreException;
}
