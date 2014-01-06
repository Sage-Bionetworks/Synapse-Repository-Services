package org.sagebionetworks.bridge.manager.participantdata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sagebionetworks.bridge.model.ParticipantDataDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResultsUtil;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.PaginatedRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

public class ParticipantDataManagerImpl implements ParticipantDataManager {

	@Autowired
	private ParticipantDataDAO participantDataDAO;
	@Autowired
	private ParticipantDataIdManager participantDataMappingManager;

	@Override
	public RowSet appendData(UserInfo userInfo, String participantId, String participantDataId, RowSet data) throws DatastoreException,
			NotFoundException, IOException {
		return participantDataDAO.append(participantId, participantDataId, data);
	}

	@Override
	public RowSet appendData(UserInfo userInfo, String participantDataId, RowSet data) throws DatastoreException, NotFoundException, IOException {
		List<String> participantIds = participantDataMappingManager.mapSynapseUserToParticipantIds(userInfo);
		String participantId = participantDataDAO.findParticipantForParticipantData(participantIds, participantDataId);
		if (participantId == null) {
			participantId = participantDataMappingManager.createNewParticipantForUser(userInfo);
		}
		return participantDataDAO.append(participantId, participantDataId, data);
	}

	@Override
	public RowSet updateData(UserInfo userInfo, String participantDataId, RowSet data) throws DatastoreException, NotFoundException, IOException {
		List<String> participantIds = participantDataMappingManager.mapSynapseUserToParticipantIds(userInfo);
		String participantId = participantDataDAO.findParticipantForParticipantData(participantIds, participantDataId);
		if (participantId == null) {
			throw new NotFoundException("No data to update found for this user");
		}
		return participantDataDAO.update(participantId, participantDataId, data);
	}

	@Override
	public PaginatedRowSet getData(UserInfo userInfo, String participantDataId, Integer limit, Integer offset)
			throws DatastoreException, NotFoundException, IOException {
		List<String> participantIds = participantDataMappingManager.mapSynapseUserToParticipantIds(userInfo);
		String participantId = participantDataDAO.findParticipantForParticipantData(participantIds, participantDataId);
		if (participantId == null) {
			// User has never created data for this ParticipantData type, which is not an error, so return
			// empty result. It will have no headers given the way this works.
			return getEmptyPaginatedRowSet();
		}
		RowSet rowset = participantDataDAO.get(participantId, participantDataId);
		Long totalNumberOfResults = (long) rowset.getRows().size();
		rowset.setRows(PaginatedResultsUtil.prePaginate(rowset.getRows(), limit, offset));
		PaginatedRowSet paginatedRowSet = new PaginatedRowSet();
		paginatedRowSet.setResults(rowset);
		paginatedRowSet.setTotalNumberOfResults(totalNumberOfResults);
		return paginatedRowSet;
	}

	private PaginatedRowSet getEmptyPaginatedRowSet() {
		PaginatedRowSet paginatedRowSet = new PaginatedRowSet();
		RowSet rowSet = new RowSet();
		rowSet.setHeaders(new ArrayList<String>());
		rowSet.setRows(new ArrayList<Row>());
		paginatedRowSet.setResults(rowSet);
		paginatedRowSet.setTotalNumberOfResults(0L);
		return paginatedRowSet;
	}
}
