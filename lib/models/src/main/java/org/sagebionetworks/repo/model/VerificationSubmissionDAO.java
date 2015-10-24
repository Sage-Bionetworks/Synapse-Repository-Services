package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

public interface VerificationSubmissionDAO {
	public VerificationSubmission createVerificationSubmission(VerificationSubmission dto);
	
	public VerificationSubmission getVerificationSubmission(String id);
	
	public void deleteVerificationSubmission(String id);
	
	public List<VerificationSubmission> listVerificationSubmissions(long limit, long offset);
	
	public long countVerificationSubmissions();
	
	public void updateVerificationSubmissionState(String id, VerificationStateEnum newState);

	public boolean isFileHandleIdInVerificationSubmission(String id, String fileHandleId);
}
