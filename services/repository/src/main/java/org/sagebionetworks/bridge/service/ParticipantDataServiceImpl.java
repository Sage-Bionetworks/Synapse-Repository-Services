package org.sagebionetworks.bridge.service;

import java.io.IOException;

import org.sagebionetworks.bridge.manager.participantdata.ParticipantDataDescriptionManager;
import org.sagebionetworks.bridge.manager.participantdata.ParticipantDataManager;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.PaginatedRowSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class ParticipantDataServiceImpl implements ParticipantDataService {
	@Autowired
	private UserManager userManager;
	@Autowired
	private ParticipantDataManager participantDataManager;
	@Autowired
	private ParticipantDataDescriptionManager participantDataDescriptionManager;

	@Override
	public PaginatedRowSet get(Long userId, String participantDataId, Integer limit, Integer offset) throws DatastoreException,
			NotFoundException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataManager.getData(userInfo, participantDataId, limit, offset);
	}

	@Override
	public RowSet append(Long userId, String participantDataId, RowSet data) throws DatastoreException, NotFoundException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataManager.appendData(userInfo, participantDataId, data);
	}

	@Override
	public RowSet append(Long userId, String participantId, String participantDataId, RowSet data) throws DatastoreException,
			NotFoundException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataManager.appendData(userInfo, participantId, participantDataId, data);
	}

	@Override
	public RowSet update(Long userId, String participantDataId, RowSet data) throws DatastoreException, NotFoundException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataManager.updateData(userInfo, participantDataId, data);
	}

	@Override
	public ParticipantDataDescriptor createParticipantDataDescriptor(Long userId, ParticipantDataDescriptor participantDataDescriptor) throws DatastoreException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataDescriptionManager.createParticipantDataDescriptor(userInfo, participantDataDescriptor);
	}

	@Override
	public ParticipantDataDescriptor getParticipantDataDescriptor(Long userId, String participantDataId) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataDescriptionManager.getParticipantDataDescriptor(userInfo, participantDataId);
	}

	@Override
	public PaginatedResults<ParticipantDataDescriptor> getAllParticipantDataDescriptors(Long userId, Integer limit, Integer offset)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataDescriptionManager.getAllParticipantDataDescriptors(userInfo, limit, offset);
	}

	@Override
	public PaginatedResults<ParticipantDataDescriptor> getUserParticipantDataDescriptors(Long userId, Integer limit, Integer offset)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataDescriptionManager.getUserParticipantDataDescriptors(userInfo, limit, offset);
	}

	@Override
	public ParticipantDataColumnDescriptor createParticipantDataColumnDescriptor(Long userId, ParticipantDataColumnDescriptor participantDataColumnDescriptor)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataDescriptionManager.createParticipantDataColumnDescriptor(userInfo, participantDataColumnDescriptor);
	}

	@Override
	public PaginatedResults<ParticipantDataColumnDescriptor> getParticipantDataColumnDescriptors(Long userId, String participantDataId, Integer limit,
			Integer offset) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataDescriptionManager.getParticipantDataColumnDescriptor(userInfo, participantDataId, limit, offset);
	}
}
