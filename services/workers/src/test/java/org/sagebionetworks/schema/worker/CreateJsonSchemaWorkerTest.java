package org.sagebionetworks.schema.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

/**
 * Unit test for CreateJsonSchemaWorker
 *
 */
@ExtendWith(MockitoExtension.class)
public class CreateJsonSchemaWorkerTest {
	@Mock
	private JsonSchemaManager mockJsonSchemaManager;
	@InjectMocks
	private CreateJsonSchemaWorker worker;

	@Mock
	private ProgressCallback mockProgressCallback;
	@Mock
	private AsyncJobProgressCallback mockJobCallback;
	@Mock
	private CreateSchemaRequest mockRequest;
	@Mock
	private UserInfo mockUser;
	@Mock
	private CreateSchemaResponse mockResponse;

	private String jobId;

	@BeforeEach
	public void before() {
		jobId = "123";
	}

	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		when(mockJsonSchemaManager.createJsonSchema(any(), any())).thenReturn(mockResponse);
		// call under test
		CreateSchemaResponse result = worker.run(jobId, mockUser, mockRequest, mockJobCallback);

		assertEquals(mockResponse, result);

		verify(mockJsonSchemaManager).createJsonSchema(mockUser, mockRequest);
	}
}
