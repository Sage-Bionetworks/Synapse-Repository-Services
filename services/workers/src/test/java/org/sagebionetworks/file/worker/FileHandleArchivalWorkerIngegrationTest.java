package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.FileMetadataUtils;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.file.FileHandleArchivalRequest;
import org.sagebionetworks.repo.model.file.FileHandleArchivalResponse;
import org.sagebionetworks.repo.model.file.FileHandleStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FileHandleArchivalWorkerIngegrationTest {
	
	public static final Long MAX_WAIT_MS = 1000L * 30L;

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
	
	private UserInfo adminUser;
	
	private String bucket;
	
	private List<DBOFileHandle> files;
	
	@BeforeEach
	public void setup() {
		fileHandleDao.truncateTable();
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		bucket = config.getS3Bucket();
		files = new ArrayList<>();
	}

	@Test
	public void testArchivalRequest() throws AsynchJobFailedException {
		
		Instant now = Instant.now();
		
		Instant inRange = now.minus(31, ChronoUnit.DAYS);
		Instant beforeRange = now.minus(60, ChronoUnit.DAYS);
		
		// After range
		createDBOFile(bucket, now, FileHandleStatus.UNLINKED, "key0"); // -> untouched
		// After range but available
		createDBOFile(bucket, now, FileHandleStatus.AVAILABLE, "key0"); // -> untouched
		// After range but available and copy of one in range
		createDBOFile(bucket, now, FileHandleStatus.AVAILABLE, "key3"); // -> untouched
		
		// In range
		createDBOFile(bucket, inRange, FileHandleStatus.UNLINKED, "key1"); // -> archive and tag
		// In range and a copy
		createDBOFile(bucket, inRange, FileHandleStatus.UNLINKED, "key1"); // -> archive and tag
		// In range
		createDBOFile(bucket, inRange, FileHandleStatus.UNLINKED, "key2"); // -> archive and tag
		// In range
		createDBOFile(bucket, inRange, FileHandleStatus.UNLINKED, "key3"); // -> archive and tag
		// In range but available
		createDBOFile(bucket, inRange, FileHandleStatus.AVAILABLE, "key4"); // -> untouched
		// In range
		createDBOFile(bucket, inRange, FileHandleStatus.UNLINKED, "key5"); // -> archived but not tagged
		
		// Before range, but copy of a key in range
		createDBOFile(bucket, beforeRange, FileHandleStatus.UNLINKED, "key2"); // -> archive and tag
		// Before range, but AVAILABLE
		createDBOFile(bucket, beforeRange, FileHandleStatus.AVAILABLE, "key6"); // -> untouched
		// Before range, but AVAILABLE and copy of a key in range
		createDBOFile(bucket, beforeRange, FileHandleStatus.AVAILABLE, "key5"); // -> untouched
		
		FileHandleArchivalRequest request = new FileHandleArchivalRequest();
		
		// call under test
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, request, (FileHandleArchivalResponse response) -> {
			assertEquals(4L, response.getCount());
		}, MAX_WAIT_MS);
	}

	private DBOFileHandle createDBOFile(String bucket, Instant updatedOn, FileHandleStatus status, String key) {
		DBOFileHandle file = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(adminUser.getId().toString(), idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		
		file.setBucketName(bucket);
		file.setUpdatedOn(Timestamp.from(updatedOn));
		file.setKey(key);
		file.setStatus(status.name());
		
		fileHandleDao.createBatchDbo(Arrays.asList(file));
		files.add(file);
		return file;
	}
	
}
