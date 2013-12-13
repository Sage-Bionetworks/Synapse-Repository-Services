package org.sagebionetworks.bridge.manager.participantdata;

import java.util.List;

import org.sagebionetworks.bridge.model.ParticipantDataDescriptorDAO;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.PaginatedResultsUtil;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class ParticipantDataDescriptionManagerImpl implements ParticipantDataDescriptionManager {

	@Autowired
	private ParticipantDataDescriptorDAO participantDataDescriptorDAO;

	@Autowired
	private ParticipantDataMappingManager participantDataMappingManager;

	@Override
	public ParticipantDataDescriptor createParticipantDataDescriptor(UserInfo userInfo, ParticipantDataDescriptor participantDataDescriptor) {
		return participantDataDescriptorDAO.createParticipantDataDescriptor(participantDataDescriptor);
	}

	@Override
	public ParticipantDataDescriptor getParticipantDataDescriptor(UserInfo userInfo, String participantDataId) throws DatastoreException, NotFoundException {
		return participantDataDescriptorDAO.getParticipantDataDescriptor(participantDataId);
	}

	@Override
	public PaginatedResults<ParticipantDataDescriptor> getAllParticipantDataDescriptors(UserInfo userInfo, Integer limit, Integer offset) {
		List<ParticipantDataDescriptor> participantDatas = participantDataDescriptorDAO.getParticipantDatas();
		return PaginatedResultsUtil.createPaginatedResults(participantDatas, limit, offset);
	}

	@Override
	public PaginatedResults<ParticipantDataDescriptor> getUserParticipantDataDescriptors(UserInfo userInfo, Integer limit, Integer offset) {
		List<String> participantIds = participantDataMappingManager.mapSynapseUserToParticipantIds(userInfo);
		List<ParticipantDataDescriptor> participantDatas = participantDataDescriptorDAO.getParticipantDatasForUser(participantIds);
		return PaginatedResultsUtil.createPaginatedResults(participantDatas, limit, offset);
	}

	@Override
	public ParticipantDataColumnDescriptor createParticipantDataColumnDescriptor(UserInfo userInfo, ParticipantDataColumnDescriptor participantDataColumnDescriptor) {
		return participantDataDescriptorDAO.createParticipantDataColumnDescriptor(participantDataColumnDescriptor);
	}

	@Override
	public PaginatedResults<ParticipantDataColumnDescriptor> getParticipantDataColumnDescriptor(UserInfo userInfo, String participantDataId, Integer limit,
			Integer offset) {
		List<ParticipantDataColumnDescriptor> participantDataColumns = participantDataDescriptorDAO.getParticipantDataColumns(participantDataId);
		return PaginatedResultsUtil.createPaginatedResults(participantDataColumns, limit, offset);
	}
}
