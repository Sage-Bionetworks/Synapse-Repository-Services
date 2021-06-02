package org.sagebionetworks.repo.manager.athena;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.athena.AthenaQueryExecution;
import org.sagebionetworks.repo.model.athena.AthenaQueryExecutionState;
import org.sagebionetworks.repo.model.athena.AthenaQueryResultPage;
import org.sagebionetworks.repo.model.athena.AthenaSupport;
import org.sagebionetworks.repo.model.athena.RecurrentAthenaQueryResult;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RecurrentAthenaQueryManagerImpl implements RecurrentAthenaQueryManager {
	
	static final int MAX_QUERY_RESULTS = 1000;
	static final int MAX_PAGE_REQUESTS = 10;

	private Map<String, RecurrentAthenaQueryProcessor<?>> processorMap;
	
	private ObjectMapper objectMapper;
	private AthenaSupport athenaSupport;
	private AmazonSQS sqsClient;
	
	@Autowired
	public RecurrentAthenaQueryManagerImpl(ObjectMapper objectMapper, AthenaSupport athenaSupport, AmazonSQS sqsClient) {
		this.objectMapper = objectMapper;
		this.athenaSupport = athenaSupport;
		this.sqsClient = sqsClient;
	}
	
	@Autowired
	public void configureProcessorMap(List<RecurrentAthenaQueryProcessor<?>> processors) {
		
		processorMap = new HashMap<>(processors.size());
		
		processors.forEach(p -> {
			if (processorMap.put(p.getQueryName(), p) != null) {
				throw new IllegalStateException("Duplicate query processor for queryName " + p.getQueryName());
			};
		});
		
	}
	
	@Override
	public RecurrentAthenaQueryResult fromSqsMessage(Message message) {
		RecurrentAthenaQueryResult request;

		try {
			request = objectMapper.readValue(message.getBody(), RecurrentAthenaQueryResult.class);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Could not parse RecurrentAthenaQueryResult from message: " + e.getMessage(), e);
		}

		return request;
	}
	
	String toJsonMessage(RecurrentAthenaQueryResult request) {
		String messageBody;
		
		try {
			messageBody = objectMapper.writeValueAsString(request);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Could not serialize RecurrentAthenaQueryResult message: " + e.getMessage(), e);
		}
		
		return messageBody;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void processRecurrentAthenaQueryResult(RecurrentAthenaQueryResult request, String queueUrl) throws RecoverableMessageException {
		ValidateArgument.requiredNotBlank(request.getQueryName(), "The queryName");
		ValidateArgument.requiredNotBlank(request.getFunctionExecutionId(), "The functionExecutionId");
		ValidateArgument.required(request.getQueryExecutionId(), "The queryExecutionId");
		ValidateArgument.requiredNotBlank(queueUrl, "The queueUrl");
		
		RecurrentAthenaQueryProcessor<?> processor = processorMap.get(request.getQueryName());
		
		if (processor == null) {
			throw new IllegalArgumentException(String.format("Unsupported query: %s.", request.getQueryName()));
		}

		final String queryExecutionId = request.getQueryExecutionId();

		AthenaQueryExecution execution = athenaSupport.getQueryExecutionStatus(queryExecutionId);

		if (AthenaQueryExecutionState.QUEUED.equals(execution.getState()) || AthenaQueryExecutionState.RUNNING.equals(execution.getState())) {
			throw new RecoverableMessageException("The query with id " + queryExecutionId + " is still processing.");
		}

		if (!AthenaQueryExecutionState.SUCCEEDED.equals(execution.getState())) {
			throw new IllegalStateException("The query with id " + queryExecutionId + " did not SUCCEED (State: " + execution.getState() + ", reason: " + execution.getStateChangeReason() + ")");
		}
		
		// We limit the processing of the results to a finite set of pages: this allows the worker to perform a limited set of work if retrying is necessary or if the message is put
		// back on the queue (e.g. when a worker dies) without starting from scratch, if more results are available subsequent SQS messages are sent to the same queue so that the 
		// worker can keep processing the remaining results.
		int pageRequests = 0;
		
		// The initial page token is null since it's coming from the step function that does not include any (e.g. first page)
		String currentPageToken = request.getPageToken();
		
		do {
		
			AthenaQueryResultPage page = athenaSupport.getQueryResultsPage(queryExecutionId, processor.getRowMapper(), currentPageToken, MAX_QUERY_RESULTS);
			
			if (page.getResults() == null || page.getResults().isEmpty()) {
				return;
			}
			
			processor.processQueryResultsPage(page.getResults());
			
			currentPageToken = page.getNextPageToken();
			pageRequests++;
		
		} while (currentPageToken != null && pageRequests < MAX_PAGE_REQUESTS);

		// If there are more results to process we send a message on the same queue, this reduce the amount of time a worker spends processing
		if (currentPageToken != null) {
			sendNextPageMessage(queueUrl, request, currentPageToken);
		}
		
	}
	
	private void sendNextPageMessage(String queueUrl, final RecurrentAthenaQueryResult request, final String nextPageToken) {
		RecurrentAthenaQueryResult nextRequest = new RecurrentAthenaQueryResult()
				.withQueryName(request.getQueryName())
				.withFunctionExecutionId(request.getFunctionExecutionId())
				.withQueryExecutionId(request.getQueryExecutionId())
				.withPageToken(nextPageToken);
		
		String messageBody = toJsonMessage(nextRequest);
		
		SendMessageRequest sendMessageRequest = new SendMessageRequest()
				.withQueueUrl(queueUrl)
				.withMessageBody(messageBody);
				
		sqsClient.sendMessage(sendMessageRequest);
	}

}
