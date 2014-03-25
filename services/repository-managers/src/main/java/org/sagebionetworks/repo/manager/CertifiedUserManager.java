package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.questionnaire.Questionnaire;
import org.sagebionetworks.repo.model.questionnaire.QuestionnaireResponse;

/**
 * 
 * @author brucehoff
 *
 */
public interface CertifiedUserManager {
	
	/**
	 * Get the (static) Certified Users Questionnaire
	 * @return
	 */
	public Questionnaire getCertificationQuestionnaire();
	
	/**
	 * Store the response and score it. If the user passes they are added to the 
	 * Certified Users group
	 * 
	 * @param response
	 * @return
	 */
	public QuestionnaireResponse submitCertificationQuestionnaireResponse(UserInfo userInfo, QuestionnaireResponse response);

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
	public PaginatedResults<QuestionnaireResponse> getQuestionnaireResponses(UserInfo userInfo, Long principalId, long limit, long offset);
	
	/**
	 * Delete a Questionnaire Response
	 * @param userInfo
	 * @param responseId
	 */
	public void deleteQuestionnaireResponse(UserInfo userInfo, Long responseId);
	
	
	/**
	 * Get the info about the user (indicated by principalId) passing the test.
	 * Requestor must be the 
	 * @param userInfo
	 * @param principalId
	 * @return
	 */
	public Object getPassingRecord(UserInfo userInfo, Long principalId);
}
