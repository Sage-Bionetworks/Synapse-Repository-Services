package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.dataaccess.RestrictionInformationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.util.ReflectionStaticTestUtils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class EntityPermissionsManagerImplUnitTest {
	
	private EntityPermissionsManagerImpl entityPermissionsManager;
	private UserInfo nonCertifiedUserInfo;
	private UserInfo certifiedUserInfo;
	private static final String projectId = "syn123";
	private static final String folderId = "syn456";
	private static final String fileId = "syn333";
	private static final String dockerRepoId = "syn789";
	private static final String projectParentId = "syn000";
	private static final String folderParentId = "syn999";
	private static final String benefactorId = "syn987";
	private Node project;
	private Node folder;
	private Node file;
	private Node dockerRepo;
	
	@Mock
	private UserGroupDAO mockUserGroupDAO;
	@Mock
	private NodeDAO mockNodeDao;
	@Mock
	private AccessControlListDAO mockAclDAO;
	@Mock
	private RestrictionInformationManager  mockRestrictionInformationManager;
	@Mock
	private StackConfiguration mockStackConfiguration;
	@Mock
	private ProjectSettingsManager mockProjectSettingsManager;
	@Mock
	private UserInfo mockUser;
	@Mock
	private ProjectStatsManager mockProjectStatsManager;
	@Mock
	private TransactionalMessenger mockTransactionalMessenger;
	@Mock
	private ObjectTypeManager mockObjectTypeManager;
	
	private UserInfo anonymousUser;
	
	private Set<Long> mockUsersGroups;
	private Set<Long> nonvisibleIds;
	
	private String entityId;
	private Long userId;
	
	private String newEtag;

	private RestrictionInformationRequest restrictionInfoRqst;
	private RestrictionInformationResponse hasUnmetAccessRqmtResponse;
	private RestrictionInformationResponse noUnmetAccessRqmtResponse;

	// here we set up a certified and a non-certified user, a project and a non-project Node
	@BeforeEach
	public void setUp() throws Exception {
		entityPermissionsManager = new EntityPermissionsManagerImpl();
		
		nonCertifiedUserInfo = new UserInfo(false);
		nonCertifiedUserInfo.setId(765432L);
		nonCertifiedUserInfo.setGroups(Collections.singleton(9999L));
		nonCertifiedUserInfo.setAcceptsTermsOfUse(true);

		certifiedUserInfo = new UserInfo(false);
		certifiedUserInfo.setId(1234567L);
		certifiedUserInfo.setGroups(Collections.singleton(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId()));
		certifiedUserInfo.setAcceptsTermsOfUse(true);
		
		ReflectionStaticTestUtils.mockAutowire(this, entityPermissionsManager);

    	userId = 111L;
    	
    	project = new Node();
    	project.setId(projectId);
    	project.setCreatedByPrincipalId(userId);
    	project.setNodeType(EntityType.project);
       	project.setParentId(projectParentId);

    	folder = new Node();
    	folder.setId(folderId);
    	folder.setCreatedByPrincipalId(userId);
        folder.setParentId(folderParentId);
    	folder.setNodeType(EntityType.folder);

    	file = new Node();
    	file.setId(fileId);
    	file.setCreatedByPrincipalId(userId);
    	file.setParentId(folderParentId);
    	file.setNodeType(EntityType.file);

    	dockerRepo = new Node();
    	dockerRepo.setId(dockerRepoId);
    	dockerRepo.setCreatedByPrincipalId(userId);
    	dockerRepo.setParentId(folderParentId);
    	dockerRepo.setNodeType(EntityType.dockerrepo);

		anonymousUser = new UserInfo(false);
		anonymousUser.setId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		anonymousUser.setGroups(ImmutableSet.of(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId()));

		mockUsersGroups = Sets.newHashSet(444L,555L);
		nonvisibleIds = Sets.newHashSet(888L,999L);
		entityId = "syn888";
		newEtag = "newEtag";
		
		restrictionInfoRqst = new RestrictionInformationRequest();
		restrictionInfoRqst.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		hasUnmetAccessRqmtResponse = new RestrictionInformationResponse();
		hasUnmetAccessRqmtResponse.setHasUnmetAccessRequirement(true);
		noUnmetAccessRqmtResponse = new RestrictionInformationResponse();
		noUnmetAccessRqmtResponse.setHasUnmetAccessRequirement(false);

	}

	@Test
	public void testGetUserPermissionsForCertifiedUserOnProject() {
		// Mock dependencies.
		when(mockNodeDao.getNode(projectId)).thenReturn(project);
		when(mockNodeDao.getBenefactor(projectId)).thenReturn(benefactorId);
		when(mockNodeDao.getBenefactor(projectParentId)).thenReturn(benefactorId);
		when(mockNodeDao.getNodeTypeById(projectId)).thenReturn(EntityType.project);

		when(mockAclDAO.canAccess(eq(certifiedUserInfo.getGroups()), eq(benefactorId), eq(ObjectType.ENTITY),
				any(ACCESS_TYPE.class))).thenReturn(true);

		restrictionInfoRqst.setObjectId(projectId);
		when(mockRestrictionInformationManager.
				getRestrictionInformation(certifiedUserInfo, restrictionInfoRqst)).
					thenReturn(hasUnmetAccessRqmtResponse);

		// Method under test.
		UserEntityPermissions uep = entityPermissionsManager.
				getUserPermissionsForEntity(certifiedUserInfo, projectId);
		
		assertTrue(uep.getCanAddChild());
		assertTrue(uep.getCanChangePermissions()); 
		assertTrue(uep.getCanChangeSettings()); 
		assertTrue(uep.getCanDelete());
		assertTrue(uep.getCanEdit());
		assertTrue(uep.getCanEnableInheritance());
		assertFalse(uep.getCanPublicRead());
		assertTrue(uep.getCanView());
		assertFalse(uep.getCanDownload());
		assertTrue(uep.getCanUpload());
		assertTrue(uep.getCanCertifiedUserAddChild());
		assertTrue(uep.getCanCertifiedUserEdit());
		assertTrue(uep.getIsCertifiedUser());
		assertTrue(uep.getCanModerate());
		assertTrue(uep.getIsCertificationRequired());
		
		assertTrue(entityPermissionsManager.canCreate(project.getParentId(), project.getNodeType(), certifiedUserInfo).isAuthorized());
		
		assertTrue(entityPermissionsManager.canCreateWiki(projectId, certifiedUserInfo).isAuthorized());
	}
	
	@Test
	public void testGetUserPermissionsForNonCertifiedUserOnProject() {
		// Mock dependencies.
		when(mockNodeDao.getNode(projectId)).thenReturn(project);
		when(mockNodeDao.getBenefactor(projectId)).thenReturn(benefactorId);
		when(mockNodeDao.getBenefactor(projectParentId)).thenReturn(benefactorId);
		when(mockNodeDao.getNodeTypeById(projectId)).thenReturn(EntityType.project);

		when(mockAclDAO.canAccess(eq(nonCertifiedUserInfo.getGroups()), eq(benefactorId), eq(ObjectType.ENTITY),
				any(ACCESS_TYPE.class))).thenReturn(true);

		restrictionInfoRqst.setObjectId(projectId);
		when(mockRestrictionInformationManager.
				getRestrictionInformation(nonCertifiedUserInfo, restrictionInfoRqst)).
					thenReturn(hasUnmetAccessRqmtResponse);

		// Method under test.
		UserEntityPermissions uep = entityPermissionsManager.
				getUserPermissionsForEntity(nonCertifiedUserInfo, projectId);
		
		assertFalse(uep.getCanAddChild()); // not certified!
		assertTrue(uep.getCanChangePermissions()); 
		assertTrue(uep.getCanChangeSettings()); 
		assertTrue(uep.getCanDelete());
		assertTrue(uep.getCanEdit()); // not certified but is a project!
		assertTrue(uep.getCanEnableInheritance());
		assertFalse(uep.getCanPublicRead());
		assertTrue(uep.getCanView());
		assertFalse(uep.getCanDownload());
		assertTrue(uep.getCanUpload());
		assertTrue(uep.getCanCertifiedUserAddChild());
		assertTrue(uep.getCanCertifiedUserEdit());
		assertFalse(uep.getIsCertifiedUser()); // not certified!
		assertTrue(uep.getCanModerate());
		assertTrue(uep.getIsCertificationRequired());
		
		assertTrue(entityPermissionsManager.canCreate(project.getParentId(), project.getNodeType(), nonCertifiedUserInfo).isAuthorized());
		
		assertTrue(entityPermissionsManager.canCreateWiki(projectId, nonCertifiedUserInfo).isAuthorized());
	}

	@Test
	public void testCanUpload() {
		// Mock dependencies.
		when(mockNodeDao.getNode(projectId)).thenReturn(project);
		when(mockNodeDao.getBenefactor(projectId)).thenReturn(benefactorId);
		when(mockNodeDao.getNodeTypeById(projectId)).thenReturn(EntityType.project);

		when(mockAclDAO.canAccess(eq(certifiedUserInfo.getGroups()), eq(benefactorId), eq(ObjectType.ENTITY),
				any(ACCESS_TYPE.class))).thenReturn(true);

		restrictionInfoRqst.setObjectId(projectId);
		when(mockRestrictionInformationManager.
				getRestrictionInformation(certifiedUserInfo, restrictionInfoRqst)).
					thenReturn(hasUnmetAccessRqmtResponse);
		
		// show that user has access
		
		// method under test
		UserEntityPermissions uep = entityPermissionsManager.
				getUserPermissionsForEntity(certifiedUserInfo, projectId);
		
		assertTrue(uep.getCanUpload());
	}
	
	@Test
	public void testGetUserPermissionsForCertifiedUserOnFolder() {
		// Mock dependencies.
		when(mockNodeDao.getNode(folderId)).thenReturn(folder);
		when(mockNodeDao.getBenefactor(folderId)).thenReturn(benefactorId);
		when(mockNodeDao.getBenefactor(folderParentId)).thenReturn(benefactorId);
		when(mockNodeDao.getNodeTypeById(folderId)).thenReturn(EntityType.folder);

		when(mockAclDAO.canAccess(eq(certifiedUserInfo.getGroups()), eq(benefactorId), eq(ObjectType.ENTITY),
				any(ACCESS_TYPE.class))).thenReturn(true);
		
		restrictionInfoRqst.setObjectId(folderId);
		when(mockRestrictionInformationManager.
				getRestrictionInformation(certifiedUserInfo, restrictionInfoRqst)).
					thenReturn(noUnmetAccessRqmtResponse);

		// Method under test.
		UserEntityPermissions uep = entityPermissionsManager.
				getUserPermissionsForEntity(certifiedUserInfo, folderId);
		
		assertTrue(uep.getCanAddChild());
		assertTrue(uep.getCanChangePermissions()); 
		assertTrue(uep.getCanChangeSettings()); 
		assertTrue(uep.getCanDelete());
		assertTrue(uep.getCanEdit());
		assertTrue(uep.getCanEnableInheritance());
		assertFalse(uep.getCanPublicRead());
		assertTrue(uep.getCanView());
		assertTrue(uep.getCanDownload());
		assertTrue(uep.getCanUpload());
		assertTrue(uep.getCanCertifiedUserAddChild());
		assertTrue(uep.getCanCertifiedUserEdit());
		assertTrue(uep.getIsCertifiedUser());
		assertTrue(uep.getCanModerate());
		assertTrue(uep.getIsCertificationRequired());
		
		assertTrue(entityPermissionsManager.canCreate(folder.getParentId(), folder.getNodeType(), certifiedUserInfo).isAuthorized());
		
		assertTrue(entityPermissionsManager.canCreateWiki(folderId, certifiedUserInfo).isAuthorized());
	}
	
	@Test
	public void testReadButNotDownload() {
		// Mock dependencies.
		when(mockNodeDao.getNode(folderId)).thenReturn(folder);
		when(mockNodeDao.getBenefactor(folderId)).thenReturn(benefactorId);
		when(mockNodeDao.getBenefactor(folderParentId)).thenReturn(benefactorId);
		when(mockNodeDao.getNodeTypeById(folderId)).thenReturn(EntityType.folder);

		when(mockAclDAO.canAccess(eq(certifiedUserInfo.getGroups()), eq(benefactorId), eq(ObjectType.ENTITY),
				any(ACCESS_TYPE.class))).thenReturn(true);

		// if READ is in the ACL but DOWNLOAD is not in the ACL, then I can't download
		when(mockAclDAO.canAccess(eq(certifiedUserInfo.getGroups()), eq(benefactorId), 
				eq(ObjectType.ENTITY), eq(ACCESS_TYPE.DOWNLOAD))).thenReturn(false);
		// check that my mocks are set up correctly
		assertTrue(mockAclDAO.canAccess(certifiedUserInfo.getGroups(), benefactorId, 
				ObjectType.ENTITY, ACCESS_TYPE.READ));
		assertFalse(mockAclDAO.canAccess(certifiedUserInfo.getGroups(), benefactorId, 
				ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD));
		
		// now on to the test:
		UserEntityPermissions uep = entityPermissionsManager.
				getUserPermissionsForEntity(certifiedUserInfo, folderId);
		
		assertTrue(uep.getCanAddChild());
		assertTrue(uep.getCanChangePermissions()); 
		assertTrue(uep.getCanChangeSettings()); 
		assertTrue(uep.getCanDelete());
		assertTrue(uep.getCanEdit());
		assertTrue(uep.getCanEnableInheritance());
		assertFalse(uep.getCanPublicRead());
		assertTrue(uep.getCanView());
		assertFalse(uep.getCanDownload());
		assertTrue(uep.getCanUpload());
		assertTrue(uep.getCanCertifiedUserAddChild());
		assertTrue(uep.getCanCertifiedUserEdit());
		assertTrue(uep.getIsCertifiedUser());
		assertTrue(uep.getCanModerate());
		assertTrue(uep.getIsCertificationRequired());
		
		assertTrue(entityPermissionsManager.canCreate(folder.getParentId(), folder.getNodeType(), certifiedUserInfo).isAuthorized());
		
		assertTrue(entityPermissionsManager.canCreateWiki(folderId, certifiedUserInfo).isAuthorized());
	}
	
	@Test
	public void testGetUserPermissionsForNonCertifiedUserOnFolder() {
		// Mock dependencies.
		when(mockNodeDao.getNode(folderId)).thenReturn(folder);
		when(mockNodeDao.getBenefactor(folderId)).thenReturn(benefactorId);
		when(mockNodeDao.getNodeTypeById(folderId)).thenReturn(EntityType.folder);

		when(mockAclDAO.canAccess(eq(nonCertifiedUserInfo.getGroups()), eq(benefactorId), eq(ObjectType.ENTITY),
				any(ACCESS_TYPE.class))).thenReturn(true);

		restrictionInfoRqst.setObjectId(folderId);
		when(mockRestrictionInformationManager.
				getRestrictionInformation(nonCertifiedUserInfo, restrictionInfoRqst)).
					thenReturn(noUnmetAccessRqmtResponse);

		// Method under test.
		UserEntityPermissions uep = entityPermissionsManager.
				getUserPermissionsForEntity(nonCertifiedUserInfo, folderId);
		
		assertFalse(uep.getCanAddChild()); // not certified!
		assertTrue(uep.getCanChangePermissions()); 
		assertTrue(uep.getCanChangeSettings()); 
		assertTrue(uep.getCanDelete());
		assertFalse(uep.getCanEdit()); // not certified and not a project!
		assertTrue(uep.getCanEnableInheritance());
		assertFalse(uep.getCanPublicRead());
		assertTrue(uep.getCanView());
		assertTrue(uep.getCanDownload());
		assertTrue(uep.getCanUpload());
		assertTrue(uep.getCanCertifiedUserAddChild());
		assertTrue(uep.getCanCertifiedUserEdit());
		assertFalse(uep.getIsCertifiedUser()); // not certified!
		assertTrue(uep.getCanModerate());
		assertTrue(uep.getIsCertificationRequired());
		
		assertFalse(entityPermissionsManager.canCreate(folder.getParentId(), folder.getNodeType(), nonCertifiedUserInfo).isAuthorized());

		assertThrows(UserCertificationRequiredException.class,
				() -> entityPermissionsManager.canCreate(folder.getParentId(), folder.getNodeType(), nonCertifiedUserInfo).checkAuthorizationOrElseThrow()
		);


		assertFalse(entityPermissionsManager.canCreateWiki(folderId, nonCertifiedUserInfo).isAuthorized());
	}
	
	@Test
	public void testAnonymousCannotDownloadDockerRepo() {
		// Mock dependencies.
		when(mockNodeDao.getNodeTypeById(dockerRepoId)).thenReturn(EntityType.dockerrepo);
		when(mockNodeDao.getBenefactor(dockerRepoId)).thenReturn(benefactorId);

		assertFalse(entityPermissionsManager.hasAccess(dockerRepoId, ACCESS_TYPE.DOWNLOAD, anonymousUser).isAuthorized());
	}
	
	@Test
	public void testGetUserPermissionsForCertifiedUserOnDockerRepo() {
		// Mock dependencies.
		when(mockNodeDao.getNode(dockerRepoId)).thenReturn(dockerRepo);
		when(mockNodeDao.getBenefactor(dockerRepoId)).thenReturn(benefactorId);
		when(mockNodeDao.getNodeTypeById(dockerRepoId)).thenReturn(EntityType.dockerrepo);
		when(mockAclDAO.canAccess(eq(certifiedUserInfo.getGroups()), eq(benefactorId), eq(ObjectType.ENTITY),
				any(ACCESS_TYPE.class))).thenReturn(true);
		restrictionInfoRqst.setObjectId(dockerRepoId);
		when(mockRestrictionInformationManager.
				getRestrictionInformation(certifiedUserInfo, restrictionInfoRqst)).
					thenReturn(noUnmetAccessRqmtResponse);

		// Method under test.
		UserEntityPermissions uep = entityPermissionsManager.
				getUserPermissionsForEntity(certifiedUserInfo, dockerRepoId);
		
		assertTrue(uep.getCanAddChild());
		assertTrue(uep.getCanChangePermissions()); 
		assertTrue(uep.getCanChangeSettings()); 
		assertTrue(uep.getCanDelete());
		assertTrue(uep.getCanEdit());
		assertTrue(uep.getCanEnableInheritance());
		assertFalse(uep.getCanPublicRead());
		assertTrue(uep.getCanView());
		assertTrue(uep.getCanDownload());
		assertTrue(uep.getCanUpload());
		assertTrue(uep.getCanCertifiedUserAddChild());
		assertTrue(uep.getCanCertifiedUserEdit());
		assertTrue(uep.getIsCertifiedUser());
		assertTrue(uep.getCanModerate());
		assertTrue(uep.getIsCertificationRequired());
		
	}

	@Test
	public void testGetNonvisibleChildrenNonAdmin(){
		// Mock dependencies.
		when(mockUser.getGroups()).thenReturn(mockUsersGroups);
		when(mockAclDAO.getNonVisibleChilrenOfEntity(anySet(), anyString())).thenReturn(nonvisibleIds);

		String parentId = "syn123";
		// call under test
		Set<Long> results = entityPermissionsManager.getNonvisibleChildren(mockUser, parentId);
		verify(mockAclDAO).getNonVisibleChilrenOfEntity(mockUsersGroups, parentId);
		assertEquals(nonvisibleIds, results);
	}
	
	@Test
	public void testGetNonvisibleChildrenAdmin(){
		when(mockUser.isAdmin()).thenReturn(true);
		String parentId = "syn123";
		// call under test
		Set<Long> results = entityPermissionsManager.getNonvisibleChildren(mockUser, parentId);
		// should not hit the dao.
		verify(mockAclDAO, never()).getNonVisibleChilrenOfEntity(anySet(), anyString());
		// empty results.
		assertEquals(new HashSet<Long>(), results);
	}
	
	@Test
	public void testGetNonvisibleChildrenNullUser(){
		String parentId = "syn123";
		mockUser = null;
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> entityPermissionsManager.getNonvisibleChildren(mockUser, parentId)
		);
	}
	
	@Test
	public void testGetNonvisibleChildrenNullParentId(){
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> entityPermissionsManager.getNonvisibleChildren(mockUser, null)
		);
	}
	
	@Test
	public void testUpdateAcl(){
		// Mock dependencies.
		when(mockNodeDao.getBenefactor(entityId)).thenReturn(entityId);

		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.isAdmin()).thenReturn(false);
		when(mockUser.getGroups()).thenReturn(mockUsersGroups);

		when(mockAclDAO.canAccess(mockUser.getGroups(), entityId, ObjectType.ENTITY, ACCESS_TYPE.CHANGE_PERMISSIONS))
				.thenReturn(true);

		Long addedPrincipalId = 444L;
		AccessControlList oldAcl = AccessControlListUtil.createACLToGrantEntityAdminAccess(entityId, mockUser, new Date());
		AccessControlList updatedAcl = AccessControlListUtil.createACLToGrantEntityAdminAccess(entityId, mockUser, new Date());
		// add access to another User
		ResourceAccess access = new ResourceAccess();
		access.setAccessType(Sets.newHashSet(ACCESS_TYPE.READ));
		access.setPrincipalId(addedPrincipalId);
		updatedAcl.getResourceAccess().add(access);
		
		when(mockNodeDao.getCreatedBy(entityId)).thenReturn(111L);
		when(mockAclDAO.get(entityId, ObjectType.ENTITY)).thenReturn(oldAcl);
		
		// call under test
		entityPermissionsManager.updateACL(updatedAcl, mockUser);
		// project stats should be called for all new users
		verify(mockProjectStatsManager).updateProjectStats(eq(addedPrincipalId), eq(entityId), eq(ObjectType.ENTITY), any(Date.class));
	}
	
	@Test
	public void testOverrideInheritanceProject(){
		// Mock dependencies.
		when(mockNodeDao.getNode(projectId)).thenReturn(project);
		when(mockNodeDao.getBenefactor(projectId)).thenReturn(benefactorId);
		when(mockNodeDao.getBenefactor(benefactorId)).thenReturn(benefactorId);

		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.isAdmin()).thenReturn(false);
		when(mockUser.getGroups()).thenReturn(mockUsersGroups);

		when(mockAclDAO.canAccess(anySet(), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class)))
				.thenReturn(true);
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(projectId, mockUser, new Date());
		when(mockAclDAO.get(projectId, ObjectType.ENTITY)).thenReturn(acl);
		when(mockNodeDao.touch(userId, projectId)).thenReturn(newEtag);

		// call under test
		entityPermissionsManager.overrideInheritance(acl, mockUser);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(projectId, ObjectType.ENTITY_CONTAINER, ChangeType.UPDATE);
	}
	
	@Test
	public void testOverrideInheritanceFolder(){
		// Mock dependencies.
		when(mockNodeDao.getNode(folderId)).thenReturn(folder);
		when(mockNodeDao.getBenefactor(folderId)).thenReturn(benefactorId);
		when(mockNodeDao.getBenefactor(benefactorId)).thenReturn(benefactorId);

		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.isAdmin()).thenReturn(false);
		when(mockUser.getGroups()).thenReturn(mockUsersGroups);

		when(mockAclDAO.canAccess(anySet(), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class)))
				.thenReturn(true);
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(folderId, mockUser, new Date());
		when(mockAclDAO.get(folderId, ObjectType.ENTITY)).thenReturn(acl);
		when(mockNodeDao.touch(userId, folderId)).thenReturn(newEtag);

		// call under test
		entityPermissionsManager.overrideInheritance(acl, mockUser);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(folderId, ObjectType.ENTITY_CONTAINER, ChangeType.UPDATE);
	}
	
	@Test
	public void testOverrideInheritanceFile(){
		// Mock dependencies.
		when(mockNodeDao.getNode(fileId)).thenReturn(file);
		when(mockNodeDao.getBenefactor(fileId)).thenReturn(benefactorId);
		when(mockNodeDao.getBenefactor(benefactorId)).thenReturn(benefactorId);

		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.isAdmin()).thenReturn(false);
		when(mockUser.getGroups()).thenReturn(mockUsersGroups);

		when(mockAclDAO.canAccess(anySet(), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class)))
				.thenReturn(true);
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(fileId, mockUser, new Date());
		when(mockAclDAO.get(fileId, ObjectType.ENTITY)).thenReturn(acl);

		// call under test
		entityPermissionsManager.overrideInheritance(acl, mockUser);
		// file should not trigger container message
		verifyNoMoreInteractions(mockTransactionalMessenger);
	}

	@Test
	public void testOverrideInheritance_CannotOverrideOnChildOfStsFolder() {
		// Mock dependencies.
		when(mockNodeDao.getNode(fileId)).thenReturn(file);
		when(mockNodeDao.getBenefactor(fileId)).thenReturn(benefactorId);
		when(mockNodeDao.getBenefactor(benefactorId)).thenReturn(benefactorId);

		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.isAdmin()).thenReturn(false);
		when(mockUser.getGroups()).thenReturn(mockUsersGroups);

		when(mockAclDAO.canAccess(anySet(), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class)))
				.thenReturn(true);
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(fileId, mockUser, new Date());

		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setProjectId(folderId);
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, fileId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class)).thenReturn(Optional.of(projectSetting));
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(true);

		// Call under test - throws exception.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> entityPermissionsManager.overrideInheritance(acl, mockUser));
		assertEquals("Cannot override ACLs in a child of an STS-enabled folder", ex.getMessage());
	}

	@Test
	public void testOverrideInheritance_CanOverrideOnStsFolder() {
		// Mock dependencies.
		when(mockNodeDao.getNode(folderId)).thenReturn(folder);
		when(mockNodeDao.getBenefactor(folderId)).thenReturn(benefactorId);
		when(mockNodeDao.getBenefactor(benefactorId)).thenReturn(benefactorId);

		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.isAdmin()).thenReturn(false);
		when(mockUser.getGroups()).thenReturn(mockUsersGroups);

		when(mockAclDAO.canAccess(anySet(), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class)))
				.thenReturn(true);
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(folderId, mockUser, new Date());
		when(mockAclDAO.get(folderId, ObjectType.ENTITY)).thenReturn(acl);

		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setProjectId(folderId);
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, folderId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class)).thenReturn(Optional.of(projectSetting));
		// It doesn't actually matter whether the folder is STS or not, because the permissions we're overriding are
		// on the same folder as the folder with the project settings.

		// Call under test.
		AccessControlList result = entityPermissionsManager.overrideInheritance(acl, mockUser);
		assertSame(acl, result);
		verify(mockAclDAO).create(acl, ObjectType.ENTITY);
	}

	@Test
	public void testOverrideInheritance_CanOverrideOnNonStsFolder() {
		// Mock dependencies.
		when(mockNodeDao.getNode(fileId)).thenReturn(file);
		when(mockNodeDao.getBenefactor(fileId)).thenReturn(benefactorId);
		when(mockNodeDao.getBenefactor(benefactorId)).thenReturn(benefactorId);

		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.isAdmin()).thenReturn(false);
		when(mockUser.getGroups()).thenReturn(mockUsersGroups);

		when(mockAclDAO.canAccess(anySet(), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class)))
				.thenReturn(true);
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(fileId, mockUser, new Date());
		when(mockAclDAO.get(fileId, ObjectType.ENTITY)).thenReturn(acl);

		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setProjectId(folderId);
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, fileId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class)).thenReturn(Optional.of(projectSetting));
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(false);

		// Call under test.
		AccessControlList result = entityPermissionsManager.overrideInheritance(acl, mockUser);
		assertSame(acl, result);
		verify(mockAclDAO).create(acl, ObjectType.ENTITY);
	}

	@Test
	public void testRestoreInheritanceProject(){
		when(mockNodeDao.getBenefactor(project.getId())).thenReturn(project.getId());
		when(mockAclDAO.canAccess(anySet(), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class))).
		thenReturn(true);
		when(mockNodeDao.touch(any(Long.class), anyString())).thenReturn(newEtag);
		when(mockNodeDao.getNodeTypeById(projectId)).thenReturn(EntityType.project);
		// call under test
		entityPermissionsManager.restoreInheritance(projectId, mockUser);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(projectId, ObjectType.ENTITY_CONTAINER, ChangeType.UPDATE);
	}
	
	@Test
	public void testRestoreInheritanceFolder(){
		when(mockNodeDao.getBenefactor(folder.getId())).thenReturn(folder.getId());
		when(mockAclDAO.canAccess(anySet(), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class))).
		thenReturn(true);
		when(mockNodeDao.touch(any(Long.class), anyString())).thenReturn(newEtag);
		when(mockNodeDao.getNodeTypeById(folderId)).thenReturn(EntityType.folder);
		// call under test
		entityPermissionsManager.restoreInheritance(folderId, mockUser);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(folderId, ObjectType.ENTITY_CONTAINER, ChangeType.UPDATE);
	}
	
	@Test
	public void testRestoreInheritanceFile(){
		when(mockNodeDao.getBenefactor(file.getId())).thenReturn(file.getId());
		when(mockAclDAO.canAccess(anySet(), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class))).
		thenReturn(true);
		when(mockNodeDao.touch(any(Long.class), anyString())).thenReturn(newEtag);
		when(mockNodeDao.getNodeTypeById(fileId)).thenReturn(EntityType.file);
		// call under test
		entityPermissionsManager.restoreInheritance(fileId, mockUser);
		verifyNoMoreInteractions(mockTransactionalMessenger);
	}
		
	// Tests for PLFM-6059

	@Test
	public void testHasDownloadAccessAsCertifiedUserAndUnacceptedTermOfUse() {
		String nodeId = fileId;
		String benefactorId = nodeId;
		UserInfo userInfo = certifiedUserInfo;
		boolean acceptedTermsOfUse = false;
		
		when(mockNodeDao.getNodeTypeById(nodeId)).thenReturn(EntityType.file);
		when(mockNodeDao.getBenefactor(nodeId)).thenReturn(benefactorId);
		userInfo.setAcceptsTermsOfUse(acceptedTermsOfUse);
		
		// Call under test
		AuthorizationStatus status = entityPermissionsManager.hasAccess(nodeId, ACCESS_TYPE.DOWNLOAD, userInfo);
		
		assertFalse(status.isAuthorized());
		assertEquals("You have not yet agreed to the Synapse Terms of Use.", status.getMessage());
		
		verify(mockNodeDao).getNodeTypeById(nodeId);
		verify(mockNodeDao).getBenefactor(nodeId);
	}
	
	@Test
	public void testHasDownloadAccessWithOpenDataAsAnonymous() {
		String nodeId = fileId;
		DataType dataType = DataType.OPEN_DATA;
		String benefactorId = nodeId;
		UserInfo userInfo = anonymousUser;
		
		when(mockNodeDao.getNodeTypeById(nodeId)).thenReturn(EntityType.file);
		when(mockNodeDao.getBenefactor(nodeId)).thenReturn(benefactorId);
		when(mockObjectTypeManager.getObjectsDataType(nodeId, ObjectType.ENTITY)).thenReturn(dataType);
		when(mockAclDAO.canAccess(userInfo.getGroups(), benefactorId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(true);
		restrictionInfoRqst.setObjectId(nodeId);
		when(mockRestrictionInformationManager.
				getRestrictionInformation(userInfo, restrictionInfoRqst)).
					thenReturn(noUnmetAccessRqmtResponse);
		
		// Call under test
		AuthorizationStatus status = entityPermissionsManager.hasAccess(nodeId, ACCESS_TYPE.DOWNLOAD, userInfo);
		
		assertTrue(status.isAuthorized());
		
		verify(mockAclDAO).canAccess(userInfo.getGroups(), benefactorId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockNodeDao).getNodeTypeById(nodeId);
		verify(mockNodeDao).getBenefactor(nodeId);
		verify(mockObjectTypeManager).getObjectsDataType(nodeId, ObjectType.ENTITY);
	}
	
	@Test
	public void testHasDownloadAccessWithOpenDataAsAnonymousWithUnmetAccessRequirements() {
		
		String nodeId = fileId;
		DataType dataType = DataType.OPEN_DATA;
		String benefactorId = nodeId;
		UserInfo userInfo = anonymousUser;
		
		when(mockNodeDao.getNodeTypeById(nodeId)).thenReturn(EntityType.file);
		when(mockNodeDao.getBenefactor(nodeId)).thenReturn(benefactorId);
		when(mockObjectTypeManager.getObjectsDataType(nodeId, ObjectType.ENTITY)).thenReturn(dataType);
		when(mockAclDAO.canAccess(userInfo.getGroups(), benefactorId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(true);
		restrictionInfoRqst.setObjectId(nodeId);
		when(mockRestrictionInformationManager.
				getRestrictionInformation(userInfo, restrictionInfoRqst)).
					thenReturn(hasUnmetAccessRqmtResponse);
		
		// Call under test
		AuthorizationStatus status = entityPermissionsManager.hasAccess(nodeId, ACCESS_TYPE.DOWNLOAD, userInfo);
		
		assertFalse(status.isAuthorized());
		assertEquals("There are unmet access requirements that must be met to read content in the requested container.", status.getMessage());
		
		verify(mockAclDAO).canAccess(userInfo.getGroups(), benefactorId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockNodeDao).getNodeTypeById(nodeId);
		verify(mockNodeDao).getBenefactor(nodeId);
		verify(mockObjectTypeManager).getObjectsDataType(nodeId, ObjectType.ENTITY);
		verify(mockRestrictionInformationManager).getRestrictionInformation(userInfo, restrictionInfoRqst);

	}
	
	@Test
	public void testHasDownloadAccessWithOpenDataAsCertifiedUser() {
		String nodeId = fileId;
		DataType dataType = DataType.OPEN_DATA;
		String benefactorId = nodeId;
		UserInfo userInfo = certifiedUserInfo;
		boolean acceptedTermsOfUse = true;
		userInfo.setAcceptsTermsOfUse(acceptedTermsOfUse);
		
		when(mockNodeDao.getNodeTypeById(nodeId)).thenReturn(EntityType.file);
		when(mockNodeDao.getBenefactor(nodeId)).thenReturn(benefactorId);
		when(mockObjectTypeManager.getObjectsDataType(nodeId, ObjectType.ENTITY)).thenReturn(dataType);
		when(mockAclDAO.canAccess(userInfo.getGroups(), benefactorId, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(true);
		restrictionInfoRqst.setObjectId(nodeId);
		when(mockRestrictionInformationManager.
				getRestrictionInformation(userInfo, restrictionInfoRqst)).
					thenReturn(noUnmetAccessRqmtResponse);
		
		// Call under test
		AuthorizationStatus status = entityPermissionsManager.hasAccess(nodeId, ACCESS_TYPE.DOWNLOAD, userInfo);
		
		assertTrue(status.isAuthorized());
		
		verify(mockAclDAO).canAccess(userInfo.getGroups(), benefactorId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockNodeDao).getNodeTypeById(nodeId);
		verify(mockNodeDao).getBenefactor(nodeId);
		verify(mockObjectTypeManager).getObjectsDataType(nodeId, ObjectType.ENTITY);
	}
	
	@Test
	public void testHasDownloadAccessWihoutOpenDataAsAnonymous() {
		
		UserInfo userInfo = anonymousUser;
		String nodeId = fileId;
		DataType dataType = DataType.SENSITIVE_DATA;
		String benefactorId = nodeId;
		
		when(mockNodeDao.getNodeTypeById(nodeId)).thenReturn(EntityType.file);
		when(mockNodeDao.getBenefactor(nodeId)).thenReturn(benefactorId);
		when(mockObjectTypeManager.getObjectsDataType(nodeId, ObjectType.ENTITY)).thenReturn(dataType);

		when(mockAclDAO.canAccess(userInfo.getGroups(), benefactorId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD)).thenReturn(false);
		
		// Call under test
		AuthorizationStatus status = entityPermissionsManager.hasAccess(nodeId, ACCESS_TYPE.DOWNLOAD, userInfo);
		
		assertFalse(status.isAuthorized());
		assertEquals("You lack DOWNLOAD access to the requested entity.", status.getMessage());
		
		verify(mockNodeDao).getNodeTypeById(nodeId);
		verify(mockNodeDao).getBenefactor(nodeId);
		verify(mockObjectTypeManager).getObjectsDataType(nodeId, ObjectType.ENTITY);
		verify(mockAclDAO).canAccess(userInfo.getGroups(), benefactorId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD);
	}
	
}