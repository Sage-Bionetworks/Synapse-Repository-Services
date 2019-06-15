package org.sagebionetworks.repo.manager.trash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.trash.TrashManager.PurgeCallback;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.TrashCanDao;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.common.collect.Lists;

public class TrashManagerImplTest {
	
	@Mock
	private AuthorizationManager mockAuthorizationManager;
	
	@Mock 
	private NodeManager mockNodeManager;
	
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

	@InjectMocks
	private TrashManagerImpl trashManager;
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
	private String newEtag;
	
	private List<TrashedEntity> trashList;
	
	private PurgeCallback purgeCallback;
	
	@Before
	public void setUp() throws Exception {
		trashManager = new TrashManagerImpl();
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
		testNode.setId(nodeID);
		testNode.setName(nodeName);
		testNode.setParentId(nodeParentID);
		testNode.setNodeType(EntityType.file);
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

		
		when(mockNodeDAO.peekCurrentEtag(child1ID)).thenReturn(child1Etag);
		when(mockNodeDAO.peekCurrentEtag(child2ID)).thenReturn(child2Etag);
		
		when(mockNodeDAO.getParentId(child1ID)).thenReturn(nodeID);
		when(mockNodeDAO.getParentId(child2ID)).thenReturn(nodeID);
		
		when(mockTrashCanDao.getTrashedEntity(nodeID)).thenReturn(nodeTrashedEntity);
		when(mockNodeDAO.getNode(nodeID)).thenReturn(testNode);
		
		when(mockAuthorizationManager.canAccess(any(UserInfo.class), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class)))
		.thenReturn(AuthorizationStatus.authorized());
		when(mockAuthorizationManager.canUserMoveRestrictedEntity(any(UserInfo.class), anyString(), anyString()))
		.thenReturn(AuthorizationStatus.authorized());
		
		//mocking for getDescendants()
		when(mockNodeDAO.getChildrenIdsAsList(nodeID))
		.thenReturn(childrenIDs);
		when(mockNodeDAO.getChildrenIdsAsList(child1ID))
		.thenReturn(emptyChildIDList);
		when(mockNodeDAO.getChildrenIdsAsList(child2ID))
		.thenReturn(emptyChildIDList);
		
