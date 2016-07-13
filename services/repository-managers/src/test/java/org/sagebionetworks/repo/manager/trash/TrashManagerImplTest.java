package org.sagebionetworks.repo.manager.trash;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.TrashCanDao;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
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
	private TransactionalMessenger transactionalMessenger;

	@Mock
	private StackConfiguration stackConfig;
	
	private TrashManager trashManager;
	private long userID;
	private UserInfo userInfo;
	
	private String nodeID;
	private String nodeName;
	private String nodeParentID;
	private Node testNode;
	
	@Before
	public void setUp() throws Exception {
		trashManager = new TrashManagerImpl();
		MockitoAnnotations.initMocks(this);
		
		userID = 12345L;
		userInfo = new UserInfo(false /*not admin*/);
		userInfo.setId(userID);
		
		nodeID = "syn420";
		nodeName = "testName.test";
		nodeParentID = "syn489";
		Node testNode = new Node();
		testNode.setName(nodeName);
		testNode.setParentId(nodeParentID);
		
		ReflectionTestUtils.setField(trashManager, "trashCanDao", mockTrashCanDao);
		ReflectionTestUtils.setField(trashManager, "authorizationManager", mockAuthorizationManager);
		ReflectionTestUtils.setField(trashManager, "nodeManager", mockNodeManager);
		
		Mockito.when(mockNodeDAO.getNode(nodeID)).thenReturn(testNode);
		
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
		
		
		verify(mockNodeManager, times(1)).updateForTrashCan(userInfo, testNode, ChangeType.DELETE);
		verify(mockTrashCanDao, times(1)).create(userInfo.getId().toString(), nodeID, nodeName, nodeParentID);
		
		//verify(mockTrashCanDao, times(.size())).delete(userGroupId, nodeId);
		//TODO: finish after writing tests for getDescendants()
		
		
		
		fail("not implemented");
	}
	
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
		fail("not implemented");
	}
	
	@Test
	public void testGetDescendantsNoDescendants(){
		fail("not implemented");
	}
	
	@Test
	public void testGetDescendantsHaveDescendants(){
		fail("not implemented");
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
		verify(mockTrashCanDao,times(1)).getTrashBefore(now);//prob not necessary
	}
	

}
