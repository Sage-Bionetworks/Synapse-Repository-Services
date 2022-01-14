package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.TableIndexManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelPage;
import org.sagebionetworks.repo.model.table.ViewColumnModelRequest;
import org.sagebionetworks.repo.model.table.ViewColumnModelResponse;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.worker.AsyncJobProgressCallback;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class ViewColumnModelRequestWorkerTest {
		
	@Mock
	private TableIndexConnectionFactory mockindexConnectionFactory;
	
	@InjectMocks
	private ViewColumnModelRequestWorker worker;
	
	@Mock
	private ProgressCallback mockCallback;

	@Mock
	private AsyncJobProgressCallback mockJobCallback;
	
	@Mock
	private TableIndexManager mockIndexManager;
	
	@Mock
	private ViewColumnModelRequest mockRequest;
	
	@Mock
	private UserInfo mockUser;

	@Mock
	private ViewScope mockViewScope;
	
	@Mock
	private ColumnModelPage mockModelPage;
	
	@Mock
	private List<ColumnModel> mockModelResults;
	
	@Test
	public void testRun() throws Exception {
		when(mockindexConnectionFactory.connectToFirstIndex()).thenReturn(mockIndexManager);
		when(mockIndexManager.getPossibleColumnModelsForScope(any(), any())).thenReturn(mockModelPage);
		when(mockModelPage.getResults()).thenReturn(mockModelResults);
		
		String jobId = "1";
		List<String> scope = ImmutableList.of("1", "2");
		ViewEntityType viewEntityType = ViewEntityType.submissionview;

		when(mockViewScope.getScope()).thenReturn(scope);
		when(mockViewScope.getViewEntityType()).thenReturn(viewEntityType);
		
		when(mockRequest.getViewScope()).thenReturn(mockViewScope);
		
		ViewColumnModelResponse expectedResponse = new ViewColumnModelResponse();
		
		expectedResponse.setResults(mockModelResults);
		
		// Call under test
		ViewColumnModelResponse result = worker.run(mockCallback, jobId, mockUser, mockRequest, mockJobCallback);
		
		assertEquals(expectedResponse, result);
		
		verify(mockJobCallback).updateProgress("Processing ViewColumnModelRequest job (EntiyViewType: submissionview, Scope Size: 2)...", 0L, 100L);
		verify(mockIndexManager).getPossibleColumnModelsForScope(mockViewScope, null);
	}
	
	@Test
	public void testRunWithNextPageToken() throws Exception {
		when(mockindexConnectionFactory.connectToFirstIndex()).thenReturn(mockIndexManager);
		when(mockIndexManager.getPossibleColumnModelsForScope(any(), any())).thenReturn(mockModelPage);
		when(mockModelPage.getResults()).thenReturn(mockModelResults);
		
		String jobId = "1";
		List<String> scope = ImmutableList.of("1", "2");
		ViewEntityType viewEntityType = ViewEntityType.submissionview;
		String nextPageToken = "someToken";

		when(mockViewScope.getScope()).thenReturn(scope);
		when(mockViewScope.getViewEntityType()).thenReturn(viewEntityType);
		
		when(mockRequest.getViewScope()).thenReturn(mockViewScope);
		when(mockRequest.getNextPageToken()).thenReturn(nextPageToken);
		
		when(mockModelPage.getNextPageToken()).thenReturn(nextPageToken);
		ViewColumnModelResponse expectedResponse = new ViewColumnModelResponse();
		
		expectedResponse.setResults(mockModelResults);
		expectedResponse.setNextPageToken(nextPageToken);
		
		// Call under test
		ViewColumnModelResponse result = worker.run(mockCallback, jobId, mockUser, mockRequest, mockJobCallback);
		
		assertEquals(expectedResponse, result);
		
		verify(mockJobCallback).updateProgress("Processing ViewColumnModelRequest job (EntiyViewType: submissionview, Scope Size: 2)...", 0L, 100L);
		verify(mockIndexManager).getPossibleColumnModelsForScope(mockViewScope, nextPageToken);
	}
	
	@Test
	public void testRunWithFailure() throws Exception {
		when(mockindexConnectionFactory.connectToFirstIndex()).thenReturn(mockIndexManager);
		
		IllegalArgumentException ex = new IllegalArgumentException("Some exception");
		
		doThrow(ex).when(mockIndexManager).getPossibleColumnModelsForScope(any(), any());
		
		String jobId = "1";
		List<String> scope = ImmutableList.of("1", "2");
		ViewEntityType viewEntityType = ViewEntityType.submissionview;

		when(mockViewScope.getScope()).thenReturn(scope);
		when(mockViewScope.getViewEntityType()).thenReturn(viewEntityType);
		
		when(mockRequest.getViewScope()).thenReturn(mockViewScope);
		
		IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			worker.run(mockCallback, jobId, mockUser, mockRequest, mockJobCallback);
		});
		
		assertEquals(ex, result);
		
		verify(mockJobCallback).updateProgress("Processing ViewColumnModelRequest job (EntiyViewType: submissionview, Scope Size: 2)...", 0L, 100L);
		verify(mockIndexManager).getPossibleColumnModelsForScope(mockViewScope, null);
	}
	
	
}
