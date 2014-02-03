package org.sagebionetworks.bridge.model.dbo.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sagebionetworks.bridge.model.ParticipantDataStatusDAO;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatus;
import org.sagebionetworks.bridge.model.dbo.persistence.DBOParticipantDataStatus;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class DBOParticipantDataStatusDAOImpl implements ParticipantDataStatusDAO {

	private static final String PARTICIPANT_DATA_IDS = "participantDataIds";

	private static final String SELECT_PARTICIPANT_DATA_STATUSES = "SELECT * FROM " + SqlConstants.TABLE_PARTICIPANT_DATA_STATUS + " WHERE "
			+ SqlConstants.COL_PARTICIPANT_DATA_STATUS_PARTICIPANT_DATA_ID + " IN ( :" + PARTICIPANT_DATA_IDS + " )";

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	private static final RowMapper<DBOParticipantDataStatus> participantDataStatusMapper = (new DBOParticipantDataStatus()).getTableMapping();

	public DBOParticipantDataStatusDAOImpl() {
	}

	@Override
	public void getParticipantStatuses(Map<String, ParticipantDataDescriptor> participantDataDescriptors) {
		if (!participantDataDescriptors.isEmpty()) {
			Map<String, DBOParticipantDataStatus> dataStatusesMap = getDataStatuses(participantDataDescriptors.keySet());

			for (Entry<String, ParticipantDataDescriptor> entry : participantDataDescriptors.entrySet()) {
				DBOParticipantDataStatus dboDataStatus = dataStatusesMap.get(entry.getKey());
				ParticipantDataStatus status;
				if (dboDataStatus != null) {
					status = dboDataStatus.getStatus();
				} else {
					status = new ParticipantDataStatus();
				}
				status.setParticipantDataDescriptorId(entry.getValue().getId());
				entry.getValue().setStatus(status);
			}
		}
	}

	@Override
	public ParticipantDataStatus getParticipantStatus(String participantDataId, ParticipantDataDescriptor participantDataDescriptor)
			throws DatastoreException {
		DBOParticipantDataStatus dboStatus;
		try {
			dboStatus = basicDao.getObjectByPrimaryKey(DBOParticipantDataStatus.class, new SinglePrimaryKeySqlParameterSource(
					participantDataId));
			return dboStatus.getStatus();
		} catch (NotFoundException e) {
			ParticipantDataStatus status = new ParticipantDataStatus();
			status.setParticipantDataDescriptorId(participantDataDescriptor.getId());
			return status;
		}
	}

	@Override
	public void update(List<ParticipantDataStatus> statuses, Map<String, ParticipantDataDescriptor> participantDataDescriptors) {

		Map<String, DBOParticipantDataStatus> dataStatusesMap = getDataStatuses(participantDataDescriptors.keySet());

		Map<String, String> descriptorIdToDataId = Maps.newHashMap();
		for (Entry<String, ParticipantDataDescriptor> entry : participantDataDescriptors.entrySet()) {
			descriptorIdToDataId.put(entry.getValue().getId(), entry.getKey());
		}

		for (ParticipantDataStatus status : statuses) {
			DBOParticipantDataStatus dboDataStatus = dataStatusesMap.get(status.getParticipantDataDescriptorId());
			if (dboDataStatus != null) {
				dboDataStatus.setStatus(status);
			} else {
				String participantDataId = descriptorIdToDataId.get(status.getParticipantDataDescriptorId());
				dboDataStatus = new DBOParticipantDataStatus();
				dboDataStatus.setParticipantDataId(Long.parseLong(participantDataId));
				dboDataStatus.setParticipantDataDescriptorId(Long.parseLong(status.getParticipantDataDescriptorId()));
				dboDataStatus.setStatus(status);
				dataStatusesMap.put(status.getParticipantDataDescriptorId(), dboDataStatus);
			}
		}

		basicDao.createOrUpdateBatch(Lists.newArrayList(dataStatusesMap.values()));
	}

	private Map<String, DBOParticipantDataStatus> getDataStatuses(Collection<String> ids) {
		MapSqlParameterSource params = new MapSqlParameterSource().addValue(PARTICIPANT_DATA_IDS, ids);
		List<DBOParticipantDataStatus> dataStatuses = simpleJdbcTemplate.query(SELECT_PARTICIPANT_DATA_STATUSES, participantDataStatusMapper,
				params);

		Map<String, DBOParticipantDataStatus> dataStatusesMap = Maps.newHashMap();
		for (DBOParticipantDataStatus dataStatus : dataStatuses) {
			dataStatusesMap.put(dataStatus.getParticipantDataId().toString(), dataStatus);
		}
		return dataStatusesMap;
	}
}
