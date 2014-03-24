package org.sagebionetworks.repo.manager;

/**
 * 
 * @author brucehoff
 *
 */
public interface CertifiedUserManager {
	
	public Object getCertificationQuestionnaire();
	
	public Object submitCertificationQuestionnaireResponse(Object quizResponse);

}
