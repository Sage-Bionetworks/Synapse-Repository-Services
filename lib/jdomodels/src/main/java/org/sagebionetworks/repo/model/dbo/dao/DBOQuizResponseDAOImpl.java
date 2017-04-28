package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUIZ_RESPONSE_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUIZ_RESPONSE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUIZ_RESPONSE_QUIZ_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUIZ_RESPONSE_SCORE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_QUIZ_RESPONSE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QuizResponseDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOQuizResponse;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.sagebionetworks.repo.transactions.WriteTransaction;

public class DBOQuizResponseDAOImpl implements QuizResponseDAO {
	
	@Autowired
	private DBOBasicDao basicDao;	
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;

	private static final RowMapper<DBOQuizResponse> QUIZ_RESPONSE_ROW_MAPPER = (new DBOQuizResponse()).getTableMapping();
	
	private static final String SELECT_FOR_QUIZ_ID_CORE = " FROM "+TABLE_QUIZ_RESPONSE+" WHERE "+
			COL_QUIZ_RESPONSE_QUIZ_ID+"=:"+COL_QUIZ_RESPONSE_QUIZ_ID;

	private static final String SELECT_FOR_QUIZ_ID_PAGINATED = "SELECT * "+SELECT_FOR_QUIZ_ID_CORE+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_FOR_QUIZ_ID_COUNT = "SELECT COUNT(ID) "+SELECT_FOR_QUIZ_ID_CORE;
	
	private static final String SELECT_FOR_QUIZ_ID_AND_USER_CORE = " FROM "+TABLE_QUIZ_RESPONSE+" WHERE "+
			COL_QUIZ_RESPONSE_QUIZ_ID+"=:"+COL_QUIZ_RESPONSE_QUIZ_ID+" AND "+
			COL_QUIZ_RESPONSE_CREATED_BY+"=:"+COL_QUIZ_RESPONSE_CREATED_BY;

	private static final String SELECT_FOR_QUIZ_ID_AND_USER_PAGINATED = "SELECT * "+SELECT_FOR_QUIZ_ID_AND_USER_CORE+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_FOR_QUIZ_ID_AND_USER_COUNT = "SELECT COUNT(ID) "+SELECT_FOR_QUIZ_ID_AND_USER_CORE;
	
	private static final String SELECT_RESPONSES_FOR_USER_AND_QUIZ_CORE = " FROM "+TABLE_QUIZ_RESPONSE+" WHERE "+
			COL_QUIZ_RESPONSE_QUIZ_ID+"=:"+COL_QUIZ_RESPONSE_QUIZ_ID+" AND "+
			COL_QUIZ_RESPONSE_CREATED_BY+"=:"+COL_QUIZ_RESPONSE_CREATED_BY;

	// select * from QUIZ_RESPONSE where CREATED_BY=? and QUIZ_ID=? order by score desc limit 1
	private static final String SELECT_BEST_RESPONSE_FOR_USER_AND_QUIZ = "SELECT * "+
			SELECT_RESPONSES_FOR_USER_AND_QUIZ_CORE+
			" ORDER BY "+COL_QUIZ_RESPONSE_SCORE+" DESC LIMIT 1";

	private static final String SELECT_RESPONSES_FOR_USER_AND_QUIZ_PAGINATED = "SELECT * "+
			SELECT_RESPONSES_FOR_USER_AND_QUIZ_CORE+" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;

	private static final String SELECT_RESPONSES_FOR_USER_AND_QUIZ_COUNT = "SELECT COUNT(ID) "+SELECT_RESPONSES_FOR_USER_AND_QUIZ_CORE;

	@WriteTransaction
	@Override
	public QuizResponse create(QuizResponse dto, PassingRecord passingRecord) throws DatastoreException {
		
		DBOQuizResponse dbo = new DBOQuizResponse();
		QuizResponseUtils.copyDtoToDbo(dto, passingRecord, dbo);
		if (dbo.getId() == null) {
			dbo.setId(idGenerator.generateNewId(IdType.QUIZ_RESPONSE_ID));
		}
		dbo = basicDao.createNew(dbo);
		QuizResponse result = QuizResponseUtils.copyDboToDto(dbo);
		return result;
	}

