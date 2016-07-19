package org.sagebionetworks.repo.manager.asynch;

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
}
