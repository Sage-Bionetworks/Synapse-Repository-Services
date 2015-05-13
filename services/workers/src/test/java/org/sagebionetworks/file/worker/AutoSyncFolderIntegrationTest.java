package org.sagebionetworks.file.worker;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.junit.BeforeAll;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.S3TestUtils;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalSyncSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.util.ThreadLocalProvider;
import org.sagebionetworks.util.TimedAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * This test validates that when a file is created, the message propagates to the preview queue, is processed by the
 * preview worker and a preview is created.
 * 
 * @author John
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AutoSyncFolderIntegrationTest {

	private static final String DESTINATION_TEST_BUCKET = "dev.test.destination.bucket.sagebase.org";
	public static final long MAX_WAIT = 30 * 1000; // 30 seconds

	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;

	@Autowired
	private FileHandleManager fileUploadManager;

	@Autowired
	private UserManager userManager;

	@Autowired
	private AmazonS3Client s3Client;

	@Autowired
	private FileHandleDao fileMetadataDao;

	@Autowired
	private SemaphoreManager semphoreManager;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private AutowireCapableBeanFactory factory;

	@Autowired
	private ProjectSettingsManager projectSettingsManager;

	private static final ThreadLocal<Long> currentUserIdThreadLocal = ThreadLocalProvider.getInstance(AuthorizationConstants.USER_ID_PARAM,
			Long.class);

	private UserInfo adminUserInfo;
	private List<String> entities = Lists.newArrayList();
	private Long testBucketLocationId;
	private String testBucketBaseKey;
	private String projectId;
	private ProjectSetting syncSetting;

	@Before
	public void before() throws Exception {
		s3Client.createBucket(DESTINATION_TEST_BUCKET);

		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		testBucketBaseKey = UUID.randomUUID().toString() + '/';

		S3TestUtils.createObjectFromString(DESTINATION_TEST_BUCKET, testBucketBaseKey + "owner.txt",
				StackConfiguration.getMigrationAdminUsername(), s3Client);

		ExternalS3StorageLocationSetting storageLocationSetting = new ExternalS3StorageLocationSetting();
		storageLocationSetting.setBucket(DESTINATION_TEST_BUCKET);
		storageLocationSetting.setBaseKey(testBucketBaseKey);
		storageLocationSetting.setUploadType(UploadType.S3);
		storageLocationSetting = projectSettingsManager.createStorageLocationSetting(adminUserInfo, storageLocationSetting);
		testBucketLocationId = storageLocationSetting.getStorageLocationId();

		Project project = new Project();
		project.setName("project" + UUID.randomUUID());
		projectId = entityManager.createEntity(adminUserInfo, project, null);
		entities.add(projectId);

		ExternalSyncSetting externalSyncSetting = new ExternalSyncSetting();
		externalSyncSetting.setAutoSync(true);
		externalSyncSetting.setProjectId(projectId);
		externalSyncSetting.setSettingsType(ProjectSettingsType.external_sync);
		externalSyncSetting.setLocationId(testBucketLocationId);
		this.syncSetting = projectSettingsManager.createProjectSetting(adminUserInfo, externalSyncSetting);

		currentUserIdThreadLocal.set(adminUserInfo.getId());
	}

	@After
	public void after() throws Exception {
		currentUserIdThreadLocal.set(null);
		for (String entity : Lists.reverse(entities)) {
			entityManager.deleteEntity(adminUserInfo, entity);
		}
		S3TestUtils.doDeleteAfter(s3Client);
	}

	@Test
	public void testUpdate() throws Exception {

		assertEquals(0, nodeDao.getChildrenIds(projectId).size());

		S3TestUtils.createObjectFromString(DESTINATION_TEST_BUCKET, testBucketBaseKey + "file1", "abc", s3Client);

		// force update message
		projectSettingsManager.updateProjectSetting(adminUserInfo, syncSetting);
		TimedAssert.waitForAssert(MAX_WAIT, 100, new Runnable() {
			@Override
			public void run() {
				assertEquals(1, nodeDao.getChildrenIds(projectId).size());
			}
		});
	}

	@Test
	public void testUpdateVersion() throws Exception {
		if (!StackConfiguration.singleton().getAutoSyncSubFoldersAllowed()) {
			// intially, this feature is not enabled
			return;
		}
		S3TestUtils.createObjectFromString(DESTINATION_TEST_BUCKET, testBucketBaseKey + "file1", "abc", s3Client);

		// force update message
		projectSettingsManager.updateProjectSetting(adminUserInfo, syncSetting);
		TimedAssert.waitForAssert(MAX_WAIT, 100, new Runnable() {
			@Override
			public void run() {
				assertEquals(1, nodeDao.getChildrenIds(projectId).size());
			}
		});
		final Long version = Iterables.getOnlyElement(nodeDao.getChildren(projectId)).getVersionNumber();

		S3TestUtils.createObjectFromString(DESTINATION_TEST_BUCKET, testBucketBaseKey + "file1", "abcd", s3Client);

		// force update message
		syncSetting = projectSettingsManager.getProjectSetting(adminUserInfo, syncSetting.getId());
		projectSettingsManager.updateProjectSetting(adminUserInfo, syncSetting);
		TimedAssert.waitForAssert(MAX_WAIT, 100, new Runnable() {
			@Override
			public void run() {
				assertEquals(1, nodeDao.getChildrenIds(projectId).size());
				assertEquals(version + 1, Iterables.getOnlyElement(nodeDao.getChildren(projectId)).getVersionNumber().longValue());
			}
		});
	}

	@Test
	public void testUpdatePath() throws Exception {
		if (!StackConfiguration.singleton().getAutoSyncSubFoldersAllowed()) {
			// intially, this feature is not enabled
			return;
		}

		S3TestUtils.createObjectFromString(DESTINATION_TEST_BUCKET, testBucketBaseKey + "folder1/folder2/file1", "abc", s3Client);
		S3TestUtils.createObjectFromString(DESTINATION_TEST_BUCKET, testBucketBaseKey + "folder1/folder2/file2", "abc", s3Client);

		// force update message
		projectSettingsManager.updateProjectSetting(adminUserInfo, syncSetting);
		TimedAssert.waitForAssert(MAX_WAIT, 100, new Runnable() {
			@Override
			public void run() {
				List<Folder> folders = entityManager.getEntityChildren(adminUserInfo, projectId, Folder.class);
				assertEquals(1, folders.size());
				assertEquals("folder1", folders.get(0).getName());

				folders = entityManager.getEntityChildren(adminUserInfo, folders.get(0).getId(), Folder.class);
				assertEquals(1, folders.size());
				assertEquals("folder2", folders.get(0).getName());

				List<FileEntity> files = entityManager.getEntityChildren(adminUserInfo, folders.get(0).getId(), FileEntity.class);
				assertEquals(2, files.size());
				assertEquals(Sets.newHashSet("file1", "file2"), Sets.newHashSet(files.get(0).getName(), files.get(1).getName()));
			}
		});
	}
}
