package org.sagebionetworks.repo.manager.trash;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.AccessRequirementManager;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.NodeInheritanceManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserNotFoundException;
import org.sagebionetworks.repo.model.dao.TrashCanDao;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.lang.Collections;

import org.sagebionetworks.repo.model.Node;

public class TrashManagerImplTest {
	
	@Mock
	private AuthorizationManager mockAuthorizationManager;
	
	@Mock 
	private NodeManager mockNodeManager;
	
	@Mock 
	private NodeInheritanceManager mockNodeInheritanceManager;
	
//	@Mock 
//	private EntityPermissionsManager mockEntityPermissionsManager;
	
	@Mock 
	private NodeDAO mockNodeDAO;
	
	@Mock
	private AccessControlListDAO mockAclDAO;
	
	@Mock 
	private TrashCanDao mockTrashCanDao;
	
	@Mock
	private TransactionalMessenger mockTransactionalMessenger;

	@Mock
	private StackConfiguration stackConfig;
	
	private TrashManager trashManager;
	private long userID;
	private long adminUserID;
	private UserInfo userInfo;
	private UserInfo adminUserInfo;
	
	private String nodeID;
	private String nodeName;
	private String nodeParentID;
	private Node testNode;
	
	private final String child1ID = "syn124";
	private final String child2ID = "syn567";
	private List<String> childrenIDs;
	private List<String> emptyChildIDList;//no children for child1 and child2
	
	@Before
	public void setUp() throws Exception {
		trashManager = Mockito.spy(new TrashManagerImpl());
		MockitoAnnotations.initMocks(this);
		
		userID = 12345L;
		userInfo = new UserInfo(false /*not admin*/);
		userInfo.setId(userID);
		
		adminUserID = 67890L;
		adminUserInfo = new UserInfo(true);
		userInfo.setId(adminUserID);
		
		nodeID = "syn420";
		nodeName = "testName.test";
		nodeParentID = "syn489";
		testNode = new Node();
		testNode.setName(nodeName);
		testNode.setParentId(nodeParentID);
		
		childrenIDs = new ArrayList<String>();
		childrenIDs.add(child1ID);
		childrenIDs.add(child2ID);
		
		emptyChildIDList = new ArrayList<String>();
		
		ReflectionTestUtils.setField(trashManager, "nodeDao", mockNodeDAO);
		ReflectionTestUtils.setField(trashManager, "aclDAO", mockAclDAO);
		ReflectionTestUtils.setField(trashManager, "trashCanDao", mockTrashCanDao);
		ReflectionTestUtils.setField(trashManager, "authorizationManager", mockAuthorizationManager);
		ReflectionTestUtils.setField(trashManager, "nodeManager", mockNodeManager);
		ReflectionTestUtils.setField(trashManager, "transactionalMessenger", mockTransactionalMessenger);
		
		
		
		
		
		
		Mockito.when(mockNodeDAO.getNode(nodeID)).thenReturn(testNode);
		
		//mocking for getDescendants()
		Mockito.when(mockNodeDAO.getChildrenIdsAsList(nodeID))
		.thenReturn(childrenIDs);
		Mockito.when(mockNodeDAO.getChildrenIdsAsList(child1ID))
		.thenReturn(emptyChildIDList);
		Mockito.when(mockNodeDAO.getChildrenIdsAsList(child2ID))
		.thenReturn(emptyChildIDList);
	}

	@After
	public void tearDown() throws Exception {
		//TODO: IDK YET
	}
	
	
	///////////////////////
	//moveToTrash() Tests
	///////////////////////
	
