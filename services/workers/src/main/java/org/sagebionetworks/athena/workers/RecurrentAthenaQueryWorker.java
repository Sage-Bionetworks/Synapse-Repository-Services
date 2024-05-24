package org.sagebionetworks.athena.workers;

import javax.annotation.PostConstruct;

import org.sagebionetworks.repo.manager.athena.RecurrentAthenaQueryManager;
import org.sagebionetworks.repo.model.athena.RecurrentAthenaQueryResult;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.worker.TypedMessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;

/**
 * The worker process the SQS messages coming from the step functions that execute some athena query
 */
public class RecurrentAthenaQueryWorker implements TypedMessageDrivenRunner<RecurrentAthenaQueryResult> {

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
	public Class<RecurrentAthenaQueryResult> getObjectClass() {
		return RecurrentAthenaQueryResult.class;
	}
	
	@Override
	public void run(ProgressCallback progressCallback, Message message, RecurrentAthenaQueryResult result) throws RecoverableMessageException {
		manager.processRecurrentAthenaQueryResult(result, queueUrl);
	}

}
