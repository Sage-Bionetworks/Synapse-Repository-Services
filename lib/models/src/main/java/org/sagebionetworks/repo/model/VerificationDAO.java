package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

public interface VerificationDAO {
	
	/**
	 * create new object
	 * @param dto
	 * @return
	 */
	public VerificationSubmission createVerificationSubmission(VerificationSubmission dto);
	
	/**
	 * 
	 * @param states the results are limited to verification submissions in any of the given states (optional)
	 * @param userId the results are limited to verification submissions for the given userId (optional)
	 * @param limit required
	 * @param offset required
	 * @return
	 */
	public List<VerificationSubmission> listVerificationSubmissions(List<VerificationStateEnum> states, Long userId, long limit, long offset);
	
	/**
	 * 
	 * @param states the results are limited to verification submissions in any of the given states (optional)
	 * @param userId the results are limited to verification submissions for the given userId (optional)
	 * @return
	 */
	public long countVerificationSubmissions(List<VerificationStateEnum> states, Long userId);
	
	/**
	 * delete object given its ID
	 * @param id
	 */
	public void deleteVerificationSubmission(String id);
	
	/**
	 * append a new state object to the given submission's state history, updating its state
	 * 
	 * @param verificationSubmissionId
	 * @param newState
	 */
	public void appendVerificationSubmissionState(long verificationSubmissionId, VerificationState newState);
	

	/**
	 * check whether a file handle ID is in a verification submission
	 * this is used in authorization checks
	 * 
	 * @param id
	 * @param fileHandleId
	 * @return
	 */
	public boolean isFileHandleIdInVerificationSubmission(long id, long fileHandleId);
}
