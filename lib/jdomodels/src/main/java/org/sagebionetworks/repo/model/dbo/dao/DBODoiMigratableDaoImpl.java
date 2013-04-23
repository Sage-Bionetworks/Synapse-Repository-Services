package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOI;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DoiMigratableDao;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBODoi;
import org.sagebionetworks.repo.model.doi.Doi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBODoiMigratableDaoImpl implements DoiMigratableDao {

	private static final String SELECT_COUNT =
			"SELECT COUNT(" + COL_DOI_ID  + ") FROM " + TABLE_DOI;

	private static final String SELECT_DOI_BY_ID = 
			"SELECT * FROM " + TABLE_DOI + " WHERE "
			+ COL_DOI_ID + " = :" + COL_DOI_ID;

	private static final String SELECT_DOI_IN_RANGE =
			"SELECT * FROM " + TABLE_DOI
			+ " LIMIT :" + LIMIT_PARAM_NAME
			+ " OFFSET :" + OFFSET_PARAM_NAME;

	private static final RowMapper<DBODoi> rowMapper = (new DBODoi()).getTableMapping();

	@Autowired private SimpleJdbcTemplate simpleJdbcTemplate;
	@Autowired private DBOBasicDao basicDao;

	@Override
	public long getCount() throws DatastoreException {
		return simpleJdbcTemplate.queryForLong(SELECT_COUNT);
	}

	@Override
	public QueryResults<MigratableObjectData> getMigrationObjectData(
			long offset, long limit, boolean includeDependencies)
			throws DatastoreException {

		if (limit < 0) {
			throw new IllegalArgumentException("limit must be greater than 0");
		}
		if (offset < 0) {
			throw new IllegalArgumentException("offset must be greater than 0");
		}

		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(OFFSET_PARAM_NAME, offset);
		param.addValue(LIMIT_PARAM_NAME, limit);
		List<MigratableObjectData> list = simpleJdbcTemplate.query(SELECT_DOI_IN_RANGE,

				new RowMapper<MigratableObjectData>() {
					@Override
					public MigratableObjectData mapRow(ResultSet rs, int rowNum) throws SQLException {
						String id = rs.getString(COL_DOI_ID);
						MigratableObjectData objectData = new MigratableObjectData();
						MigratableObjectDescriptor od = new MigratableObjectDescriptor();
						od.setId(id);
						od.setType(MigratableObjectType.DOI);
						objectData.setId(od);
						objectData.setDependencies(new HashSet<MigratableObjectDescriptor>(0));
						objectData.setEtag(rs.getString(COL_DOI_ETAG));
						return objectData;
					}
				}, param);

		QueryResults<MigratableObjectData> queryResults = new QueryResults<MigratableObjectData>();
		queryResults.setResults(list);
		queryResults.setTotalNumberOfResults((int)getCount());
		return queryResults;
	}

	@Override
	public MigratableObjectType getMigratableObjectType() {
		return MigratableObjectType.DOI;
	}

	@Override
	public Doi get(String id) throws DatastoreException {

		if (id == null) {
			throw new IllegalArgumentException("ID cannot be null.");
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_DOI_ID, id);
		List<DBODoi> dboList = simpleJdbcTemplate.query(SELECT_DOI_BY_ID, rowMapper, paramMap);
		if (dboList.isEmpty()) {
			return null;
		}
		return DoiUtils.convertToDto(dboList.get(0));
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean createOrUpdate(Doi backup) throws DatastoreException {

		if (backup == null) {
			throw new IllegalArgumentException("Backup object cannot be null.");
		}

		DBODoi dbo = DoiUtils.convertToDbo(backup);
		if (get(backup.getId()) == null) {
			basicDao.createNew(dbo);
			return true;
		}
		return basicDao.update(dbo);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) throws DatastoreException {
		if (id == null) {
			throw new IllegalArgumentException("ID cannot be null.");
		}
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_DOI_ID.toLowerCase(), id);
		basicDao.deleteObjectByPrimaryKey(DBODoi.class, param);
	}
}
