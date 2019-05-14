package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.FileConstants;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.FileProvider;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.StringInputStream;

/**
 * Unit test for the TableCSVAppenderWorker.
 * 
 * @author John
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TableUploadManagerTest {
	
	@Mock
	ProgressCallback mockProgressCallback;
	@Mock
	TableManagerSupport mockTableManagerSupport;
	@Mock
	FileHandleManager  mockFileHandleManger;
	@Mock
	SynapseS3Client mockS3Client;
	@Mock
	UploadRowProcessor rowProcessor;
	@Mock
	FileProvider mockFileProvider;
	@Mock
	File mockFile;
	
	@InjectMocks
	TableUploadManagerImpl manager;
	S3FileHandle fileHandle;
	UserInfo user;
	UploadToTableRequest uploadRequest;
	IdAndVersion idAndVersion;
	List<ColumnModel> tableSchema;
	ObjectMetadata fileMetadata;
	String csvString;
	List<SparseRowDto> rowsRead;
	
	@Before
	public void before() throws Exception {
		// User
		user = new UserInfo(false);
		user.setId(999L);

		uploadRequest = new UploadToTableRequest();
		uploadRequest.setTableId("syn456");
		uploadRequest.setUploadFileHandleId("789");
		uploadRequest.setUpdateEtag("updateEtag");
		idAndVersion = IdAndVersion.parse(uploadRequest.getTableId());

		// FileHandle
		fileHandle = new S3FileHandle();
		fileHandle.setId(uploadRequest.getUploadFileHandleId());
		fileHandle.setBucketName("someBucket");
		fileHandle.setKey("someKey");
		fileHandle.setContentType("text/csv");
		fileHandle.setContentSize(FileConstants.BULK_FILE_DOWNLOAD_MAX_SIZE_BYTES);
		// meta
		fileMetadata = new ObjectMetadata();
		// schema
		tableSchema = TableModelTestUtils.createColumsWithNames("a","b","c");
		// Create the CSV
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "a", "b", "c" });
		input.add(new String[] { "CCC", null, "false" });
		input.add(new String[] { "FFF", "4", "true" });
		csvString = TableModelTestUtils.createCSVString(input);
		StringInputStream csvStream = new StringInputStream(csvString);
		
		when(mockFileProvider.createTempFile(anyString(), anyString())).thenReturn(mockFile);
		when(mockFileProvider.createFileInputStream(any(File.class))).thenReturn(csvStream);

		when(mockFileHandleManger.getRawFileHandle(user, uploadRequest.getUploadFileHandleId())).thenReturn(fileHandle);
		when(mockS3Client.getObjectMetadata(fileHandle.getBucketName(), fileHandle.getKey())).thenReturn(fileMetadata);
		when(mockTableManagerSupport.getColumnModelsForTable(idAndVersion)).thenReturn(tableSchema);
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
		verify(mockFile).delete();
	}
	
	@Test
	public void testFileOverSizeLimit() {
		// file over size
		fileHandle.setContentSize(FileConstants.BULK_FILE_DOWNLOAD_MAX_SIZE_BYTES+1);
		// call under test;
		try {
			manager.uploadCSV(mockProgressCallback, user, uploadRequest, rowProcessor);
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains(""+FileConstants.MAX_FILE_SIZE_GB));
		}
	}
	
	@Test
	public void testTempDeletedOnFailure() throws DatastoreException, NotFoundException, IOException {
		// setup a failure
		IllegalArgumentException wentWrong = new IllegalArgumentException("Something went wrong");
		when(rowProcessor.processRows(eq(user), eq(uploadRequest.getTableId()), anyListOf(ColumnModel.class), any(Iterator.class), anyString(), eq(mockProgressCallback))).thenThrow(wentWrong);
		// call under test;
		try {
			manager.uploadCSV(mockProgressCallback, user, uploadRequest, rowProcessor);
			fail();
		} catch (IllegalArgumentException e) {
			// expected
		}
		// the temp file should still be deleted.
		verify(mockFile).delete();
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testFileContentSizeNull() {
		fileHandle.setContentSize(null);
		// call under test
		manager.uploadCSV(mockProgressCallback, user, uploadRequest, rowProcessor);
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
		
		when(mockFileProvider.createTempFile(anyString(), anyString())).thenReturn(mockFile);
		when(mockFileProvider.createFileInputStream(any(File.class))).thenReturn(csvStream);
		
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(false);
		uploadRequest.setCsvTableDescriptor(descriptor);
		uploadRequest.setLinesToSkip(1L);
		
		when(mockFileHandleManger.getRawFileHandle(user, uploadRequest.getUploadFileHandleId())).thenReturn(fileHandle);
		when(mockS3Client.getObjectMetadata(fileHandle.getBucketName(), fileHandle.getKey())).thenReturn(fileMetadata);
		when(mockTableManagerSupport.getColumnModelsForTable(idAndVersion)).thenReturn(tableSchema);
		
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
