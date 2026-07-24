package org.sagebionetworks.curation.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.curation.compute.ComputeTaskDispatcher;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.curation.ComputeTaskExecutionRequest;
import org.sagebionetworks.repo.model.curation.ComputeTaskExecutionResponse;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class ComputeTaskExecutionWorkerTest {

	@Mock
	private ComputeTaskDispatcher mockDispatcher;

	@Mock
	private AsyncJobProgressCallback mockCallback;

	@InjectMocks
	private ComputeTaskExecutionWorker worker;

	@Test
	public void testGetRequestType() {
		// call under test
		assertEquals(ComputeTaskExecutionRequest.class, worker.getRequestType());
	}

	@Test
	public void testGetResponseType() {
		// call under test
		assertEquals(ComputeTaskExecutionResponse.class, worker.getResponseType());
	}

	@Test
	public void testRunWithSuccess() throws Exception {
		String jobId = "job-123";
		UserInfo user = new UserInfo(false, 101L);
		ComputeTaskExecutionRequest request = new ComputeTaskExecutionRequest().setTaskId(987L);
		ComputeTaskExecutionResponse expectedResponse = new ComputeTaskExecutionResponse().setTaskId(987L);

		when(mockDispatcher.dispatch(jobId, user, request, mockCallback)).thenReturn(expectedResponse);

		// call under test
		ComputeTaskExecutionResponse result = worker.run(jobId, user, request, mockCallback);

		assertEquals(expectedResponse, result);
		verify(mockDispatcher).dispatch(jobId, user, request, mockCallback);
	}

	@Test
	public void testRunWithRecoverableException() throws Exception {
		String jobId = "job-123";
		UserInfo user = new UserInfo(false, 101L);
		ComputeTaskExecutionRequest request = new ComputeTaskExecutionRequest().setTaskId(987L);

		when(mockDispatcher.dispatch(jobId, user, request, mockCallback))
				.thenThrow(new RecoverableMessageException("transient failure"));

		// call under test
		assertThrows(RecoverableMessageException.class,
				() -> worker.run(jobId, user, request, mockCallback));
	}

	@Test
	public void testRunWithException() throws Exception {
		String jobId = "job-123";
		UserInfo user = new UserInfo(false, 101L);
		ComputeTaskExecutionRequest request = new ComputeTaskExecutionRequest().setTaskId(987L);

		when(mockDispatcher.dispatch(jobId, user, request, mockCallback))
				.thenThrow(new RuntimeException("permanent failure"));

		// call under test
		RuntimeException thrown = assertThrows(RuntimeException.class,
				() -> worker.run(jobId, user, request, mockCallback));

		assertEquals("permanent failure", thrown.getMessage());
	}
}
