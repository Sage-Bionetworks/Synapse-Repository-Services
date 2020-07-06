package org.sagebionetworks.repo.manager.sqs;

import org.sagebionetworks.repo.model.message.SQSSendMessageRequest;
import org.sagebionetworks.repo.model.message.SQSSendMessageResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;

/**
 * Entrypoint for operations in SQS
 * 
 * @author Marco Marasca
 */
public interface SQSManager {

	/**
	 * Sends the SQS message specified in the given {@link SQSSendMessageRequest}
	 * 
	 * @param messageRequest Container for the message to send
	 * @return An object with information about the sent message (e.g. message id)
	 * @throws NotFoundException           If the queue referred by the message does
	 *                                     not exist
	 * @throws ServiceUnavailableException If an exception occurs contacting SQS
	 */
	SQSSendMessageResponse sendMessage(SQSSendMessageRequest messageRequest) throws NotFoundException, TemporarilyUnavailableException;

}
