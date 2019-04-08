package org.sagebionetworks.table.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressListener;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sqs.model.Message;

/**
 * Unit test for TableCSVAppenderPreviewWorker.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TableCSVAppenderPreviewWorkerTest {
	
	@Mock
	private AsynchJobStatusManager mockAsynchJobStatusManager;
	@Mock
	private TableEntityManager mockTableEntityManager;
	@Mock
	private UserManager mockUserManger;
	@Mock
	private FileHandleManager mockFileHandleManager;
	@Mock
	private SynapseS3Client mockS3Client;
	@Mock
	private ProgressCallback mockCallback;
	@Mock
	private Message mockMessage;
	@Mock
	private S3Object mockS3Ojbect;
	@Mock
	private S3ObjectInputStream mockInputStream;
	
	
	UploadToTablePreviewRequest body;

	@InjectMocks
	TableCSVAppenderPreviewWorker worker;
	
	AsynchronousJobStatus status;
	S3FileHandle fileHandle;
	ObjectMetadata fileMetadata;

	long userId;
	UserInfo userInfo;

	@Before
	public void before() throws JSONObjectAdapterException{
		status = new AsynchronousJobStatus();
		when(mockAsynchJobStatusManager.lookupJobStatus(anyString())).thenReturn(status);
		
		body = new UploadToTablePreviewRequest();
		body.setUploadFileHandleId("fileHandleId");
		status.setRequestBody(body);
		status.setStartedByUserId(userId);
		String jsonBody = EntityFactory.createJSONStringForEntity(body);
		when(mockMessage.getBody()).thenReturn(jsonBody);

		userInfo = new UserInfo(false, userId);
		when(mockUserManger.getUserInfo(userId)).thenReturn(userInfo);
		
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
	public void testProgressListenerRemoved() throws Throwable{
		worker.run(mockCallback, mockMessage);
		verify(mockCallback).addProgressListener(any(ProgressListener.class));
		verify(mockCallback).removeProgressListener(any(ProgressListener.class));
	}

}
