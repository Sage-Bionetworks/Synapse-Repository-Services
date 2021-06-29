package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opentest4j.AssertionFailedError;
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
import org.sagebionetworks.repo.model.file.FileHandleArchivalRequest;
import org.sagebionetworks.repo.model.file.FileHandleArchivalResponse;
import org.sagebionetworks.repo.model.file.FileHandleStatus;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.s3.model.Tag;

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
	}

	@Test
	public void testArchivalRequest() throws Exception {
		
		Instant now = Instant.now();
		
		Instant inRange = now.minus(31, ChronoUnit.DAYS);
		Instant beforeRange = now.minus(60, ChronoUnit.DAYS);
		
		String key0 = uploadFile("key_0", true);
		String key1 = uploadFile("key_1", true);
		String key1Preview = uploadFile("key_1_preview", true);
		String key2 = uploadFile("key_2", true);
		String key2Preview = uploadFile("key_2_preview", true);
		String key3 = uploadFile("key_3", true);
		String key4 = uploadFile("key_4", true);
		String key5 = uploadFile("key_5", true);
		String key6 = uploadFile("key_6", true);
		String key7 = uploadFile("key_7", false);
		
		// After range
		Long key0File1 = createDBOFile(bucket, now, FileHandleStatus.UNLINKED, key0).getId(); // -> untouched
		// After range but available
		Long key0File2 = createDBOFile(bucket, now, FileHandleStatus.AVAILABLE, key0).getId(); // -> untouched
		// After range but available and copy of one in range
		Long key3File1 = createDBOFile(bucket, now, FileHandleStatus.AVAILABLE, key3).getId(); // -> untouched
		
		Long key1PreviewId = createDBOFile(bucket, inRange, FileHandleStatus.AVAILABLE, key1Preview, true, null).getId();
		// In range
		Long key1File1 = createDBOFile(bucket, inRange, FileHandleStatus.UNLINKED, key1, false, key1PreviewId).getId(); // -> archive and tag and delete the preview
		// In range and a copy
		Long key1File2 = createDBOFile(bucket, inRange, FileHandleStatus.UNLINKED, key1, false, key1PreviewId).getId(); // -> archive and tag and delete the preview
		
		Long key2PreviewId = createDBOFile(bucket, inRange, FileHandleStatus.AVAILABLE, key2Preview, true, null).getId();
		// In range
		Long key2File1 = createDBOFile(bucket, inRange, FileHandleStatus.UNLINKED, key2, false, key2PreviewId).getId(); // -> archive and tag, but does not delete the preview as it is used
		// In range
		Long key3File2 = createDBOFile(bucket, inRange, FileHandleStatus.UNLINKED, key3).getId(); // -> archive but not tagged
		// In range but available
		Long key4File1 = createDBOFile(bucket, inRange, FileHandleStatus.AVAILABLE, key4).getId(); // -> untouched
		// In range
		Long key5File1 = createDBOFile(bucket, inRange, FileHandleStatus.UNLINKED, key5).getId(); // -> archived but not tagged
		// In range
		Long key7File1 = createDBOFile(bucket, inRange, FileHandleStatus.UNLINKED, key7).getId(); // -> deleted
		
		// Before range, but copy of a key in range
		Long key2File2 = createDBOFile(bucket, beforeRange, FileHandleStatus.UNLINKED, key2).getId(); // -> archive and tag
		// Before range, but AVAILABLE
		Long key6File1 = createDBOFile(bucket, beforeRange, FileHandleStatus.AVAILABLE, key6, false, key2PreviewId).getId(); // -> untouched
		// Before range, but AVAILABLE and copy of a key in range
		Long key5File2 = createDBOFile(bucket, beforeRange, FileHandleStatus.AVAILABLE, key5).getId(); // -> untouched
		
		FileHandleArchivalRequest request = new FileHandleArchivalRequest();
		
		// First wait for the dispatcher job
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, request, (FileHandleArchivalResponse response) -> {
			assertEquals(5L, response.getCount());
		}, MAX_WAIT_MS);

		// Now wait to verify the results
		TimeUtils.waitFor(MAX_WAIT_MS, 2000, () -> {
			
			try {
				verify(key0File1, FileHandleStatus.UNLINKED, null, false);
				verify(key0File2, FileHandleStatus.AVAILABLE, null, false);
				verify(key3File1, FileHandleStatus.AVAILABLE, null, false);
				verify(key1File1, FileHandleStatus.ARCHIVED, null, true);
				verify(key1File2, FileHandleStatus.ARCHIVED, null, true);
				assertFalse(fileHandleDao.doesExist(key1PreviewId.toString()));
				verify(key2File1, FileHandleStatus.ARCHIVED, null, true);
				verify(key2PreviewId, FileHandleStatus.AVAILABLE, null, false);
				verify(key3File2, FileHandleStatus.ARCHIVED, null, false);
				verify(key4File1, FileHandleStatus.AVAILABLE, null, false);
				verify(key5File1, FileHandleStatus.ARCHIVED, null, false);
				assertFalse(fileHandleDao.doesExist(key7File1.toString()));
				verify(key2File2, FileHandleStatus.ARCHIVED, null, true);
				verify(key6File1, FileHandleStatus.AVAILABLE, key2PreviewId, false);
				verify(key5File2, FileHandleStatus.AVAILABLE, null, false);
			} catch (AssertionFailedError assertion) {
				assertion.printStackTrace();
				return new Pair<Boolean, Void>(false, null);
			}
			
			return new Pair<Boolean, Void>(true, null);
		});
	}
	
	private void verify(Long id, FileHandleStatus status, Long previewId, boolean tagged) {
		S3FileHandle handle = (S3FileHandle) fileHandleDao.get(id.toString());
		
		assertEquals(status, handle.getStatus());
		assertEquals(handle.getPreviewId(), previewId == null ? null : previewId.toString());
		
		List<Tag> tags = s3Client.getObjectTags(bucket, handle.getKey());
		
		if (tagged && !tags.stream().filter(t->t.getKey().equals("synapse-status") && t.getValue().equals("archived")).findFirst().isPresent()) {
			fail("The file handle with key " + handle.getKey() + " was not tagged");
		} else if (!tagged && !tags.isEmpty()) {
			fail("The file handle with key " + handle.getKey() + " was not supposed to be tagged");
		}
	}
	
	
	private String uploadFile(String prefix, boolean doUpload) throws Exception {
		String key = "tests/" + FileHandleArchivalWorkerIngegrationTest.class.getSimpleName() + "/" + prefix + "/" + UUID.randomUUID().toString();
	
		if	(doUpload) {
			S3TestUtils.createObjectFromString(bucket, key, "Some data", s3Client);
		}
		
		return key;
	}
	
	private DBOFileHandle createDBOFile(String bucket, Instant updatedOn, FileHandleStatus status, String key) {
		return createDBOFile(bucket, updatedOn, status, key, false, null);
	}

	private DBOFileHandle createDBOFile(String bucket, Instant updatedOn, FileHandleStatus status, String key, boolean isPreview, Long previewId) {
		DBOFileHandle file = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(adminUser.getId().toString(), idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		
		file.setBucketName(bucket);
		file.setUpdatedOn(Timestamp.from(updatedOn));
		file.setKey(key);
		file.setStatus(status.name());
		file.setIsPreview(isPreview);
		file.setPreviewId(previewId);
		
		fileHandleDao.createBatchDbo(Arrays.asList(file));
		return file;
	}
	
}
