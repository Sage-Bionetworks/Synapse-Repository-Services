package org.sagebionetworks.evaluation.dao;

import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_CONTRIBUTOR_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBMISSION_CONTRIBUTOR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.evaluation.dbo.SubmissionContributorDBO;
import org.sagebionetworks.evaluation.dbo.SubmissionDBO;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionContributor;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.util.EvaluationUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.dao.ListQueryUtils;
import org.sagebionetworks.repo.model.evaluation.SubmissionDAO;
import org.sagebionetworks.repo.model.query.SQLConstants;
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
	
	private static final String SELECT_CONTRIBUTORS_FOR_SUBMISSION_PREFIX = 
			"SELECT * FROM "+TABLE_SUBMISSION_CONTRIBUTOR+
			" WHERE "+COL_SUBMISSION_CONTRIBUTOR_SUBMISSION_ID;
	
	private static final RowMapper<SubmissionDBO> SUBMISSION_ROW_MAPPER = 
			((new SubmissionDBO()).getTableMapping());
	
	private static final RowMapper<SubmissionContributorDBO> SUBMISSION_CONTRIBUTOR_ROW_MAPPER = 
			(new SubmissionContributorDBO()).getTableMapping();
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String create(Submission dto) {
		EvaluationUtils.ensureNotNull(dto, "Submission");

		// Convert to DBO
		SubmissionDBO dbo = new SubmissionDBO();
		SubmissionUtils.copyDtoToDbo(dto, dbo);
		
		// Ensure DBO has required information
		verifySubmissionDBO(dbo);
		
		// Create DBO
		try {
			dbo = basicDao.createNew(dbo);
			List<SubmissionContributorDBO> contributors = new ArrayList<SubmissionContributorDBO>();
			if (dto.getContributors()!=null) {
				for (SubmissionContributor sc : dto.getContributors()) {
					contributors.add(createContributorDbo(
							sc.getPrincipalId(), dto.getCreatedOn(), dto.getId()));						
				}
				if (!contributors.isEmpty()) basicDao.createBatch(contributors);
			}
			return dbo.getId().toString();
		} catch (Exception e) {
			throw new DatastoreException(e.getMessage() + " id=" + dbo.getId() +
					" userId=" + dto.getUserId() + " entityId=" + dto.getEntityId());
		}
	}
	
	private SubmissionContributorDBO createContributorDbo(String principalId, Date createdOn, String submissionid) {
		SubmissionContributorDBO dbo = new SubmissionContributorDBO();
		dbo.setId(idGenerator.generateNewId(TYPE.SUBMISSION_CONTRIBUTOR_ID));
		dbo.setEtag(UUID.randomUUID().toString());
		dbo.setCreatedOn(createdOn);
		dbo.setPrincipalId(Long.parseLong(principalId));
		dbo.setSubmissionId(Long.parseLong(submissionid));
		return dbo;
	}
	


	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void addSubmissionContributor(String submissionId, SubmissionContributor dto) {
		SubmissionContributorDBO dbo = createContributorDbo(
				dto.getPrincipalId(), dto.getCreatedOn(), submissionId);
		try {
			basicDao.createNew(dbo);
		} catch (Exception e) {
			throw new DatastoreException("Failed to add contributor " + dbo.getPrincipalId() +
					" to submission " + submissionId, e);
		}
	}
	
	/*
	 * Given a list of submissions, retrieves the contributors and adds them
	 * in to the DTOs.  Used in the various query methods of this DAO.
	 */
	private void insertContributors(List<Submission> submissions) {
		if (submissions.isEmpty()) return;
		MapSqlParameterSource param = new MapSqlParameterSource();
		Map<String, Submission> submissionMap = new HashMap<String, Submission>();
		for (int i=0; i<submissions.size(); i++) {
			param.addValue(ListQueryUtils.bindVariable(i), submissions.get(i).getId());
			submissionMap.put(submissions.get(i).getId(), submissions.get(i));
		}
		// now select * from submission_contributor where submission_id in (...)
		String sql = SELECT_CONTRIBUTORS_FOR_SUBMISSION_PREFIX+
				ListQueryUtils.selectListInClause(submissions.size());
		List<SubmissionContributorDBO> contributorDbos = 
				simpleJdbcTemplate.query(sql, SUBMISSION_CONTRIBUTOR_ROW_MAPPER, param);
		for (SubmissionContributorDBO dbo : contributorDbos) {
			Submission sub = submissionMap.get(dbo.getSubmissionId().toString());
			if (sub==null) throw new IllegalStateException("Unrecognized submission Id "+dbo.getSubmissionId());
			Set<SubmissionContributor> contributorDtos = sub.getContributors();
			if (contributorDtos==null) {
				contributorDtos = new HashSet<SubmissionContributor>();
				sub.setContributors(contributorDtos);
			}
			contributorDtos.add(SubmissionUtils.convertDboToDto(dbo));
		}
	}
	
	@Override
	public Submission get(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, id);
		SubmissionDBO dbo = basicDao.getObjectByPrimaryKey(SubmissionDBO.class, param);
		Submission dto = new Submission();
		SubmissionUtils.copyDboToDto(dbo, dto);
		insertContributors(Collections.singletonList(dto));
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
		List<SubmissionDBO> dbos = simpleJdbcTemplate.query(SELECT_BY_USER_SQL, SUBMISSION_ROW_MAPPER, param);
		List<Submission> dtos = new ArrayList<Submission>();
		for (SubmissionDBO dbo : dbos) {
			Submission dto = new Submission();
			SubmissionUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		insertContributors(dtos);
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
		List<SubmissionDBO> dbos = simpleJdbcTemplate.query(SELECT_BY_EVALUATION_SQL, SUBMISSION_ROW_MAPPER, param);
		List<Submission> dtos = new ArrayList<Submission>();
		for (SubmissionDBO dbo : dbos) {
			Submission dto = new Submission();
			SubmissionUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		insertContributors(dtos);
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
		List<SubmissionDBO> dbos = simpleJdbcTemplate.query(SELECT_BY_EVAL_AND_USER_SQL, SUBMISSION_ROW_MAPPER, param);
		List<Submission> dtos = new ArrayList<Submission>();
		for (SubmissionDBO dbo : dbos) {
			Submission dto = new Submission();
			SubmissionUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		insertContributors(dtos);
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
		List<SubmissionDBO> dbos = simpleJdbcTemplate.query(SELECT_BY_EVAL_AND_STATUS_SQL, SUBMISSION_ROW_MAPPER, param);
		List<Submission> dtos = new ArrayList<Submission>();
		for (SubmissionDBO dbo : dbos) {
			Submission dto = new Submission();
			SubmissionUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		insertContributors(dtos);
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
	public void delete(String id) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, id);
		basicDao.deleteObjectByPrimaryKey(SubmissionDBO.class, param);
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
		EvaluationUtils.ensureNotNull(dbo.getId(), "Submission ID");
		EvaluationUtils.ensureNotNull(dbo.getCreatedOn(), "Creation date");
	}	
	
	// query for evaluation, list of allowed sub-statuses (optional), start ts (optional), end ts (optional), teamId
	// SELECT COUNT(*) FROM JDOSUBMISSION sb, JDOSUBMISSION_STATUS ss 
	// WHERE  sb.ID=ss.ID AND ss.EVALUATION_ID= ? AND TEAM_ID = ?
	// AND ss.STATUS in (?, ?, ?)
	// AND CREATED_ON >= startTime (long)
	// AND CREATED_ON < endTime (long)
	
	public long countSubmissionsByTeam() {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("foo", null);
		return simpleJdbcTemplate.queryForLong("sql", param);
	}
}
