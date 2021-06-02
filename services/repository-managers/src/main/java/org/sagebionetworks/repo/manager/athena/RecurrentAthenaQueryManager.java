package org.sagebionetworks.repo.manager.athena;

import org.sagebionetworks.repo.model.athena.RecurrentAthenaQueryResult;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

/**
 * Manager used to process results from Athena queries that are run from a step function defined in
 * the stack builder
 */
public interface RecurrentAthenaQueryManager {

	/**
	 * @param message The SQS message representing a recurrent Athena query result
	 * @return The de-serialized {@link RecurrentAthenaQueryResult} from the given SQS message
	 */
	RecurrentAthenaQueryResult fromSqsMessage(Message message);

	/**
	 * Process the given {@link RecurrentAthenaQueryResult} using the appropriate processor. The
	 * processor will only consume a subset of result pages and will use the given queue URL to send
	 * additional pages for processing in subsequent requests.
	 * 
	 * @param request  The request to process
	 * @param queueUrl The url of the queue that received the request
	 * @throws RecoverableMessageException If the processing failed but can be retried again
	 */
	void processRecurrentAthenaQueryResult(RecurrentAthenaQueryResult request, String queueUrl) throws RecoverableMessageException;

}
