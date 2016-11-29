package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.StringInputStream;

/**
 * Unit test for the TableCSVAppenderWorker.
 * 
 * @author John
 *
 */
public class TableUploadManagerTest {
	
	@Mock
	ProgressCallback<Void> mockProgressCallback;
	@Mock
	TableManagerSupport mockTableManagerSupport;
	@Mock
	FileHandleManager  mockFileHandleManger;
	@Mock
	AmazonS3Client mockS3Client;
	@Mock
	UploadRowProcessor rowProcessor;
	
	TableUploadManagerImpl manager;
	S3FileHandle fileHandle;
	UserInfo user;
	UploadToTableRequest uploadRequest;
	List<ColumnModel> tableSchema;
	ObjectMetadata fileMetadata;
	S3Object s3Object;
	String csvString;
	List<SparseRowDto> rowsRead;
	
	@Before
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);
		// User
		user = new UserInfo(false);
		user.setId(999L);

		uploadRequest = new UploadToTableRequest();
		uploadRequest.setTableId("syn456");
		uploadRequest.setUploadFileHandleId("789");
		uploadRequest.setUpdateEtag("updateEtag");

		// FileHandle
		fileHandle = new S3FileHandle();
		fileHandle.setId(uploadRequest.getUploadFileHandleId());
		fileHandle.setBucketName("someBucket");
		fileHandle.setKey("someKey");
		fileHandle.setContentType("text/csv");
		// meta
		fileMetadata = new ObjectMetadata();
		// Object 
		s3Object = new S3Object();
		// schema
		tableSchema = TableModelTestUtils.createColumsWithNames("a","b","c");
		manager = new TableUploadManagerImpl();
		ReflectionTestUtils.setField(manager, "tableManagerSupport", mockTableManagerSupport);
		ReflectionTestUtils.setField(manager, "fileHandleManager", mockFileHandleManger);
		ReflectionTestUtils.setField(manager, "s3Client", mockS3Client);
		// Create the CSV
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "a", "b", "c" });
		input.add(new String[] { "CCC", null, "false" });
		input.add(new String[] { "FFF", "4", "true" });
		csvString = TableModelTestUtils.createCSVString(input);
		StringInputStream csvStream = new StringInputStream(csvString);
		s3Object.setObjectContent(new S3ObjectInputStream(csvStream, null));

		when(mockFileHandleManger.getRawFileHandle(user, uploadRequest.getUploadFileHandleId())).thenReturn(fileHandle);
		when(mockS3Client.getObjectMetadata(fileHandle.getBucketName(), fileHandle.getKey())).thenReturn(fileMetadata);
		when(mockS3Client.getObject(fileHandle.getBucketName(), fileHandle.getKey())).thenReturn(s3Object);
		when(mockTableManagerSupport.getColumnModelsForTable(uploadRequest.getTableId())).thenReturn(tableSchema);
		rowsRead = new LinkedList<SparseRowDto>();
		doAnswer(new Answer<String>(){
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				int count = 0;
				Iterator<SparseRowDto> it = (Iterator<SparseRowDto>) invocation.getArguments()[3];
				while(it.hasNext()){
					rowsRead.add(it.next());
					count++;
				}
				return "etag"+count;
			}}).when(rowProcessor).processRows(eq(user), eq(uploadRequest.getTableId()), eq(tableSchema), any(Iterator.class), eq(uploadRequest.getUpdateEtag()), eq(mockProgressCallback));
	}
	
	@Test
	public void testHappyCase() throws IOException{
		// call under test;
		UploadToTableResult results = manager.uploadCSV(mockProgressCallback, user, uploadRequest, rowProcessor);
		assertNotNull(results);
		assertEquals("etag2", results.getEtag());
		assertEquals(new Long(2), results.getRowsProcessed());
		assertEquals(2, rowsRead.size());
		verify(rowProcessor).processRows(eq(user), eq(uploadRequest.getTableId()), eq(tableSchema), any(Iterator.class), eq(uploadRequest.getUpdateEtag()), eq(mockProgressCallback));
	}
	

}
