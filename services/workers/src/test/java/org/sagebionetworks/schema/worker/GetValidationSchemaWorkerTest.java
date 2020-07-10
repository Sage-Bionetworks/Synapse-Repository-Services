package org.sagebionetworks.schema.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.schema.GetValidationSchemaRequest;
import org.sagebionetworks.repo.model.schema.GetValidationSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

/**
 * Unit test for CreateJsonSchemaWorker
 *
 */
@ExtendWith(MockitoExtension.class)
public class GetValidationSchemaWorkerTest {
	
	@Mock
	private AsynchJobStatusManager mockAsynchJobStatusManager;
	@Mock
	private JsonSchemaManager mockJsonSchemaManager;
	@Mock
	private ProgressCallback mockProgressCallback;
	@Mock
	private Message mockMessage;
	
	@InjectMocks
	GetValidationSchemaWorker worker;
	
	String jobId;
	JsonSchema schema;
	AsynchronousJobStatus jobStatus;
	GetValidationSchemaRequest requestBody;
	Long startedByUserId;
	UserInfo user;
	JsonSchemaVersionInfo newVersionInfo;
	GetValidationSchemaResponse response;
	
	@BeforeEach
	public void before() {
		jobId = "123";
		startedByUserId = 987L;
		boolean isAdmin = false;
		user = new UserInfo(isAdmin, startedByUserId);
		schema = new JsonSchema();
		schema.set$id("org/path.name/1.0.1");
		requestBody = new GetValidationSchemaRequest();
		requestBody.set$id(schema.get$id());
		jobStatus = new AsynchronousJobStatus();
		jobStatus.setJobId(jobId);
		jobStatus.setRequestBody(requestBody);
		jobStatus.setStartedByUserId(startedByUserId);
		
		newVersionInfo = new JsonSchemaVersionInfo();
		newVersionInfo.setCreatedBy(startedByUserId.toString());
		newVersionInfo.setCreatedOn(new Date());
		newVersionInfo.setJsonSHA256Hex("sha-hash");
		newVersionInfo.setVersionId("4444");
		response = new GetValidationSchemaResponse();
		response.setValidationSchema(schema);
	}
	

	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		when(mockMessage.getBody()).thenReturn(jobId);
		when(mockAsynchJobStatusManager.lookupJobStatus(any())).thenReturn(jobStatus);
		when(mockJsonSchemaManager.getValidationSchema(any())).thenReturn(schema);
		// call under test
		worker.run(mockProgressCallback, mockMessage);
		verify(mockAsynchJobStatusManager).lookupJobStatus(jobId);
		verify(mockAsynchJobStatusManager).updateJobProgress(jobId, 0L, 100L, "Starting job...");
		verify(mockJsonSchemaManager).getValidationSchema(schema.get$id());
		verify(mockAsynchJobStatusManager).setComplete(jobId, response);
		verify(mockAsynchJobStatusManager, never()).setJobFailed(any(), any());
	}
	
	@Test
	public void testRunFailed() throws RecoverableMessageException, Exception {
		when(mockMessage.getBody()).thenReturn(jobId);
		when(mockAsynchJobStatusManager.lookupJobStatus(any())).thenReturn(jobStatus);
		NotFoundException exception = new NotFoundException("not found");
		when(mockJsonSchemaManager.getValidationSchema(any())).thenThrow(exception);
		// call under test
		worker.run(mockProgressCallback, mockMessage);
		verify(mockJsonSchemaManager).getValidationSchema(schema.get$id());
		verify(mockAsynchJobStatusManager, never()).setComplete(any(), any());
		verify(mockAsynchJobStatusManager).setJobFailed(jobId, exception);
	}
	
}
