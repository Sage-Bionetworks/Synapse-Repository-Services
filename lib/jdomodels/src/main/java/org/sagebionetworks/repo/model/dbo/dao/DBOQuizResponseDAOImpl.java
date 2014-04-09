package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUIZ_RESPONSE_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUIZ_RESPONSE_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUIZ_RESPONSE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUIZ_RESPONSE_PASSED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUIZ_RESPONSE_QUIZ_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUIZ_RESPONSE_SCORE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_QUIZ_RESPONSE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QuizResponseDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOQuizResponse;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBOQuizResponseDAOImpl implements QuizResponseDAO {
	
	@Autowired
	private DBOBasicDao basicDao;	
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

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
	
	// select * from QUIZ_RESPONSE where CREATED_BY=? and QUIZ_ID=? order by score desc limit 1
	private static final String SELECT_BEST_RESPONSE_FOR_USER_AND_QUIZ = "SELECT "+
			COL_QUIZ_RESPONSE_ID+", "+
			COL_QUIZ_RESPONSE_CREATED_BY+", "+
			COL_QUIZ_RESPONSE_CREATED_ON+", "+
			COL_QUIZ_RESPONSE_QUIZ_ID+", "+
			COL_QUIZ_RESPONSE_SCORE+", "+
			COL_QUIZ_RESPONSE_PASSED+
			" FROM "+TABLE_QUIZ_RESPONSE+" WHERE "+
			COL_QUIZ_RESPONSE_QUIZ_ID+"=:"+COL_QUIZ_RESPONSE_QUIZ_ID+" AND "+
			COL_QUIZ_RESPONSE_CREATED_BY+"=:"+COL_QUIZ_RESPONSE_CREATED_BY+
			" ORDER BY "+COL_QUIZ_RESPONSE_SCORE+" DESC LIMIT 1";
	
	private static final RowMapper<PassingRecord> PASSING_RECORD_ROW_MAPPER = new RowMapper<PassingRecord>() {
		@Override
		public PassingRecord mapRow(ResultSet rs, int arg1) throws SQLException {
			PassingRecord pr = new PassingRecord();
			pr.setPassed(rs.getBoolean(COL_QUIZ_RESPONSE_PASSED));
			pr.setPassedOn(new Date(rs.getLong(COL_QUIZ_RESPONSE_CREATED_ON)));
			pr.setQuizId(rs.getLong(COL_QUIZ_RESPONSE_QUIZ_ID));
			pr.setResponseId(rs.getLong(COL_QUIZ_RESPONSE_ID));
			pr.setScore(rs.getLong(COL_QUIZ_RESPONSE_SCORE));
			pr.setUserId(rs.getString(COL_QUIZ_RESPONSE_CREATED_BY));
			return pr;
		}
	};

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public QuizResponse create(QuizResponse dto) throws DatastoreException {
		
		DBOQuizResponse dbo = new DBOQuizResponse();
		QuizResponseUtils.copyDtoToDbo(dto, dbo);
		if (dbo.getId() == null) {
			dbo.setId(idGenerator.generateNewId());
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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
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
		List<DBOQuizResponse> dbos = simpleJdbcTemplate.query(SELECT_FOR_QUIZ_ID_PAGINATED, QUIZ_RESPONSE_ROW_MAPPER, param);
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
		return simpleJdbcTemplate.queryForLong(SELECT_FOR_QUIZ_ID_COUNT, param);
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
		List<DBOQuizResponse> dbos = simpleJdbcTemplate.query(SELECT_FOR_QUIZ_ID_AND_USER_PAGINATED, QUIZ_RESPONSE_ROW_MAPPER, param);
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
		return simpleJdbcTemplate.queryForLong(SELECT_FOR_QUIZ_ID_AND_USER_COUNT, param);
	}		

	@Override
	public PassingRecord getPassingRecord(Long quizId, Long principalId)
			throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_QUIZ_RESPONSE_QUIZ_ID, quizId);
		param.addValue(COL_QUIZ_RESPONSE_CREATED_BY, principalId);
		try {
			return simpleJdbcTemplate.queryForObject(SELECT_BEST_RESPONSE_FOR_USER_AND_QUIZ, PASSING_RECORD_ROW_MAPPER, param);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("No quiz results for quiz "+quizId+" and user "+principalId, e);
		}
	}

}
