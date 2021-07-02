package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.S3TestUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.FileMetadataUtils;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.file.FileHandleRestoreRequest;
import org.sagebionetworks.repo.model.file.FileHandleRestoreResponse;
import org.sagebionetworks.repo.model.file.FileHandleRestoreResult;
import org.sagebionetworks.repo.model.file.FileHandleRestoreStatus;
import org.sagebionetworks.repo.model.file.FileHandleStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FileHandleRestoreRequestWorkerIntegrationTest {
	
	public static final Long MAX_WAIT_MS = 1000L * 30;

	@Autowired
	private AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private FileHandleDao fileHandleDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private StackConfiguration config;
	
	@Autowired
	private SynapseS3Client s3Client;
	
	private UserInfo adminUser;
	
	private String bucket;

	@BeforeEach
	public void setup() {
		fileHandleDao.truncateTable();
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		bucket = config.getS3Bucket();
	}
	
	@AfterEach
	public void cleanup() {
		S3TestUtils.doDeleteAfter(s3Client);
		fileHandleDao.truncateTable();
	}
	
	@Test
	public void testRestoreRequestForAvailable() throws Exception {
		DBOFileHandle fileHandle = createFile(FileHandleStatus.AVAILABLE);
		
		FileHandleRestoreRequest request = new FileHandleRestoreRequest().setFileHandleIds(Collections.singletonList(fileHandle.getIdString()));
		
		List<FileHandleRestoreResult> expectedResults = Collections.singletonList(
				new FileHandleRestoreResult().setFileHandleId(fileHandle.getIdString())
					.setStatus(FileHandleRestoreStatus.NO_ACTION)
					.setStatusMessage("The file handle is already AVAILABLE. For files in the synapse bucket it might take a few hours before the file can be downloaded.")
		);
		
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, request, (FileHandleRestoreResponse response) -> {
			assertEquals(expectedResults, response.getRestoreResults());
		}, MAX_WAIT_MS);
		
	}
	
	@Test
	public void testRestoreRequestForUnlinked() throws Exception {
		DBOFileHandle fileHandle = createFile(FileHandleStatus.UNLINKED);
		
		FileHandleRestoreRequest request = new FileHandleRestoreRequest().setFileHandleIds(Collections.singletonList(fileHandle.getIdString()));
		
		List<FileHandleRestoreResult> expectedResults = Collections.singletonList(
				new FileHandleRestoreResult().setFileHandleId(fileHandle.getIdString())
					.setStatus(FileHandleRestoreStatus.RESTORED)
		);
		
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, request, (FileHandleRestoreResponse response) -> {
			assertEquals(expectedResults, response.getRestoreResults());
			assertEquals(FileHandleStatus.AVAILABLE, fileHandleDao.get(fileHandle.getIdString()).getStatus());
		}, MAX_WAIT_MS);
		
	}
	
	@Test
	public void testRestoreRequestForArchived() throws Exception {
		DBOFileHandle fileHandle = createFile(FileHandleStatus.ARCHIVED);
		
		FileHandleRestoreRequest request = new FileHandleRestoreRequest().setFileHandleIds(Collections.singletonList(fileHandle.getIdString()));
		
		List<FileHandleRestoreResult> expectedResults = Collections.singletonList(
				new FileHandleRestoreResult().setFileHandleId(fileHandle.getIdString())
					.setStatus(FileHandleRestoreStatus.RESTORED)
		);
		
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, request, (FileHandleRestoreResponse response) -> {
			assertEquals(expectedResults, response.getRestoreResults());
			assertEquals(FileHandleStatus.AVAILABLE, fileHandleDao.get(fileHandle.getIdString()).getStatus());
		}, MAX_WAIT_MS);
		
	}
	
	private DBOFileHandle createFile(FileHandleStatus status) throws Exception {
		DBOFileHandle file = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(adminUser.getId().toString(), idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		
		file.setBucketName(bucket);
		file.setKey(uploadTestFile());
		file.setStatus(status.name());
		file.setContentSize(123L);
		
		fileHandleDao.createBatchDbo(Arrays.asList(file));
		return file;
	}
	
	private String uploadTestFile() throws Exception {
		String key = "tests/" + FileHandleRestoreRequestWorkerIntegrationTest.class.getSimpleName() + "/" + "/" + UUID.randomUUID().toString();
	
		S3TestUtils.createObjectFromString(bucket, key, "Some data", s3Client);
		
		return key;
	}
}
