package org.sagebionetworks.repo.manager;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.Quiz;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * 
 * @author brucehoff
 *
 */
public interface CertifiedUserManager {
	
	/**
	 * Get the Certified Users Quiz
	 * @return
	 */
	public Quiz getCertificationQuiz(UserInfo userInfo);
	
	/**
	 * Store the response and score it. If the user passes they are added to the 
	 * Certified Users group
	 * 
	 * @parm userInfo
	 * @param response
	 * @return
	 * @throws NotFoundException 
	 */
	public PassingRecord submitCertificationQuizResponse(UserInfo userInfo, QuizResponse response) throws NotFoundException;

	/**
	 * Retrieve the questionnaire responses in the system, optionally filtering by user Id.
	 * Must be a Synapse admin to make this call
	 * @param userInfo
	 * @param questionnaireId
	 * @param principalId
	 * @param limit
	 * @param offset
	 * @return
	 */
	public PaginatedResults<QuizResponse> getQuizResponses(UserInfo userInfo, Long principalId, long limit, long offset);
	
	/**
	 * Delete a Quiz Response
	 * @param userInfo
	 * @param responseId
	 * @throws NotFoundException 
	 */
	public void deleteQuizResponse(UserInfo userInfo, Long responseId) throws NotFoundException;
	
	
	/**
	 * Get the info about the user (indicated by principalId) passing the test.

	 * @param principalId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public PassingRecord getPassingRecord(Long principalId) throws DatastoreException, NotFoundException;

	/**
	 * Get all Passing Records for a given user.
	 * Must be a Synapse admin to make this call.
	 */
	public PaginatedResults<PassingRecord> getPassingRecords(UserInfo userInfo, Long principalId, long limit, long offset) throws DatastoreException, NotFoundException;


	/**
	 * For integration testing purposes, this sets the 'certified user' status of the 
	 * user referred to by 'principalId' to the value given by 'isCertified'.  
	 * Can only be invoked by a Synapse administrator
	 * @param userInfo
	 * @param principalId
	 * @param isCertified
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void setUserCertificationStatus(UserInfo userInfo, Long principalId, boolean isCertified) throws DatastoreException, NotFoundException;


}
