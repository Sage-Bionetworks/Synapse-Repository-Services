package org.sagebionetworks.worker;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

/**
 * Runner that is driven by an {@link AsynchronousRequestBody}.
 */
public interface AsyncJobRunner<RequestType extends AsynchronousRequestBody, ResponseType extends AsynchronousResponseBody> {
	
	/**
	 * @return The class of the request object
	 */
	Class<RequestType> getRequestType();
	
	/**
	 * @return The class of the response object
	 */
	Class<ResponseType> getResponseType();
	
	/**
	 * This is invoked after an SQS message pointing to a job id is converted to the original request. The implementing worker will need to return the relative response object.
	 * The response will be persisted in the job record and its status will be set to complete on success. On failure the corresponding job will be set as failed.  
	 * @param jobId The id of the asynchronous job
	 * @param user The user that started the job
	 * @param request The job request
	 * @param jobProgressCallback Callback that can be used to update the progress of the job
	 * 
	 * @return The job response
	 */
	ResponseType run(String jobId, UserInfo user, RequestType request, AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception;

}
