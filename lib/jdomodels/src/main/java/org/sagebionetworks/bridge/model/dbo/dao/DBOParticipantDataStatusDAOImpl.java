package org.sagebionetworks.bridge.model.dbo.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.bridge.model.ParticipantDataId;
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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

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
	public void getParticipantStatuses(List<ParticipantDataDescriptor> participantDataDescriptors,
			Map<String, ParticipantDataId> participantDataDescriptorToDataIdMap) {
		if (!participantDataDescriptors.isEmpty()) {
			Map<ParticipantDataId, DBOParticipantDataStatus> dataStatusesMap = getDataStatuses(participantDataDescriptorToDataIdMap.values());

			for (ParticipantDataDescriptor dataDescriptor : participantDataDescriptors) {
				ParticipantDataStatus status = null;

				ParticipantDataId dataId = participantDataDescriptorToDataIdMap.get(dataDescriptor.getId());
				if (dataId != null) {
					DBOParticipantDataStatus dboDataStatus = dataStatusesMap.get(dataId);
					if (dboDataStatus != null) {
						status = dboDataStatus.getStatus();
					}
				}
				if (status == null) {
					status = new ParticipantDataStatus();
					status.setParticipantDataDescriptorId(dataDescriptor.getId());
				}
				dataDescriptor.setStatus(status);
			}
		}
	}

	@Override
	public ParticipantDataStatus getParticipantStatus(ParticipantDataId participantDataId, ParticipantDataDescriptor participantDataDescriptor)
			throws DatastoreException {
		DBOParticipantDataStatus dboStatus;
		try {
			dboStatus = basicDao.getObjectByPrimaryKey(DBOParticipantDataStatus.class, new SinglePrimaryKeySqlParameterSource(
					participantDataId.getId()));
			return dboStatus.getStatus();
		} catch (NotFoundException e) {
			ParticipantDataStatus status = new ParticipantDataStatus();
			status.setParticipantDataDescriptorId(participantDataDescriptor.getId());
			return status;
		}
	}

	@Override
	public void update(List<ParticipantDataStatus> statuses, Map<String, ParticipantDataId> participantDataDescriptorToDataIdMap) {
		Map<ParticipantDataId, DBOParticipantDataStatus> dataStatusesMap = getDataStatuses(participantDataDescriptorToDataIdMap.values());

		List<DBOParticipantDataStatus> statusesToUpdate = Lists.newArrayListWithExpectedSize(statuses.size());
		for (ParticipantDataStatus status : statuses) {
			DBOParticipantDataStatus dboDataStatus = dataStatusesMap.get(status.getParticipantDataDescriptorId());
			if (dboDataStatus == null) {
				ParticipantDataId dataId = participantDataDescriptorToDataIdMap.get(status.getParticipantDataDescriptorId());
				if (dataId != null) {
					dboDataStatus = new DBOParticipantDataStatus();
					dboDataStatus.setParticipantDataId(dataId.getId());
					dboDataStatus.setParticipantDataDescriptorId(Long.parseLong(status.getParticipantDataDescriptorId()));
					dboDataStatus.setStatus(status);
				}
			}
			if (dboDataStatus != null) {
				dboDataStatus.setStatus(status);
				statusesToUpdate.add(dboDataStatus);
			}
		}

		basicDao.createOrUpdateBatch(statusesToUpdate);
	}

	private Map<ParticipantDataId, DBOParticipantDataStatus> getDataStatuses(Collection<ParticipantDataId> participantDataIds) {
		MapSqlParameterSource params = new MapSqlParameterSource().addValue(PARTICIPANT_DATA_IDS,
				ParticipantDataId.convert(participantDataIds));
		List<DBOParticipantDataStatus> dataStatuses = simpleJdbcTemplate.query(SELECT_PARTICIPANT_DATA_STATUSES, participantDataStatusMapper,
				params);

		Map<ParticipantDataId, DBOParticipantDataStatus> dataStatusesMap = Maps.newHashMap();
		for (DBOParticipantDataStatus dataStatus : dataStatuses) {
			dataStatusesMap.put(new ParticipantDataId(dataStatus.getParticipantDataId()), dataStatus);
		}
		return dataStatusesMap;
	}
}
