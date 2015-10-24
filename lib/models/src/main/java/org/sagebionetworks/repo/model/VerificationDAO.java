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
	 * @param newState optional
	 * @param userId optional
	 * @param limit required
	 * @param offset required
	 * @return
	 */
	public List<VerificationSubmission> listVerificationSubmissions(VerificationStateEnum state, Long userId, long limit, long offset);
	
	/**
	 * 
	 * @param state optional
	 * @param userId optional
	 * @return
	 */
	public long countVerificationSubmissions(VerificationStateEnum state, Long userId);
	
	/**
	 * delete object given its ID
	 * @param id
	 */
	public void deleteVerificationSubmission(String id);
	
	/**
	 * append a new state object to the given submission's state history, updating its state
	 * 
	 * @param newState
	 */
	public void appendVerificationSubmissionState(VerificationState newState);

	/**
	 * check whether a file handle ID is in a verification submission
	 * this is used in authorization checks
	 * 
	 * @param id
	 * @param fileHandleId
	 * @return
	 */
	public boolean isFileHandleIdInVerificationSubmission(String id, String fileHandleId);
}
