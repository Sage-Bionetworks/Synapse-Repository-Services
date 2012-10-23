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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOActivity;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


/**
 * @author dburdick
 *
 */
public class DBOActivityDAOImpl implements ActivityDAO {
	
	@Autowired
	private TagMessenger tagMessenger;
	@Autowired
	private DBOBasicDao basicDao;	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;	
	
	private static final String SELECT_ALL_IDS_SQL = "SELECT " + SqlConstants.COL_ACTIVITY_ID + " FROM " + SqlConstants.TABLE_ACTIVITY;

	private static final String SELECT_FOR_RANGE_SQL = "SELECT * FROM " + TABLE_ACTIVITY 
			+ " ORDER BY " + COL_ACTIVITY_ID 
			+ " LIMIT :" + LIMIT_PARAM_NAME 
			+ " OFFSET :" + OFFSET_PARAM_NAME;

	private static final String SQL_ETAG_WITHOUT_LOCK = "SELECT "+COL_ACTIVITY_ETAG+" FROM "+TABLE_ACTIVITY+" WHERE ID = ?";
	private static final String SQL_ETAG_FOR_UPDATE = SQL_ETAG_WITHOUT_LOCK+" FOR UPDATE";
	private static final String UPDATE_ETAG_SQL = "UPDATE "+TABLE_ACTIVITY+" SET "+COL_ACTIVITY_ETAG+" = ? WHERE "+COL_ACTIVITY_ID+" = ?";
	private static final String SQL_COUNT_ACTIVITY_ID = "SELECT COUNT("+COL_ACTIVITY_ID+") FROM "+TABLE_ACTIVITY+" WHERE "+COL_ACTIVITY_ID +" = :"+COL_ACTIVITY_ID;
	private static final String SQL_GET_NODES_FOR_ACTIVITY = "SELECT " + COL_REVISION_OWNER_NODE + ", " + COL_REVISION_NUMBER +
															 " FROM " + TABLE_REVISION + 
															 " WHERE " + COL_REVISION_ACTIVITY_ID + " = :" + COL_REVISION_ACTIVITY_ID; 

	
	public DBOActivityDAOImpl() { }
	
