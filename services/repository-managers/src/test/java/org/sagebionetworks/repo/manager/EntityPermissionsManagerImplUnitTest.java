package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.util.ReflectionStaticTestUtils;

public class EntityPermissionsManagerImplUnitTest {
	
	private EntityPermissionsManagerImpl entityPermissionsManager;
	private UserInfo nonCertifiedUserInfo;
	private UserInfo certifiedUserInfo;
	private static final String projectId = "syn123";
	private static final String folderId = "syn456";
	private static final String projectParentId = "syn000";
	private static final String folderParentId = "syn999";
	private Node project;
	private Node folder;
	
	@Mock
	private UserGroupDAO mockUserGroupDAO;
	@Mock
	private NodeDAO mockNodeDao;
	@Mock
	private AccessControlListDAO mockAclDAO;
	@Mock
	private AccessRequirementDAO  mockAccessRequirementDAO;
	@Mock
	private NodeInheritanceManager mockNodeInheritanceManager;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private AuthenticationManager mockAuthenticationManager;
	@Mock
	private StackConfiguration mockStackConfiguration;
	@Mock
	private ProjectSettingsManager mockProjectSettingsManager;


	// here we set up a certified and a non-certified user, a project and a non-project Node
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		entityPermissionsManager = new EntityPermissionsManagerImpl();
		
		nonCertifiedUserInfo = new UserInfo(false);
		nonCertifiedUserInfo.setId(765432L);
		nonCertifiedUserInfo.setGroups(Collections.singleton(9999L));

		certifiedUserInfo = new UserInfo(false);
		certifiedUserInfo.setId(1234567L);
		certifiedUserInfo.setGroups(Collections.singleton(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId()));
		
		ReflectionStaticTestUtils.mockAutowire(this, entityPermissionsManager);
    	
    	when(mockStackConfiguration.getDisableCertifiedUser()).thenReturn(false);
    	
    	when(mockAuthenticationManager.hasUserAcceptedTermsOfUse(nonCertifiedUserInfo.getId(), DomainType.SYNAPSE)).thenReturn(true);
    	when(mockAuthenticationManager.hasUserAcceptedTermsOfUse(certifiedUserInfo.getId(), DomainType.SYNAPSE)).thenReturn(true);

    	project = new Node();
    	project.setId(projectId);
    	project.setCreatedByPrincipalId(111111L);
    	project.setNodeType(EntityType.project);
       	project.setParentId(projectParentId);
    	when(mockNodeDao.getNode(projectId)).thenReturn(project);
    	when(mockNodeDao.getNodeTypeById(projectId)).thenReturn(EntityType.project);
    	
    	folder = new Node();
    	folder.setId(folderId);
    	folder.setCreatedByPrincipalId(111111L);
        folder.setParentId(folderParentId);
    	folder.setNodeType(EntityType.folder);
    	when(mockNodeDao.getNode(folderId)).thenReturn(folder);
    	when(mockNodeDao.getNodeTypeById(folderId)).thenReturn(EntityType.folder);
   	
    	UserInfo anonymousUser = new UserInfo(false);
    	anonymousUser.setId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
    	when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId())).thenReturn(anonymousUser);

    	String benefactorId = "syn987";
		when(mockNodeInheritanceManager.getBenefactor(projectId)).thenReturn(benefactorId);
		when(mockNodeInheritanceManager.getBenefactor(folderId)).thenReturn(benefactorId);
		when(mockNodeInheritanceManager.getBenefactor(projectParentId)).thenReturn(benefactorId);
		when(mockNodeInheritanceManager.getBenefactor(folderParentId)).thenReturn(benefactorId);
		when(mockNodeInheritanceManager.getBenefactor(benefactorId)).thenReturn(benefactorId);
		
		// we make the given user a fully authorized 'owner' of the entity
		when(mockAclDAO.canAccess(eq(certifiedUserInfo.getGroups()), eq(benefactorId), eq(ObjectType.ENTITY), (ACCESS_TYPE)any())).
		thenReturn(true);
		when(mockAclDAO.canAccess(eq(nonCertifiedUserInfo.getGroups()), eq(benefactorId), eq(ObjectType.ENTITY), (ACCESS_TYPE)any())).
		thenReturn(true);
		
		// now let's apply an access requirement to "syn987" that does not apply to the benefactor
		when(mockAccessRequirementDAO.unmetAccessRequirements(
				Collections.singletonList(projectId), RestrictableObjectType.ENTITY, certifiedUserInfo.getGroups(), 
				Collections.singletonList(ACCESS_TYPE.DOWNLOAD))).thenReturn(Collections.singletonList(77777L));
		when(mockAccessRequirementDAO.unmetAccessRequirements(
				Collections.singletonList(projectId), RestrictableObjectType.ENTITY, nonCertifiedUserInfo.getGroups(), 
				Collections.singletonList(ACCESS_TYPE.DOWNLOAD))).thenReturn(Collections.singletonList(77777L));
		
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
		
		assertTrue(entityPermissionsManager.canCreate(project, certifiedUserInfo).getAuthorized());
		
		assertTrue(entityPermissionsManager.canCreateWiki(projectId, certifiedUserInfo).getAuthorized());
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
		
		assertTrue(entityPermissionsManager.canCreate(project, nonCertifiedUserInfo).getAuthorized());
		
		assertTrue(entityPermissionsManager.canCreateWiki(projectId, nonCertifiedUserInfo).getAuthorized());
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
		
		assertTrue(entityPermissionsManager.canCreate(folder, certifiedUserInfo).getAuthorized());
		
		assertTrue(entityPermissionsManager.canCreateWiki(folderId, certifiedUserInfo).getAuthorized());
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
		
		assertFalse(entityPermissionsManager.canCreate(folder, nonCertifiedUserInfo).getAuthorized());
		
		assertFalse(entityPermissionsManager.canCreateWiki(folderId, nonCertifiedUserInfo).getAuthorized());
	}

}