	@Override
	public QuizResponse get(String id) throws DatastoreException,
			NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_QUIZ_RESPONSE_ID.toLowerCase(), id);
		DBOQuizResponse dbo = basicDao.getObjectByPrimaryKey(DBOQuizResponse.class, param);
		QuizResponse dto = QuizResponseUtils.copyDboToDto(dbo);
		return dto;
	}

	@WriteTransaction
	@Override
	public void delete(Long id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_QUIZ_RESPONSE_ID.toLowerCase(), id);
		basicDao.deleteObjectByPrimaryKey(DBOQuizResponse.class, param);
	}

	@Override
	public List<QuizResponse> getAllResponsesForQuiz(Long quizId, Long limit,
			Long offset) throws DatastoreException {
		List<QuizResponse>  dtos = new ArrayList<QuizResponse>();
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_QUIZ_RESPONSE_QUIZ_ID, quizId);
		param.addValue(LIMIT_PARAM_NAME, limit);
		param.addValue(OFFSET_PARAM_NAME, offset);
		List<DBOQuizResponse> dbos = namedJdbcTemplate.query(SELECT_FOR_QUIZ_ID_PAGINATED, param, QUIZ_RESPONSE_ROW_MAPPER);
		for (DBOQuizResponse dbo : dbos) {
			QuizResponse dto = QuizResponseUtils.copyDboToDto(dbo);
			dtos.add(dto);
		}
		return dtos;
	}

	
	@Override
	public long getAllResponsesForQuizCount(Long quizId)
			throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_QUIZ_RESPONSE_QUIZ_ID, quizId);
		return namedJdbcTemplate.queryForObject(SELECT_FOR_QUIZ_ID_COUNT, param, Long.class);
	}

	@Override
	public List<QuizResponse> getUserResponsesForQuiz(Long quizId,
			Long principalId, Long limit, Long offset)
			throws DatastoreException {
		List<QuizResponse>  dtos = new ArrayList<QuizResponse>();
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_QUIZ_RESPONSE_QUIZ_ID, quizId);
		param.addValue(COL_QUIZ_RESPONSE_CREATED_BY, principalId);
		param.addValue(LIMIT_PARAM_NAME, limit);
		param.addValue(OFFSET_PARAM_NAME, offset);
		List<DBOQuizResponse> dbos = namedJdbcTemplate.query(SELECT_FOR_QUIZ_ID_AND_USER_PAGINATED, param, QUIZ_RESPONSE_ROW_MAPPER);
		for (DBOQuizResponse dbo : dbos) {
			QuizResponse dto = QuizResponseUtils.copyDboToDto(dbo);
			dtos.add(dto);
		}
		return dtos;
	}

	@Override
	public long getUserResponsesForQuizCount(Long quizId, Long principalId) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_QUIZ_RESPONSE_QUIZ_ID, quizId);
		param.addValue(COL_QUIZ_RESPONSE_CREATED_BY, principalId);
		return namedJdbcTemplate.queryForObject(SELECT_FOR_QUIZ_ID_AND_USER_COUNT, param, Long.class);
	}		

	@Override
	public PassingRecord getPassingRecord(Long quizId, Long principalId)
			throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_QUIZ_RESPONSE_QUIZ_ID, quizId);
		param.addValue(COL_QUIZ_RESPONSE_CREATED_BY, principalId);
		try {
			DBOQuizResponse dbo = namedJdbcTemplate.queryForObject(SELECT_BEST_RESPONSE_FOR_USER_AND_QUIZ, param, QUIZ_RESPONSE_ROW_MAPPER);
			byte[] prSerizalized = dbo.getPassingRecord();
			PassingRecord passingRecord = (PassingRecord)JDOSecondaryPropertyUtils.decompressedObject(prSerizalized);
			passingRecord.setResponseId(dbo.getId());
			return passingRecord;
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("No quiz results for quiz " + quizId + " and user " + principalId);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	public List<PassingRecord> getAllPassingRecords(Long quizId, Long principalId, Long limit, Long offset) throws DatastoreException, NotFoundException {
		List<PassingRecord>  dtos = new ArrayList<PassingRecord>();
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_QUIZ_RESPONSE_QUIZ_ID, quizId);
		param.addValue(COL_QUIZ_RESPONSE_CREATED_BY, principalId);
		param.addValue(LIMIT_PARAM_NAME, limit);
		param.addValue(OFFSET_PARAM_NAME, offset);
		try {
			List<DBOQuizResponse> dbos = namedJdbcTemplate.query(SELECT_RESPONSES_FOR_USER_AND_QUIZ_PAGINATED, param, QUIZ_RESPONSE_ROW_MAPPER);
			for (DBOQuizResponse dbo : dbos) {
				byte[] prSerizalized = dbo.getPassingRecord();
				PassingRecord passingRecord = (PassingRecord)JDOSecondaryPropertyUtils.decompressedObject(prSerizalized);
				passingRecord.setResponseId(dbo.getId());
				dtos.add(passingRecord);
			}
			return dtos;
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("No quiz results for quiz " + quizId + " and user " + principalId);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	public long getAllPassingRecordsCount(Long quizId, Long principalId) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_QUIZ_RESPONSE_QUIZ_ID, quizId);
		param.addValue(COL_QUIZ_RESPONSE_CREATED_BY, principalId);
		return namedJdbcTemplate.queryForObject(SELECT_RESPONSES_FOR_USER_AND_QUIZ_COUNT, param, Long.class);
	}
}
