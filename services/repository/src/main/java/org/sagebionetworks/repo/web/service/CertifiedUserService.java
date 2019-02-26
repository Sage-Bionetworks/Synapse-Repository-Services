package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.Quiz;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.web.NotFoundException;

public interface CertifiedUserService {
	
	/**
	 * Get the (static) Certified Users Quiz
	 * @return
	 */
	public Quiz getCertificationQuiz(Long userId);
	
	/**
	 * Store the response and score it. If the user passes they are added to the 
	 * Certified Users group
	 * 
	 * @param userId
	 * @param response
	 * @return
	 * @throws NotFoundException 
	 */
	public PassingRecord submitCertificationQuizResponse(Long userId, QuizResponse response) throws NotFoundException;

	/**
	 * Retrieve the questionnaire responses in the system, optionally filtering by user Id.
	 * Must be a Synapse admin to make this call
	 * @param userId
	 * @param questionnaireId
	 * @param principalId
	 * @param limit
	 * @param offset
	 * @return
	 * @throws NotFoundException 
	 */
	public PaginatedResults<QuizResponse> getQuizResponses(Long userId, Long principalId, long limit, long offset) throws NotFoundException;
	
	/**
	 * Delete a Quiz Response
	 * @param userId
	 * @param responseId
	 * @throws NotFoundException 
	 */
	public void deleteQuizResponse(Long userId, Long responseId) throws NotFoundException;
	
	
	/**
	 * Get the info about the user (indicated by principalId) passing the test.
	 * Requestor must be the 
	 * @param userIf
	 * @param principalId
	 * @return
	 */
	public PassingRecord getPassingRecord(Long userId, Long principalId) throws NotFoundException;

	/**
	 * Get all Passing Records for a given user.
	 * Must be a Synapse admin to make this call
	 */
	public PaginatedResults<PassingRecord> getPassingRecords(Long userId, Long principalId, long limit, long offset) throws NotFoundException;
	
	/**
	 * For integration testing only.  'userId' must be a Synapse administrator
	 * @param userId
	 * @param principalId
	 * @param isCertified
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void setUserCertificationStatus(Long userId, Long principalId, boolean isCertified) throws DatastoreException, NotFoundException;


}
