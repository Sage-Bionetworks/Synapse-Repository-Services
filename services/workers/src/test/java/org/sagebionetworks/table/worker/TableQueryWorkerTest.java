package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dbo.dao.table.TableExceptionTranslator;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class TableQueryWorkerTest {

	@Mock
	private TableQueryManager mockTableQueryManager;
	@Mock
	private TableExceptionTranslator mockTableExceptionTranslator;

	@InjectMocks
	private TableQueryWorker worker;
	
	@Mock
	private AsyncJobProgressCallback mockJobCallback;

	Long userId;
	UserInfo userInfo;
	QueryBundleRequest request;
	AsynchronousJobStatus status;
	String jobId;
	Message message;
	
	QueryResultBundle results;

	@BeforeEach
	public void before() throws Exception {		
		userId = 987L;
		userInfo = new UserInfo(false);
		userInfo.setId(userId);

		Query query = new Query();
		query.setSql("select * from syn123");
		request = new QueryBundleRequest();
		request.setQuery(query);
		
		jobId = "1";
		status = new AsynchronousJobStatus();
		status.setJobId(jobId);
		status.setRequestBody(request);
		status.setStartedByUserId(userId);
		
		results = new QueryResultBundle();
		
		when(mockTableQueryManager.queryBundle(mockJobCallback, userInfo, request)).thenReturn(results);

	}

	@Test
	public void testBasicQuery() throws Exception {
		// call under test
		QueryResultBundle response = worker.run(jobId, userInfo, request, mockJobCallback);
		
		assertEquals(results, response);
		
		verify(mockTableQueryManager).queryBundle(mockJobCallback, userInfo, request);
	}
	
	@Test
	public void testTableUnavailableException() throws Exception {
		// table not available
		when(mockTableQueryManager.queryBundle(mockJobCallback, userInfo, request)).thenThrow(new TableUnavailableException(new TableStatus()));
		assertThrows(RecoverableMessageException.class, () -> {			
			// call under test
			worker.run(jobId, userInfo, request, mockJobCallback);
		});
		verify(mockTableQueryManager).queryBundle(mockJobCallback, userInfo, request);
		verify(mockJobCallback).updateProgress("Waiting for the table index to become available...", 0L, 100L);
	}
	
	@Test
	public void testLockUnavilableExceptionException() throws Exception {
		// table not available
		when(mockTableQueryManager.queryBundle(mockJobCallback, userInfo, request)).thenThrow(new LockUnavilableException());
		assertThrows(RecoverableMessageException.class, () -> {			
			// call under test
			worker.run(jobId, userInfo, request, mockJobCallback);
		});
		verify(mockTableQueryManager).queryBundle(mockJobCallback, userInfo, request);
		verify(mockJobCallback).updateProgress("Waiting for the table index to become available...", 0L, 100L);
	}
	
	@Test
	public void testTableFailedExceptionException() throws Exception {
		TableFailedException exception = new TableFailedException(new TableStatus());
		// table not available
		when(mockTableQueryManager.queryBundle(mockJobCallback, userInfo, request)).thenThrow(exception);
		// call under test
		TableFailedException result = assertThrows(TableFailedException.class, () -> {			
			// call under test
			worker.run(jobId, userInfo, request, mockJobCallback);
		});
		
		assertEquals(exception, result);
		verify(mockTableQueryManager).queryBundle(mockJobCallback, userInfo, request);
	}
	
	@Test
	public void testUnknownException() throws Exception {
		
		RuntimeException error = new RuntimeException("Bad stuff happened");
		when(mockTableQueryManager.queryBundle(mockJobCallback, userInfo, request)).thenThrow(error);
		
		RuntimeException translatedException = new RuntimeException("translated");
		
		when(mockTableExceptionTranslator.translateException(any())).thenReturn(translatedException);

		RuntimeException result = assertThrows(RuntimeException.class, () -> {
			// call under test
			worker.run(jobId, userInfo, request, mockJobCallback);
		});
	
		assertEquals(translatedException, result);
		verify(mockTableQueryManager).queryBundle(mockJobCallback, userInfo, request);
		// the exception should be translated.
		verify(mockTableExceptionTranslator).translateException(error);
	}
}
