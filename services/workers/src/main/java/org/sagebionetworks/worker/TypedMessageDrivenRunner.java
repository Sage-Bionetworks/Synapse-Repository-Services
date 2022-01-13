package org.sagebionetworks.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

/**
 * Runner driven by an SQS message converted to the provided type
 * 
 * @param <T>
 */
public interface TypedMessageDrivenRunner<T> {

	/**
	 * @return The class of the object to convert
	 */
	Class<T> getObjectClass();
	
	/**
	 * This method is invoked after the body in the SQS message is converted into an object
	 *
	 * @param progressCallback
	 * @param message The original SQS message body
	 * @param convertedMessage The converted body of the SQS message
	 * @throws RecoverableMessageException
	 * @throws Exception
	 */
	void run(ProgressCallback progressCallback, Message message, T convertedMessage) throws RecoverableMessageException, Exception;
	
}
