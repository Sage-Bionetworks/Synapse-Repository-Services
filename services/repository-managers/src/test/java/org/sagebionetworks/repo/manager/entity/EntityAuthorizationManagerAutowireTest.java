package org.sagebionetworks.repo.manager.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_CANNOT_REMOVE_ACL_OF_PROJECT;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_CERTIFIED_USER_CONTENT;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_ENTITY_IN_TRASH_TEMPLATE;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_THERE_ARE_UNMET_ACCESS_REQUIREMENTS;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_YOU_LACK_ACCESS_TO_REQUESTED_ENTITY_TEMPLATE;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_YOU_NEED_TWO_FA;
import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
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
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.dao.DataTypeDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
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
	private DaoObjectHelper<Node> nodeDaoHelper;
	@Autowired
	private AccessControlListObjectHelper aclHelper;
	@Autowired
	private DaoObjectHelper<ManagedACTAccessRequirement> managedHelper;
	@Autowired
	private DaoObjectHelper<AccessApproval> accessApprovalHelper;
	@Autowired
	private DaoObjectHelper<UserGroup> userGroupHelper;
	@Autowired
	private EntityAuthorizationManager entityAuthManager;
	@Autowired
	private AuthenticationDAO authDao;
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	private UserInfo adminUserInfo;
	private UserInfo anonymousUser;
	private UserInfo userOne;
	private UserInfo userTwo;
	private Long teamOneId;

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

		teamOneId = Long.parseLong(userGroupHelper.create(u -> {
			u.setIsIndividual(false);
		}).getId());
	}

	@AfterEach
	public void after() {
		accessApprovalDAO.clear();
		accessRequirementDAO.truncateAll();
		aclDao.truncateAll();
		dataTypeDao.truncateAllData();
		nodeDao.truncateAll();
		if (userOne != null) {
			userManager.deletePrincipal(adminUserInfo, userOne.getId());
		}
		if (userTwo != null) {
			userManager.deletePrincipal(adminUserInfo, userTwo.getId());
		}
		if (teamOneId != null) {
			userManager.deletePrincipal(adminUserInfo, teamOneId);
		}
	}

	@Test
	public void testHasAccessWithEntityDoesNotExist() {
		String entityId = "syn123";
		UserInfo user = userOne;
		List<ACCESS_TYPE> types = Arrays.asList(
				ACCESS_TYPE.CREATE,
				ACCESS_TYPE.UPDATE,
				ACCESS_TYPE.DELETE,
				ACCESS_TYPE.CHANGE_PERMISSIONS);
		for(ACCESS_TYPE accessType: types) {
			// new call under test
			String newMessage = assertThrows(NotFoundException.class, () -> {
				entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
			}).getMessage();
			assertEquals("Resource: 'syn123' does not exist", newMessage);
		}
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

		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
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

		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
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
		
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}

	@Test
	public void testHasAccessDownloadWithMetAccessRestrictionsAndWithoutExemptionEligible() {
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
		
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}

	@Test
	public void testHasAccessDownloadWithUnMetAccessRestrictionsAndWithExemptionEligible() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});

		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.setResourceAccess(Collections.singleton(
					new ResourceAccess().setPrincipalId(userTwo.getId())
							.setAccessType(Set.of(ACCESS_TYPE.UPDATE, ACCESS_TYPE.DELETE, ACCESS_TYPE.DOWNLOAD))));
		});

		Long arId = managedHelper.create(a -> {
			a.setAccessType(ACCESS_TYPE.DOWNLOAD);
			a.setSubjectIds(Collections.singletonList(
					new RestrictableObjectDescriptor().setId(project.getId())
							.setType(RestrictableObjectType.ENTITY)));
		}).getId();

		aclHelper.create((a) -> {
			a.setId(arId.toString());
			a.setResourceAccess(Set.of(
					new ResourceAccess().setPrincipalId(Long.valueOf(userTwo.getId()))
							.setAccessType(Collections.singleton(ACCESS_TYPE.EXEMPTION_ELIGIBLE))));
		} , ObjectType.ACCESS_REQUIREMENT);

		// call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(userTwo, project.getId(), ACCESS_TYPE.DOWNLOAD);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}

	@Test
	public void testHasAccessDownloadWithMetAccessRestrictionsAndWithExemptionEligible() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userTwo.getId());
		});

		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.setResourceAccess(Collections.singleton(
					new ResourceAccess().setPrincipalId(userTwo.getId())
							.setAccessType(Set.of(ACCESS_TYPE.UPDATE, ACCESS_TYPE.DELETE, ACCESS_TYPE.DOWNLOAD))));
		});

		Long arId = managedHelper.create(a -> {
			a.setAccessType(ACCESS_TYPE.DOWNLOAD);
			a.setSubjectIds(Collections.singletonList(
					new RestrictableObjectDescriptor().setId(project.getId())
							.setType(RestrictableObjectType.ENTITY)));
		}).getId();

		aclHelper.create((a) -> {
			a.setId(arId.toString());
			a.setResourceAccess(Set.of(
					new ResourceAccess().setPrincipalId(Long.valueOf(userTwo.getId()))
							.setAccessType(Collections.singleton(ACCESS_TYPE.EXEMPTION_ELIGIBLE))));
		} , ObjectType.ACCESS_REQUIREMENT);

		// call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(userTwo, project.getId(), ACCESS_TYPE.DOWNLOAD);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}

	@Test
	public void testHasAccessDownloadWithUnmetAccessRestrictionsAndWithoutExemptionEligible() {
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

		// call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(userTwo, project.getId(), ACCESS_TYPE.DOWNLOAD).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(ERR_MSG_THERE_ARE_UNMET_ACCESS_REQUIREMENTS, newMessage);
	}

	/**
	 * A Data Contributor’s AR “exemption” must be limited to data within the scope of their contribution.
	 */
	@Test
	public void testHasAccessDownloadWithExemptionEligibleOnSomeFilesOfOneProject() {
		Node projectOne = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(adminUserInfo.getId());
		});

		Node fileOne = nodeDaoHelper.create(n -> {
			n.setName("aFile");
			n.setCreatedByPrincipalId(adminUserInfo.getId());
			n.setParentId(projectOne.getId());
			n.setNodeType(EntityType.file);
		});

		Node fileTwo = nodeDaoHelper.create(n -> {
			n.setName("aFileTwo");
			n.setCreatedByPrincipalId(adminUserInfo.getId());
			n.setParentId(projectOne.getId());
			n.setNodeType(EntityType.file);
		});

		Node projectTwo = nodeDaoHelper.create(n -> {
			n.setName("anotherProject");
			n.setCreatedByPrincipalId(adminUserInfo.getId());
		});

		Node fileThree = nodeDaoHelper.create(n -> {
			n.setName("aFileThree");
			n.setCreatedByPrincipalId(adminUserInfo.getId());
			n.setParentId(projectTwo.getId());
		});

		// userTwo is contributor for fileOne and has download access
		aclHelper.create((a) -> {
			a.setId(fileOne.getId());
			a.setResourceAccess(Collections.singleton(
					new ResourceAccess().setPrincipalId(userTwo.getId())
							.setAccessType(Set.of(ACCESS_TYPE.UPDATE, ACCESS_TYPE.DELETE, ACCESS_TYPE.DOWNLOAD))));
		});

		// userTwo is contributor for fileTwo with no download access
		aclHelper.create((a) -> {
			a.setId(fileTwo.getId());
			a.setResourceAccess(Collections.singleton(
					new ResourceAccess().setPrincipalId(userTwo.getId())
							.setAccessType(Set.of(ACCESS_TYPE.UPDATE, ACCESS_TYPE.DELETE))));
		});

		// userTwo is contributor for fileThree with no download access
		aclHelper.create((a) -> {
			a.setId(fileThree.getId());
			a.setResourceAccess(Collections.singleton(
					new ResourceAccess().setPrincipalId(userTwo.getId())
							.setAccessType(Set.of(ACCESS_TYPE.UPDATE, ACCESS_TYPE.DELETE))));
		});

		//AR on fileOne and fileTwo
		Long arId = managedHelper.create(a -> {
			a.setAccessType(ACCESS_TYPE.DOWNLOAD);
			a.setSubjectIds(List.of(
					new RestrictableObjectDescriptor()
							.setId(fileOne.getId())
							.setType(RestrictableObjectType.ENTITY),
					new RestrictableObjectDescriptor()
							.setId(fileTwo.getId())
							.setType(RestrictableObjectType.ENTITY)));
		}).getId();

		//Exemption ACL on arId
		aclHelper.create((a) -> {
			a.setId(arId.toString());
			a.setResourceAccess(Set.of(
					new ResourceAccess()
							.setPrincipalId(Long.valueOf(userTwo.getId()))
							.setAccessType(Collections.singleton(ACCESS_TYPE.EXEMPTION_ELIGIBLE))));
		} , ObjectType.ACCESS_REQUIREMENT);

		// There is no AR on file three and also no download access
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(userTwo, fileThree.getId(), ACCESS_TYPE.DOWNLOAD).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals("You lack DOWNLOAD access to the requested entity.", newMessage);

		// userTwo is exempted for file Two but not met access requirement and has no download access.
		String newMessageTwo = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(userTwo, fileTwo.getId(), ACCESS_TYPE.DOWNLOAD).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals("You lack DOWNLOAD access to the requested entity.", newMessageTwo);

		//userTwo is contributor for file One and exempted
		AuthorizationStatus status = entityAuthManager.hasAccess(userTwo, fileOne.getId(), ACCESS_TYPE.DOWNLOAD);
		assertNotNull(status);
		assertTrue(status.isAuthorized());

		//userOne is neither exempted nor met access approval
		String message = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(userOne, fileOne.getId(), ACCESS_TYPE.DOWNLOAD).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(ERR_MSG_THERE_ARE_UNMET_ACCESS_REQUIREMENTS, message);
	}

	@Test
	public void testHasAccessDownloadWhenASingleARIsBoundToMultipleEntities() {
		groupMembersDAO.addMembers(teamOneId.toString(),List.of(userTwo.getId().toString()));

		userTwo = userManager.getUserInfo(userTwo.getId());

		Node projectOne = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});

		Node fileOne = nodeDaoHelper.create(n -> {
			n.setName("fileOne");
			n.setParentId(projectOne.getId());
			n.setCreatedByPrincipalId(userOne.getId());
			n.setNodeType(EntityType.file);
		});

		Node fileTwo = nodeDaoHelper.create(n -> {
			n.setName("fileTwo");
			n.setParentId(projectOne.getId());
			n.setCreatedByPrincipalId(userOne.getId());
			n.setNodeType(EntityType.file);
		});

		//teamOneId is contributor for fileOne
		aclHelper.create((a) -> {
			a.setId(fileOne.getId());
			a.setResourceAccess(Collections.singleton(
					new ResourceAccess().setPrincipalId(teamOneId)
							.setAccessType(Set.of(ACCESS_TYPE.UPDATE, ACCESS_TYPE.DELETE, ACCESS_TYPE.DOWNLOAD))));
		});

		// userOne is contributor for fileTwo
		aclHelper.create((a) -> {
			a.setId(fileTwo.getId());
			a.setResourceAccess(Collections.singleton(
					new ResourceAccess().setPrincipalId(userOne.getId())
							.setAccessType(Set.of(ACCESS_TYPE.UPDATE, ACCESS_TYPE.DELETE, ACCESS_TYPE.DOWNLOAD))));
		});

		//AR for file one and two
		Long arId = managedHelper.create(a -> {
			a.setAccessType(ACCESS_TYPE.DOWNLOAD);
			a.setSubjectIds(List.of(
					new RestrictableObjectDescriptor()
							.setId(fileOne.getId())
							.setType(RestrictableObjectType.ENTITY),
					new RestrictableObjectDescriptor()
							.setId(fileTwo.getId())
							.setType(RestrictableObjectType.ENTITY)));
		}).getId();

		//Exemption ACL on AR
		aclHelper.create((a) -> {
			a.setId(arId.toString());
			a.setResourceAccess(Set.of(
					new ResourceAccess()
							.setPrincipalId(Long.valueOf(teamOneId))
							.setAccessType(Collections.singleton(ACCESS_TYPE.EXEMPTION_ELIGIBLE)),
					new ResourceAccess()
							.setPrincipalId(Long.valueOf(userOne.getId()))
							.setAccessType(Collections.singleton(ACCESS_TYPE.EXEMPTION_ELIGIBLE))));
		} , ObjectType.ACCESS_REQUIREMENT);

		//userTwo is member teamOne. teamOne is eligible for exemption but not exempted as it is neither contributor nor met access
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(userTwo, fileTwo.getId(), ACCESS_TYPE.DOWNLOAD).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(ERR_MSG_THERE_ARE_UNMET_ACCESS_REQUIREMENTS, newMessage);

		//userTwo is member teamOne. TeamOne is contributor for fileOne and eligible for exemption
		AuthorizationStatus statusOne = entityAuthManager.hasAccess(userTwo, fileOne.getId(), ACCESS_TYPE.DOWNLOAD);
		assertNotNull(statusOne);
		assertTrue(statusOne.isAuthorized());

		//userOne is creator of fileTwo hence approved and has download access
		AuthorizationStatus statusTwo =entityAuthManager.hasAccess(userOne, fileTwo.getId(), ACCESS_TYPE.DOWNLOAD);
		assertNotNull(statusTwo);
		assertTrue(statusTwo.isAuthorized());

		//userOne creator of fileOne hence approved but has no download access
		String message = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(userOne, fileOne.getId(), ACCESS_TYPE.DOWNLOAD).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals("You lack DOWNLOAD access to the requested entity.", message);
	}

	@Test
	public void testHasAccessDownloadWithMultipleAROnEntityWithAndWithoutExemption() {
		Node projectOne = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		Node fileOne = nodeDaoHelper.create(n -> {
			n.setName("aFile");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId(projectOne.getId());
			n.setNodeType(EntityType.file);
		});

		//userTwo is contributor for fileOne
		aclHelper.create((a) -> {
			a.setId(fileOne.getId());
			a.setResourceAccess(Collections.singleton(
					new ResourceAccess().setPrincipalId(userTwo.getId())
							.setAccessType(Set.of(ACCESS_TYPE.UPDATE, ACCESS_TYPE.DELETE, ACCESS_TYPE.DOWNLOAD))));
		});


		//AR on fileOne
		Long arId = managedHelper.create(a -> {
			a.setAccessType(ACCESS_TYPE.DOWNLOAD);
			a.setSubjectIds(List.of(
					new RestrictableObjectDescriptor()
							.setId(fileOne.getId())
							.setType(RestrictableObjectType.ENTITY)));
		}).getId();
		;
		//AR Two on fileOne
		Long arIdTwo = managedHelper.create(a -> {
			a.setAccessType(ACCESS_TYPE.DOWNLOAD);
			a.setSubjectIds(List.of(
					new RestrictableObjectDescriptor()
							.setId(fileOne.getId())
							.setType(RestrictableObjectType.ENTITY)));
		}).getId();

		//AR three on fileOne
		Long arIdThree = managedHelper.create(a -> {
			a.setAccessType(ACCESS_TYPE.DOWNLOAD);
			a.setSubjectIds(List.of(
					new RestrictableObjectDescriptor()
							.setId(fileOne.getId())
							.setType(RestrictableObjectType.ENTITY)));
		}).getId();

		//Exemption ACL on AR One
		aclHelper.create((a) -> {
			a.setId(arId.toString());
			a.setResourceAccess(Set.of(
					new ResourceAccess()
							.setPrincipalId(Long.valueOf(userTwo.getId()))
							.setAccessType(Collections.singleton(ACCESS_TYPE.EXEMPTION_ELIGIBLE))));
		} , ObjectType.ACCESS_REQUIREMENT);

		aclHelper.create((a) -> {
			a.setId(arIdTwo.toString());
			a.setResourceAccess(Set.of(
					new ResourceAccess()
							.setPrincipalId(Long.valueOf(userTwo.getId()))
							.setAccessType(Collections.singleton(ACCESS_TYPE.REVIEW_SUBMISSIONS))));
		} , ObjectType.ACCESS_REQUIREMENT);

		aclHelper.create((a) -> {
			a.setId(arIdThree.toString());
			a.setResourceAccess(Set.of(
					new ResourceAccess()
							.setPrincipalId(Long.valueOf(userTwo.getId()))
							.setAccessType(Collections.singleton(ACCESS_TYPE.DOWNLOAD))));
		} , ObjectType.ENTITY);

		//Multiple AR, one is eligible and the other is not on fileOne
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(userTwo, fileOne.getId(), ACCESS_TYPE.DOWNLOAD).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals("There are unmet access requirements that must be met to read content in the requested container.", newMessage);

		//userOne is creator of fileOne so user met access requirement without exemption and has no download access.
		String newMessageTwo = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(userOne, fileOne.getId(), ACCESS_TYPE.DOWNLOAD).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals("You lack DOWNLOAD access to the requested entity.", newMessageTwo);
	}


	@Test
	public void testHasAccessDownloadWithTwoAROnEntityOneHasExemptionAndOneHasApproval() {
		Node projectOne = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		Node fileOne = nodeDaoHelper.create(n -> {
			n.setName("aFile");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId(projectOne.getId());
			n.setNodeType(EntityType.file);
		});

		//userTwo is contributor for fileOne
		aclHelper.create((a) -> {
			a.setId(fileOne.getId());
			a.setResourceAccess(Collections.singleton(
					new ResourceAccess().setPrincipalId(userTwo.getId())
							.setAccessType(Set.of(ACCESS_TYPE.UPDATE, ACCESS_TYPE.DELETE, ACCESS_TYPE.DOWNLOAD))));
		});


		//AR on fileOne
		Long arId = managedHelper.create(a -> {
			a.setAccessType(ACCESS_TYPE.DOWNLOAD);
			a.setSubjectIds(List.of(
					new RestrictableObjectDescriptor()
							.setId(fileOne.getId())
							.setType(RestrictableObjectType.ENTITY)));
		}).getId();
		;
		//AR two on fileOne
		ManagedACTAccessRequirement arIdTwo = managedHelper.create(a -> {
			a.setAccessType(ACCESS_TYPE.DOWNLOAD);
			a.setSubjectIds(List.of(
					new RestrictableObjectDescriptor()
							.setId(fileOne.getId())
							.setType(RestrictableObjectType.ENTITY)));
		});

		//Exemption ACL on AR one
		aclHelper.create((a) -> {
			a.setId(arId.toString());
			a.setResourceAccess(Set.of(
					new ResourceAccess()
							.setPrincipalId(Long.valueOf(userTwo.getId()))
							.setAccessType(Collections.singleton(ACCESS_TYPE.EXEMPTION_ELIGIBLE))));
		} , ObjectType.ACCESS_REQUIREMENT);

		// Approval for AR Two
		accessApprovalHelper.create(a -> {
			a.setCreatedBy(userOne.getId().toString());
			a.setSubmitterId(userOne.getId().toString());
			a.setRequirementId(arIdTwo.getId());
			a.setRequirementVersion(arIdTwo.getVersionNumber());
			a.setState(ApprovalState.APPROVED);
			a.setAccessorId(userTwo.getId().toString());
		});


		//Two AR, one is exemption eligible and the other has approval
		AuthorizationStatus status =entityAuthManager.hasAccess(userTwo, fileOne.getId(), ACCESS_TYPE.DOWNLOAD);
		assertNotNull(status);
		assertTrue(status.isAuthorized());

		//userOne is creator of fileOne so user met access requirement without exemption and has no download access.
		String newMessageTwo = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(userOne, fileOne.getId(), ACCESS_TYPE.DOWNLOAD).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals("You lack DOWNLOAD access to the requested entity.", newMessageTwo);
	}

	@Test
	public void testHasAccessDownloadWithMultipleAROnMultipleEntityWithAndWithoutExemption() {
		Node projectOne = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(adminUserInfo.getId());
		});
		Node fileOne = nodeDaoHelper.create(n -> {
			n.setName("aFile");
			n.setCreatedByPrincipalId(adminUserInfo.getId());
			n.setParentId(projectOne.getId());
			n.setNodeType(EntityType.file);
		});
		Node fileTwo = nodeDaoHelper.create(n -> {
			n.setName("aFileTwo");
			n.setCreatedByPrincipalId(adminUserInfo.getId());
			n.setParentId(projectOne.getId());
			n.setNodeType(EntityType.file);
		});


		//userTwo is contributor for fileOne
		aclHelper.create((a) -> {
			a.setId(fileOne.getId());
			a.setResourceAccess(Collections.singleton(
					new ResourceAccess().setPrincipalId(userTwo.getId())
							.setAccessType(Set.of(ACCESS_TYPE.UPDATE, ACCESS_TYPE.DELETE, ACCESS_TYPE.DOWNLOAD))));
		});

		//userTwo is contributor for fileTwo
		aclHelper.create((a) -> {
			a.setId(fileTwo.getId());
			a.setResourceAccess(Collections.singleton(
					new ResourceAccess().setPrincipalId(userTwo.getId())
							.setAccessType(Set.of(ACCESS_TYPE.UPDATE, ACCESS_TYPE.DELETE, ACCESS_TYPE.DOWNLOAD))));
		});


		//AR on fileOne and file Two
		Long arId = managedHelper.create(a -> {
			a.setAccessType(ACCESS_TYPE.DOWNLOAD);
			a.setSubjectIds(List.of(
					new RestrictableObjectDescriptor()
							.setId(fileOne.getId())
							.setType(RestrictableObjectType.ENTITY),
					new RestrictableObjectDescriptor()
							.setId(fileTwo.getId())
							.setType(RestrictableObjectType.ENTITY)));
		}).getId();
		;
		//AR on fileOne and file Two
		Long arIdTwo = managedHelper.create(a -> {
			a.setAccessType(ACCESS_TYPE.DOWNLOAD);
			a.setSubjectIds(List.of(
					new RestrictableObjectDescriptor()
							.setId(fileOne.getId())
							.setType(RestrictableObjectType.ENTITY),
					new RestrictableObjectDescriptor()
							.setId(fileTwo.getId())
							.setType(RestrictableObjectType.ENTITY)));
		}).getId();

		//AR three on fileOne
		Long arIdThree = managedHelper.create(a -> {
			a.setAccessType(ACCESS_TYPE.DOWNLOAD);
			a.setSubjectIds(List.of(
					new RestrictableObjectDescriptor()
							.setId(fileOne.getId())
							.setType(RestrictableObjectType.ENTITY)));
		}).getId();

		//Exemption ACL on AR One
		aclHelper.create((a) -> {
			a.setId(arId.toString());
			a.setResourceAccess(Set.of(
					new ResourceAccess()
							.setPrincipalId(Long.valueOf(userTwo.getId()))
							.setAccessType(Collections.singleton(ACCESS_TYPE.EXEMPTION_ELIGIBLE))));
		} , ObjectType.ACCESS_REQUIREMENT);

		//Exemption ACL on AR Two
		aclHelper.create((a) -> {
			a.setId(arIdTwo.toString());
			a.setResourceAccess(Set.of(
					new ResourceAccess()
							.setPrincipalId(Long.valueOf(userTwo.getId()))
							.setAccessType(Collections.singleton(ACCESS_TYPE.EXEMPTION_ELIGIBLE))));
		} , ObjectType.ACCESS_REQUIREMENT);

		//3 AR on fileOne and userTwo is exempted on 2 AR
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(userTwo, fileOne.getId(), ACCESS_TYPE.DOWNLOAD).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(ERR_MSG_THERE_ARE_UNMET_ACCESS_REQUIREMENTS, newMessage);

		//3 AR on fileTwp and userTwo is exempted on all 3 AR
		AuthorizationStatus status =entityAuthManager.hasAccess(userTwo, fileTwo.getId(), ACCESS_TYPE.DOWNLOAD);
		assertNotNull(status);
		assertTrue(status.isAuthorized());
	}
	@Test
	public void testHasAccessDownloadWithUnmetTwoFaRequirement() {
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
			a.setIsTwoFaRequired(true);
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
		
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(ERR_MSG_YOU_NEED_TWO_FA, newMessage);
	}
	
	@Test
	public void testHasAccessDownloadWithMetTwoFaRequirement() {
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
			a.setIsTwoFaRequired(true);
		});
		
		accessApprovalHelper.create(a -> {
			a.setCreatedBy(userOne.getId().toString());
			a.setSubmitterId(userOne.getId().toString());
			a.setRequirementId(managedRequirement.getId());
			a.setRequirementVersion(managedRequirement.getVersionNumber());
			a.setState(ApprovalState.APPROVED);
			a.setAccessorId(userTwo.getId().toString());
		});
		
		// Enable 2FA for the user
		authDao.setTwoFactorAuthState(userTwo.getId(), true);
		userTwo = userManager.getUserInfo(userTwo.getId());

		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.DOWNLOAD;
		
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
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

		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
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
		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
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

		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
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
		
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
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

		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(ERR_MSG_ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION, newMessage);
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
		
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.canCreate(parentId, createType, user);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testCanCreateWithNullUser() {
		String parentId = "syn123";
		EntityType createType = EntityType.project;
		UserInfo user = null;
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			entityAuthManager.canCreate(parentId, createType, user);
		}).getMessage();
		assertEquals("UserInfo is required.", message);
	}
	
	@Test
	public void testCanCreateWithNullType() {
		String parentId = "syn123";
		EntityType createType = null;
		UserInfo user = userTwo;
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			entityAuthManager.canCreate(parentId, createType, user);
		}).getMessage();
		assertEquals("entityCreateType is required.", message);
	}
	
	@Test
	public void testCanCreateWithNullParentId() {
		String parentId = null;
		EntityType createType = EntityType.project;
		UserInfo user = userTwo;
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			entityAuthManager.canCreate(parentId, createType, user);
		}).getMessage();
		assertEquals("parentId is required.", message);
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

		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.canCreate(parentId, createType, user).checkAuthorizationOrElseThrow();
		}).getMessage();
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

		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
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

		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
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

		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
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

		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(ERR_MSG_ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION, newMessage);
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

		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
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

		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
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

		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
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

		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
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

		// call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
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

		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
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

		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
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

		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
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

		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
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

		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
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
		userTwo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CHANGE_SETTINGS;

		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testHasAccessWithChangeSettingsNotCertified() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CHANGE_SETTINGS));
		});
		userTwo.getGroups().remove(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CHANGE_SETTINGS;

		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(ERR_MSG_CERTIFIED_USER_CONTENT, newMessage);
	}
	
	@Test
	public void testHasAccessWithChangeSettingsAsCreatorWithoutPermission() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userOne.getId(), ACCESS_TYPE.READ));
		});
		userOne.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		String entityId = project.getId();
		UserInfo user = userOne;
		ACCESS_TYPE accessType = ACCESS_TYPE.CHANGE_SETTINGS;

		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
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
		userTwo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		String entityId = project.getId();
		UserInfo user = userTwo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CHANGE_SETTINGS;

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

		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
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
		
		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
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

		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
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

		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.hasAccess(user, entityId, accessType);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
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
			n.setParentId(NodeConstants.BOOTSTRAP_NODES.TRASH.getId().toString());
		});
		String entityId = project.getId();
		UserInfo user = adminUserInfo;
		ACCESS_TYPE accessType = ACCESS_TYPE.CHANGE_PERMISSIONS;

		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
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

		// new call under test
		String newMessage = assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
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

		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.hasAccess(user, entityId, accessType).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(ERR_MSG_ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION, newMessage);
	}
	
	@Test
	public void testDetermineCanDeleteACL() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		Node folder = nodeDaoHelper.create(n -> {
			n.setName("aFolder");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId(project.getId());
			n.setNodeType(EntityType.folder);
		});
		aclHelper.create((a) -> {
			a.setId(folder.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CHANGE_PERMISSIONS));
		});
		String entityId = folder.getId();
		UserInfo user = userTwo;
		// call under test
		AuthorizationStatus status = entityAuthManager.canDeleteACL(user, entityId);
		AuthorizationStatus expected = AuthorizationStatus.authorized();
		assertEquals(expected, status);
	}
	
	@Test
	public void testDetermineCanDeleteACLWithRootParent() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId(NodeConstants.BOOTSTRAP_NODES.ROOT.getId().toString());
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CHANGE_PERMISSIONS));
		});
		String entityId = project.getId();
		UserInfo user = userTwo;
		// call under test
		AuthorizationStatus status = entityAuthManager.canDeleteACL(user, entityId);
		AuthorizationStatus expected = AuthorizationStatus.accessDenied(ERR_MSG_CANNOT_REMOVE_ACL_OF_PROJECT);
		assertEquals(expected, status);
	}
	
	@Test
	public void testDetermineCanDeleteACLWithAdmin() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		Node folder = nodeDaoHelper.create(n -> {
			n.setName("aFolder");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId(project.getId());
			n.setNodeType(EntityType.folder);
		});
		aclHelper.create((a) -> {
			a.setId(folder.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CHANGE_PERMISSIONS));
		});
		String entityId = folder.getId();
		UserInfo user = adminUserInfo;
		// call under test
		AuthorizationStatus status = entityAuthManager.canDeleteACL(user, entityId);
		AuthorizationStatus expected = AuthorizationStatus.authorized();
		assertEquals(expected, status);
	}
	
	@Test
	public void testDetermineCanDeleteACLWithInTrash() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId(NodeConstants.BOOTSTRAP_NODES.TRASH.getId().toString());
		});
		
		String entityId = project.getId();
		UserInfo user = userTwo;
		
		String message = assertThrows(EntityInTrashCanException.class, () -> {
			// call under test
			entityAuthManager.canDeleteACL(user, entityId).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(String.format(ERR_MSG_ENTITY_IN_TRASH_TEMPLATE, project.getId()), message);
	}
	
	@Test
	public void testDetermineCanDeleteACLWithAnonymous() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		Node folder = nodeDaoHelper.create(n -> {
			n.setName("aFolder");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId(project.getId());
			n.setNodeType(EntityType.folder);
		});
		aclHelper.create((a) -> {
			a.setId(folder.getId());
			a.getResourceAccess()
			.add(createResourceAccess(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(), ACCESS_TYPE.CHANGE_PERMISSIONS));
		});
		
		String entityId = folder.getId();
		UserInfo user = anonymousUser;
		
		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			entityAuthManager.canDeleteACL(user, entityId).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(ERR_MSG_ANONYMOUS_USERS_HAVE_ONLY_READ_ACCESS_PERMISSION, message);
	}
	
	@Test
	public void testCanCreateWiki() {
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
		
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.canCreateWiki(entityId, user);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
	
	@Test
	public void testCanCreateWikiWithFolderNotCertified() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		Node folder = nodeDaoHelper.create(n -> {
			n.setName("aFolder");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId(project.getId());
			n.setNodeType(EntityType.folder);
		});
		aclHelper.create((a) -> {
			a.setId(folder.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});
		userTwo.getGroups().remove(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		String entityId = folder.getId();
		UserInfo user = userTwo;
		
		// new call under test
		String newMessage = assertThrows(UnauthorizedException.class, () -> {
			entityAuthManager.canCreateWiki(entityId, user).checkAuthorizationOrElseThrow();
		}).getMessage();
		assertEquals(ERR_MSG_CERTIFIED_USER_CONTENT, newMessage);
	}
	
	@Test
	public void testCanCreateWikiWithFolderCertified() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(userOne.getId());
		});
		Node folder = nodeDaoHelper.create(n -> {
			n.setName("aFolder");
			n.setCreatedByPrincipalId(userOne.getId());
			n.setParentId(project.getId());
			n.setNodeType(EntityType.folder);
		});
		aclHelper.create((a) -> {
			a.setId(folder.getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.CREATE));
		});
		userTwo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		String entityId = folder.getId();
		UserInfo user = userTwo;
		
		// new call under test
		AuthorizationStatus newStatus = entityAuthManager.canCreateWiki(entityId, user);
		assertNotNull(newStatus);
		assertTrue(newStatus.isAuthorized());
	}
}
