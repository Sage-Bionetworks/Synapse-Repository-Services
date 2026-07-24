package org.sagebionetworks.repo.manager.athena;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.athena.AthenaQueryExecutionAdapter;
import org.sagebionetworks.repo.model.athena.AthenaQueryResultPage;
import org.sagebionetworks.repo.model.athena.AthenaSupport;
import org.sagebionetworks.repo.model.athena.RecurrentAthenaQueryResult;
import org.sagebionetworks.repo.model.athena.RowMapper;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import software.amazon.awssdk.services.athena.model.QueryExecution;
import software.amazon.awssdk.services.athena.model.QueryExecutionState;
import software.amazon.awssdk.services.athena.model.QueryExecutionStatus;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class RecurrentAthenaQueryManagerTest {
	
	@Mock
	private ObjectMapper mockObjectMapper;
	@Mock
	private AthenaSupport mockAthenaSupport;
	@Mock
	private AmazonSQS mockSqsClient;
	@Mock
	private StackConfiguration mockConfig;
	
	@InjectMocks
	private RecurrentAthenaQueryManagerImpl manager;
	
	@Mock
	private RecurrentAthenaQueryProcessor<Long> mockProcessor;
	
	@Mock
	private RowMapper<Long> mockRowMapper;
	
	@Mock
	private Message mockMessage;
	
	private QueryExecution mockQueryExecution;
	private RecurrentAthenaQueryResult mockRequest;

	private String queueUrl = "queueUrl";

	@BeforeEach
	public void beforeEach() {
		when(mockProcessor.getQueryName()).thenReturn("SomeQueryName");
		
		manager.configureProcessorMap(Arrays.asList(mockProcessor));
		
		mockQueryExecution = QueryExecution.builder().queryExecutionId("456").status(QueryExecutionStatus.builder().state(QueryExecutionState.SUCCEEDED).build()).build();
		
		mockRequest = new RecurrentAthenaQueryResult()
				.withQueryName("SomeQueryName")
				.withFunctionExecutionId("123")
				.withQueryExecutionId("456");
	}
	
	@Test
	public void testProcessRecurrentAthenaQueryResultWithNoQueryUrl() {
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processRecurrentAthenaQueryResult(mockRequest, null);
		}).getMessage();
		
		assertEquals("The queueUrl is required and must not be the empty string.", message);
	}
	
	@Test
	public void testProcessRecurrentAthenaQueryResultWithNoQueryName() {
		
		mockRequest.withQueryName(null);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processRecurrentAthenaQueryResult(mockRequest, queueUrl);
		}).getMessage();
		
		assertEquals("The queryName is required and must not be the empty string.", message);
	}
	
	@Test
	public void testProcessRecurrentAthenaQueryResultWithWrongQueryName() {
		
		mockRequest.withQueryName("SomeOtherQuery");
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processRecurrentAthenaQueryResult(mockRequest, queueUrl);
		}).getMessage();
		
		assertEquals("Unsupported query: SomeOtherQuery.", message);
	}
	
	@Test
	public void testProcessRecurrentAthenaQueryResultWithNoQueryExecutionId() {
		
		mockRequest.withQueryExecutionId(null);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processRecurrentAthenaQueryResult(mockRequest, queueUrl);
		}).getMessage();
		
		assertEquals("The queryExecutionId is required.", message);
	}
	
	@Test
	public void testProcessRecurrentAthenaQueryResultWithNoFunctionExecutionId() {
		
		mockRequest.withFunctionExecutionId(null);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processRecurrentAthenaQueryResult(mockRequest, queueUrl);
		}).getMessage();
		
		assertEquals("The functionExecutionId is required and must not be the empty string.", message);
	}
		
	@Test
	public void testProcessRecurrentAthenaQueryResultWithQueryQueued() {

		mockQueryExecution = mockQueryExecution.toBuilder().status(QueryExecutionStatus.builder().state(QueryExecutionState.QUEUED).build()).build();

		when(mockAthenaSupport.getQueryExecutionStatus(anyString())).thenReturn(new AthenaQueryExecutionAdapter(mockQueryExecution));
		
		String message = assertThrows(RecoverableMessageException.class, () -> {
			// Call under test
			manager.processRecurrentAthenaQueryResult(mockRequest, queueUrl);
		}).getMessage();
		
		assertEquals("The query with id " + mockQueryExecution.queryExecutionId() + " is still processing.", message);
	}
	
	@Test
	public void testProcessRecurrentAthenaQueryResultWithQueryRunning() {

		mockQueryExecution = mockQueryExecution.toBuilder().status(QueryExecutionStatus.builder().state(QueryExecutionState.RUNNING).build()).build();

		when(mockAthenaSupport.getQueryExecutionStatus(anyString())).thenReturn(new AthenaQueryExecutionAdapter(mockQueryExecution));
		
		String message = assertThrows(RecoverableMessageException.class, () -> {
			// Call under test
			manager.processRecurrentAthenaQueryResult(mockRequest, queueUrl);
		}).getMessage();
		
		assertEquals("The query with id " + mockQueryExecution.queryExecutionId() + " is still processing.", message);
	}
	
	@Test
	public void testProcessRecurrentAthenaQueryResultWithQueryNotSucceded() {
		
		int count = 0;
		
		for (QueryExecutionState state : QueryExecutionState.knownValues()) {
			
			if (state == QueryExecutionState.RUNNING || state == QueryExecutionState.QUEUED || state == QueryExecutionState.SUCCEEDED) {
				continue;
			}
			
			mockQueryExecution = mockQueryExecution.toBuilder().status(QueryExecutionStatus.builder().state(state).stateChangeReason("some reason").build()).build();

			when(mockAthenaSupport.getQueryExecutionStatus(anyString())).thenReturn(new AthenaQueryExecutionAdapter(mockQueryExecution));
			
			String message = assertThrows(IllegalStateException.class, () -> {
				// Call under test
				manager.processRecurrentAthenaQueryResult(mockRequest, queueUrl);
			}).getMessage();
			
			assertEquals("The query with id " +  mockQueryExecution.queryExecutionId() + " did not SUCCEED (State: " + state + ", reason: some reason)", message);
			
			verify(mockAthenaSupport, times(++count)).getQueryExecutionStatus(mockQueryExecution.queryExecutionId());
		}
		
	}

	@Test
	public void testProcessRecurrentAthenaQueryResultWithQueryExecStateUnknown() {
		QueryExecutionState unknownState = QueryExecutionState.UNKNOWN_TO_SDK_VERSION;
		mockQueryExecution = mockQueryExecution.toBuilder().status(QueryExecutionStatus.builder().state(unknownState).stateChangeReason("some reason").build()).build();

		when(mockAthenaSupport.getQueryExecutionStatus(anyString())).thenReturn(new AthenaQueryExecutionAdapter(mockQueryExecution));

		assertThrows(IllegalArgumentException.class, () -> { manager.processRecurrentAthenaQueryResult(mockRequest, queueUrl); });

	}
	
	@Test
	public void testProcessRecurrentAthenaQueryResultWithQuerySucceeded() throws RecoverableMessageException {
		
		AthenaQueryResultPage<Long> page = new AthenaQueryResultPage<Long>()
				.withResults(Arrays.asList(1L, 2L, 3L))
				.withQueryExecutionId(mockQueryExecution.queryExecutionId())
				.withNextPageToken(null);
		
		when(mockProcessor.getRowMapper()).thenReturn(mockRowMapper);
		when(mockAthenaSupport.getQueryExecutionStatus(anyString())).thenReturn(new AthenaQueryExecutionAdapter(mockQueryExecution));	
		when(mockAthenaSupport.getQueryResultsPage(any(), any(RowMapper.class), any(), anyInt())).thenReturn(page);
		
		// Call under test
		manager.processRecurrentAthenaQueryResult(mockRequest, queueUrl);
		
		verify(mockAthenaSupport).getQueryExecutionStatus(mockQueryExecution.queryExecutionId());
		verify(mockAthenaSupport).getQueryResultsPage(mockQueryExecution.queryExecutionId(), mockRowMapper, null, RecurrentAthenaQueryManagerImpl.MAX_QUERY_RESULTS);
		verify(mockProcessor).processQueryResultsPage(page.getResults());
		verify(mockSqsClient, never()).sendMessage(any());
	}
	
	@Test
	public void testProcessRecurrentAthenaQueryResultWithEmptyResults() throws RecoverableMessageException {
		
		AthenaQueryResultPage<Long> page = new AthenaQueryResultPage<Long>()
				.withResults(Collections.emptyList())
				.withQueryExecutionId(mockQueryExecution.queryExecutionId())
				.withNextPageToken(null);
		
		when(mockProcessor.getRowMapper()).thenReturn(mockRowMapper);
		when(mockAthenaSupport.getQueryExecutionStatus(anyString())).thenReturn(new AthenaQueryExecutionAdapter(mockQueryExecution));	
		when(mockAthenaSupport.getQueryResultsPage(any(), any(RowMapper.class), any(), anyInt())).thenReturn(page);
		
		// Call under test
		manager.processRecurrentAthenaQueryResult(mockRequest, queueUrl);
		
		verify(mockAthenaSupport).getQueryExecutionStatus(mockQueryExecution.queryExecutionId());
		verify(mockAthenaSupport).getQueryResultsPage(mockQueryExecution.queryExecutionId(), mockRowMapper, null, RecurrentAthenaQueryManagerImpl.MAX_QUERY_RESULTS);
		verifyNoMoreInteractions(mockProcessor);
		verify(mockSqsClient, never()).sendMessage(any());
	}
	
	@Test
	public void testProcessRecurrentAthenaQueryResultWithNoResults() throws RecoverableMessageException {
		
		AthenaQueryResultPage<Long> page = new AthenaQueryResultPage<Long>()
				.withResults(null)
				.withQueryExecutionId(mockQueryExecution.queryExecutionId())
				.withNextPageToken(null);
		
		when(mockProcessor.getRowMapper()).thenReturn(mockRowMapper);
		when(mockAthenaSupport.getQueryExecutionStatus(anyString())).thenReturn(new AthenaQueryExecutionAdapter(mockQueryExecution));	
		when(mockAthenaSupport.getQueryResultsPage(any(), any(RowMapper.class), any(), anyInt())).thenReturn(page);
		
		// Call under test
		manager.processRecurrentAthenaQueryResult(mockRequest, queueUrl);
		
		verify(mockAthenaSupport).getQueryExecutionStatus(mockQueryExecution.queryExecutionId());
		verify(mockAthenaSupport).getQueryResultsPage(mockQueryExecution.queryExecutionId(), mockRowMapper, null, RecurrentAthenaQueryManagerImpl.MAX_QUERY_RESULTS);
		verifyNoMoreInteractions(mockProcessor);
		verify(mockSqsClient, never()).sendMessage(any());
	}
	
	@Test
	public void testProcessRecurrentAthenaQueryResultWithNextPage() throws RecoverableMessageException {
		
		AthenaQueryResultPage<Long> page1 = new AthenaQueryResultPage<Long>()
				.withResults(Arrays.asList(1L, 2L, 3L))
				.withQueryExecutionId(mockQueryExecution.queryExecutionId())
				.withNextPageToken("next");
		
		AthenaQueryResultPage<Long> page2 = new AthenaQueryResultPage<Long>()
				.withResults(Arrays.asList(4L, 5L))
				.withQueryExecutionId(mockQueryExecution.queryExecutionId())
				.withNextPageToken(null);
		
		when(mockProcessor.getRowMapper()).thenReturn(mockRowMapper);
		when(mockAthenaSupport.getQueryExecutionStatus(anyString())).thenReturn(new AthenaQueryExecutionAdapter(mockQueryExecution));	
		when(mockAthenaSupport.getQueryResultsPage(any(), any(RowMapper.class), any(), anyInt())).thenReturn(page1, page2);
		
		// Call under test
		manager.processRecurrentAthenaQueryResult(mockRequest, queueUrl);
		
		verify(mockAthenaSupport).getQueryExecutionStatus(mockQueryExecution.queryExecutionId());
		verify(mockAthenaSupport).getQueryResultsPage(mockQueryExecution.queryExecutionId(), mockRowMapper, null, RecurrentAthenaQueryManagerImpl.MAX_QUERY_RESULTS);
		verify(mockAthenaSupport).getQueryResultsPage(mockQueryExecution.queryExecutionId(), mockRowMapper, page1.getNextPageToken(), RecurrentAthenaQueryManagerImpl.MAX_QUERY_RESULTS);
		verify(mockProcessor).processQueryResultsPage(page1.getResults());
		verify(mockProcessor).processQueryResultsPage(page2.getResults());
		verify(mockSqsClient, never()).sendMessage(any());
	}
	
	@Test
	public void testProcessRecurrentAthenaQueryResultWithExceedProcessingLimit() throws JsonProcessingException, RecoverableMessageException {
		
		String nextToken = "next"; 
				
		AthenaQueryResultPage<Long> page1 = new AthenaQueryResultPage<Long>()
				.withResults(Arrays.asList(1L, 2L, 3L))
				.withQueryExecutionId(mockQueryExecution.queryExecutionId())
				.withNextPageToken(nextToken);
		
		List<AthenaQueryResultPage<Long>> nextPages = new ArrayList<>();		
		
		for (int i=0; i< RecurrentAthenaQueryManagerImpl.MAX_PAGE_REQUESTS; i++) {
			AthenaQueryResultPage<Long> next = new AthenaQueryResultPage<Long>()
				.withResults(Arrays.asList(4L, 5L))
				.withQueryExecutionId(mockQueryExecution.queryExecutionId())
				.withNextPageToken(nextToken);
			nextPages.add(next);
		}
		
		when(mockProcessor.getRowMapper()).thenReturn(mockRowMapper);
		when(mockAthenaSupport.getQueryExecutionStatus(anyString())).thenReturn(new AthenaQueryExecutionAdapter(mockQueryExecution));	
		when(mockAthenaSupport.getQueryResultsPage(any(), any(RowMapper.class), any(), anyInt())).thenReturn(page1, nextPages.toArray(new AthenaQueryResultPage[nextPages.size()]));
		when(mockObjectMapper.writeValueAsString(any())).thenReturn("sqsMessage");
		
		// Call under test
		manager.processRecurrentAthenaQueryResult(mockRequest, queueUrl);
		
		verify(mockAthenaSupport).getQueryExecutionStatus(mockQueryExecution.queryExecutionId());
		
		verify(mockAthenaSupport).getQueryResultsPage(mockQueryExecution.queryExecutionId(), mockRowMapper, null, RecurrentAthenaQueryManagerImpl.MAX_QUERY_RESULTS);
		verify(mockAthenaSupport, times(RecurrentAthenaQueryManagerImpl.MAX_PAGE_REQUESTS - 1)).getQueryResultsPage(mockQueryExecution.queryExecutionId(), mockRowMapper, nextToken, RecurrentAthenaQueryManagerImpl.MAX_QUERY_RESULTS);
		
		verify(mockProcessor, times(RecurrentAthenaQueryManagerImpl.MAX_PAGE_REQUESTS)).processQueryResultsPage(anyList());
		
		verify(mockSqsClient).sendMessage(new SendMessageRequest()
				.withQueueUrl(queueUrl)
				.withMessageBody("sqsMessage")
		);

		RecurrentAthenaQueryResult expectedRequest = new RecurrentAthenaQueryResult()
				.withQueryName(mockRequest.getQueryName())
				.withFunctionExecutionId(mockRequest.getFunctionExecutionId())
				.withPageToken(nextToken)
				.withQueryExecutionId(mockRequest.getQueryExecutionId());
		
		verify(mockObjectMapper).writeValueAsString(expectedRequest);
	}

	
}