		when(mockNodeDAO.isNodeAvailable(anyString())).thenReturn(true);
		newEtag = "newEtag";
		when(mockNodeDAO.touch(any(Long.class), anyString(), any(ChangeType.class))).thenReturn(newEtag);
	}
	
	@Test
	public void testUpdateNodeForTrashCanNonContainer(){
		testNode.setNodeType(EntityType.file);
		testNode.setModifiedOn(new Date(0));
		testNode.setModifiedByPrincipalId(-1L);
		ArgumentCaptor<Node> nodeCapture = ArgumentCaptor.forClass(Node.class);
		ChangeType changeType = ChangeType.DELETE;
		// call under test
		trashManager.updateNodeForTrashCan(userInfo, testNode, changeType);
		verify(mockNodeDAO).touch(userInfo.getId(), testNode.getId(), changeType);
		// the entity is not a container so a message should not be sent.
		verify(mockTransactionalMessenger, never()).sendMessageAfterCommit(anyString(), any(ObjectType.class), anyString(), any(ChangeType.class));
		verify(mockNodeDAO).updateNode(nodeCapture.capture());
		Node capturedNode = nodeCapture.getValue();
		assertNotNull(capturedNode);
		assertNotNull(capturedNode.getModifiedOn());
	}
	
	@Test
	public void testUpdateNodeForTrashCanContainer(){
		testNode.setNodeType(EntityType.folder);
		testNode.setModifiedOn(new Date(0));
		testNode.setModifiedByPrincipalId(-1L);
		ChangeType changeType = ChangeType.DELETE;
		// call under test
		trashManager.updateNodeForTrashCan(userInfo, testNode, changeType);
		verify(mockNodeDAO).touch(userInfo.getId(), testNode.getId(), changeType);
		// the entity is a container so a container event should be generated.
		verify(mockTransactionalMessenger).sendMessageAfterCommit(testNode.getId(), ObjectType.ENTITY_CONTAINER, newEtag, changeType);
	}
	
	@Test
	public void testDeleteAllAclsInHierarchy() throws LimitExceededException{
		List<Long> parentIdsList = Lists.newArrayList(123L,456L);
		Set<Long> parentIds = new LinkedHashSet<Long>(parentIdsList);
		when(mockNodeDAO.getAllContainerIds(nodeID, TrashManagerImpl.MAX_IDS_TO_LOAD)).thenReturn(parentIds);
		List<Long> childernWithAcls = Lists.newArrayList(456L, 444L);
		when(mockAclDAO.getChildrenEntitiesWithAcls(parentIdsList)).thenReturn(childernWithAcls);
		// call under test
		trashManager.deleteAllAclsInHierarchy(nodeID);
		// delete the acl of the node
		verify(mockAclDAO).delete(nodeID, ObjectType.ENTITY);
		// delete all acls for the hierarchy.
		verify(mockAclDAO).delete(childernWithAcls, ObjectType.ENTITY);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDeleteAllAclsInHierarchyOverLimit() throws LimitExceededException{
		// setup limit exceeded.
		LimitExceededException exception = new LimitExceededException("too many");
		doThrow(exception).when(mockNodeDAO).getAllContainerIds(anyString(), anyInt());
		// call under test
		trashManager.deleteAllAclsInHierarchy(nodeID);
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
		.thenReturn(AuthorizationStatus.accessDenied(""));
		trashManager.moveToTrash(userInfo, nodeID);
	} 
	
	@Test
	public void testMoveToTrashAuthourized(){
		//setup
		when(mockAuthorizationManager.canAccess(userInfo, nodeID, ObjectType.ENTITY, ACCESS_TYPE.DELETE))
		.thenReturn(AuthorizationStatus.authorized());
		EntityHeader mockChild1EntityHeader = mock(EntityHeader.class);
		EntityHeader mockChild2EntityHeader = mock(EntityHeader.class);
		when(mockNodeDAO.getEntityHeader(child1ID, null)).thenReturn(mockChild1EntityHeader);
		when(mockChild1EntityHeader.getName()).thenReturn(child1Name);
		when(mockNodeDAO.getEntityHeader(child2ID, null)).thenReturn(mockChild2EntityHeader);
		when(mockChild2EntityHeader.getName()).thenReturn(child2Name);
		
		trashManager.moveToTrash(userInfo, nodeID);
		
		verify(mockNodeDAO,times(1)).getNode(nodeID);
		verify(mockNodeDAO, times(1)).updateNode(testNode);
		verify(mockTrashCanDao, times(1)).create(userInfo.getId().toString(), nodeID, nodeName, nodeParentID);
		
		verify(mockAclDAO).delete(anyListOf(Long.class), any(ObjectType.class));
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
		when(mockNodeDAO.isNodeAvailable(fakeNewParentID)).thenReturn(false);
		trashManager.restoreFromTrash(userInfo, nodeID, fakeNewParentID);
	}
	
	@Test 
	public void testRestoreFromTrashUnauthourizedNewParent(){
		when(mockAuthorizationManager.canAccess(userInfo, nodeParentID, ObjectType.ENTITY, ACCESS_TYPE.CREATE))
		.thenReturn(AuthorizationStatus.accessDenied("I'm a teapot."));
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
		.thenReturn(AuthorizationStatus.accessDenied("U can't touch this."));
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
		verify(mockNodeDAO, times(1)).updateNode(testNode);
		verify(mockTrashCanDao).delete(deletedBy ,nodeID);
		verify(mockAclDAO, never()).create(any(AccessControlList.class), any(ObjectType.class));
	}
	
	/**
	 * Test restoring a project to root.
	 * 
	 */
	@Test 
	public void testRestoreFromTrashProjectNewParentRoot(){
		testNode.setNodeType(EntityType.project);
		// move the entity to root.
		nodeParentID = NodeUtils.ROOT_ENTITY_ID;
		// call under test
		trashManager.restoreFromTrash(userInfo, nodeID, nodeParentID);
		final String deletedBy = nodeTrashedEntity.getDeletedByPrincipalId();
		verify(mockNodeDAO, times(1)).updateNode(testNode);
		verify(mockTrashCanDao).delete(deletedBy ,nodeID);
		// An ACL should be created for the project
		verify(mockAclDAO).create(any(AccessControlList.class), eq(ObjectType.ENTITY));
	}
	
	/**
	 * Test restoring a non-project to root.
	 * 
	 */
	@Test (expected=IllegalArgumentException.class)
	public void testRestoreFromTrashNonProjectNewParentRoot(){
		testNode.setNodeType(EntityType.folder);
		// move the entity to root.
		nodeParentID = NodeUtils.ROOT_ENTITY_ID;
		// call under test
		trashManager.restoreFromTrash(userInfo, nodeID, nodeParentID);
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
	
		verify(mockNodeDAO).delete(nodeID);
		verify(mockAclDAO).delete(nodeID, ObjectType.ENTITY);
		verify(mockTrashCanDao).delete(userInfo.getId().toString(), nodeID);
				
		//not very important but might as well check
		verify(purgeCallback).startPurge(any(String.class));
		verify(purgeCallback).endPurge();
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
		verify(mockTrashCanDao).getInRange(true, 0, Long.MAX_VALUE);
	}
	
	//////////////////////////////////////////////////
	// TODO: TESTS FOR 
	// purgeTrash(List<TrashedEntity> trashList, PurgeCallback purgeCallback)
	// But it might get replaced very soon by purgeTrashAdmin() so skip for now.
	//////////////////////////////////////////////////
	
	
	
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
	
	@Test
	public void testMoveToTrashAlreadyTrashed(){
		// not available means deleted or does not exist
		when(mockNodeDAO.isNodeAvailable(nodeID)).thenReturn(false);
		// call under test
		trashManager.moveToTrash(userInfo, nodeID);
		verify(mockNodeDAO).isNodeAvailable(nodeID);
		verifyNoMoreInteractions(mockAuthorizationManager);
	}

}
