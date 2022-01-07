package org.sagebionetworks.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

/**
 * Runner driven by an SQS message converted to the provided type
 * 
 * @param <T>
 */
public interface TypedMessageDrivenRunner<T> {

	Class<T> getObjectClass();
	
	void run(ProgressCallback progressCallback, T message) throws RecoverableMessageException, Exception;
	
}
