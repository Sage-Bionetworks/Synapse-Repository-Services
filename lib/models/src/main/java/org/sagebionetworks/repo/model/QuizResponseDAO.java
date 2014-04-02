package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.web.NotFoundException;

public interface QuizResponseDAO {
	public QuizResponse create(QuizResponse dto) throws DatastoreException;

	public QuizResponse get(String id) throws DatastoreException, NotFoundException;

	public List<QuizResponse> getAllResponsesForQuiz(Long quizId, Long limit, Long offset) throws DatastoreException;
	
	public long getAllResponsesForQuizCount(Long quizId) throws DatastoreException;
	
	public List<QuizResponse> getUserResponsesForQuiz(Long quizId, Long principalId, Long limit, Long offset) throws DatastoreException;
	
	public long getUserResponsesForQuizCount(Long quizId, Long principalId);
	
	public PassingRecord getPassingRecord(Long quizId, Long principalId) throws DatastoreException;
	
	public void delete(String id) throws DatastoreException, NotFoundException;
}
