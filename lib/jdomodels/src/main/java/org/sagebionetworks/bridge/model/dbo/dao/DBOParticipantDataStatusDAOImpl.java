package org.sagebionetworks.bridge.model.dbo.dao;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.bridge.model.ParticipantDataStatusDAO;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatus;
import org.sagebionetworks.bridge.model.dbo.persistence.DBOParticipantDataStatus;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class DBOParticipantDataStatusDAOImpl implements ParticipantDataStatusDAO {

	private static final String PARTICIPANT_DATA_DESCRIPTOR_IDS = "participantDataDescriptorIds";

	private static final String SELECT_PARTICIPANT_DATA_STATUSES = "SELECT * FROM " + SqlConstants.TABLE_PARTICIPANT_DATA_STATUS + " WHERE "
			+ SqlConstants.COL_PARTICIPANT_DATA_PARTICIPANT_DATA_DESCRIPTOR_ID + " IN ( :" + PARTICIPANT_DATA_DESCRIPTOR_IDS + " )";

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	private static final RowMapper<DBOParticipantDataStatus> participantDataStatusMapper = (new DBOParticipantDataStatus()).getTableMapping();

	public DBOParticipantDataStatusDAOImpl() {
	}

	@Override
	public List<ParticipantDataDescriptor> getParticipantStatuses(List<ParticipantDataDescriptor> participantDatas) {
		if (!participantDatas.isEmpty()) {
			List<String> ids = getDescriptorIds(participantDatas);
			Map<Long, DBOParticipantDataStatus> dataStatusesMap = getDataStatuses(ids);

			for (ParticipantDataDescriptor descriptor : participantDatas) {
				Long participantDataDescriptorId = Long.parseLong(descriptor.getId());
				DBOParticipantDataStatus dataStatus = dataStatusesMap.get(participantDataDescriptorId);
				ParticipantDataStatus status;
				if (dataStatus != null) {
					status = dataStatus.getStatus();
				} else {
					status = new ParticipantDataStatus();
					status.setParticipantDataDescriptorId(descriptor.getId());
				}
				descriptor.setStatus(status);
			}
		}
		return participantDatas;
	}

	@Override
	public void update(List<ParticipantDataStatus> statuses) {
		List<String> ids = getStatusIds(statuses);

		Map<Long, DBOParticipantDataStatus> dataStatusesMap = getDataStatuses(ids);

		for (ParticipantDataStatus status : statuses) {
			Long participantDataDescriptorId = Long.parseLong(status.getParticipantDataDescriptorId());
			DBOParticipantDataStatus dataStatus = dataStatusesMap.get(participantDataDescriptorId);
			if (dataStatus != null) {
				dataStatus.setStatus(status);
			} else {
				dataStatus = new DBOParticipantDataStatus();
				dataStatus.setParticipantDataDescriptorId(participantDataDescriptorId);
				dataStatus.setStatus(status);
				dataStatusesMap.put(participantDataDescriptorId, dataStatus);
			}
		}

		basicDao.createOrUpdateBatch(Lists.newArrayList(dataStatusesMap.values()));
	}

	private Map<Long, DBOParticipantDataStatus> getDataStatuses(List<String> ids) {
		MapSqlParameterSource params = new MapSqlParameterSource().addValue(PARTICIPANT_DATA_DESCRIPTOR_IDS, ids);
		List<DBOParticipantDataStatus> dataStatuses = simpleJdbcTemplate.query(SELECT_PARTICIPANT_DATA_STATUSES, participantDataStatusMapper,
				params);

		Map<Long, DBOParticipantDataStatus> dataStatusesMap = Maps.newHashMap();
		for (DBOParticipantDataStatus dataStatus : dataStatuses) {
			dataStatusesMap.put(dataStatus.getParticipantDataDescriptorId(), dataStatus);
		}
		return dataStatusesMap;
	}

	private List<String> getDescriptorIds(List<ParticipantDataDescriptor> participantDatas) {
		return Lists.transform(participantDatas, new Function<ParticipantDataDescriptor, String>() {
			@Override
			public String apply(ParticipantDataDescriptor descriptor) {
				return descriptor.getId();
			}
		});
	}

	private List<String> getStatusIds(List<ParticipantDataStatus> statuses) {
		return Lists.transform(statuses, new Function<ParticipantDataStatus, String>() {
			@Override
			public String apply(ParticipantDataStatus status) {
				return status.getParticipantDataDescriptorId();
			}
		});
	}

}
