package org.sagebionetworks.bridge.manager.participantdata;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang.BooleanUtils;
import org.sagebionetworks.bridge.model.ParticipantDataDAO;
import org.sagebionetworks.bridge.model.ParticipantDataStatusDAO;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataCurrentRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatus;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataValue;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.PaginatedResultsUtil;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class ParticipantDataManagerImpl implements ParticipantDataManager {

	private final static ParticipantDataRow EMPTY_ROW;

	static {
		EMPTY_ROW = new ParticipantDataRow();
		EMPTY_ROW.setData(Collections.<String, ParticipantDataValue> emptyMap());
	}

	@Autowired
	private ParticipantDataDAO participantDataDAO;
	@Autowired
	private ParticipantDataIdMappingManager participantDataMappingManager;
	@Autowired
	private ParticipantDataStatusDAO participantDataStatusDAO;
	@Autowired
	private ParticipantDataDescriptionManager participantDataDescriptionManager;

	@Override
	public List<ParticipantDataRow> appendData(UserInfo userInfo, String participantId, String participantDataId,
			List<ParticipantDataRow> data) throws DatastoreException, NotFoundException, IOException {
		List<ParticipantDataColumnDescriptor> columns = participantDataDescriptionManager.getColumns(participantDataId);
		return participantDataDAO.append(participantId, participantDataId, data, columns);
	}

	@Override
	public List<ParticipantDataRow> appendData(UserInfo userInfo, String participantDataId, List<ParticipantDataRow> data)
			throws DatastoreException, NotFoundException, IOException, GeneralSecurityException {
		List<String> participantIds = participantDataMappingManager.mapSynapseUserToParticipantIds(userInfo);
		String participantId = participantDataDAO.findParticipantForParticipantData(participantIds, participantDataId);
		if (participantId == null) {
			participantId = participantDataMappingManager.createNewParticipantForUser(userInfo);
		}
		List<ParticipantDataColumnDescriptor> columns = participantDataDescriptionManager.getColumns(participantDataId);
		return participantDataDAO.append(participantId, participantDataId, data, columns);
	}
	
	@Override
	public void deleteRows(UserInfo userInfo, String participantDataId, IdList rowIds) throws IOException, NotFoundException,
			GeneralSecurityException {
		List<String> participantIds = participantDataMappingManager.mapSynapseUserToParticipantIds(userInfo);
		String participantId = participantDataDAO.findParticipantForParticipantData(participantIds, participantDataId);
		participantDataDAO.deleteRows(participantId, participantDataId, rowIds);
	};

	@Override
	public List<ParticipantDataRow> updateData(UserInfo userInfo, String participantDataId, List<ParticipantDataRow> data)
			throws DatastoreException, NotFoundException, IOException, GeneralSecurityException {
		List<String> participantIds = participantDataMappingManager.mapSynapseUserToParticipantIds(userInfo);
		String participantId = participantDataDAO.findParticipantForParticipantData(participantIds, participantDataId);
		if (participantId == null) {
			throw new NotFoundException("No data to update found for this user");
		}
		List<ParticipantDataColumnDescriptor> columns = participantDataDescriptionManager.getColumns(participantDataId);
		return participantDataDAO.update(participantId, participantDataId, data, columns);
	}

	@Override
	public PaginatedResults<ParticipantDataRow> getData(UserInfo userInfo, String participantDataId, Integer limit, Integer offset)
			throws DatastoreException, NotFoundException, IOException, GeneralSecurityException {
		List<String> participantIds = participantDataMappingManager.mapSynapseUserToParticipantIds(userInfo);
		String participantId = participantDataDAO.findParticipantForParticipantData(participantIds, participantDataId);
		if (participantId == null) {
			// User has never created data for this ParticipantData type, which is not an error, so return
			// empty result. It will have no headers given the way this works.
			return PaginatedResultsUtil.createEmptyPaginatedResults();
		}
		List<ParticipantDataColumnDescriptor> columns = participantDataDescriptionManager.getColumns(participantDataId);
		List<ParticipantDataRow> rowList = participantDataDAO.get(participantId, participantDataId, columns);
		return PaginatedResultsUtil.createPaginatedResults(rowList, limit, offset);
	}

	@Override
	public ParticipantDataRow getDataRow(UserInfo userInfo, String participantDataId, Long rowId) throws DatastoreException,
			NotFoundException, IOException, GeneralSecurityException {
		List<String> participantIds = participantDataMappingManager.mapSynapseUserToParticipantIds(userInfo);
		String participantId = participantDataDAO.findParticipantForParticipantData(participantIds, participantDataId);
		if (participantId == null) {
			// User has never created data for this ParticipantData type, which is not an error, so return
			// empty result. It will have no headers given the way this works.
			throw new NotFoundException("No participant data with id " + participantDataId);
		}
		List<ParticipantDataColumnDescriptor> columns = participantDataDescriptionManager.getColumns(participantDataId);
		return participantDataDAO.getRow(participantId, participantDataId, rowId, columns);
	}

	@Override
	public ParticipantDataCurrentRow getCurrentData(UserInfo userInfo, String participantDataDescriptorId) throws DatastoreException,
			NotFoundException, IOException, GeneralSecurityException {
		ParticipantDataCurrentRow result = new ParticipantDataCurrentRow();
		result.setDescriptor(participantDataDescriptionManager.getParticipantDataDescriptor(userInfo, participantDataDescriptorId));
		result.setColumns(participantDataDescriptionManager.getColumns(participantDataDescriptorId));
		List<String> participantIds = participantDataMappingManager.mapSynapseUserToParticipantIds(userInfo);
		String participantId = participantDataDAO.findParticipantForParticipantData(participantIds, participantDataDescriptorId);
		if (participantId == null) {
			// User has never created data for this ParticipantData type, which is not an error, so return empty status
			ParticipantDataStatus status = new ParticipantDataStatus();
			status.setParticipantDataDescriptorId(participantDataDescriptorId);
			result.setStatus(status);
			return result;
		}

		ParticipantDataStatus status = participantDataStatusDAO.getParticipantStatus(participantId, result.getDescriptor());
		result.setStatus(status);

		result.setCurrentData(EMPTY_ROW);
		result.setPreviousData(EMPTY_ROW);

		List<ParticipantDataRow> rowList = participantDataDAO.get(participantId, participantDataDescriptorId, result.getColumns());
		ListIterator<ParticipantDataRow> iter = rowList.listIterator(rowList.size());
		if (iter.hasPrevious()) {
			ParticipantDataRow lastRow = iter.previous();
			if (BooleanUtils.isFalse(status.getLastEntryComplete())) {
				result.setCurrentData(lastRow);
				if (iter.hasPrevious()) {
					result.setPreviousData(iter.previous());
				}
			} else {
				result.setPreviousData(lastRow);
			}
		}
		return result;
	}
}
