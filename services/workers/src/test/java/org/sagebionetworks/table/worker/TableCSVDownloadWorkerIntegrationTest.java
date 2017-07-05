package org.sagebionetworks.table.worker;

import static org.junit.Assert.assertArrayEquals;
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
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.table.CSVToRowIterator;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import au.com.bytecode.opencsv.CSVReader;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableCSVDownloadWorkerIntegrationTest {

	// This test can be slow when run from outside of Amazon.
	public static final int MAX_WAIT_MS = 1000 * 60;
	
	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	StackConfiguration config;
	@Autowired
	FileHandleDao fileHandleDao;
	@Autowired
	EntityManager entityManager;
	@Autowired
	TableEntityManager tableEntityManager;
	@Autowired
	TableQueryManager tableQueryManger;
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
	ProgressCallback mockProgressCallback;
	
	@Before
	public void before() throws NotFoundException{
		// Only run this test if the table feature is enabled.
		Assume.assumeTrue(config.getTableEnabled());
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
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
		}
	}

	@Test
	public void testRoundTrip() throws Exception{
		List<String[]> input = createTable();
		
		String sql = "select * from "+tableId;
		// Wait for the table to be ready
		RowSet result = waitForConsistentQuery(adminUserInfo, sql);
		assertNotNull(result);
		// Now download the data from this table as a csv
		DownloadFromTableRequest request = new DownloadFromTableRequest();
		request.setSql(sql);
		request.setWriteHeader(true);
		request.setIncludeRowIdAndRowVersion(true);
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, request);
		// Wait for the job to complete.
		status = waitForStatus(status);
		assertNotNull(status);
		assertNotNull(status.getResponseBody());
		assertTrue(status.getResponseBody() instanceof DownloadFromTableResult);
		DownloadFromTableResult response = (DownloadFromTableResult) status.getResponseBody();
		assertNotNull(response.getEtag());
		assertNotNull(response.getResultsFileHandleId());
		assertEquals(tableId, response.getTableId());
		// Get the filehandle
		fileHandle = (S3FileHandle) fileHandleDao.get(response.getResultsFileHandleId());
		checkResults(fileHandle, input, true);
	}

	@Test
	public void testRoundTripSorted() throws Exception{
		List<String[]> input = createTable();
		
		String sql = "select * from "+tableId;
		// Wait for the table to be ready
		RowSet result = waitForConsistentQuery(adminUserInfo, sql);
		assertNotNull(result);
		// Now download the data from this table as a csv
		DownloadFromTableRequest request = new DownloadFromTableRequest();
		request.setSql(sql);
		request.setWriteHeader(true);
		request.setIncludeRowIdAndRowVersion(true);
		SortItem sortItem = new SortItem();
		sortItem.setColumn("c");
		sortItem.setDirection(SortDirection.DESC);
		request.setSort(Lists.newArrayList(sortItem));
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, request);
		// Wait for the job to complete.
		status = waitForStatus(status);
		assertNotNull(status);
		assertNotNull(status.getResponseBody());
		assertTrue(status.getResponseBody() instanceof DownloadFromTableResult);
		DownloadFromTableResult response = (DownloadFromTableResult) status.getResponseBody();
		assertNotNull(response.getEtag());
		assertNotNull(response.getResultsFileHandleId());
		assertEquals(tableId, response.getTableId());
		// Get the filehandle
		fileHandle = (S3FileHandle) fileHandleDao.get(response.getResultsFileHandleId());
		input = Lists.newArrayList(input.get(0), input.get(4), input.get(2), input.get(1), input.get(3));
		checkResults(fileHandle, input, true);
	}

	@Test
	public void testRoundTripWithZeroResults() throws Exception {
		createTable();

		String sql = "select * from " + tableId + " where a = 'xxxxxx'";
		// Wait for the table to be ready
		RowSet result = waitForConsistentQuery(adminUserInfo, sql);
		assertNotNull(result);
		// Now download the data from this table as a csv
		DownloadFromTableRequest request = new DownloadFromTableRequest();
		request.setSql(sql);
		request.setWriteHeader(true);
		request.setIncludeRowIdAndRowVersion(true);
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, request);
		// Wait for the job to complete.
		status = waitForStatus(status);
		assertNotNull(status);
		assertNotNull(status.getResponseBody());
		assertTrue(status.getResponseBody() instanceof DownloadFromTableResult);
		DownloadFromTableResult response = (DownloadFromTableResult) status.getResponseBody();
		assertNotNull(response.getEtag());
		assertNotNull(response.getResultsFileHandleId());
		assertEquals(tableId, response.getTableId());
		// Get the filehandle
		fileHandle = (S3FileHandle) fileHandleDao.get(response.getResultsFileHandleId());
		checkResults(fileHandle, Lists.<String[]> newArrayList(new String[] { "a", "b", "c" }), true);
	}

	@Test
	public void testDownloadWitoutWaitForSql() throws Exception {
		List<String[]> input = createTable();

		String sql = "select * from " + tableId;
		// download the data from this table as a csv
		DownloadFromTableRequest request = new DownloadFromTableRequest();
		request.setSql(sql);
		request.setWriteHeader(true);
		request.setIncludeRowIdAndRowVersion(false);
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, request);
		// Wait for the job to complete.
		status = waitForStatus(status);
		assertNotNull(status);
		assertNotNull(status.getResponseBody());
		assertTrue(status.getResponseBody() instanceof DownloadFromTableResult);
		DownloadFromTableResult response = (DownloadFromTableResult) status.getResponseBody();
		assertNotNull(response.getEtag());
		assertNotNull(response.getResultsFileHandleId());
		assertEquals(tableId, response.getTableId());
		// Get the filehandle
		fileHandle = (S3FileHandle) fileHandleDao.get(response.getResultsFileHandleId());
		checkResults(fileHandle, input, false);
	}

	private void checkResults(S3FileHandle fileHandle, List<String[]> input, boolean includeRowAndVersion) throws IOException,
			FileNotFoundException {
		CSVReader csvReader;
		List<String[]> results;
		assertEquals("text/csv", fileHandle.getContentType());
		assertNotNull(fileHandle.getFileName());
		assertNotNull(fileHandle.getContentMd5());
		// Download the file
		tempFile = File.createTempFile("DownloadCSV", ".csv");
		s3Client.getObject(new GetObjectRequest(fileHandle.getBucketName(), fileHandle.getKey()), tempFile);
		// Load the CSV data
		csvReader = new CSVReader(new FileReader(tempFile));
		results = null;
		try {
			results = csvReader.readAll();
		} finally {
			csvReader.close();
		}
		assertNotNull(results);
		assertEquals(input.size(), results.size());
		for (int i = 0; i < input.size(); i++) {
			assertArrayEquals(input.get(i), Arrays.copyOfRange(results.get(i), includeRowAndVersion ? 2 : 0, results.get(i).length));
		}
	}

	private List<String[]> createTable() throws NotFoundException, IOException {
		// Create one column of each type
		List<ColumnModel> temp = new LinkedList<ColumnModel>();
		temp.add(TableModelTestUtils.createColumn(0L, "a", ColumnType.STRING));
		temp.add(TableModelTestUtils.createColumn(0L, "b", ColumnType.INTEGER));
		temp.add(TableModelTestUtils.createColumn(0L, "c", ColumnType.DOUBLE));
		schema = new LinkedList<ColumnModel>();
		for (ColumnModel cm : temp) {
			cm = columnManager.createColumnModel(adminUserInfo, cm);
			schema.add(cm);
		}
		List<String> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(headers);
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		tableEntityManager.setTableSchema(adminUserInfo, headers, tableId);
		// Create some CSV data
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "a", "b", "c" });
		input.add(new String[] { "AAA", "2", "1.1" });
		input.add(new String[] { null, "3", "1.2" });
		input.add(new String[] { "FFF", "4", null });
		input.add(new String[] { "ZZZ", null, "1.3" });
		// This is the starting input stream
		CSVReader reader = TableModelTestUtils.createReader(input);
		// Write the CSV to the table
		CSVToRowIterator iterator = new CSVToRowIterator(schema, reader, true, null, null);
		tableEntityManager.appendRowsAsStream(adminUserInfo, tableId, schema, iterator,
				null, null, null);
		return input;
	}

	private RowSet waitForConsistentQuery(UserInfo user, String sql) throws Exception {
		long start = System.currentTimeMillis();
		while(true){
			try {
				return tableQueryManger.querySinglePage(mockProgressCallback, adminUserInfo, sql, null, null, 0L, 100L, true, false, false, true).getQueryResult().getQueryResults();
			}  catch (LockUnavilableException e) {
				System.out.println("Waiting for table lock: "+e.getLocalizedMessage());
			} catch (TableUnavailableException e) {
				System.out.println("Waiting for table index worker to build table. Status: "+e.getStatus());
			}
			assertTrue("Timed out waiting for table index worker to make the table available.", (System.currentTimeMillis()-start) <  MAX_WAIT_MS);
			Thread.sleep(1000);
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
