package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACTIVITY_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACTIVITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURRENT_REV;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ACTIVITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_OWNER_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACTIVITY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REVISION;

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
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOActivity;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
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
	
	private static final String SELECT_FOR_RANGE_SQL = "SELECT " + COL_ACTIVITY_ID +", "+ COL_ACTIVITY_ETAG 
													+ " FROM " + TABLE_ACTIVITY 
													+ " ORDER BY " + COL_ACTIVITY_ID 
													+ " LIMIT :" + LIMIT_PARAM_NAME 
													+ " OFFSET :" + OFFSET_PARAM_NAME;

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
	public String create(Activity dto) throws DatastoreException, InvalidModelException {
		return createPrivate(dto, false);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String createFromBackup(Activity dto) throws DatastoreException, InvalidModelException {		
		if(dto == null) throw new IllegalArgumentException("Activity cannot be null");
		if(dto.getEtag() == null) throw new IllegalArgumentException("The backup Activity must have an etag");
		if(dto.getId() == null) throw new IllegalArgumentException("The backup Activity must have an id");
		// The ID must not change
		Long startingId = KeyFactory.stringToKey(dto.getId());
		// Create the node.
		// We want to force the use of the current eTag. See PLFM-845
		boolean forceUseEtag = true;
		String id = createPrivate(dto, true);
		// validate that the ID is unchanged.
		if(!startingId.equals(KeyFactory.stringToKey(id))) throw new DatastoreException("Creating an activity from a backup changed the ID.");
		return id;
	}
	
	private String createPrivate(Activity dto, boolean forceEtag) throws DatastoreException, InvalidModelException {
		DBOActivity dbo = new DBOActivity();
		ActivityUtils.copyDtoToDbo(dto, dbo);

		if(forceEtag){
			if(dto.getEtag() == null) throw new IllegalArgumentException("Cannot force the use of an ETag when the ETag is null");
			dbo.seteTag(KeyFactory.urlDecode(dto.getEtag()));
			// Send a message without changing the etag;
			tagMessenger.sendMessage(dbo, ChangeType.CREATE);
		}else{
			// add eTag
			tagMessenger.generateEtagAndSendMessage(dbo, ChangeType.CREATE);
		}

		basicDao.createNew(dbo);				
		return dbo.getIdString();		
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Activity update(Activity dto) throws DatastoreException,
			InvalidModelException,NotFoundException, ConflictingUpdateException {
		return update(dto, false);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Activity updateFromBackup(Activity dto)
			throws InvalidModelException, NotFoundException,
			ConflictingUpdateException, DatastoreException {
		return update(dto, true);
	}

	/**
	 * @param fromBackup Whether we are updating from backup.
	 *                   Skip optimistic locking and accept the backup e-tag when restoring from backup.
	 */
	private Activity update(Activity dto, boolean fromBackup) throws DatastoreException,
			InvalidModelException,NotFoundException, ConflictingUpdateException {				
		DBOActivity dbo = getDBO(dto.getId());
		ActivityUtils.copyDtoToDbo(dto, dbo);
		
		if(fromBackup) {			
			lockAndSendTagMessage(dbo, ChangeType.UPDATE); // keep same eTag but send message of update
		}
		
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
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) throws DatastoreException {				
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACTIVITY_ID.toLowerCase(), id);
		basicDao.deleteObjectById(DBOActivity.class, param);
	}
			
	@Override
	public long getCount() throws DatastoreException {
		return basicDao.getCount(DBOActivity.class);
	}

	@Override
	public QueryResults<MigratableObjectData> getMigrationObjectData(long offset, long limit, boolean includeDependencies) throws DatastoreException {
		if(limit < 0 || offset < 0) throw new IllegalArgumentException("limit and offset must be greater than 0");
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
	
	@Transactional(readOnly = false, propagation = Propagation.MANDATORY)
	@Override
	public String lockActivityAndGenerateEtag(String id, String eTag, ChangeType changeType)
			throws NotFoundException, ConflictingUpdateException, DatastoreException {
		String currentTag = lockActivity(id);
		// Check the eTags
		if(!currentTag.equals(eTag)){
			throw new ConflictingUpdateException("Node: "+id+" was updated since you last fetched it, retrieve it again and reapply the update");
		}
		// Get a new e-tag
		DBOActivity dbo = getDBO(id);
		tagMessenger.generateEtagAndSendMessage(dbo, changeType);
		return dbo.geteTag();
	}

	@Transactional(readOnly = false, propagation = Propagation.MANDATORY)
	@Override
	public void sendDeleteMessage(String id) {
		tagMessenger.sendDeleteMessage(id, ObjectType.ACTIVITY);		
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
	public PaginatedResults<Reference> getEntitiesGeneratedBy(String activityId, int limit, int offset) {
		if(activityId == null) throw new IllegalArgumentException("Activity id can not be null");
		if(limit < 0 || offset < 0) throw new IllegalArgumentException("limit and offset must be greater than 0");
		// get one page of node ids
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_REVISION_ACTIVITY_ID, activityId);
		params.addValue(OFFSET_PARAM_NAME, offset);
		params.addValue(LIMIT_PARAM_NAME, limit);

		List<Reference> generatedBy = null;
		generatedBy = simpleJdbcTemplate.query(SQL_GET_NODES_FOR_ACTIVITY, new RowMapper<Reference>() {
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
		}, params);
			
		// return the page of objects, along with the total result count
		PaginatedResults<Reference> queryResults = new PaginatedResults<Reference>();
		queryResults.setResults(generatedBy);
		long totalCount = 0;
		try {
			totalCount = simpleJdbcTemplate.queryForLong(SQL_GET_NODES_FOR_ACTIVITY_COUNT, params);		
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
			DBOActivity dbo = basicDao.getObjectById(DBOActivity.class, param);
			return dbo;
		} catch (NotFoundException e) {
			throw new NotFoundException(String.format(ACTIVITY_NOT_FOUND, id));
		}
	}

	private String lockActivity(String id) {
		// Create a Select for update query
		return simpleJdbcTemplate.queryForObject(SQL_ETAG_FOR_UPDATE, String.class, id);
	}
	
	private void lockAndSendTagMessage(DBOActivity dbo, ChangeType changeType) {
		lockActivity(dbo.getIdString());
		tagMessenger.sendMessage(dbo, changeType);		
	}


}
