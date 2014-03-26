package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.manager.CertifiedUserManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.questionnaire.PassingRecord;
import org.sagebionetworks.repo.model.questionnaire.Questionnaire;
import org.sagebionetworks.repo.model.questionnaire.QuestionnaireResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class CertifiedUserServiceImpl implements CertifiedUserService {
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private CertifiedUserManager certifiedUserManager;
	
	@Override
	public Questionnaire getCertificationQuestionnaire() {
		return certifiedUserManager.getCertificationQuestionnaire();
	}

	@Override
	public QuestionnaireResponse submitCertificationQuestionnaireResponse(
			Long userId, QuestionnaireResponse response) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return certifiedUserManager.submitCertificationQuestionnaireResponse(userInfo, response);
	}

	@Override
	public PaginatedResults<QuestionnaireResponse> getQuestionnaireResponses(
			Long userId, Long principalId, long limit, long offset) throws NotFoundException {

		UserInfo userInfo = userManager.getUserInfo(userId);
		return certifiedUserManager.getQuestionnaireResponses(userInfo, principalId, limit, offset);
	}

	@Override
	public void deleteQuestionnaireResponse(Long userId, Long responseId) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		certifiedUserManager.deleteQuestionnaireResponse(userInfo, responseId);
	}

	@Override
	public PassingRecord getPassingRecord(Long userId, Long principalId) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return certifiedUserManager.getPassingRecord(userInfo, principalId);
	}

}
