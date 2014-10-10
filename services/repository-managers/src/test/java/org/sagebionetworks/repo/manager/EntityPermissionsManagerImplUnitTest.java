package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.springframework.test.util.ReflectionTestUtils;

public class EntityPermissionsManagerImplUnitTest {
	
	private EntityPermissionsManagerImpl entityPermissionsManager;
	private UserInfo userInfo;
	private static final String entityId = "syn123";
	
	private UserGroupDAO mockUserGroupDAO;
	private NodeDAO mockNodeDao;
	private AccessControlListDAO mockAclDAO;
	private AccessRequirementDAO  mockAccessRequirementDAO;
	private NodeInheritanceManager mockNodeInheritanceManager;
	private UserManager mockUserManager;
	private AuthenticationManager mockAuthenticationManager;


	@Before
	public void setUp() throws Exception {
		entityPermissionsManager = new EntityPermissionsManagerImpl();
		userInfo = new UserInfo(false);
		userInfo.setId(1234567L);
		userInfo.setGroups(Collections.singleton(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId()));
		mockUserGroupDAO = Mockito.mock(UserGroupDAO.class);
    	ReflectionTestUtils.setField(entityPermissionsManager, "userGroupDAO", mockUserGroupDAO);
    	mockNodeDao = Mockito.mock(NodeDAO.class);
    	ReflectionTestUtils.setField(entityPermissionsManager, "nodeDao", mockNodeDao);
    	mockAclDAO = Mockito.mock(AccessControlListDAO.class);
    	ReflectionTestUtils.setField(entityPermissionsManager, "aclDAO", mockAclDAO);
    	mockAccessRequirementDAO = Mockito.mock(AccessRequirementDAO.class);
    	ReflectionTestUtils.setField(entityPermissionsManager, "accessRequirementDAO", mockAccessRequirementDAO);
    	mockNodeInheritanceManager = Mockito.mock(NodeInheritanceManager.class);
    	ReflectionTestUtils.setField(entityPermissionsManager, "nodeInheritanceManager", mockNodeInheritanceManager);
    	mockUserManager = Mockito.mock(UserManager.class);
    	ReflectionTestUtils.setField(entityPermissionsManager, "userManager", mockUserManager);
    	mockAuthenticationManager = Mockito.mock(AuthenticationManager.class);
    	ReflectionTestUtils.setField(entityPermissionsManager, "authenticationManager", mockAuthenticationManager);
    	
    	when(mockAuthenticationManager.hasUserAcceptedTermsOfUse(userInfo.getId(), DomainType.SYNAPSE)).thenReturn(true);
    	Node node = new Node();
    	node.setId(entityId);
    	node.setCreatedByPrincipalId(111111L);
    	node.setNodeType("project");
    	when(mockNodeDao.getNode(entityId)).thenReturn(node);
    	
    	UserInfo anonymousUser = new UserInfo(false);
    	anonymousUser.setId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
    	when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId())).thenReturn(anonymousUser);
	}

	@Test
	public void testGetUserPermissionsForEntity() throws Exception {
		String benefactorId = "syn987";
		when(mockNodeInheritanceManager.getBenefactor(entityId)).thenReturn(benefactorId);
		when(mockNodeInheritanceManager.getBenefactor(benefactorId)).thenReturn(benefactorId);
		
		// we make the given user a fully authorized 'owner' of the entity
		when(mockAclDAO.canAccess(eq(userInfo.getGroups()), eq(benefactorId), eq(ObjectType.ENTITY), (ACCESS_TYPE)any())).
			thenReturn(true);
		
		// now let's apply an access requirement to "syn987" that does not apply to the benefactor
		Set<Long> principalIds = new HashSet<Long>();
		for (Long ug : userInfo.getGroups()) {
			principalIds.add(ug);
		}
		when(mockAccessRequirementDAO.unmetAccessRequirements(
				Collections.singletonList(entityId), RestrictableObjectType.ENTITY, principalIds, 
				Collections.singletonList(ACCESS_TYPE.DOWNLOAD))).thenReturn(Collections.singletonList(77777L));
		
		UserEntityPermissions uep = entityPermissionsManager.
				getUserPermissionsForEntity(userInfo, entityId);
		
		assertTrue(uep.getCanAddChild());
		assertTrue(uep.getCanChangePermissions());
		assertTrue(uep.getCanDelete());
		assertTrue(uep.getCanEdit());
		assertTrue(uep.getCanEnableInheritance());
		assertFalse(uep.getCanPublicRead());
		assertTrue(uep.getCanView());
		assertFalse(uep.getCanDownload());
		assertTrue(uep.getCanUpload());
	}

}
