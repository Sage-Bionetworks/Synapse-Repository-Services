package org.sagebionetworks.migration.worker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.dao.StorageLocationUtils;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOStorageLocation;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.migration.CleanupStorageLocationsRequest;
import org.sagebionetworks.repo.model.migration.CleanupStorageLocationsResponse;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.TemporaryCode;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
@TemporaryCode(author = "marco.marasca@sagebase.org")
public class StorageLocationsCleanupTest {

	@Autowired
	private UserManager userManager;
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private ProjectSettingsManager projectSettingManager;
	
	@Autowired
	private StorageLocationsCleanup storageLocationsCleanup;
	
	@Autowired
	private FileHandleDao fileHandleDao;
	
	@Autowired
	private StorageLocationDAO storageLocationDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	private UserInfo adminUserInfo;
	
	private UserInfo userInfo;
	
	private ExternalStorageLocationSetting adminStorageLocation;
	
	private ExternalStorageLocationSetting userStorageLocation;
	
	private FileHandle adminFileHandle;
	
	private FileHandle userFileHandle;
	
	private String userProjectId;
	
	private String adminProjectId;
	
	private UploadDestinationListSetting userProjectSetting;
	
	private UploadDestinationListSetting adminProjectSetting;
	
	private Long masterStorageLocationId;
	private List<Long> duplicateStorageLocationsIds;
	
	@Before
	public void setup() throws Exception {
		// Get the admin user
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		NewUser user = new NewUser();
		String username = UUID.randomUUID().toString();
		user.setEmail(username + "@test.com");
		user.setUserName(username);
		userInfo = userManager.getUserInfo(userManager.createUser(user));
		userInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		
		Project project = new Project();
		project.setName("project" + RandomStringUtils.randomAlphanumeric(10));
		userProjectId = entityManager.createEntity(userInfo, project, null);
		
		userStorageLocation = TestUtils.createExternalStorageLocation(userInfo.getId(), "Some user storage location");
		adminStorageLocation = TestUtils.createExternalStorageLocation(userInfo.getId(), "Some admin storage location");
		
		userStorageLocation = projectSettingManager.createStorageLocationSetting(userInfo, userStorageLocation);
		adminStorageLocation = projectSettingManager.createStorageLocationSetting(adminUserInfo, adminStorageLocation);
		
		userProjectSetting = new UploadDestinationListSetting();
		userProjectSetting.setProjectId(userProjectId);
		userProjectSetting.setSettingsType(ProjectSettingsType.upload);
		userProjectSetting.setLocations(Lists.newArrayList(userStorageLocation.getStorageLocationId()));
		
		userProjectSetting = (UploadDestinationListSetting) projectSettingManager.createProjectSetting(userInfo, userProjectSetting);
		
		userFileHandle = TestUtils.createExternalFileHandle(userInfo.getId().toString(), idGenerator.generateNewId(IdType.FILE_IDS).toString());
		userFileHandle.setStorageLocationId(userStorageLocation.getStorageLocationId());
		userFileHandle = fileHandleDao.createFile(userFileHandle);
		
		adminFileHandle = TestUtils.createExternalFileHandle(adminUserInfo.getId().toString(), idGenerator.generateNewId(IdType.FILE_IDS).toString());
		adminFileHandle.setStorageLocationId(adminStorageLocation.getStorageLocationId());
		adminFileHandle  = fileHandleDao.createFile(adminFileHandle);
		
		List<Long> storageLocations = createStorageLocationsWithSameHash(adminUserInfo.getId(), 3);
		
		masterStorageLocationId = storageLocations.get(2);
		duplicateStorageLocationsIds = storageLocations.subList(0, 2);
		
		Project adminProject = new Project();
		adminProject.setName("project" + RandomStringUtils.randomAlphanumeric(10));
		adminProjectId = entityManager.createEntity(adminUserInfo, adminProject, null);
		
		adminProjectSetting = new UploadDestinationListSetting();
		adminProjectSetting.setProjectId(adminProjectId);
		adminProjectSetting.setSettingsType(ProjectSettingsType.upload);
		adminProjectSetting.setLocations(storageLocations);
		
		adminProjectSetting = (UploadDestinationListSetting) projectSettingManager.createProjectSetting(adminUserInfo, adminProjectSetting);
		
	}
	
	@Test
	public void testCleanupStorageLocations() {
		
		CleanupStorageLocationsRequest request = new CleanupStorageLocationsRequest();
		request.setUsers(Arrays.asList(userInfo.getId()));
		
		// Call under test
		CleanupStorageLocationsResponse response = storageLocationsCleanup.cleanupStorageLocations(adminUserInfo, request);
		
		assertEquals(1L, response.getUpdatedFilesCount());
		assertEquals(1L, response.getDeletedProjectSettingsCount());
		
		assertNull(fileHandleDao.get(userFileHandle.getId()).getStorageLocationId());
		assertEquals(adminStorageLocation.getStorageLocationId(), fileHandleDao.get(adminFileHandle.getId()).getStorageLocationId());
		
		try {
			projectSettingManager.getProjectSetting(userInfo, userProjectSetting.getId());
			fail("Didn't delete the project setting of the user");
		} catch(NotFoundException e) {
			// pass
		}
		
		DBOStorageLocation masterStorageLocation = basicDao.getObjectByPrimaryKey(DBOStorageLocation.class, new SinglePrimaryKeySqlParameterSource(masterStorageLocationId));
		
		assertFalse(masterStorageLocation.getDataHash().contains("_d_"));
		
		for (Long duplicateLocation : duplicateStorageLocationsIds) {
			DBOStorageLocation dbo = basicDao.getObjectByPrimaryKey(DBOStorageLocation.class, new SinglePrimaryKeySqlParameterSource(duplicateLocation));
			assertTrue(dbo.getDataHash().contains(masterStorageLocation.getDataHash() + "_d_"));
		}
		
		
	}
	
	@Autowired
	private DBOBasicDao basicDao;

	private List<Long> createStorageLocationsWithSameHash(Long userId, int n) throws Exception {

		List<Long> createdIds = new ArrayList<>();

		String hash = UUID.randomUUID().toString();

		for (int i = 0; i < n; i++) {
			ExternalStorageLocationSetting locationSetting = TestUtils.createExternalStorageLocation(userId, "Description");
			Long id = createStorageLocationWithHash(locationSetting, hash);
			createdIds.add(id);
		}

		return createdIds;
	}

	private Long createStorageLocationWithHash(StorageLocationSetting setting, String hash) throws Exception {
		DBOStorageLocation dbo = StorageLocationUtils.convertDTOtoDBO(setting);
		dbo.setDataHash(hash);
		if (dbo.getId() == null) {
			dbo.setId(idGenerator.generateNewId(IdType.STORAGE_LOCATION_ID));
		}
		if (dbo.getEtag() == null) {
			dbo.setEtag(UUID.randomUUID().toString());
		}
		Long id = basicDao.createNew(dbo).getId();
		return id;
	}

}
