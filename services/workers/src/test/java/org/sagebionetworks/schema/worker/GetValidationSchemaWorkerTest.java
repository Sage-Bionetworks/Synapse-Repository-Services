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
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.schema.GetValidationSchemaRequest;
import org.sagebionetworks.repo.model.schema.GetValidationSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

/**
 * Unit test for CreateJsonSchemaWorker
 *
 */
@ExtendWith(MockitoExtension.class)
public class GetValidationSchemaWorkerTest {

	@Mock
	private JsonSchemaManager mockJsonSchemaManager;

	@InjectMocks
	private GetValidationSchemaWorker worker;
	@Mock
	private AsyncJobProgressCallback mockJobCallback;
	@Mock
	private GetValidationSchemaRequest mockRequest;
	@Mock
	private JsonSchema mockSchema;
	@Mock
	private UserInfo mockUser;

	private String jobId;

	@BeforeEach
	public void before() {
		jobId = "123";
	}

	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		
		when(mockRequest.get$id()).thenReturn("schemaId");
		when(mockJsonSchemaManager.getValidationSchema(any())).thenReturn(mockSchema);

		GetValidationSchemaResponse expected = new GetValidationSchemaResponse().setValidationSchema(mockSchema);

		// call under test
		GetValidationSchemaResponse result = worker.run(jobId, mockUser, mockRequest, mockJobCallback);

		assertEquals(expected, result);

		verify(mockJsonSchemaManager).getValidationSchema("schemaId");
	}

}