	public DBOActivityDAOImpl(TagMessenger tagMessenger, DBOBasicDao basicDao,
			SimpleJdbcTemplate simpleJdbcTemplate) {
		super();
		this.tagMessenger = tagMessenger;
		this.basicDao = basicDao;
		this.simpleJdbcTemplate = simpleJdbcTemplate;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends Activity> T create(T dto) throws DatastoreException, InvalidModelException {	
		DBOActivity dbo = new DBOActivity();
		ActivityUtils.copyDtoToDbo(dto, dbo);
		// add eTag
		tagMessenger.generateEtagAndSendMessage(dbo, ChangeType.CREATE);
		dbo = basicDao.createNew(dbo);		
		T result = ActivityUtils.copyDboToDto(dbo);
		return result;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends Activity> T update(T dto) throws DatastoreException,
			InvalidModelException,NotFoundException, ConflictingUpdateException {
		return update(dto, false);
	}

	/**
	 * @param fromBackup Whether we are updating from backup.
	 *                   Skip optimistic locking and accept the backup e-tag when restoring from backup.
	 */
	private <T extends Activity> T update(T dto, boolean fromBackup) throws DatastoreException,
			InvalidModelException,NotFoundException, ConflictingUpdateException {		
		if(!doesActivityExist(dto.getId())) throw new NotFoundException("Activity with id " + dto.getId() + " could not be found.");
		DBOActivity dbo = getDBO(dto.getId());
		ActivityUtils.copyDtoToDbo(dto, dbo);
		boolean success = basicDao.update(dbo);

		if (!success) throw new DatastoreException("Unsuccessful updating user Activity in database.");
		T updatedActivity = ActivityUtils.copyDboToDto(dbo);

		return updatedActivity;
	} // the 'commit' is implicit in returning from a method annotated 'Transactional'
	
	
	@Override
	public Activity get(String id) throws DatastoreException, NotFoundException {
		if(!doesActivityExist(id)) throw new NotFoundException("Activity with id " + id + " could not be found.");
		DBOActivity dbo = getDBO(id);		
		return ActivityUtils.copyDboToDto(dbo);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		if(!doesActivityExist(id)) throw new NotFoundException("Activity with id " + id + " could not be found.");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACTIVITY_ID.toLowerCase(), id);
		basicDao.deleteObjectById(DBOActivity.class, param);
	}
	
	@Override
	public List<String> getIds() {		
		return simpleJdbcTemplate.query(SELECT_ALL_IDS_SQL, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString(SqlConstants.COL_ACTIVITY_ID);
			}
		});
	}
		
	@Override
	public long getCount() throws DatastoreException {
		return basicDao.getCount(DBOActivity.class);
	}

	@Override
	public QueryResults<MigratableObjectData> getMigrationObjectData(long offset, long limit, boolean includeDependencies) throws DatastoreException {
		// get one 'page' of Activities (just their IDs and Etags)
		List<MigratableObjectData> ods = null;
		{
			MapSqlParameterSource param = new MapSqlParameterSource();
			param.addValue(OFFSET_PARAM_NAME, offset);
			param.addValue(LIMIT_PARAM_NAME, limit);
			ods = simpleJdbcTemplate.query(SELECT_FOR_RANGE_SQL, new RowMapper<MigratableObjectData>() {
				@Override
				public MigratableObjectData mapRow(ResultSet rs, int rowNum) throws SQLException {
					String id = rs.getString(COL_ACTIVITY_ID);
					String etag = rs.getString(COL_ACTIVITY_ETAG);
					MigratableObjectData objectData = new MigratableObjectData();
					MigratableObjectDescriptor od = new MigratableObjectDescriptor();
					od.setId(id);
					od.setType(MigratableObjectType.ACTIVITY);
					objectData.setId(od);
					objectData.setEtag(etag);
					objectData.setDependencies(new HashSet<MigratableObjectDescriptor>(0));
					return objectData;
				}
			
			}, param);
		}
		
		// Activity has no dependencies
		
		// return the 'page' of objects, along with the total result count
		QueryResults<MigratableObjectData> queryResults = new QueryResults<MigratableObjectData>();
		queryResults.setResults(ods);
		queryResults.setTotalNumberOfResults((int)getCount());
		return queryResults;
	}

	@Override
	public MigratableObjectType getMigratableObjectType() {
		return MigratableObjectType.ACTIVITY;
	}
	
	/**
	 * Note: You cannot call this method outside of a transaction.
	 * @param id
	 * @param eTag
	 * @return
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 * @throws DatastoreException
	 */
	@Transactional(readOnly = false, propagation = Propagation.MANDATORY)
	@Override
	public String lockActivityAndIncrementEtag(String id, String eTag, ChangeType changeType)
			throws NotFoundException, ConflictingUpdateException, DatastoreException {
		// Create a Select for update query
		final Long longId = new Long(id);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("bindId", longId);
		// Check the eTags
		String currentTag = simpleJdbcTemplate.queryForObject(SQL_ETAG_FOR_UPDATE, String.class, longId);
		if(!currentTag.equals(eTag)){
			throw new ConflictingUpdateException("Node: "+id+" was updated since you last fetched it, retrieve it again and reapply the update");
		}
		// Get a new e-tag
		DBOActivity dbo = getDBO(id);
		tagMessenger.generateEtagAndSendMessage(dbo, changeType);
		currentTag = dbo.geteTag();
		// Update the etag
		int updated = simpleJdbcTemplate.update(UPDATE_ETAG_SQL, currentTag, longId);
		if(updated != 1) throw new ConflictingUpdateException("Failed to lock Node: "+longId);
		
		// Return the new tag
		return String.valueOf(currentTag);
	}

	@Override
	public boolean doesActivityExist(String id) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(COL_ACTIVITY_ID, id);
		try{
			long count = simpleJdbcTemplate.queryForLong(SQL_COUNT_ACTIVITY_ID, parameters);
			return count > 0;
		}catch(Exception e){
			// Can occur when the schema does not exist.
			return false;
		}
	}

	@Override
	public List<Reference> getEntitiesGeneratedBy(String id) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(COL_REVISION_ACTIVITY_ID, id);
		List<Reference> generatedBy = null;
		generatedBy = simpleJdbcTemplate.query(SQL_GET_NODES_FOR_ACTIVITY, new RowMapper<Reference>() {
			@Override
			public Reference mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				BigDecimal targetId = rs.getBigDecimal(COL_REVISION_OWNER_NODE);
				BigDecimal targetVersionNumber = rs.getBigDecimal(COL_REVISION_NUMBER);
				Reference ref = new Reference();
				ref.setTargetId(targetId.toPlainString());
				ref.setTargetVersionNumber(targetVersionNumber.longValue());
				return ref;
			}
		}, parameters);	
		return generatedBy;
	}


	
	/*
	 * Private Methods
	 */
	private DBOActivity getDBO(String id) throws NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACTIVITY_ID.toLowerCase(), id);
		DBOActivity dbo = basicDao.getObjectById(DBOActivity.class, param);
		return dbo;
	}
	
}
