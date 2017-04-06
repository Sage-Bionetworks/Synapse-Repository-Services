package org.sagebionetworks.table.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.google.common.collect.Lists;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableCSVAppenderWorkerIntegrationTest {

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
	ColumnModelManager columnManager;
	@Autowired
	UserManager userManager;
	@Autowired
	AmazonS3Client s3Client;
	@Autowired
	SemaphoreManager semphoreManager;
	@Autowired
	private IdGenerator idGenerator;
	private UserInfo adminUserInfo;
	RowReferenceSet referenceSet;
	List<ColumnModel> schema;
	List<String> headers;
	private String tableId;
	private List<String> toDelete = Lists.newArrayList();
	private List<File> tempFiles = Lists.newArrayList();
	private List<S3FileHandle> fileHandles = Lists.newArrayList();

	@Before
	public void before() throws NotFoundException {
		// Only run this test if the table feature is enabled.
		Assume.assumeTrue(config.getTableEnabled());
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
		// Start with an empty queue.
		asynchJobStatusManager.emptyAllQueues();
		// Get the admin user
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		this.schema = new LinkedList<ColumnModel>();
	}

	@After
	public void after() {
		if (config.getTableEnabled()) {
			if (adminUserInfo != null) {
				for (String id : toDelete) {
					try {
						entityManager.deleteEntity(adminUserInfo, id);
					} catch (Exception e) {
					}
				}
			}
			for (File tempFile : tempFiles)
				tempFile.delete();
		}
		for (S3FileHandle fileHandle : fileHandles) {
			s3Client.deleteObject(fileHandle.getBucketName(), fileHandle.getKey());
			fileHandleDao.delete(fileHandle.getId());
		}
	}

	@Test
	public void testRoundTripWithCSVHeaders() throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException,
			IOException, InterruptedException {
		doTestRoundTrip(true);
	}

	@Test
	public void testRoundTripWithColumnIds() throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException,
			IOException, InterruptedException {
		doTestRoundTrip(false);
	}

	private void doTestRoundTrip(boolean useCSVHeader) throws DatastoreException, InvalidModelException, UnauthorizedException,
			NotFoundException, IOException,
			InterruptedException {
		// Create a few columns
		// String
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.STRING);
		cm.setName("somestrings");
		cm = columnManager.createColumnModel(adminUserInfo, cm);
		this.schema.add(cm);
		// integer
		cm = new ColumnModel();
		cm.setColumnType(ColumnType.INTEGER);
		cm.setName("someinteger");
		cm = columnManager.createColumnModel(adminUserInfo, cm);
		schema.add(cm);
		headers = TableModelUtils.getIds(schema);

		// create the table
		createTableWithSchema();

		// Create a CSV file to upload
		File tempFile = File.createTempFile("TableCSVAppenderWorkerIntegrationTest", ".csv");
		tempFiles.add(tempFile);
		CSVWriter csv = new CSVWriter(new FileWriter(tempFile));
		int rowCount = 100;
		try {
			if (useCSVHeader) {
				// Write the header
				csv.writeNext(new String[] { schema.get(1).getName(), schema.get(0).getName() });
			}
			// Write some rows
			for (int i = 0; i < rowCount; i++) {
				csv.writeNext(new String[] { "" + i, "stringdata" + i });
			}
		} finally {
			csv.close();
		}
		S3FileHandle fileHandle = uploadFile(tempFile);
		// We are now ready to start the job
		UploadToTableRequest body = new UploadToTableRequest();
		body.setTableId(tableId);
		body.setUploadFileHandleId(fileHandle.getId());
		body.setEntityId(tableId);
		CsvTableDescriptor csvTableDescriptor = new CsvTableDescriptor();
		csvTableDescriptor.setIsFirstLineHeader(useCSVHeader);
		body.setCsvTableDescriptor(csvTableDescriptor);
		if (!useCSVHeader) {
			body.setColumnIds(Lists.newArrayList(schema.get(1).getId(), schema.get(0).getId()));
		}
		
		TableUpdateTransactionRequest txRequest = TableModelUtils.wrapInTransactionRequest(body);
		
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, txRequest);
		// Wait for the job to complete.
		status = waitForStatus(adminUserInfo, status);
		assertNotNull(status);
		UploadToTableResult response = TableModelUtils.extractResponseFromTransaction(status.getResponseBody(), UploadToTableResult.class);
		assertNotNull(response.getEtag());
		assertEquals(new Long(rowCount), response.getRowsProcessed());
		// There should be one change set applied to the table
		List<TableRowChange> changes = this.tableEntityManager.listRowSetsKeysForTable(tableId);
		assertNotNull(changes);
		assertEquals(1, changes.size());
		TableRowChange change = changes.get(0);
		assertEquals(new Long(rowCount), change.getRowCount());
		// the etag of the change should match what the job returned.
		assertEquals(change.getEtag(), response.getEtag());
	}

	@Test
	public void testUpdateRoundTrip() throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException,
			IOException, InterruptedException {
		// Create a few columns
		// String
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.STRING);
		cm.setName("somestrings");
		cm = columnManager.createColumnModel(adminUserInfo, cm);
		this.schema.add(cm);
		// integer
		cm = new ColumnModel();
		cm.setColumnType(ColumnType.INTEGER);
		cm.setName("someinteger");
		cm = columnManager.createColumnModel(adminUserInfo, cm);
		schema.add(cm);
		// create the table.
		createTableWithSchema();

		// Create a CSV file to upload
		File tempFile = File.createTempFile("TableCSVAppenderWorkerIntegrationTest", ".csv");
		tempFiles.add(tempFile);
		CSVWriter csv = new CSVWriter(new FileWriter(tempFile));
		int rowCount = 100;
		try {
			// Write the header
			csv.writeNext(new String[] { schema.get(1).getName(), schema.get(0).getName() });
			// Write some rows
			for (int i = 0; i < rowCount; i++) {
				csv.writeNext(new String[] { "" + i, "stringdata" + i });
			}
		} finally {
			csv.close();
		}
		S3FileHandle fileHandle = uploadFile(tempFile);
		// We are now ready to start the job
		UploadToTableRequest body = new UploadToTableRequest();
		body.setTableId(tableId);
		body.setEntityId(tableId);
		body.setUploadFileHandleId(fileHandle.getId());
		System.out.println("Inserting");
		TableUpdateTransactionRequest txRequest = TableModelUtils.wrapInTransactionRequest(body);
		
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, txRequest);
		// Wait for the job to complete.
		status = waitForStatus(adminUserInfo, status);
		// download the csv
		DownloadFromTableRequest download = new DownloadFromTableRequest();
		download.setSql("select somestrings, someinteger from " + tableId);
		download.setIncludeRowIdAndRowVersion(true);
		download.setCsvTableDescriptor(new CsvTableDescriptor());
		download.getCsvTableDescriptor().setIsFirstLineHeader(true);
		System.out.println("Downloading");
		status = asynchJobStatusManager.startJob(adminUserInfo, download);
		status = waitForStatus(adminUserInfo, status);
		DownloadFromTableResult downloadResult = (DownloadFromTableResult) status.getResponseBody();
		S3FileHandle resultFile = (S3FileHandle) fileHandleDao.get(downloadResult.getResultsFileHandleId());
		fileHandles.add(resultFile);
		tempFile = File.createTempFile("DownloadCSV", ".csv");
		tempFiles.add(tempFile);
		s3Client.getObject(new GetObjectRequest(resultFile.getBucketName(), resultFile.getKey()), tempFile);
		// Load the CSV data
		CSVReader csvReader = new CSVReader(new FileReader(tempFile));
		List<String[]> results = csvReader.readAll();
		csvReader.close();

		// modify it
		int i = 3000;
		for (String[] row : results.subList(1, results.size())) {
			assertEquals(4, row.length);
			row[2] += "-changed" + i++;
		}

		tempFile = File.createTempFile("TableCSVAppenderWorkerIntegrationTest2", ".csv");
		tempFiles.add(tempFile);
		csv = new CSVWriter(new FileWriter(tempFile));
		for (String[] row : results) {
			csv.writeNext(row);
		}
		csv.close();

		fileHandle = uploadFile(tempFile);
		// We are now ready to start the job
		body = new UploadToTableRequest();
		body.setTableId(tableId);
		body.setEntityId(tableId);
		body.setUploadFileHandleId(fileHandle.getId());
		System.out.println("Appending");
		txRequest = TableModelUtils.wrapInTransactionRequest(body);
		status = asynchJobStatusManager.startJob(adminUserInfo, txRequest);
		// Wait for the job to complete.
		status = waitForStatus(adminUserInfo, status);

		// There should be two change sets applied to the table
		List<TableRowChange> changes = this.tableEntityManager.listRowSetsKeysForTable(tableId);
		assertNotNull(changes);
		assertEquals(2, changes.size());
	}
	
	/**
	 * A complex use case is needed to verify that both PLFM-3155 and PLFM-3355 are fixed.
	 * For this use case a CSV that includes both ROW_ID and ROW_VERSION is used to create 
	 * a new table.  The CSV contains one column of actual string data with a header named 'bar'.
	 * The new table table has a single string column named 'foo'.  Since the column name
	 * of the table and the CSV header do not match, the CSV will be uploaded with
	 * linesToSkip=1 and isFirstLineHeader=false.  The expected result is all data from
	 * the 'bar' column in the CSV is successfully added to the 'foo' column of the
	 * table.
	 */
	@Test
	public void testPLFM_3155andPLFM_3354() throws Exception {
		// the column name is 'foo;
		ColumnModel column = TableModelTestUtils.createColumn(null, "foo", ColumnType.STRING);
		column.setMaximumSize(2L);
		column = columnManager.createColumnModel(adminUserInfo, column);
		schema = Lists.newArrayList(column);
		// create the table
		createTableWithSchema();
		
		// Create a CSV file to upload
		File tempFile = File.createTempFile("TestPLFM_3155andPLFM_3354", ".csv");
		tempFiles.add(tempFile);
		CSVWriter csv = new CSVWriter(new FileWriter(tempFile));
		int rowCount = 10;
		try {
			// the header name is 'bar'
			csv.writeNext(new String[] { TableConstants.ROW_ID, TableConstants.ROW_VERSION, "bar"});
			// Write some rows
			for (int i = 0; i < rowCount; i++) {
				csv.writeNext(new String[] { "" + (100+i), "99","d" + i });
			}
		} finally {
			csv.close();
		}
		S3FileHandle fileHandle = uploadFile(tempFile);
		// Upload the CSV to the table
		UploadToTableRequest body = new UploadToTableRequest();
		body.setTableId(tableId);
		body.setEntityId(tableId);
		body.setUploadFileHandleId(fileHandle.getId());
		// skip the first line
		body.setLinesToSkip(1L);
		CsvTableDescriptor csvDescriptor = new CsvTableDescriptor();
		// the first 
		csvDescriptor.setIsFirstLineHeader(false);
		body.setCsvTableDescriptor(csvDescriptor);
		TableUpdateTransactionRequest txRequest = TableModelUtils.wrapInTransactionRequest(body);
		
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, txRequest);
		// Wait for the job to complete.
		status = waitForStatus(adminUserInfo, status);
		assertNotNull(status);
		UploadToTableResult response = TableModelUtils.extractResponseFromTransaction(status.getResponseBody(), UploadToTableResult.class);
		assertNotNull(response.getEtag());
		assertEquals(new Long(rowCount), response.getRowsProcessed());
	}
	
	/**
	 * Create a table using the schema.
	 * 
	 */
	void createTableWithSchema() {
		List<String> ids = TableModelUtils.getIds(schema);
		headers = ids;

		// Create the table
		TableEntity table = new TableEntity();
		table.setColumnIds(headers);
		table.setName(UUID.randomUUID().toString());
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		toDelete.add(tableId);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		tableEntityManager.setTableSchema(adminUserInfo, headers, tableId);
	}

	private AsynchronousJobStatus waitForStatus(UserInfo user, AsynchronousJobStatus status) throws InterruptedException, DatastoreException,
			NotFoundException {
		long start = System.currentTimeMillis();
		while (!AsynchJobState.COMPLETE.equals(status.getJobState())) {
			assertFalse("Job Failed: " + status.getErrorDetails(), AsynchJobState.FAILED.equals(status.getJobState()));
			assertTrue("Timed out waiting for table status", (System.currentTimeMillis() - start) < MAX_WAIT_MS);
			Thread.sleep(1000);
			// Get the status again
			status = this.asynchJobStatusManager.getJobStatus(user, status.getJobId());
		}
		return status;
	}
	
	/**
	 * Upload the given file to S3 and create a S3FileHandle for it.
	 * 
	 * @param tempFile
	 * @return
	 */
	private S3FileHandle uploadFile(File tempFile){
		S3FileHandle fileHandle = new S3FileHandle();
		fileHandle.setBucketName(StackConfiguration.getS3Bucket());
		fileHandle.setKey(UUID.randomUUID() + ".csv");
		fileHandle.setContentType("text/csv");
		fileHandle.setCreatedBy("" + adminUserInfo.getId());
		fileHandle.setFileName(tempFile.getName());
		fileHandle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		fileHandle.setEtag(UUID.randomUUID().toString());
		fileHandle.setPreviewId(fileHandle.getId());
		// Upload the File to S3
		fileHandle = (S3FileHandle) fileHandleDao.createFile(fileHandle);
		fileHandles.add(fileHandle);
		// Upload the file to S3.
		s3Client.putObject(fileHandle.getBucketName(), fileHandle.getKey(), tempFile);
		return fileHandle;
	}
}
