package org.sagebionetworks.evaluation.dao;

import org.sagebionetworks.evaluation.model.EvaluationSubmissions;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;

public interface EvaluationSubmissionsDAO {
	/**
	 * 
	 * @param evalSub
	 * @return
	 * @throws DatastoreException
	 */
	public EvaluationSubmissions createForEvaluation(long evaluationId) throws DatastoreException;
	
	/**
	 * 
	 * @param evaluationId
	 * @return
	 */
	public EvaluationSubmissions getForEvaluationIfExists(long evaluationId);
	
	/**
	 * 
	 * @param evaluationId
	 * @return
	 * @throws NotFoundException
	 */
	public EvaluationSubmissions lockAndGetForEvaluation(long evaluationId) throws NotFoundException;
	
	/**
	 * lock the row for the given evaluation ID, change the etag and return the new etag value
	 * @param evaluationId
	 * @param sendChangeMessage
	 * @return the new etag
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public String updateEtagForEvaluation(long evaluationId, boolean sendChangeMessage, ChangeType changeType) throws DatastoreException, NotFoundException;
	
	/**
	 * 
	 * @param evaluationId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void deleteForEvaluation(long evaluationId) throws DatastoreException, NotFoundException;
}
