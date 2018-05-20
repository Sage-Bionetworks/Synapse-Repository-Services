package org.sagebionetworks.table.worker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

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
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.s3.AmazonS3;
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
	private AmazonS3 mockS3Client;
	@Mock
	private ProgressCallback mockCallback;
	@Mock
	private Message mockMessage;
	@Mock
	private S3Object mockS3Ojbect;
	@Mock
	private S3ObjectInputStream mockInputStream;
	
	
	UploadToTablePreviewRequest body;
	
	TableCSVAppenderPreviewWorker worker;
	
	AsynchronousJobStatus status;
	S3FileHandle fileHandle;
	ObjectMetadata fileMetadata;
	
	@Before
	public void before() throws JSONObjectAdapterException{
		worker = new TableCSVAppenderPreviewWorker();
		ReflectionTestUtils.setField(worker, "asynchJobStatusManager", mockAsynchJobStatusManager);
		ReflectionTestUtils.setField(worker, "tableEntityManager", mockTableEntityManager);
		ReflectionTestUtils.setField(worker, "userManger", mockUserManger);
		ReflectionTestUtils.setField(worker, "fileHandleManager", mockFileHandleManager);
		ReflectionTestUtils.setField(worker, "s3Client", mockS3Client);
		
		status = new AsynchronousJobStatus();
		when(mockAsynchJobStatusManager.lookupJobStatus(anyString())).thenReturn(status);
		
		body = new UploadToTablePreviewRequest();
		status.setRequestBody(body);
		String jsonBody = EntityFactory.createJSONStringForEntity(body);
		when(mockMessage.getBody()).thenReturn(jsonBody);
		
		fileHandle = new S3FileHandle();
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
