package org.sagebionetworks.repo.model;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.web.NotFoundException;

public interface QuizResponseDAO {
	QuizResponse create(QuizResponse dto, PassingRecord passingRecord) throws DatastoreException;

	QuizResponse get(String id) throws DatastoreException, NotFoundException;

	List<QuizResponse> getAllResponsesForQuiz(Long quizId, Long limit, Long offset) throws DatastoreException;
	
	long getAllResponsesForQuizCount(Long quizId) throws DatastoreException;
	
	List<QuizResponse> getUserResponsesForQuiz(Long quizId, Long principalId, Long limit, Long offset) throws DatastoreException;
	
	long getUserResponsesForQuizCount(Long quizId, Long principalId);
	
	Optional<PassingRecord> getLatestPassingRecord(Long quizId, Long principalId) throws DatastoreException, NotFoundException;
	
	void delete(Long id) throws DatastoreException, NotFoundException;

	List<PassingRecord> getAllPassingRecords(Long quizId, Long principalId, Long limit, Long offset) throws DatastoreException, NotFoundException;

	long getAllPassingRecordsCount(Long quizId, Long principalId) throws DatastoreException;
	
	boolean revokeQuizResponse(Long responseId);
}
