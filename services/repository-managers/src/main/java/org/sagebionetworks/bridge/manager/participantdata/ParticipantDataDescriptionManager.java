package org.sagebionetworks.bridge.manager.participantdata;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptorWithColumns;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatus;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ParticipantDataDescriptionManager {
	ParticipantDataDescriptor createParticipantDataDescriptor(UserInfo userInfo, ParticipantDataDescriptor participantDataDescriptor);

	void updateParticipantDataDescriptor(UserInfo userInfo, ParticipantDataDescriptor participantDataDescriptor) throws NotFoundException;
	
	ParticipantDataDescriptor getParticipantDataDescriptor(UserInfo userInfo, String participantDataId) throws DatastoreException,
			NotFoundException;
	
	ParticipantDataDescriptorWithColumns getParticipantDataDescriptorWithColumns(UserInfo userInfo, String participantDataId)
			throws DatastoreException, NotFoundException, GeneralSecurityException, IOException;

	PaginatedResults<ParticipantDataDescriptor> getAllParticipantDataDescriptors(UserInfo userInfo, Integer limit, Integer offset);

	PaginatedResults<ParticipantDataDescriptor> getUserParticipantDataDescriptors(UserInfo userInfo, Integer limit, Integer offset)
			throws IOException, GeneralSecurityException;

	ParticipantDataColumnDescriptor createParticipantDataColumnDescriptor(UserInfo userInfo,
			ParticipantDataColumnDescriptor participantDataColumnDescriptor);

	PaginatedResults<ParticipantDataColumnDescriptor> getParticipantDataColumnDescriptor(UserInfo userInfo, String participantDataId,
			Integer limit, Integer offset);

	void updateStatuses(UserInfo userInfo, List<ParticipantDataStatus> statuses) throws DatastoreException, IOException,
			GeneralSecurityException;

	List<ParticipantDataColumnDescriptor> getColumns(String participantDataDescriptorId) throws DatastoreException, NotFoundException;
}
