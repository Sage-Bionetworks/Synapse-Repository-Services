package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACTIVITY_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACTIVITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ACTIVITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_OWNER_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACTIVITY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REVISION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOActivity;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * @author dburdick
 *
 */
public class DBOActivityDAOImpl implements ActivityDAO {

	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static final String SQL_ETAG_WITHOUT_LOCK = "SELECT "+COL_ACTIVITY_ETAG+" FROM "+TABLE_ACTIVITY+" WHERE ID = ?";
	private static final String SQL_ETAG_FOR_UPDATE = SQL_ETAG_WITHOUT_LOCK+" FOR UPDATE";
	private static final String SQL_COUNT_ACTIVITY_ID = "SELECT COUNT("+COL_ACTIVITY_ID+") FROM "+TABLE_ACTIVITY+" WHERE "+COL_ACTIVITY_ID +" = :"+COL_ACTIVITY_ID;
	private static final String SQL_OFFSET_AND_LIMIT =	" LIMIT :" + LIMIT_PARAM_NAME +
														" OFFSET :" + OFFSET_PARAM_NAME;
	private static final String SQL_GET_NODES_FOR_ACTIVITY_FROMWHERE =
 															 " FROM " + TABLE_REVISION + " r" +
 															 " WHERE r." + COL_REVISION_ACTIVITY_ID + " = :" + COL_REVISION_ACTIVITY_ID; 															
	private static final String SQL_GET_NODES_FOR_ACTIVITY = "SELECT DISTINCT r." + COL_REVISION_OWNER_NODE + ", r." + COL_REVISION_NUMBER + 
																SQL_GET_NODES_FOR_ACTIVITY_FROMWHERE + SQL_OFFSET_AND_LIMIT; 
	private static final String SQL_GET_NODES_FOR_ACTIVITY_COUNT = "SELECT COUNT(r." + COL_REVISION_OWNER_NODE + ")" + SQL_GET_NODES_FOR_ACTIVITY_FROMWHERE;

	private static final String ACTIVITY_NOT_FOUND = "Activity with id '%1$s' could not be found.";

	@WriteTransaction
	@Override
	public String create(Activity dto) throws DatastoreException, InvalidModelException {
		DBOActivity dbo = new DBOActivity();
		ActivityUtils.copyDtoToDbo(dto, dbo);
		
		// Change the etag
		dbo.seteTag(UUID.randomUUID().toString());

		basicDao.createNew(dbo);
		return dbo.getIdString();
	}

	@WriteTransaction
	@Override
	public Activity update(Activity dto) throws DatastoreException,
			InvalidModelException,NotFoundException, ConflictingUpdateException {
		DBOActivity dbo = getDBO(dto.getId());
		ActivityUtils.copyDtoToDbo(dto, dbo);
		
		boolean success = basicDao.update(dbo);

		if (!success) throw new DatastoreException("Unsuccessful updating user Activity in database.");
		Activity updatedActivity = ActivityUtils.copyDboToDto(dbo);

		return updatedActivity;
	} // the 'commit' is implicit in returning from a method annotated 'Transactional'
	
	@Override
	public Activity get(String id) throws DatastoreException, NotFoundException {
		DBOActivity dbo = getDBO(id);		
		return ActivityUtils.copyDboToDto(dbo);
	}
	
	@WriteTransaction
	@Override
	public void delete(String id) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACTIVITY_ID.toLowerCase(), id);
		try {
			basicDao.deleteObjectByPrimaryKey(DBOActivity.class, param);
		} catch (DataIntegrityViolationException e) {
			throw new IllegalArgumentException("If you wish to delete this activity, please first delete all Entities generated by this Activity.", e);
		}
	}

	@Override
	public long getCount() throws DatastoreException {
		return basicDao.getCount(DBOActivity.class);
	}
	
	@WriteTransaction
	@Override
	public String lockActivityAndGenerateEtag(String id, String eTag, ChangeType changeType)
			throws NotFoundException, ConflictingUpdateException, DatastoreException {
		String currentTag = lockActivity(id);
		// Check the eTags
		if(!currentTag.equals(eTag)){
			throw new ConflictingUpdateException("Node: "+id+" was updated since you last fetched it, retrieve it again and reapply the update");
		}
		// Get a new etag
		DBOActivity dbo = getDBO(id);
		dbo.seteTag(UUID.randomUUID().toString());
		return dbo.getEtag();
	}

	@Override
	public boolean doesActivityExist(String id) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(COL_ACTIVITY_ID, id);
		try{
			long count = namedJdbcTemplate.queryForObject(SQL_COUNT_ACTIVITY_ID, parameters, Long.class);
			return count > 0;
		}catch(Exception e){
			// Can occur when the schema does not exist.
			return false;
		}
	}

	@Override
	public PaginatedResults<Reference> getEntitiesGeneratedBy(String activityId, int limit, int offset) {
		if(activityId == null) throw new IllegalArgumentException("Activity id can not be null");
		if(limit < 0 || offset < 0) throw new IllegalArgumentException("limit and offset must be greater than 0");
		// get one page of node ids
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_REVISION_ACTIVITY_ID, activityId);
		params.addValue(OFFSET_PARAM_NAME, offset);
		params.addValue(LIMIT_PARAM_NAME, limit);

		List<Reference> generatedBy = null;
		generatedBy = namedJdbcTemplate.query(SQL_GET_NODES_FOR_ACTIVITY, params, new RowMapper<Reference>() {
			@Override
			public Reference mapRow(ResultSet rs, int rowNum) throws SQLException {
				Reference ref = new Reference();
				Long targetId = rs.getLong(COL_REVISION_OWNER_NODE);
				if(rs.wasNull()) targetId = null;
				Long versionNumber = rs.getLong(COL_REVISION_NUMBER);
				if(rs.wasNull()) versionNumber = null;				
				ref.setTargetId(KeyFactory.keyToString(targetId));
				ref.setTargetVersionNumber(versionNumber);
				return ref;
			}
		});
			
		// return the page of objects, along with the total result count
		PaginatedResults<Reference> queryResults = new PaginatedResults<Reference>();
		queryResults.setResults(generatedBy);
		long totalCount = 0;
		try {
			totalCount = namedJdbcTemplate.queryForObject(SQL_GET_NODES_FOR_ACTIVITY_COUNT, params, Long.class);
		} catch (EmptyResultDataAccessException e) {
			// count = 0
		}
		queryResults.setTotalNumberOfResults(totalCount);
		return queryResults;
	}

	/*
	 * Private Methods
	 */
	private DBOActivity getDBO(String id) throws NotFoundException {
		if(id == null) throw new NotFoundException("No activity");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACTIVITY_ID.toLowerCase(), id);
		try {
			DBOActivity dbo = basicDao.getObjectByPrimaryKey(DBOActivity.class, param);
			return dbo;
		} catch (NotFoundException e) {
			throw new NotFoundException(String.format(ACTIVITY_NOT_FOUND, id));
		}
	}

	private String lockActivity(String id) {
		// Create a Select for update query
		return jdbcTemplate.queryForObject(SQL_ETAG_FOR_UPDATE, String.class, id);
	}
}
