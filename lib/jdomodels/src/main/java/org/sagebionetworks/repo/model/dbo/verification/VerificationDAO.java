package org.sagebionetworks.repo.model.dbo.verification;

import java.util.List;
import java.util.Set;

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
	 * Get the latest verification submission
	 * @param userId
	 * @return
	 */
	public VerificationSubmission getCurrentVerificationSubmissionForUser(long userId);
	
	/**
	 * 
	 * @param currentVerificationState the results are limited to verification submissions in any of the given states (optional)
	 * @param userId the results are limited to verification submissions for the given userId (optional)
	 * @param limit required
	 * @param offset required
	 * @return
	 */
	public List<VerificationSubmission> listVerificationSubmissions(List<VerificationStateEnum> currentVerificationState, Long userId, long limit, long offset);
	
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
	public void deleteVerificationSubmission(long verificationId);
	
	/**
	 * append a new state object to the given submission's state history, updating its state
	 * 
	 * @param verificationSubmissionId
	 * @param newState
	 */
	public void appendVerificationSubmissionState(long verificationId, VerificationState newState);
	
	/**
	 * 
	 * @param verificationSubmissionId
	 * @return
	 */
	public VerificationStateEnum getVerificationState(long verificationId);

	/**
	 * 
	 * @param verificationId
	 * @return the file handle IDs associated with the given verification submission
	 */
	public List<Long> listFileHandleIds(long verificationId);

	/**
	 * 
	 * @param verificationId
	 * @return the user who submitted the verification request
	 */
	public long getVerificationSubmitter(long verificationId);

	/**
	 * 
	 * @param userIds
	 * @return true if all users have validated profiles; false otherwise.
	 */
	public boolean haveValidatedProfiles(Set<String> userIds);
}
