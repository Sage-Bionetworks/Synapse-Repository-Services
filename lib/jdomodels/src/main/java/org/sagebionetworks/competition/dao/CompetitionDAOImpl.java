package org.sagebionetworks.competition.dao;

import static org.sagebionetworks.competition.query.jdo.SQLConstants.*;

import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.sagebionetworks.competition.dbo.CompetitionDBO;
import org.sagebionetworks.competition.dbo.DBOConstants;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.competition.util.CompetitionUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class CompetitionDAOImpl implements CompetitionDAO {
	
	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private TagMessenger tagMessenger;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	private static final String ID = DBOConstants.PARAM_COMPETITION_ID;
	private static final String NAME = DBOConstants.PARAM_COMPETITION_NAME;
	private static final String STATUS = DBOConstants.PARAM_COMPETITION_STATUS;
	
	private static final String SELECT_BY_NAME_SQL = 
			"SELECT ID FROM "+ TABLE_COMPETITION +
			" WHERE "+ COL_COMPETITION_NAME + "=:" + NAME;
	
	private static final String SELECT_ALL_SQL_PAGINATED = 
			"SELECT * FROM "+ TABLE_COMPETITION +
			" LIMIT :"+ LIMIT_PARAM_NAME +
			" OFFSET :" + OFFSET_PARAM_NAME;
	
	private static final String SELECT_BY_STATUS_SQL_PAGINATED = 
			"SELECT * FROM "+ TABLE_COMPETITION +
			" WHERE " + COL_COMPETITION_STATUS + "=:" + STATUS +
			" LIMIT :"+ LIMIT_PARAM_NAME +
			" OFFSET :" + OFFSET_PARAM_NAME;
	
	private static final String SELECT_ID_ETAG_SQL_PAGINATED = 
			"SELECT " + COL_COMPETITION_ID + ", " + COL_COMPETITION_ETAG +
			" FROM "+ TABLE_COMPETITION +
			" ORDER BY " + COL_COMPETITION_ID +
			" LIMIT :"+ LIMIT_PARAM_NAME +
			" OFFSET :" + OFFSET_PARAM_NAME;
	
	private static final String SQL_ETAG_WITHOUT_LOCK = "SELECT " + COL_COMPETITION_ETAG + " FROM " +
														TABLE_COMPETITION +" WHERE ID = ?";
	
	private static final String SQL_ETAG_FOR_UPDATE = SQL_ETAG_WITHOUT_LOCK + " FOR UPDATE";
	
	private static final RowMapper<CompetitionDBO> rowMapper = ((new CompetitionDBO()).getTableMapping());

	private static final String COMPETITION_NOT_FOUND = "Competition could not be found with id :";

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String create(Competition dto, String ownerId) throws DatastoreException {
		CompetitionUtils.ensureNotNull(dto, "Competition object");
		CompetitionUtils.ensureNotNull(ownerId, "Owner ID");
		
		// convert to DBO
		CompetitionDBO dbo = new CompetitionDBO();
		copyDtoToDbo(dto, dbo);
		
		// generate unique ID
		dbo.setId(idGenerator.generateNewId());
		
		// set Owner ID
		dbo.setOwnerId(Long.parseLong(ownerId));
		
		// set creation date
		dbo.setCreatedOn(System.currentTimeMillis());
		
		// generate eTag
		tagMessenger.generateEtagAndSendMessage(dbo, ChangeType.CREATE);
		
		// ensure DBO has required information
		verifyCompetitionDBO(dbo);
		
		// create DBO
		try {
			dbo = basicDao.createNew(dbo);
			return dbo.getId().toString();
		} catch (Exception e) {
			String message = "id=" + dbo.getId() + " name=" + dto.getName();
			
			// check if a name conflict occurred
			if (e.getClass() == IllegalArgumentException.class) {
				IllegalArgumentException e2 = (IllegalArgumentException) e;
				if (e2.getCause().getClass() == DuplicateKeyException.class)
					message = "A Competition already exists with the name '" +
							dto.getName() + "'";
			}
			
			throw new DatastoreException(message, e);
		}
	}

	@Override
	public Competition get(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, id);
		CompetitionDBO dbo = basicDao.getObjectById(CompetitionDBO.class, param);
		Competition dto = new Competition();
		copyDboToDto(dbo, dto);
		return dto;
	}

	@Override
	public List<Competition> getInRange(long limit, long offset) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(OFFSET_PARAM_NAME, offset);
		param.addValue(LIMIT_PARAM_NAME, limit);	
		List<CompetitionDBO> dbos = simpleJdbcTemplate.query(SELECT_ALL_SQL_PAGINATED, rowMapper, param);
		List<Competition> dtos = new ArrayList<Competition>();
		for (CompetitionDBO dbo : dbos) {
			Competition dto = new Competition();
			copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}

	@Override
	public List<Competition> getInRange(long limit, long offset,
			CompetitionStatus status) throws DatastoreException,
			NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(OFFSET_PARAM_NAME, offset);
		param.addValue(LIMIT_PARAM_NAME, limit);	
		param.addValue(COL_COMPETITION_STATUS, status.ordinal());
		List<CompetitionDBO> dbos = simpleJdbcTemplate.query(SELECT_BY_STATUS_SQL_PAGINATED, rowMapper, param);
		List<Competition> dtos = new ArrayList<Competition>();
		for (CompetitionDBO dbo : dbos) {
			Competition dto = new Competition();
			copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}

	@Override
	public long getCount() throws DatastoreException {
		return basicDao.getCount(CompetitionDBO.class);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void update(Competition dto)
			throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException {
		update(dto, false);		
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void updateFromBackup(Competition dto)
			throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException {
		update(dto, true);
	}

	private void update(Competition dto, boolean fromBackup) throws ConflictingUpdateException, DatastoreException, NotFoundException {
		CompetitionDBO dbo = new CompetitionDBO();
		copyDtoToDbo(dto, dbo);
		verifyCompetitionDBO(dbo);
		
		if (fromBackup) {
			// keep same eTag
			lockAndSendTagMessage(dbo, ChangeType.UPDATE); 
		} else {
			// update eTag
			String newEtag = lockAndGenerateEtag(dbo.getIdString(), dbo.geteTag(), ChangeType.UPDATE);	
			dbo.seteTag(newEtag);
		}
		
		// TODO: detect and log NO-OP update
		basicDao.update(dbo);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, id);
		basicDao.deleteObjectById(CompetitionDBO.class, param);		
	}

	@Override
	public String lookupByName(String name) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(NAME, name);
		try {
			Long id = simpleJdbcTemplate.queryForLong(SELECT_BY_NAME_SQL, param);		
			return id.toString();
		} catch (EmptyResultDataAccessException e) {
			// name is not in use
			return null;
		}
	}

	/**
	 * Copy a CompetitionDBO database object to a Competition data transfer object
	 * 
	 * @param dto
	 * @param dbo
	 */
	private static void copyDtoToDbo(Competition dto, CompetitionDBO dbo) {		
		dbo.setId(dto.getId() == null ? null : Long.parseLong(dto.getId()));
		dbo.seteTag(dto.getEtag());
		dbo.setName(dto.getName());
		dbo.setDescription(dto.getDescription() == null ? null : dto.getDescription().getBytes());
		dbo.setOwnerId(dto.getOwnerId() == null ? null : Long.parseLong(dto.getOwnerId()));
		dbo.setCreatedOn(dto.getCreatedOn() == null ? null : dto.getCreatedOn().getTime());
		dbo.setContentSource(dto.getContentSource());
		dbo.setStatusEnum(dto.getStatus());
	}
	
	/**
	 * Copy a Competition data transfer object to a CompetitionDBO database object
	 * 
	 * @param dbo
	 * @param dto
	 * @throws DatastoreException
	 */
	private static void copyDboToDto(CompetitionDBO dbo, Competition dto) throws DatastoreException {		
		dto.setId(dbo.getId() == null ? null : dbo.getId().toString());
		dto.setEtag(dbo.geteTag());
		dto.setName(dbo.getName());
		if (dbo.getDescription() != null) {
			try {
				dto.setDescription(new String(dbo.getDescription(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new DatastoreException(e);
			}
		} else {
			dto.setDescription(null);
		}
		dto.setOwnerId(dbo.getOwnerId().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setContentSource(dbo.getContentSource());
		dto.setStatus(dbo.getStatusEnum());
	}

	/**
	 * Ensure that a CompetitionDBO object has all required components
	 * 
	 * @param dbo
	 */
	private void verifyCompetitionDBO(CompetitionDBO dbo) {
		CompetitionUtils.ensureNotNull(dbo.getId(), "ID");
		CompetitionUtils.ensureNotNull(dbo.geteTag(), "eTag");
		CompetitionUtils.ensureNotNull(dbo.getName(), "name");
		CompetitionUtils.ensureNotNull(dbo.getOwnerId(), "ownerID");
		CompetitionUtils.ensureNotNull(dbo.getCreatedOn(), "creation date");
		CompetitionUtils.ensureNotNull(dbo.getContentSource(), "content source");
		CompetitionUtils.ensureNotNull(dbo.getStatusEnum(), "status");
	}
	
	private String lockAndGenerateEtag(String id, String eTag, ChangeType changeType)
			throws NotFoundException, ConflictingUpdateException, DatastoreException {
		String currentTag = lockForUpdate(id);
		// Check the eTags
		if(!currentTag.equals(eTag)){
			throw new ConflictingUpdateException("Competition: "+id+" was updated since you last fetched it, retrieve it again and reapply the update");
		}
		// Get a new e-tag
		CompetitionDBO dbo = getDBO(id);
		tagMessenger.generateEtagAndSendMessage(dbo, changeType);
		return dbo.geteTag();
	}
	
	private CompetitionDBO getDBO(String id) throws NotFoundException {
		CompetitionUtils.ensureNotNull(id, "Competition id");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOConstants.PARAM_COMPETITION_ID, id);
		try {
			CompetitionDBO dbo = basicDao.getObjectById(CompetitionDBO.class, param);
			return dbo;
		} catch (NotFoundException e) {
			throw new NotFoundException(COMPETITION_NOT_FOUND + id);
		}
	}
	
	private String lockForUpdate(String id) {
		// Create a Select for update query
		return simpleJdbcTemplate.queryForObject(SQL_ETAG_FOR_UPDATE, String.class, id);
	}
	
	private void lockAndSendTagMessage(CompetitionDBO dbo, ChangeType changeType) {
		lockForUpdate(dbo.getIdString());
		tagMessenger.sendMessage(dbo, changeType);		
	}

	@Override
	public QueryResults<MigratableObjectData> getMigrationObjectData (
			long offset, long limit, boolean includeDependencies)
			throws DatastoreException {
		// get one 'page' of Competitions (just IDs and eTags)
		List<MigratableObjectData> ods = null;
		{
			MapSqlParameterSource param = new MapSqlParameterSource();
			param.addValue(OFFSET_PARAM_NAME, offset);
			param.addValue(LIMIT_PARAM_NAME, limit);
			ods = simpleJdbcTemplate.query(SELECT_ID_ETAG_SQL_PAGINATED, new RowMapper<MigratableObjectData>() {

				@Override
				public MigratableObjectData mapRow(ResultSet rs, int rowNum)
						throws SQLException {
					String id = rs.getString(COL_COMPETITION_ID);
					String etag = rs.getString(COL_COMPETITION_ETAG);
					MigratableObjectData objectData = new MigratableObjectData();
					MigratableObjectDescriptor od = new MigratableObjectDescriptor();
					od.setId(id);
					od.setType(MigratableObjectType.COMPETITION);
					objectData.setId(od);
					objectData.setEtag(etag);
					
					// note that the principal specified by ownerId is a required dependency
					// this dependency does not need to be specified per PLFM-1537
					
					return objectData;
				}
			
			}, param);
		}
		
		// return the 'page' of objects, along with the total result count
		QueryResults<MigratableObjectData> queryResults = new QueryResults<MigratableObjectData>();
		queryResults.setResults(ods);
		queryResults.setTotalNumberOfResults((int) getCount());
		return queryResults;
	}

	@Override
	public MigratableObjectType getMigratableObjectType() {
		return MigratableObjectType.COMPETITION;
	}
	
}
