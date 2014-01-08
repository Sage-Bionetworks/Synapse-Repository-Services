package org.sagebionetworks.bridge.model;

import java.util.List;

import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatus;

public interface ParticipantDataStatusDAO {

	void update(List<ParticipantDataStatus> statusUpdates);

	List<ParticipantDataDescriptor> getParticipantStatuses(List<ParticipantDataDescriptor> participantDatas);
}
