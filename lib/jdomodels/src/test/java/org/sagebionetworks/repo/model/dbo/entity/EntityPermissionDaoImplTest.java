package org.sagebionetworks.repo.model.dbo.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.dao.DataTypeDao;
import org.sagebionetworks.repo.model.helper.DoaObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Sets;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class EntityPermissionDaoImplTest {

	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	AccessControlListDAO aclDao;

	@Autowired
	DataTypeDao dataTypeDao;

	@Autowired
	private DoaObjectHelper<Node> nodeDaoHelper;

	@Autowired
	private DoaObjectHelper<UserGroup> userGroupHelpler;

	@Autowired
	private DoaObjectHelper<AccessControlList> aclHelper;

	@Autowired
	EntityPermissionDao entityPermissionDao;

	Long userOneId;
	Long userTwoId;
	Long userThreeId;
	Long teamOneId;

	Set<Long> userOneGroups;

	Node project;
	Node folder;
	Node file;

	Long projectId;
	Long folderId;
	Long fileId;

	@BeforeEach
	public void before() throws Exception {

		UserGroup ug = new UserGroup();
		ug.setIsIndividual(true);
		ug.setCreationDate(new Date());
		userOneId = Long.parseLong(userGroupHelpler.create(u -> {
		}).getId());
		userTwoId = Long.parseLong(userGroupHelpler.create(u -> {
		}).getId());
		userThreeId = Long.parseLong(userGroupHelpler.create(u -> {
		}).getId());

		teamOneId = Long.parseLong(userGroupHelpler.create(u -> {
			u.setIsIndividual(false);
		}).getId());

		userOneGroups = Sets.newHashSet(userOneId, teamOneId, BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId(),
				BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId(),
				BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());

	}

	@AfterEach
	public void after() {
		aclDao.truncateAll();
		dataTypeDao.truncateAllData();
		nodeDao.truncateAll();
		if (userThreeId != null) {
			userGroupDAO.delete(userThreeId.toString());
		}
	}

	@Test
	public void testGetEntityPermissionsWithNullGroups() {
		userOneGroups = null;
		List<Long> entityIds = Arrays.asList(111L, 222L);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		}).getMessage();
		assertEquals("userGroups is required.", message);
	}

	@Test
	public void testGetEntityPermissionsWithEmptyGroups() {
		userOneGroups = Collections.emptySet();
		List<Long> entityIds = Arrays.asList(111L, 222L);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		}).getMessage();
		assertEquals("User's groups cannot be empty", message);
	}

	@Test
	public void testGetEntityPermissionsWithNullEntityIds() {
		List<Long> entityIds = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		}).getMessage();
		assertEquals("entityIdss is required.", message);
	}

	@Test
	public void testGetEntityPermissionsWithEmptyEntityIds() {
		List<Long> entityIds = Collections.emptyList();
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Collections.emptyList();
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithNoAcl() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays
				.asList(new EntityPermission(fileId).withBenefactorId(null).withEntityType(null).withtHasRead(false));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithAclOnFile() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(file.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.READ));
		});
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays.asList(new EntityPermission(fileId).withBenefactorId(fileId)
				.withEntityType(EntityType.file).withtHasRead(true));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithAclOnFolder() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(folder.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.READ));
		});
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays.asList(new EntityPermission(fileId).withBenefactorId(folderId)
				.withEntityType(EntityType.file).withtHasRead(true));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithAclOnProject() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.READ));
		});
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays.asList(new EntityPermission(fileId).withBenefactorId(projectId)
				.withEntityType(EntityType.file).withtHasRead(true));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithAclOnProjectNoGrantAndAclOnFileWithGrant() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwoId, ACCESS_TYPE.READ));
		});
		aclHelper.create((a) -> {
			a.setId(file.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.READ));
		});
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays.asList(new EntityPermission(fileId).withBenefactorId(fileId)
				.withEntityType(EntityType.file).withtHasRead(true));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithAclOnProjectWithGrantAndAclOnFileWithoutGrant() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.READ));
		});
		aclHelper.create((a) -> {
			a.setId(file.getId());
			a.getResourceAccess().add(createResourceAccess(userTwoId, ACCESS_TYPE.READ));
		});
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays.asList(new EntityPermission(fileId).withBenefactorId(fileId)
				.withEntityType(EntityType.file).withtHasRead(false));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithAclOnProjectWithDuplicateGrantToMultiplePricipals() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.READ));
			a.getResourceAccess().add(createResourceAccess(teamOneId, ACCESS_TYPE.READ));
		});
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays.asList(new EntityPermission(fileId).withBenefactorId(projectId)
				.withEntityType(EntityType.file).withtHasRead(true));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithCanDownload() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.DOWNLOAD));
		});
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays.asList(new EntityPermission(fileId).withBenefactorId(projectId)
				.withEntityType(EntityType.file).withHasDownload(true).withtHasRead(false));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithCanDownloadAndCanRead() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.DOWNLOAD));
			a.getResourceAccess().add(createResourceAccess(teamOneId, ACCESS_TYPE.READ));
		});
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays.asList(new EntityPermission(fileId).withBenefactorId(projectId)
				.withEntityType(EntityType.file).withHasDownload(true).withtHasRead(true));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithMultipleFiles() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId, projectId, folderId);
		aclHelper.create((a) -> {
			a.setId(file.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.READ));
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.DOWNLOAD));
		});
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays.asList(
				new EntityPermission(fileId).withBenefactorId(fileId).withEntityType(EntityType.file)
						.withHasDownload(false).withtHasRead(true),
				new EntityPermission(projectId).withBenefactorId(projectId).withEntityType(EntityType.project)
						.withHasDownload(true).withtHasRead(false),
				new EntityPermission(folderId).withBenefactorId(projectId).withEntityType(EntityType.folder)
						.withHasDownload(true).withtHasRead(false));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithOpenData() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.READ));
		});
		dataTypeDao.changeDataType(userOneId, file.getId(), ObjectType.ENTITY, DataType.OPEN_DATA);
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays
				.asList(new EntityPermission(fileId).withBenefactorId(projectId).withEntityType(EntityType.file)
						.withHasDownload(false).withtHasRead(true).withDataType(DataType.OPEN_DATA));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithSensitiveData() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.READ));
		});
		dataTypeDao.changeDataType(userOneId, file.getId(), ObjectType.ENTITY, DataType.SENSITIVE_DATA);
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays
				.asList(new EntityPermission(fileId).withBenefactorId(projectId).withEntityType(EntityType.file)
						.withHasDownload(false).withtHasRead(true).withDataType(DataType.SENSITIVE_DATA));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithNoDataTypeSet() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.READ));
		});
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays
				.asList(new EntityPermission(fileId).withBenefactorId(projectId).withEntityType(EntityType.file)
						.withHasDownload(false).withtHasRead(true).withDataType(DataType.SENSITIVE_DATA));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithChangePermission() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.CHANGE_PERMISSIONS));
		});
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays.asList(new EntityPermission(fileId).withBenefactorId(projectId)
				.withEntityType(EntityType.file).withHasChangePermissions(true).withHasChangeSettings(false)
				.withHasCreate(false).withHasDelete(false).withHasDownload(false).withtHasRead(false)
				.withHasModerate(false).withDataType(DataType.SENSITIVE_DATA));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithChangeSettings() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.CHANGE_SETTINGS));
		});
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays.asList(new EntityPermission(fileId).withBenefactorId(projectId)
				.withEntityType(EntityType.file).withHasChangePermissions(false).withHasChangeSettings(true)
				.withHasCreate(false).withHasDelete(false).withHasDownload(false).withtHasRead(false)
				.withHasModerate(false).withDataType(DataType.SENSITIVE_DATA));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithCreate() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.CREATE));
		});
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays.asList(new EntityPermission(fileId).withBenefactorId(projectId)
				.withEntityType(EntityType.file).withHasChangePermissions(false).withHasChangeSettings(false)
				.withHasCreate(true).withHasDelete(false).withHasDownload(false).withtHasRead(false)
				.withHasModerate(false).withDataType(DataType.SENSITIVE_DATA));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithDelete() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.DELETE));
		});
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays.asList(new EntityPermission(fileId).withBenefactorId(projectId)
				.withEntityType(EntityType.file).withHasChangePermissions(false).withHasChangeSettings(false)
				.withHasCreate(false).withHasDelete(true).withHasDownload(false).withtHasRead(false)
				.withHasModerate(false).withDataType(DataType.SENSITIVE_DATA));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithDownload() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.DOWNLOAD));
		});
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays.asList(new EntityPermission(fileId).withBenefactorId(projectId)
				.withEntityType(EntityType.file).withHasChangePermissions(false).withHasChangeSettings(false)
				.withHasCreate(false).withHasDelete(false).withHasDownload(true).withtHasRead(false)
				.withHasModerate(false).withDataType(DataType.SENSITIVE_DATA));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithRead() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.READ));
		});
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays.asList(new EntityPermission(fileId).withBenefactorId(projectId)
				.withEntityType(EntityType.file).withHasChangePermissions(false).withHasChangeSettings(false)
				.withHasCreate(false).withHasDelete(false).withHasDownload(false).withtHasRead(true)
				.withHasModerate(false).withDataType(DataType.SENSITIVE_DATA));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithModerate() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.MODERATE));
		});
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays.asList(new EntityPermission(fileId).withBenefactorId(projectId)
				.withEntityType(EntityType.file).withHasChangePermissions(false).withHasChangeSettings(false)
				.withHasCreate(false).withHasDelete(false).withHasDownload(false).withtHasRead(false)
				.withHasModerate(true).withDataType(DataType.SENSITIVE_DATA));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithAllPermissions() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess()
					.add(createResourceAccess(userOneId, ACCESS_TYPE.CHANGE_PERMISSIONS, ACCESS_TYPE.CHANGE_SETTINGS,
							ACCESS_TYPE.CREATE, ACCESS_TYPE.DELETE, ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.READ,
							ACCESS_TYPE.MODERATE));
		});
		// call under test
		List<EntityPermission> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<EntityPermission> expected = Arrays.asList(new EntityPermission(fileId).withBenefactorId(projectId)
				.withEntityType(EntityType.file).withHasChangePermissions(true).withHasChangeSettings(true)
				.withHasCreate(true).withHasDelete(true).withHasDownload(true).withtHasRead(true).withHasModerate(true)
				.withDataType(DataType.SENSITIVE_DATA));
		assertEquals(expected, results);
	}

	/**
	 * Helper to setup a node hierarchy with the provided user as the creator.
	 * 
	 * @param userId
	 */
	public void setupNodeHierarchy(Long userId) {

		project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userId);
		});
		projectId = KeyFactory.stringToKey(project.getId());
		folder = nodeDaoHelper.create(n -> {
			n.setName("aFolder");
			n.setCreatedByPrincipalId(userId);
			n.setParentId(project.getId());
			n.setNodeType(EntityType.folder);
		});
		folderId = KeyFactory.stringToKey(folder.getId());
		file = nodeDaoHelper.create(n -> {
			n.setName("aFile");
			n.setCreatedByPrincipalId(userId);
			n.setParentId(folder.getId());
			n.setNodeType(EntityType.file);
		});
		fileId = KeyFactory.stringToKey(file.getId());
	}

}
