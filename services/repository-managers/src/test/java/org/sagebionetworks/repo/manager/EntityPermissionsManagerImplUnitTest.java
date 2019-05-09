package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySetOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.util.ReflectionStaticTestUtils;

import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
	private AccessRequirementDAO  mockAccessRequirementDAO;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private AuthenticationManager mockAuthenticationManager;
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
	
	Set<Long> mockUsersGroups;
	Set<Long> nonvisibleIds;
	
	String entityId;
	Long userId;
	
	String newEtag;

	// here we set up a certified and a non-certified user, a project and a non-project Node
	@BeforeEach
	public void setUp() throws Exception {
		entityPermissionsManager = new EntityPermissionsManagerImpl();
		
		nonCertifiedUserInfo = new UserInfo(false);
		nonCertifiedUserInfo.setId(765432L);
		nonCertifiedUserInfo.setGroups(Collections.singleton(9999L));

		certifiedUserInfo = new UserInfo(false);
		certifiedUserInfo.setId(1234567L);
		certifiedUserInfo.setGroups(Collections.singleton(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId()));
		
		ReflectionStaticTestUtils.mockAutowire(this, entityPermissionsManager);
    	
    	when(mockStackConfiguration.getDisableCertifiedUser()).thenReturn(false);
    	
    	when(mockAuthenticationManager.hasUserAcceptedTermsOfUse(nonCertifiedUserInfo.getId())).thenReturn(true);
    	when(mockAuthenticationManager.hasUserAcceptedTermsOfUse(certifiedUserInfo.getId())).thenReturn(true);

    	userId = 111L;
    	
    	project = new Node();
    	project.setId(projectId);
    	project.setCreatedByPrincipalId(userId);
    	project.setNodeType(EntityType.project);
       	project.setParentId(projectParentId);
    	when(mockNodeDao.getNode(projectId)).thenReturn(project);
    	when(mockNodeDao.getNodeTypeById(projectId)).thenReturn(EntityType.project);
    	
    	folder = new Node();
    	folder.setId(folderId);
    	folder.setCreatedByPrincipalId(userId);
        folder.setParentId(folderParentId);
    	folder.setNodeType(EntityType.folder);
    	when(mockNodeDao.getNode(folderId)).thenReturn(folder);
    	when(mockNodeDao.getNodeTypeById(folderId)).thenReturn(EntityType.folder);
    	
    	file = new Node();
    	file.setId(fileId);
    	file.setCreatedByPrincipalId(userId);
    	file.setParentId(folderParentId);
    	file.setNodeType(EntityType.file);
    	when(mockNodeDao.getNode(fileId)).thenReturn(file);
    	when(mockNodeDao.getNodeTypeById(fileId)).thenReturn(EntityType.file);
    	
    	when(mockNodeDao.getBenefactor(anyString())).thenReturn(benefactorId);
   	
    	dockerRepo = new Node();
    	dockerRepo.setId(dockerRepoId);
    	dockerRepo.setCreatedByPrincipalId(userId);
    	dockerRepo.setParentId(folderParentId);
    	dockerRepo.setNodeType(EntityType.dockerrepo);
    	when(mockNodeDao.getNode(dockerRepoId)).thenReturn(dockerRepo);
    	when(mockNodeDao.getNodeTypeById(dockerRepoId)).thenReturn(EntityType.dockerrepo);
   	
    	UserInfo anonymousUser = new UserInfo(false);
    	anonymousUser.setId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
    	when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId())).thenReturn(anonymousUser);

		when(mockNodeDao.getBenefactor(anyString())).thenReturn(benefactorId);
		
		// we make the given user a fully authorized 'owner' of the entity
		when(mockAclDAO.canAccess(eq(certifiedUserInfo.getGroups()), eq(benefactorId), eq(ObjectType.ENTITY), (ACCESS_TYPE)any())).
		thenReturn(true);
		when(mockAclDAO.canAccess(eq(nonCertifiedUserInfo.getGroups()), eq(benefactorId), eq(ObjectType.ENTITY), (ACCESS_TYPE)any())).
		thenReturn(true);

		// now let's apply an access requirement to "syn987" that does not apply to the benefactor
		when(mockAccessRequirementDAO.getAllUnmetAccessRequirements(
				Collections.singletonList(projectId), RestrictableObjectType.ENTITY, certifiedUserInfo.getGroups(),
				Collections.singletonList(ACCESS_TYPE.DOWNLOAD))).thenReturn(Collections.singletonList(77777L));
		when(mockAccessRequirementDAO.getAllUnmetAccessRequirements(
				Collections.singletonList(projectId), RestrictableObjectType.ENTITY, nonCertifiedUserInfo.getGroups(), 
				Collections.singletonList(ACCESS_TYPE.DOWNLOAD))).thenReturn(Collections.singletonList(77777L));
		
		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.isAdmin()).thenReturn(false);
		mockUsersGroups = Sets.newHashSet(444L,555L);
		when(mockUser.getGroups()).thenReturn(mockUsersGroups);
		nonvisibleIds = Sets.newHashSet(888L,999L);
		when(mockAclDAO.getNonVisibleChilrenOfEntity(anySetOf(Long.class), anyString())).thenReturn(nonvisibleIds);
		
		entityId = "syn888";
		when(mockNodeDao.getBenefactor(entityId)).thenReturn(entityId);
		when(mockAclDAO.canAccess(mockUser.getGroups(), entityId, ObjectType.ENTITY, ACCESS_TYPE.CHANGE_PERMISSIONS)).
		thenReturn(true);
		
		newEtag = "newEtag";
		when(mockNodeDao.touch(any(Long.class), anyString())).thenReturn(newEtag);
	}

	@Test
	public void testGetUserPermissionsForCertifiedUserOnProject() throws Exception {
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
		
		assertTrue(entityPermissionsManager.canCreate(project.getParentId(), project.getNodeType(), certifiedUserInfo).isAuthorized());
		
		assertTrue(entityPermissionsManager.canCreateWiki(projectId, certifiedUserInfo).isAuthorized());
	}
	
	@Test
	public void testGetUserPermissionsForNonCertifiedUserOnProject() throws Exception {
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
		
		assertTrue(entityPermissionsManager.canCreate(project.getParentId(), project.getNodeType(), nonCertifiedUserInfo).isAuthorized());
		
		assertTrue(entityPermissionsManager.canCreateWiki(projectId, nonCertifiedUserInfo).isAuthorized());
	}

	@Test
	public void testGetUserPermissionsForCertifiedUserOnFolder() throws Exception {
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
		
		assertTrue(entityPermissionsManager.canCreate(folder.getParentId(), folder.getNodeType(), certifiedUserInfo).isAuthorized());
		
		assertTrue(entityPermissionsManager.canCreateWiki(folderId, certifiedUserInfo).isAuthorized());
	}
	
	@Test
	public void testReadButNotDownload() throws Exception {
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
		
		assertTrue(entityPermissionsManager.canCreate(folder.getParentId(), folder.getNodeType(), certifiedUserInfo).isAuthorized());
		
		assertTrue(entityPermissionsManager.canCreateWiki(folderId, certifiedUserInfo).isAuthorized());
	}
	
	@Test
	public void testGetUserPermissionsForNonCertifiedUserOnFolder() throws Exception {
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
		
		assertFalse(entityPermissionsManager.canCreate(folder.getParentId(), folder.getNodeType(), nonCertifiedUserInfo).isAuthorized());

		assertThrows(UserCertificationRequiredException.class,
				() -> entityPermissionsManager.canCreate(folder.getParentId(), folder.getNodeType(), nonCertifiedUserInfo).checkAuthorizationOrElseThrow()
		);


		assertFalse(entityPermissionsManager.canCreateWiki(folderId, nonCertifiedUserInfo).isAuthorized());
	}
	
	@Test
	public void testAnonymousCannotDownloadDockerRepo() throws Exception {
    	UserInfo anonymousUser = new UserInfo(false);
    	anonymousUser.setId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		assertFalse(entityPermissionsManager.
				hasAccess(dockerRepoId, ACCESS_TYPE.DOWNLOAD, anonymousUser).isAuthorized());
		
	}
	
	@Test
	public void testGetUserPermissionsForCertifiedUserOnDockerRepo() throws Exception {
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
		
	}

	@Test
	public void testGetNonvisibleChildrenNonAdmin(){
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
		verify(mockAclDAO, never()).getNonVisibleChilrenOfEntity(anySetOf(Long.class), anyString());
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
		String parentId = null;
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> entityPermissionsManager.getNonvisibleChildren(mockUser, parentId)
		);
	}
	
	@Test
	public void testUpdateAcl(){
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
		when(mockAclDAO.canAccess(anySetOf(Long.class), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class))).
		thenReturn(true);
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(projectId, mockUser, new Date());
		when(mockAclDAO.get(projectId, ObjectType.ENTITY)).thenReturn(acl);
		// call under test
		entityPermissionsManager.overrideInheritance(acl, mockUser);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(projectId, ObjectType.ENTITY_CONTAINER, newEtag, ChangeType.UPDATE);
	}
	
	@Test
	public void testOverrideInheritanceFolder(){
		when(mockAclDAO.canAccess(anySetOf(Long.class), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class))).
		thenReturn(true);
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(folderId, mockUser, new Date());
		when(mockAclDAO.get(folderId, ObjectType.ENTITY)).thenReturn(acl);
		// call under test
		entityPermissionsManager.overrideInheritance(acl, mockUser);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(folderId, ObjectType.ENTITY_CONTAINER, newEtag, ChangeType.UPDATE);
	}
	
	@Test
	public void testOverrideInheritanceFile(){
		when(mockAclDAO.canAccess(anySetOf(Long.class), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class))).
		thenReturn(true);
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(fileId, mockUser, new Date());
		when(mockAclDAO.get(fileId, ObjectType.ENTITY)).thenReturn(acl);
		// call under test
		entityPermissionsManager.overrideInheritance(acl, mockUser);
		// file should not trigger container message
		verifyNoMoreInteractions(mockTransactionalMessenger);	}
	
	@Test
	public void testRestoreInheritanceProject(){
		when(mockNodeDao.getBenefactor(project.getId())).thenReturn(project.getId());
		when(mockAclDAO.canAccess(anySetOf(Long.class), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class))).
		thenReturn(true);
		// call under test
		entityPermissionsManager.restoreInheritance(projectId, mockUser);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(projectId, ObjectType.ENTITY_CONTAINER, newEtag, ChangeType.UPDATE);
	}
	
	@Test
	public void testRestoreInheritanceFolder(){
		when(mockNodeDao.getBenefactor(folder.getId())).thenReturn(folder.getId());
		when(mockAclDAO.canAccess(anySetOf(Long.class), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class))).
		thenReturn(true);
		// call under test
		entityPermissionsManager.restoreInheritance(folderId, mockUser);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(folderId, ObjectType.ENTITY_CONTAINER, newEtag, ChangeType.UPDATE);
	}
	
	@Test
	public void testRestoreInheritanceFile(){
		when(mockNodeDao.getBenefactor(file.getId())).thenReturn(file.getId());
		when(mockAclDAO.canAccess(anySetOf(Long.class), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class))).
		thenReturn(true);
		// call under test
		entityPermissionsManager.restoreInheritance(fileId, mockUser);
		verifyNoMoreInteractions(mockTransactionalMessenger);
	}
}