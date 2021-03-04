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
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.dao.DataTypeDao;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Sets;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class UsersEntityPermissionsDaoImplTest {

	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private AccessControlListDAO aclDao;

	@Autowired
	private DataTypeDao dataTypeDao;

	@Autowired
	private DaoObjectHelper<Node> nodeDaoHelper;

	@Autowired
	private DaoObjectHelper<UserGroup> userGroupHelpler;

	@Autowired
	private AccessControlListObjectHelper aclHelper;

	@Autowired
	UsersEntityPermissionsDao entityPermissionDao;

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
		if (userOneId != null) {
			userGroupDAO.delete(userOneId.toString());
		}
		if (userTwoId != null) {
			userGroupDAO.delete(userTwoId.toString());
		}
		if (userThreeId != null) {
			userGroupDAO.delete(userThreeId.toString());
		}
		if (teamOneId != null) {
			userGroupDAO.delete(teamOneId.toString());
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
		assertEquals("entityIds is required.", message);
	}

	@Test
	public void testGetEntityPermissionsWithEmptyEntityIds() {
		List<Long> entityIds = Collections.emptyList();
		// call under test
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Collections.emptyList();
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithNoAcl() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		// call under test
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays.asList(new UserEntityPermissionsState(fileId)
				.withtDoesEntityExist(false).withBenefactorId(null).withEntityType(null).withHasRead(false));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithEntityDoesNotExist() {
		List<Long> entityIds = Arrays.asList(111L);
		// call under test
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays.asList(new UserEntityPermissionsState(111L).withtDoesEntityExist(false)
				.withBenefactorId(null).withEntityType(null).withHasChangePermissions(false)
				.withHasChangeSettings(false).withHasCreate(false).withHasDelete(false).withHasDownload(false)
				.withHasRead(false).withHasModerate(false).withDataType(DataType.SENSITIVE_DATA));
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
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays.asList(new UserEntityPermissionsState(fileId).withBenefactorId(fileId)
				.withEntityType(EntityType.file).withEntityCreatedBy(userOneId).withEntityParentId(folderId).withHasRead(true).withtDoesEntityExist(true));
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetEntityPermissionsWithNullParentId() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(projectId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.READ));
		});
		// call under test
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays.asList(new UserEntityPermissionsState(projectId)
				.withBenefactorId(projectId).withEntityType(EntityType.project).withEntityCreatedBy(userOneId)
				.withEntityParentId(null).withHasRead(true).withtDoesEntityExist(true));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithAclWithSameIdButDifferntType() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(file.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.READ));
		}, ObjectType.TEAM);
		// call under test
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		// without an acl the entity does not exist
		List<UserEntityPermissionsState> expected = Arrays
				.asList(new UserEntityPermissionsState(fileId).withtDoesEntityExist(false));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithAclOnFolder() {
		setupNodeHierarchy(userOneId);
		// add a second file to the folder
		Node fileTwo = nodeDaoHelper.create(n -> {
			n.setName("secondFile");
			n.setCreatedByPrincipalId(userOneId);
			n.setParentId(folder.getId());
			n.setNodeType(EntityType.file);
		});
		Long fileTwoId = KeyFactory.stringToKey(fileTwo.getId());
		List<Long> entityIds = Arrays.asList(fileId, fileTwoId);
		aclHelper.create((a) -> {
			a.setId(folder.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.READ));
		});
		// call under test
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays.asList(
				createExpectedState(file).withBenefactorId(folderId).withEntityType(EntityType.file)
						.withHasRead(true).withtDoesEntityExist(true),
						createExpectedState(fileTwo).withBenefactorId(folderId).withEntityType(EntityType.file)
						.withHasRead(true).withtDoesEntityExist(true));
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
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays
				.asList(createExpectedState(file).withBenefactorId(projectId).withEntityType(EntityType.file)
						.withHasRead(true).withtDoesEntityExist(true));
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
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays
				.asList(createExpectedState(file).withBenefactorId(fileId).withEntityType(EntityType.file)
						.withHasRead(true).withtDoesEntityExist(true).withtDoesEntityExist(true));
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
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays.asList(createExpectedState(file).withBenefactorId(fileId)
				.withEntityType(EntityType.file).withHasRead(false).withtDoesEntityExist(true));
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
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays
				.asList(createExpectedState(file).withBenefactorId(projectId).withEntityType(EntityType.file)
						.withHasRead(true).withtDoesEntityExist(true));
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
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays
				.asList(createExpectedState(file).withBenefactorId(projectId).withEntityType(EntityType.file)
						.withHasDownload(true).withHasRead(false).withtDoesEntityExist(true));
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
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays
				.asList(createExpectedState(file).withBenefactorId(projectId).withEntityType(EntityType.file)
						.withHasDownload(true).withHasRead(true).withtDoesEntityExist(true));
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
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays.asList(
				createExpectedState(file).withBenefactorId(fileId).withEntityType(EntityType.file)
						.withHasDownload(false).withHasRead(true).withtDoesEntityExist(true),
				createExpectedState(project).withBenefactorId(projectId).withEntityType(EntityType.project)
						.withHasDownload(true).withHasRead(false).withtDoesEntityExist(true),
				createExpectedState(folder).withBenefactorId(projectId).withEntityType(EntityType.folder)
						.withHasDownload(true).withHasRead(false).withtDoesEntityExist(true));
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetEntityPermissionsWithPublicRead() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(file.getId());
			a.getResourceAccess().add(createResourceAccess(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(), ACCESS_TYPE.READ));
		});
		// call under test
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays.asList(
				createExpectedState(file).withBenefactorId(fileId).withEntityType(EntityType.file)
						.withHasRead(true).withHasPublicRead(true));
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetEntityPermissionsWithMultipleFilesWithPublicRead() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId, projectId, folderId);
		aclHelper.create((a) -> {
			a.setId(file.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.READ));
			a.getResourceAccess().add(createResourceAccess(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(), ACCESS_TYPE.READ));
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.DOWNLOAD));
		});
		// call under test
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays.asList(
				createExpectedState(file).withBenefactorId(fileId).withEntityType(EntityType.file)
						.withHasRead(true).withHasPublicRead(true),
				createExpectedState(project).withBenefactorId(projectId).withEntityType(EntityType.project)
						.withHasDownload(true).withHasRead(false).withHasPublicRead(false),
				createExpectedState(folder).withBenefactorId(projectId).withEntityType(EntityType.folder)
						.withHasDownload(true).withHasRead(false).withHasPublicRead(false));
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
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays.asList(createExpectedState(file)
				.withBenefactorId(projectId).withEntityType(EntityType.file).withHasDownload(false).withHasRead(true)
				.withDataType(DataType.OPEN_DATA).withtDoesEntityExist(true));
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
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays.asList(createExpectedState(file)
				.withBenefactorId(projectId).withEntityType(EntityType.file).withHasDownload(false).withHasRead(true)
				.withDataType(DataType.SENSITIVE_DATA).withtDoesEntityExist(true));
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
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays.asList(createExpectedState(file)
				.withBenefactorId(projectId).withEntityType(EntityType.file).withHasDownload(false).withHasRead(true)
				.withDataType(DataType.SENSITIVE_DATA).withtDoesEntityExist(true));
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
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays
				.asList(createExpectedState(file).withBenefactorId(projectId).withEntityType(EntityType.file)
						.withHasChangePermissions(true).withHasChangeSettings(false).withHasCreate(false)
						.withHasDelete(false).withHasDownload(false).withHasRead(false).withHasModerate(false)
						.withDataType(DataType.SENSITIVE_DATA).withtDoesEntityExist(true));
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
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays
				.asList(createExpectedState(file).withBenefactorId(projectId).withEntityType(EntityType.file)
						.withHasChangePermissions(false).withHasChangeSettings(true).withHasCreate(false)
						.withHasDelete(false).withHasDownload(false).withHasRead(false).withHasModerate(false)
						.withDataType(DataType.SENSITIVE_DATA).withtDoesEntityExist(true));
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
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays.asList(
				createExpectedState(file).withBenefactorId(projectId).withEntityType(EntityType.file)
						.withHasChangePermissions(false).withHasChangeSettings(false).withHasCreate(true)
						.withHasUpdate(false).withHasDelete(false).withHasDownload(false).withHasRead(false)
						.withHasModerate(false).withDataType(DataType.SENSITIVE_DATA).withtDoesEntityExist(true));
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetEntityPermissionsWithUpdate() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOneId, ACCESS_TYPE.UPDATE));
		});
		// call under test
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays.asList(
				createExpectedState(file).withBenefactorId(projectId).withEntityType(EntityType.file)
						.withHasChangePermissions(false).withHasChangeSettings(false).withHasCreate(false)
						.withHasUpdate(true).withHasDelete(false).withHasDownload(false).withHasRead(false)
						.withHasModerate(false).withDataType(DataType.SENSITIVE_DATA).withtDoesEntityExist(true));
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
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays
				.asList(createExpectedState(file).withBenefactorId(projectId).withEntityType(EntityType.file)
						.withHasChangePermissions(false).withHasChangeSettings(false).withHasCreate(false)
						.withHasDelete(true).withHasDownload(false).withHasRead(false).withHasModerate(false)
						.withDataType(DataType.SENSITIVE_DATA).withtDoesEntityExist(true));
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
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays
				.asList(createExpectedState(file).withBenefactorId(projectId).withEntityType(EntityType.file)
						.withHasChangePermissions(false).withHasChangeSettings(false).withHasCreate(false)
						.withHasDelete(false).withHasDownload(true).withHasRead(false).withHasModerate(false)
						.withDataType(DataType.SENSITIVE_DATA).withtDoesEntityExist(true));
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
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays
				.asList(createExpectedState(file).withBenefactorId(projectId).withEntityType(EntityType.file)
						.withHasChangePermissions(false).withHasChangeSettings(false).withHasCreate(false)
						.withHasDelete(false).withHasDownload(false).withHasRead(true).withHasModerate(false)
						.withDataType(DataType.SENSITIVE_DATA).withtDoesEntityExist(true));
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
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays
				.asList(createExpectedState(file).withBenefactorId(projectId).withEntityType(EntityType.file)
						.withHasChangePermissions(false).withHasChangeSettings(false).withHasCreate(false)
						.withHasDelete(false).withHasDownload(false).withHasRead(false).withHasModerate(true)
						.withDataType(DataType.SENSITIVE_DATA).withtDoesEntityExist(true));
		assertEquals(expected, results);
	}

	@Test
	public void testGetEntityPermissionsWithUnusedPermissionType() {
		setupNodeHierarchy(userOneId);
		List<Long> entityIds = Arrays.asList(fileId);
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess()
					.add(createResourceAccess(userOneId, ACCESS_TYPE.MODERATE, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE));
		});
		// call under test
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays
				.asList(createExpectedState(file).withBenefactorId(projectId)
						.withHasChangePermissions(false).withHasChangeSettings(false).withHasCreate(false)
						.withHasDelete(false).withHasDownload(false).withHasRead(false).withHasModerate(true)
						.withDataType(DataType.SENSITIVE_DATA).withtDoesEntityExist(true));
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
							ACCESS_TYPE.CREATE, ACCESS_TYPE.UPDATE, ACCESS_TYPE.DELETE, ACCESS_TYPE.DOWNLOAD,
							ACCESS_TYPE.READ, ACCESS_TYPE.MODERATE));
		});
		// call under test
		List<UserEntityPermissionsState> results = entityPermissionDao.getEntityPermissions(userOneGroups, entityIds);
		List<UserEntityPermissionsState> expected = Arrays.asList(
				createExpectedState(file).withBenefactorId(projectId).withEntityType(EntityType.file)
						.withHasChangePermissions(true).withHasChangeSettings(true).withHasCreate(true)
						.withHasUpdate(true).withHasDelete(true).withHasDownload(true).withHasRead(true)
						.withHasModerate(true).withDataType(DataType.SENSITIVE_DATA).withtDoesEntityExist(true));
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
	
	/**
	 * Helper to create an expected UserEntityPermissionsState from a given node.
	 * @param node
	 * @return
	 */
	UserEntityPermissionsState createExpectedState(Node node) {
		return new UserEntityPermissionsState(KeyFactory.stringToKey(node.getId())).withEntityType(node.getNodeType())
				.withEntityCreatedBy(node.getCreatedByPrincipalId())
				.withEntityParentId(node.getParentId() == null ? null : KeyFactory.stringToKey(node.getParentId()))
				.withtDoesEntityExist(true);
	}

}
