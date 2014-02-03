package org.sagebionetworks.bridge.model;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ParticipantDataDescriptorDAO {

	List<ParticipantDataDescriptor> getParticipantDatas();

	Map<String, ParticipantDataDescriptor> getParticipantDataDescriptorsForUser(List<String> participantIds);

	List<ParticipantDataColumnDescriptor> getParticipantDataColumns(String participantDataId);

	ParticipantDataDescriptor getParticipantDataDescriptor(String participantDataId) throws DatastoreException, NotFoundException;

	ParticipantDataDescriptor createParticipantDataDescriptor(ParticipantDataDescriptor participantDataDescriptor);

	void updateParticipantDataDescriptor(ParticipantDataDescriptor participantDataDescriptor) throws NotFoundException;

	void deleteParticipantDataDescriptor(String participantDataId);

	ParticipantDataColumnDescriptor createParticipantDataColumnDescriptor(ParticipantDataColumnDescriptor participantDataColumnDescriptor);

	void updateParticipantDataColumnDescriptor(ParticipantDataColumnDescriptor participantDataColumnDescriptor) throws NotFoundException;

	void deleteParticipantDataColumnDescriptor(String participantDataId, String participantDataColumnDescriptorId);
}
