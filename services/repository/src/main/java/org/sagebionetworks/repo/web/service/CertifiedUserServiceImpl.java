package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.CertifiedUserManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.Quiz;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class CertifiedUserServiceImpl implements CertifiedUserService {
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private CertifiedUserManager certifiedUserManager;
	
	@Override
	public Quiz getCertificationQuiz(Long userId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return certifiedUserManager.getCertificationQuiz(userInfo);
	}

	@Override
	public PassingRecord submitCertificationQuizResponse(
			Long userId, QuizResponse response) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return certifiedUserManager.submitCertificationQuizResponse(userInfo, response);
	}

	@Override
	public PaginatedResults<QuizResponse> getQuizResponses(
			Long userId, Long principalId, long limit, long offset) throws NotFoundException {

		UserInfo userInfo = userManager.getUserInfo(userId);
		return certifiedUserManager.getQuizResponses(userInfo, principalId, limit, offset);
	}

	@Override
	public void deleteQuizResponse(Long userId, Long responseId) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		certifiedUserManager.deleteQuizResponse(userInfo, responseId);
	}

	@Override
	public PassingRecord getPassingRecord(Long userId, Long principalId) throws NotFoundException {
		return certifiedUserManager.getPassingRecord(principalId);
	}

	@Override
	public PaginatedResults<PassingRecord> getPassingRecords(
			Long userId, Long principalId, long limit, long offset) throws NotFoundException {

		UserInfo userInfo = userManager.getUserInfo(userId);
		return certifiedUserManager.getPassingRecords(userInfo, principalId, limit, offset);
	}
	
	@Override
	public void setUserCertificationStatus(Long userId, Long principalId, boolean isCertified) 
			throws DatastoreException, NotFoundException {

				UserInfo userInfo = userManager.getUserInfo(userId);
				certifiedUserManager.setUserCertificationStatus(userInfo, principalId, isCertified);
	}

}
