package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACL_SCHEME;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ReferenceDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This is the unit test version of this class.
 * 
 * @author jmhill
 *
 */
public class NodeManagerImpleUnitTest {
	
	private NodeDAO mockNodeDao = null;
	private AuthorizationManager mockAuthDao = null;
	private NodeManagerImpl nodeManager = null;
	private AccessControlListDAO mockAclDao = null;
	private EntityBootstrapper mockEntityBootstrapper;
	private NodeInheritanceManager mockNodeInheritanceManager = null;
	private ReferenceDao mockReferenceDao = null;
	private ActivityManager mockActivityManager;
		
	private final UserInfo mockUserInfo = new UserInfo(false);
	private final UserInfo anonUserInfo = new UserInfo(false);

	@Before
	public void before() throws Exception {
		mockNodeDao = Mockito.mock(NodeDAO.class);
		mockAuthDao = Mockito.mock(AuthorizationManager.class);
		mockAclDao = Mockito.mock(AccessControlListDAO.class);
		mockEntityBootstrapper = Mockito.mock(EntityBootstrapper.class);
		mockNodeInheritanceManager = Mockito.mock(NodeInheritanceManager.class);
		mockReferenceDao = Mockito.mock(ReferenceDao.class);
		mockActivityManager = Mockito.mock(ActivityManager.class);
		// Create the manager dao with mocked dependent daos.
		nodeManager = new NodeManagerImpl(mockNodeDao, mockAuthDao, mockAclDao, 
				mockEntityBootstrapper, mockNodeInheritanceManager, mockReferenceDao, mockActivityManager);

		UserGroup userGroup = new UserGroup();
		userGroup.setId("2");
		userGroup.setName("two@foo.bar");
		userGroup.setIsIndividual(true);
		User mockUser = new User();
		mockUser.setId("101");
		mockUser.setUserId("test-user");
		mockUserInfo.setUser(mockUser);
		mockUserInfo.setIndividualGroup(userGroup);
		
		User anonUser = new User();
		anonUser.setId("102");
		anonUser.setUserId(AuthorizationConstants.ANONYMOUS_USER_ID);
		anonUserInfo.setUser(anonUser);
		anonUserInfo.setIndividualGroup(userGroup);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateNullNode(){
		NodeManagerImpl.validateNode(null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateNullNodeName(){
		Node node = new Node();
		NodeManagerImpl.validateNode(node);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateNullNodeType(){
		Node node = new Node();
		node.setName("notNull");
		NodeManagerImpl.validateNode(node);
	}
	
	@Test
	public void testValidateNode(){
		Node node = new Node();
		node.setName("notNull");
		node.setNodeType("some type");
		NodeManagerImpl.validateNode(node);
	}
	
	@Test
	public void testValidateNodeCreatedDataWithPreset(){
		Node node = new Node();
		Long presetCreatedBy = 1L;
		Date presetCreatedOn = new Date(100L);
		node.setCreatedByPrincipalId(presetCreatedBy);
		node.setCreatedOn(presetCreatedOn);
		// Now validate the node
		NodeManagerImpl.validateNodeCreationData(Long.parseLong(anonUserInfo.getIndividualGroup().getId()), node);
		// the values SHOULD  have changed
		assertTrue(Math.abs(System.currentTimeMillis()-node.getCreatedOn().getTime())<100L);
		assertEquals(anonUserInfo.getIndividualGroup().getId(), node.getCreatedByPrincipalId().toString());
	}
	
	@Test
	public void testValidateNodeCreatedDataWithNulls(){
		Node node = new Node();
		// Now validate the node
		NodeManagerImpl.validateNodeCreationData(Long.parseLong(anonUserInfo.getIndividualGroup().getId()), node);
		// the values should not have changed
		assertNotNull(node.getCreatedOn());
		assertEquals(anonUserInfo.getIndividualGroup().getId(), node.getCreatedByPrincipalId().toString());
	}
	
	@Test
	public void testValidateNodeModifiedDataWithPreset(){
		Node node = new Node();
		Long presetModifiedBy = 2L;
		Date presetModifiedOn = new Date(100L);
		node.setModifiedByPrincipalId(presetModifiedBy);
		node.setModifiedOn(presetModifiedOn);
		// Now validate the node
		NodeManagerImpl.validateNodeModifiedData(Long.parseLong(anonUserInfo.getIndividualGroup().getId()), node);
		// the values should have changed
		assertTrue(!presetModifiedOn.equals( node.getModifiedOn()));
		assertTrue(!presetModifiedBy.equals( node.getModifiedByPrincipalId().toString()));
	}
	
	@Test
	public void testValidateNodeModifiedDataWithNulls(){
		Node node = new Node();
		// Now validate the node
		NodeManagerImpl.validateNodeModifiedData(Long.parseLong(anonUserInfo.getIndividualGroup().getId()), node);
		// the values should not have changed
		assertNotNull(node.getModifiedOn());
		assertEquals(anonUserInfo.getIndividualGroup().getId(), node.getModifiedByPrincipalId().toString());
	}
	
	@Test
	public void testCreateNode() throws Exception {
		// Test creating a new node with nothing but the name and type set
		Node newNode = new Node();
		newNode.setName("testCreateNode");
		newNode.setNodeType(EntityType.folder.name());
		when(mockEntityBootstrapper.getChildAclSchemeForPath("/root")).thenReturn(ACL_SCHEME.INHERIT_FROM_PARENT);
		
		// Sure the mock is ready.
		ArgumentCaptor<Node> argument = ArgumentCaptor.forClass(Node.class);
		when(mockNodeDao.createNew(argument.capture())).thenReturn("101");
		UserInfo userInfo = anonUserInfo;
		when(mockAuthDao.canCreate(eq(userInfo), (Node)any())).thenReturn(true);
		// Make the actual call
		String id = nodeManager.createNewNode(newNode, userInfo);
		// Now validate that t
		assertEquals("101", id);
		Node processedNode = argument.getValue();
		assertNotNull(processedNode);
		assertEquals(anonUserInfo.getIndividualGroup().getId(), processedNode.getCreatedByPrincipalId().toString());
		assertEquals(anonUserInfo.getIndividualGroup().getId(), processedNode.getModifiedByPrincipalId().toString());
		assertNotNull(processedNode.getModifiedOn());
		assertNotNull(processedNode.getModifiedByPrincipalId());
	}

	@Test
	public void testCreateNodeActivity404() throws Exception {
		// Test creating a new node with nothing but the name and type set
		Node newNode = new Node();
		newNode.setName("testCreateNode");
		newNode.setNodeType(EntityType.folder.name());
		when(mockAuthDao.canCreate(eq(mockUserInfo), (Node)any())).thenReturn(true);
		String activityId = "8439208403928402";
		newNode.setActivityId(activityId);
		when(mockEntityBootstrapper.getChildAclSchemeForPath("/root")).thenReturn(ACL_SCHEME.INHERIT_FROM_PARENT);
		
		// not found
		try {
			when(mockActivityManager.doesActivityExist(activityId)).thenReturn(false);		
			nodeManager.createNewNode(newNode, mockUserInfo);
			fail("node should not have been created");
		} catch (NotFoundException e) {
			// good.
		}
		
		// found
		reset(mockAuthDao);
		when(mockActivityManager.doesActivityExist(activityId)).thenReturn(true);
		when(mockAuthDao.canAccessActivity(mockUserInfo, activityId)).thenReturn(true);
		when(mockAuthDao.canCreate(eq(mockUserInfo), (Node)any())).thenReturn(true);
		when(mockNodeDao.createNew(any(Node.class))).thenReturn("101");
		nodeManager.createNewNode(newNode, mockUserInfo);		
		verify(mockNodeDao).createNew(newNode);
	}
	
	@Test
	public void testCreateNodeActivity403() throws Exception {
		// Test creating a new node with nothing but the name and type set
		Node newNode = new Node();
		newNode.setName("testCreateNode");
		newNode.setNodeType(EntityType.folder.name());
		when(mockAuthDao.canCreate(eq(mockUserInfo), (Node)any())).thenReturn(true);
		String activityId = "8439208403928402";
		newNode.setActivityId(activityId);
		when(mockEntityBootstrapper.getChildAclSchemeForPath("/root")).thenReturn(ACL_SCHEME.INHERIT_FROM_PARENT);
		when(mockActivityManager.doesActivityExist(activityId)).thenReturn(true);
		
		// fail authorization
		try {
			when(mockAuthDao.canAccessActivity(mockUserInfo, activityId)).thenReturn(false);		
			nodeManager.createNewNode(newNode, mockUserInfo);
			fail("node should not have been created");
		} catch (UnauthorizedException e) {
			// good.
		}
		
		// pass authorization
		reset(mockAuthDao);
		when(mockAuthDao.canAccessActivity(mockUserInfo, activityId)).thenReturn(true);
		when(mockAuthDao.canCreate(eq(mockUserInfo), (Node)any())).thenReturn(true);
		when(mockNodeDao.createNew(any(Node.class))).thenReturn("101");
		nodeManager.createNewNode(newNode, mockUserInfo);		
		verify(mockNodeDao).createNew(newNode);
	} 
		
	@Test
	public void testUpdateNodeActivity404() throws Exception {
		// Test creating a new node with nothing but the name and type set
		Node node = new Node();
		node.setId("123");
		node.setName("testUpdateNode");
		node.setNodeType(EntityType.folder.name());		
		String activityId = "8439208403928402";
		node.setActivityId(activityId);		
		when(mockAuthDao.canAccess(eq(mockUserInfo), eq(node.getId()), eq(ACCESS_TYPE.UPDATE))).thenReturn(true);
		
		// not found
		try {
			when(mockActivityManager.doesActivityExist(activityId)).thenReturn(false);		
			nodeManager.update(mockUserInfo, node);
			fail("node should not have been updated");
		} catch (NotFoundException e) {
			// good.
		}
		
		// found
		reset(mockAuthDao);
		when(mockActivityManager.doesActivityExist(activityId)).thenReturn(true);
		when(mockAuthDao.canAccessActivity(mockUserInfo, activityId)).thenReturn(true);
		when(mockAuthDao.canAccess(eq(mockUserInfo), eq(node.getId()), eq(ACCESS_TYPE.UPDATE))).thenReturn(true);
		when(mockAuthDao.canAccess(eq(mockUserInfo), eq(node.getId()), eq(ACCESS_TYPE.READ))).thenReturn(true);
		nodeManager.update(mockUserInfo, node);		
		verify(mockNodeDao).updateNode(node);		
	}
	
	@Test
	public void testUpdateNodeActivity403() throws Exception {
		// Test creating a new node with nothing but the name and type set
		Node node = new Node();
		node.setId("123");
		node.setName("testUpdateNode");
		node.setNodeType(EntityType.folder.name());		
		String activityId = "8439208403928402";
		node.setActivityId(activityId);		
		when(mockAuthDao.canAccess(eq(mockUserInfo), eq(node.getId()), eq(ACCESS_TYPE.UPDATE))).thenReturn(true);
		when(mockActivityManager.doesActivityExist(activityId)).thenReturn(true);		
		
		// fail authZ
		try {
			when(mockAuthDao.canAccessActivity(mockUserInfo, activityId)).thenReturn(false);
			nodeManager.update(mockUserInfo, node);
			fail("node should not have been updated");
		} catch (UnauthorizedException e) {
			// good.
		}
		
		// pass authZ
		reset(mockAuthDao);
		when(mockActivityManager.doesActivityExist(activityId)).thenReturn(true);
		when(mockAuthDao.canAccessActivity(mockUserInfo, activityId)).thenReturn(true);
		when(mockAuthDao.canAccess(eq(mockUserInfo), eq(node.getId()), eq(ACCESS_TYPE.UPDATE))).thenReturn(true);
		when(mockAuthDao.canAccess(eq(mockUserInfo), eq(node.getId()), eq(ACCESS_TYPE.READ))).thenReturn(true);
		nodeManager.update(mockUserInfo, node);		
		verify(mockNodeDao).updateNode(node);		
	}
	
	@Test
	public void testGetAnnotations() throws NotFoundException, DatastoreException, UnauthorizedException{
		String id = "101";
		NamedAnnotations named = new NamedAnnotations();
		Annotations annos = named.getAdditionalAnnotations();
		annos.addAnnotation("stringKey", "a");
		annos.addAnnotation("longKey", Long.MAX_VALUE);
		when(mockNodeDao.getAnnotations(id)).thenReturn(named);
		UserInfo userInfo = anonUserInfo;
		when(mockAuthDao.canAccess(userInfo, id, ACCESS_TYPE.READ)).thenReturn(true);
		NamedAnnotations namedCopy = nodeManager.getAnnotations(userInfo, id);
		Annotations copy = namedCopy.getAdditionalAnnotations();
		assertEquals(copy, annos);
	}
	
	@Test
	public void testGetReferences() throws Exception {
		QueryResults<EntityHeader> expected = new QueryResults<EntityHeader>();
		expected.setResults(new ArrayList<EntityHeader>());
		Long id = 101L;
		UserInfo userInfo = anonUserInfo;
		when(mockReferenceDao.getReferrers(id, null, userInfo, null, null)).thenReturn(expected);
		QueryResults<EntityHeader> actual = nodeManager.getEntityReferences(userInfo, ""+id, null, null, null);
	}
	
	/**
	 * See PLFM-1533
	 */
	@Test
	public void testIsParentIDChange(){
		// test the various flavors of parent id change
		assertTrue(NodeManagerImpl.isParenIdChange(null, "notNull"));
		assertTrue(NodeManagerImpl.isParenIdChange("notNull", null));
		assertTrue(NodeManagerImpl.isParenIdChange("one", "two"));
		assertTrue(NodeManagerImpl.isParenIdChange("two", "one"));
		assertFalse(NodeManagerImpl.isParenIdChange(null, null));
		assertFalse(NodeManagerImpl.isParenIdChange("one", "one"));
	}
	
	@Test
	public void testGetActivityForNode() throws Exception {		
		String nodeId = "123";
		String activityId = "1";
		Activity act = new Activity();
		act.setId(activityId);
		when(mockActivityManager.getActivity(mockUserInfo, activityId)).thenReturn(act);
		
		// test activity not found		
		when(mockNodeDao.getActivityId(nodeId)).thenThrow(new NotFoundException());
		try {
			nodeManager.getActivityForNode(mockUserInfo, nodeId, null);
			fail("method is swallowing not found exception");
		} catch (NotFoundException e) {
			// good
		}
		
		// test current version
		reset(mockNodeDao);
		when(mockNodeDao.getActivityId(nodeId)).thenReturn(activityId);		
		Activity actCurrent = nodeManager.getActivityForNode(mockUserInfo, nodeId, null);
		assertEquals(act, actCurrent);
		
		// test specific version
		reset(mockNodeDao);
		Long versionNumber = 1L;
		when(mockNodeDao.getActivityId(nodeId, versionNumber)).thenReturn(activityId);
		Activity actVersion = nodeManager.getActivityForNode(mockUserInfo, nodeId, versionNumber);
		assertEquals(act, actVersion);
	}

	@Test
	public void testSetActivityForNode() throws Exception {
		String nodeId = "123";
		String activityId = "1";
		Activity act = new Activity();
		act.setId(activityId);
		Node node = mock(Node.class);
		
		when(node.getId()).thenReturn(nodeId);
		when(node.getNodeType()).thenReturn(EntityType.project.toString());
		when(node.getName()).thenReturn("some name");
		when(mockActivityManager.getActivity(mockUserInfo, activityId)).thenReturn(act);
		when(mockNodeDao.getNode(nodeId)).thenReturn(node);
		when(mockAuthDao.canAccess(eq(mockUserInfo), eq(nodeId), eq(ACCESS_TYPE.UPDATE))).thenReturn(false);
		when(mockAuthDao.canAccess(eq(mockUserInfo), eq(nodeId), eq(ACCESS_TYPE.READ))).thenReturn(true);
		
		// unathorized
		try {
			nodeManager.setActivityForNode(mockUserInfo, nodeId, activityId);
			fail("Should not have allowed update");
		} catch (UnauthorizedException e) {
			// good
		}

		reset(node);
		when(node.getId()).thenReturn(nodeId);
		when(node.getNodeType()).thenReturn(EntityType.project.toString());
		when(node.getName()).thenReturn("some name");
		
		// update for real
		when(mockAuthDao.canAccess(eq(mockUserInfo), eq(nodeId), eq(ACCESS_TYPE.UPDATE))).thenReturn(true);
		
		nodeManager.setActivityForNode(mockUserInfo, nodeId, activityId);
		verify(node).setActivityId(activityId);		
		verify(mockNodeDao).updateNode(node);		
	}

	@Test
	public void testDeleteActivityForNode() throws Exception {
		String nodeId = "123";
		String activityId = "1";
		Activity act = new Activity();
		act.setId(activityId);
		Node node = mock(Node.class);
		
		when(node.getId()).thenReturn(nodeId);
		when(node.getNodeType()).thenReturn(EntityType.project.toString());
		when(node.getName()).thenReturn("some name");
		when(mockActivityManager.getActivity(mockUserInfo, activityId)).thenReturn(act);
		when(mockNodeDao.getNode(nodeId)).thenReturn(node);
		when(mockAuthDao.canAccess(eq(mockUserInfo), eq(nodeId), eq(ACCESS_TYPE.UPDATE))).thenReturn(false);
		when(mockAuthDao.canAccess(eq(mockUserInfo), eq(nodeId), eq(ACCESS_TYPE.READ))).thenReturn(true);
		
		// unathorized
		try {
			nodeManager.deleteActivityLinkToNode(mockUserInfo, nodeId);
			fail("Should not have allowed update");
		} catch (UnauthorizedException e) {
			// good
		}

		reset(node);
		when(node.getId()).thenReturn(nodeId);
		when(node.getNodeType()).thenReturn(EntityType.project.toString());
		when(node.getName()).thenReturn("some name");
		
		// update for real
		when(mockAuthDao.canAccess(eq(mockUserInfo), eq(nodeId), eq(ACCESS_TYPE.UPDATE))).thenReturn(true);
		
		nodeManager.deleteActivityLinkToNode(mockUserInfo, nodeId);
		verify(node).setActivityId(null);		
		verify(mockNodeDao).updateNode(node);		
	}
}
