package org.sagebionetworks.repo.manager.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_CERTIFIED_USER_CONTENT;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_ENTITY_IN_TRASH_TEMPLATE;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_THERE_ARE_UNMET_ACCESS_REQUIREMENTS;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_YOU_HAVE_NOT_YET_AGREED_TO_THE_SYNAPSE_TERMS_OF_USE;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_YOU_LACK_ACCESS_TO_REQUESTED_ENTITY_TEMPLATE;
import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.UserCertificationRequiredException;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.dao.DataTypeDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.helper.DoaObjectHelper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * This was implemented as an integration test so we could ensure both the old
 * and new code had the same behavior for all cases.
 *
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntityAuthorizationManagerAutowireTest {

	@Autowired
	private UserManager userManager;

	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private AccessControlListDAO aclDao;
	@Autowired
	private DataTypeDao dataTypeDao;
	@Autowired
	private AccessApprovalDAO accessApprovalDAO;
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;
	@Autowired
	private DoaObjectHelper<Node> nodeDaoHelper;
	@Autowired
	private AccessControlListObjectHelper aclHelper;
	@Autowired
	private DoaObjectHelper<ManagedACTAccessRequirement> managedHelper;
	@Autowired
	private DoaObjectHelper<AccessApproval> accessApprovalHelper;
	@Autowired
	private EntityAuthorizationManager entityAuthManager;
	@Autowired
	private EntityPermissionsManager entityPermissionManager;

	private UserInfo adminUserInfo;
	private UserInfo anonymousUser;
	private UserInfo userOne;
	private UserInfo userTwo;

	@BeforeEach
	public void before() {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		anonymousUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());

		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setAgreesToTermsOfUse(Boolean.TRUE);

		DBOCredential cred = new DBOCredential();
		cred.setSecretKey("");

		NewUser nu = new NewUser();
		nu.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu.setUserName(UUID.randomUUID().toString());
		userOne = userManager.createOrGetTestUser(adminUserInfo, nu, cred, tou);

		nu = new NewUser();
		nu.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu.setUserName(UUID.randomUUID().toString());
		userTwo = userManager.createOrGetTestUser(adminUserInfo, nu, cred, tou);
	}

	@AfterEach
	public void after() {
		accessApprovalDAO.clear();
		accessRequirementDAO.clear();
		aclDao.truncateAll();
		dataTypeDao.truncateAllData();
		nodeDao.truncateAll();
		if (userOne != null) {
			userManager.deletePrincipal(adminUserInfo, userOne.getId());
		}
		if (userTwo != null) {
			userManager.deletePrincipal(adminUserInfo, userTwo.getId());
		}
	}

	@Test
	public void testHasAccessWithEntityDoesNotExist() {
		String entityId = "syn123";
		UserInfo user = userOne;
		ACCESS_TYPE accessType = ACCESS_TYPE.DOWNLOAD;
		// old call under test
		String oldMessage = assertThrows(NotFoundException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(NotFoundException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND, newMessage);
	}

	@Test
	public void testHasAccessDownloadWithAccess() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.DOWNLOAD));
		});

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.DOWNLOAD;
		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}

	@Test
	public void testHasAccessDownloadWithoutAccess() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.READ));
		});

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.DOWNLOAD;
		String message = assertThrows(UnauthorizedException.class, () -> {
			// old call under test
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(String.format(ERR_MSG_YOU_LACK_ACCESS_TO_REQUESTED_ENTITY_TEMPLATE, ACCESS_TYPE.DOWNLOAD.name()),
				message);

		message = assertThrows(UnauthorizedException.class, () -> {
			// new call under test
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(String.format(ERR_MSG_YOU_LACK_ACCESS_TO_REQUESTED_ENTITY_TEMPLATE, ACCESS_TYPE.DOWNLOAD.name()),
				message);
	}

	@Test
	public void testHasAccessDownloadWithInTrash() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId("" + NodeConstants.BOOTSTRAP_NODES.TRASH.getId());
		});

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.DOWNLOAD;
		// old call under test
		String oldMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(String.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE, project.getId()), newMessage);
	}
	
	/**
	 * Even an Admin should see the entity in the trash.
	 */
	@Test
	public void testHasAccessDownloadWithAccessAsAdminInTrash() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId("" + NodeConstants.BOOTSTRAP_NODES.TRASH.getId());
		});
		String entityId = project.getId();
		UserInfo user = adminUserInfo;
		ACCESS_TYPE accessType = ACCESS_TYPE.DOWNLOAD;

		// old call under test
		String oldMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(String.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE, project.getId()), newMessage);
	}

	@Test
	public void testHasAccessDownloadWithAccessAsAdmin() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.DOWNLOAD));
		});

		String entityId = project.getId();
		UserInfo user = adminUserInfo;
		ACCESS_TYPE accessType = ACCESS_TYPE.DOWNLOAD;
		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}

	@Test
	public void testHasAccessWithDownloadAsAnonymous() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess()
					.add(createResourceAccess(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(), ACCESS_TYPE.DOWNLOAD));
		});

		String entityId = project.getId();
		UserInfo user = anonymousUser;
		ACCESS_TYPE accessType = ACCESS_TYPE.DOWNLOAD;

		/*
		 * Note: The old call assumes that granting download to public is blocked at the
		 * ACL level, and incorrectly allows download to anonymous.
		 */
		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(ERR_MSG_ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION, newMessage);
	}

	@Test
	public void testHasAccessWithDownloadAsAnonymousOpenData() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess()
					.add(createResourceAccess(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(), ACCESS_TYPE.READ));
		});
		dataTypeDao.changeDataType(adminUserInfo.getId(), project.getId(), ObjectType.ENTITY, DataType.OPEN_DATA);

		String entityId = project.getId();
		UserInfo user = anonymousUser;
		ACCESS_TYPE accessType = ACCESS_TYPE.DOWNLOAD;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}

	@Test
	public void testHasAccessDownloadWithHasNotAcceptedTermsOfUse() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.DOWNLOAD));
		});

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.DOWNLOAD;
		// change the user to not accept.
		userTwo.setAcceptsTermsOfUse(false);

		// old call under test
		String oldMessage = assertThrows(UnauthorizedException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_YOU_HAVE_NOT_YET_AGREED_TO_THE_SYNAPSE_TERMS_OF_USE, newMessage);
	}

	@Test
	public void testHasAccessDownloadWithMetAccessRestrictions() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.DOWNLOAD));
		});
		ManagedACTAccessRequirement managedRequirement = managedHelper.create(a -> {
			a.setCreatedBy(userOne.getId().toString());
			a.getSubjectIds().get(0).setId(project.getId());
		});
		accessApprovalHelper.create(a -> {
			a.setCreatedBy(userOne.getId().toString());
			a.setSubmitterId(userOne.getId().toString());
			a.setRequirementId(managedRequirement.getId());
			a.setRequirementVersion(managedRequirement.getVersionNumber());
			a.setState(ApprovalState.APPROVED);
			a.setAccessorId(userTwo.getId().toString());
		});

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.DOWNLOAD;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}

	@Test
	public void testHasAccessDownloadWithUnmetAccessRestrictions() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.DOWNLOAD));
		});
		managedHelper.create(a -> {
			a.setCreatedBy(userOne.getId().toString());
			a.getSubjectIds().get(0).setId(project.getId());
		});

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.DOWNLOAD;

		// old call under test
		String oldMessage = assertThrows(UnauthorizedException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_THERE_ARE_UNMET_ACCESS_REQUIREMENTS, newMessage);
	}

	@Test
	public void testHasAccessUpdate() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.UPDATE));
		});
		// enuser userTwo is certified
		userTwo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.UPDATE;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithUpdateDoesNotExist() {
		String entityId = "syn123";
		UserInfo user = userOne;
		ACCESS_TYPE accessType = ACCESS_TYPE.UPDATE;
		// old call under test
		String oldMessage = assertThrows(NotFoundException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(NotFoundException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND, newMessage);
	}
	
	@Test
	public void testHasAccessWithUpdateWihtoutPermission() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.READ));
		});

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.UPDATE;

		// old call under test
		String oldMessage = assertThrows(UnauthorizedException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// Note: The old message is not consistent with other calls
		assertEquals(String.format(ERR_MSG_YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE, accessType.name(), entityId), oldMessage);
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(String.format(ERR_MSG_YOU_LACK_ACCESS_TO_REQUESTED_ENTITY_TEMPLATE, ACCESS_TYPE.UPDATE.name()),
				newMessage);
	}
	
	@Test
	public void testHasAccessWithUpdateAdminWithoutPermission() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.READ));
		});

		String entityId = project.getId();
		UserInfo user = adminUserInfo;
		ACCESS_TYPE accessType = ACCESS_TYPE.UPDATE;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithUpdateInTrash() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId("" + NodeConstants.BOOTSTRAP_NODES.TRASH.getId());
		});

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.UPDATE;
		// old call under test
		String oldMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(String.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE, project.getId()), newMessage);
	}
	
	@Test
	public void testHasAccessWithUpdateAsAdminInTrash() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId("" + NodeConstants.BOOTSTRAP_NODES.TRASH.getId());
		});
		String entityId = project.getId();
		UserInfo user = adminUserInfo;
		ACCESS_TYPE accessType = ACCESS_TYPE.UPDATE;

		// old call under test
		String oldMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(String.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE, project.getId()), newMessage);
	}
	
	@Test
	public void testHasAccessWithUpdateProjectWithCertifiedFalse() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.UPDATE));
		});
		userTwo.getGroups().remove(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.UPDATE;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithUpdateProjectWithCertifiedTrue() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.UPDATE));
		});
		userTwo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.UPDATE;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithUpdateFolderWithCertifiedFalse() {
		Node folder = nodeDaoHelper.create(n -> {
			n.setName("afolder");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setNodeType(EntityType.folder);
		});
		aclHelper.create((a) -> {
			a.setId(folder.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.UPDATE));
		});
		userTwo.getGroups().remove(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		String entityId = folder.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.UPDATE;

		// old call under test
		String oldMessage = assertThrows(UnauthorizedException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_CERTIFIED_USER_CONTENT, newMessage);
	}
	
	@Test
	public void testHasAccessWithUpdateFolderWithCertifiedTrue() {
		Node folder = nodeDaoHelper.create(n -> {
			n.setName("afolder");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setNodeType(EntityType.folder);
		});
		aclHelper.create((a) -> {
			a.setId(folder.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.UPDATE));
		});
		userTwo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		String entityId = folder.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.UPDATE;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithUpdateAsAnonymous() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess()
					.add(createResourceAccess(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(), ACCESS_TYPE.UPDATE));
		});

		String entityId = project.getId();
		UserInfo user = anonymousUser;
		ACCESS_TYPE accessType = ACCESS_TYPE.UPDATE;

		// old call under test
		String oldMessage = assertThrows(UnauthorizedException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION, newMessage);
	}
	
	@Test
	public void testHasAccessWithUpdateWithoutTermsOfUse() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess()
					.add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.UPDATE));
		});
		userTwo.setAcceptsTermsOfUse(false);
		userTwo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		
		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.UPDATE;

		// old call under test
		// The old code works but should not
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(ERR_MSG_YOU_HAVE_NOT_YET_AGREED_TO_THE_SYNAPSE_TERMS_OF_USE, newMessage);
	}
	
	@Test
	public void testCanCreate() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});
		String parentId = project.getId();
		EntityType createType = EntityType.project;
		UserInfo user = userTwo;
		
		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.canCreate(parentId, createType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.canCreate(parentId, createType, user);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testCanCreateWithProjectCertifiedFalse() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});
		
		userTwo.getGroups().remove(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		
		String parentId = project.getId();
		EntityType createType = EntityType.project;
		UserInfo user = userTwo;
		
		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.canCreate(parentId, createType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.canCreate(parentId, createType, user);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testCanCreateWithFileCertifiedFalse() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});
		
		userTwo.getGroups().remove(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		
		String parentId = project.getId();
		EntityType createType = EntityType.file;
		UserInfo user = userTwo;
				
		// old call under test
		// This is another cases where the old code was not consistent with the exception type.
		String oldMessage = assertThrows(UserCertificationRequiredException.class, () -> {
			entityPermissionManager.canCreate(parentId, createType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.canCreate(parentId, createType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_CERTIFIED_USER_CONTENT, newMessage);
	}
	
	@Test
	public void testCanCreateWithFileCertifiedTrue() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});
		
		userTwo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		
		String parentId = project.getId();
		EntityType createType = EntityType.file;
		UserInfo user = userTwo;
		
		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.canCreate(parentId, createType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.canCreate(parentId, createType, user);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessCreate() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});
		userTwo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CREATE;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithCreateDoesNotExist() {
		String entityId = "syn123";
		UserInfo user = userOne;
		ACCESS_TYPE accessType = ACCESS_TYPE.CREATE;
		// old call under test
		String oldMessage = assertThrows(NotFoundException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(NotFoundException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND, newMessage);
	}
	
	@Test
	public void testHasAccessWithCreateWihtoutPermission() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.READ));
		});
		userTwo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CREATE;

		// old call under test
		String oldMessage = assertThrows(UnauthorizedException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// Note: The old message is not consistent with other calls
		assertEquals(String.format(ERR_MSG_YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE, accessType.name(), entityId),
				oldMessage);
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(String.format(ERR_MSG_YOU_LACK_ACCESS_TO_REQUESTED_ENTITY_TEMPLATE, accessType.name()),
				newMessage);
	}
	
	@Test
	public void testHasAccessWithCreateAdminWithoutPermission() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.READ));
		});
		userTwo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		String entityId = project.getId();
		UserInfo user = adminUserInfo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CREATE;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithCreateAsAdminInTrash() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId("" + NodeConstants.BOOTSTRAP_NODES.TRASH.getId());
		});
		String entityId = project.getId();
		UserInfo user = adminUserInfo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CREATE;

		// old call under test
		// The old code erroneously allows admins users to create entities in the trash.
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(String.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE, project.getId()), newMessage);
	}
	
	@Test
	public void testHasAccessWithCreateInTrash() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId("" + NodeConstants.BOOTSTRAP_NODES.TRASH.getId());
		});
		userTwo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CREATE;
		// old call under test
		// The old code erroneously allows users to create entities in the trash.
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(String.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE, project.getId()), newMessage);
	}
	
	/**
	 * Note: For hasAccess() the creatEntityType is always null so a user must
	 * always be certified for a create called through this path.
	 */
	@Test
	public void testHasAccessWithCreateProjectWithCertifiedFalse() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});
		userTwo.getGroups().remove(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CREATE;

		// old call under test
		String oldMessage = assertThrows(UnauthorizedException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_CERTIFIED_USER_CONTENT, newMessage);
	}
	
	@Test
	public void testHasAccessWithCreateProjectWithCertifiedTrue() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});
		userTwo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CREATE;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithCreateFolderWithCertifiedFalse() {
		Node folder = nodeDaoHelper.create(n -> {
			n.setName("afolder");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setNodeType(EntityType.folder);
		});
		aclHelper.create((a) -> {
			a.setId(folder.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});
		userTwo.getGroups().remove(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		String entityId = folder.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CREATE;

		// old call under test
		String oldMessage = assertThrows(UnauthorizedException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_CERTIFIED_USER_CONTENT, newMessage);
	}
	
	@Test
	public void testHasAccessWithCreateFolderWithCertifiedTrue() {
		Node folder = nodeDaoHelper.create(n -> {
			n.setName("afolder");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setNodeType(EntityType.folder);
		});
		aclHelper.create((a) -> {
			a.setId(folder.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});
		userTwo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		String entityId = folder.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CREATE;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithCreateAsAnonymous() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess()
					.add(createResourceAccess(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(), ACCESS_TYPE.CREATE));
		});

		String entityId = project.getId();
		UserInfo user = anonymousUser;
		ACCESS_TYPE accessType = ACCESS_TYPE.CREATE;

		// old call under test
		String oldMessage = assertThrows(UnauthorizedException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// Old code failed for the wrong reason...
		assertEquals(ERR_MSG_CERTIFIED_USER_CONTENT, oldMessage);
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(ERR_MSG_ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION, newMessage);
	}
	
	@Test
	public void testHasAccessWithCreateWithoutTermsOfUse() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess()
					.add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});
		userTwo.setAcceptsTermsOfUse(false);
		userTwo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		
		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CREATE;

		// old call under test
		// The old code works but should not
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(ERR_MSG_YOU_HAVE_NOT_YET_AGREED_TO_THE_SYNAPSE_TERMS_OF_USE, newMessage);
	}
	
	@Test
	public void testHasAccessRead() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.READ));
		});

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.READ;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithReadDoesNotExist() {
		String entityId = "syn123";
		UserInfo user = userOne;
		ACCESS_TYPE accessType = ACCESS_TYPE.READ;
		// old call under test
		String oldMessage = assertThrows(NotFoundException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(NotFoundException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND, newMessage);
	}
	
	@Test
	public void testHasAccessWithReadWihtoutPermission() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.READ;

		// old call under test
		String oldMessage = assertThrows(UnauthorizedException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// Note: The old message is not consistent with other calls
		assertEquals(String.format(ERR_MSG_YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE, accessType.name(), entityId), oldMessage);
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(String.format(ERR_MSG_YOU_LACK_ACCESS_TO_REQUESTED_ENTITY_TEMPLATE, accessType.name()),
				newMessage);
	}
	
	@Test
	public void testHasAccessWithReadAdminWithoutPermission() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});

		String entityId = project.getId();
		UserInfo user = adminUserInfo;
		ACCESS_TYPE accessType = ACCESS_TYPE.READ;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithReadAsAdminInTrash() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId("" + NodeConstants.BOOTSTRAP_NODES.TRASH.getId());
		});
		String entityId = project.getId();
		UserInfo user = adminUserInfo;
		ACCESS_TYPE accessType = ACCESS_TYPE.READ;

		// old call under test
		String oldMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(String.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE, project.getId()), newMessage);
	}
	
	@Test
	public void testHasAccessWithReadInTrash() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId("" + NodeConstants.BOOTSTRAP_NODES.TRASH.getId());
		});

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.READ;
		// old call under test
		String oldMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(String.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE, project.getId()), newMessage);
	}
	
	@Test
	public void testHasAccessDelete() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.DELETE));
		});

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.DELETE;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithDeleteDoesNotExist() {
		String entityId = "syn123";
		UserInfo user = userOne;
		ACCESS_TYPE accessType = ACCESS_TYPE.DELETE;
		// old call under test
		String oldMessage = assertThrows(NotFoundException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(NotFoundException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND, newMessage);
	}
	
	@Test
	public void testHasAccessWithDeleteWihtoutPermission() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.DELETE;

		// old call under test
		String oldMessage = assertThrows(UnauthorizedException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// Note: The old message is not consistent with other calls
		assertEquals(String.format(ERR_MSG_YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE, accessType.name(), entityId), oldMessage);
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(String.format(ERR_MSG_YOU_LACK_ACCESS_TO_REQUESTED_ENTITY_TEMPLATE, accessType.name()),
				newMessage);
	}
	
	@Test
	public void testHasAccessWithDeleteAsAdminWithoutPermission() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});

		String entityId = project.getId();
		UserInfo user = adminUserInfo;
		ACCESS_TYPE accessType = ACCESS_TYPE.DELETE;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithDeleteAsAdminInTrash() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId("" + NodeConstants.BOOTSTRAP_NODES.TRASH.getId());
		});
		String entityId = project.getId();
		UserInfo user = adminUserInfo;
		ACCESS_TYPE accessType = ACCESS_TYPE.DELETE;

		// old call under test
		// The old method erroneously works.
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(String.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE, project.getId()), newMessage);
	}
	
	@Test
	public void testHasAccessWithDeletedInTrash() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId("" + NodeConstants.BOOTSTRAP_NODES.TRASH.getId());
		});
		
		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.DELETE;

		// old call under test
		String oldMessage = assertThrows(UnauthorizedException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// Note: The old message is not consistent with other calls
		assertEquals(String.format(ERR_MSG_YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE, accessType.name(), entityId), oldMessage);
		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(String.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE, project.getId()), newMessage);
	}
	
	@Test
	public void testHasAccessWithDeleteAsAnonymous() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess()
					.add(createResourceAccess(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(), ACCESS_TYPE.UPDATE));
		});

		String entityId = project.getId();
		UserInfo user = anonymousUser;
		ACCESS_TYPE accessType = ACCESS_TYPE.DELETE;

		// old call under test
		String oldMessage = assertThrows(UnauthorizedException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION, newMessage);
	}
	
	@Test
	public void testHasAccessWithModerate() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.MODERATE));
		});

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.MODERATE;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithModerateDoesNotExist() {
		String entityId = "syn123";
		UserInfo user = userOne;
		ACCESS_TYPE accessType = ACCESS_TYPE.MODERATE;
		// old call under test
		String oldMessage = assertThrows(NotFoundException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(NotFoundException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND, newMessage);
	}
	
	@Test
	public void testHasAccessWithModerateWihtoutPermission() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.MODERATE;

		// old call under test
		String oldMessage = assertThrows(UnauthorizedException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// Note: The old message is not consistent with other calls
		assertEquals(String.format(ERR_MSG_YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE, accessType.name(), entityId), oldMessage);
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(String.format(ERR_MSG_YOU_LACK_ACCESS_TO_REQUESTED_ENTITY_TEMPLATE, accessType.name()),
				newMessage);
	}
	
	@Test
	public void testHasAccessWithModerateAsAdminWithoutPermission() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});

		String entityId = project.getId();
		UserInfo user = adminUserInfo;
		ACCESS_TYPE accessType = ACCESS_TYPE.MODERATE;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithModerateAsAdminInTrash() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId("" + NodeConstants.BOOTSTRAP_NODES.TRASH.getId());
		});
		String entityId = project.getId();
		UserInfo user = adminUserInfo;
		ACCESS_TYPE accessType = ACCESS_TYPE.MODERATE;

		// old call under test
		String oldMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(String.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE, project.getId()), newMessage);
	}
	
	@Test
	public void testHasAccessWithModerateInTrash() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId("" + NodeConstants.BOOTSTRAP_NODES.TRASH.getId());
		});
		
		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.MODERATE;

		// old call under test
		String oldMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(String.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE, project.getId()), newMessage);
	}
	
	@Test
	public void testHasAccessWithModerateAnonymous() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess()
					.add(createResourceAccess(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(), ACCESS_TYPE.MODERATE));
		});

		String entityId = project.getId();
		UserInfo user = anonymousUser;
		ACCESS_TYPE accessType = ACCESS_TYPE.MODERATE;

		// old call under test
		String oldMessage = assertThrows(UnauthorizedException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION, newMessage);
	}
	
	@Test
	public void testHasAccessWithChangeSettings() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CHANGE_SETTINGS));
		});

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CHANGE_SETTINGS;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithChangeSettingsDoesNotExist() {
		String entityId = "syn123";
		UserInfo user = userOne;
		ACCESS_TYPE accessType = ACCESS_TYPE.CHANGE_SETTINGS;
		// old call under test
		String oldMessage = assertThrows(NotFoundException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(NotFoundException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND, newMessage);
	}
	
	@Test
	public void testHasAccessWithChangeSettingsWihtoutPermission() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CHANGE_SETTINGS;

		// old call under test
		String oldMessage = assertThrows(UnauthorizedException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// Note: The old message is not consistent with other calls
		assertEquals(String.format(ERR_MSG_YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE, accessType.name(), entityId), oldMessage);
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(String.format(ERR_MSG_YOU_LACK_ACCESS_TO_REQUESTED_ENTITY_TEMPLATE, accessType.name()),
				newMessage);
	}
	
	@Test
	public void testHasAccessWithChangeSettingsAsAdminWithoutPermission() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});

		String entityId = project.getId();
		UserInfo user = adminUserInfo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CHANGE_SETTINGS;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithChangeSettingsAsAdminInTrash() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId("" + NodeConstants.BOOTSTRAP_NODES.TRASH.getId());
		});
		String entityId = project.getId();
		UserInfo user = adminUserInfo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CHANGE_SETTINGS;

		// old call under test
		String oldMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(String.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE, project.getId()), newMessage);
	}
	
	@Test
	public void testHasAccessWithChangeSettingsInTrash() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId("" + NodeConstants.BOOTSTRAP_NODES.TRASH.getId());
		});
		
		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CHANGE_SETTINGS;

		// old call under test
		String oldMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(String.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE, project.getId()), newMessage);
	}
	
	@Test
	public void testHasAccessWithChangeSettingsAnonymous() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess()
					.add(createResourceAccess(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(), ACCESS_TYPE.CHANGE_SETTINGS));
		});

		String entityId = project.getId();
		UserInfo user = anonymousUser;
		ACCESS_TYPE accessType = ACCESS_TYPE.CHANGE_SETTINGS;

		// old call under test
		String oldMessage = assertThrows(UnauthorizedException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION, newMessage);
	}

	@Test
	public void testHasAccessWithChangePermissions() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CHANGE_PERMISSIONS));
		});

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CHANGE_PERMISSIONS;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithChangePermissionsDoesNotExist() {
		String entityId = "syn123";
		UserInfo user = userOne;
		ACCESS_TYPE accessType = ACCESS_TYPE.CHANGE_PERMISSIONS;
		// old call under test
		String oldMessage = assertThrows(NotFoundException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(NotFoundException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND, newMessage);
	}
	
	@Test
	public void testHasAccessWithChangePermissionsWihtoutPermission() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CHANGE_PERMISSIONS;

		// old call under test
		String oldMessage = assertThrows(UnauthorizedException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// Note: The old message is not consistent with other calls
		assertEquals(String.format(ERR_MSG_YOU_DO_NOT_HAVE_PERMISSION_TEMPLATE, accessType.name(), entityId), oldMessage);
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(String.format(ERR_MSG_YOU_LACK_ACCESS_TO_REQUESTED_ENTITY_TEMPLATE, accessType.name()),
				newMessage);
	}
	
	@Test
	public void testHasAccessWithChangePermissionsAsAdminWithoutPermission() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});

		String entityId = project.getId();
		UserInfo user = adminUserInfo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CHANGE_PERMISSIONS;

		// old call under test
		AuthorizationStatus oldStatus = entityPermissionManager.hasAccess(entityId, accessType, user);
		assertNotNull(oldStatus);
		assertTrue(oldStatus.isAuthorized());
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithChangePermissionsAsAdminInTrash() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId("" + NodeConstants.BOOTSTRAP_NODES.TRASH.getId());
		});
		String entityId = project.getId();
		UserInfo user = adminUserInfo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CHANGE_PERMISSIONS;

		// old call under test
		String oldMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(String.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE, project.getId()), newMessage);
	}
	
	@Test
	public void testHasAccessWithChangePermissionsInTrash() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId("" + NodeConstants.BOOTSTRAP_NODES.TRASH.getId());
		});
		
		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CHANGE_PERMISSIONS;

		// old call under test
		String oldMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user);
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(String.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE, project.getId()), newMessage);
	}
	
	@Test
	public void testHasAccessWithChangePermissionsAnonymous() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess()
					.add(createResourceAccess(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(), ACCESS_TYPE.CHANGE_PERMISSIONS));
		});

		String entityId = project.getId();
		UserInfo user = anonymousUser;
		ACCESS_TYPE accessType = ACCESS_TYPE.CHANGE_PERMISSIONS;

		// old call under test
		String oldMessage = assertThrows(UnauthorizedException.class, () -> {
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION, newMessage);
	}
}
