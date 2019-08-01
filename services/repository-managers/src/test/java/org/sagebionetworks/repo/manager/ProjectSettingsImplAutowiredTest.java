package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.dao.StorageLocationUtils;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOStorageLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.migration.MergeStorageLocationsResponse;
import org.sagebionetworks.repo.model.project.ExternalObjectStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.util.TemporaryCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.StringInputStream;
import com.google.common.collect.Lists;

/**
 * Tests message access requirement checking and the sending of messages Note: only the logic for sending messages is
 * tested, a separate test handles tests of sending emails
 * 
 * Sorting of messages is not tested. All tests order their results as most recent first.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ProjectSettingsImplAutowiredTest {

	@Autowired
	private ProjectSettingsManager projectSettingsManager;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private UserManager userManager;
	
	@Autowired
	private StorageLocationDAO storageLocationDao;

	@Autowired
	private SynapseS3Client s3Client;
	
	private ExternalStorageLocationSetting externalLocationSetting;

	private ExternalS3StorageLocationSetting externalS3LocationSetting;

	private UserInfo userInfo;
	private String projectId;
	private String childId;
	private String childChildId;
	private List<Long> storageLocationsDelete;

	@Before
	public void setUp() throws Exception {
		NewUser user = new NewUser();
		String username = UUID.randomUUID().toString();
		user.setEmail(username + "@test.com");
		user.setUserName(username);
		userInfo = userManager.getUserInfo(userManager.createUser(user));
		userInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		Project project = new Project();
		project.setName("project" + RandomStringUtils.randomAlphanumeric(10));
		projectId = entityManager.createEntity(userInfo, project, null);

		Folder child = new Folder();
		child.setName("child");
		child.setParentId(projectId);
		childId = entityManager.createEntity(userInfo, child, null);

		Folder childChild = new Folder();
		childChild.setName("childchild");
		childChild.setParentId(childId);
		childChildId = entityManager.createEntity(userInfo, childChild, null);
		
		storageLocationsDelete = new ArrayList<>();

		externalLocationSetting = new ExternalStorageLocationSetting();
		externalLocationSetting.setUploadType(UploadType.SFTP);
		externalLocationSetting.setUrl("sftp://here.com");
		externalLocationSetting = projectSettingsManager.createStorageLocationSetting(userInfo, externalLocationSetting);

		externalS3LocationSetting = new ExternalS3StorageLocationSetting();
		externalS3LocationSetting.setUploadType(UploadType.S3);
		externalS3LocationSetting.setBucket(StackConfigurationSingleton.singleton().getExternalS3TestBucketName());
		externalS3LocationSetting.setBaseKey("key" + UUID.randomUUID());
		
		s3Client.createBucket(externalS3LocationSetting.getBucket());

		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(username.length());
		s3Client.putObject(externalS3LocationSetting.getBucket(), externalS3LocationSetting.getBaseKey() + "owner.txt",
				new StringInputStream(username), metadata);

		externalS3LocationSetting = projectSettingsManager.createStorageLocationSetting(userInfo, externalS3LocationSetting);

		storageLocationsDelete.add(externalLocationSetting.getStorageLocationId());
		storageLocationsDelete.add(externalS3LocationSetting.getStorageLocationId());
	}

	@After
	public void tearDown() throws Exception {
		UserInfo adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		entityManager.deleteEntity(adminUserInfo, childChildId);
		entityManager.deleteEntity(adminUserInfo, childId);
		entityManager.deleteEntity(adminUserInfo, projectId);
		userManager.deletePrincipal(adminUserInfo, Long.parseLong(userInfo.getId().toString()));
		
		for (Long storageLocationId : storageLocationsDelete) {
			storageLocationDao.delete(storageLocationId);
		}
	}

	@Test
	public void testCRUD() throws Exception {
		UploadDestinationListSetting toCreate = new UploadDestinationListSetting();
		toCreate.setProjectId(projectId);
		toCreate.setSettingsType(ProjectSettingsType.upload);
		toCreate.setLocations(Lists.newArrayList(externalLocationSetting.getStorageLocationId()));
		ProjectSetting settings = projectSettingsManager.createProjectSetting(userInfo, toCreate);
		assertTrue(settings instanceof UploadDestinationListSetting);

		ProjectSetting copy = projectSettingsManager.getProjectSetting(userInfo, settings.getId());
		assertEquals(settings, copy);

		((UploadDestinationListSetting) settings).setLocations(Lists.newArrayList(externalLocationSetting.getStorageLocationId(),
				externalS3LocationSetting.getStorageLocationId()));
		projectSettingsManager.updateProjectSetting(userInfo, settings);
		copy = projectSettingsManager.getProjectSetting(userInfo, settings.getId());
		assertNotSame(settings, copy);
		settings.setEtag(copy.getEtag());
		assertEquals(settings, copy);

		projectSettingsManager.deleteProjectSetting(userInfo, settings.getId());
	}

	@Test
	public void testFind() throws Exception {
		UploadDestinationListSetting toCreate = new UploadDestinationListSetting();
		toCreate.setProjectId(projectId);
		toCreate.setSettingsType(ProjectSettingsType.upload);
		toCreate.setLocations(Lists.newArrayList(externalLocationSetting.getStorageLocationId()));
		projectSettingsManager.createProjectSetting(userInfo, toCreate);

		UploadDestinationListSetting setting = projectSettingsManager.getProjectSettingForNode(userInfo, projectId,
				ProjectSettingsType.upload, UploadDestinationListSetting.class);
		assertEquals(externalLocationSetting.getStorageLocationId(), setting.getLocations().get(0));

		setting = projectSettingsManager.getProjectSettingForNode(userInfo, childId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class);
		assertEquals(externalLocationSetting.getStorageLocationId(), setting.getLocations().get(0));

		setting = projectSettingsManager.getProjectSettingForNode(userInfo, childChildId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class);
		assertEquals(externalLocationSetting.getStorageLocationId(), setting.getLocations().get(0));
	}

	@Test
	public void testFindInParents() throws Exception {
		UploadDestinationListSetting setting = projectSettingsManager.getProjectSettingForNode(userInfo, childChildId,
				ProjectSettingsType.upload, UploadDestinationListSetting.class);
		assertNull(setting);

		UploadDestinationListSetting toCreate = new UploadDestinationListSetting();
		toCreate.setProjectId(projectId);
		toCreate.setSettingsType(ProjectSettingsType.upload);
		toCreate.setLocations(Lists.newArrayList(externalLocationSetting.getStorageLocationId()));
		projectSettingsManager.createProjectSetting(userInfo, toCreate);

		setting = projectSettingsManager.getProjectSettingForNode(userInfo, childChildId,
				ProjectSettingsType.upload, UploadDestinationListSetting.class);
		assertEquals(1, setting.getLocations().size());

		UploadDestinationListSetting toCreate2 = new UploadDestinationListSetting();
		toCreate2.setProjectId(childId);
		toCreate2.setSettingsType(ProjectSettingsType.upload);
		toCreate2.setLocations(Lists.newArrayList(externalLocationSetting.getStorageLocationId(),
				externalLocationSetting.getStorageLocationId()));
		projectSettingsManager.createProjectSetting(userInfo, toCreate2);

		setting = projectSettingsManager.getProjectSettingForNode(userInfo, childChildId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class);
		assertEquals(2, setting.getLocations().size());
		setting = projectSettingsManager.getProjectSettingForNode(userInfo, childId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class);
		assertEquals(2, setting.getLocations().size());
		setting = projectSettingsManager.getProjectSettingForNode(userInfo, projectId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class);
		assertEquals(1, setting.getLocations().size());

		UploadDestinationListSetting toCreate3 = new UploadDestinationListSetting();
		toCreate3.setProjectId(childChildId);
		toCreate3.setSettingsType(ProjectSettingsType.upload);
		toCreate3.setLocations(Lists.newArrayList(externalLocationSetting.getStorageLocationId(),
				externalLocationSetting.getStorageLocationId(), externalLocationSetting.getStorageLocationId()));
		projectSettingsManager.createProjectSetting(userInfo, toCreate3);

		setting = projectSettingsManager.getProjectSettingForNode(userInfo, childChildId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class);
		assertEquals(3, setting.getLocations().size());
		setting = projectSettingsManager.getProjectSettingForNode(userInfo, childId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class);
		assertEquals(2, setting.getLocations().size());
		setting = projectSettingsManager.getProjectSettingForNode(userInfo, projectId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class);
		assertEquals(1, setting.getLocations().size());
	}

	@Test
	public void testValidExternalObjectStorageSetting() throws IOException {
		ExternalObjectStorageLocationSetting externalObjectStorageLocationSetting = new ExternalObjectStorageLocationSetting();
		externalObjectStorageLocationSetting.setEndpointUrl("https://www.someurl.com");
		externalObjectStorageLocationSetting.setBucket("i-am-a-bucket-yay");
		//call under test
		ExternalObjectStorageLocationSetting result = projectSettingsManager.createStorageLocationSetting(userInfo, externalObjectStorageLocationSetting);
		assertNotNull(result);
		Assert.assertEquals(externalObjectStorageLocationSetting.getEndpointUrl(), result.getEndpointUrl());
		Assert.assertEquals(externalObjectStorageLocationSetting.getBucket(), result.getBucket());
		assertNotNull(result.getStorageLocationId());
		storageLocationsDelete.add(result.getStorageLocationId());
	}

	@Test
	public void testValidExternalObjectStorageSettingWithSlashes() throws IOException {
		ExternalObjectStorageLocationSetting externalObjectStorageLocationSetting = new ExternalObjectStorageLocationSetting();
		String endpoint = "https://www.someurl.com";
		String bucket = "bucket-mc-bucket-face";
		externalObjectStorageLocationSetting.setEndpointUrl("////" + endpoint + "//////");
		externalObjectStorageLocationSetting.setBucket(bucket);

		//call under test
		ExternalObjectStorageLocationSetting result = projectSettingsManager.createStorageLocationSetting(userInfo, externalObjectStorageLocationSetting);

		assertNotNull(result);
		Assert.assertEquals(endpoint, result.getEndpointUrl());
		Assert.assertEquals(bucket, result.getBucket());
		assertNotNull(result.getStorageLocationId());
		storageLocationsDelete.add(result.getStorageLocationId());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testExternalObjectStorageSettingInvalidBucket() throws IOException {
		ExternalObjectStorageLocationSetting externalObjectStorageSetting = new ExternalObjectStorageLocationSetting();
		externalObjectStorageSetting.setBucket(" ");
		externalObjectStorageSetting.setEndpointUrl("https://www.someurl.com");
		//call under test
		projectSettingsManager.createStorageLocationSetting(userInfo, externalObjectStorageSetting);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testExternalObjectStorageSettingInvalidBucketWithSlashes() throws IOException {
		ExternalObjectStorageLocationSetting externalObjectStorageSetting = new ExternalObjectStorageLocationSetting();
		externalObjectStorageSetting.setBucket(" / / / / / ");
		externalObjectStorageSetting.setEndpointUrl("https://www.someurl.com");
		//call under test
		projectSettingsManager.createStorageLocationSetting(userInfo, externalObjectStorageSetting);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testExternalObjectStorageSettingInvalidURL() throws IOException {
		ExternalObjectStorageLocationSetting externalObjectStorageSetting = new ExternalObjectStorageLocationSetting();
		externalObjectStorageSetting.setBucket("someBucket");
		externalObjectStorageSetting.setEndpointUrl("not a url");
		//call under test
		projectSettingsManager.createStorageLocationSetting(userInfo, externalObjectStorageSetting);
	}
	
	@Test
	public void testMergeDuplicateStorageLocations() throws Exception {
		
		List<Long> storageLocationsIds = createStorageLocationsWithSameHash(2);
		Long firstCreatedId = storageLocationsIds.get(0);
		Long latestCreatedId = storageLocationsIds.get(1);
		
		List<Long> projectLocations = new ArrayList<>();
		
		projectLocations.add(externalLocationSetting.getStorageLocationId());
		
		// Just add one duplicate storage location
		projectLocations.add(firstCreatedId);
		
		UploadDestinationListSetting toCreate = new UploadDestinationListSetting();
		
		toCreate.setProjectId(projectId);
		toCreate.setSettingsType(ProjectSettingsType.upload);
		toCreate.setLocations(projectLocations);
		
		ProjectSetting settings = projectSettingsManager.createProjectSetting(userInfo, toCreate);
		
		// Call under test
		MergeStorageLocationsResponse response = projectSettingsManager.mergeDuplicateStorageLocations(new UserInfo(true));
		
		assertEquals(Long.valueOf(1), response.getDuplicateLocationsCount());
		assertEquals(Long.valueOf(1), response.getUpdatedProjectsCount());
		
		ProjectSetting setting = projectSettingsManager.getProjectSetting(userInfo, settings.getId());
		
		assertTrue(setting instanceof UploadDestinationListSetting);
		
		List<Long> updatedLocations = ((UploadDestinationListSetting)setting).getLocations();
		
		assertEquals(Arrays.asList(externalLocationSetting.getStorageLocationId(), latestCreatedId), updatedLocations);
		
	}
	
	@Autowired
	@TemporaryCode(author = "marco.marasca@sagebase.org")
	private DBOBasicDao basicDao;

	@Autowired
	@TemporaryCode(author = "marco.marasca@sagebase.org")
	private IdGenerator idGenerator;

	private List<Long> createStorageLocationsWithSameHash(int n) throws Exception {

		List<Long> createdIds = new ArrayList<>();

		String hash = UUID.randomUUID().toString();

		for (int i = 0; i < n; i++) {
			ExternalStorageLocationSetting locationSetting = TestUtils.createExternalStorageLocation(userInfo.getId(), "Description");
			Long id = createStorageLocationsWithHash(locationSetting, hash);
			createdIds.add(id);
		}

		return createdIds;
	}

	private Long createStorageLocationsWithHash(StorageLocationSetting setting, String hash) throws Exception {
		DBOStorageLocation dbo = StorageLocationUtils.convertDTOtoDBO(setting);
		dbo.setDataHash(hash);
		if (dbo.getId() == null) {
			dbo.setId(idGenerator.generateNewId(IdType.STORAGE_LOCATION_ID));
		}
		if (dbo.getEtag() == null) {
			dbo.setEtag(UUID.randomUUID().toString());
		}
		Long id = basicDao.createNew(dbo).getId();
		storageLocationsDelete.add(id);
		return id;
	}

}
