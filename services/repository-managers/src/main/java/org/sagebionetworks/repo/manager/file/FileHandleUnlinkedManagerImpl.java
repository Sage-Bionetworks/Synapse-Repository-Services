package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.athena.AthenaQueryExecution;
import org.sagebionetworks.repo.model.athena.AthenaQueryExecutionState;
import org.sagebionetworks.repo.model.athena.AthenaQueryResultPage;
import org.sagebionetworks.repo.model.athena.AthenaSupport;
import org.sagebionetworks.repo.model.athena.RowMapper;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.FileHandleStatus;
import org.sagebionetworks.repo.model.exception.RecoverableException;
import org.sagebionetworks.repo.model.file.FileHandleUnlinkedRequest;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.athena.model.Row;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FileHandleUnlinkedManagerImpl implements FileHandleUnlinkedManager {

	static final int MAX_QUERY_RESULTS = 1000;
	static final int MAX_PAGE_REQUESTS = 10;

	static final String QUEUE_NAME = "UNLINKED_FILE_HANDLES";
	static final String QUERY_NAME = "UnlinkedFileHandles";
	static final RowMapper<Long> ROW_MAPPER = (Row row) -> {
		return Long.valueOf(row.getData().get(0).getVarCharValue());
	};

	private ObjectMapper objectMapper;
	private AthenaSupport athenaSupport;
	private FileHandleDao fileHandleDao;
	private AmazonSQS sqsClient;
	private String sqsQueueUrl;

	@Autowired
	public FileHandleUnlinkedManagerImpl(ObjectMapper objectMapper, AthenaSupport athenaSupport, FileHandleDao fileHandleDao, AmazonSQS sqsClient) {
		this.objectMapper = objectMapper;
		this.athenaSupport = athenaSupport;
		this.fileHandleDao = fileHandleDao;
		this.sqsClient = sqsClient;
	}

	@Autowired
	public void configureQueryUrl(StackConfiguration config) {
		this.sqsQueueUrl = sqsClient.getQueueUrl(config.getQueueName(QUEUE_NAME)).getQueueUrl();
	}

	@Override
	public FileHandleUnlinkedRequest fromSqsMessage(Message message) {
		FileHandleUnlinkedRequest request;

		try {
			request = objectMapper.readValue(message.getBody(), FileHandleUnlinkedRequest.class);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Could not parse FileHandleUnlinkedRequest from message: " + e.getMessage(), e);
		}

		return request;
	}
	
	String toJsonMessage(FileHandleUnlinkedRequest request) {
		String messageBody;
		
		try {
			messageBody = objectMapper.writeValueAsString(request);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Could not serialize FileHandleUnlinkedRequest message: " + e.getMessage(), e);
		}
		
		return messageBody;
	}
	
	@Override
	public void processFileHandleUnlinkRequest(FileHandleUnlinkedRequest request) {
		ValidateArgument.requiredNotBlank(request.getQueryName(), "The queryName");
		ValidateArgument.requiredNotBlank(request.getFunctionExecutionId(), "The functionExecutionId");
		ValidateArgument.required(request.getQueryExecution(), "The queryExecution");
		ValidateArgument.required(request.getQueryExecution().getQueryExecutionId(), "The queryExecution.queryExecutionId");
		ValidateArgument.requirement(QUERY_NAME.equals(request.getQueryName()), String.format("Unsupported query: was %s, expected %s.", request.getQueryName(), QUERY_NAME));

		final String queryExecutionId = request.getQueryExecution().getQueryExecutionId();

		AthenaQueryExecution execution = athenaSupport.getQueryExecutionStatus(queryExecutionId);

		if (AthenaQueryExecutionState.QUEUED.equals(execution.getState())
				|| AthenaQueryExecutionState.RUNNING.equals(execution.getState())) {
			throw new RecoverableException("The query with id " + queryExecutionId + " is still processing.");
		}

		if (!AthenaQueryExecutionState.SUCCEEDED.equals(execution.getState())) {
			throw new IllegalStateException("The query with id " + queryExecutionId + " did not SUCCEED (State: " + execution.getState() + ", reason: " + execution.getStateChangeReason() + ")");
		}
		
		int pageRequests = 0;
		
		// The initial page token is null since it's coming from the step function that does not include any (e.g. first page)
		String currentPageToken = request.getPageToken();
		
		do {
		
			AthenaQueryResultPage<Long> page = athenaSupport.getQueryResultsPage(queryExecutionId, ROW_MAPPER, currentPageToken, MAX_QUERY_RESULTS);
			
			if (page.getResults() == null || page.getResults().isEmpty()) {
				return;
			}
			
			fileHandleDao.updateBatchStatus(page.getResults(), FileHandleStatus.UNLINKED, FileHandleStatus.AVAILABLE);
			
			currentPageToken = page.getNextPageToken();
			pageRequests++;
		
		} while (currentPageToken != null && pageRequests < MAX_PAGE_REQUESTS);

		// If there are more results to process we send a message on the same queue, this reduce the amount of time a worker spends processing
		if (currentPageToken != null) {
			sendNextPageMessage(request, currentPageToken);
		}
		
	}
	
	private void sendNextPageMessage(final FileHandleUnlinkedRequest request, final String nextPageToken) {
		FileHandleUnlinkedRequest nextRequest = new FileHandleUnlinkedRequest()
				.withQueryName(request.getQueryName())
				.withFunctionExecutionId(request.getFunctionExecutionId())
				.withQueryExecution(request.getQueryExecution())
				.withPageToken(nextPageToken);
		
		String messageBody = toJsonMessage(nextRequest);
		
		SendMessageRequest sendMessageRequest = new SendMessageRequest()
				.withQueueUrl(sqsQueueUrl)
				.withMessageBody(messageBody);
				
		sqsClient.sendMessage(sendMessageRequest);
	}

}
