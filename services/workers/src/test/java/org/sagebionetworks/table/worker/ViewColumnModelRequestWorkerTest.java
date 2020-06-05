package org.sagebionetworks.table.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.TableIndexManager;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelPage;
import org.sagebionetworks.repo.model.table.ViewColumnModelRequest;
import org.sagebionetworks.repo.model.table.ViewColumnModelResponse;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewScope;

import com.amazonaws.services.sqs.model.Message;
import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class ViewColumnModelRequestWorkerTest {
	
	@Mock
	private AsynchJobStatusManager mockJobStatusManager;
	
	@Mock
	private TableIndexConnectionFactory mockindexConnectionFactory;
	
	@InjectMocks
	private ViewColumnModelRequestWorker worker;
	
	@Mock
	private ProgressCallback mockCallback;
	
	@Mock
	private Message mockMessage;
	
	@Mock
	private AsynchronousJobStatus mockJobStatus;
	
	@Mock
	private TableIndexManager mockIndexManager;
	
	@Mock
	private ViewColumnModelRequest mockRequest;

	@Mock
	private ViewScope mockViewScope;
	
	@Mock
	private ColumnModelPage mockModelPage;
	
	@Mock
	private List<ColumnModel> mockModelResults;
	
	@Test
	public void testRun() throws Exception {
		when(mockJobStatusManager.lookupJobStatus(any())).thenReturn(mockJobStatus);
		when(mockJobStatus.getRequestBody()).thenReturn(mockRequest);
		when(mockindexConnectionFactory.connectToFirstIndex()).thenReturn(mockIndexManager);
		when(mockIndexManager.getPossibleColumnModelsForScope(any(), any())).thenReturn(mockModelPage);
		when(mockModelPage.getResults()).thenReturn(mockModelResults);
		
		String jobId = "1";
		List<String> scope = ImmutableList.of("1", "2");
		ViewEntityType viewEntityType = ViewEntityType.submissionview;

		when(mockJobStatus.getJobId()).thenReturn(jobId);
		when(mockViewScope.getScope()).thenReturn(scope);
		when(mockViewScope.getViewEntityType()).thenReturn(viewEntityType);
		
		when(mockRequest.getViewScope()).thenReturn(mockViewScope);
		
		ViewColumnModelResponse expectedResponse = new ViewColumnModelResponse();
		
		expectedResponse.setResults(mockModelResults);
		
		// Call under test
		worker.run(mockCallback, mockMessage);
		
		verify(mockJobStatusManager).updateJobProgress(jobId, 0L, 100L, "Processing ViewColumnModelRequest job (EntiyViewType: submissionview, Scope Size: 2)...");
		verify(mockIndexManager).getPossibleColumnModelsForScope(mockViewScope, null);
		verify(mockJobStatusManager).setComplete(jobId, expectedResponse);
	}
	
	@Test
	public void testRunWithNextPageToken() throws Exception {
		when(mockJobStatusManager.lookupJobStatus(any())).thenReturn(mockJobStatus);
		when(mockJobStatus.getRequestBody()).thenReturn(mockRequest);
		when(mockindexConnectionFactory.connectToFirstIndex()).thenReturn(mockIndexManager);
		when(mockIndexManager.getPossibleColumnModelsForScope(any(), any())).thenReturn(mockModelPage);
		when(mockModelPage.getResults()).thenReturn(mockModelResults);
		
		String jobId = "1";
		List<String> scope = ImmutableList.of("1", "2");
		ViewEntityType viewEntityType = ViewEntityType.submissionview;
		String nextPageToken = "someToken";

		when(mockJobStatus.getJobId()).thenReturn(jobId);
		when(mockViewScope.getScope()).thenReturn(scope);
		when(mockViewScope.getViewEntityType()).thenReturn(viewEntityType);
		
		when(mockRequest.getViewScope()).thenReturn(mockViewScope);
		when(mockRequest.getNextPageToken()).thenReturn(nextPageToken);
		
		when(mockModelPage.getNextPageToken()).thenReturn(nextPageToken);
		ViewColumnModelResponse expectedResponse = new ViewColumnModelResponse();
		
		expectedResponse.setResults(mockModelResults);
		expectedResponse.setNextPageToken(nextPageToken);
		
		// Call under test
		worker.run(mockCallback, mockMessage);
		
		verify(mockJobStatusManager).updateJobProgress(jobId, 0L, 100L, "Processing ViewColumnModelRequest job (EntiyViewType: submissionview, Scope Size: 2)...");
		verify(mockIndexManager).getPossibleColumnModelsForScope(mockViewScope, nextPageToken);
		verify(mockJobStatusManager).setComplete(jobId, expectedResponse);
	}
	
	@Test
	public void testRunWithFailure() throws Exception {
		when(mockJobStatusManager.lookupJobStatus(any())).thenReturn(mockJobStatus);
		when(mockJobStatus.getRequestBody()).thenReturn(mockRequest);
		when(mockindexConnectionFactory.connectToFirstIndex()).thenReturn(mockIndexManager);
		
		IllegalArgumentException ex = new IllegalArgumentException("Some exception");
		
		doThrow(ex).when(mockIndexManager).getPossibleColumnModelsForScope(any(), any());
		
		String jobId = "1";
		List<String> scope = ImmutableList.of("1", "2");
		ViewEntityType viewEntityType = ViewEntityType.submissionview;

		when(mockJobStatus.getJobId()).thenReturn(jobId);
		when(mockViewScope.getScope()).thenReturn(scope);
		when(mockViewScope.getViewEntityType()).thenReturn(viewEntityType);
		
		when(mockRequest.getViewScope()).thenReturn(mockViewScope);
		
		// Call under test
		worker.run(mockCallback, mockMessage);
		
		verify(mockJobStatusManager).updateJobProgress(jobId, 0L, 100L, "Processing ViewColumnModelRequest job (EntiyViewType: submissionview, Scope Size: 2)...");
		verify(mockIndexManager).getPossibleColumnModelsForScope(mockViewScope, null);
		verify(mockJobStatusManager).setJobFailed(jobId, ex);
	}
	
	
}
