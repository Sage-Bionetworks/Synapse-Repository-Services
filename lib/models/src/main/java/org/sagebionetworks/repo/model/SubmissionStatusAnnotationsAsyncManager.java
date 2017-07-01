package org.sagebionetworks.repo.model;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * This is the manager that "sync's up" the submission status records with the Annotations query table
 * 
 *
 */
public interface SubmissionStatusAnnotationsAsyncManager {
	
	/**
	 * Update the query tables with any Annotations included in the created Evaluation's SubmissionStatuses.
	 * 
	 * @param evalId
	 * @param submissionsEtag
	 * 
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws JSONObjectAdapterException
	 */
	public void createEvaluationSubmissionStatuses(String evalId, String submissionsEtag) 
			throws NotFoundException, DatastoreException, JSONObjectAdapterException;

	/**
	 * Update the query tables with any Annotations included in the updated Evaluation's SubmissionStatuses.
	 * 
	 * @param evalId
	 * @param submissionsEtag
	 * @return
	 * @throws NotFoundException
	 * @throws JSONObjectAdapterException 
	 * @throws DatastoreException 
	 */
	public void updateEvaluationSubmissionStatuses(String evalId, String submissionsEtag) 
			throws NotFoundException, DatastoreException, JSONObjectAdapterException;

	/**
	 * Delete all entries in the query tables for the deleted Evaluation.
	 * 
	 * @param evalId
	 * @param submissionsEtag
	 * @return
	 */
	public void deleteEvaluationSubmissionStatuses(String evalId, String submissionsEtag);

}
