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
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.util.progress.ProgressListener;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

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
	private S3Object mockS3Ojbect;
	@Mock
	private S3ObjectInputStream mockInputStream;	
	
	@InjectMocks
	private TableCSVAppenderPreviewWorker worker;
	

	private UploadToTablePreviewRequest request;
	private S3FileHandle fileHandle;
	private ObjectMetadata fileMetadata;

	private UserInfo userInfo;

	@BeforeEach
	public void before() throws JSONObjectAdapterException{
		
		request = new UploadToTablePreviewRequest();
		request.setUploadFileHandleId("fileHandleId");

		userInfo = new UserInfo(false, 123L);
		
		fileHandle = new S3FileHandle();
		fileHandle.setBucketName("bucketName");
		fileHandle.setKey("key");
		when(mockFileHandleManager.getRawFileHandle(any(UserInfo.class), anyString())).thenReturn(fileHandle);
		
		fileMetadata = new ObjectMetadata();
		when(mockS3Client.getObjectMetadata(anyString(), anyString())).thenReturn(fileMetadata);
		when(mockS3Client.getObject(anyString(), anyString())).thenReturn(mockS3Ojbect);
		when(mockS3Ojbect.getObjectContent()).thenReturn(mockInputStream);
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
