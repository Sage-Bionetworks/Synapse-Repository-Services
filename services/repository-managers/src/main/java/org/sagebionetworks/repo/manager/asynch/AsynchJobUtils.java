package org.sagebionetworks.repo.manager.asynch;

import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.util.ValidateArgument;

public class AsynchJobUtils {

	/**
	 * Helper to extract a job body from a request with all of the error checking.
	 * 
	 * @param status
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T extends AsynchronousRequestBody> T extractRequestBody(
			AsynchronousJobStatus status, Class<T> clazz) {
		ValidateArgument.required(status, "status");
		ValidateArgument.required(clazz, "class");
		ValidateArgument.required(status.getRequestBody(), "status.requestBody()");
		if(!clazz.isInstance(status.getRequestBody())){
			throw new IllegalArgumentException("Expected a job body of type: " + clazz.getName() + " but received: "
					+ status.getRequestBody().getClass().getName());
		}
		return (T)status.getRequestBody();
	}
	
	/**
	 * If the passed job has failed, extract and throw an exception for the failure.
	 * 
	 * @param jobStatus
	 * @return
	 * @throws Throwable 
	 */
	public static void throwExceptionIfFailed(AsynchronousJobStatus jobStatus) throws Throwable {
		if (jobStatus.getJobState() == AsynchJobState.FAILED) {
			if (jobStatus.getException() != null) {
				Throwable exception = null;
				try {
					@SuppressWarnings("unchecked")
					Class<Throwable> exceptionClass = (Class<Throwable>) Class.forName(jobStatus.getException());
					exception = exceptionClass.getConstructor(String.class).newInstance(jobStatus.getErrorMessage());
				} catch (Throwable t) {
					// ignore, just throw async job failed exception on any failure in trying to get a better exception
					// here
				}
				if (exception != null) {
					throw exception;
				}
			}
			throw new AsynchJobFailedException(jobStatus);
		}
	}
}
