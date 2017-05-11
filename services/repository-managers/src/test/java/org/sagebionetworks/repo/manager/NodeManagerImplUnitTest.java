package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.manager.util.CollectionUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACL_SCHEME;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * This is the unit test version of this class.
 * 
 * @author jmhill
 *
 */
public class NodeManagerImplUnitTest {
	
	@Mock
	private NodeDAO mockNodeDao = null;
	@Mock
	private AuthorizationManager mockAuthManager = null;
	@Mock
	private AccessControlListDAO mockAclDao = null;
	@Mock
	private EntityBootstrapper mockEntityBootstrapper;
	@Mock
	private ActivityManager mockActivityManager;
	@Mock
	private ProjectSettingsManager projectSettingsManager;
	@Mock
	private Node mockNode;
	@Mock
	private TransactionalMessenger transactionalMessenger;
	
	private NodeManagerImpl nodeManager = null;
		
	private UserInfo mockUserInfo;
	private UserInfo anonUserInfo;

	String nodeId;
	String parentId;
	EntityType type;
	Set<EntityType> entityTypesWithCountLimits;
	String newEtag;
	
	@Before
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);
		// Create the manager dao with mocked dependent daos.
		nodeManager = new NodeManagerImpl();
		ReflectionTestUtils.setField(nodeManager, "nodeDao", mockNodeDao);
		ReflectionTestUtils.setField(nodeManager, "authorizationManager", mockAuthManager);
		ReflectionTestUtils.setField(nodeManager, "aclDAO", mockAclDao);
		ReflectionTestUtils.setField(nodeManager, "entityBootstrapper", mockEntityBootstrapper);
		ReflectionTestUtils.setField(nodeManager, "activityManager", mockActivityManager);
		ReflectionTestUtils.setField(nodeManager, "projectSettingsManager", projectSettingsManager);
		ReflectionTestUtils.setField(nodeManager, "transactionalMessenger", transactionalMessenger);

		mockUserInfo = new UserInfo(false, 101L);
		
		anonUserInfo = new UserInfo(false, 102L);
		
		nodeId = "123";
		parentId = "456";
		newEtag = "newEtag";
		
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getParentId()).thenReturn(parentId);
		when(mockNode.getNodeType()).thenReturn(EntityType.project);
		when(mockNode.getName()).thenReturn("some name");
		when(mockNodeDao.getNode(nodeId)).thenReturn(mockNode);
		
		type = EntityType.file;
		when(mockNodeDao.getChildCount(parentId)).thenReturn(StackConfiguration.getMaximumNumberOfEntitiesPerContainer()-1);
		// Types that have count limits
		entityTypesWithCountLimits = Sets.newHashSet(EntityType.file, EntityType.folder, EntityType.link);
				
		when(mockAuthManager.canCreate(any(UserInfo.class), any(String.class), any(EntityType.class))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthManager.canAccess(any(UserInfo.class), anyString(), eq(ObjectType.ENTITY), any(ACCESS_TYPE.class))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthManager.canUserMoveRestrictedEntity(any(UserInfo.class),  anyString(),  anyString())).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthManager.canAccessRawFileHandleById(any(UserInfo.class), anyString())).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthManager.canAccessActivity(any(UserInfo.class), anyString())).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockActivityManager.doesActivityExist(anyString())).thenReturn(true);
		
		when(mockNodeDao.lockNodeAndIncrementEtag(anyString(), anyString(), any(ChangeType.class))).thenReturn(newEtag);
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
		node.setNodeType(EntityType.file);
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
		NodeManagerImpl.validateNodeCreationData(anonUserInfo.getId(), node);
		// the values SHOULD  have changed
		assertTrue(Math.abs(System.currentTimeMillis()-node.getCreatedOn().getTime())<100L);
		assertEquals(anonUserInfo.getId().toString(), node.getCreatedByPrincipalId().toString());
	}
	
	@Test
	public void testValidateNodeCreatedDataWithNulls(){
		Node node = new Node();
		// Now validate the node
		NodeManagerImpl.validateNodeCreationData(anonUserInfo.getId(), node);
		// the values should not have changed
		assertNotNull(node.getCreatedOn());
		assertEquals(anonUserInfo.getId(), node.getCreatedByPrincipalId());
	}
	
	@Test
	public void testValidateNodeModifiedDataWithPreset(){
		Node node = new Node();
		Long presetModifiedBy = 2L;
		Date presetModifiedOn = new Date(100L);
		node.setModifiedByPrincipalId(presetModifiedBy);
		node.setModifiedOn(presetModifiedOn);
		// Now validate the node
		NodeManagerImpl.validateNodeModifiedData(anonUserInfo.getId(), node);
		// the values should have changed
		assertTrue(!presetModifiedOn.equals( node.getModifiedOn()));
		assertTrue(!presetModifiedBy.equals( node.getModifiedByPrincipalId().toString()));
	}
	
	@Test
	public void testValidateNodeModifiedDataWithNulls(){
		Node node = new Node();
		// Now validate the node
		NodeManagerImpl.validateNodeModifiedData(anonUserInfo.getId(), node);
		// the values should not have changed
		assertNotNull(node.getModifiedOn());
		assertEquals(anonUserInfo.getId(), node.getModifiedByPrincipalId());
	}
	
	@Test
	public void testCreateNode() throws Exception {
		// Test creating a new node with nothing but the name and type set
		Node newNode = new Node();
		newNode.setName("testCreateNode");
		newNode.setNodeType(EntityType.folder);
		newNode.setParentId(parentId);
		when(mockEntityBootstrapper.getChildAclSchemeForPath("/root")).thenReturn(ACL_SCHEME.INHERIT_FROM_PARENT);
		
		// Sure the mock is ready.
		ArgumentCaptor<Node> argument = ArgumentCaptor.forClass(Node.class);
		newNode.setId("101");
		when(mockNodeDao.createNewNode(argument.capture())).thenReturn(newNode);
		UserInfo userInfo = anonUserInfo;
		when(mockAuthManager.canCreate(eq(userInfo), any(String.class), any(EntityType.class))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		// Make the actual call
		String id = nodeManager.createNewNode(newNode, userInfo);
		// Now validate that t
		assertEquals("101", id);
		Node processedNode = argument.getValue();
		assertNotNull(processedNode);
		assertEquals(anonUserInfo.getId(), processedNode.getCreatedByPrincipalId());
		assertEquals(anonUserInfo.getId(), processedNode.getModifiedByPrincipalId());
		assertNotNull(processedNode.getModifiedOn());
		assertNotNull(processedNode.getModifiedByPrincipalId());
		// a child count check should occur
		verify(mockNodeDao).getChildCount(parentId);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testCreateNodeNoUpload() throws Exception {
		// Test creating a new node with nothing but the name and type set
		Node newNode = new Node();
		newNode.setName("testCreateNode");
		newNode.setNodeType(EntityType.folder);  // in reality it would be a 'FileEntity'
		String fileHandleId = "123";
		newNode.setFileHandleId(fileHandleId);
		String parentId = "202";
		newNode.setParentId(parentId);
		when(mockEntityBootstrapper.getChildAclSchemeForPath("/root")).thenReturn(ACL_SCHEME.INHERIT_FROM_PARENT);
		
		// make sure the mock is ready
		ArgumentCaptor<Node> argument = ArgumentCaptor.forClass(Node.class);
		newNode.setId("101");
		when(mockNodeDao.createNewNode(argument.capture())).thenReturn(newNode);
		UserInfo userInfo = anonUserInfo;
		// OK to upload to parentId
		nodeManager.createNewNode(newNode, userInfo);
		when(mockAuthManager.canAccess(userInfo, parentId, ObjectType.ENTITY, ACCESS_TYPE.UPLOAD)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		// NOT OK to upload
		nodeManager.createNewNode(newNode, userInfo);
	}

	@Test//(expected=UnauthorizedException.class)
	public void testUpdateNoUpload() throws Exception {
		String nodeId = "101";
		String parentId = "202";
		String fileHandleId = "123";
		// Test creating a new node with nothing but the name and type set
		Node newNode = new Node();
		newNode.setName("testCreateNode");
		newNode.setNodeType(EntityType.folder);  // in reality it would be a 'FileEntity'
		newNode.setFileHandleId(fileHandleId);
		newNode.setParentId(parentId);
		when(mockEntityBootstrapper.getChildAclSchemeForPath("/root")).thenReturn(ACL_SCHEME.INHERIT_FROM_PARENT);
		
		// make sure the mock is ready
		ArgumentCaptor<Node> argument = ArgumentCaptor.forClass(Node.class);
		newNode.setId(nodeId);
		when(mockNodeDao.createNewNode(argument.capture())).thenReturn(newNode);
		UserInfo userInfo = anonUserInfo;
		// Make the actual call
		assertEquals(nodeId, nodeManager.createNewNode(newNode, userInfo));
		newNode.setId(nodeId);

		Node oldNode = mock(Node.class);
		when(oldNode.getParentId()).thenReturn(parentId);
		when(mockNodeDao.getNode("101")).thenReturn(oldNode);
		
		when(mockNodeDao.getParentId(nodeId)).thenReturn(parentId);
		nodeManager.update(userInfo, newNode);

		when(mockAuthManager.canAccess(userInfo, parentId, ObjectType.ENTITY, ACCESS_TYPE.UPLOAD)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		try {
			nodeManager.update(userInfo, newNode);
			fail("expected UnauthorizedException");
		} catch (UnauthorizedException e) {
			// as expected
		}
	}

	@Test
	public void testCreateNodeActivity404() throws Exception {
		// Test creating a new node with nothing but the name and type set
		Node newNode = new Node();
		newNode.setName("testCreateNode");
		newNode.setNodeType(EntityType.folder);
		newNode.setParentId(parentId);
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
		when(mockActivityManager.doesActivityExist(activityId)).thenReturn(true);
		newNode.setId("101");
		when(mockNodeDao.createNewNode(any(Node.class))).thenReturn(newNode);
		nodeManager.createNewNode(newNode, mockUserInfo);		
		verify(mockNodeDao).createNewNode(newNode);
	}
	
	@Test
	public void testCreateNodeActivity403() throws Exception {
		// Test creating a new node with nothing but the name and type set
		Node newNode = new Node();
		newNode.setName("testCreateNode");
		newNode.setNodeType(EntityType.folder);
		newNode.setParentId(parentId);
		String activityId = "8439208403928402";
		newNode.setActivityId(activityId);
		when(mockEntityBootstrapper.getChildAclSchemeForPath("/root")).thenReturn(ACL_SCHEME.INHERIT_FROM_PARENT);
		when(mockActivityManager.doesActivityExist(activityId)).thenReturn(true);
		
		// fail authorization
		try {
			when(mockAuthManager.canAccessActivity(mockUserInfo, activityId)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);		
			nodeManager.createNewNode(newNode, mockUserInfo);
			fail("node should not have been created");
		} catch (UnauthorizedException e) {
			// good.
		}
		
		// pass authorization
		when(mockAuthManager.canAccessActivity(mockUserInfo, activityId)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		newNode.setId("101");
		when(mockNodeDao.createNewNode(any(Node.class))).thenReturn(newNode);
		nodeManager.createNewNode(newNode, mockUserInfo);		
		verify(mockNodeDao).createNewNode(newNode);
	} 
		
	@Test
	public void testUpdateNodeActivity404() throws Exception {
		// Test creating a new node with nothing but the name and type set
		Node node = new Node();
		String nodeId = "123";
		String parentId = "456";
		node.setId(nodeId);
		node.setName("testUpdateNode");
		node.setNodeType(EntityType.folder);	
		node.setParentId(parentId);
		String activityId = "8439208403928402";
		node.setActivityId(activityId);		
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(node.getId()), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockNodeDao.getParentId(nodeId)).thenReturn(parentId);
		when(mockAuthManager.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(parentId))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		Node oldNode = mock(Node.class);
		when(oldNode.getParentId()).thenReturn(parentId);
		when(mockNodeDao.getNode("123")).thenReturn(oldNode);
		// not found
		try {
			when(mockActivityManager.doesActivityExist(activityId)).thenReturn(false);		
			nodeManager.update(mockUserInfo, node);
			fail("node should not have been updated");
		} catch (NotFoundException e) {
			// good.
		}
		
		// found
		when(mockActivityManager.doesActivityExist(activityId)).thenReturn(true);
		nodeManager.update(mockUserInfo, node);		
		verify(mockNodeDao).updateNode(node);		
	}
	
	@Test
	public void testUpdateNodeActivity403() throws Exception {
		// Test creating a new node with nothing but the name and type set
		Node node = new Node();
		String nodeId = "123";
		String parentId = "456";
		node.setId(nodeId);
		node.setParentId(parentId);
		node.setName("testUpdateNode");
		node.setNodeType(EntityType.folder);		
		String activityId = "8439208403928402";
		node.setActivityId(activityId);			
		when(mockNodeDao.getParentId(nodeId)).thenReturn(parentId);
		Node oldNode = mock(Node.class);
		when(oldNode.getParentId()).thenReturn(parentId);
		when(mockNodeDao.getNode("123")).thenReturn(oldNode);

		// fail authZ
		try {
			when(mockAuthManager.canAccessActivity(mockUserInfo, activityId)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
			nodeManager.update(mockUserInfo, node);
			fail("node should not have been updated");
		} catch (UnauthorizedException e) {
			// good.
		}
		
		// pass authZ
		when(mockAuthManager.canAccessActivity(mockUserInfo, activityId)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
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
		when(mockAuthManager.canAccess(userInfo, id, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		NamedAnnotations namedCopy = nodeManager.getAnnotations(userInfo, id);
		Annotations copy = namedCopy.getAdditionalAnnotations();
		assertEquals(copy, annos);
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
		String activityId = "1";
		Activity act = new Activity();
		act.setId(activityId);

		when(mockActivityManager.getActivity(mockUserInfo, activityId)).thenReturn(act);
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		
		// unathorized
		try {
			nodeManager.setActivityForNode(mockUserInfo, nodeId, activityId);
			fail("Should not have allowed update");
		} catch (UnauthorizedException e) {
			// good
		}

		reset(mockNode);
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getParentId()).thenReturn(parentId);
		when(mockNode.getNodeType()).thenReturn(EntityType.project);
		when(mockNode.getName()).thenReturn("some name");
		
		// update for real
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);

		when(mockNodeDao.getParentId(nodeId)).thenReturn(parentId);
		when(mockAuthManager.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(parentId))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);

		nodeManager.setActivityForNode(mockUserInfo, nodeId, activityId);
		verify(mockNode).setActivityId(activityId);		
		verify(mockNodeDao).updateNode(mockNode);		
	}

	@Test
	public void testDeleteActivityForNode() throws Exception {
		String activityId = "1";
		Activity act = new Activity();
		act.setId(activityId);
		
		when(mockActivityManager.getActivity(mockUserInfo, activityId)).thenReturn(act);
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		when(mockNodeDao.getParentId(nodeId)).thenReturn(parentId);
		
		// unauthorized
		try {
			nodeManager.deleteActivityLinkToNode(mockUserInfo, nodeId);
			fail("Should not have allowed update");
		} catch (UnauthorizedException e) {
			// good
		}

		reset(mockNode);
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getParentId()).thenReturn(parentId);
		when(mockNode.getNodeType()).thenReturn(EntityType.project);
		when(mockNode.getName()).thenReturn("some name");
		
		// update for real
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		
		nodeManager.deleteActivityLinkToNode(mockUserInfo, nodeId);
		verify(mockNode).setActivityId(NodeDAO.DELETE_ACTIVITY_VALUE);		
		verify(mockNodeDao).updateNode(mockNode);		
	}
	
	@Test
	public void testUpdateNodeNewParent() throws DatastoreException, NotFoundException {
		String authorizedParentId = "456";
		String unauthorizedParentId = "789";

		when(mockNode.getParentId()).thenReturn(unauthorizedParentId);
		when(mockNodeDao.getParentId(nodeId)).thenReturn(parentId);
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(unauthorizedParentId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.CREATE))).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		when(mockAuthManager.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(unauthorizedParentId))).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		Node oldNode = mock(Node.class);
		when(oldNode.getParentId()).thenReturn(parentId);
		when(mockNodeDao.getNode("123")).thenReturn(oldNode);
		
		// unauthorized
		try {
			nodeManager.update(mockUserInfo, mockNode);
			fail("Should not have allowed update");
		} catch (UnauthorizedException e) {
			// expected
			verify(mockNodeDao, never()).updateNode(any(Node.class));
		}

		reset(mockNode);
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getParentId()).thenReturn(authorizedParentId);
		when(mockNode.getNodeType()).thenReturn(EntityType.table);
		when(mockNode.getName()).thenReturn("some name");
		
		// authorized	
		nodeManager.update(mockUserInfo, mockNode);
		verify(mockNodeDao).updateNode(mockNode);
		
		// governance restriction on move
		reset(mockNode);
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getParentId()).thenReturn(authorizedParentId);
		when(mockAuthManager.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(authorizedParentId))).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		try {
			nodeManager.update(mockUserInfo, mockNode);
			fail("Should not have allowed update");
		} catch (UnauthorizedException e) {
			// expected
		}
	}
	
	@Test
	public void testUpdateNewParentIdValidateCount(){
		String currentParentId = "246";
		String newParentId = "syn456";
		when(mockNode.getParentId()).thenReturn(newParentId);
		when(mockNode.getNodeType()).thenReturn(EntityType.file);
		when(mockNodeDao.getParentId(nodeId)).thenReturn(currentParentId);
		Node oldNode = mock(Node.class);
		when(oldNode.getParentId()).thenReturn(currentParentId);
		when(mockNodeDao.getNode(nodeId)).thenReturn(oldNode);
		// call under test
		nodeManager.update(mockUserInfo, mockNode);
		// count check should occur
		verify(mockNodeDao).getChildCount(newParentId);
	}
	
	@Test
	public void testUpdateNewParentIdFolder(){
		String currentParentId = "syn111";
		String newParentId = "syn222";
		when(mockNode.getParentId()).thenReturn(newParentId);
		when(mockNode.getNodeType()).thenReturn(EntityType.folder);
		when(mockNodeDao.getParentId(nodeId)).thenReturn(currentParentId);
		Node oldNode = mock(Node.class);
		when(oldNode.getParentId()).thenReturn(currentParentId);
		when(mockNodeDao.getNode(nodeId)).thenReturn(oldNode);
		// call under test
		nodeManager.update(mockUserInfo, mockNode);
		// count check should occur
		verify(transactionalMessenger).sendMessageAfterCommit(nodeId, ObjectType.ENTITY_CONTAINER, newEtag, ChangeType.UPDATE);
	}
	
	@Test
	public void testUpdateNewParentIdFile(){
		String currentParentId = "syn111";
		String newParentId = "syn222";
		when(mockNode.getParentId()).thenReturn(newParentId);
		when(mockNode.getNodeType()).thenReturn(EntityType.file);
		when(mockNodeDao.getParentId(nodeId)).thenReturn(currentParentId);
		Node oldNode = mock(Node.class);
		when(oldNode.getParentId()).thenReturn(currentParentId);
		when(mockNodeDao.getNode(nodeId)).thenReturn(oldNode);
		// call under test
		nodeManager.update(mockUserInfo, mockNode);
		// message should not be sent for a file.
		verify(transactionalMessenger, never()).sendMessageAfterCommit(anyString(), any(ObjectType.class), anyString(), any(ChangeType.class));
	}
	
	@Test
	public void testUpdateSameParentFolder(){
		String currentParentId = "syn222";
		String newParentId = "syn222";
		when(mockNode.getParentId()).thenReturn(newParentId);
		when(mockNode.getNodeType()).thenReturn(EntityType.folder);
		when(mockNodeDao.getParentId(nodeId)).thenReturn(currentParentId);
		Node oldNode = mock(Node.class);
		when(oldNode.getParentId()).thenReturn(currentParentId);
		when(mockNodeDao.getNode(nodeId)).thenReturn(oldNode);
		// call under test
		nodeManager.update(mockUserInfo, mockNode);
		// message should not be sent since this is not a parent change.
		verify(transactionalMessenger, never()).sendMessageAfterCommit(anyString(), any(ObjectType.class), anyString(), any(ChangeType.class));
	}
	
	@Test
	public void testUpdateSameParentValidateCount(){
		String currentParentId = "syn222";
		String newParentId = "222";
		when(mockNode.getParentId()).thenReturn(newParentId);
		when(mockNode.getNodeType()).thenReturn(EntityType.file);
		when(mockNodeDao.getParentId(nodeId)).thenReturn(currentParentId);
		Node oldNode = mock(Node.class);
		when(oldNode.getParentId()).thenReturn(currentParentId);
		when(mockNodeDao.getNode(nodeId)).thenReturn(oldNode);
		// call under test
		nodeManager.update(mockUserInfo, mockNode);
		// not a parent change so count should not be checked.
		verify(mockNodeDao, never()).getChildCount(newParentId);
	}

	@Test
	public void testPromoteVersionAuthorized() throws Exception {
		String nodeId = "123";
		long versionNumber = 1L;
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		Node mockNode = mock(Node.class);
		when(mockNodeDao.getNodeForVersion(nodeId, versionNumber)).thenReturn(mockNode);
		List<VersionInfo> versionInfoList = new ArrayList<VersionInfo>();
		versionInfoList.add(mock(VersionInfo.class));
		when(mockNodeDao.getVersionsOfEntity(nodeId, 0, 1)).thenReturn(versionInfoList);
		nodeManager.promoteEntityVersion(mockUserInfo, nodeId, versionNumber);
		verify(mockNodeDao, times(1)).lockNodeAndIncrementEtag(eq(nodeId), anyString());
		verify(mockNodeDao, times(1)).createNewVersion(mockNode);
	}

	@Test(expected=UnauthorizedException.class)
	public void testPromoteVersionUnauthorized() throws Exception {
		String nodeId = "123";
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		nodeManager.promoteEntityVersion(mockUserInfo, nodeId, 1L);
	}

	@Test
	public void testGetNodeHeaderByMd5() throws Exception {

		// Test empty results
		final String nodeId = "testGetNodeHeaderByMd5";

		// no intersection
		Set<Long> intersection = Sets.newHashSet();
		when(mockAuthManager.getAccessibleBenefactors(any(UserInfo.class), (any(Set.class)))).thenReturn(intersection);
		
		List<EntityHeader> results = new ArrayList<EntityHeader>(1);
		EntityHeader header = new EntityHeader();
		header.setId(nodeId);
		header.setBenefactorId(0L);
		results.add(header);
		String md5 = "md5NotFound";
		when(mockNodeDao.getEntityHeaderByMd5(md5)).thenReturn(results);
		results = nodeManager.getNodeHeaderByMd5(mockUserInfo, md5);
		assertNotNull(results);
		assertEquals(0, results.size());

		// Test 2 nodes and 1 node gets filtered out
		final String nodeId1 = "canRead";
		final String nodeId2 = "cannotRead";

		results = new ArrayList<EntityHeader>(1);
		EntityHeader header1 = new EntityHeader();
		header1.setId(nodeId1);
		header1.setBenefactorId(1L);
		results.add(header1);
		EntityHeader header2 = new EntityHeader();
		header2.setId(nodeId2);
		header2.setBenefactorId(2L);
		results.add(header2);
		md5 = "md5";
		
		intersection = Sets.newHashSet(header1.getBenefactorId());
		when(mockAuthManager.getAccessibleBenefactors(any(UserInfo.class), (any(Set.class)))).thenReturn(intersection);
		
		when(mockNodeDao.getEntityHeaderByMd5(md5)).thenReturn(results);
		List<EntityHeader> headerList = nodeManager.getNodeHeaderByMd5(mockUserInfo, md5);
		assertNotNull(headerList);
		assertEquals(1, headerList.size());
		assertEquals(nodeId1, headerList.get(0).getId());
	}
	
	@Test
	public void testFilterUnauthorizedHeaders(){
		List<EntityHeader> toFilter = createTestEntityHeaders(3);
		// Filter out the middle benefactor.
		Set<Long> intersection = Sets.newHashSet(toFilter.get(0).getBenefactorId(), toFilter.get(2).getBenefactorId());
		when(mockAuthManager.getAccessibleBenefactors(any(UserInfo.class), (any(Set.class)))).thenReturn(intersection);
		//call under test
		List<EntityHeader> results = nodeManager.filterUnauthorizedHeaders(mockUserInfo, toFilter);
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals(toFilter.get(0), results.get(0));
		assertEquals(toFilter.get(2), results.get(1));
	}
	
	@Test
	public void testFilterUnauthorizedHeadersNoIntersection(){
		List<EntityHeader> toFilter = createTestEntityHeaders(3);
		// Filter out the middle benefactor.
		Set<Long> intersection = Sets.newHashSet();
		when(mockAuthManager.getAccessibleBenefactors(any(UserInfo.class), (any(Set.class)))).thenReturn(intersection);
		//call under test
		List<EntityHeader> results = nodeManager.filterUnauthorizedHeaders(mockUserInfo, toFilter);
		assertNotNull(results);
		assertEquals(0, results.size());
	}
	
	@Test
	public void testFilterUnauthorizedHeadersEmptyStart(){
		// empty starting list should not hit the DB.
		List<EntityHeader> toFilter = createTestEntityHeaders(0);
		//call under test
		List<EntityHeader> results = nodeManager.filterUnauthorizedHeaders(mockUserInfo, toFilter);
		assertNotNull(results);
		assertEquals(0, results.size());
		verify(mockAuthManager, never()).getAccessibleBenefactors(any(UserInfo.class), (any(Set.class)));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testFilterUnauthorizedHeadersNullFilter(){
		List<EntityHeader> toFilter = null;
		//call under test
		 nodeManager.filterUnauthorizedHeaders(mockUserInfo, toFilter);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testFilterUnauthorizedHeadersNullUser(){
		List<EntityHeader> toFilter = createTestEntityHeaders(2);
		mockUserInfo = null;
		//call under test
		 nodeManager.filterUnauthorizedHeaders(mockUserInfo, toFilter);
	}
	
	/**
	 * Helper to create some EntityHeaders.
	 * @param count
	 * @return
	 */
	private List<EntityHeader> createTestEntityHeaders(int count){
		List<EntityHeader> list = Lists.newArrayListWithCapacity(count);
		for(int i=0; i<count; i++){
			EntityHeader header = new EntityHeader();
			header.setBenefactorId(new Long(i-1));
			header.setId(""+i);
			header.setName("name"+i);
			list.add(header);
		}
		return list;
	}
	
	@Test
	public void testGetNodeHeader(){
		List<EntityHeader> allResults = createTestEntityHeaders(3);
		List<Reference> refs = Lists.newArrayList(new Reference(), new Reference(), new Reference(), new Reference());
		refs.get(0).setTargetId(allResults.get(0).getId());
		refs.get(1).setTargetId(allResults.get(1).getId());
		refs.get(2).setTargetId(allResults.get(2).getId());
		refs.get(3).setTargetId("4");
		
		when(mockNodeDao.getEntityHeader(any(List.class))).thenReturn(allResults);
		// the user can only read 0 and 2.
		Set<Long> intersection = Sets.newHashSet(allResults.get(0).getBenefactorId(), allResults.get(2).getBenefactorId());
		when(mockAuthManager.getAccessibleBenefactors(any(UserInfo.class), (any(Set.class)))).thenReturn(intersection);
		
		List<EntityHeader> results = nodeManager.getNodeHeader(mockUserInfo, refs);
		// only two results should be returned
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals(allResults.get(0), results.get(0));
		assertEquals(allResults.get(2), results.get(1));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetFileHandleIdsAssociatedWithFileEntityNullFileHandleIds(){
		nodeManager.getFileHandleIdsAssociatedWithFileEntity(null, "syn123");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetFileHandleIdsAssociatedWithFileEntityNullEntityId(){
		nodeManager.getFileHandleIdsAssociatedWithFileEntity(new ArrayList<String>(), null);
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithFileEntity(){
		List<String> input = Arrays.asList("1", "2", "3");
		List<Long> inputLong = new ArrayList<Long>();
		CollectionUtils.convertStringToLong(input, inputLong);
		String entityId = "syn123";
		Set<Long> output = new HashSet<Long>();
		output.add(2L);
		when(mockNodeDao.getFileHandleIdsAssociatedWithFileEntity(inputLong, 123L)).thenReturn(output);
		Set<String> outputString = nodeManager.getFileHandleIdsAssociatedWithFileEntity(input, entityId);
		assertNotNull(outputString);
		assertEquals(1L, outputString.size());
		assertTrue(outputString.contains("2"));
	}
	
	@Test
	public void testValidateChildCountFileUnder(){
		nodeManager.validateChildCount(parentId, type);
		verify(mockNodeDao).getChildCount(parentId);
	}
	
	@Test
	public void testValidateChildCountFileOver(){
		EntityType type = EntityType.file;
		when(mockNodeDao.getChildCount(parentId)).thenReturn(StackConfiguration.getMaximumNumberOfEntitiesPerContainer());
		try {
			nodeManager.validateChildCount(parentId, type);
			fail();
		} catch (IllegalArgumentException e) {
			// expected
			assertTrue(e.getMessage().contains(""+StackConfiguration.getMaximumNumberOfEntitiesPerContainer()));
			assertTrue(e.getMessage().contains(parentId));
		}
	}
	
	@Test
	public void testValidateChildCountFolderUnder(){
		type = EntityType.folder;
		nodeManager.validateChildCount(parentId, type);
		verify(mockNodeDao).getChildCount(parentId);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateChildCountFolderOver(){
		type = EntityType.folder;
		when(mockNodeDao.getChildCount(parentId)).thenReturn(StackConfiguration.getMaximumNumberOfEntitiesPerContainer());
		nodeManager.validateChildCount(parentId, type);
	}
	
	@Test
	public void testValidateChildCountLinkUnder(){
		type = EntityType.link;
		nodeManager.validateChildCount(parentId, type);
		verify(mockNodeDao).getChildCount(parentId);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateChildCountLinkOver(){
		type = EntityType.link;
		when(mockNodeDao.getChildCount(parentId)).thenReturn(StackConfiguration.getMaximumNumberOfEntitiesPerContainer());
		nodeManager.validateChildCount(parentId, type);
	}
	
	@Test
	public void testValidateChildCountTypesWithoutLimits(){
		for(EntityType type: EntityType.values()){
			if(!entityTypesWithCountLimits.contains(type)){
				reset(mockNodeDao);
				nodeManager.validateChildCount(parentId, type);
				// count should not occur for this type
				verify(mockNodeDao, never()).getChildCount(anyString());
			}
		}
	}
	
	@Test
	public void testValidateChildCountParentTrash(){
		parentId = StackConfiguration.getTrashFolderEntityIdStatic();
		nodeManager.validateChildCount(parentId, type);
		// no check should occur for a move to the trash
		verify(mockNodeDao, never()).getChildCount(anyString());
	}
	
	@Test
	public void testValidateChildCountParentRoot(){
		parentId = StackConfiguration.getRootFolderEntityIdStatic();
		nodeManager.validateChildCount(parentId, type);
		// no check should occur for a create in root
		verify(mockNodeDao, never()).getChildCount(anyString());
	}
}
