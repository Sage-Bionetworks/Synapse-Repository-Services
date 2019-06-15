package org.sagebionetworks.repo.manager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_PERMISSIONS;

import java.util.Collections;
import java.util.Date;
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
	private EntityAuthorizationManager mockEntityAuthorizationManager;
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
		when(mockEntityAuthorizationManager.hasAccess(eq(benefactorId), (ACCESS_TYPE)any(), eq(certifiedUserInfo))).thenReturn(AuthorizationStatus.authorized());
		when(mockEntityAuthorizationManager.hasAccess(eq(benefactorId), (ACCESS_TYPE)any(), eq(nonCertifiedUserInfo))).thenReturn(AuthorizationStatus.authorized());

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
		entityId = "syn888";
		when(mockNodeDao.getBenefactor(entityId)).thenReturn(entityId);
		when(mockEntityAuthorizationManager.hasAccess(entityId, CHANGE_PERMISSIONS, mockUser)).thenReturn(AuthorizationStatus.authorized());
		
		newEtag = "newEtag";
		when(mockNodeDao.touch(any(Long.class), anyString())).thenReturn(newEtag);
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
		when(mockEntityAuthorizationManager.hasAccess(benefactorId, CHANGE_PERMISSIONS, mockUser)).thenReturn(AuthorizationStatus.authorized());
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(projectId, mockUser, new Date());
		when(mockAclDAO.get(projectId, ObjectType.ENTITY)).thenReturn(acl);
		// call under test
		entityPermissionsManager.overrideInheritance(acl, mockUser);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(projectId, ObjectType.ENTITY_CONTAINER, newEtag, ChangeType.UPDATE);
	}
	
	@Test
	public void testOverrideInheritanceFolder(){
		when(mockEntityAuthorizationManager.hasAccess(benefactorId, CHANGE_PERMISSIONS, mockUser)).thenReturn(AuthorizationStatus.authorized());
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(folderId, mockUser, new Date());
		when(mockAclDAO.get(folderId, ObjectType.ENTITY)).thenReturn(acl);
		// call under test
		entityPermissionsManager.overrideInheritance(acl, mockUser);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(folderId, ObjectType.ENTITY_CONTAINER, newEtag, ChangeType.UPDATE);
	}
	
	@Test
	public void testOverrideInheritanceFile(){
		when(mockEntityAuthorizationManager.hasAccess(benefactorId, CHANGE_PERMISSIONS, mockUser)).thenReturn(AuthorizationStatus.authorized());
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(fileId, mockUser, new Date());
		when(mockAclDAO.get(fileId, ObjectType.ENTITY)).thenReturn(acl);
		// call under test
		entityPermissionsManager.overrideInheritance(acl, mockUser);
		// file should not trigger container message
		verifyNoMoreInteractions(mockTransactionalMessenger);	}
	
	@Test
	public void testRestoreInheritanceProject(){
		when(mockNodeDao.getBenefactor(project.getId())).thenReturn(project.getId());
		when(mockEntityAuthorizationManager.hasAccess(projectId, CHANGE_PERMISSIONS, mockUser)).thenReturn(AuthorizationStatus.authorized());
		// call under test
		entityPermissionsManager.restoreInheritance(projectId, mockUser);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(projectId, ObjectType.ENTITY_CONTAINER, newEtag, ChangeType.UPDATE);
	}
	
	@Test
	public void testRestoreInheritanceFolder(){
		when(mockNodeDao.getBenefactor(folder.getId())).thenReturn(folder.getId());
		when(mockEntityAuthorizationManager.hasAccess(folderId, CHANGE_PERMISSIONS, mockUser)).thenReturn(AuthorizationStatus.authorized());
		// call under test
		entityPermissionsManager.restoreInheritance(folderId, mockUser);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(folderId, ObjectType.ENTITY_CONTAINER, newEtag, ChangeType.UPDATE);
	}
	
	@Test
	public void testRestoreInheritanceFile(){
		when(mockNodeDao.getBenefactor(file.getId())).thenReturn(file.getId());
		when(mockEntityAuthorizationManager.hasAccess(fileId, CHANGE_PERMISSIONS, mockUser)).thenReturn(AuthorizationStatus.authorized());
		// call under test
		entityPermissionsManager.restoreInheritance(fileId, mockUser);
		verifyNoMoreInteractions(mockTransactionalMessenger);
	}
}