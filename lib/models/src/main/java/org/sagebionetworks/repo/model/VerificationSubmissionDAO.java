package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.model.verification.VerificationApproval;
import org.sagebionetworks.repo.model.verification.VerificationRetraction;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

public interface VerificationSubmissionDAO {
	public VerificationSubmission createVerificationSubmission(VerificationSubmission dto);
	
	public VerificationSubmission getVerificationSubmission(String id);
	
	public void deleteVerificationSubmission(String id);
	
	public List<VerificationSubmission> listVerificationSubmissions(long limit, long offset);
	
	public long countVerificationSubmissions();
	
	public void approveVerificationSubmission(String id, VerificationApproval approval);
	
	public void retractVerificationSubmission(String id, VerificationRetraction retraction);

	public boolean isFileHandleIdInVerificationSubmission(String id, String fileHandleId);
}
