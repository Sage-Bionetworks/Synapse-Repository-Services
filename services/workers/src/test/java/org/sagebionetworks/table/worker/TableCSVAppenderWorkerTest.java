package org.sagebionetworks.table.worker;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.AsynchUploadJobBody;
import org.sagebionetworks.repo.model.table.ColumnModel;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.model.Message;

/**
 * Unit test for the TableCSVAppenderWorker.
 * 
 * @author John
 *
 */
public class TableCSVAppenderWorkerTest {
	
	AsynchJobStatusManager mockAasynchJobStatusManager;
	TableRowManager mockTableRowManager;
	FileHandleManager  mockFileHandleManger;
	UserManager mockUserManager;
	AmazonS3Client mockS3Client;
	List<Message> messageList;
	AsynchronousJobStatus status;
	TableCSVAppenderWorker worker;
	S3FileHandle fileHandle;
	UserInfo user;
	List<ColumnModel> tableSchema;
	ObjectMetadata fileMetadata;
	S3Object s3Object;
	
	@Before
	public void before() throws Exception {
		mockAasynchJobStatusManager = Mockito.mock(AsynchJobStatusManager.class);
		mockTableRowManager = Mockito.mock(TableRowManager.class);
		mockFileHandleManger = Mockito.mock(FileHandleManager.class);
		mockUserManager = Mockito.mock(UserManager.class);
		mockS3Client = Mockito.mock(AmazonS3Client.class);
		messageList = new LinkedList<Message>();
		// User
		user = new UserInfo(false);
		user.setId(999L);
		// Message
		status = new AsynchronousJobStatus();
		status.setChangedOn(new Date());
		status.setJobId("123");
		status.setStartedByUserId(user.getId());
		AsynchUploadJobBody body = new AsynchUploadJobBody();
		body.setTableId("syn456");
		body.setUploadFileHandleId("789");
		status.setJobBody(body);

		// FileHandle
		fileHandle = new S3FileHandle();
		fileHandle.setId(body.getUploadFileHandleId());
		fileHandle.setBucketName("someBucket");
		fileHandle.setKey("someKey");
		// meta
		fileMetadata = new ObjectMetadata();
		// Object 
		s3Object = new S3Object();
		// schema
		tableSchema = TableModelTestUtils.createColumsWithNames("a","b","c");
		worker = new TableCSVAppenderWorker(mockAasynchJobStatusManager, mockTableRowManager, mockFileHandleManger, mockUserManager, mockS3Client, messageList);

		when(mockUserManager.getUserInfo(user.getId())).thenReturn(user);
		when(mockFileHandleManger.getRawFileHandle(user, body.getUploadFileHandleId())).thenReturn(fileHandle);
		when(mockS3Client.getObjectMetadata(fileHandle.getBucketName(), fileHandle.getKey())).thenReturn(fileMetadata);
		when(mockS3Client.getObject(fileHandle.getBucketName(), fileHandle.getKey())).thenReturn(s3Object);
		when(mockTableRowManager.getColumnModelsForTable(body.getTableId())).thenReturn(tableSchema);
	}

	
	@Test
	public void testContentTypeUnknown() throws Exception{
		messageList.add(MessageUtils.buildMessage(status));
		List<Message> resutls = worker.call();
		assertNotNull(resutls);
	}
}
