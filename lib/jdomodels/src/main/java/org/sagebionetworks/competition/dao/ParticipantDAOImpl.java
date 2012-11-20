package org.sagebionetworks.competition.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.competition.dbo.DBOConstants;
import org.sagebionetworks.competition.dbo.ParticipantDBO;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.competition.query.jdo.SQLConstants;
import org.sagebionetworks.competition.util.Utility;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class ParticipantDAOImpl implements ParticipantDAO {
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	private static final String USER_ID = DBOConstants.PARAM_PARTICIPANT_USER_ID;
	private static final String COMP_ID = DBOConstants.PARAM_PARTICIPANT_COMP_ID;
	
	private static final String SELECT_ALL_SQL_PAGINATED = 
			"SELECT * FROM "+ SQLConstants.TABLE_PARTICIPANT +
			" LIMIT :"+ SQLConstants.LIMIT_PARAM_NAME +
			" OFFSET :" + SQLConstants.OFFSET_PARAM_NAME;
	
	private static final String SELECT_BY_COMPETITION_SQL = 
			"SELECT * FROM "+ SQLConstants.TABLE_PARTICIPANT +
			" WHERE "+ SQLConstants.COL_PARTICIPANT_COMP_ID + "=:"+ COMP_ID;
	
	private static final String COUNT_BY_COMPETITION_SQL = 
			"SELECT COUNT * FROM " +  SQLConstants.TABLE_PARTICIPANT +
			" WHERE "+ SQLConstants.COL_PARTICIPANT_COMP_ID + "=:" + COMP_ID;
	
	private static final RowMapper<ParticipantDBO> rowMapper = ((new ParticipantDBO()).getTableMapping());

	@Override
	public void create(Participant dto) throws DatastoreException {		
		// Convert to DBO
		ParticipantDBO dbo = new ParticipantDBO();
		copyDtoToDbo(dto, dbo);
			
		// Set creation date
		dbo.setCreatedOn(System.currentTimeMillis());
		
		// Ensure DBO has required information
		verifyParticipantDBO(dbo);
		
		// Create DBO
		try {
			dbo = basicDao.createNew(dbo);
		} catch (Exception e) {
			throw new DatastoreException("id="+dbo.getUserId()+" name="+dto.getName(), e);
		}
	}

	@Override
	public Participant get(String userId, String compId) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(USER_ID, userId);
		param.addValue(COMP_ID, compId);
		ParticipantDBO dbo = basicDao.getObjectById(ParticipantDBO.class, param);
		Participant dto = new Participant();
		copyDboToDto(dbo, dto);
		return dto;
	}

	@Override
	public List<Participant> getInRange(long startIncl, long endExcl) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(SQLConstants.OFFSET_PARAM_NAME, startIncl);
		long limit = endExcl - startIncl;
		if (limit <= 0)
			throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(SQLConstants.LIMIT_PARAM_NAME, limit);	
		List<ParticipantDBO> dbos = simpleJdbcTemplate.query(SELECT_ALL_SQL_PAGINATED, rowMapper, param);
		List<Participant> dtos = new ArrayList<Participant>();
		for (ParticipantDBO dbo : dbos) {
			Participant dto = new Participant();
			copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}
	
	@Override
	public List<Participant> getAllByCompetition(String compId) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COMP_ID, compId);		
		List<ParticipantDBO> dbos = simpleJdbcTemplate.query(SELECT_BY_COMPETITION_SQL, rowMapper, param);
		List<Participant> dtos = new ArrayList<Participant>();
		for (ParticipantDBO dbo : dbos) {
			Participant dto = new Participant();
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
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(String userId, String compId) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(USER_ID, userId);
		param.addValue(COMP_ID, compId);
		basicDao.deleteObjectById(ParticipantDBO.class, param);		
	}

	/**
	 * Copy a ParticipantDBO database object to a Participant data transfer object
	 * 
	 * @param dto
	 * @param dbo
	 */
	private static void copyDtoToDbo(Participant dto, ParticipantDBO dbo) {		
		dbo.setCompId(dto.getCompetitionId() == null ? null : Long.parseLong(dto.getCompetitionId()));
		dbo.setUserId(dto.getCompetitionId() == null ? null : Long.parseLong(dto.getUserId()));
		dbo.setCreatedOn(dto.getCreatedOn() == null ? null : dto.getCreatedOn().getTime());
	}
	
	/**
	 * Copy a Participant data transfer object to a ParticipantDBO database object
	 * 
	 * @param dbo
	 * @param dto
	 * @throws DatastoreException
	 */
	private static void copyDboToDto(ParticipantDBO dbo, Participant dto) throws DatastoreException {		
		dto.setCompetitionId(dbo.getCompId() == null ? null : dbo.getCompId().toString());
		dto.setUserId(dbo.getUserId() == null ? null : dbo.getUserId().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
	}

	/**
	 * Ensure that a ParticipantDBO object has all required components
	 * 
	 * @param dbo
	 */
	private void verifyParticipantDBO(ParticipantDBO dbo) {
		Utility.ensureNotNull(dbo.getCompId(), dbo.getUserId(), dbo.getCreatedOn());
	}
	
}
