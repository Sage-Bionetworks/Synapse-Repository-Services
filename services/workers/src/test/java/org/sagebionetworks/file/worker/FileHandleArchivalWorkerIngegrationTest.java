package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.sagebionetworks.repo.manager.file.FileHandleArchivalManager.S3_TAG_ARCHIVED;
import static org.sagebionetworks.repo.manager.file.FileHandleArchivalManager.S3_TAG_SIZE_THRESHOLD;

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
import org.sagebionetworks.aws.CannotDetermineBucketLocationException;
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
		fileHandleDao.truncateTable();
	}
	
	@Test
	public void testArchivalRequestWithAfterTimeWindow() throws Exception {
		
		Instant modifiedOn = Instant.now();
		
		String availableFileKey = uploadFile("key_0", true);
		
		// Available file modified after the time window
		Long availableFile = createDBOFile(bucket, modifiedOn, FileHandleStatus.AVAILABLE, availableFileKey).getId();
		
		// Unlinked file modified after the time window
		Long unlinkedFile = createDBOFile(bucket, modifiedOn, FileHandleStatus.UNLINKED, uploadFile("key_1", true)).getId();
		
		// Unlinked file modified after the time window, copy of the available
		Long unlinkedFileCopy = createDBOFile(bucket, modifiedOn, FileHandleStatus.UNLINKED, availableFileKey).getId();
		
		FileHandleArchivalRequest request = new FileHandleArchivalRequest();
		
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, request, (FileHandleArchivalResponse response) -> {
			assertEquals(0L, response.getCount());
		}, MAX_WAIT_MS);
		
		verifyAsynch(() -> {
			verify(availableFile, FileHandleStatus.AVAILABLE, null, false);
			verify(unlinkedFile, FileHandleStatus.UNLINKED, null, false);
			verify(unlinkedFileCopy, FileHandleStatus.UNLINKED, null, false);
		});
		
	}
	
	@Test
	public void testArchivalRequestWithUnlinked() throws Exception {
		
		Instant modifiedOn = Instant.now().minus(31, ChronoUnit.DAYS);
		
		// Unlinked file modified before the time window
		Long unlinkedFile = createDBOFile(bucket, modifiedOn, FileHandleStatus.UNLINKED, uploadFile("key_0", true)).getId();
		
		FileHandleArchivalRequest request = new FileHandleArchivalRequest();
		
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, request, (FileHandleArchivalResponse response) -> {
			assertEquals(1L, response.getCount());
		}, MAX_WAIT_MS);
		
		verifyAsynch(() -> {
			verify(unlinkedFile, FileHandleStatus.ARCHIVED, null, true);
		});
	}
	
	@Test
	public void testArchivalRequestWithUnlinkedInExternalBucket() throws Exception {
		
		Instant modifiedOn = Instant.now().minus(31, ChronoUnit.DAYS);
		
		// Unlinked file modified before the time window
		Long unlinkedFile = createDBOFile("anotherBucket", modifiedOn, FileHandleStatus.UNLINKED, uploadFile("key_0", false)).getId();
		
		FileHandleArchivalRequest request = new FileHandleArchivalRequest();
		
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, request, (FileHandleArchivalResponse response) -> {
			assertEquals(0L, response.getCount());
		}, MAX_WAIT_MS);
		
		verifyAsynch(() -> {
			verify(unlinkedFile, FileHandleStatus.UNLINKED, null, false);
		});
	}
	
	@Test
	public void testArchivalRequestWithAvailable() throws Exception {
		
		Instant modifiedOn = Instant.now().minus(31, ChronoUnit.DAYS);
		
		// Available file modified before the time window
		Long availableFile = createDBOFile(bucket, modifiedOn, FileHandleStatus.AVAILABLE, uploadFile("key_0", true)).getId();
		
		FileHandleArchivalRequest request = new FileHandleArchivalRequest();
		
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, request, (FileHandleArchivalResponse response) -> {
			assertEquals(0L, response.getCount());
		}, MAX_WAIT_MS);
		
		verifyAsynch(() -> {
			verify(availableFile, FileHandleStatus.AVAILABLE, null, false);
		});
	}
	
	@Test
	public void testArchivalRequestWithUnlinkedWithCopy() throws Exception {
		
		Instant modifiedOn = Instant.now().minus(31, ChronoUnit.DAYS);
		
		String fileKey = uploadFile("key_0", true);
		
		// Unlinked file modified before the time window
		Long unlinkedFile = createDBOFile(bucket, modifiedOn, FileHandleStatus.UNLINKED, fileKey).getId();
		
		// Unlinked file modified before the time window, copy of the previous one
		Long unlinkedFileCopy = createDBOFile(bucket, modifiedOn, FileHandleStatus.UNLINKED, fileKey).getId();
		
		FileHandleArchivalRequest request = new FileHandleArchivalRequest();
		
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, request, (FileHandleArchivalResponse response) -> {
			assertEquals(1L, response.getCount());
		}, MAX_WAIT_MS);
		
		verifyAsynch(() -> {
			verify(unlinkedFile, FileHandleStatus.ARCHIVED, null, true);
			verify(unlinkedFileCopy, FileHandleStatus.ARCHIVED, null, true);
		});
	}
	
	@Test
	public void testArchivalRequestWithUnlinkedWithAvailableCopy() throws Exception {
		
		Instant modifiedOn = Instant.now().minus(31, ChronoUnit.DAYS);
		
		String fileKey = uploadFile("key_0", true);
		
		// Unlinked file modified before the time window
		Long unlinkedFile = createDBOFile(bucket, modifiedOn, FileHandleStatus.UNLINKED, fileKey).getId();
		
		// Available file modified before the time window, copy of the previous one
		Long availableFileCopy = createDBOFile(bucket, modifiedOn, FileHandleStatus.AVAILABLE, fileKey).getId();
		
		FileHandleArchivalRequest request = new FileHandleArchivalRequest();
		
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, request, (FileHandleArchivalResponse response) -> {
			assertEquals(1L, response.getCount());
		}, MAX_WAIT_MS);
		
		verifyAsynch(() -> {
			verify(unlinkedFile, FileHandleStatus.ARCHIVED, null, false); // The s3 object is not tagged as a copy is still available
			verify(availableFileCopy, FileHandleStatus.AVAILABLE, null, false);
		});
	}
	
	@Test
	public void testArchivalRequestWithUnlinkedWithAvailableCopyAfterTimeWindow() throws Exception {
		
		Instant now = Instant.now();
		Instant modifiedOn = now.minus(31, ChronoUnit.DAYS);
		
		String fileKey = uploadFile("key_0", true);
		
		// Unlinked file modified before the time window
		Long unlinkedFile = createDBOFile(bucket, modifiedOn, FileHandleStatus.UNLINKED, fileKey).getId();
		
		// Available file modified after the time window, copy of the previous one
		Long availableFileCopy = createDBOFile(bucket, now, FileHandleStatus.AVAILABLE, fileKey).getId();
		
		FileHandleArchivalRequest request = new FileHandleArchivalRequest();
		
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, request, (FileHandleArchivalResponse response) -> {
			assertEquals(1L, response.getCount());
		}, MAX_WAIT_MS);
		
		verifyAsynch(() -> {
			verify(unlinkedFile, FileHandleStatus.ARCHIVED, null, false); // The s3 object is not tagged as a copy is still available
			verify(availableFileCopy, FileHandleStatus.AVAILABLE, null, false);
		});
	}
	
	@Test
	public void testArchivalRequestWithUnlinkedWithUnlinkedCopyAfterTimeWindow() throws Exception {
		
		Instant now = Instant.now();
		Instant modifiedOn = now.minus(31, ChronoUnit.DAYS);
		
		String fileKey = uploadFile("key_0", true);
		
		// Unlinked file modified before the time window
		Long unlinkedFile = createDBOFile(bucket, modifiedOn, FileHandleStatus.UNLINKED, fileKey).getId();
		
		// Unlinked file modified after the time window, copy of the previous one
		Long unlinkedFileCopy = createDBOFile(bucket, now, FileHandleStatus.UNLINKED, fileKey).getId();
		
		FileHandleArchivalRequest request = new FileHandleArchivalRequest();
		
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, request, (FileHandleArchivalResponse response) -> {
			assertEquals(1L, response.getCount());
		}, MAX_WAIT_MS);
		
		verifyAsynch(() -> {
			verify(unlinkedFile, FileHandleStatus.ARCHIVED, null, false); // The s3 object is not tagged as a copy is unlinked but now within the time window
			verify(unlinkedFileCopy, FileHandleStatus.UNLINKED, null, false);
		});
	}
	
	@Test
	public void testArchivalRequestWithUnlinkedUnderSizeThreshold() throws Exception {
		
		Instant modifiedOn = Instant.now().minus(31, ChronoUnit.DAYS);
		
		// Unlinked file modified before the time window under the size threshold for tagging
		Long unlinkedFile = createDBOFile(bucket, modifiedOn, FileHandleStatus.UNLINKED, uploadFile("key_0", true), S3_TAG_SIZE_THRESHOLD - 1, false, null).getId();
		
		FileHandleArchivalRequest request = new FileHandleArchivalRequest();
		
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, request, (FileHandleArchivalResponse response) -> {
			assertEquals(1L, response.getCount());
		}, MAX_WAIT_MS);
		
		verifyAsynch(() -> {
			verify(unlinkedFile, FileHandleStatus.ARCHIVED, null, false);
		});
	}
	
	@Test
	public void testArchivalRequestWithUnlinkedNotExisting() throws Exception {
		
		Instant modifiedOn = Instant.now().minus(31, ChronoUnit.DAYS);
		
		// Unlinked file modified before the time window under the size threshold for tagging
		Long unlinkedFile = createDBOFile(bucket, modifiedOn, FileHandleStatus.UNLINKED, uploadFile("key_0", false)).getId();
		
		FileHandleArchivalRequest request = new FileHandleArchivalRequest();
		
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, request, (FileHandleArchivalResponse response) -> {
			assertEquals(1L, response.getCount());
		}, MAX_WAIT_MS);
		
		verifyAsynch(() -> {
			assertFalse(fileHandleDao.doesExist(unlinkedFile.toString()));
		});
	}
	
	@Test
	public void testArchivalRequestWithUnlinkedAndPreview() throws Exception {
		Instant modifiedOn = Instant.now().minus(31, ChronoUnit.DAYS);
		
		Long preview = createDBOFile(bucket, modifiedOn, FileHandleStatus.AVAILABLE, uploadFile("key_0_preview", true), 123L, true, null).getId();
		
		// Unlinked file modified before the time window with a preview
		Long unlinkedFile = createDBOFile(bucket, modifiedOn, FileHandleStatus.UNLINKED, uploadFile("key_0", true), S3_TAG_SIZE_THRESHOLD, false, preview).getId();
		
		FileHandleArchivalRequest request = new FileHandleArchivalRequest();
		
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, request, (FileHandleArchivalResponse response) -> {
			assertEquals(1L, response.getCount());
		}, MAX_WAIT_MS);
		
		verifyAsynch(() -> {
			// The preview was deleted as unused
			assertFalse(fileHandleDao.doesExist(preview.toString()));
			verify(unlinkedFile, FileHandleStatus.ARCHIVED, null, true);
		});
	}
	
	@Test
	public void testArchivalRequestWithUnlinkedAndLinkedPreview() throws Exception {
		Instant modifiedOn = Instant.now().minus(31, ChronoUnit.DAYS);
		
		Long preview = createDBOFile(bucket, modifiedOn, FileHandleStatus.AVAILABLE, uploadFile("preview", true), 123L, true, null).getId();
		
		// Unlinked file modified before the time window with a preview
		Long unlinkedFile = createDBOFile(bucket, modifiedOn, FileHandleStatus.UNLINKED, uploadFile("key_0", true), S3_TAG_SIZE_THRESHOLD, false, preview).getId();
		
		// Available file modified before the time window with same preview
		Long availableFile = createDBOFile(bucket, modifiedOn, FileHandleStatus.AVAILABLE, uploadFile("key_1", true), S3_TAG_SIZE_THRESHOLD, false, preview).getId();
		
		FileHandleArchivalRequest request = new FileHandleArchivalRequest();
		
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, request, (FileHandleArchivalResponse response) -> {
			assertEquals(1L, response.getCount());
		}, MAX_WAIT_MS);
		
		verifyAsynch(() -> {
			// The preview is still available as it is used by another file handle
			verify(preview, FileHandleStatus.AVAILABLE, null, false);
			verify(unlinkedFile, FileHandleStatus.ARCHIVED, null, true);
			verify(availableFile, FileHandleStatus.AVAILABLE, preview, false);
		});
	}
	
	private void verifyAsynch(Runnable runnable) throws Exception {
		TimeUtils.waitFor(MAX_WAIT_MS, 500, () -> {
			try {
				runnable.run();
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
		
		
		List<Tag> tags;
		
		try {
			tags = s3Client.getObjectTags(handle.getBucketName(), handle.getKey());
		} catch (CannotDetermineBucketLocationException ex) {
			if (!tagged) { // If the bucket does not exists and the object wasn't supposed to be tagged then it's fine
				return;
			}
			throw ex;
		}
		
		if (tagged && !tags.stream().filter(t->t.equals(S3_TAG_ARCHIVED)).findFirst().isPresent()) {
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
		return createDBOFile(bucket, updatedOn, status, key, S3_TAG_SIZE_THRESHOLD, false, null);
	}

	private DBOFileHandle createDBOFile(String bucket, Instant updatedOn, FileHandleStatus status, String key, Long contentSize, boolean isPreview, Long previewId) {
		DBOFileHandle file = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(adminUser.getId().toString(), idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		
		file.setBucketName(bucket);
		file.setUpdatedOn(Timestamp.from(updatedOn));
		file.setKey(key);
		file.setStatus(status.name());
		file.setIsPreview(isPreview);
		file.setPreviewId(previewId);
		file.setContentSize(contentSize);
		
		fileHandleDao.createBatchDbo(Arrays.asList(file));
		return file;
	}
	
}
