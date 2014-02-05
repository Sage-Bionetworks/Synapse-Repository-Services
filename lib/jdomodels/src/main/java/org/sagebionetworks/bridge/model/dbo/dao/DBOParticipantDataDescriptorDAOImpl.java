/**
 * 
 */
package org.sagebionetworks.bridge.model.dbo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.bridge.model.ParticipantDataDescriptorDAO;
import org.sagebionetworks.bridge.model.ParticipantDataId;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.dbo.persistence.DBOParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.dbo.persistence.DBOParticipantDataDescriptor;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class DBOParticipantDataDescriptorDAOImpl implements ParticipantDataDescriptorDAO {

	private static final String PARTICIPANT_DATA_IDS = "participantDataIds";
	private static final String PARTICIPANT_DATA_ID = "participantDataId";

	private static final String SELECT_PARTICIPANT_DATA_DESCRIPTORS = "SELECT * FROM " + SqlConstants.TABLE_PARTICIPANT_DATA_DESCRIPTOR;
	private static final String SELECT_PARTICIPANT_DATA_FOR_USER = "select d." + SqlConstants.COL_PARTICIPANT_DATA_PARTICIPANT_DATA_ID
			+ " as " + PARTICIPANT_DATA_ID + ", dd.* from " + SqlConstants.TABLE_PARTICIPANT_DATA_DESCRIPTOR + " dd join "
			+ SqlConstants.TABLE_PARTICIPANT_DATA + " d on dd." + SqlConstants.COL_PARTICIPANT_DATA_DESCRIPTOR_ID + " = d."
			+ SqlConstants.COL_PARTICIPANT_DATA_PARTICIPANT_DATA_DESCRIPTOR_ID + " where d."
			+ SqlConstants.COL_PARTICIPANT_DATA_PARTICIPANT_DATA_ID + " IN ( :" + PARTICIPANT_DATA_IDS + " )";
	private static final String SELECT_PARTICIPANT_DATA_COLUMN_DESCRIPTORS = "SELECT * FROM "
			+ SqlConstants.TABLE_PARTICIPANT_DATA_COLUMN_DESCRIPTOR + " WHERE "
			+ SqlConstants.COL_PARTICIPANT_DATA_COLUMN_DESCRIPTOR_PARTICIPANT_DATA_ID + " = ?";

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;

	private static final RowMapper<DBOParticipantDataDescriptor> participantDataDescriptorRowMapper = (new DBOParticipantDataDescriptor())
			.getTableMapping();
	private static final RowMapper<DBOParticipantDataColumnDescriptor> participantDataColumnDescriptorRowMapper = (new DBOParticipantDataColumnDescriptor())
			.getTableMapping();

	private static final Function<DBOParticipantDataDescriptor, ParticipantDataDescriptor> dboToDtoParticipantDataDescriptor = new Function<DBOParticipantDataDescriptor, ParticipantDataDescriptor>() {
		@Override
		public ParticipantDataDescriptor apply(DBOParticipantDataDescriptor dboParticipantDataDescriptor) {
			ParticipantDataDescriptor participantDataDescriptor = new ParticipantDataDescriptor();
			participantDataDescriptor.setId(Long.toString(dboParticipantDataDescriptor.getId()));
			participantDataDescriptor.setName(dboParticipantDataDescriptor.getName());
			participantDataDescriptor.setDescription(dboParticipantDataDescriptor.getDescription());
			participantDataDescriptor.setRepeatType(dboParticipantDataDescriptor.getRepeatType());
			participantDataDescriptor.setRepeatFrequency(dboParticipantDataDescriptor.getRepeatFrequency());
			participantDataDescriptor.setDatetimeStartColumnName(dboParticipantDataDescriptor.getDatetimeStartColumnName());
			participantDataDescriptor.setDatetimeEndColumnName(dboParticipantDataDescriptor.getDatetimeEndColumnName());
			return participantDataDescriptor;
		}
	};

	private static final Function<ParticipantDataDescriptor, DBOParticipantDataDescriptor> dtoToDboParticipantDataDescriptor = new Function<ParticipantDataDescriptor, DBOParticipantDataDescriptor>() {
		@Override
		public DBOParticipantDataDescriptor apply(ParticipantDataDescriptor participantDataDescriptor) {
			DBOParticipantDataDescriptor dboParticipantDataDescriptor = new DBOParticipantDataDescriptor();
			dboParticipantDataDescriptor.setId(participantDataDescriptor.getId() != null ? Long.parseLong(participantDataDescriptor.getId())
					: null);
			dboParticipantDataDescriptor.setName(participantDataDescriptor.getName());
			dboParticipantDataDescriptor.setDescription(participantDataDescriptor.getDescription());
			dboParticipantDataDescriptor.setRepeatType(participantDataDescriptor.getRepeatType());
			dboParticipantDataDescriptor.setRepeatFrequency(participantDataDescriptor.getRepeatFrequency());
			dboParticipantDataDescriptor.setDatetimeStartColumnName(participantDataDescriptor.getDatetimeStartColumnName());
			dboParticipantDataDescriptor.setDatetimeEndColumnName(participantDataDescriptor.getDatetimeEndColumnName());
			return dboParticipantDataDescriptor;
		}
	};

	private static final Function<DBOParticipantDataColumnDescriptor, ParticipantDataColumnDescriptor> dboToDtoParticipantDataColumnDescriptor = new Function<DBOParticipantDataColumnDescriptor, ParticipantDataColumnDescriptor>() {
		@Override
		public ParticipantDataColumnDescriptor apply(DBOParticipantDataColumnDescriptor dboParticipantDataColumnDescriptor) {
			ParticipantDataColumnDescriptor participantDataColumnDescriptor = dboParticipantDataColumnDescriptor
					.getParticipantDataColumnDescriptor();
			participantDataColumnDescriptor.setId(dboParticipantDataColumnDescriptor.getId().toString());
			participantDataColumnDescriptor.setName(dboParticipantDataColumnDescriptor.getName());
			participantDataColumnDescriptor.setParticipantDataDescriptorId(dboParticipantDataColumnDescriptor
					.getParticipantDataDescriptorId().toString());
			return participantDataColumnDescriptor;
		}
	};

	@Override
	public List<ParticipantDataDescriptor> getParticipantDatas() {
		List<DBOParticipantDataDescriptor> dboParticipantDataDescriptors = simpleJdbcTemplate.query(SELECT_PARTICIPANT_DATA_DESCRIPTORS,
				participantDataDescriptorRowMapper);
		return Lists.transform(dboParticipantDataDescriptors, dboToDtoParticipantDataDescriptor);
	}

	@Override
	public Map<ParticipantDataId, ParticipantDataDescriptor> getParticipantDataDescriptorsForUser(List<ParticipantDataId> participantDataIds) {
		if (participantDataIds.isEmpty()) {
			return Collections.<ParticipantDataId, ParticipantDataDescriptor> emptyMap();
		}
		MapSqlParameterSource params = new MapSqlParameterSource().addValue(PARTICIPANT_DATA_IDS,
				ParticipantDataId.convert(participantDataIds));
		final Map<ParticipantDataId, ParticipantDataDescriptor> participantDataDescriptors = Maps.newHashMap();
		simpleJdbcTemplate.query(SELECT_PARTICIPANT_DATA_FOR_USER, new RowMapper<Object>() {
			@Override
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOParticipantDataDescriptor dboParticipantDataDescriptor = participantDataDescriptorRowMapper.mapRow(rs, rowNum);
				long participantDataId = rs.getLong(PARTICIPANT_DATA_ID);
				ParticipantDataDescriptor participantDataDescriptor = dboToDtoParticipantDataDescriptor.apply(dboParticipantDataDescriptor);
				participantDataDescriptors.put(new ParticipantDataId(participantDataId), participantDataDescriptor);
				return participantDataId;
			}
		}, params);
		return participantDataDescriptors;
	}

	@Override
	public List<ParticipantDataColumnDescriptor> getParticipantDataColumns(String participantDataDescriptorId) {
		List<DBOParticipantDataColumnDescriptor> dboParticipantDataColumnDescriptors = simpleJdbcTemplate.query(
				SELECT_PARTICIPANT_DATA_COLUMN_DESCRIPTORS, participantDataColumnDescriptorRowMapper, participantDataDescriptorId);
		return Lists.transform(dboParticipantDataColumnDescriptors, dboToDtoParticipantDataColumnDescriptor);
	}

	@Override
	public ParticipantDataDescriptor getParticipantDataDescriptor(String participantDataDescriptorId) throws DatastoreException,
			NotFoundException {
		DBOParticipantDataDescriptor dboParticipantDataDescriptor = basicDao.getObjectByPrimaryKey(DBOParticipantDataDescriptor.class,
				new SinglePrimaryKeySqlParameterSource(participantDataDescriptorId));
		return dboToDtoParticipantDataDescriptor.apply(dboParticipantDataDescriptor);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ParticipantDataDescriptor createParticipantDataDescriptor(ParticipantDataDescriptor participantDataDescriptor) {
		DBOParticipantDataDescriptor dboParticipantDataDescriptor = dtoToDboParticipantDataDescriptor.apply(participantDataDescriptor);
		dboParticipantDataDescriptor.setId(idGenerator.generateNewId(TYPE.COLUMN_MODEL_ID));
		dboParticipantDataDescriptor = basicDao.createNew(dboParticipantDataDescriptor);
		return dboToDtoParticipantDataDescriptor.apply(dboParticipantDataDescriptor);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateParticipantDataDescriptor(ParticipantDataDescriptor participantDataDescriptor) throws NotFoundException {
		DBOParticipantDataDescriptor dboParticipantDataDescriptor = dtoToDboParticipantDataDescriptor.apply(participantDataDescriptor);
		if (!basicDao.update(dboParticipantDataDescriptor)) {
			throw new NotFoundException("Update for ParticipantDataDescriptor " + participantDataDescriptor.getId()
					+ " found nothing to update");
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteParticipantDataDescriptor(String participantDataDescriptorId) {
		basicDao.deleteObjectByPrimaryKey(DBOParticipantDataDescriptor.class, new SinglePrimaryKeySqlParameterSource(
				participantDataDescriptorId));
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ParticipantDataColumnDescriptor createParticipantDataColumnDescriptor(
			ParticipantDataColumnDescriptor participantDataColumnDescriptor) {
		DBOParticipantDataColumnDescriptor dboParticipantDataColumnDescriptor = new DBOParticipantDataColumnDescriptor();
		dboParticipantDataColumnDescriptor.setId(idGenerator.generateNewId(TYPE.COLUMN_MODEL_ID));
		dboParticipantDataColumnDescriptor.setParticipantDataDescriptorId(Long.parseLong(participantDataColumnDescriptor
				.getParticipantDataDescriptorId()));
		dboParticipantDataColumnDescriptor.setName(participantDataColumnDescriptor.getName());
		dboParticipantDataColumnDescriptor.setParticipantDataColumnDescriptor(participantDataColumnDescriptor);
		dboParticipantDataColumnDescriptor = basicDao.createNew(dboParticipantDataColumnDescriptor);
		return dboToDtoParticipantDataColumnDescriptor.apply(dboParticipantDataColumnDescriptor);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateParticipantDataColumnDescriptor(ParticipantDataColumnDescriptor participantDataColumnDescriptor)
			throws NotFoundException {
		DBOParticipantDataColumnDescriptor dboParticipantDataColumnDescriptor = new DBOParticipantDataColumnDescriptor();
		dboParticipantDataColumnDescriptor.setParticipantDataDescriptorId(Long.parseLong(participantDataColumnDescriptor
				.getParticipantDataDescriptorId()));
		dboParticipantDataColumnDescriptor.setId(Long.parseLong(participantDataColumnDescriptor.getId()));
		dboParticipantDataColumnDescriptor.setName(participantDataColumnDescriptor.getName());
		dboParticipantDataColumnDescriptor.setParticipantDataColumnDescriptor(participantDataColumnDescriptor);
		if (!basicDao.update(dboParticipantDataColumnDescriptor)) {
			throw new NotFoundException("Update for ParticipantDataColumnDescriptor " + participantDataColumnDescriptor.getId()
					+ " found nothing to update");
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteParticipantDataColumnDescriptor(String participantDataDescriptorId, String participantDataColumnDescriptorId) {
		MapSqlParameterSource param = new MapSqlParameterSource().addValue(
				DBOParticipantDataColumnDescriptor.PARTICIPANT_DATA_DESCRIPTOR_ID_FIELD, participantDataDescriptorId).addValue(
				DBOParticipantDataColumnDescriptor.PARTICIPANT_DATA_COLUMN_DESCRIPTOR_ID_FIELD,
				Long.parseLong(participantDataColumnDescriptorId));
		basicDao.deleteObjectByPrimaryKey(DBOParticipantDataColumnDescriptor.class, param);
	}
}
