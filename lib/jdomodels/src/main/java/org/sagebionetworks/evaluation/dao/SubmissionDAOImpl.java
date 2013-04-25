package org.sagebionetworks.evaluation.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.evaluation.dbo.SubmissionDBO;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.query.jdo.SQLConstants;
import org.sagebionetworks.evaluation.util.EvaluationUtils;
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
	
	private static final String ID = DBOConstants.PARAM_SUBMISSION_ID;
	private static final String USER_ID = DBOConstants.PARAM_SUBMISSION_USER_ID;
	private static final String EVAL_ID = DBOConstants.PARAM_SUBMISSION_EVAL_ID;
	private static final String STATUS = DBOConstants.PARAM_SUBSTATUS_STATUS;
	
	private static final String SELECT_ALL = "SELECT *";
	private static final String SELECT_COUNT = "SELECT COUNT(*)";
	private static final String LIMIT_OFFSET = 			
			" LIMIT :"+ SQLConstants.LIMIT_PARAM_NAME +
			" OFFSET :" + SQLConstants.OFFSET_PARAM_NAME;
	
	private static final String BY_USER_SQL = 
			" FROM "+ SQLConstants.TABLE_SUBMISSION +
			" WHERE "+ SQLConstants.COL_SUBMISSION_USER_ID + "=:"+ USER_ID;

	private static final String BY_EVALUATION_SQL = 
			" FROM "+ SQLConstants.TABLE_SUBMISSION +
			" WHERE "+ SQLConstants.COL_SUBMISSION_EVAL_ID + "=:"+ EVAL_ID;
	
	private static final String BY_EVAL_AND_USER_SQL = 
			" FROM "+ SQLConstants.TABLE_SUBMISSION +
			" WHERE "+ SQLConstants.COL_SUBMISSION_USER_ID + "=:"+ USER_ID +
			" AND " + SQLConstants.COL_SUBMISSION_EVAL_ID + "=:"+ EVAL_ID;
	
	private static final String BY_EVAL_AND_STATUS_SQL = 
			" FROM "+ SQLConstants.TABLE_SUBMISSION + " n" +
			" INNER JOIN " + SQLConstants.TABLE_SUBSTATUS + " r" +
			" ON n." + SQLConstants.COL_SUBMISSION_ID + " = r." + SQLConstants.COL_SUBSTATUS_SUBMISSION_ID +
			" WHERE n."+ SQLConstants.COL_SUBMISSION_EVAL_ID + "=:"+ EVAL_ID +
			" AND r." + SQLConstants.COL_SUBSTATUS_STATUS + "=:" + STATUS;
	
	private static final String SELECT_BY_USER_SQL = 
			SELECT_ALL + BY_USER_SQL + LIMIT_OFFSET;
	
	private static final String SELECT_BY_EVALUATION_SQL = 
			SELECT_ALL + BY_EVALUATION_SQL + LIMIT_OFFSET;
	
	private static final String SELECT_BY_EVAL_AND_USER_SQL = 
			SELECT_ALL + BY_EVAL_AND_USER_SQL + LIMIT_OFFSET;
	
	private static final String SELECT_BY_EVAL_AND_STATUS_SQL = 
			SELECT_ALL + BY_EVAL_AND_STATUS_SQL + LIMIT_OFFSET;
	
	private static final String COUNT_BY_USER_SQL = 
			SELECT_COUNT + BY_USER_SQL;
	
	private static final String COUNT_BY_EVAL_SQL = 
			SELECT_COUNT + BY_EVALUATION_SQL;
	
	private static final String COUNT_BY_EVAL_AND_USER_SQL = 
			SELECT_COUNT + BY_EVAL_AND_USER_SQL;
	
	private static final String COUNT_BY_EVAL_AND_STATUS_SQL = 
			SELECT_COUNT + BY_EVAL_AND_STATUS_SQL;
	
	private static final RowMapper<SubmissionDBO> rowMapper = ((new SubmissionDBO()).getTableMapping());

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String create(Submission dto) {
		EvaluationUtils.ensureNotNull(dto, "Submission");

		// Convert to DBO
		SubmissionDBO dbo = new SubmissionDBO();
		copyDtoToDbo(dto, dbo);
		
		// Ensure DBO has required information
		verifySubmissionDBO(dbo);
		
		// Create DBO
		try {
			dbo = basicDao.createNew(dbo);
			return dbo.getId().toString();
		} catch (Exception e) {
			throw new DatastoreException(e.getMessage() + " id=" + dbo.getId() +
					" userId=" + dto.getUserId() + " entityId=" + dto.getEntityId());
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
	public long getCount() throws DatastoreException, NotFoundException {
		return basicDao.getCount(SubmissionDBO.class);
	}

	@Override
	public List<Submission> getAllByUser(String userId, long limit, long offset) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(SQLConstants.OFFSET_PARAM_NAME, offset);
		param.addValue(SQLConstants.LIMIT_PARAM_NAME, limit);
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
	public long getCountByUser(String userId) throws DatastoreException, NotFoundException {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(USER_ID, userId);
		return simpleJdbcTemplate.queryForLong(COUNT_BY_USER_SQL, parameters);
	}
	
	@Override
	public List<Submission> getAllByEvaluation(String evalId, long limit, long offset) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(SQLConstants.OFFSET_PARAM_NAME, offset);
		param.addValue(SQLConstants.LIMIT_PARAM_NAME, limit);
		param.addValue(EVAL_ID, evalId);		
		List<SubmissionDBO> dbos = simpleJdbcTemplate.query(SELECT_BY_EVALUATION_SQL, rowMapper, param);
		List<Submission> dtos = new ArrayList<Submission>();
		for (SubmissionDBO dbo : dbos) {
			Submission dto = new Submission();
			copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}
	
	@Override
	public long getCountByEvaluation(String evalId) throws DatastoreException, NotFoundException {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(EVAL_ID, evalId);
		return simpleJdbcTemplate.queryForLong(COUNT_BY_EVAL_SQL, parameters);
	}

	@Override
	public List<Submission> getAllByEvaluationAndUser(String evalId, String principalId, long limit, long offset) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(SQLConstants.OFFSET_PARAM_NAME, offset);
		param.addValue(SQLConstants.LIMIT_PARAM_NAME, limit);	
		param.addValue(USER_ID, principalId);
		param.addValue(EVAL_ID, evalId);
		List<SubmissionDBO> dbos = simpleJdbcTemplate.query(SELECT_BY_EVAL_AND_USER_SQL, rowMapper, param);
		List<Submission> dtos = new ArrayList<Submission>();
		for (SubmissionDBO dbo : dbos) {
			Submission dto = new Submission();
			copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}
	
	@Override
	public long getCountByEvaluationAndUser(String evalId, String userId) throws DatastoreException, NotFoundException {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(EVAL_ID, evalId);
		parameters.put(USER_ID, userId);
		return simpleJdbcTemplate.queryForLong(COUNT_BY_EVAL_AND_USER_SQL, parameters);
	}
	
	@Override
	public List<Submission> getAllByEvaluationAndStatus(String evalId, SubmissionStatusEnum status, long limit, long offset) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(SQLConstants.OFFSET_PARAM_NAME, offset);
		param.addValue(SQLConstants.LIMIT_PARAM_NAME, limit);	
		param.addValue(EVAL_ID, evalId);
		param.addValue(STATUS, status.ordinal());
		List<SubmissionDBO> dbos = simpleJdbcTemplate.query(SELECT_BY_EVAL_AND_STATUS_SQL, rowMapper, param);
		List<Submission> dtos = new ArrayList<Submission>();
		for (SubmissionDBO dbo : dbos) {
			Submission dto = new Submission();
			copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}
	
	@Override
	public long getCountByEvaluationAndStatus(String evalId, SubmissionStatusEnum status) throws DatastoreException, NotFoundException {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(EVAL_ID, evalId);
		parameters.put(STATUS, status.ordinal());
		return simpleJdbcTemplate.queryForLong(COUNT_BY_EVAL_AND_STATUS_SQL, parameters);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, id);
		basicDao.deleteObjectById(SubmissionDBO.class, param);		
	}

	/**
	 * Copy a Submission data transfer object to a SubmissionDBO database object
	 * 
	 * @param dbo
	 * @param dto
	 * @throws DatastoreException
	 */
	protected static void copyDtoToDbo(Submission dto, SubmissionDBO dbo) {	
		try {
			dbo.setId(dto.getId() == null ? null : Long.parseLong(dto.getId()));
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Invalid Submission ID: " + dto.getId());
		}
		try {
			dbo.setUserId(dto.getUserId() == null ? null : Long.parseLong(dto.getUserId()));
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Invalid User ID: " + dto.getUserId());
		}
		try {
			dbo.setEvalId(dto.getEvaluationId() == null ? null : Long.parseLong(dto.getEvaluationId()));
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Invalid Evaluation ID: " + dto.getEvaluationId());
		}
		dbo.setEntityId(dto.getEntityId() == null ? null : KeyFactory.stringToKey(dto.getEntityId()));
		dbo.setVersionNumber(dto.getVersionNumber());
		dbo.setName(dto.getName());
		dbo.setCreatedOn(dto.getCreatedOn() == null ? null : dto.getCreatedOn().getTime());
		dbo.setEntityBundle(dto.getEntityBundleJSON() == null ? null : dto.getEntityBundleJSON().getBytes());
	}
	
	/**
	 * Copy a SubmissionDBO database object to a Submission data transfer object
	 * 
	 * @param dto
	 * @param dbo
	 */
	protected static void copyDboToDto(SubmissionDBO dbo, Submission dto) throws DatastoreException {
		dto.setId(dbo.getId() == null ? null : dbo.getId().toString());
		dto.setUserId(dbo.getUserId() == null ? null : dbo.getUserId().toString());
		dto.setEvaluationId(dbo.getEvalId() == null ? null : dbo.getEvalId().toString());
		dto.setEntityId(dbo.getEntityId() == null ? null : KeyFactory.keyToString(dbo.getEntityId()));
		dto.setVersionNumber(dbo.getVersionNumber());
		dto.setName(dbo.getName());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setEntityBundleJSON(dbo.getEntityBundle() == null ? null : new String(dbo.getEntityBundle()));
	}

	/**
	 * Ensure that a SubmissionDBO object has all required components
	 * 
	 * @param dbo
	 */
	private void verifySubmissionDBO(SubmissionDBO dbo) {
		EvaluationUtils.ensureNotNull(dbo.getEvalId(), "Evaluation ID");
		EvaluationUtils.ensureNotNull(dbo.getUserId(), "User ID");
		EvaluationUtils.ensureNotNull(dbo.getEntityId(), "Entity ID");
		EvaluationUtils.ensureNotNull(dbo.getVersionNumber(), "Entity Version");
		EvaluationUtils.ensureNotNull(dbo.getEntityBundle(), "Serialized EntityWithAnnotations");
		EvaluationUtils.ensureNotNull(dbo.getId(), "Submission ID");
		EvaluationUtils.ensureNotNull(dbo.getCreatedOn(), "Creation date");
	}	
}
