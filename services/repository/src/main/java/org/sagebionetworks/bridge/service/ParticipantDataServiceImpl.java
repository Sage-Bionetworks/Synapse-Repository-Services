package org.sagebionetworks.bridge.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.sagebionetworks.bridge.manager.participantdata.ParticipantDataDescriptionManager;
import org.sagebionetworks.bridge.manager.participantdata.ParticipantDataManager;
import org.sagebionetworks.bridge.model.ParticipantDataId;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataCurrentRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptorWithColumns;
import org.sagebionetworks.bridge.model.data.ParticipantDataRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatus;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ParticipantDataServiceImpl implements ParticipantDataService {
	@Autowired
	private UserManager userManager;
	@Autowired
	private ParticipantDataManager participantDataManager;
	@Autowired
	private ParticipantDataDescriptionManager participantDataDescriptionManager;

	@Override
	public PaginatedResults<ParticipantDataRow> get(Long userId, String participantDataId, Integer limit, Integer offset)
			throws DatastoreException, NotFoundException, IOException, GeneralSecurityException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataManager.getData(userInfo, participantDataId, limit, offset);
	}

	@Override
	public ParticipantDataRow getRow(Long userId, String participantDataId, Long rowId) throws DatastoreException,
			NotFoundException, IOException, GeneralSecurityException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataManager.getDataRow(userInfo, participantDataId, rowId);
	}

	@Override
	public ParticipantDataCurrentRow getCurrent(Long userId, String participantDataId) throws DatastoreException,
			NotFoundException, IOException, GeneralSecurityException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataManager.getCurrentData(userInfo, participantDataId);
	}

	@Override
	public List<ParticipantDataRow> append(Long userId, String participantDataId, List<ParticipantDataRow> data) throws DatastoreException,
			NotFoundException, IOException, GeneralSecurityException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataManager.appendData(userInfo, participantDataId, data);
	}

	@Override
	public List<ParticipantDataRow> append(Long userId, String participantId, String participantDataId, List<ParticipantDataRow> data)
			throws DatastoreException, NotFoundException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataManager.appendData(userInfo, new ParticipantDataId(Long.parseLong(participantId)), participantDataId, data);
	}

	@Override
	public void deleteRows(Long userId, String participantDataId, IdList rowIds) throws IOException, NotFoundException,
			GeneralSecurityException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		participantDataManager.deleteRows(userInfo, participantDataId, rowIds);
	}
	
	
	@Override
	public List<ParticipantDataRow> update(Long userId, String participantDataId, List<ParticipantDataRow> data) throws DatastoreException,
			NotFoundException, IOException, GeneralSecurityException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataManager.updateData(userInfo, participantDataId, data);
	}

	@Override
	public void updateParticipantStatuses(Long userId, List<ParticipantDataStatus> statuses) throws NotFoundException, DatastoreException,
			IOException, GeneralSecurityException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		participantDataDescriptionManager.updateStatuses(userInfo, statuses);
	}

	@Override
	public ParticipantDataDescriptor createParticipantDataDescriptor(Long userId, ParticipantDataDescriptor participantDataDescriptor)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataDescriptionManager.createParticipantDataDescriptor(userInfo, participantDataDescriptor);
	}

	@Override
	public void updateParticipantDataDescriptor(Long userId, ParticipantDataDescriptor participantDataDescriptor)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		participantDataDescriptionManager.updateParticipantDataDescriptor(userInfo, participantDataDescriptor);
	}
	
	@Override
	public ParticipantDataDescriptor getParticipantDataDescriptor(Long userId, String participantDataId) throws DatastoreException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataDescriptionManager.getParticipantDataDescriptor(userInfo, participantDataId);
	}

	@Override
	public ParticipantDataDescriptorWithColumns getParticipantDataDescriptorWithColumns(Long userId,
			String participantDataId) throws DatastoreException, NotFoundException, GeneralSecurityException,
			IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataDescriptionManager.getParticipantDataDescriptorWithColumns(userInfo, participantDataId);
	}
	
	@Override
	public PaginatedResults<ParticipantDataDescriptor> getAllParticipantDataDescriptors(Long userId, Integer limit, Integer offset)
			throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataDescriptionManager.getAllParticipantDataDescriptors(userInfo, limit, offset);
	}

	@Override
	public PaginatedResults<ParticipantDataDescriptor> getUserParticipantDataDescriptors(Long userId, Integer limit, Integer offset)
			throws DatastoreException, NotFoundException, IOException, GeneralSecurityException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataDescriptionManager.getUserParticipantDataDescriptors(userInfo, limit, offset);
	}

	@Override
	public ParticipantDataColumnDescriptor createParticipantDataColumnDescriptor(Long userId,
			ParticipantDataColumnDescriptor participantDataColumnDescriptor) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataDescriptionManager.createParticipantDataColumnDescriptor(userInfo, participantDataColumnDescriptor);
	}

	@Override
	public PaginatedResults<ParticipantDataColumnDescriptor> getParticipantDataColumnDescriptors(Long userId, String participantDataId,
			Integer limit, Integer offset) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataDescriptionManager.getParticipantDataColumnDescriptor(userInfo, participantDataId, limit, offset);
	}
}
