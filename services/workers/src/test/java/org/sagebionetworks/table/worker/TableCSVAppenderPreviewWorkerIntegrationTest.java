package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import au.com.bytecode.opencsv.CSVWriter;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableCSVAppenderPreviewWorkerIntegrationTest {
	
	public static final int MAX_WAIT_MS = 1000 * 60;
	
	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	StackConfiguration config;
	@Autowired
	FileHandleDao fileHandleDao;
	@Autowired
	ColumnModelManager columnManager;
	@Autowired
	UserManager userManager;
	@Autowired
	SynapseS3Client s3Client;
	@Autowired
	SemaphoreManager semphoreManager;
	@Autowired
	private IdGenerator idGenerator;
	
	private UserInfo adminUserInfo;
	RowReferenceSet referenceSet;
	List<ColumnModel> schema;
	List<String> headers;
	private File tempFile;
	S3FileHandle fileHandle;
	
	@BeforeEach
	public void before() throws NotFoundException{
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
		// Start with an empty queue.
		asynchJobStatusManager.emptyAllQueues();
		// Get the admin user
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}
	
	@AfterEach
	public void after(){
		if(tempFile != null){
			tempFile.delete();
		}
		if(fileHandle != null){
			s3Client.deleteObject(fileHandle.getBucketName(), fileHandle.getKey());
			fileHandleDao.delete(fileHandle.getId());
		}
	}

	@Test
	public void testRoundTrip() throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, IOException, InterruptedException{
		this.schema = new LinkedList<ColumnModel>();
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
		
		// Create a CSV file to upload
		this.tempFile = File.createTempFile("TableCSVAppenderPreviewWorkerIntegrationTest", ".csv");
		CSVWriter csv = new CSVWriter(new FileWriter(tempFile));
		int rowCount = 100;
		try{
			// Write the header
			csv.writeNext(new String[]{schema.get(1).getName(), schema.get(0).getName()});
			// Write some rows
			for(int i=0; i<rowCount; i++){
				csv.writeNext(new String[]{""+i, "stringdata"+i});
			}
		}finally{
			csv.close();
		}
		fileHandle = new S3FileHandle();
		fileHandle.setBucketName(StackConfigurationSingleton.singleton().getS3Bucket());
		fileHandle.setKey(UUID.randomUUID()+".csv");
		fileHandle.setContentType("text/csv");
		fileHandle.setCreatedBy(""+adminUserInfo.getId());
		fileHandle.setFileName("ToAppendToTable.csv");
		fileHandle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		fileHandle.setEtag(UUID.randomUUID().toString());
		fileHandle.setPreviewId(fileHandle.getId());
		// Upload the File to S3
		fileHandle = (S3FileHandle) fileHandleDao.createFile(fileHandle);
		// Upload the file to S3.
		s3Client.putObject(fileHandle.getBucketName(), fileHandle.getKey(), this.tempFile);
		// We are now ready to start the job
		UploadToTablePreviewRequest body = new UploadToTablePreviewRequest();
		body.setUploadFileHandleId(fileHandle.getId());
		CsvTableDescriptor ctd = new CsvTableDescriptor();
		ctd.setIsFirstLineHeader(true);
		body.setCsvTableDescriptor(ctd);
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, body);
		// Wait for the job to complete.
		status = waitForStatus(status);
		assertNotNull(status);
		assertNotNull(status.getResponseBody());
		assertTrue(status.getResponseBody() instanceof UploadToTablePreviewResult);
		UploadToTablePreviewResult response = (UploadToTablePreviewResult) status.getResponseBody();
		assertEquals(new Long(rowCount), response.getRowsScanned());
		List<ColumnModel> suggestedColumns = response.getSuggestedColumns();
		assertNotNull(suggestedColumns);
		assertEquals(2,suggestedColumns.size());
		assertEquals(ColumnType.INTEGER, suggestedColumns.get(0).getColumnType());
		assertEquals(ColumnType.STRING, suggestedColumns.get(1).getColumnType());
		assertNotNull(response.getSampleRows());
		assertEquals(5, response.getSampleRows().size());
	}
	
	private AsynchronousJobStatus waitForStatus(AsynchronousJobStatus status) throws InterruptedException, DatastoreException, NotFoundException{
		long start = System.currentTimeMillis();
		while(!AsynchJobState.COMPLETE.equals(status.getJobState())){
			assertFalse(AsynchJobState.FAILED.equals(status.getJobState()), "Job Failed: "+status.getErrorDetails());
			System.out.println("Waiting for job to complete: Message: "+status.getProgressMessage()+" progress: "+status.getProgressCurrent()+"/"+status.getProgressTotal());
			assertTrue((System.currentTimeMillis()-start) < MAX_WAIT_MS, "Timed out waiting for table status");
			Thread.sleep(1000);
			// Get the status again 
			status = this.asynchJobStatusManager.getJobStatus(adminUserInfo, status.getJobId());
		}
		return status;
	}
}
