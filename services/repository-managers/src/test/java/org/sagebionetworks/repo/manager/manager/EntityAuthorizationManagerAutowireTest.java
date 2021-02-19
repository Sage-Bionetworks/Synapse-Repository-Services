package org.sagebionetworks.repo.manager.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_ENTITY_IN_TRASH_TEMPLATE;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_THERE_ARE_UNMET_ACCESS_REQUIREMENTS;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_THE_RESOURCE_YOU_ARE_ATTEMPTING_TO_ACCESS_CANNOT_BE_FOUND;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_YOU_HAVE_NOT_YET_AGREED_TO_THE_SYNAPSE_TERMS_OF_USE;
import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DataType;
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
		if(userOne != null) {
			userManager.deletePrincipal(adminUserInfo, userOne.getId());
		}
		if(userTwo != null) {
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
	public void testHasAccessDownloadWithInTrash() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId(""+NodeConstants.BOOTSTRAP_NODES.TRASH.getId());
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
			a.getResourceAccess().add(createResourceAccess(
					BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(), ACCESS_TYPE.DOWNLOAD));
		});
		
		String entityId = project.getId();
		UserInfo user = anonymousUser;
		ACCESS_TYPE accessType = ACCESS_TYPE.DOWNLOAD;
		
		/*
		 * Note: The old call assumes that granting download to public is blocked at the ACL level,
		 * and incorrectly allows download to anonymous.
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
			a.getResourceAccess().add(createResourceAccess(
					BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(), ACCESS_TYPE.READ));
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
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();;
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
			entityPermissionManager.hasAccess(entityId, accessType, user).checkAuthorizationOrElseThrow();;
		}).getMessage();
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(oldMessage, newMessage);
		assertEquals(ERR_MSG_THERE_ARE_UNMET_ACCESS_REQUIREMENTS, newMessage);
	}

}
