package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalObjectStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.StringInputStream;
import com.google.common.collect.Lists;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ProjectSettingsManagerAutowiredTest {

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

	@BeforeEach
	public void setUp() throws Exception {
		NewUser user = new NewUser();
		String username = UUID.randomUUID().toString();
		user.setEmail(username + "@test.com");
		user.setUserName(username);
		userInfo = userManager.getUserInfo(userManager.createUser(user));
		userInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		userInfo.setAcceptsTermsOfUse(true);

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
		s3Client.putObject(externalS3LocationSetting.getBucket(), externalS3LocationSetting.getBaseKey() + "/owner.txt",
				new StringInputStream(username), metadata);

		externalS3LocationSetting = projectSettingsManager.createStorageLocationSetting(userInfo, externalS3LocationSetting);

		storageLocationsDelete.add(externalLocationSetting.getStorageLocationId());
		storageLocationsDelete.add(externalS3LocationSetting.getStorageLocationId());
	}

	@AfterEach
	public void tearDown() {
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
	public void testCRUD() {
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
	public void testFind() {
		UploadDestinationListSetting toCreate = new UploadDestinationListSetting();
		toCreate.setProjectId(projectId);
		toCreate.setSettingsType(ProjectSettingsType.upload);
		toCreate.setLocations(Lists.newArrayList(externalLocationSetting.getStorageLocationId()));
		projectSettingsManager.createProjectSetting(userInfo, toCreate);

		Optional<UploadDestinationListSetting> setting = projectSettingsManager.getProjectSettingForNode(userInfo, projectId,
				ProjectSettingsType.upload, UploadDestinationListSetting.class);
		assertTrue(setting.isPresent());
		assertEquals(externalLocationSetting.getStorageLocationId(), setting.get().getLocations().get(0));

		setting = projectSettingsManager.getProjectSettingForNode(userInfo, childId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class);
		assertTrue(setting.isPresent());
		assertEquals(externalLocationSetting.getStorageLocationId(), setting.get().getLocations().get(0));

		setting = projectSettingsManager.getProjectSettingForNode(userInfo, childChildId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class);
		assertTrue(setting.isPresent());
		assertEquals(externalLocationSetting.getStorageLocationId(), setting.get().getLocations().get(0));
	}

	@Test
	public void testFindInParents() {
		Optional<UploadDestinationListSetting> setting = projectSettingsManager.getProjectSettingForNode(userInfo, childChildId,
				ProjectSettingsType.upload, UploadDestinationListSetting.class);
		assertFalse(setting.isPresent());

		UploadDestinationListSetting toCreate = new UploadDestinationListSetting();
		toCreate.setProjectId(projectId);
		toCreate.setSettingsType(ProjectSettingsType.upload);
		toCreate.setLocations(Lists.newArrayList(externalLocationSetting.getStorageLocationId()));
		projectSettingsManager.createProjectSetting(userInfo, toCreate);

		setting = projectSettingsManager.getProjectSettingForNode(userInfo, childChildId,
				ProjectSettingsType.upload, UploadDestinationListSetting.class);
		assertTrue(setting.isPresent());
		assertEquals(1, setting.get().getLocations().size());

		UploadDestinationListSetting toCreate2 = new UploadDestinationListSetting();
		toCreate2.setProjectId(childId);
		toCreate2.setSettingsType(ProjectSettingsType.upload);
		toCreate2.setLocations(Lists.newArrayList(externalLocationSetting.getStorageLocationId(),
				externalLocationSetting.getStorageLocationId()));
		projectSettingsManager.createProjectSetting(userInfo, toCreate2);

		setting = projectSettingsManager.getProjectSettingForNode(userInfo, childChildId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class);
		assertTrue(setting.isPresent());
		assertEquals(2, setting.get().getLocations().size());
		setting = projectSettingsManager.getProjectSettingForNode(userInfo, childId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class);
		assertTrue(setting.isPresent());
		assertEquals(2, setting.get().getLocations().size());
		setting = projectSettingsManager.getProjectSettingForNode(userInfo, projectId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class);
		assertTrue(setting.isPresent());
		assertEquals(1, setting.get().getLocations().size());

		UploadDestinationListSetting toCreate3 = new UploadDestinationListSetting();
		toCreate3.setProjectId(childChildId);
		toCreate3.setSettingsType(ProjectSettingsType.upload);
		toCreate3.setLocations(Lists.newArrayList(externalLocationSetting.getStorageLocationId(),
				externalLocationSetting.getStorageLocationId(), externalLocationSetting.getStorageLocationId()));
		projectSettingsManager.createProjectSetting(userInfo, toCreate3);

		setting = projectSettingsManager.getProjectSettingForNode(userInfo, childChildId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class);
		assertTrue(setting.isPresent());
		assertEquals(3, setting.get().getLocations().size());
		setting = projectSettingsManager.getProjectSettingForNode(userInfo, childId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class);
		assertTrue(setting.isPresent());
		assertEquals(2, setting.get().getLocations().size());
		setting = projectSettingsManager.getProjectSettingForNode(userInfo, projectId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class);
		assertTrue(setting.isPresent());
		assertEquals(1, setting.get().getLocations().size());
	}

	@Test
	public void testValidExternalObjectStorageSetting() throws IOException {
		ExternalObjectStorageLocationSetting externalObjectStorageLocationSetting = new ExternalObjectStorageLocationSetting();
		externalObjectStorageLocationSetting.setEndpointUrl("https://www.someurl.com");
		externalObjectStorageLocationSetting.setBucket("i-am-a-bucket-yay");
		//call under test
		ExternalObjectStorageLocationSetting result = projectSettingsManager.createStorageLocationSetting(userInfo, externalObjectStorageLocationSetting);
		assertNotNull(result);
		assertEquals(externalObjectStorageLocationSetting.getEndpointUrl(), result.getEndpointUrl());
		assertEquals(externalObjectStorageLocationSetting.getBucket(), result.getBucket());
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
		assertEquals(endpoint, result.getEndpointUrl());
		assertEquals(bucket, result.getBucket());
		assertNotNull(result.getStorageLocationId());
		storageLocationsDelete.add(result.getStorageLocationId());
	}

	@Test
	public void testExternalObjectStorageSettingInvalidBucket() {
		ExternalObjectStorageLocationSetting externalObjectStorageSetting = new ExternalObjectStorageLocationSetting();
		externalObjectStorageSetting.setBucket(" ");
		externalObjectStorageSetting.setEndpointUrl("https://www.someurl.com");
		//call under test
		Exception ex = assertThrows(IllegalArgumentException.class, () -> projectSettingsManager.createStorageLocationSetting(
				userInfo, externalObjectStorageSetting));
		assertEquals("Invalid bucket name.", ex.getMessage());
	}

	@Test
	public void testExternalObjectStorageSettingInvalidBucketWithSlashes() {
		ExternalObjectStorageLocationSetting externalObjectStorageSetting = new ExternalObjectStorageLocationSetting();
		externalObjectStorageSetting.setBucket(" / / / / / ");
		externalObjectStorageSetting.setEndpointUrl("https://www.someurl.com");
		//call under test
		Exception ex = assertThrows(IllegalArgumentException.class, () -> projectSettingsManager.createStorageLocationSetting(
				userInfo, externalObjectStorageSetting));
		assertEquals("Invalid bucket name.", ex.getMessage());
	}

	@Test
	public void testExternalObjectStorageSettingInvalidURL() {
		ExternalObjectStorageLocationSetting externalObjectStorageSetting = new ExternalObjectStorageLocationSetting();
		externalObjectStorageSetting.setBucket("someBucket");
		externalObjectStorageSetting.setEndpointUrl("not a url");
		//call under test
		Exception ex = assertThrows(IllegalArgumentException.class, () -> projectSettingsManager.createStorageLocationSetting(
				userInfo, externalObjectStorageSetting));
		assertEquals("The External URL is not a valid url: not a url", ex.getMessage());
	}
}
