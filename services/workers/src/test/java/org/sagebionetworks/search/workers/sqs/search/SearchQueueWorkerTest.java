package org.sagebionetworks.search.workers.sqs.search;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import com.amazonaws.services.cloudsearchdomain.model.AmazonCloudSearchDomainException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.search.SearchManager;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class SearchQueueWorkerTest {

	@Mock
	private ProgressCallback mockCallback;
	@Mock
	private SearchManager mockSearchManager;
	@Mock
	private WorkerLogger mockWorkerLogger;

	private SearchQueueWorker worker;
	private ChangeMessage message;

	@Before
	public void before(){

		worker = new SearchQueueWorker();
		ReflectionTestUtils.setField(worker, "searchManager", mockSearchManager);
		ReflectionTestUtils.setField(worker, "workerLogger", mockWorkerLogger);

		message = new ChangeMessage();
	}

	@Test
	public void testNoFailure() throws IOException, RecoverableMessageException {
		worker.run(mockCallback, message);
		verify(mockSearchManager, times(1)).documentChangeMessage(message);
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(SearchQueueWorker.class), any(ChangeMessage.class), any(Throwable.class), anyBoolean());
	}

	@Test
	public void testIOExceptionThrown() throws IOException, RecoverableMessageException {
		exceptionThrowTestHelper(IOException.class);
	}

	@Test
	public void testCloudSearchServerExceptionThrown() throws IOException, RecoverableMessageException {
		exceptionThrowTestHelper(AmazonCloudSearchDomainException.class);
	}

	@Test
	public void testTemporarilyUnavailableExceptionThrown() throws IOException, RecoverableMessageException {
		exceptionThrowTestHelper(TemporarilyUnavailableException.class);
	}

	@Test
	public void testExceptionThrownUnexpected() throws IOException, RecoverableMessageException {
		doThrow(IllegalArgumentException.class).when(mockSearchManager).documentChangeMessage(message);
		try {
			worker.run(mockCallback, message);
			fail("Excepted Exception to be thrown");
		}catch (IllegalArgumentException e){
			//expected behavior
		}
		verify(mockSearchManager, times(1)).documentChangeMessage(message);
		verify(mockWorkerLogger, times(1)).logWorkerFailure(eq(SearchQueueWorker.class), eq(message), any(IllegalArgumentException.class), eq(false));

	}

	private void exceptionThrowTestHelper(Class<? extends Throwable> clazz) throws IOException, RecoverableMessageException {
		doThrow(clazz).when(mockSearchManager).documentChangeMessage(message);
		try {
			worker.run(mockCallback, message);
			fail("Excepted RecoverableMessageException to be thrown");
		}catch (RecoverableMessageException e){
			//expected behavior
		}
		verify(mockSearchManager, times(1)).documentChangeMessage(message);
		verify(mockWorkerLogger, times(1)).logWorkerFailure(eq(SearchQueueWorker.class), eq(message), any(clazz), eq(true));

	}

}
