package org.sagebionetworks.repo.manager.trash;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.manager.NodeInheritanceManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.trash.TrashManager.PurgeCallback;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.TrashCanDao;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.web.NotFoundException;


import org.sagebionetworks.repo.model.Node;

public class TrashManagerImplTest {
	
	@Mock
	private AuthorizationManager mockAuthorizationManager;
	
	@Mock 
	private NodeManager mockNodeManager;
	
	@Mock 
	private NodeInheritanceManager mockNodeInheritanceManager;
	
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
	private TrashedEntity nodeTrashedEntity;
	
	private final String child1ID = "syn124";
	private final String child2ID = "syn567";
	private List<String> childrenIDs;
	private List<String> emptyChildIDList;//no children for child1 and child2
	private String child1Name;
	private String child2Name;
	private String child1Etag;
	private String child2Etag;
	
	private List<TrashedEntity> trashList;
	
	private PurgeCallback purgeCallback;
	
	@Before
	public void setUp() throws Exception {
		trashManager = spy(new TrashManagerImpl());
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
		nodeTrashedEntity = spy(new TrashedEntity());
		nodeTrashedEntity.setOriginalParentId(nodeParentID);
		nodeTrashedEntity.setEntityId(nodeID);
		nodeTrashedEntity.setEntityName(nodeName);
		nodeTrashedEntity.setDeletedOn(new Date(System.currentTimeMillis()));
		nodeTrashedEntity.setDeletedByPrincipalId(userInfo.getId().toString());
		
		childrenIDs = new ArrayList<String>();
		childrenIDs.add(child1ID);
		childrenIDs.add(child2ID);
		
		child1Name = "Child1's fake name";
		child2Name = "Child2's fake name";
		child1Etag = "Child1's fake etag";
		child2Etag = "Child2's fake etag";
		
		trashList = new ArrayList<TrashedEntity>();
		
		emptyChildIDList = new ArrayList<String>();
		
		purgeCallback = spy(new PurgeCallback(){

			@Override
			public void startPurge(String id) {
				System.out.println("startPurge() Called");
			}

			@Override
			public void endPurge() {
				System.out.println("endPurge() Called");
			}

			@Override
			public void startPurge(List<Long> ids) {
				System.out.println("startPurge(List) called");
			}
			
			
		});
		
		setField(trashManager, "nodeDao", mockNodeDAO);
		setField(trashManager, "aclDAO", mockAclDAO);
		setField(trashManager, "trashCanDao", mockTrashCanDao);
		setField(trashManager, "authorizationManager", mockAuthorizationManager);
		setField(trashManager, "nodeManager", mockNodeManager);
		setField(trashManager, "transactionalMessenger", mockTransactionalMessenger);
		
		
		
		
		when(mockNodeDAO.peekCurrentEtag(child1ID)).thenReturn(child1Etag);
		when(mockNodeDAO.peekCurrentEtag(child2ID)).thenReturn(child2Etag);
		
		when(mockNodeDAO.getParentId(child1ID)).thenReturn(nodeID);
		when(mockNodeDAO.getParentId(child2ID)).thenReturn(nodeID);
		
		when(mockTrashCanDao.getTrashedEntity(nodeID)).thenReturn(nodeTrashedEntity);
		when(mockNodeDAO.getNode(nodeID)).thenReturn(testNode);
		
		when(mockAuthorizationManager.canAccess(userInfo, nodeParentID, ObjectType.ENTITY, ACCESS_TYPE.CREATE))
		.thenReturn(new AuthorizationStatus(true, "DO IT! YES YOU CAN! JUST DO IT!"));
		when(mockAuthorizationManager.canUserMoveRestrictedEntity(userInfo, nodeTrashedEntity.getOriginalParentId(), nodeParentID))
		.thenReturn(new AuthorizationStatus(true, "YESTERDAY YOU SAID TOMORROW, SO JUST DO IT!"));
		
		//mocking for getDescendants()
		when(mockNodeDAO.getChildrenIdsAsList(nodeID))
		.thenReturn(childrenIDs);
		when(mockNodeDAO.getChildrenIdsAsList(child1ID))
		.thenReturn(emptyChildIDList);
		when(mockNodeDAO.getChildrenIdsAsList(child2ID))
		.thenReturn(emptyChildIDList);
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
		when(mockAuthorizationManager.canAccess(userInfo, nodeID, ObjectType.ENTITY, ACCESS_TYPE.DELETE))
		.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		trashManager.moveToTrash(userInfo, nodeID);
	} 
	
