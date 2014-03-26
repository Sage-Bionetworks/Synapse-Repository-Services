package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.questionnaire.PassingRecord;
import org.sagebionetworks.repo.model.questionnaire.Questionnaire;
import org.sagebionetworks.repo.model.questionnaire.QuestionnaireResponse;
import org.sagebionetworks.repo.web.NotFoundException;

public interface CertifiedUserService {
	
	/**
	 * Get the (static) Certified Users Questionnaire
	 * @return
	 */
	public Questionnaire getCertificationQuestionnaire();
	
	/**
	 * Store the response and score it. If the user passes they are added to the 
	 * Certified Users group
	 * 
	 * @param userId
	 * @param response
	 * @return
	 * @throws NotFoundException 
	 */
	public QuestionnaireResponse submitCertificationQuestionnaireResponse(Long userId, QuestionnaireResponse response) throws NotFoundException;

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
	public PaginatedResults<QuestionnaireResponse> getQuestionnaireResponses(Long userId, Long principalId, long limit, long offset) throws NotFoundException;
	
	/**
	 * Delete a Questionnaire Response
	 * @param userId
	 * @param responseId
	 * @throws NotFoundException 
	 */
	public void deleteQuestionnaireResponse(Long userId, Long responseId) throws NotFoundException;
	
	
	/**
	 * Get the info about the user (indicated by principalId) passing the test.
	 * Requestor must be the 
	 * @param userIf
	 * @param principalId
	 * @return
	 */
	public PassingRecord getPassingRecord(Long userId, Long principalId) throws NotFoundException;
}
