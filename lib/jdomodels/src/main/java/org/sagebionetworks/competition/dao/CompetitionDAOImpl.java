package org.sagebionetworks.competition.dao;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.competition.dbo.CompetitionDBO;
import org.sagebionetworks.competition.dbo.DBOConstants;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.competition.query.jdo.SQLConstants;
import org.sagebionetworks.competition.util.CompetitionUtils;
import org.sagebionetworks.ids.ETagGenerator;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
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
	private ETagGenerator eTagGenerator;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	private static final String ID = DBOConstants.PARAM_COMPETITION_ID;
	private static final String NAME = DBOConstants.PARAM_COMPETITION_NAME;
	
	private static final String SELECT_BY_NAME_SQL = 
			"SELECT ID FROM "+ SQLConstants.TABLE_COMPETITION +
			" WHERE "+ SQLConstants.COL_COMPETITION_NAME + "=:"+NAME;
	
	private static final String SELECT_ALL_SQL_PAGINATED = 
			"SELECT * FROM "+ SQLConstants.TABLE_COMPETITION +
			" LIMIT :"+ SQLConstants.LIMIT_PARAM_NAME +
			" OFFSET :" + SQLConstants.OFFSET_PARAM_NAME;
	
	private static final String COUNT_COMPETITIONS_BY_ID_SQL = 
			"SELECT COUNT(" + ID + ") FROM " + 
			SQLConstants.TABLE_COMPETITION + " WHERE "+ 
			ID + "=:" + ID;
	
	private static final RowMapper<CompetitionDBO> rowMapper = ((new CompetitionDBO()).getTableMapping());

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String create(Competition dto, String ownerId) throws DatastoreException {
		CompetitionUtils.ensureNotNull(dto, "Competition object");
		CompetitionUtils.ensureNotNull(ownerId, "Owner ID");
		
		// Convert to DBO
		CompetitionDBO dbo = new CompetitionDBO();
		copyDtoToDbo(dto, dbo);
		
		// Ensure name is not taken
		if (lookupByName(dto.getName()) != null)
			throw new IllegalArgumentException("Sorry, a Competition already exists with the name " + dto.getName());
		
		// Generate ID
		dbo.setId(idGenerator.generateNewId());
		
		// TODO: Generate eTag
		dbo.seteTag(eTagGenerator.generateETag(dbo));
		
		// set Owner ID
		dbo.setOwnerId(Long.parseLong(ownerId));
		
		// Set creation date
		dbo.setCreatedOn(System.currentTimeMillis());
		
		// Ensure DBO has required information
		verifyCompetitionDBO(dbo);
		
		// Create DBO
		try {
			dbo = basicDao.createNew(dbo);
			return dbo.getId().toString();
		} catch (Exception e) {
			throw new DatastoreException("id="+dbo.getId()+" name="+dto.getName(), e);
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
	//TODO: filter by Competition status
	public List<Competition> getInRange(long startIncl, long endExcl) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(SQLConstants.OFFSET_PARAM_NAME, startIncl);
		long limit = endExcl - startIncl;
		if (limit <= 0)
			throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(SQLConstants.LIMIT_PARAM_NAME, limit);	
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
	public List<Competition> getInRange(long startIncl, long endExcl,
			CompetitionStatus status) throws DatastoreException,
			NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getCount() throws DatastoreException, NotFoundException {
		return basicDao.getCount(CompetitionDBO.class);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void update(Competition dto)
			throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException {		
		CompetitionDBO dbo = new CompetitionDBO();
		copyDtoToDbo(dto, dbo);
		verifyCompetitionDBO(dbo);
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

	@Override
	public boolean doesIdExist(String id) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(ID, id);
		try {
			long count = simpleJdbcTemplate.queryForLong(COUNT_COMPETITIONS_BY_ID_SQL, parameters);
			return count > 0;
		} catch(Exception e) {
			// Can occur when the schema does not exist.
			return false;
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
	
}
