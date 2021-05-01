package org.sagebionetworks.athena.workers;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.athena.RecurrentAthenaQueryManager;
import org.sagebionetworks.repo.model.athena.RecurrentAthenaQueryResult;
import org.sagebionetworks.repo.model.exception.RecoverableException;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;

/**
 * The worker process the SQS messages coming from the step functions that execute some athena query
 */
public class RecurrentAthenaQueryWorker implements MessageDrivenRunner {

	private static final Logger LOG = LogManager.getLogger(RecurrentAthenaQueryWorker.class);

	private RecurrentAthenaQueryManager manager;
	private AmazonSQSClient sqsClient;
	private String queueName;
	private String queueUrl;
	
	@Autowired
	public RecurrentAthenaQueryWorker(RecurrentAthenaQueryManager manager, AmazonSQSClient sqsClient) {
		this.manager = manager;
		this.sqsClient = sqsClient;
	}
	
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}
	
	@PostConstruct
	public void configure() {
		this.queueUrl = sqsClient.getQueueUrl(queueName).getQueueUrl();
	}
	
	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		RecurrentAthenaQueryResult result = manager.fromSqsMessage(message);
		
		try {
			manager.processRecurrentAthenaQueryResult(result, queueUrl);
		} catch (RecoverableException e) {
			LOG.warn(e.getMessage(), e);
			throw new RecoverableMessageException(e.getMessage(), e);
		}
	}

}
