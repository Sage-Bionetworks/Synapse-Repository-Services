package org.sagebionetworks.repo.model.bootstrap;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACL_SCHEME;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class EntityBootstrapperSemaphoreUnitTest {
	
	private NodeDAO mockNodeDao;
	private UserGroupDAO mockUserGroupDao;
	private UserProfileDAO mockUserProfileDao;
	private GroupMembersDAO mockGroupMembersDao;
	private AuthenticationDAO mockAuthenticationDao;
	private AccessControlListDAO mockAclDao;
	private CountingSemaphore mockSemaphoreDao;
	
	private List<EntityBootstrapData> bootstrapData;
	
	EntityBootstrapper bootstrapper;

	@Before
	public void before() throws Exception {
		mockNodeDao = Mockito.mock(NodeDAO.class);
		mockUserGroupDao = Mockito.mock(UserGroupDAO.class);
		mockUserProfileDao = Mockito.mock(UserProfileDAO.class);
		mockGroupMembersDao = Mockito.mock(GroupMembersDAO.class);
		mockAuthenticationDao = Mockito.mock(AuthenticationDAO.class);
		mockAclDao = Mockito.mock(AccessControlListDAO.class);
		mockSemaphoreDao = Mockito.mock(CountingSemaphore.class);
		// Inject
		bootstrapper = new EntityBootstrapperImpl();
		ReflectionTestUtils.setField(bootstrapper, "nodeDao", mockNodeDao);
		ReflectionTestUtils.setField(bootstrapper, "userGroupDAO", mockUserGroupDao);
		ReflectionTestUtils.setField(bootstrapper, "userProfileDAO", mockUserProfileDao);
		ReflectionTestUtils.setField(bootstrapper, "groupMembersDAO", mockGroupMembersDao);
		ReflectionTestUtils.setField(bootstrapper, "authDAO", mockAuthenticationDao);
		ReflectionTestUtils.setField(bootstrapper, "aclDAO", mockAclDao);
		ReflectionTestUtils.setField(bootstrapper, "semaphoreDao", mockSemaphoreDao);
		// Setup EntityBootstrapData
		bootstrapData = new ArrayList<EntityBootstrapData>();
		EntityBootstrapData ebd = new EntityBootstrapData();
		ebd.setEntityId(1001L);
		ebd.setEntityType(EntityType.folder);
		ebd.setEntityPath("/root");
		ebd.setDefaultChildAclScheme(ACL_SCHEME.INHERIT_FROM_PARENT);
		List<AccessBootstrapData> accessList = new ArrayList<AccessBootstrapData>();
		ebd.setAccessList(accessList);
		bootstrapData.add(ebd);
		ReflectionTestUtils.setField(bootstrapper, "bootstrapEntities", bootstrapData);
	}

	@After
	public void after() throws Exception {
	}

	@Test
	public void testBootsrapSemaphore() throws Exception {
		when(mockSemaphoreDao.attemptToAcquireLock("ENTITYBOOTSTRAPPERLOCK", 30L, 1)).thenReturn(null, null, null, "token");
		when(mockNodeDao.getNodeIdForPath(bootstrapData.get(0).getEntityPath())).thenReturn(null, bootstrapData.get(0).getEntityId().toString()); // Should force node creation
		when(mockNodeDao.createNew(any(Node.class))).thenReturn(bootstrapData.get(0).getEntityId().toString());
		bootstrapper.bootstrapAll();
		verify(mockSemaphoreDao, times(4)).attemptToAcquireLock("ENTITYBOOTSTRAPPERLOCK", 30L, 1);
		verify(mockUserGroupDao).bootstrapUsers();
		verify(mockUserProfileDao).bootstrapProfiles();
		verify(mockGroupMembersDao).bootstrapGroups();
		verify(mockAuthenticationDao).bootstrapCredentials();
		verify(mockNodeDao).createNew(any(Node.class));
		verify(mockAclDao).create(any(AccessControlList.class), any(ObjectType.class));
		verify(mockSemaphoreDao).releaseLock("ENTITYBOOTSTRAPPERLOCK", "token");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBootstrapSemaphoreException() throws Exception {
		when(mockSemaphoreDao.attemptToAcquireLock("ENTITYBOOTSTRAPPERLOCK", 30L, 1)).thenReturn(null, null, null, "token");
		when(mockNodeDao.getNodeIdForPath(bootstrapData.get(0).getEntityPath())).thenReturn(null, bootstrapData.get(0).getEntityId().toString()); // Should force node creation
		when(mockNodeDao.createNew(any(Node.class))).thenReturn(bootstrapData.get(0).getEntityId().toString());
		when(mockNodeDao.createNew(any(Node.class))).thenThrow(new IllegalArgumentException());
		try {
			bootstrapper.bootstrapAll();
		} finally {
			verify(mockSemaphoreDao, times(4)).attemptToAcquireLock("ENTITYBOOTSTRAPPERLOCK", 30L, 1);
			verify(mockUserGroupDao).bootstrapUsers();
			verify(mockUserProfileDao).bootstrapProfiles();
			verify(mockGroupMembersDao).bootstrapGroups();
			verify(mockAuthenticationDao).bootstrapCredentials();
			verify(mockSemaphoreDao).releaseLock("ENTITYBOOTSTRAPPERLOCK", "token");
		}
	}

}
