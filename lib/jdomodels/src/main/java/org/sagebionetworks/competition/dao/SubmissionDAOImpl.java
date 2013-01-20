package org.sagebionetworks.competition.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.competition.dbo.DBOConstants;
import org.sagebionetworks.competition.dbo.SubmissionDBO;
import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.competition.model.SubmissionStatusEnum;
import org.sagebionetworks.competition.query.jdo.SQLConstants;
import org.sagebionetworks.competition.util.CompetitionUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class SubmissionDAOImpl implements SubmissionDAO {
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	@Autowired
	private IdGenerator idGenerator;
	
	private static final String ID = DBOConstants.PARAM_SUBMISSION_ID;
	private static final String USER_ID = DBOConstants.PARAM_SUBMISSION_USER_ID;
	private static final String COMP_ID = DBOConstants.PARAM_SUBMISSION_COMP_ID;
	private static final String STATUS = DBOConstants.PARAM_SUBSTATUS_STATUS;
	
	private static final String SELECT_BY_USER_SQL = 
			"SELECT * FROM "+ SQLConstants.TABLE_SUBMISSION +
			" WHERE "+ SQLConstants.COL_SUBMISSION_USER_ID + "=:"+ USER_ID;
	
	private static final String SELECT_BY_COMPETITION_SQL = 
			"SELECT * FROM "+ SQLConstants.TABLE_SUBMISSION +
			" WHERE "+ SQLConstants.COL_SUBMISSION_COMP_ID + "=:"+ COMP_ID;
	
	private static final String SELECT_BY_COMPETITION_AND_STATUS_SQL = 
			"SELECT * FROM "+ SQLConstants.TABLE_SUBMISSION + " n " +
			"INNER JOIN " + SQLConstants.TABLE_SUBSTATUS + " r " +
			"ON n." + SQLConstants.COL_SUBMISSION_ID + " = r." + SQLConstants.COL_SUBSTATUS_SUBMISSION_ID +
			" WHERE n."+ SQLConstants.COL_SUBMISSION_COMP_ID + "=:"+ COMP_ID +
			" AND r." + SQLConstants.COL_SUBSTATUS_STATUS + "=:" + STATUS;
	
	private static final String COUNT_BY_COMPETITION_SQL = 
			"SELECT COUNT(*) FROM " +  SQLConstants.TABLE_SUBMISSION +
			" WHERE "+ SQLConstants.COL_PARTICIPANT_COMP_ID + "=:" + COMP_ID;
	
	private static final RowMapper<SubmissionDBO> rowMapper = ((new SubmissionDBO()).getTableMapping());

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String create(Submission dto) throws DatastoreException {		
		// Convert to DBO
		SubmissionDBO dbo = new SubmissionDBO();
		copyDtoToDbo(dto, dbo);
		
		// Generate ID
		dbo.setId(idGenerator.generateNewId());
			
		// Set creation date
		dbo.setCreatedOn(System.currentTimeMillis());
		
		// Ensure DBO has required information
		verifySubmissionDBO(dbo);
		
		// Create DBO
		try {
			dbo = basicDao.createNew(dbo);
			return dbo.getId().toString();
		} catch (Exception e) {
			throw new DatastoreException("id=" + dbo.getId() + " userId=" + 
						dto.getUserId() + " entityId=" + dto.getEntityId(), e);
		}
	}

	@Override
	public Submission get(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, id);
		SubmissionDBO dbo = basicDao.getObjectById(SubmissionDBO.class, param);
		Submission dto = new Submission();
		copyDboToDto(dbo, dto);
		return dto;
	}
	
	@Override
	public List<Submission> getAllByUser(String userId) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(USER_ID, userId);		
		List<SubmissionDBO> dbos = simpleJdbcTemplate.query(SELECT_BY_USER_SQL, rowMapper, param);
		List<Submission> dtos = new ArrayList<Submission>();
		for (SubmissionDBO dbo : dbos) {
			Submission dto = new Submission();
			copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}
	
	@Override
	public List<Submission> getAllByCompetition(String compId) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COMP_ID, compId);		
		List<SubmissionDBO> dbos = simpleJdbcTemplate.query(SELECT_BY_COMPETITION_SQL, rowMapper, param);
		List<Submission> dtos = new ArrayList<Submission>();
		for (SubmissionDBO dbo : dbos) {
			Submission dto = new Submission();
			copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}
	
	@Override
	public List<Submission> getAllByCompetitionAndStatus(String compId, SubmissionStatusEnum status) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COMP_ID, compId);
		param.addValue(STATUS, status.ordinal());
		List<SubmissionDBO> dbos = simpleJdbcTemplate.query(SELECT_BY_COMPETITION_AND_STATUS_SQL, rowMapper, param);
		List<Submission> dtos = new ArrayList<Submission>();
		for (SubmissionDBO dbo : dbos) {
			Submission dto = new Submission();
			copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}
	
	@Override
	public long getCountByCompetition(String compId) throws DatastoreException, NotFoundException {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(COMP_ID, compId);
		return simpleJdbcTemplate.queryForLong(COUNT_BY_COMPETITION_SQL, parameters);
	}
	
	@Override
	public long getCount() throws DatastoreException, NotFoundException {
		return basicDao.getCount(SubmissionDBO.class);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, id);
		basicDao.deleteObjectById(SubmissionDBO.class, param);		
	}

	/**
	 * Copy a SubmissionDBO database object to a Participant data transfer object
	 * 
	 * @param dto
	 * @param dbo
	 */
	protected static void copyDtoToDbo(Submission dto, SubmissionDBO dbo) {	
		dbo.setId(dto.getId() == null ? null : Long.parseLong(dto.getId()));
		dbo.setUserId(dto.getCompetitionId() == null ? null : Long.parseLong(dto.getUserId()));
		dbo.setCompId(dto.getCompetitionId() == null ? null : Long.parseLong(dto.getCompetitionId()));
		dbo.setEntityId(dto.getEntityId() == null ? null : KeyFactory.stringToKey(dto.getEntityId()));
		dbo.setVersionNumber(dto.getVersionNumber());
		dbo.setName(dto.getName());
		dbo.setCreatedOn(dto.getCreatedOn() == null ? null : dto.getCreatedOn().getTime());
	}
	
	/**
	 * Copy a Submission data transfer object to a SubmissionDBO database object
	 * 
	 * @param dbo
	 * @param dto
	 * @throws DatastoreException
	 */
	protected static void copyDboToDto(SubmissionDBO dbo, Submission dto) throws DatastoreException {
		dto.setId(dbo.getId() == null ? null : dbo.getId().toString());
		dto.setUserId(dbo.getUserId() == null ? null : dbo.getUserId().toString());
		dto.setCompetitionId(dbo.getCompId() == null ? null : dbo.getCompId().toString());
		dto.setEntityId(dbo.getEntityId() == null ? null : KeyFactory.keyToString(dbo.getEntityId()));
		dto.setVersionNumber(dbo.getVersionNumber());
		dto.setName(dbo.getName());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
	}

	/**
	 * Ensure that a SubmissionDBO object has all required components
	 * 
	 * @param dbo
	 */
	private void verifySubmissionDBO(SubmissionDBO dbo) {
		CompetitionUtils.ensureNotNull(dbo.getCompId(), "Competition ID");
		CompetitionUtils.ensureNotNull(dbo.getUserId(), "User ID");
		CompetitionUtils.ensureNotNull(dbo.getEntityId(), "Entity ID");
		CompetitionUtils.ensureNotNull(dbo.getId(), "Submission ID");
		CompetitionUtils.ensureNotNull(dbo.getCreatedOn(), "Creation date");
	}
	
}
