package org.sagebionetworks.search.workers.sqs.search;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.search.SearchManager;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.cloudsearchdomain.model.AmazonCloudSearchDomainException;

@RunWith(MockitoJUnitRunner.class)
public class SearchQueueWorkerTest {

	@Mock
	private ProgressCallback mockCallback;
	@Mock
	private SearchManager mockSearchManager;
	@Mock
	private WorkerLogger mockWorkerLogger;

	private SearchQueueWorker worker;
	private List<ChangeMessage> messages;

	@Before
	public void before(){

		worker = new SearchQueueWorker();
		ReflectionTestUtils.setField(worker, "searchManager", mockSearchManager);
		ReflectionTestUtils.setField(worker, "workerLogger", mockWorkerLogger);

		messages = Collections.singletonList(new ChangeMessage());
	}

	@Test
	public void testNoFailure() throws IOException, RecoverableMessageException {
		worker.run(mockCallback, messages);
		verify(mockSearchManager, times(1)).documentChangeMessages(messages);
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(SearchQueueWorker.class), any(ChangeMessage.class), any(Throwable.class), anyBoolean());
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
		doThrow(IllegalArgumentException.class).when(mockSearchManager).documentChangeMessages(messages);
		try {
			worker.run(mockCallback, messages);
			fail("Excepted Exception to be thrown");
		}catch (IllegalArgumentException e){
			//expected behavior
		}
		verify(mockSearchManager, times(1)).documentChangeMessages(messages);
		verify(mockWorkerLogger, times(1)).logWorkerFailure(eq(SearchQueueWorker.class.getName()), any(IllegalArgumentException.class), eq(false));

	}

	private void exceptionThrowTestHelper(Class<? extends Throwable> clazz) throws IOException, RecoverableMessageException {
		doThrow(clazz).when(mockSearchManager).documentChangeMessages(messages);
		try {
			worker.run(mockCallback,messages);
			fail("Excepted RecoverableMessageException to be thrown");
		}catch (RecoverableMessageException e){
			//expected behavior
		}
		verify(mockSearchManager, times(1)).documentChangeMessages(messages);
		verify(mockWorkerLogger, times(1)).logWorkerFailure(eq(SearchQueueWorker.class.getName()),  any(clazz), eq(true));

	}

}