	@Test
	public void testMoveToTrashAuthourized(){
		//setup
		
		
		when(mockAuthorizationManager.canAccess(userInfo, nodeID, ObjectType.ENTITY, ACCESS_TYPE.DELETE))
		.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		EntityHeader mockChild1EntityHeader = mock(EntityHeader.class);
		EntityHeader mockChild2EntityHeader = mock(EntityHeader.class);
		when(mockNodeDAO.getEntityHeader(child1ID, null)).thenReturn(mockChild1EntityHeader);
		when(mockChild1EntityHeader.getName()).thenReturn(child1Name);
		when(mockNodeDAO.getEntityHeader(child2ID, null)).thenReturn(mockChild2EntityHeader);
		when(mockChild2EntityHeader.getName()).thenReturn(child2Name);
		
		trashManager.moveToTrash(userInfo, nodeID);
		
		verify(mockNodeDAO,times(1)).getNode(nodeID);
		verify(mockNodeManager, times(1)).updateForTrashCan(userInfo, testNode, ChangeType.DELETE);
		verify(mockTrashCanDao, times(1)).create(userInfo.getId().toString(), nodeID, nodeName, nodeParentID);
		verify(trashManager).getDescendants( eq(nodeID), Matchers.<Collection<String>>any() );
		
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
	
	///////////////////////////
	//restoreFromTrash() Tests
	///////////////////////////
	@Test(expected = IllegalArgumentException.class)
	public void testRestoreFromTrashNullUser(){
		trashManager.restoreFromTrash(null, nodeID, nodeParentID);//newParent (3rd one) is an optional parameter 
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testRestoreFromTrashNullNodeID(){
		trashManager.restoreFromTrash(userInfo, null, nodeParentID);
	}
	
	@Test (expected = NotFoundException.class)
	public void testRestoreFromTrashBadNodeID(){
		final String badNodeID = "synFAKEID";
		when(mockTrashCanDao.getTrashedEntity(badNodeID)).thenReturn(null);
		trashManager.restoreFromTrash(userInfo, badNodeID, nodeParentID);
	}
	
	@Test
	public void testRestoreFromTrashNotAdminAndNotDeletedByUser(){
		final String fakeDeletedByID = "synDEFINITELYNOTTHISUSER";
		nodeTrashedEntity.setDeletedByPrincipalId(fakeDeletedByID);
		try{
			trashManager.restoreFromTrash(userInfo, nodeID, nodeParentID);
			fail("expected UnauthorizedException");
		}catch (UnauthorizedException e){
			//expected
			verify(mockTrashCanDao).getTrashedEntity(nodeID);
			verify(mockAuthorizationManager, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class));
			verify(mockAuthorizationManager, never()).canUserMoveRestrictedEntity(any(UserInfo.class), anyString(), anyString());
		}
	}
	
	@Test (expected = ParentInTrashCanException.class)
	public void testRestoreFromTrashParentIDInTrash(){
		final String fakeNewParentID = "synFAKEPARENTID";
		when(mockTrashCanDao.getTrashedEntity(fakeNewParentID)).thenReturn(new TrashedEntity());
		trashManager.restoreFromTrash(userInfo, nodeID, fakeNewParentID);
	}
	
	@Test 
	public void testRestoreFromTrashUnauthourizedNewParent(){
		when(mockAuthorizationManager.canAccess(userInfo, nodeParentID, ObjectType.ENTITY, ACCESS_TYPE.CREATE))
		.thenReturn(new AuthorizationStatus(false, "I'm a teapot."));
		try{
			trashManager.restoreFromTrash(userInfo, nodeID, nodeParentID);
			fail("expected UnauthorizedException");
		}catch (UnauthorizedException e){
			//expected
			verify(mockTrashCanDao).getTrashedEntity(nodeID);
			verify(mockAuthorizationManager).canAccess(userInfo, nodeParentID, ObjectType.ENTITY, ACCESS_TYPE.CREATE);
			verify(mockAuthorizationManager, never()).canUserMoveRestrictedEntity(any(UserInfo.class), anyString(), anyString());
		}
		
	}
	
	@Test
	public void testRestoreFromTrashUnauthourizedNodeID(){
		when(mockAuthorizationManager.canUserMoveRestrictedEntity(userInfo, nodeTrashedEntity.getOriginalParentId(), nodeParentID))
		.thenReturn(new AuthorizationStatus(false, "U can't touch this."));
		try{
			trashManager.restoreFromTrash(userInfo, nodeID, nodeParentID);
		}catch (UnauthorizedException e){
			//expected
			verify(mockTrashCanDao).getTrashedEntity(nodeID);
			verify(mockAuthorizationManager).canAccess(userInfo, nodeParentID, ObjectType.ENTITY, ACCESS_TYPE.CREATE);
			verify(mockAuthorizationManager).canUserMoveRestrictedEntity(userInfo, nodeTrashedEntity.getOriginalParentId(), nodeParentID);
		}
	}
	
	@Test 
	public void testRestoreFromTrashCan(){
		trashManager.restoreFromTrash(userInfo, nodeID, nodeParentID);
		final String deletedBy = nodeTrashedEntity.getDeletedByPrincipalId();
		verify(mockNodeManager).updateForTrashCan(userInfo, testNode, ChangeType.CREATE);
		verify(mockTrashCanDao).delete(deletedBy ,nodeID);
		verify(trashManager).getDescendants( eq(nodeID), Matchers.<Collection<String>>any() );
		
		verify(mockTrashCanDao).delete(deletedBy, child1ID);
		verify(mockNodeDAO).getParentId(child1ID);
		verify(mockNodeDAO).peekCurrentEtag(child1ID);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(child1ID, ObjectType.ENTITY, child1Etag, nodeID, ChangeType.CREATE);
		
		verify(mockTrashCanDao).delete(deletedBy, child2ID);
		verify(mockNodeDAO).getParentId(child2ID);
		verify(mockNodeDAO).peekCurrentEtag(child2ID);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(child2ID, ObjectType.ENTITY, child2Etag, nodeID, ChangeType.CREATE);
	}
	
	///////////////////////
	//viewTrashForUser()
	//////////////////////
	@Test (expected = IllegalArgumentException.class)
	public void testViewTrashForUserNullCurrentUser(){
		trashManager.viewTrashForUser(null, userInfo, 1, 1);
	}
	@Test (expected = IllegalArgumentException.class)
	public void testViewTrashForUserNullOtherUser(){
		trashManager.viewTrashForUser(userInfo,null, 1, 1);
	}
	@Test (expected = IllegalArgumentException.class)
	public void testViewTrashForUserNegativeOffset(){
		trashManager.viewTrashForUser(userInfo, userInfo, -1 , 1);
	}
	@Test (expected = IllegalArgumentException.class)
	public void testViewTrashForUserNegativeLimit(){
		trashManager.viewTrashForUser(userInfo, userInfo, 1, -1);
	}
	
	@Test (expected = UnauthorizedException.class)
	public void testViewTrashForUserWhenCurrentUserIsNotAdminAndDifferentOtherUser(){
		final long tempUserID = 1234567890L;
		UserInfo tempUser = new UserInfo(false);
		tempUser.setId(tempUserID);
		trashManager.viewTrashForUser(userInfo, tempUser, 1, 1);
	}
	
	@Test
	public void testViewTrashForUser(){
		final long limit = 1;
		final long offset = 0;
		when(mockTrashCanDao.getInRangeForUser(userInfo.getId().toString(), false, offset , limit))
		.thenReturn(trashList);
		List<TrashedEntity> results = trashManager.viewTrashForUser(userInfo, userInfo, offset , limit);
		assertEquals(trashList, results);
	}
	///////////////////////
	//viewTrash()
	//////////////////////
	@Test (expected = IllegalArgumentException.class)
	public void testViewTrashNullCurrentUser(){
		trashManager.viewTrash(null, 1, 1);
	}
	@Test (expected = IllegalArgumentException.class)
	public void testViewTrashNegativeOffset(){
		trashManager.viewTrash(adminUserInfo, -1 , 1);
	}
	@Test (expected = IllegalArgumentException.class)
	public void testViewTrashNegativeLimit(){
		trashManager.viewTrash(adminUserInfo, 1, -1);
	}	
	@Test (expected = UnauthorizedException.class)
	public void testViewTrashCurrentUserNotAdmin(){
		trashManager.viewTrash(userInfo, 0, 1);
	}
	@Test
	public void testViewTrash(){
		final long limit = 1;
		final long offset = 0;
		
		when(mockTrashCanDao.getInRange(false, offset , limit))
		.thenReturn(trashList);
		List<TrashedEntity> results = trashManager.viewTrash(adminUserInfo, offset , limit);
		assertEquals(trashList, results);
	}
	
	////////////////////////////
	//purgeTrashForUser(User, NodeID) Tests
	/////////////////////////////
	
	@Test (expected = IllegalArgumentException.class)
	public void testPurgeTrashForUserNullCurrentUser(){
		trashManager.purgeTrashForUser(null, nodeID, purgeCallback);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testPurgeTrashForUserNullNodeId(){
		trashManager.purgeTrashForUser(userInfo, null, purgeCallback);
	}
	
	@Test (expected = NotFoundException.class)
	public void testPurgeTrashForUserNotDeletedByUser(){
		when(mockTrashCanDao.exists(userInfo.getId().toString(), nodeID))
		.thenReturn(false);
		trashManager.purgeTrashForUser(userInfo, nodeID, purgeCallback);
	}
	
	@Test 
	public void testPurgeTrashForUser(){
		when(mockTrashCanDao.exists(userInfo.getId().toString(), nodeID))
		.thenReturn(true);
		trashManager.purgeTrashForUser(userInfo, nodeID, purgeCallback);
		
		verify(trashManager).getDescendants( eq(nodeID), Matchers.<Collection<String>>any() );
		verify(mockNodeDAO).delete(nodeID);
		verify(mockAclDAO).delete(nodeID, ObjectType.ENTITY);
		verify(mockTrashCanDao).delete(userInfo.getId().toString(), nodeID);
		
		verify(mockTrashCanDao).delete(userInfo.getId().toString(), child1ID);
		verify(mockTrashCanDao).delete(userInfo.getId().toString(), child2ID);
		
		//not very important but might as well check
		verify(purgeCallback, times(3)).startPurge(any(String.class));
		verify(purgeCallback, times(3)).startPurge(any(String.class));
	}
	
	////////////////////////////////
	//purgeTrashForUser(User) Tests
	////////////////////////////////
	@Test (expected = IllegalArgumentException.class)
	public void testPurgeAllTrashForUserNullCurrentUser(){
		trashManager.purgeTrashForUser(null, purgeCallback);
	}
	
	@Test
	public void testPurgeAllTrashForUser(){
		
		when(mockTrashCanDao.getInRangeForUser(userInfo.getId().toString(), true, 0, Long.MAX_VALUE))
		.thenReturn(trashList);
		trashManager.purgeTrashForUser(userInfo, purgeCallback);
		verify(mockTrashCanDao).getInRangeForUser(userInfo.getId().toString(), true, 0, Long.MAX_VALUE);
		verify(trashManager).purgeTrash(trashList, purgeCallback);
		
	}
	
	/////////////////////////
	//purgeTrash(User) Tests
	/////////////////////////
	@Test (expected = IllegalArgumentException.class)
	public void testPurgeAllTrashNullCurrentUser(){
		trashManager.purgeTrash((UserInfo) null, purgeCallback);
	}
	@Test (expected = UnauthorizedException.class)
	public void testPurgeAllTrashNonAdmin(){
		trashManager.purgeTrash(userInfo, purgeCallback);
	}
	@Test
	public void testPurgeAllTrash(){
		when(mockTrashCanDao.getInRange(true, 0, Long.MAX_VALUE))
		.thenReturn(trashList);
		trashManager.purgeTrash(adminUserInfo, purgeCallback);
		verify(trashManager).purgeTrash(trashList, purgeCallback);
	}
	
	//////////////////////////////////////////////////
	// TODO: TESTS FOR 
	// purgeTrash(List<TrashedEntity> trashList, PurgeCallback purgeCallback)
	// But it might get replaced very soon by purgeTrashAdmin() so skip for now.
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
		
		when(mockNodeDAO.getChildrenIdsAsList(nodeID))
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
		when(mockTrashCanDao.getTrashLeaves(daysBefore, limit)).thenReturn(trashIdList);
		
		assertEquals(trashManager.getTrashLeavesBefore(daysBefore, limit), trashIdList);
		verify(mockTrashCanDao, times(1)).getTrashLeaves(daysBefore, limit);
	}

}
