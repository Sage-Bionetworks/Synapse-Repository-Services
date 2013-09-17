package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TRASH_CAN;

import java.sql.Timestamp;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.dao.TrashCanBackupDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTrashedEntity;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class DBOTrashCanBackupDaoImpl implements TrashCanBackupDao {

	private static final String SELECT_TRASH_BY_NODE_ID =
			"SELECT * FROM " + TABLE_TRASH_CAN
			+ " WHERE " + COL_TRASH_CAN_NODE_ID + " = :" + COL_TRASH_CAN_NODE_ID;

	private static final String SELECT_COUNT =
			"SELECT COUNT(" + COL_TRASH_CAN_NODE_ID + ") FROM " + TABLE_TRASH_CAN;

	private static final String SELECT_TRASH_IN_RANGE =
			"SELECT * FROM " + TABLE_TRASH_CAN
			+ " LIMIT :" + LIMIT_PARAM_NAME + " OFFSET :" + OFFSET_PARAM_NAME;

	private static final RowMapper<DBOTrashedEntity> rowMapper = (new DBOTrashedEntity()).getTableMapping();

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Override
	public TrashedEntity get(String entityId) {

		if (entityId == null) {
			throw new IllegalArgumentException("entityId cannot be null.");
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		Long nodeId = KeyFactory.stringToKey(entityId);
		paramMap.addValue(COL_TRASH_CAN_NODE_ID, nodeId);
		List<DBOTrashedEntity> dboList = simpleJdbcTemplate.query(SELECT_TRASH_BY_NODE_ID, rowMapper, paramMap);
		if (dboList.isEmpty()) {
			return null;
		}
		if (dboList.size() > 1) {
			throw new DatastoreException("Entity " + entityId
					+ " fetched back more than 1 entry from the trash can table.");
		}

		return TrashedEntityUtils.convertDboToDto(dboList.get(0));
	}

	@Override
	public void delete(String entityId) {

		if (entityId == null) {
			throw new IllegalArgumentException("entityId cannot be null.");
		}

		MapSqlParameterSource params = new MapSqlParameterSource();
		Long nodeId = KeyFactory.stringToKey(entityId);
		params.addValue("nodeId", nodeId);
		basicDao.deleteObjectByPrimaryKey(DBOTrashedEntity.class, params);
	}

	@Override
	public void update(TrashedEntity entity) {

		if (entity == null) {
			throw new IllegalArgumentException("entity cannot be null.");
		}

		DBOTrashedEntity dbo = new DBOTrashedEntity();
		dbo.setNodeId(KeyFactory.stringToKey(entity.getEntityId()));
		dbo.setNodeName(entity.getEntityName());
		dbo.setDeletedBy(KeyFactory.stringToKey(entity.getDeletedByPrincipalId()));
		dbo.setDeletedOn(new Timestamp(entity.getDeletedOn().getTime()));
		dbo.setParentId(KeyFactory.stringToKey(entity.getOriginalParentId()));

		if (get(entity.getEntityId()) == null) {
			basicDao.createNew(dbo);
		} else {
			basicDao.update(dbo);
		}
	}

	@Override
	public long getCount() throws DatastoreException {
		return simpleJdbcTemplate.queryForLong(SELECT_COUNT);
	}
}
