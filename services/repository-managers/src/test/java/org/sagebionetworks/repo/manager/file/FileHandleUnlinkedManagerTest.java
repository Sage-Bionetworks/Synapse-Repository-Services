package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
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
import org.sagebionetworks.repo.model.athena.RowMapper;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.FileHandleStatus;
import org.sagebionetworks.repo.model.exception.RecoverableException;
import org.sagebionetworks.repo.model.file.FileHandleUnlinkedRequest;

import com.amazonaws.services.athena.model.QueryExecution;
import com.amazonaws.services.athena.model.QueryExecutionState;
import com.amazonaws.services.athena.model.QueryExecutionStatus;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class FileHandleUnlinkedManagerTest {
	
	@Mock
	private ObjectMapper mockObjectMapper;
	@Mock
	private AthenaSupport mockAthenaSupport;
	@Mock
	private FileHandleDao mockFileHandleDao;
	@Mock
	private AmazonSQS mockSqsClient;
	@Mock
	private StackConfiguration mockConfig;
	
	@InjectMocks
	private FileHandleUnlinkedManagerImpl manager;
	
	@Mock
	private Message mockMessage;
	
	private QueryExecution mockQueryExecution;
	private FileHandleUnlinkedRequest mockRequest;

	private String queueName = "queue";
	private String queueUrl = "queueUrl";

	@BeforeEach
	public void beforeEach() {
		when(mockConfig.getQueueName(FileHandleUnlinkedManagerImpl.QUEUE_NAME)).thenReturn(queueName);
		when(mockSqsClient.getQueueUrl(queueName)).thenReturn(new GetQueueUrlResult().withQueueUrl(queueUrl));
		
		manager.configureQueryUrl(mockConfig);
		
		mockQueryExecution = new QueryExecution().withQueryExecutionId("456").withStatus(new QueryExecutionStatus().withState(QueryExecutionState.SUCCEEDED));
		mockRequest = new FileHandleUnlinkedRequest()
				.withQueryName("UnlinkedFileHandles")
				.withFunctionExecutionId("123")
				.withQueryExecution(mockQueryExecution);
	}
	
	@AfterEach
	public void after() {
		verify(mockConfig).getQueueName(FileHandleUnlinkedManagerImpl.QUEUE_NAME);
		verify(mockSqsClient).getQueueUrl(queueName);
	}

	@Test
	public void testFromSqsMessage() throws JsonMappingException, JsonProcessingException {
		
		String messageBody = "{ \"body\": \"value\"}";
		when(mockMessage.getBody()).thenReturn(messageBody);
		when(mockObjectMapper.readValue(anyString(), any(Class.class))).thenReturn(mockRequest);
		
		// Call under test
		manager.fromSqsMessage(mockMessage);
		
		verify(mockObjectMapper).readValue(messageBody, FileHandleUnlinkedRequest.class);
		
	}
	
	@Test
	public void testFromSqsMessageWithJsonProcessingException() throws JsonMappingException, JsonProcessingException {
		
		JsonProcessingException ex = new JsonParseException(null, "Some error");
		
		String messageBody = "{ \"body\": \"value\"}";
		
		when(mockMessage.getBody()).thenReturn(messageBody);
		
		doThrow(ex).when(mockObjectMapper).readValue(anyString(), any(Class.class));
		
		IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.fromSqsMessage(mockMessage);
		});
		
		assertEquals(ex, result.getCause());
		assertEquals("Could not parse FileHandleUnlinkedRequest from message: " + ex.getMessage(), result.getMessage());
		
	}
	
	@Test
	public void testProcessFileHandleUnlinkRequestWithNoQueryName() {
		
		mockRequest.withQueryName(null);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processFileHandleUnlinkRequest(mockRequest);
		}).getMessage();
		
		assertEquals("The queryName is required and must not be the empty string.", message);
	}
	
	@Test
	public void testProcessFileHandleUnlinkRequestWithWrongQueryName() {
		
		mockRequest.withQueryName("SomeOtherQuery");
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processFileHandleUnlinkRequest(mockRequest);
		}).getMessage();
		
		assertEquals("Unsupported query: was SomeOtherQuery, expected UnlinkedFileHandles.", message);
	}
	
	@Test
	public void testProcessFileHandleUnlinkRequestWithNoQueryExecution() {
		
		mockRequest.withQueryExecution(null);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processFileHandleUnlinkRequest(mockRequest);
		}).getMessage();
		
		assertEquals("The queryExecution is required.", message);
	}
	
	@Test
	public void testProcessFileHandleUnlinkRequestWithNoFunctionExecutionId() {
		
		mockRequest.withFunctionExecutionId(null);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processFileHandleUnlinkRequest(mockRequest);
		}).getMessage();
		
		assertEquals("The functionExecutionId is required and must not be the empty string.", message);
	}
	
	@Test
	public void testProcessFileHandleUnlinkRequestWithNoQueryExecutionId() {
		
		mockQueryExecution.withQueryExecutionId(null);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processFileHandleUnlinkRequest(mockRequest);
		}).getMessage();
		
		assertEquals("The queryExecution.queryExecutionId is required.", message);
	}

	@Test
	public void testProcessFileHandleUnlinkRequestWithQueryQueued() {
		
		mockQueryExecution.setStatus(new QueryExecutionStatus().withState(QueryExecutionState.QUEUED));
		
		when(mockAthenaSupport.getQueryExecutionStatus(anyString())).thenReturn(new AthenaQueryExecutionAdapter(mockQueryExecution));
		
		String message = assertThrows(RecoverableException.class, () -> {
			// Call under test
			manager.processFileHandleUnlinkRequest(mockRequest);
		}).getMessage();
		
		assertEquals("The query with id " + mockQueryExecution.getQueryExecutionId() + " is still processing.", message);
	}
	
	@Test
	public void testProcessFileHandleUnlinkRequestWithQueryRunning() {
		
		mockQueryExecution.setStatus(new QueryExecutionStatus().withState(QueryExecutionState.RUNNING));
		
		when(mockAthenaSupport.getQueryExecutionStatus(anyString())).thenReturn(new AthenaQueryExecutionAdapter(mockQueryExecution));
		
		String message = assertThrows(RecoverableException.class, () -> {
			// Call under test
			manager.processFileHandleUnlinkRequest(mockRequest);
		}).getMessage();
		
		assertEquals("The query with id " + mockQueryExecution.getQueryExecutionId() + " is still processing.", message);
	}
	
	@Test
	public void testProcessFileHandleUnlinkRequestWithQueryNotSucceded() {
		
		int count = 0;
		
		for (QueryExecutionState state : QueryExecutionState.values()) {
			
			if (state == QueryExecutionState.RUNNING || state == QueryExecutionState.QUEUED || state == QueryExecutionState.SUCCEEDED) {
				continue;
			}
			
			mockQueryExecution.setStatus(new QueryExecutionStatus().withState(state).withStateChangeReason("some reason"));
			
			when(mockAthenaSupport.getQueryExecutionStatus(anyString())).thenReturn(new AthenaQueryExecutionAdapter(mockQueryExecution));
			
			String message = assertThrows(IllegalStateException.class, () -> {
				// Call under test
				manager.processFileHandleUnlinkRequest(mockRequest);
			}).getMessage();
			
			assertEquals("The query with id " +  mockQueryExecution.getQueryExecutionId() + " did not SUCCEED (State: " + state + ", reason: some reason)", message);
			
			verify(mockAthenaSupport, times(++count)).getQueryExecutionStatus(mockQueryExecution.getQueryExecutionId());
		}
		
	}
	
	@Test
	public void testProcessFileHandleUnlinkRequestWithQuerySucceeded() {
		
		AthenaQueryResultPage<Long> page = new AthenaQueryResultPage<Long>()
				.withResults(Arrays.asList(1L, 2L, 3L))
				.withQueryExecutionId(mockQueryExecution.getQueryExecutionId())
				.withNextPageToken(null);
		
		when(mockAthenaSupport.getQueryExecutionStatus(anyString())).thenReturn(new AthenaQueryExecutionAdapter(mockQueryExecution));	
		when(mockAthenaSupport.getQueryResultsPage(any(), any(RowMapper.class), any(), anyInt())).thenReturn(page);
		
		// Call under test
		manager.processFileHandleUnlinkRequest(mockRequest);
		
		verify(mockAthenaSupport).getQueryExecutionStatus(mockQueryExecution.getQueryExecutionId());
		verify(mockAthenaSupport).getQueryResultsPage(mockQueryExecution.getQueryExecutionId(), FileHandleUnlinkedManagerImpl.ROW_MAPPER, null, FileHandleUnlinkedManagerImpl.MAX_QUERY_RESULTS);
		verify(mockFileHandleDao).updateStatus(page.getResults(), FileHandleStatus.UNLINKED, FileHandleStatus.AVAILABLE);
		verify(mockSqsClient, never()).sendMessage(any());
	}
	
	@Test
	public void testProcessFileHandleUnlinkRequestWithEmptyResults() {
		
		AthenaQueryResultPage<Long> page = new AthenaQueryResultPage<Long>()
				.withResults(Collections.emptyList())
				.withQueryExecutionId(mockQueryExecution.getQueryExecutionId())
				.withNextPageToken(null);
		
		when(mockAthenaSupport.getQueryExecutionStatus(anyString())).thenReturn(new AthenaQueryExecutionAdapter(mockQueryExecution));	
		when(mockAthenaSupport.getQueryResultsPage(any(), any(RowMapper.class), any(), anyInt())).thenReturn(page);
		
		// Call under test
		manager.processFileHandleUnlinkRequest(mockRequest);
		
		verify(mockAthenaSupport).getQueryExecutionStatus(mockQueryExecution.getQueryExecutionId());
		verify(mockAthenaSupport).getQueryResultsPage(mockQueryExecution.getQueryExecutionId(), FileHandleUnlinkedManagerImpl.ROW_MAPPER, null, FileHandleUnlinkedManagerImpl.MAX_QUERY_RESULTS);
		verifyZeroInteractions(mockFileHandleDao);
		verify(mockSqsClient, never()).sendMessage(any());
	}
	
	@Test
	public void testProcessFileHandleUnlinkRequestWithNoResults() {
		
		AthenaQueryResultPage<Long> page = new AthenaQueryResultPage<Long>()
				.withResults(null)
				.withQueryExecutionId(mockQueryExecution.getQueryExecutionId())
				.withNextPageToken(null);
		
		when(mockAthenaSupport.getQueryExecutionStatus(anyString())).thenReturn(new AthenaQueryExecutionAdapter(mockQueryExecution));	
		when(mockAthenaSupport.getQueryResultsPage(any(), any(RowMapper.class), any(), anyInt())).thenReturn(page);
		
		// Call under test
		manager.processFileHandleUnlinkRequest(mockRequest);
		
		verify(mockAthenaSupport).getQueryExecutionStatus(mockQueryExecution.getQueryExecutionId());
		verify(mockAthenaSupport).getQueryResultsPage(mockQueryExecution.getQueryExecutionId(), FileHandleUnlinkedManagerImpl.ROW_MAPPER, null, FileHandleUnlinkedManagerImpl.MAX_QUERY_RESULTS);
		verifyZeroInteractions(mockFileHandleDao);
		verify(mockSqsClient, never()).sendMessage(any());
	}
	
	@Test
	public void testProcessFileHandleUnlinkRequestWithNextPage() {
		
		AthenaQueryResultPage<Long> page1 = new AthenaQueryResultPage<Long>()
				.withResults(Arrays.asList(1L, 2L, 3L))
				.withQueryExecutionId(mockQueryExecution.getQueryExecutionId())
				.withNextPageToken("next");
		
		AthenaQueryResultPage<Long> page2 = new AthenaQueryResultPage<Long>()
				.withResults(Arrays.asList(4L, 5L))
				.withQueryExecutionId(mockQueryExecution.getQueryExecutionId())
				.withNextPageToken(null);
		
		when(mockAthenaSupport.getQueryExecutionStatus(anyString())).thenReturn(new AthenaQueryExecutionAdapter(mockQueryExecution));	
		when(mockAthenaSupport.getQueryResultsPage(any(), any(RowMapper.class), any(), anyInt())).thenReturn(page1, page2);
		
		// Call under test
		manager.processFileHandleUnlinkRequest(mockRequest);
		
		verify(mockAthenaSupport).getQueryExecutionStatus(mockQueryExecution.getQueryExecutionId());
		verify(mockAthenaSupport).getQueryResultsPage(mockQueryExecution.getQueryExecutionId(), FileHandleUnlinkedManagerImpl.ROW_MAPPER, null, FileHandleUnlinkedManagerImpl.MAX_QUERY_RESULTS);
		verify(mockAthenaSupport).getQueryResultsPage(mockQueryExecution.getQueryExecutionId(), FileHandleUnlinkedManagerImpl.ROW_MAPPER, page1.getNextPageToken(), FileHandleUnlinkedManagerImpl.MAX_QUERY_RESULTS);
		verify(mockFileHandleDao).updateStatus(page1.getResults(), FileHandleStatus.UNLINKED, FileHandleStatus.AVAILABLE);
		verify(mockFileHandleDao).updateStatus(page2.getResults(), FileHandleStatus.UNLINKED, FileHandleStatus.AVAILABLE);
		verify(mockSqsClient, never()).sendMessage(any());
	}
	
	@Test
	public void testProcessFileHandleUnlinkRequestWithExceedProcessingLimit() throws JsonProcessingException {
		
		String nextToken = "next"; 
				
		AthenaQueryResultPage<Long> page1 = new AthenaQueryResultPage<Long>()
				.withResults(Arrays.asList(1L, 2L, 3L))
				.withQueryExecutionId(mockQueryExecution.getQueryExecutionId())
				.withNextPageToken(nextToken);
		
		List<AthenaQueryResultPage<Long>> nextPages = new ArrayList<>();		
		
		for (int i=0; i< FileHandleUnlinkedManagerImpl.MAX_PAGE_REQUESTS; i++) {
			AthenaQueryResultPage<Long> next = new AthenaQueryResultPage<Long>()
				.withResults(Arrays.asList(4L, 5L))
				.withQueryExecutionId(mockQueryExecution.getQueryExecutionId())
				.withNextPageToken(nextToken);
			nextPages.add(next);
		}
		
		when(mockAthenaSupport.getQueryExecutionStatus(anyString())).thenReturn(new AthenaQueryExecutionAdapter(mockQueryExecution));	
		when(mockAthenaSupport.getQueryResultsPage(any(), any(RowMapper.class), any(), anyInt())).thenReturn(page1, nextPages.toArray(new AthenaQueryResultPage[nextPages.size()]));
		when(mockObjectMapper.writeValueAsString(any())).thenReturn("sqsMessage");
		
		// Call under test
		manager.processFileHandleUnlinkRequest(mockRequest);
		
		verify(mockAthenaSupport).getQueryExecutionStatus(mockQueryExecution.getQueryExecutionId());
		
		verify(mockAthenaSupport).getQueryResultsPage(mockQueryExecution.getQueryExecutionId(), FileHandleUnlinkedManagerImpl.ROW_MAPPER, null, FileHandleUnlinkedManagerImpl.MAX_QUERY_RESULTS);
		verify(mockAthenaSupport, times(FileHandleUnlinkedManagerImpl.MAX_PAGE_REQUESTS - 1)).getQueryResultsPage(mockQueryExecution.getQueryExecutionId(), FileHandleUnlinkedManagerImpl.ROW_MAPPER, nextToken, FileHandleUnlinkedManagerImpl.MAX_QUERY_RESULTS);
		
		verify(mockFileHandleDao, times(FileHandleUnlinkedManagerImpl.MAX_PAGE_REQUESTS)).updateStatus(anyList(), eq(FileHandleStatus.UNLINKED), eq(FileHandleStatus.AVAILABLE));
		
		verify(mockSqsClient).sendMessage(new SendMessageRequest()
				.withQueueUrl(queueUrl)
				.withMessageBody("sqsMessage")
		);

		FileHandleUnlinkedRequest expectedRequest = new FileHandleUnlinkedRequest()
				.withQueryName(mockRequest.getQueryName())
				.withFunctionExecutionId(mockRequest.getFunctionExecutionId())
				.withPageToken(nextToken)
				.withQueryExecution(mockRequest.getQueryExecution());
		
		verify(mockObjectMapper).writeValueAsString(expectedRequest);
	}

	
}
