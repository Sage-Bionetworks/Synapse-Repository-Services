package org.sagebionetworks.asynchronous.workers.changes;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.progress.ProgressCallback;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;

/**
 * A message driven runner that can read a batch of change messages and forwared
 * each message to the provided worker.
 * 
 * @author jhill
 *
 */
public class ChangeMessageBatchProcessor implements MessageDrivenRunner {

	static private Logger log = LogManager
			.getLogger(ChangeMessageBatchProcessor.class);

	private AmazonSQSClient awsSQSClient;
	private String queueUrl;
	private ChangeMessageDrivenRunner runner;

	public ChangeMessageBatchProcessor(AmazonSQSClient awsSQSClient,
			String queueName, ChangeMessageDrivenRunner runner) {
		this.awsSQSClient = awsSQSClient;
		this.queueUrl = awsSQSClient.getQueueUrl(queueName).getQueueUrl();
		this.runner = runner;
	}

	@Override
	public void run(final ProgressCallback<Message> progressCallback,
			final Message message) throws RecoverableMessageException,
			Exception {
		// read the batch.
		List<ChangeMessage> batch = MessageUtils
				.extractChangeMessageBatch(message);
		// Run each batch
		for (ChangeMessage change : batch) {
			try {
				// Make progress before each message
				progressCallback.progressMade(message);
				runner.run(new ProgressCallback<ChangeMessage>() {
					@Override
					public void progressMade(ChangeMessage t) {
						progressCallback.progressMade(message);
					}
				}, change);
			} catch (RecoverableMessageException e) {
				if (batch.size() == 1) {
					// Let the container handle retry for single messages.
					throw e;
				} else {
					// Add the message back to the queue as a single message
					awsSQSClient.sendMessage(queueUrl,
							EntityFactory.createJSONStringForEntity(change));
				}
			} catch (Throwable e) {
				log.error("Failed on Change Number: " + change.getChangeNumber(),
						e);
			}
		}
	}

}
