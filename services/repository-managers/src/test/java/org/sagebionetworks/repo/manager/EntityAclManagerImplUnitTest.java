package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
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
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class EntityAclManagerImplUnitTest {
	
	@InjectMocks
	private EntityAclManagerImpl entityAclManager;
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
	private EntityAuthorizationManager mockEntityAuthorizationManager;
	
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
		
		nonCertifiedUserInfo = new UserInfo(false);
		nonCertifiedUserInfo.setId(765432L);
		nonCertifiedUserInfo.setGroups(Collections.singleton(9999L));
		nonCertifiedUserInfo.setAcceptsTermsOfUse(true);

		certifiedUserInfo = new UserInfo(false);
		certifiedUserInfo.setId(1234567L);
		certifiedUserInfo.setGroups(Collections.singleton(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId()));
		certifiedUserInfo.setAcceptsTermsOfUse(true);

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
	public void testGetNonvisibleChildrenNonAdmin(){
		// Mock dependencies.
		when(mockUser.getGroups()).thenReturn(mockUsersGroups);
		when(mockAclDAO.getNonVisibleChilrenOfEntity(anySet(), anyString())).thenReturn(nonvisibleIds);

		String parentId = "syn123";
		// call under test
		Set<Long> results = entityAclManager.getNonvisibleChildren(mockUser, parentId);
		verify(mockAclDAO).getNonVisibleChilrenOfEntity(mockUsersGroups, parentId);
		assertEquals(nonvisibleIds, results);
	}
	
	@Test
	public void testGetNonvisibleChildrenAdmin(){
		when(mockUser.isAdmin()).thenReturn(true);
		String parentId = "syn123";
		// call under test
		Set<Long> results = entityAclManager.getNonvisibleChildren(mockUser, parentId);
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
				() -> entityAclManager.getNonvisibleChildren(mockUser, parentId)
		);
	}
	
	@Test
	public void testGetNonvisibleChildrenNullParentId(){
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> entityAclManager.getNonvisibleChildren(mockUser, null)
		);
	}
	
	@Test
	public void testUpdateAcl(){
		// Mock dependencies.
		when(mockNodeDao.getBenefactor(entityId)).thenReturn(entityId);

		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.isAdmin()).thenReturn(false);
		when(mockUser.getGroups()).thenReturn(mockUsersGroups);
		
		when(mockEntityAuthorizationManager.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());

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
		entityAclManager.updateACL(updatedAcl, mockUser);
		// project stats should be called for all new users
		verify(mockProjectStatsManager).updateProjectStats(eq(addedPrincipalId), eq(entityId), eq(ObjectType.ENTITY), any(Date.class));
	}
	
	@Test
	public void testOverrideInheritanceProject(){
		// Mock dependencies.
		when(mockNodeDao.getNode(projectId)).thenReturn(project);
		when(mockNodeDao.getBenefactor(projectId)).thenReturn(benefactorId);

		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.isAdmin()).thenReturn(false);
		when(mockUser.getGroups()).thenReturn(mockUsersGroups);

		when(mockEntityAuthorizationManager.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(projectId, mockUser, new Date());
		when(mockAclDAO.get(projectId, ObjectType.ENTITY)).thenReturn(acl);
		when(mockNodeDao.touch(userId, projectId)).thenReturn(newEtag);

		// call under test
		entityAclManager.overrideInheritance(acl, mockUser);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(projectId, ObjectType.ENTITY_CONTAINER, ChangeType.UPDATE);
	}
	
	@Test
	public void testOverrideInheritanceFolder(){
		// Mock dependencies.
		when(mockNodeDao.getNode(folderId)).thenReturn(folder);
		when(mockNodeDao.getBenefactor(folderId)).thenReturn(benefactorId);

		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.isAdmin()).thenReturn(false);
		when(mockUser.getGroups()).thenReturn(mockUsersGroups);

		when(mockEntityAuthorizationManager.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(folderId, mockUser, new Date());
		when(mockAclDAO.get(folderId, ObjectType.ENTITY)).thenReturn(acl);
		when(mockNodeDao.touch(userId, folderId)).thenReturn(newEtag);

		// call under test
		entityAclManager.overrideInheritance(acl, mockUser);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(folderId, ObjectType.ENTITY_CONTAINER, ChangeType.UPDATE);
	}
	
	@Test
	public void testOverrideInheritanceFile(){
		// Mock dependencies.
		when(mockNodeDao.getNode(fileId)).thenReturn(file);
		when(mockNodeDao.getBenefactor(fileId)).thenReturn(benefactorId);

		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.isAdmin()).thenReturn(false);
		when(mockUser.getGroups()).thenReturn(mockUsersGroups);

		when(mockEntityAuthorizationManager.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(fileId, mockUser, new Date());
		when(mockAclDAO.get(fileId, ObjectType.ENTITY)).thenReturn(acl);

		// call under test
		entityAclManager.overrideInheritance(acl, mockUser);
		// file should not trigger container message
		verifyNoMoreInteractions(mockTransactionalMessenger);
	}

	@Test
	public void testOverrideInheritance_CannotOverrideOnChildOfStsFolder() {
		// Mock dependencies.
		when(mockNodeDao.getNode(fileId)).thenReturn(file);
		when(mockNodeDao.getBenefactor(fileId)).thenReturn(benefactorId);

		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.isAdmin()).thenReturn(false);
		when(mockUser.getGroups()).thenReturn(mockUsersGroups);

		when(mockEntityAuthorizationManager.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(fileId, mockUser, new Date());

		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setProjectId(folderId);
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, fileId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class)).thenReturn(Optional.of(projectSetting));
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(true);

		// Call under test - throws exception.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> entityAclManager.overrideInheritance(acl, mockUser));
		assertEquals("Cannot override ACLs in a child of an STS-enabled folder", ex.getMessage());
	}

	@Test
	public void testOverrideInheritance_CanOverrideOnStsFolder() {
		// Mock dependencies.
		when(mockNodeDao.getNode(folderId)).thenReturn(folder);
		when(mockNodeDao.getBenefactor(folderId)).thenReturn(benefactorId);

		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.isAdmin()).thenReturn(false);
		when(mockUser.getGroups()).thenReturn(mockUsersGroups);

		when(mockEntityAuthorizationManager.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(folderId, mockUser, new Date());
		when(mockAclDAO.get(folderId, ObjectType.ENTITY)).thenReturn(acl);

		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setProjectId(folderId);
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, folderId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class)).thenReturn(Optional.of(projectSetting));
		// It doesn't actually matter whether the folder is STS or not, because the permissions we're overriding are
		// on the same folder as the folder with the project settings.

		// Call under test.
		AccessControlList result = entityAclManager.overrideInheritance(acl, mockUser);
		assertSame(acl, result);
		verify(mockAclDAO).create(acl, ObjectType.ENTITY);
	}

	@Test
	public void testOverrideInheritance_CanOverrideOnNonStsFolder() {
		// Mock dependencies.
		when(mockNodeDao.getNode(fileId)).thenReturn(file);
		when(mockNodeDao.getBenefactor(fileId)).thenReturn(benefactorId);

		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.isAdmin()).thenReturn(false);
		when(mockUser.getGroups()).thenReturn(mockUsersGroups);

		when(mockEntityAuthorizationManager.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(fileId, mockUser, new Date());
		when(mockAclDAO.get(fileId, ObjectType.ENTITY)).thenReturn(acl);

		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setProjectId(folderId);
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, fileId, ProjectSettingsType.upload,
				UploadDestinationListSetting.class)).thenReturn(Optional.of(projectSetting));
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(false);

		// Call under test.
		AccessControlList result = entityAclManager.overrideInheritance(acl, mockUser);
		assertSame(acl, result);
		verify(mockAclDAO).create(acl, ObjectType.ENTITY);
	}

	@Test
	public void testRestoreInheritanceProject(){
		when(mockNodeDao.getBenefactor(project.getId())).thenReturn(project.getId());
		when(mockEntityAuthorizationManager.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.touch(any(Long.class), anyString())).thenReturn(newEtag);
		when(mockNodeDao.getNodeTypeById(projectId)).thenReturn(EntityType.project);
		// call under test
		entityAclManager.restoreInheritance(projectId, mockUser);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(projectId, ObjectType.ENTITY_CONTAINER, ChangeType.UPDATE);
	}
	
	@Test
	public void testRestoreInheritanceFolder(){
		when(mockNodeDao.getBenefactor(folder.getId())).thenReturn(folder.getId());
		when(mockEntityAuthorizationManager.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.touch(any(Long.class), anyString())).thenReturn(newEtag);
		when(mockNodeDao.getNodeTypeById(folderId)).thenReturn(EntityType.folder);
		// call under test
		entityAclManager.restoreInheritance(folderId, mockUser);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(folderId, ObjectType.ENTITY_CONTAINER, ChangeType.UPDATE);
	}
	
	@Test
	public void testRestoreInheritanceFile(){
		when(mockNodeDao.getBenefactor(file.getId())).thenReturn(file.getId());
		when(mockEntityAuthorizationManager.hasAccess(any(), any(), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.touch(any(Long.class), anyString())).thenReturn(newEtag);
		when(mockNodeDao.getNodeTypeById(fileId)).thenReturn(EntityType.file);
		// call under test
		entityAclManager.restoreInheritance(fileId, mockUser);
		verifyNoMoreInteractions(mockTransactionalMessenger);
	}
	
}