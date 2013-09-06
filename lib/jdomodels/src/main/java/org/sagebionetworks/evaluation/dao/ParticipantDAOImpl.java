package org.sagebionetworks.evaluation.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.evaluation.dbo.ParticipantDBO;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.util.EvaluationUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.query.SQLConstants;
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
	@Autowired
	private IdGenerator idGenerator;
	
	private static final String USER_ID = DBOConstants.PARAM_PARTICIPANT_USER_ID;
	private static final String EVAL_ID = DBOConstants.PARAM_PARTICIPANT_EVAL_ID;
	
	private static final String SELECT_ALL_SQL_PAGINATED = 
			"SELECT * FROM "+ SQLConstants.TABLE_PARTICIPANT +
			" LIMIT :"+ SQLConstants.LIMIT_PARAM_NAME +
			" OFFSET :" + SQLConstants.OFFSET_PARAM_NAME;
	
	private static final String SELECT_BY_EVALUATION_SQL = 
			"SELECT * FROM "+ SQLConstants.TABLE_PARTICIPANT +
			" WHERE "+ SQLConstants.COL_PARTICIPANT_EVAL_ID + "=:"+ EVAL_ID +
			" LIMIT :"+ SQLConstants.LIMIT_PARAM_NAME +
			" OFFSET :" + SQLConstants.OFFSET_PARAM_NAME;
	
	private static final String COUNT_BY_EVALUATION_SQL = 
			"SELECT COUNT(*) FROM " +  SQLConstants.TABLE_PARTICIPANT +
			" WHERE " + SQLConstants.COL_PARTICIPANT_EVAL_ID + "=:" + EVAL_ID;
	
	private static final RowMapper<ParticipantDBO> rowMapper = ((new ParticipantDBO()).getTableMapping());
	

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public long create(Participant dto) throws DatastoreException {		
		// Convert to DBO
		ParticipantDBO dbo = new ParticipantDBO();
		copyDtoToDbo(dto, dbo);
		
		// Ensure DBO has required information
		verifyParticipantDBO(dbo);
		dbo.setId(idGenerator.generateNewId(TYPE.PARTICIPANT_ID));
		
		// Create DBO
		try {
			dbo = basicDao.createNew(dbo);
		} catch (Exception e) {
			throw new DatastoreException(e.getMessage() + " [userId="+dbo.getUserId()+" evaluationId="+dto.getEvaluationId() + "]", e);
		}
		
		return dbo.getId();
	}

	@Override
	public Participant get(String userId, String evalId) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(USER_ID, userId);
		param.addValue(EVAL_ID, evalId);
		ParticipantDBO dbo = basicDao.getObjectByPrimaryKey(ParticipantDBO.class, param);
		Participant dto = new Participant();
		copyDboToDto(dbo, dto);
		return dto;
	}

	@Override
	public List<Participant> getInRange(long limit, long offset) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(SQLConstants.OFFSET_PARAM_NAME, offset);
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
	public List<Participant> getAllByEvaluation(String evalId, long limit, long offset) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(SQLConstants.OFFSET_PARAM_NAME, offset);
		param.addValue(SQLConstants.LIMIT_PARAM_NAME, limit);	
		param.addValue(EVAL_ID, evalId);		
		List<ParticipantDBO> dbos = simpleJdbcTemplate.query(SELECT_BY_EVALUATION_SQL, rowMapper, param);
		List<Participant> dtos = new ArrayList<Participant>();
		for (ParticipantDBO dbo : dbos) {
			Participant dto = new Participant();
			copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}
	
	@Override
	public long getCountByEvaluation(String evalId) throws DatastoreException, NotFoundException {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(EVAL_ID, evalId);
		return simpleJdbcTemplate.queryForLong(COUNT_BY_EVALUATION_SQL, parameters);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(String userId, String evalId) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(USER_ID, userId);
		param.addValue(EVAL_ID, evalId);
		basicDao.deleteObjectByPrimaryKey(ParticipantDBO.class, param);		
	}

	/**
	 * Copy a ParticipantDBO database object to a Participant data transfer object
	 * 
	 * @param dto
	 * @param dbo
	 */
	protected static void copyDtoToDbo(Participant dto, ParticipantDBO dbo) {
		try {
			dbo.setEvalId(dto.getEvaluationId() == null ? null : Long.parseLong(dto.getEvaluationId()));
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Invalid Evaluation ID: " + dto.getEvaluationId());
		}
		try {
			dbo.setUserId(dto.getUserId() == null ? null : Long.parseLong(dto.getUserId()));
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Invalid User ID: " + dto.getUserId());
		}
		dbo.setCreatedOn(dto.getCreatedOn() == null ? null : dto.getCreatedOn().getTime());
	}
	
	/**
	 * Copy a Participant data transfer object to a ParticipantDBO database object
	 * 
	 * @param dbo
	 * @param dto
	 * @throws DatastoreException
	 */
	protected static void copyDboToDto(ParticipantDBO dbo, Participant dto) throws DatastoreException {		
		dto.setEvaluationId(dbo.getEvalId() == null ? null : dbo.getEvalId().toString());
		dto.setUserId(dbo.getUserId() == null ? null : dbo.getUserId().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
	}

	/**
	 * Ensure that a ParticipantDBO object has all required components
	 * 
	 * @param dbo
	 */
	private void verifyParticipantDBO(ParticipantDBO dbo) {
		EvaluationUtils.ensureNotNull(dbo.getEvalId(), "Evaluation ID");
		EvaluationUtils.ensureNotNull(dbo.getUserId(), "User ID");
		EvaluationUtils.ensureNotNull(dbo.getCreatedOn(), "Creation date");
	}
	
}
