package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

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
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.s3.AmazonS3;
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
	ProgressCallback mockProgressCallback;
	@Mock
	TableManagerSupport mockTableManagerSupport;
	@Mock
	FileHandleManager  mockFileHandleManger;
	@Mock
	AmazonS3 mockS3Client;
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
		doAnswer(new Answer<TableUpdateResponse>(){
			@Override
			public TableUpdateResponse answer(InvocationOnMock invocation) throws Throwable {
				UploadToTableResult result = new UploadToTableResult();
				int count = 0;
				Iterator<SparseRowDto> it = (Iterator<SparseRowDto>) invocation.getArguments()[3];
				while(it.hasNext()){
					rowsRead.add(it.next());
					count++;
				}
				result.setEtag("etag"+count);
				result.setRowsProcessed(new Long(count));
				return result;
			}}).when(rowProcessor).processRows(eq(user), eq(uploadRequest.getTableId()), anyListOf(ColumnModel.class), any(Iterator.class), anyString(), eq(mockProgressCallback));
	}
	
	@Test
	public void testHappyCase() throws IOException{
		// call under test;
		TableUpdateResponse results = manager.uploadCSV(mockProgressCallback, user, uploadRequest, rowProcessor);
		assertNotNull(results);
		assertTrue(results instanceof UploadToTableResult);
		UploadToTableResult uploadResult = (UploadToTableResult)results;
		assertEquals("etag2", uploadResult.getEtag());
		assertEquals(new Long(2), uploadResult.getRowsProcessed());
		assertEquals(2, rowsRead.size());
		verify(rowProcessor).processRows(eq(user), eq(uploadRequest.getTableId()), eq(tableSchema), any(Iterator.class), eq(uploadRequest.getUpdateEtag()), eq(mockProgressCallback));
	}
	
	/**
	 * A case where the first line is skipped.
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3155() throws Exception{
		// Setup a column with the name foo.
		tableSchema = TableModelTestUtils.createColumsWithNames("foo");
		String columnId = tableSchema.get(0).getId();
		// Create the CSV
		List<String[]> input = new ArrayList<String[]>(3);
		/*
		 * The first row is a header but the name 'bar' does not match 'foo'.
		 * For this case the caller will set firstLineHeader=false and
		 * linesToSkip=1. We still need to read the file correctly which
		 * includes detecting ROW_ID and ROW_VERSION and mapping the column
		 * 'bar' to 'foo' by column index.
		 */
		input.add(new String[] { ROW_ID, ROW_VERSION, "bar" });
		input.add(new String[] { "1", "10", "a" });
		input.add(new String[] { "2", "10", "b" });
		csvString = TableModelTestUtils.createCSVString(input);
		StringInputStream csvStream = new StringInputStream(csvString);
		s3Object.setObjectContent(new S3ObjectInputStream(csvStream, null));
		
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(false);
		uploadRequest.setCsvTableDescriptor(descriptor);
		uploadRequest.setLinesToSkip(1L);
		
		when(mockFileHandleManger.getRawFileHandle(user, uploadRequest.getUploadFileHandleId())).thenReturn(fileHandle);
		when(mockS3Client.getObjectMetadata(fileHandle.getBucketName(), fileHandle.getKey())).thenReturn(fileMetadata);
		when(mockS3Client.getObject(fileHandle.getBucketName(), fileHandle.getKey())).thenReturn(s3Object);
		when(mockTableManagerSupport.getColumnModelsForTable(uploadRequest.getTableId())).thenReturn(tableSchema);
		
		// call under test;
		TableUpdateResponse results = manager.uploadCSV(mockProgressCallback, user, uploadRequest, rowProcessor);
		assertNotNull(results);
		assertTrue(results instanceof UploadToTableResult);
		UploadToTableResult uploadResult = (UploadToTableResult)results;
		assertEquals("etag2", uploadResult.getEtag());
		assertEquals(new Long(2), uploadResult.getRowsProcessed());
		assertEquals(2, rowsRead.size());
		// one
		SparseRowDto one = rowsRead.get(0);
		assertEquals(new Long(1), one.getRowId());
		assertEquals(new Long(10), one.getVersionNumber());
		assertNotNull(one.getValues());
		assertEquals("a",one.getValues().get(columnId));
		// two
		SparseRowDto two = rowsRead.get(1);
		assertEquals(new Long(2), two.getRowId());
		assertEquals(new Long(10), two.getVersionNumber());
		assertNotNull(two.getValues());
		assertEquals("b",two.getValues().get(columnId));
	}
	

}
