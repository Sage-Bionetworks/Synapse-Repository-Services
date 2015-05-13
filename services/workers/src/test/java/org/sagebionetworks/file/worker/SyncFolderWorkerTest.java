package org.sagebionetworks.file.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.asynchronous.workers.sqs.MessageReceiver;
import org.sagebionetworks.junit.BeforeAll;
import org.sagebionetworks.junit.ParallelizedSpringJUnit4ClassRunner;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileCopyRequest;
import org.sagebionetworks.repo.model.file.S3FileCopyResultType;
import org.sagebionetworks.repo.model.file.S3FileCopyResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandleInterface;
import org.sagebionetworks.repo.model.util.RandomUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.ProgressCallback;
import org.sagebionetworks.util.ReflectionStaticTestUtils;
import org.sagebionetworks.util.TestStreams;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.test.context.ContextConfiguration;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;

public class SyncFolderWorkerTest {

	@Mock
	private EntityManager entityManager;
	@Mock
	private AmazonS3Client s3Client;
	@Mock
	private UserManager userManager;
	@Mock
	private ProjectSettingsManager projectSettingsManager;
	@Mock
	private NodeDAO nodeDao;
	@Mock
	private FileHandleDao fileHandleDao;

	private SyncFolderWorker syncFolderWorker;
	private UserInfo owner = new UserInfo(true, 100L);

	@Before
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);
		syncFolderWorker = new SyncFolderWorker();
		ReflectionStaticTestUtils.mockAutowire(this, syncFolderWorker);
	}

	@Test
	public void skipOwnertxt() throws Exception {
		Node folder = new Node();
		S3ObjectSummary objectSummary = new S3ObjectSummary();
		objectSummary.setKey("owner.txt");
		syncFolderWorker.syncNode(folder, null, 10L, "20", null, objectSummary);
		verifyZeroInteractions(entityManager, fileHandleDao);
	}

	@Test
	public void testSyncNewFile() throws Exception {
		Node folder = new Node();
		S3ObjectSummary objectSummary = new S3ObjectSummary();
		objectSummary.setKey("base/file1");
		objectSummary.setETag("aabb");
		when(nodeDao.getEntityHeaderByChildName("20", "file1")).thenThrow(new NotFoundException("dummy"));
		S3FileHandle fileHandle = new S3FileHandle();
		fileHandle.setId("30");
		when(fileHandleDao.createFile(any(S3FileHandle.class), eq(false))).thenReturn(fileHandle);

		syncFolderWorker.syncNode(folder, owner, 10L, "20", "base/", objectSummary);

		verify(entityManager).createEntity(eq(owner), any(FileEntity.class), isNull(String.class));
		verify(fileHandleDao).createFile(any(S3FileHandle.class), eq(false));
	}

	@Test
	public void testSyncUpdateFile() throws Exception {
		Node folder = new Node();
		S3ObjectSummary objectSummary = new S3ObjectSummary();
		objectSummary.setKey("file1");
		objectSummary.setETag("aabb");
		EntityHeader header = new EntityHeader();
		header.setType(FileEntity.class.getName());
		header.setId("30");
		when(nodeDao.getEntityHeaderByChildName("20", "file1")).thenReturn(header);
		FileEntity fileEntity = new FileEntity();
		fileEntity.setDataFileHandleId("33");
		fileEntity.setVersionNumber(222L);
		when(entityManager.getEntity(owner, "30", FileEntity.class)).thenReturn(fileEntity);
		S3FileHandle fileHandle = new S3FileHandle();
		fileHandle.setId("33");
		when(fileHandleDao.get("33")).thenReturn(fileHandle);
		when(fileHandleDao.createFile(any(S3FileHandle.class), eq(false))).thenReturn(fileHandle);

		syncFolderWorker.syncNode(folder, owner, 10L, "20", null, objectSummary);

		verify(entityManager).updateEntity(eq(owner), any(FileEntity.class), eq(true), isNull(String.class));
		verify(fileHandleDao).createFile(any(S3FileHandle.class), eq(false));
	}

	@Test
	public void testNullBasekey() throws Exception {
		Node folder = new Node();
		S3ObjectSummary objectSummary = new S3ObjectSummary();
		objectSummary.setKey("file1");
		objectSummary.setETag("aabb");
		when(nodeDao.getEntityHeaderByChildName("20", "file1")).thenThrow(new NotFoundException("dummy"));
		S3FileHandle fileHandle = new S3FileHandle();
		fileHandle.setId("30");
		when(fileHandleDao.createFile(any(S3FileHandle.class), eq(false))).thenReturn(fileHandle);

		syncFolderWorker.syncNode(folder, owner, 10L, "20", null, objectSummary);

		verify(entityManager).createEntity(eq(owner), any(FileEntity.class), isNull(String.class));
		verify(fileHandleDao).createFile(any(S3FileHandle.class), eq(false));
	}
}
