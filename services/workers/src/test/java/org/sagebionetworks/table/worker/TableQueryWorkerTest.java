package org.sagebionetworks.table.worker;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
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
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.model.Message;

@RunWith(MockitoJUnitRunner.class)
public class TableQueryWorkerTest {

	@Mock
	private AsynchJobStatusManager mockAsynchJobStatusManager;
	@Mock
	private TableQueryManager mockTableQueryManager;
	@Mock
	private UserManager mockUserManger;
	@Mock
	TableExceptionTranslator mockTableExceptionTranslator;

	@Mock
	ProgressCallback mockProgressCallback;

	TableQueryWorker worker;

	Long userId;
	UserInfo userInfo;
	QueryBundleRequest request;
	AsynchronousJobStatus status;
	String jobId;
	Message message;
	
	QueryResultBundle results;
	
	RuntimeException translatedException;

	@Before
	public void before() throws Exception {
		worker = new TableQueryWorker();
		ReflectionTestUtils.setField(worker, "asynchJobStatusManager", mockAsynchJobStatusManager);
		ReflectionTestUtils.setField(worker, "tableQueryManager", mockTableQueryManager);
		ReflectionTestUtils.setField(worker, "userManger", mockUserManger);
		ReflectionTestUtils.setField(worker, "tableExceptionTranslator", mockTableExceptionTranslator);
		
		userId = 987L;
		userInfo = new UserInfo(false);
		userInfo.setId(userId);
		when(mockUserManger.getUserInfo(userId)).thenReturn(userInfo);

		Query query = new Query();
		query.setSql("select * from syn123");
		request = new QueryBundleRequest();
		request.setQuery(query);
		
		jobId = "1";
		status = new AsynchronousJobStatus();
		status.setJobId(jobId);
		status.setRequestBody(request);
		status.setStartedByUserId(userId);

		message = new Message();
		message.setBody(jobId);
		when(mockAsynchJobStatusManager.lookupJobStatus(jobId)).thenReturn(status);
		
		results = new QueryResultBundle();
		
		when(mockTableQueryManager.queryBundle(mockProgressCallback, userInfo, request)).thenReturn(results);
		
		doAnswer(new Answer<RuntimeException>() {

			@Override
			public RuntimeException answer(InvocationOnMock invocation) throws Throwable {
				Throwable exception = (Throwable) invocation.getArguments()[0];
				translatedException = new RuntimeException("translated",exception);
				return translatedException;
			}
		}).when(mockTableExceptionTranslator).translateException(any(Throwable.class));

	}

	@Test
	public void testBasicQuery() throws Exception {
		// call under test
		worker.run(mockProgressCallback, message);
		verify(mockAsynchJobStatusManager).setComplete(jobId, results);
	}
	
	@Test
	public void testTableUnavailableException() throws Exception {
		// table not available
		when(mockTableQueryManager.queryBundle(mockProgressCallback, userInfo, request)).thenThrow(new TableUnavailableException(new TableStatus()));
		try {
			// call under test
			worker.run(mockProgressCallback, message);
			fail();
		} catch (RecoverableMessageException e) {
			// expected
		}
		verify(mockAsynchJobStatusManager).updateJobProgress(eq(jobId), any(Long.class),  any(Long.class), anyString());
	}
	
	@Test
	public void testLockUnavilableExceptionException() throws Exception {
		// table not available
		when(mockTableQueryManager.queryBundle(mockProgressCallback, userInfo, request)).thenThrow(new LockUnavilableException());
		try {
			// call under test
			worker.run(mockProgressCallback, message);
			fail();
		} catch (RecoverableMessageException e) {
			// expected
		}
		verify(mockAsynchJobStatusManager).updateJobProgress(eq(jobId), any(Long.class),  any(Long.class), anyString());
	}
	
	@Test
	public void testTableFailedExceptionException() throws Exception {
		TableFailedException exception = new TableFailedException(new TableStatus());
		// table not available
		when(mockTableQueryManager.queryBundle(mockProgressCallback, userInfo, request)).thenThrow(exception);
		// call under test
		worker.run(mockProgressCallback, message);
		verify(mockAsynchJobStatusManager).setJobFailed(jobId, exception);
	}
	
	@Test
	public void testUnknownException() throws Exception {
		RuntimeException error = new RuntimeException("Bad stuff happened");
		// table not available
		when(mockTableQueryManager.queryBundle(mockProgressCallback, userInfo, request)).thenThrow(error);
		try {
			// call under test
			worker.run(mockProgressCallback, message);
			fail();
		} catch (Throwable e) {
			// expected
		}
		// the exception should be translated.
		verify(mockTableExceptionTranslator).translateException(error);
		// The translated exception should be set
		verify(mockAsynchJobStatusManager).setJobFailed(jobId, translatedException);
	}
}
