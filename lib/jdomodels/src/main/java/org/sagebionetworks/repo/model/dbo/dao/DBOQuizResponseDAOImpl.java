package org.sagebionetworks.repo.model.dbo.dao;

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
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class DBOQuizResponseDAOImpl implements QuizResponseDAO {
	
	@Autowired
	private DBOBasicDao basicDao;	
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<QuizResponse> getAllResponsesForQuiz(Long quizId, Long limit,
			Long offset) throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getAllResponsesForQuizCount(Long quizId)
			throws DatastoreException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<QuizResponse> getUserResponsesForQuiz(Long quizId,
			Long principalId, Long limit, Long offset)
			throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getUserResponsesForQuizCount(Long quizId, Long principalId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public PassingRecord getPassingRecord(Long quizId, Long principalId)
			throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		// TODO Auto-generated method stub
		
	}

}
