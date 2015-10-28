package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.verification.VerificationPagedResults;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.springframework.beans.factory.annotation.Autowired;

public class VerificationManagerImpl implements VerificationManager {
	
	@Autowired
	private VerificationDAO verificationDao;
	
	@Autowired
	private UserProfileManager userProfileManager;
	
	@Autowired
	private FileHandleManager fileHandleManager;
	
	public VerificationManagerImpl() {}

	// for testing
	public VerificationManagerImpl(
			VerificationDAO verificationDao,
			UserProfileManager userProfileManager,
			FileHandleManager fileHandleManager) {
		this.verificationDao = verificationDao;
		this.userProfileManager = userProfileManager;
		this.fileHandleManager = fileHandleManager;
	}

	@Override
	public VerificationSubmission createVerificationSubmission(
			UserInfo userInfo, VerificationSubmission verificationSubmission) {
		// TODO check whether there is already an active verification submission
		// TODO validate the content 
		// 		Content must match user profile, emails, ORCID in system at the time the request is made.
		//		Rejected if required fields are blank.
		// TODO including whether user is owner of all filehandle Ids
		return verificationDao.createVerificationSubmission(verificationSubmission);
	}

	@Override
	public VerificationPagedResults listVerificationSubmissions(
			UserInfo userInfo, List<VerificationStateEnum> currentVerificationState,
			Long verifiedUserId, long limit, long offset) {
		// TODO check that user is in ACT (or an admin)
		List<VerificationSubmission>  list = verificationDao.listVerificationSubmissions(currentVerificationState, verifiedUserId, limit, offset);
		long totalNumberOfResults = verificationDao.countVerificationSubmissions(currentVerificationState, verifiedUserId);
		VerificationPagedResults result = new VerificationPagedResults();
		result.setResults(list);
		result.setTotalNumberOfResults(totalNumberOfResults);
		return result;
	}

	@Override
	public void changeSubmissionState(UserInfo userInfo,
			long verificationSubmissionId, VerificationState newState) {
		// TODO check that user is in ACT (or an admin)
		// TODO check that the state transition is allowed, by comparing to the current state
		verificationDao.appendVerificationSubmissionState(verificationSubmissionId, newState);
	}

	@Override
	public String getDownloadURL(UserInfo userInfo, long verificationSubmissionId, long fileHandleId) {
		// TODO check whether user is the owner of the verification or in the ACT
		if (!verificationDao.isFileHandleIdInVerificationSubmission(verificationSubmissionId, fileHandleId))
			throw new UnauthorizedException("You are not allowed to download this file.");

		return fileHandleManager.getRedirectURLForFileHandle((new Long(fileHandleId)).toString());
	}

}
