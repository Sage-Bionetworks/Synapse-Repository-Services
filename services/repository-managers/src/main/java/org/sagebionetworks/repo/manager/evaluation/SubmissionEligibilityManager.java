package org.sagebionetworks.repo.manager.evaluation;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.TeamSubmissionEligibility;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.web.NotFoundException;

public interface SubmissionEligibilityManager {
	
	/*
	 * Compute the submission eligibility information for the given evaluation and team
	 */
	public TeamSubmissionEligibility getTeamSubmissionEligibility(Evaluation evaluation, String teamId, Date now) throws DatastoreException, NumberFormatException, NotFoundException;
	
	/*
	 * Determine whether a Team and its members are authorized to submit to 
	 * the given evaluation.
	 */
	AuthorizationStatus isTeamEligible(String evalId, String teamId,
			List<String> contributors, String submissionEligibilityHashString, Date now)
			throws DatastoreException, NotFoundException;
	/*
	 * Determine whether an individual is authorized to submit to the given evaluation
	 */
	public AuthorizationStatus isIndividualEligible(String evalId, UserInfo userInfo, Date now) throws DatastoreException, NotFoundException;

	
}
