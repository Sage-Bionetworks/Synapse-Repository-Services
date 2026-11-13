package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.util.progress.ProgressListener;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

/**
 * Unit test for TableCSVAppenderPreviewWorker.
 *
 */
@ExtendWith(MockitoExtension.class)
public class TableCSVAppenderPreviewWorkerTest {
	
	@Mock
	private FileHandleManager mockFileHandleManager;
	@Mock
	private SynapseS3Client mockS3Client;
	@Mock
	private AsyncJobProgressCallback mockJobCallback;
	@Mock
	private ResponseInputStream<GetObjectResponse> mockObjectStream;

	@InjectMocks
	private TableCSVAppenderPreviewWorker worker;


	private UploadToTablePreviewRequest request;
	private S3FileHandle fileHandle;
	private HeadObjectResponse fileMetadata;

	private UserInfo userInfo;

	@BeforeEach
	public void before() throws JSONObjectAdapterException{

		request = new UploadToTablePreviewRequest();
		request.setUploadFileHandleId("fileHandleId");

		userInfo = new UserInfo(false, 123L, AuthorizationConstants.DEFAULT_REALM_ID);

		fileHandle = new S3FileHandle();
		fileHandle.setBucketName("bucketName");
		fileHandle.setKey("key");
		when(mockFileHandleManager.getRawFileHandle(any(UserInfo.class), anyString())).thenReturn(fileHandle);

		fileMetadata = HeadObjectResponse.builder().contentLength(1024L).build();
		when(mockS3Client.getObjectMetadataV2(anyString(), anyString())).thenReturn(fileMetadata);
		when(mockS3Client.getObjectV2(any(GetObjectRequest.class))).thenReturn(mockObjectStream);
	}
	
	@Test
	public void testProgressListenerRemoved() throws Throwable {
		
		// We do not have a meaningful IS so this will throw but this test is only checking that the listener is removed
		assertThrows(IOException.class, () -> {			
			worker.run("123", userInfo, request, mockJobCallback);
		});
		
		verify(mockJobCallback).addProgressListener(any(ProgressListener.class));
		verify(mockJobCallback).removeProgressListener(any(ProgressListener.class));
	}

}
