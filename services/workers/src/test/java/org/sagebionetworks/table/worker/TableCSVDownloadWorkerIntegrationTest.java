package org.sagebionetworks.table.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.table.CSVToRowIterator;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.AsynchDownloadRequestBody;
import org.sagebionetworks.repo.model.table.AsynchDownloadResponseBody;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.csv.CsvNullReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableCSVDownloadWorkerIntegrationTest {

	// This test can be slow when run from outside of Amazon.
	public static final int MAX_WAIT_MS = 1000*60*3;
	
	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	StackConfiguration config;
	@Autowired
	FileHandleDao fileHandleDao;
	@Autowired
	EntityManager entityManager;
	@Autowired
	TableRowManager tableRowManager;
	@Autowired
	ColumnModelManager columnManager;
	@Autowired
	UserManager userManager;
	@Autowired
	AmazonS3Client s3Client;
	@Autowired
	SemaphoreManager semphoreManager;
	
	private UserInfo adminUserInfo;
	RowReferenceSet referenceSet;
	List<ColumnModel> schema;
	List<String> headers;
	private String tableId;
	private List<String> toDelete;
	private File tempFile;
	S3FileHandle fileHandle;
	S3FileHandle fileHandle2;
	
	@Before
	public void before() throws NotFoundException{
		// Only run this test if the table feature is enabled.
		Assume.assumeTrue(config.getTableEnabled());
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
		// Start with an empty queue.
		asynchJobStatusManager.emptyAllQueues();
		// Get the admin user
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		toDelete = new LinkedList<String>();
	}
	
	@After
	public void after(){
		if(config.getTableEnabled()){
			if(adminUserInfo != null){
				for(String id: toDelete){
					try {
						entityManager.deleteEntity(adminUserInfo, id);
					} catch (Exception e) {}
				}
			}
			if(tempFile != null){
				tempFile.delete();
			}
			if(fileHandle != null){
				s3Client.deleteObject(fileHandle.getBucketName(), fileHandle.getKey());
				fileHandleDao.delete(fileHandle.getId());
			}
			if (fileHandle2 != null) {
				s3Client.deleteObject(fileHandle.getBucketName(), fileHandle.getKey());
				fileHandleDao.delete(fileHandle.getId());
			}
		}
	}

	@Test
	public void testRoundTrip() throws Exception{
		List<String[]> input = createTable();
		
		String sql = "select * from "+tableId;
		// Wait for the table to be ready
		RowSet result = waitForConsistentQuery(adminUserInfo, sql+" limit 100");
		assertNotNull(result);
		// Now download the data from this table as a csv
		AsynchDownloadRequestBody request = new AsynchDownloadRequestBody();
		request.setSql(sql);
		request.setWriteHeader(true);
		request.setIncludeRowIdAndRowVersion(false);
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, request);
		// Wait for the job to complete.
		status = waitForStatus(status);
		assertNotNull(status);
		assertNotNull(status.getResponseBody());
		assertTrue(status.getResponseBody() instanceof AsynchDownloadResponseBody);
		AsynchDownloadResponseBody response = (AsynchDownloadResponseBody) status.getResponseBody();
		assertNotNull(response.getEtag());
		assertNotNull(response.getResultsFileHandleId());
		assertEquals(tableId, response.getTableId());
		// Get the filehandle
		fileHandle = (S3FileHandle) fileHandleDao.get(response.getResultsFileHandleId());
		checkResults(fileHandle, input);
	}

	@Test
	public void test1() throws Exception {
		testDownloadToFile();
	}

	@Test
	public void test2() throws Exception {
		testDownloadToFile();
	}

	@Test
	public void test3() throws Exception {
		testDownloadToFile();
	}

	@Test
	public void test4() throws Exception {
		testDownloadToFile();
	}

	@Test
	public void test5() throws Exception {
		testDownloadToFile();
	}

	@Test
	public void test6() throws Exception {
		testDownloadToFile();
	}
	@Test
	public void testDownloadToFile() throws Exception {
		List<String[]> input = createTable();

		// Now download the data from this table as a csv
		AsynchDownloadRequestBody request = new AsynchDownloadRequestBody();
		String sql = "select * from "+tableId;
		request.setSql(sql);
		request.setWriteHeader(true);
		request.setIncludeRowIdAndRowVersion(false);
		request.setFileName("newfile");
		request.setFileParentId(tableId);
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, request);
		// Wait for the job to complete.
		status = waitForStatus(status);
		assertNotNull(status);
		assertNotNull(status.getResponseBody());
		assertTrue(status.getResponseBody() instanceof AsynchDownloadResponseBody);
		AsynchDownloadResponseBody response = (AsynchDownloadResponseBody) status.getResponseBody();
		assertNotNull(response.getEtag());
		assertNotNull(response.getResultsFileHandleId());
		assertNotNull(response.getResultsFileEntityId());
		assertEquals(tableId, response.getTableId());

		FileEntity fileEntity=	entityManager.getEntity(adminUserInfo, response.getResultsFileEntityId(), FileEntity.class);
		toDelete.add(fileEntity.getId());
		// Get the filehandle
		fileHandle = (S3FileHandle) fileHandleDao.get(fileEntity.getDataFileHandleId());

		// try again with update
		String[] extraData = new String[] { "FFF", "55", "55.1" };
		input.add(extraData);
		// This is the starting input stream
		CsvNullReader reader = TableModelTestUtils.createReader(Lists.<String[]> newArrayList(extraData));
		// Write the CSV to the table
		CSVToRowIterator iterator = new CSVToRowIterator(schema, reader);
		tableRowManager.appendRowsAsStream(adminUserInfo, tableId, schema, iterator, null, null);
		status = asynchJobStatusManager.startJob(adminUserInfo, request);
		// Wait for the job to complete.
		status = waitForStatus(status);
		response = (AsynchDownloadResponseBody) status.getResponseBody();

		fileEntity = entityManager.getEntity(adminUserInfo, response.getResultsFileEntityId(), FileEntity.class);
		toDelete.add(fileEntity.getId());
		// Get the filehandle
		fileHandle2 = (S3FileHandle) fileHandleDao.get(fileEntity.getDataFileHandleId());
		checkResults(fileHandle2, input);
	}

	private void checkResults(S3FileHandle fileHandle, List<String[]> input) throws IOException, FileNotFoundException {
		CsvNullReader csvReader;
		List<String[]> results;
		assertEquals("text/csv", fileHandle.getContentType());
		assertNotNull(fileHandle.getFileName());
		assertNotNull(fileHandle.getContentMd5());
		// Download the file
		tempFile = File.createTempFile("DownloadCSV", ".csv");
		s3Client.getObject(new GetObjectRequest(fileHandle.getBucketName(), fileHandle.getKey()), tempFile);
		// Load the CSV data
		csvReader = new CsvNullReader(new FileReader(tempFile));
		results = null;
		try {
			results = csvReader.readAll();
		} finally {
			csvReader.close();
		}
		assertNotNull(results);
		assertEquals(input.size(), results.size());
		for (int i = 0; i < input.size(); i++) {
			assertEquals(Arrays.toString(input.get(i)), Arrays.toString(results.get(i)));
		}
	}

	private List<String[]> createTable() throws NotFoundException, IOException {
		// Create one column of each type
		List<ColumnModel> temp = new LinkedList<ColumnModel>();
		temp.add(TableModelTestUtils.createColumn(0L, "a", ColumnType.STRING));
		temp.add(TableModelTestUtils.createColumn(0L, "b", ColumnType.LONG));
		temp.add(TableModelTestUtils.createColumn(0L, "c", ColumnType.DOUBLE));
		schema = new LinkedList<ColumnModel>();
		for (ColumnModel cm : temp) {
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		List<String> headers = TableModelUtils.getHeaders(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(headers);
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, headers, tableId, true);
		// Create some CSV data
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "a", "b", "c" });
		input.add(new String[] { "AAA", "2", "1.1" });
		input.add(new String[] { null, "3", "1.2" });
		input.add(new String[] { "FFF", "4", null });
		input.add(new String[] { "ZZZ", null, "1.3" });
		// This is the starting input stream
		CsvNullReader reader = TableModelTestUtils.createReader(input);
		// Write the CSV to the table
		CSVToRowIterator iterator = new CSVToRowIterator(schema, reader);
		tableRowManager.appendRowsAsStream(adminUserInfo, tableId, schema, iterator, null, null);
		return input;
	}

	private RowSet waitForConsistentQuery(UserInfo user, String sql) throws DatastoreException, NotFoundException, InterruptedException{
		long start = System.currentTimeMillis();
		while(true){
			try {
				return  tableRowManager.query(adminUserInfo, sql, true, false);
			} catch (TableUnavilableException e) {
				assertTrue("Timed out waiting for table index worker to make the table available.", (System.currentTimeMillis()-start) <  MAX_WAIT_MS);
				assertNotNull(e.getStatus());
				assertFalse("Failed: "+e.getStatus().getErrorMessage(),TableState.PROCESSING_FAILED.equals(e.getStatus().getState()));
				System.out.println("Waiting for table index worker to build table. Status: "+e.getStatus());
				Thread.sleep(1000);
			}
		}
	}
	
	private AsynchronousJobStatus waitForStatus(AsynchronousJobStatus status) throws InterruptedException, DatastoreException, NotFoundException{
		long start = System.currentTimeMillis();
		while(!AsynchJobState.COMPLETE.equals(status.getJobState())){
			assertFalse("Job Failed: "+status.getErrorDetails(), AsynchJobState.FAILED.equals(status.getJobState()));
			System.out.println("Waiting for job to complete: Message: "+status.getProgressMessage()+" progress: "+status.getProgressCurrent()+"/"+status.getProgressTotal());
			assertTrue("Timed out waiting for table status",(System.currentTimeMillis()-start) < MAX_WAIT_MS);
			Thread.sleep(1000);
			// Get the status again 
			status = this.asynchJobStatusManager.getJobStatus(adminUserInfo, status.getJobId());
		}
		return status;
	}
	
}
