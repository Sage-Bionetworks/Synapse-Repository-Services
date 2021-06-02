package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOProjectSettingsDAOImplTest {
	private static final Long USER_ID = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();

	@Autowired
	NodeDAO nodeDao;

	@Autowired
	ProjectSettingsDAO projectSettingsDao;

	@Autowired
	StorageLocationDAO storageLocationDAO;

	private String projectId;

	@BeforeEach
	public void setup() {
		Node project = createNodeInstance(EntityType.project, null);
		project.setName("project");
		projectId = nodeDao.createNewNode(project).getId();
	}

	@AfterEach
	public void teardown() throws Exception {
		if (projectId != null) {
			nodeDao.delete(projectId);
		}
	}

	@Test
	public void testCRUD() throws Exception {
		// Create a new type
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setEtag("etag");
		setting.setId(null);
		setting.setProjectId(projectId);
		setting.setSettingsType(ProjectSettingsType.upload);

		// there should not be a settings to begin with
		assertFalse(projectSettingsDao.get(projectId, ProjectSettingsType.upload).isPresent());
		assertEquals(0, projectSettingsDao.getAllForProject(projectId).size());

		// Create it
		String id = projectSettingsDao.create(setting);
		setting.setId(id);
		assertNotNull(id);

		// Fetch it
		ProjectSetting clone = projectSettingsDao.get(projectId, ProjectSettingsType.upload).get();
		assertNotNull(clone);
		assertEquals(setting, clone);

		// Fetch it by id
		clone = projectSettingsDao.get(id);
		assertEquals(setting, clone);

		// Fetch all by project
		List<ProjectSetting> all = projectSettingsDao.getAllForProject(projectId);
		assertEquals(1, all.size());
		assertEquals(ImmutableList.of(setting), all);

		// Update it
		ProjectSetting updatedClone = projectSettingsDao.update(clone);
		assertNotSame("etags should be different after an update", clone.getEtag(), updatedClone.getEtag());

		try {
			projectSettingsDao.update(clone);
			fail("conflicting update exception not thrown");
		} catch (ConflictingUpdateException e) {
			// We expected this exception
		}

		// Delete it
		projectSettingsDao.delete(id);

		assertFalse(projectSettingsDao.get(projectId, ProjectSettingsType.upload).isPresent());
		assertEquals(0, projectSettingsDao.getAllForProject(projectId).size());
	}

	@Test
	public void testCascadeDelete() throws Exception {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setEtag("etag");
		setting.setId(null);
		setting.setProjectId(projectId);
		setting.setSettingsType(ProjectSettingsType.upload);
		projectSettingsDao.create(setting);
		assertEquals(1, projectSettingsDao.getAllForProject(projectId).size());

		nodeDao.delete(projectId);
		assertEquals(0, projectSettingsDao.getAllForProject(projectId).size());
		projectId = null;
	}

	@Test
	public void testProjectIdMustBeSet() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId(null);
		setting.setSettingsType(ProjectSettingsType.upload);
		
		InvalidModelException ex = Assertions.assertThrows(InvalidModelException.class, () -> {
			// Call under test
			projectSettingsDao.create(setting);
		});
		
		assertEquals("projectId must be specified", ex.getMessage());
	}

	@Test
	public void testProjectIdMustBeValid() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId("123");
		setting.setSettingsType(ProjectSettingsType.upload);
		
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			projectSettingsDao.create(setting);
		});
	}

	@Test
	public void testTypeMustBeSet() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId(projectId);
		setting.setSettingsType(null);
		
		InvalidModelException ex = Assertions.assertThrows(InvalidModelException.class, () -> {
			// Call under test
			projectSettingsDao.create(setting);
		});
		
		assertEquals("settingsType must be specified", ex.getMessage());
	}

	@Test
	public void testGetUploadLocations() {
		ExternalStorageLocationSetting locationSetting1 = new ExternalStorageLocationSetting();
		locationSetting1.setUploadType(UploadType.SFTP);
		locationSetting1.setUrl("sftp://");
		locationSetting1.setCreatedBy(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		locationSetting1.setCreatedOn(new Date());
		ExternalS3StorageLocationSetting locationSetting2 = new ExternalS3StorageLocationSetting();
		locationSetting2.setUploadType(UploadType.S3);
		locationSetting2.setBucket("bucket");
		locationSetting2.setCreatedBy(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		locationSetting2.setCreatedOn(new Date());
		Long l1 = storageLocationDAO.create(locationSetting1);
		Long l2 = storageLocationDAO.create(locationSetting2);
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId(projectId);
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.newArrayList(l1, l2));
		projectSettingsDao.create(setting);

		ProjectSetting projectSetting = projectSettingsDao.get(projectId, ProjectSettingsType.upload).get();
		assertEquals(l1, ((UploadDestinationListSetting) projectSetting).getLocations().get(0));
		assertEquals(l2, ((UploadDestinationListSetting) projectSetting).getLocations().get(1));
	}

	@Test
	public void testFailOnDuplicatEntry() {
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setProjectId(projectId);
		setting.setSettingsType(ProjectSettingsType.upload);
		setting.setLocations(Lists.<Long> newArrayList());
		projectSettingsDao.create(setting);
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			projectSettingsDao.create(setting);
		});
		
		assertEquals("A project setting of type 'upload' for project " + projectId + " already exists.", ex.getMessage());
	}

	@Test
	public void testGetInheritedForEntity() {
		
		// Set up test by creating a linear folder hierarchy of 4 folders.
		Node folderA = createNodeInstance(EntityType.folder, projectId);
		folderA = nodeDao.createNewNode(folderA);

		Node folderB = createNodeInstance(EntityType.folder, folderA.getId());
		folderB = nodeDao.createNewNode(folderB);

		Node folderC = createNodeInstance(EntityType.folder, folderB.getId());
		folderC = nodeDao.createNewNode(folderC);

		Node folderD = createNodeInstance(EntityType.folder, folderC.getId());
		folderD = nodeDao.createNewNode(folderD);
		
		// Create Project Settings on A and C.
		UploadDestinationListSetting settingA = new UploadDestinationListSetting();
		settingA.setProjectId(folderA.getId());
		settingA.setSettingsType(ProjectSettingsType.upload);
		String settingAId = projectSettingsDao.create(settingA);
		settingA = (UploadDestinationListSetting) projectSettingsDao.get(settingAId);

		UploadDestinationListSetting settingC = new UploadDestinationListSetting();
		settingC.setProjectId(folderC.getId());
		settingC.setSettingsType(ProjectSettingsType.upload);
		String settingCId = projectSettingsDao.create(settingC);
		settingC = (UploadDestinationListSetting) projectSettingsDao.get(settingCId);

		// Methods under test.
		String result = projectSettingsDao.getInheritedProjectSetting(folderD.getId(), ProjectSettingsType.upload);
		assertEquals(settingC.getId(), result);

		result = projectSettingsDao.getInheritedProjectSetting(folderC.getId(), ProjectSettingsType.upload);
		assertEquals(settingC.getId(), result);

		result = projectSettingsDao.getInheritedProjectSetting(folderB.getId(), ProjectSettingsType.upload);
		assertEquals(settingA.getId(), result);

		result = projectSettingsDao.getInheritedProjectSetting(folderA.getId(), ProjectSettingsType.upload);
		assertEquals(settingA.getId(), result);

		result = projectSettingsDao.getInheritedProjectSetting(projectId, ProjectSettingsType.upload);
		assertNull(result);
	}

	private static Node createNodeInstance(EntityType type, String parentId) {
		Node node = new Node();
		node.setNodeType(type);
		node.setCreatedByPrincipalId(USER_ID);
		node.setCreatedOn(new Date());
		node.setModifiedByPrincipalId(USER_ID);
		node.setModifiedOn(new Date());
		node.setParentId(parentId);
		return node;
	}
}
