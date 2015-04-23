package org.sagebionetworks.file.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.apache.commons.fileupload.FileItemStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageReceiver;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileCopyRequest;
import org.sagebionetworks.repo.model.file.S3FileCopyResultType;
import org.sagebionetworks.repo.model.file.S3FileCopyResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandleInterface;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.ProgressCallback;
import org.sagebionetworks.util.TestStreams;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.collect.Lists;

/**
 * This test validates that when a file is created, the message propagates to the preview queue, is processed by the
 * preview worker and a preview is created.
 * 
 * @author John
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class S3FileCopyIntegrationTest {

	private static final int TOO_BIG_FOR_SINGLE_COPY = 6000000;
	private static final String DESTINATION_TEST_BUCKET = "dev.test.destination.bucket.sagebase.org";
	public static final long MAX_WAIT = 30 * 1000; // 30 seconds

	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;

	@Autowired
	private FileHandleManager fileUploadManager;

	@Autowired
	private UserManager userManager;

	@Autowired
	private MessageReceiver fileQueueMessageReveiver;

	@Autowired
	private AmazonS3Client s3Client;

	@Autowired
	private FileHandleDao fileMetadataDao;

	@Autowired
	private SemaphoreManager semphoreManager;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private AutowireCapableBeanFactory factory;

	private S3FileCopyWorker s3FileCopyWorker;
	private UserInfo adminUserInfo;
	private List<S3FileHandleInterface> toDelete = Lists.newArrayList();
	private List<String> s3ToDelete = Lists.newArrayList();
	private String[] testFileNames = { "test" + UUID.randomUUID() + ".unknown", "test" + UUID.randomUUID() + ".unknown",
			"test" + UUID.randomUUID() + ".unknown" };
	private List<String> entities = Lists.newArrayList();

	@Before
	public void before() throws Exception {
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
		// Start with an empty queue.
		asynchJobStatusManager.emptyAllQueues();

		s3FileCopyWorker = factory.createBean(S3FileCopyWorker.class);

		// Create a file
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		// First upload a file that we want to generate a preview for.
		s3Client.createBucket(DESTINATION_TEST_BUCKET);
	}

	@After
	public void after() throws Exception {
		for (String entity : Lists.reverse(entities)) {
			entityManager.deleteEntity(adminUserInfo, entity);
		}
		// Delete any files created
		for (S3FileHandleInterface meta : toDelete) {
			// delete the file from S3.
			s3Client.deleteObject(meta.getBucketName(), meta.getKey());
			if (meta.getId() != null) {
				// We also need to delete the data from the database
				fileMetadataDao.delete(meta.getId());
			}
		}
		// Delete any files created
		for (String key : s3ToDelete) {
			// delete the file from S3.
			s3Client.deleteObject(DESTINATION_TEST_BUCKET, key);
		}
	}

	@Test
	public void testCopySmallFile() throws Exception {
		testCopyFile(200);
	}

	@Test
	public void testCopyLargeFile() throws Exception {
		testCopyFile(TOO_BIG_FOR_SINGLE_COPY);
	}

	@Test
	public void testCopyNewFilesSuccess() throws Exception {
		S3FileCopyRequest request = new S3FileCopyRequest();
		request.setFiles(Lists.<String> newArrayList());
		request.setBucket(DESTINATION_TEST_BUCKET);
		request.setOverwrite(false);

		for (int i = 0; i < testFileNames.length; i++) {
			String fileEntityId = createFileEntity(i);
			request.getFiles().add(fileEntityId);
		}

		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, request);
		// Wait for the job to complete.
		status = waitForStatus(adminUserInfo, status);
		assertNotNull(status);
		assertEquals(status.toString(), AsynchJobState.COMPLETE, status.getJobState());
		assertNotNull(status.getResponseBody());
		assertTrue(status.getResponseBody() instanceof S3FileCopyResults);
		S3FileCopyResults results = (S3FileCopyResults) status.getResponseBody();
		assertEquals(testFileNames.length, results.getResults().size());
		for (int i = 0; i < testFileNames.length; i++) {
			assertEquals(S3FileCopyResultType.COPIED, results.getResults().get(i).getResultType());
			TestStreams.assertEquals(TestStreams.randomStream(200, 123L + i), s3Client.getObject(DESTINATION_TEST_BUCKET, testFileNames[i])
					.getObjectContent());
			s3ToDelete.add(results.getResults().get(i).getResultKey());
		}
	}

	@Test
	public void testCopyFailOnOneError() throws Exception {
		S3FileCopyRequest request = new S3FileCopyRequest();
		request.setFiles(Lists.<String> newArrayList());
		request.setBucket(DESTINATION_TEST_BUCKET);
		request.setOverwrite(false);

		for (int i = 0; i < testFileNames.length; i++) {
			String fileEntityId = createFileEntity(i);
		request.getFiles().add(fileEntityId);
		}
		request.getFiles().add("syn333333333333");

		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, request);
		// Wait for the job to complete.
		status = waitForStatus(adminUserInfo, status);
		assertNotNull(status);
		assertEquals(status.toString(), AsynchJobState.COMPLETE, status.getJobState());
		assertNotNull(status.getResponseBody());
		assertTrue(status.getResponseBody() instanceof S3FileCopyResults);
		S3FileCopyResults results = (S3FileCopyResults) status.getResponseBody();
		assertEquals(testFileNames.length + 1, results.getResults().size());
		for (int i = 0; i < testFileNames.length; i++) {
			assertEquals(S3FileCopyResultType.NOTCOPIED, results.getResults().get(i).getResultType());
		}
		assertEquals(S3FileCopyResultType.ERROR, results.getResults().get(testFileNames.length).getResultType());
	}

	@Test
	public void testFileNaming() throws Exception {
		S3FileCopyRequest request = new S3FileCopyRequest();
		request.setFiles(Lists.<String> newArrayList());
		request.setBucket(DESTINATION_TEST_BUCKET);
		request.setOverwrite(false);

		Project project = new Project();
		project.setName("project" + UUID.randomUUID());
		String projectId = entityManager.createEntity(adminUserInfo, project, null);
		entities.add(projectId);
		Folder folder = new Folder();
		folder.setName("folder");
		folder.setParentId(projectId);
		String folderId = entityManager.createEntity(adminUserInfo, folder, null);
		entities.add(folderId);
		String fileEntityId = createFileEntity(0, 0, 200, folderId);
		request.getFiles().add(fileEntityId);

		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, request);
		// Wait for the job to complete.
		status = waitForStatus(adminUserInfo, status);
		assertEquals(status.toString(), AsynchJobState.COMPLETE, status.getJobState());
		S3FileCopyResults results = (S3FileCopyResults) status.getResponseBody();
		s3ToDelete.add(results.getResults().get(0).getResultKey());
		assertEquals(project.getName() + "/folder/" + testFileNames[0], results.getResults().get(0).getResultKey());
	}

	@Test
	public void testUpdateSuccess() throws Exception {
		S3FileCopyRequest request = new S3FileCopyRequest();
		request.setFiles(Lists.<String> newArrayList());
		request.setBucket(DESTINATION_TEST_BUCKET);
		request.setOverwrite(true);

		String fileEntityId = createFileEntity(0, 0, TOO_BIG_FOR_SINGLE_COPY, null);
		request.getFiles().add(fileEntityId);

		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, request);
		// Wait for the job to complete.
		status = waitForStatus(adminUserInfo, status);
		assertEquals(status.toString(), AsynchJobState.COMPLETE, status.getJobState());
		S3FileCopyResults results = (S3FileCopyResults) status.getResponseBody();
		assertEquals(S3FileCopyResultType.COPIED, results.getResults().get(0).getResultType());
		s3ToDelete.add(results.getResults().get(0).getResultKey());

		// and overwrite skip
		status = asynchJobStatusManager.startJob(adminUserInfo, request);
		// Wait for the job to complete.
		status = waitForStatus(adminUserInfo, status);
		assertEquals(status.toString(), AsynchJobState.COMPLETE, status.getJobState());
		results = (S3FileCopyResults) status.getResponseBody();
		assertEquals(S3FileCopyResultType.UPTODATE, results.getResults().get(0).getResultType());

		// and overwrite all
		// delete old
		entityManager.deleteEntity(adminUserInfo, fileEntityId);
		entities.remove(fileEntityId);
		// and recreate with different content
		fileEntityId = createFileEntity(0, 100, TOO_BIG_FOR_SINGLE_COPY, null);
		request.getFiles().set(0, fileEntityId);
		status = asynchJobStatusManager.startJob(adminUserInfo, request);
		// Wait for the job to complete.
		status = waitForStatus(adminUserInfo, status);
		assertEquals(status.toString(), AsynchJobState.COMPLETE, status.getJobState());
		results = (S3FileCopyResults) status.getResponseBody();
		assertEquals(S3FileCopyResultType.COPIED, results.getResults().get(0).getResultType());
	}

	@Test
	public void testOverwriteSuccess() throws Exception {
		S3FileCopyRequest request = new S3FileCopyRequest();
		request.setFiles(Lists.<String> newArrayList());
		request.setBucket(DESTINATION_TEST_BUCKET);
		request.setOverwrite(true);

		String fileEntityId = createFileEntity(0, 0, 200, null);
		request.getFiles().add(fileEntityId);

		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, request);
		// Wait for the job to complete.
		status = waitForStatus(adminUserInfo, status);
		assertEquals(status.toString(), AsynchJobState.COMPLETE, status.getJobState());
		S3FileCopyResults results = (S3FileCopyResults) status.getResponseBody();
		assertEquals(S3FileCopyResultType.COPIED, results.getResults().get(0).getResultType());
		s3ToDelete.add(results.getResults().get(0).getResultKey());

		entityManager.deleteEntity(adminUserInfo, fileEntityId);
		entities.remove(fileEntityId);
		fileEntityId = createFileEntity(0, 1000, 200, null);
		request.getFiles().set(0, fileEntityId);

		// and overwrite all
		status = asynchJobStatusManager.startJob(adminUserInfo, request);
		// Wait for the job to complete.
		status = waitForStatus(adminUserInfo, status);
		assertEquals(status.toString(), AsynchJobState.COMPLETE, status.getJobState());
		results = (S3FileCopyResults) status.getResponseBody();
		assertEquals(S3FileCopyResultType.COPIED, results.getResults().get(0).getResultType());
	}

	@Test
	public void testDontOverwrite() throws Exception {
		S3FileCopyRequest request = new S3FileCopyRequest();
		request.setFiles(Lists.<String> newArrayList());
		request.setBucket(DESTINATION_TEST_BUCKET);
		request.setOverwrite(false);

		String fileEntityId = createFileEntity(0);
		request.getFiles().add(fileEntityId);

		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, request);
		// Wait for the job to complete.
		status = waitForStatus(adminUserInfo, status);
		assertEquals(status.toString(), AsynchJobState.COMPLETE, status.getJobState());
		S3FileCopyResults results = (S3FileCopyResults) status.getResponseBody();
		assertEquals(S3FileCopyResultType.COPIED, results.getResults().get(0).getResultType());
		s3ToDelete.add(results.getResults().get(0).getResultKey());

		// and fail overwrite
		status = asynchJobStatusManager.startJob(adminUserInfo, request);
		// Wait for the job to complete.
		status = waitForStatus(adminUserInfo, status);
		assertEquals(status.toString(), AsynchJobState.COMPLETE, status.getJobState());
		results = (S3FileCopyResults) status.getResponseBody();
		assertEquals(S3FileCopyResultType.ERROR, results.getResults().get(0).getResultType());
	}

	private String createFileEntity(int index) throws IOException, ServiceUnavailableException {
		return createFileEntity(index, index, 200, null);
	}

	private String createFileEntity(int index, int seed, int size, String parentId) throws IOException, ServiceUnavailableException {
		S3FileHandle fileHandle = uploadFile(testFileNames[index], TestStreams.randomStream(size, 123L + seed));
		toDelete.add(fileHandle);
		FileEntity fileEntity = new FileEntity();
		fileEntity.setDataFileHandleId(fileHandle.getId());
		fileEntity.setName(fileHandle.getFileName());
		fileEntity.setParentId(parentId);
		String fileEntityId = entityManager.createEntity(adminUserInfo, fileEntity, null);
		entities.add(fileEntityId);
		return fileEntityId;
	}

	private AsynchronousJobStatus waitForStatus(UserInfo user, final AsynchronousJobStatus status) throws Exception {
		return TimeUtils.waitFor(/* 60000L */6000000L, 500L, new Callable<Pair<Boolean, AsynchronousJobStatus>>() {
			@Override
			public Pair<Boolean, AsynchronousJobStatus> call() throws Exception {
				AsynchronousJobStatus currentStatus = asynchJobStatusManager.getJobStatus(adminUserInfo, status.getJobId());
				return Pair.create(!AsynchJobState.PROCESSING.equals(currentStatus.getJobState()), currentStatus);
			}
		});
	}

	private void testCopyFile(long size) throws IOException, ServiceUnavailableException {
		S3FileHandle fileHandle = uploadFile(testFileNames[0], TestStreams.randomStream(size, 123L));
		toDelete.add(fileHandle);

		@SuppressWarnings("unchecked")
		ProgressCallback<Long> progress = mock(ProgressCallback.class);
		String key = testFileNames[0];
		s3FileCopyWorker.copyFile(fileHandle, DESTINATION_TEST_BUCKET, key, progress);
		s3ToDelete.add(key);

		S3Object fileFromS3 = s3Client.getObject(DESTINATION_TEST_BUCKET, testFileNames[0]);
		TestStreams.assertEquals(TestStreams.randomStream(size, 123L), fileFromS3.getObjectContent());
		verify(progress, times((int) size / (5 * 1024 * 1024) + 1)).progressMade(any(Long.class));
	}

	private S3FileHandle uploadFile(String fileHandleName, InputStream in) throws IOException, ServiceUnavailableException {
		FileItemStream mockFiz = Mockito.mock(FileItemStream.class);
		when(mockFiz.openStream()).thenReturn(in);
		when(mockFiz.getContentType()).thenReturn("unknown/content");
		when(mockFiz.getName()).thenReturn(fileHandleName);
		// Now upload the file.
		return fileUploadManager.uploadFile(adminUserInfo.getId().toString(), mockFiz);
	}
}