	@Test (expected = IllegalArgumentException.class)
	public void testMoveToTrashWithNullUser() {
		trashManager.moveToTrash(null, nodeID);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testMoveToTrashWithNullNodeID() {
		trashManager.moveToTrash(userInfo, null);
	}
	
	@Test (expected = UnauthorizedException.class)
	public void testMoveToTrashNoAuthorization(){
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, nodeID, ObjectType.ENTITY, ACCESS_TYPE.DELETE))
		.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		trashManager.moveToTrash(userInfo, nodeID);
	} 
	
	@Test
	public void testMoveToTrashAuthourized(){
		//setup
		String child1Name = "Child1's fake name";
		String child2Name = "Child2's fake name";
		String child1Etag = "Child1's fake etag";
		String child2Etag = "Child2's fake etag";
		
		Mockito.when(mockAuthorizationManager.canAccess(userInfo, nodeID, ObjectType.ENTITY, ACCESS_TYPE.DELETE))
		.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		EntityHeader mockChild1EntityHeader = Mockito.mock(EntityHeader.class);
		EntityHeader mockChild2EntityHeader = Mockito.mock(EntityHeader.class);
		Mockito.when(mockNodeDAO.getEntityHeader(child1ID, null)).thenReturn(mockChild1EntityHeader);
		Mockito.when(mockChild1EntityHeader.getName()).thenReturn(child1Name);
		Mockito.when(mockNodeDAO.getEntityHeader(child2ID, null)).thenReturn(mockChild2EntityHeader);
		Mockito.when(mockChild2EntityHeader.getName()).thenReturn(child2Name);
		
		Mockito.when(mockNodeDAO.getParentId(child1ID)).thenReturn(nodeID);
		Mockito.when(mockNodeDAO.getParentId(child2ID)).thenReturn(nodeID);
		
		Mockito.when(mockNodeDAO.peekCurrentEtag(child1ID)).thenReturn(child1Etag);
		Mockito.when(mockNodeDAO.peekCurrentEtag(child2ID)).thenReturn(child2Etag);
		
		trashManager.moveToTrash(userInfo, nodeID);
		
		verify(mockNodeDAO,times(1)).getNode(nodeID);
		verify(mockNodeManager, times(1)).updateForTrashCan(userInfo, testNode, ChangeType.DELETE);
		verify(mockTrashCanDao, times(1)).create(userInfo.getId().toString(), nodeID, nodeName, nodeParentID);
		//verify(trashManager, times(1)).getDescendants(nodeID, Matchers.<Collection<String>>any());
		
		//verify for loop 
		//child1
		verify(mockNodeDAO,times(1)).getEntityHeader(child1ID, null);
		verify(mockChild1EntityHeader, times(1)).getName();
		verify(mockNodeDAO).getParentId(child1ID);
		verify(mockTrashCanDao).create(userInfo.getId().toString(), child1ID, child1Name, nodeID);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(child1ID, ObjectType.ENTITY, child1Etag, nodeID, ChangeType.DELETE);
		//child2
		verify(mockNodeDAO,times(1)).getEntityHeader(child2ID, null);
		verify(mockChild2EntityHeader, times(1)).getName();
		verify(mockNodeDAO).getParentId(child2ID);
		verify(mockTrashCanDao).create(userInfo.getId().toString(), child2ID, child2Name, nodeID);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(child2ID, ObjectType.ENTITY, child2Etag, nodeID, ChangeType.DELETE);
		

	}
	
	
	//////////////////////////////////////////////////
	// TODO: TESTS FOR OTHER METHODS COMMING SOON(TM)
	//////////////////////////////////////////////////
	
	
	//////////////////////////
	//getDescendants() Tests
	/////////////////////////
	@Test (expected = IllegalArgumentException.class)
	public void testGetDescendantsNullNodeID(){
		List<String> descendants = new ArrayList<String>();
		((TrashManagerImpl) trashManager).getDescendants(null, descendants);//TODO: also expose getDescendants to TrashManager???
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetDescendantsNullDescendantsCollection(){
		((TrashManagerImpl) trashManager).getDescendants(nodeID, null);
	}
	
	@Test
	public void testGetDescendantsNoDescendants(){
		List<String> descendants = new ArrayList<String>();
		
		Mockito.when(mockNodeDAO.getChildrenIdsAsList(nodeID))
		.thenReturn(new ArrayList<String>());
		
		((TrashManagerImpl) trashManager).getDescendants(nodeID,descendants);
		verify(mockNodeDAO,times(1)).getChildrenIdsAsList(any(String.class));
		assert(descendants.isEmpty());
	}
	
	@Test
	public void testGetDescendantsHaveDescendants(){
		
		
		List<String> descendants = new ArrayList<String>();
		
		trashManager.getDescendants(nodeID,descendants);
		
		
		verify(mockNodeDAO,times(1)).getChildrenIdsAsList(nodeID);
		verify(mockNodeDAO,times(1)).getChildrenIdsAsList(child1ID);
		verify(mockNodeDAO,times(1)).getChildrenIdsAsList(child2ID);
		
		verify(trashManager,times(3)).getDescendants(any(String.class), eq(descendants));
		assertTrue(descendants.containsAll(childrenIDs));
	}
	
	
	
	///////////////////////////////
	//purgeTrashAdmin() Test
	///////////////////////////////
	@Test (expected = IllegalArgumentException.class)
	public void testPurgeTrashAdminNullList(){
		trashManager.purgeTrashAdmin(null, adminUserInfo);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testPurgeTrashAdminNullUser(){
		trashManager.purgeTrashAdmin(new ArrayList<Long>(1), null);
	}
	
	@Test (expected = UnauthorizedException.class)
	public void testPurgeTrashAdminUserNotAdmin(){
		trashManager.purgeTrashAdmin(new ArrayList<Long>(1), userInfo);
	}
	
	@Test
	public void testPurgeTrashAdmin(){
		List<Long> trashIDList = new ArrayList<Long>(1);
		trashIDList.add(1L);
		trashManager.purgeTrashAdmin(trashIDList, adminUserInfo);
		
		verify(mockNodeDAO,times(1)).delete(trashIDList);
		verify(mockAclDAO,times(1)).delete(trashIDList, ObjectType.ENTITY);
		verify(mockTrashCanDao,times(1)).delete(trashIDList);
	}

	
	//////////////////////////
	// getTrashBefore() Tests
	//////////////////////////
	
	@Test
	public void testGetTrashBefore(){
		//setup
		Timestamp now = new Timestamp(System.currentTimeMillis());
		List<TrashedEntity> trashList = new ArrayList<TrashedEntity>();
		when(mockTrashCanDao.getTrashBefore(now)).thenReturn(trashList);
		
		//test
		assertEquals(trashManager.getTrashBefore(now), trashList);
		verify(mockTrashCanDao,times(1)).getTrashBefore(now);
	}
	
	
	/////////////////////////////////
	// getTrashLeavesBefore() Tests
	/////////////////////////////////
	@Test
	public void testGetTrashLeavesBefore(){
		final long daysBefore = 1;
		final long limit = 1234;
		List<Long> trashIdList = new ArrayList<Long>();
		when(mockTrashCanDao.getTrashLeavesBefore(daysBefore, limit)).thenReturn(trashIdList);
		
		assertEquals(trashManager.getTrashLeavesBefore(daysBefore, limit), trashIdList);
		verify(mockTrashCanDao, times(1)).getTrashLeavesBefore(daysBefore, limit);
	}

}
