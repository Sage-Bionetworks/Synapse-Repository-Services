package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.manager.util.CollectionUtils;
import org.sagebionetworks.repo.manager.sts.StsManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.entity.FileHandleUpdateRequest;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * This is the unit test version of this class.
 * 
 * @author jmhill
 *
 */
@ExtendWith(MockitoExtension.class)
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
	@Mock
	private FileHandleDao mockFileHandleDao;
	@Mock
	private StsManager mockStsManager;
	
	@InjectMocks
	private NodeManagerImpl nodeManager = null;
		
	private UserInfo mockUserInfo;
	private UserInfo anonUserInfo;

	String nodeId;
	String parentId;
	EntityType type;
	Set<EntityType> entityTypesWithCountLimits;
	String startEtag;
	String newEtag;
	org.sagebionetworks.repo.model.Annotations entityPropertyAnnotations;
	Annotations userAnnotations;
	
	@BeforeEach
	public void before() throws Exception {
		mockUserInfo = new UserInfo(false, 101L);
		
		anonUserInfo = new UserInfo(false, 102L);
		
		nodeId = "123";
		parentId = "456";
		startEtag = "startEtag";
		newEtag = "newEtag";
		type = EntityType.file;
		// Types that have count limits
		entityTypesWithCountLimits = Sets.newHashSet(EntityType.file, EntityType.folder, EntityType.link);
		entityPropertyAnnotations = new org.sagebionetworks.repo.model.Annotations();
		entityPropertyAnnotations.addAnnotation("key", "value");

		userAnnotations = new Annotations();
		userAnnotations.setEtag("etag");
	}
	
	@Test
	public void testValidateNullNode(){
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			NodeManagerImpl.validateNode(null);
		});
		
		assertEquals("Node cannot be null", ex.getMessage());
	}
	
	@Test
	public void testValidateNullNodeName(){	
		when(mockNode.getName()).thenReturn(null);
		when(mockNode.getNodeType()).thenReturn(type);
		// Call under test
		NodeManagerImpl.validateNode(mockNode);
	}
	
	@Test
	public void testValidateNullNodeType(){
		when(mockNode.getNodeType()).thenReturn(null);
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			NodeManagerImpl.validateNode(mockNode);
		});
		
		assertEquals("Node.type cannot be null", ex.getMessage());
	}
	
	@Test
	public void testValidateNode(){
		when(mockNode.getName()).thenReturn("Some name");
		when(mockNode.getNodeType()).thenReturn(type);
		
		NodeManagerImpl.validateNode(mockNode);
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
		when(mockAuthManager.canCreate(any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.isNodeAvailable(parentId)).thenReturn(true);
		
		// Sure the mock is ready.
		ArgumentCaptor<Node> argument = ArgumentCaptor.forClass(Node.class);
		newNode.setId("101");
		when(mockNodeDao.createNewNode(argument.capture())).thenReturn(newNode);
		UserInfo userInfo = anonUserInfo;
		when(mockAuthManager.canCreate(eq(userInfo), any(String.class), any(EntityType.class))).thenReturn(AuthorizationStatus.authorized());
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
		verify(mockNodeDao).getEntityPathDepth(parentId, NodeConstants.MAX_PATH_DEPTH+1);
	}
	
	@Test
	public void testCreateNodeParentDoesNotExist() throws Exception {
		String parenId = "syn123";
		Node node = new Node();
		node.setName("foo");
		node.setParentId("syn123");
		node.setNodeType(EntityType.folder);
		when(mockNodeDao.isNodeAvailable(parenId)).thenReturn(false);
		
		NotFoundException ex = Assertions.assertThrows(NotFoundException.class, ()-> {
			// call under test
			this.nodeManager.createNode(node, mockUserInfo);
		});
		
		assertTrue(ex.getMessage().contains(parenId+" does not exist"));
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
		when(mockAuthManager.canCreate(any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.isNodeAvailable(parentId)).thenReturn(true);
		when(mockActivityManager.doesActivityExist(activityId)).thenReturn(false);		
		
		Assertions.assertThrows(NotFoundException.class, () -> {
			nodeManager.createNewNode(newNode, mockUserInfo);
		});
		
		// found
		when(mockAuthManager.canAccessActivity(mockUserInfo, activityId)).thenReturn(AuthorizationStatus.authorized());
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
		
		when(mockAuthManager.canCreate(mockUserInfo, newNode.getParentId(), newNode.getNodeType())).thenReturn(AuthorizationStatus.authorized());
		when(mockActivityManager.doesActivityExist(activityId)).thenReturn(true);
		when(mockAuthManager.canAccessActivity(mockUserInfo, activityId)).thenReturn(AuthorizationStatus.accessDenied(""));
		when(mockNodeDao.isNodeAvailable(newNode.getParentId())).thenReturn(true);
		
		// fail authorization
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			nodeManager.createNewNode(newNode, mockUserInfo);
		});
		
		// pass authorization
		when(mockAuthManager.canAccessActivity(mockUserInfo, activityId)).thenReturn(AuthorizationStatus.authorized());
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
		node.setETag(startEtag);
		String activityId = "8439208403928402";
		node.setActivityId(activityId);		
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(node.getId()), eq(ObjectType.ENTITY), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(parentId))).thenReturn(AuthorizationStatus.authorized());
		Node oldNode = mock(Node.class);
		when(oldNode.getParentId()).thenReturn(parentId);
		when(mockNodeDao.getNode("123")).thenReturn(oldNode);
		when(mockActivityManager.doesActivityExist(activityId)).thenReturn(false);		

		// not found
		Assertions.assertThrows(NotFoundException.class, ()-> {
			nodeManager.update(mockUserInfo, node, null, false);
		});
		
		// found
		when(mockNodeDao.lockNode(nodeId)).thenReturn(startEtag);
		when(mockAuthManager.canAccessActivity(mockUserInfo, activityId)).thenReturn(AuthorizationStatus.authorized());
		when(mockActivityManager.doesActivityExist(activityId)).thenReturn(true);
		nodeManager.update(mockUserInfo, node, null, false);
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
		node.setETag(startEtag);
		String activityId = "8439208403928402";
		node.setActivityId(activityId);
		Node oldNode = mock(Node.class);
		when(oldNode.getParentId()).thenReturn(parentId);
		when(mockNodeDao.lockNode(nodeId)).thenReturn(startEtag);
		when(mockNodeDao.getNode("123")).thenReturn(oldNode);
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canUserMoveRestrictedEntity(any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockActivityManager.doesActivityExist(activityId)).thenReturn(true);
		when(mockAuthManager.canAccessActivity(mockUserInfo, activityId)).thenReturn(AuthorizationStatus.accessDenied(""));

		// fail authZ
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			nodeManager.update(mockUserInfo, node, null, false);
		});
		
		// pass authZ
		when(mockAuthManager.canAccessActivity(mockUserInfo, activityId)).thenReturn(AuthorizationStatus.authorized());
		nodeManager.update(mockUserInfo, node, null, false);
		verify(mockNodeDao).updateNode(node);		
	}
	
	@Test
	public void testGetAnnotations() throws NotFoundException, DatastoreException, UnauthorizedException{
		String id = "101";
		Annotations annos = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(annos, "stringKey", "a", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annos, "longKey", "12312", AnnotationsValueType.LONG);
		when(mockNodeDao.getUserAnnotations(id)).thenReturn(annos);
		UserInfo userInfo = anonUserInfo;
		when(mockAuthManager.canAccess(userInfo, id, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		Annotations copy = nodeManager.getUserAnnotations(userInfo, id);
		assertEquals(copy, annos);
	}

	@Test
	public void testGetEntityPropertyAnnotations() throws NotFoundException, DatastoreException, UnauthorizedException{
		String id = "101";
		org.sagebionetworks.repo.model.Annotations annos = new org.sagebionetworks.repo.model.Annotations();
		annos.addAnnotation("stringKey", "a");
		annos.addAnnotation("longKey", Long.MAX_VALUE);
		when(mockNodeDao.getEntityPropertyAnnotations(id)).thenReturn(annos);
		UserInfo userInfo = anonUserInfo;
		when(mockAuthManager.canAccess(userInfo, id, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		org.sagebionetworks.repo.model.Annotations copy = nodeManager.getEntityPropertyAnnotations(userInfo, id);
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
		
		Assertions.assertThrows(NotFoundException.class, ()-> {
			nodeManager.getActivityForNode(mockUserInfo, nodeId, null);
		});
		
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

		when(mockNodeDao.getNode(nodeId)).thenReturn(mockNode);
		when(mockNodeDao.lockNode(any())).thenReturn(startEtag);
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getParentId()).thenReturn(parentId);
		when(mockNode.getNodeType()).thenReturn(type);
		when(mockNode.getETag()).thenReturn(startEtag);
		
		//when(mockActivityManager.getActivity(mockUserInfo, activityId)).thenReturn(act);
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ))).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationStatus.accessDenied(""));
		
		// unathorized
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			nodeManager.setActivityForNode(mockUserInfo, nodeId, activityId);
		});

		reset(mockNode);
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getParentId()).thenReturn(parentId);
		when(mockNode.getNodeType()).thenReturn(EntityType.project);
		when(mockNode.getName()).thenReturn("some name");
		when(mockNode.getETag()).thenReturn(startEtag);
		
		// update for real
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(parentId))).thenReturn(AuthorizationStatus.authorized());

		nodeManager.setActivityForNode(mockUserInfo, nodeId, activityId);
		verify(mockNode).setActivityId(activityId);		
		verify(mockNodeDao).updateNode(mockNode);		
	}

	@Test
	public void testDeleteActivityForNode() throws Exception {
		String activityId = "1";
		Activity act = new Activity();
		act.setId(activityId);
		
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getParentId()).thenReturn(parentId);
		when(mockNode.getNodeType()).thenReturn(EntityType.project);
		when(mockNode.getName()).thenReturn("some name");
		when(mockNode.getETag()).thenReturn(startEtag);
		
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ))).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationStatus.accessDenied(""));
		when(mockNodeDao.getNode(nodeId)).thenReturn(mockNode);
		
		// unauthorized
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			nodeManager.deleteActivityLinkToNode(mockUserInfo, nodeId);
		});
		
		// update for real
		when(mockNodeDao.lockNode(nodeId)).thenReturn(startEtag);
		when(mockAuthManager.canUserMoveRestrictedEntity(any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationStatus.authorized());
		
		nodeManager.deleteActivityLinkToNode(mockUserInfo, nodeId);
		verify(mockNode, times(2)).setActivityId(NodeDAO.DELETE_ACTIVITY_VALUE);
		verify(mockNodeDao).updateNode(mockNode);		
	}
	
	@Test
	public void testUpdate() {
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getParentId()).thenReturn(parentId);
		when(mockNode.getNodeType()).thenReturn(type);
		when(mockNode.getETag()).thenReturn(startEtag);
		when(mockNodeDao.lockNode(any())).thenReturn(startEtag);
		
		when(mockAuthManager.canAccess(any(), anyString(), eq(ObjectType.ENTITY), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canUserMoveRestrictedEntity(any(UserInfo.class),  anyString(),  anyString())).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.getNode(nodeId)).thenReturn(mockNode);
		
		// call under test
		nodeManager.update(mockUserInfo, mockNode, null, false);
		verify(mockNodeDao).lockNode(nodeId);
		verify(mockNodeDao).touch(mockUserInfo.getId(), mockNode.getId());
	}
	
	@Test
	public void testUpdateConflictException() {
	
		when(mockNodeDao.getNode(nodeId)).thenReturn(mockNode);
		
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getParentId()).thenReturn(parentId);
		when(mockNode.getNodeType()).thenReturn(type);
		when(mockNode.getETag()).thenReturn("wrongEtag");
		when(mockNodeDao.lockNode(any())).thenReturn(startEtag);
		
		when(mockAuthManager.canAccess(any(UserInfo.class), anyString(), eq(ObjectType.ENTITY), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canUserMoveRestrictedEntity(any(UserInfo.class),  anyString(),  anyString())).thenReturn(AuthorizationStatus.authorized());
		
		ConflictingUpdateException ex = Assertions.assertThrows(ConflictingUpdateException.class, ()-> {
			// call under test
			nodeManager.update(mockUserInfo, mockNode, null, false);
		});
		// expected
		assertTrue(ex.getMessage().contains(mockNode.getId()));
		
		verify(mockNodeDao).lockNode(nodeId);
		verify(mockNodeDao, never()).touch(any(Long.class), anyString());
	}
	
	@Test
	public void testUpdateNodeNewParent() throws DatastoreException, NotFoundException {
		String authorizedParentId = "456";
		String unauthorizedParentId = "789";

		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getNodeType()).thenReturn(type);
		when(mockNode.getETag()).thenReturn(startEtag);
		when(mockNode.getParentId()).thenReturn(unauthorizedParentId);

		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(unauthorizedParentId))).thenReturn(AuthorizationStatus.accessDenied(""));

		Node oldNode = mock(Node.class);
		when(oldNode.getParentId()).thenReturn(parentId);
		when(mockNodeDao.getNode(nodeId)).thenReturn(oldNode);
		
		// unauthorized
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			nodeManager.update(mockUserInfo, mockNode, null, false);
		});
		// expected
		verify(mockNodeDao, never()).updateNode(any(Node.class));	

		reset(mockNode);
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getParentId()).thenReturn(authorizedParentId);
		when(mockNode.getNodeType()).thenReturn(EntityType.table);
		when(mockNode.getName()).thenReturn("some name");
		when(mockNode.getETag()).thenReturn(startEtag);
		
		when(mockNodeDao.lockNode(any())).thenReturn(startEtag);
		when(mockAuthManager.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(authorizedParentId))).thenReturn(AuthorizationStatus.authorized());
		
		// authorized	
		nodeManager.update(mockUserInfo, mockNode, null, false);
		verify(mockNodeDao).updateNode(mockNode);
		
		// governance restriction on move
		reset(mockNode);
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getParentId()).thenReturn(authorizedParentId);
		when(mockNode.getNodeType()).thenReturn(EntityType.table);
		when(mockNode.getETag()).thenReturn(startEtag);
		when(mockAuthManager.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(authorizedParentId))).thenReturn(AuthorizationStatus.accessDenied(""));
		
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			nodeManager.update(mockUserInfo, mockNode, null, false);
		});
	}
	
	@Test
	public void testUpdateNewParentIdValidateCount(){
		String currentParentId = "246";
		String newParentId = "syn456";
		
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getParentId()).thenReturn(newParentId);
		when(mockNode.getNodeType()).thenReturn(EntityType.file);
		when(mockNode.getETag()).thenReturn(startEtag);
		when(mockNodeDao.lockNode(nodeId)).thenReturn(startEtag);
		when(mockAuthManager.canAccess(eq(mockUserInfo), any(), eq(ObjectType.ENTITY), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canUserMoveRestrictedEntity(mockUserInfo, currentParentId, newParentId)).thenReturn(AuthorizationStatus.authorized());
		
		Node oldNode = mock(Node.class);
		when(oldNode.getParentId()).thenReturn(currentParentId);
		when(mockNodeDao.getNode(nodeId)).thenReturn(oldNode);
		// call under test
		nodeManager.update(mockUserInfo, mockNode, null, false);
		// count check should occur
		verify(mockNodeDao).getChildCount(newParentId);
		verify(mockNodeDao).getEntityPathDepth(newParentId, NodeConstants.MAX_PATH_DEPTH+1);
	}
	
	@Test
	public void testUpdateNewParentIdFolder(){
		String currentParentId = "syn111";
		String newParentId = "syn222";
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getETag()).thenReturn(startEtag);
		when(mockNode.getParentId()).thenReturn(newParentId);
		when(mockNode.getNodeType()).thenReturn(EntityType.folder);
		
		Node oldNode = mock(Node.class);
		when(oldNode.getParentId()).thenReturn(currentParentId);
		
		when(mockNodeDao.lockNode(nodeId)).thenReturn(startEtag);
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canUserMoveRestrictedEntity(any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.getNode(nodeId)).thenReturn(oldNode);
		when(mockNodeDao.touch(any(Long.class), any(String.class))).thenReturn(newEtag);
		
		// call under test
		nodeManager.update(mockUserInfo, mockNode, null, false);
		// count check should occur
		verify(transactionalMessenger).sendMessageAfterCommit(nodeId, ObjectType.ENTITY_CONTAINER, ChangeType.UPDATE);
	}
	
	@Test
	public void testUpdateDisallowedParentIdFolder(){
		String currentParentId = "syn111";
		String newParentId = "syn222";
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getETag()).thenReturn(startEtag);
		when(mockNode.getParentId()).thenReturn(newParentId);
		when(mockNode.getNodeType()).thenReturn(EntityType.folder);
		
		Node oldNode = mock(Node.class);
		when(oldNode.getParentId()).thenReturn(currentParentId);
		
		when(mockNodeDao.lockNode(nodeId)).thenReturn(startEtag);
		when(mockAuthManager.canAccess(any(), eq(nodeId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).
		thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccess(any(), eq(newParentId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.CREATE))).
		thenReturn(AuthorizationStatus.accessDenied(""));
		when(mockAuthManager.canUserMoveRestrictedEntity(any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.getNode(nodeId)).thenReturn(oldNode);
		when(mockNodeDao.touch(any(Long.class), any(String.class))).thenReturn(newEtag);
		
		// call under test
		UnauthorizedException thrown = assertThrows(UnauthorizedException.class, () ->
			nodeManager.update(mockUserInfo, mockNode, null, false));
		
		assertEquals("You cannot move content into the new location, "+newParentId+". ", thrown.getMessage());
		
	}
	
	@Test
	public void testUpdateNewParentIdFile(){
		String currentParentId = "syn111";
		String newParentId = "syn222";
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getParentId()).thenReturn(newParentId);
		when(mockNode.getNodeType()).thenReturn(EntityType.file);
		when(mockNode.getETag()).thenReturn(startEtag);
		when(mockNodeDao.lockNode(nodeId)).thenReturn(startEtag);
		
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canUserMoveRestrictedEntity(any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.touch(any(Long.class), any(String.class))).thenReturn(newEtag);
		
		Node oldNode = mock(Node.class);
		when(oldNode.getParentId()).thenReturn(currentParentId);
		when(mockNodeDao.getNode(nodeId)).thenReturn(oldNode);
		// call under test
		nodeManager.update(mockUserInfo, mockNode, null, false);
		// message should not be sent for a file.
		verify(transactionalMessenger, never()).sendMessageAfterCommit(anyString(), any(ObjectType.class), any(ChangeType.class));
	}
	
	@Test
	public void testUpdateSameParentFolder(){
		String currentParentId = "syn222";
		String newParentId = "syn222";
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getParentId()).thenReturn(newParentId);
		when(mockNode.getNodeType()).thenReturn(EntityType.folder);
		when(mockNode.getETag()).thenReturn(startEtag);
		when(mockNodeDao.lockNode(nodeId)).thenReturn(startEtag);
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canUserMoveRestrictedEntity(mockUserInfo, currentParentId, newParentId)).thenReturn(AuthorizationStatus.authorized());
		
		Node oldNode = mock(Node.class);
		when(oldNode.getParentId()).thenReturn(currentParentId);
		when(mockNodeDao.getNode(nodeId)).thenReturn(oldNode);
		// call under test
		nodeManager.update(mockUserInfo, mockNode, null, false);
		// message should not be sent since this is not a parent change.
		verify(transactionalMessenger, never()).sendMessageAfterCommit(anyString(), any(ObjectType.class), any(ChangeType.class));
	}
	
	@Test
	public void testUpdateSameParentValidateCount(){
		String currentParentId = "syn222";
		String newParentId = "222";
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getParentId()).thenReturn(newParentId);
		when(mockNode.getNodeType()).thenReturn(EntityType.file);
		when(mockNode.getETag()).thenReturn(startEtag);
		when(mockNodeDao.lockNode(nodeId)).thenReturn(startEtag);
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canUserMoveRestrictedEntity(mockUserInfo, currentParentId, newParentId)).thenReturn(AuthorizationStatus.authorized());
		
		Node oldNode = mock(Node.class);
		when(oldNode.getParentId()).thenReturn(currentParentId);
		when(mockNodeDao.getNode(nodeId)).thenReturn(oldNode);
		// call under test
		nodeManager.update(mockUserInfo, mockNode, null, false);
		// not a parent change so count should not be checked.
		verify(mockNodeDao, never()).getChildCount(newParentId);
	}

	// PLFM-4651
	@Test
	public void testUpdateNodeNoEtag() throws Exception {
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getNodeType()).thenReturn(type);
		when(mockNode.getETag()).thenReturn(null);
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			nodeManager.update(mockUserInfo, mockNode, entityPropertyAnnotations, false);
		});

		assertEquals("The eTag of the node is required.", ex.getMessage());
	}
	
	@Test
	public void testUpdateNodeWithNoId() {
		when(mockNode.getId()).thenReturn(null);
		when(mockNode.getNodeType()).thenReturn(type);
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			nodeManager.update(mockUserInfo, mockNode, entityPropertyAnnotations, false);
		});
		
		assertEquals("The id of the node is required.", ex.getMessage());
	}
	
	@Test
	public void testUpdateNodeWithNoParent() {
		when(mockNode.getId()).thenReturn(nodeId);
		when(mockNode.getParentId()).thenReturn(null);
		when(mockNode.getNodeType()).thenReturn(type);
		when(mockNode.getETag()).thenReturn(startEtag);
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			nodeManager.update(mockUserInfo, mockNode, entityPropertyAnnotations, false);
		});
		
		assertEquals("The parent id of the node is required.", ex.getMessage());
	}

	@Test
	public void testGetNodeHeaderByMd5() throws Exception {

		// Test empty results
		final String nodeId = "testGetNodeHeaderByMd5";

		// no intersection
		Set<Long> intersection = Sets.newHashSet();
		when(mockAuthManager.getAccessibleBenefactors(any(UserInfo.class), any(), (any(Set.class)))).thenReturn(intersection);
		
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
		when(mockAuthManager.getAccessibleBenefactors(any(), any(), anySet())).thenReturn(intersection);
		
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
		when(mockAuthManager.getAccessibleBenefactors(any(), any(), anySet())).thenReturn(intersection);
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
		when(mockAuthManager.getAccessibleBenefactors(any(), any(), anySet())).thenReturn(intersection);
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
		verify(mockAuthManager, never()).getAccessibleBenefactors(any(), any(), anySet());
	}
	
	@Test
	public void testFilterUnauthorizedHeadersNullFilter(){
		List<EntityHeader> toFilter = null;

		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			//call under test
			 nodeManager.filterUnauthorizedHeaders(mockUserInfo, toFilter);
		});
		
		assertEquals("toFilter is required.", ex.getMessage());
	}
	
	@Test
	public void testFilterUnauthorizedHeadersNullUser(){
		List<EntityHeader> toFilter = createTestEntityHeaders(2);
		mockUserInfo = null;
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			//call under test
			nodeManager.filterUnauthorizedHeaders(mockUserInfo, toFilter);
		});
		
		assertEquals("userInfo is required.", ex.getMessage());
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
		when(mockAuthManager.getAccessibleBenefactors(any(), any(), anySet())).thenReturn(intersection);
		
		List<EntityHeader> results = nodeManager.getNodeHeader(mockUserInfo, refs);
		// only two results should be returned
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals(allResults.get(0), results.get(0));
		assertEquals(allResults.get(2), results.get(1));
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithFileEntityNullFileHandleIds(){
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			nodeManager.getFileHandleIdsAssociatedWithFileEntity(null, "syn123");
		});
		
		assertEquals("fileHandleIds is required.", ex.getMessage());
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithFileEntityNullEntityId(){
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			nodeManager.getFileHandleIdsAssociatedWithFileEntity(new ArrayList<String>(), null);
		});
		
		assertEquals("entityId is required.", ex.getMessage());
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
	public void doesNodeHaveChildren_False() {
		// Mock dao.
		when(mockNodeDao.doesNodeHaveChildren(nodeId)).thenReturn(false);

		// Method under test.
		boolean result = nodeManager.doesNodeHaveChildren(nodeId);
		assertFalse(result);
	}

	@Test
	public void doesNodeHaveChildren_True() {
		// Mock dao.
		when(mockNodeDao.doesNodeHaveChildren(nodeId)).thenReturn(true);

		// Method under test.
		boolean result = nodeManager.doesNodeHaveChildren(nodeId);
		assertTrue(result);
	}

	@Test
	public void testValidateChildCountFileUnder(){
		nodeManager.validateChildCount(parentId, type);
		verify(mockNodeDao).getChildCount(parentId);
	}
	
	@Test
	public void testValidateChildCountFileOver(){
		EntityType type = EntityType.file;
		when(mockNodeDao.getChildCount(parentId)).thenReturn(StackConfigurationSingleton.singleton().getMaximumNumberOfEntitiesPerContainer());
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			nodeManager.validateChildCount(parentId, type);
		});
		
		// expected
		assertTrue(ex.getMessage().contains(""+StackConfigurationSingleton.singleton().getMaximumNumberOfEntitiesPerContainer()));
		assertTrue(ex.getMessage().contains(parentId));
		
	}
	
	@Test
	public void testValidateChildCountFolderUnder(){
		type = EntityType.folder;
		nodeManager.validateChildCount(parentId, type);
		verify(mockNodeDao).getChildCount(parentId);
	}
	
	@Test
	public void testValidateChildCountFolderOver(){
		type = EntityType.folder;
		when(mockNodeDao.getChildCount(parentId)).thenReturn(StackConfigurationSingleton.singleton().getMaximumNumberOfEntitiesPerContainer());
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			nodeManager.validateChildCount(parentId, type);
		});
		
		assertEquals("Limit of 10000 children exceeded for parent: 456", ex.getMessage());
	}
	
	@Test
	public void testValidateChildCountLinkUnder(){
		type = EntityType.link;
		nodeManager.validateChildCount(parentId, type);
		verify(mockNodeDao).getChildCount(parentId);
	}
	
	@Test
	public void testValidateChildCountLinkOver(){
		type = EntityType.link;
		when(mockNodeDao.getChildCount(parentId)).thenReturn(StackConfigurationSingleton.singleton().getMaximumNumberOfEntitiesPerContainer());
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			nodeManager.validateChildCount(parentId, type);
		});
		
		assertEquals("Limit of 10000 children exceeded for parent: 456", ex.getMessage());
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
		parentId = StackConfigurationSingleton.singleton().getTrashFolderEntityId();
		nodeManager.validateChildCount(parentId, type);
		// no check should occur for a move to the trash
		verify(mockNodeDao, never()).getChildCount(anyString());
	}
	
	@Test
	public void testValidateChildCountParentRoot(){
		parentId = StackConfigurationSingleton.singleton().getRootFolderEntityId();
		nodeManager.validateChildCount(parentId, type);
		// no check should occur for a create in root
		verify(mockNodeDao, never()).getChildCount(anyString());
	}
	
	@Test
	public void testValidatePathDepthWithNullParentId() {
		String parentId = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			nodeManager.validatePathDepth(parentId);
		}).getMessage();
		assertEquals("parentId is required.", message);
		verifyNoMoreInteractions(mockNodeDao);
	}
	
	@Test
	public void testValidatePathDepthWithUnderLimit() {
		String parentId = "syn123";
		when(mockNodeDao.getEntityPathDepth(any(), anyInt())).thenReturn(1);
		// call under test
		nodeManager.validatePathDepth(parentId);
		verify(mockNodeDao).getEntityPathDepth(parentId, NodeConstants.MAX_PATH_DEPTH+1);
	}
	
	@Test
	public void testValidatePathDepthWithAtLimit() {
		String parentId = "syn123";
		when(mockNodeDao.getEntityPathDepth(any(), anyInt())).thenReturn(NodeConstants.MAX_PATH_DEPTH-1);
		// call under test
		nodeManager.validatePathDepth(parentId);
		verify(mockNodeDao).getEntityPathDepth(parentId, NodeConstants.MAX_PATH_DEPTH+1);
	}
	
	@Test
	public void testValidatePathDepthWithOverLimit() {
		String parentId = "syn123";
		when(mockNodeDao.getEntityPathDepth(any(), anyInt())).thenReturn(NodeConstants.MAX_PATH_DEPTH);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			nodeManager.validatePathDepth(parentId);
		}).getMessage();
		assertEquals(
				"Exceeded the maximum hierarchical depth of: " + NodeConstants.MAX_PATH_DEPTH + " for parent: syn123",
				message);
		verify(mockNodeDao).getEntityPathDepth(parentId, NodeConstants.MAX_PATH_DEPTH + 1);
	}


	@Test
	public void testUpdateAnnotations_nullEtag(){
		Annotations updated = new Annotations();
		updated.setAnnotations(null);
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			nodeManager.updateUserAnnotations(mockUserInfo, nodeId, updated);
		});

		assertEquals("etag is required and must not be the empty string.", ex.getMessage());
	}

	@Test
	public void testUpdateAnnotations() {
		Annotations updated = new Annotations();
		updated.setEtag(startEtag);
		updated.setId(nodeId);
		
		when(mockNodeDao.lockNode(nodeId)).thenReturn(startEtag);
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), any())).thenReturn(AuthorizationStatus.authorized());
		
		// call under test
		nodeManager.updateUserAnnotations(mockUserInfo, nodeId, updated);
		verify(mockNodeDao).lockNode(nodeId);
		verify(mockNodeDao).touch(mockUserInfo.getId(), nodeId);
	}
	
	@Test
	public void testUpdateAnnotationsConflict() {
		Annotations updated = new Annotations();
		updated.setEtag("wrongEtag");
		updated.setId(nodeId);
		
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.lockNode(nodeId)).thenReturn(startEtag);
		
		ConflictingUpdateException ex = Assertions.assertThrows(ConflictingUpdateException.class, ()->{
			// call under test
			nodeManager.updateUserAnnotations(mockUserInfo, nodeId, updated);
		});
	
		// expected
		assertTrue(ex.getMessage().contains(nodeId));
		
		verify(mockNodeDao).lockNode(nodeId);
		verify(mockNodeDao, never()).touch(any(Long.class), anyString());
	}
	
	@Test
	public void testCreateSnapshotAndVersionUnauthorizedEntityUpdate() {
		SnapshotRequest request = new SnapshotRequest();
		
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE)))
				.thenReturn(AuthorizationStatus.accessDenied("Unauthorized"));
		
		UnauthorizedException ex = Assertions.assertThrows(UnauthorizedException.class, ()-> {
			// call under test
			nodeManager.createSnapshotAndVersion(mockUserInfo, nodeId, request);
		});
		
		assertEquals("Unauthorized", ex.getMessage());
	}
	
	@Test
	public void testCreateSnapshotAndVersionUnauthorizedActivityId() {
		SnapshotRequest request = new SnapshotRequest();
		String activityId = "987";
		request.setSnapshotActivityId(activityId);
		// can update the entity
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE)))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockActivityManager.doesActivityExist(anyString())).thenReturn(true);
		// but cannot access the activity.
		when(mockAuthManager.canAccessActivity(mockUserInfo, activityId))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			// call under test
			nodeManager.createSnapshotAndVersion(mockUserInfo, nodeId, request);
		});
	}
	
	@Test
	public void testCreateSnapshotAndVersion() {
		SnapshotRequest request = new SnapshotRequest();
		request.setSnapshotComment("new comment");
		request.setSnapshotLabel("new label");
		String activityId = "987";
		request.setSnapshotActivityId(activityId);
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE)))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessActivity(mockUserInfo, activityId)).thenReturn(AuthorizationStatus.authorized());
		when(mockActivityManager.doesActivityExist(activityId)).thenReturn(true);
		
		Node currentNode = new Node();
		currentNode.setId(nodeId);
		currentNode.setVersionComment("old comment");
		currentNode.setVersionLabel("old label");
		currentNode.setActivityId("765");
		when(mockNodeDao.getNode(nodeId)).thenReturn(currentNode);
		long newVersion = 444L;
		when(mockNodeDao.createNewVersion(any(Node.class))).thenReturn(newVersion);
		long snapshotVersion = 443;
		when(mockNodeDao.snapshotVersion(any(Long.class), any(String.class), any(SnapshotRequest.class))).thenReturn(snapshotVersion);
		
		// call under test
		long resultVersion = nodeManager.createSnapshotAndVersion(mockUserInfo, nodeId, request);
		assertEquals(snapshotVersion, resultVersion);
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
		verify(mockAuthManager).canAccessActivity(mockUserInfo, activityId);
		verify(mockNodeDao).lockNode(nodeId);
		verify(mockNodeDao).snapshotVersion(mockUserInfo.getId(), nodeId, request);
		Node expectedNewNode = new Node();
		expectedNewNode.setId(nodeId);
		expectedNewNode.setVersionComment(TableConstants.IN_PROGRESS);
		expectedNewNode.setVersionLabel(TableConstants.IN_PROGRESS);
		expectedNewNode.setActivityId(null);
		verify(mockNodeDao).createNewVersion(expectedNewNode);
		verify(mockNodeDao).touch(mockUserInfo.getId(), nodeId);
	}
	
	@Test
	public void testCreateSnapshotAndVersionNullUser() {
		SnapshotRequest request = new SnapshotRequest();
		UserInfo userInfo = null;
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// call under test
			nodeManager.createSnapshotAndVersion(userInfo, nodeId, request);
		});

		assertEquals("UserInfo is required.", ex.getMessage());
	}
	
	@Test
	public void testCreateSnapshotAndVersionNullNodeId() {
		SnapshotRequest request = new SnapshotRequest();
		String nullNodeId = null;
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// call under test
			nodeManager.createSnapshotAndVersion(mockUserInfo, nullNodeId, request);
		});
		
		assertEquals("id is required.", ex.getMessage());
	}
	
	@Test
	public void testCreateSnapshotAndVersionNullRequest() {
		
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.getNode(nodeId)).thenReturn(mockNode);
		
		SnapshotRequest request = null;
		// call under test
		nodeManager.createSnapshotAndVersion(mockUserInfo, nodeId, request);
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
		verify(mockAuthManager, never()).canAccessActivity(any(UserInfo.class), anyString());
		verify(mockNodeDao).lockNode(nodeId);
		verify(mockNodeDao).snapshotVersion(mockUserInfo.getId(), nodeId, new SnapshotRequest());
	}
	
	@Test
	public void testGetVersionsOfEntityFile() {
		long offset = 0;
		long limit = 10;
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.getNodeTypeById(nodeId)).thenReturn(EntityType.file);
		VersionInfo info = new VersionInfo();
		info.setId("456");
		List<VersionInfo> expected = Lists.newArrayList(info);
		when(mockNodeDao.getVersionsOfEntity(any(String.class), any(Long.class), any(Long.class))).thenReturn(expected);
		// call under test
		List<VersionInfo> results = nodeManager.getVersionsOfEntity(mockUserInfo, nodeId, offset, limit);
		assertEquals(expected, results);
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockNodeDao).getVersionsOfEntity(nodeId, offset, limit);
	}
	
	@Test
	public void testgetVersionsOfEntityTable() {
		long offset = 0;
		long limit = 10;
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.getNodeTypeById(nodeId)).thenReturn(EntityType.table);
		VersionInfo info = new VersionInfo();
		info.setId("456");
		List<VersionInfo> expected = Lists.newArrayList(info);
		when(mockNodeDao.getVersionsOfEntity(any(String.class), any(Long.class), any(Long.class))).thenReturn(expected);
		// call under test
		List<VersionInfo> results = nodeManager.getVersionsOfEntity(mockUserInfo, nodeId, offset, limit);
		assertEquals(expected, results);
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		// offset + one for tables/views
		verify(mockNodeDao).getVersionsOfEntity(nodeId, offset+1, limit);
	}
	
	@Test
	public void testgetVersionsOfEntityEntityView() {
		long offset = 0;
		long limit = 10;
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ))).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.getNodeTypeById(nodeId)).thenReturn(EntityType.entityview);
		VersionInfo info = new VersionInfo();
		info.setId("456");
		List<VersionInfo> expected = Lists.newArrayList(info);
		when(mockNodeDao.getVersionsOfEntity(any(String.class), any(Long.class), any(Long.class))).thenReturn(expected);
		// call under test
		List<VersionInfo> results = nodeManager.getVersionsOfEntity(mockUserInfo, nodeId, offset, limit);
		assertEquals(expected, results);
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		// offset + one for tables/views
		verify(mockNodeDao).getVersionsOfEntity(nodeId, offset+1, limit);
	}
	
	@Test
	public void testGetName() {
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ))).thenReturn(AuthorizationStatus.authorized());
		String name = "foo";
		when(mockNodeDao.getNodeName(nodeId)).thenReturn(name);
		// call under test
		String resultName = nodeManager.getNodeName(mockUserInfo, nodeId);
		assertEquals(name, resultName);
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockNodeDao).getNodeName(nodeId);
	}
	
	@Test
	public void testGetNameUnauthorized() {
		when(mockAuthManager.canAccess(eq(mockUserInfo), eq(nodeId), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ)))
				.thenReturn(AuthorizationStatus.accessDenied("nope"));
		assertThrows(UnauthorizedException.class, ()->{
			nodeManager.getNodeName(mockUserInfo, nodeId);
		});
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockNodeDao, never()).getNodeName(anyString());
	}
	
	@Test
	public void testUpdateNodeFileHandle() {
		String nodeId = this.nodeId;
		Long versionNumber = 1L;
		String oldFileHandleId = "123";
		String newFileHandleId = "456";
		
		FileHandleUpdateRequest updateRequest = new FileHandleUpdateRequest();
		
		updateRequest.setOldFileHandleId(oldFileHandleId);
		updateRequest.setNewFileHandleId(newFileHandleId);
		
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleById(any(), any())).thenReturn(AuthorizationStatus.authorized());
		
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.file);
		when(mockNodeDao.getFileHandleIdForVersion(any(), any())).thenReturn(oldFileHandleId);
		when(mockFileHandleDao.isMatchingMD5(any(), any())).thenReturn(true);
		when(mockNodeDao.getParentId(any())).thenReturn(parentId);
		doNothing().when(mockStsManager).validateCanAddFile(any(), any(), any());
		when(mockNodeDao.updateRevisionFileHandle(any(), any(), any())).thenReturn(true);
	
		// Call under test
		nodeManager.updateNodeFileHandle(mockUserInfo, nodeId, versionNumber, updateRequest);
		
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
		verify(mockAuthManager).canAccessRawFileHandleById(mockUserInfo, newFileHandleId);
		
		verify(mockNodeDao).getNodeTypeById(nodeId);
		verify(mockNodeDao).lockNode(nodeId);
		verify(mockNodeDao).getFileHandleIdForVersion(nodeId, versionNumber);
		verify(mockFileHandleDao).isMatchingMD5(oldFileHandleId, newFileHandleId);
		verify(mockNodeDao).getParentId(nodeId);
		verify(mockStsManager).validateCanAddFile(mockUserInfo, newFileHandleId, parentId);
		verify(mockNodeDao).touch(mockUserInfo.getId(), nodeId);
		verify(mockNodeDao).updateRevisionFileHandle(nodeId, versionNumber, newFileHandleId);
		
		verifyNoMoreInteractions(mockAuthManager);
		verifyNoMoreInteractions(mockNodeDao);
		verifyNoMoreInteractions(mockFileHandleDao);
		verifyNoMoreInteractions(mockStsManager);
		
	}
	
	@Test
	public void testUpdateNodeFileHandleWithNoReadAccess() {
		String nodeId = this.nodeId;
		Long versionNumber = 1L;
		String oldFileHandleId = "123";
		String newFileHandleId = "456";
		
		FileHandleUpdateRequest updateRequest = new FileHandleUpdateRequest();
		
		updateRequest.setOldFileHandleId(oldFileHandleId);
		updateRequest.setNewFileHandleId(newFileHandleId);
		
		when(mockAuthManager.canAccess(any(), any(), any(), eq(ACCESS_TYPE.READ))).thenReturn(AuthorizationStatus.accessDenied("Denied"));
		
		String errorMessage = assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			nodeManager.updateNodeFileHandle(mockUserInfo, nodeId, versionNumber, updateRequest);
		}).getMessage();
		
		assertEquals("Denied", errorMessage);
		
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		
		verifyNoMoreInteractions(mockAuthManager);
		verifyNoMoreInteractions(mockNodeDao);
		verifyNoMoreInteractions(mockFileHandleDao);
		verifyNoMoreInteractions(mockStsManager);
	}
	
	@Test
	public void testUpdateNodeFileHandleWithNoUpdateAccess() {
		String nodeId = this.nodeId;
		Long versionNumber = 1L;
		String oldFileHandleId = "123";
		String newFileHandleId = "456";
		
		FileHandleUpdateRequest updateRequest = new FileHandleUpdateRequest();
		
		updateRequest.setOldFileHandleId(oldFileHandleId);
		updateRequest.setNewFileHandleId(newFileHandleId);
		
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccess(any(), any(), any(), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationStatus.accessDenied("Denied"));
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.file);
		when(mockNodeDao.getFileHandleIdForVersion(any(), any())).thenReturn(oldFileHandleId);
		
		String errorMessage = assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			nodeManager.updateNodeFileHandle(mockUserInfo, nodeId, versionNumber, updateRequest);
		}).getMessage();
		
		assertEquals("Denied", errorMessage);
		
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
		verify(mockNodeDao).getNodeTypeById(nodeId);
		verify(mockNodeDao).lockNode(nodeId);
		verify(mockNodeDao).getFileHandleIdForVersion(nodeId, versionNumber);
		
		verifyNoMoreInteractions(mockAuthManager);
		verifyNoMoreInteractions(mockNodeDao);
		verifyNoMoreInteractions(mockFileHandleDao);
		verifyNoMoreInteractions(mockStsManager);
	}
	
	@Test
	public void testUpdateNodeFileHandleWithNoRawFileHandleAccess() {
		String nodeId = this.nodeId;
		Long versionNumber = 1L;
		String oldFileHandleId = "123";
		String newFileHandleId = "456";
		
		FileHandleUpdateRequest updateRequest = new FileHandleUpdateRequest();
		
		updateRequest.setOldFileHandleId(oldFileHandleId);
		updateRequest.setNewFileHandleId(newFileHandleId);
		
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleById(mockUserInfo, newFileHandleId)).thenReturn(AuthorizationStatus.accessDenied("Denied"));
		
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.file);
		when(mockNodeDao.getFileHandleIdForVersion(any(), any())).thenReturn(oldFileHandleId);
		
		String errorMessage = assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			nodeManager.updateNodeFileHandle(mockUserInfo, nodeId, versionNumber, updateRequest);
		}).getMessage();
		
		assertEquals("Denied", errorMessage);
		
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
		verify(mockAuthManager).canAccessRawFileHandleById(mockUserInfo, newFileHandleId);
		
		verify(mockNodeDao).getNodeTypeById(nodeId);
		verify(mockNodeDao).lockNode(nodeId);
		verify(mockNodeDao).getFileHandleIdForVersion(nodeId, versionNumber);
		
		verifyNoMoreInteractions(mockAuthManager);
		verifyNoMoreInteractions(mockNodeDao);
		verifyNoMoreInteractions(mockFileHandleDao);
		verifyNoMoreInteractions(mockStsManager);
	}
	
	@Test
	public void testUpdateNodeFileHandleWithWrongType() {
		String nodeId = this.nodeId;
		Long versionNumber = 1L;
		String oldFileHandleId = "123";
		String newFileHandleId = "456";
		
		FileHandleUpdateRequest updateRequest = new FileHandleUpdateRequest();
		
		updateRequest.setOldFileHandleId(oldFileHandleId);
		updateRequest.setNewFileHandleId(newFileHandleId);
		
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.project);
		
		String errorMessage = assertThrows(NotFoundException.class, () -> {			
			// Call under test
			nodeManager.updateNodeFileHandle(mockUserInfo, nodeId, versionNumber, updateRequest);
		}).getMessage();
		
		assertEquals("A file entity with id 123 and revision 1 does not exist.", errorMessage);
		
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		
		verify(mockNodeDao).getNodeTypeById(nodeId);
		verify(mockNodeDao).lockNode(nodeId);
		verify(mockNodeDao).getFileHandleIdForVersion(nodeId, versionNumber);
		
		verifyNoMoreInteractions(mockAuthManager);
		verifyNoMoreInteractions(mockNodeDao);
		verifyNoMoreInteractions(mockFileHandleDao);
		verifyNoMoreInteractions(mockStsManager);
	}
	
	@Test
	public void testUpdateNodeFileHandleWithWrongVersion() {
		String nodeId = this.nodeId;
		Long versionNumber = 1L;
		String oldFileHandleId = "123";
		String newFileHandleId = "456";
		
		FileHandleUpdateRequest updateRequest = new FileHandleUpdateRequest();
		
		updateRequest.setOldFileHandleId(oldFileHandleId);
		updateRequest.setNewFileHandleId(newFileHandleId);
		
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.file);
		when(mockNodeDao.getFileHandleIdForVersion(any(), any())).thenReturn(null);
		
		String errorMessage = assertThrows(NotFoundException.class, () -> {			
			// Call under test
			nodeManager.updateNodeFileHandle(mockUserInfo, nodeId, versionNumber, updateRequest);
		}).getMessage();
		
		assertEquals("A file entity with id 123 and revision 1 does not exist.", errorMessage);
		
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		
		verify(mockNodeDao).getNodeTypeById(nodeId);
		verify(mockNodeDao).lockNode(nodeId);
		verify(mockNodeDao).getFileHandleIdForVersion(nodeId, versionNumber);
		
		verifyNoMoreInteractions(mockAuthManager);
		verifyNoMoreInteractions(mockNodeDao);
		verifyNoMoreInteractions(mockFileHandleDao);
		verifyNoMoreInteractions(mockStsManager);
	}
	
	@Test
	public void testUpdateNodeFileHandleWithConflictingFileHandle() {
		String nodeId = this.nodeId;
		Long versionNumber = 1L;
		String oldFileHandleId = "123";
		String newFileHandleId = "456";
		
		FileHandleUpdateRequest updateRequest = new FileHandleUpdateRequest();
		
		updateRequest.setOldFileHandleId(oldFileHandleId);
		updateRequest.setNewFileHandleId(newFileHandleId);
		
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleById(any(), any())).thenReturn(AuthorizationStatus.authorized());
		
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.file);
		when(mockNodeDao.getFileHandleIdForVersion(any(), any())).thenReturn(oldFileHandleId + "_wrong");
	
		String errorMessage = assertThrows(ConflictingUpdateException.class, () -> {			
			// Call under test
			nodeManager.updateNodeFileHandle(mockUserInfo, nodeId, versionNumber, updateRequest);
		}).getMessage();
		
		assertEquals("The id of the provided file handle id (request.oldFileHandleId: 123) does not match the current file handle id.", errorMessage);
		
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
		verify(mockAuthManager).canAccessRawFileHandleById(mockUserInfo, newFileHandleId);
		
		verify(mockNodeDao).getNodeTypeById(nodeId);
		verify(mockNodeDao).lockNode(nodeId);
		verify(mockNodeDao).getFileHandleIdForVersion(nodeId, versionNumber);
		
		verifyNoMoreInteractions(mockAuthManager);
		verifyNoMoreInteractions(mockNodeDao);
		verifyNoMoreInteractions(mockFileHandleDao);
		verifyNoMoreInteractions(mockStsManager);
	}
	
	@Test
	public void testUpdateNodeFileHandleWithNoChanges() {
		String nodeId = this.nodeId;
		Long versionNumber = 1L;
		String oldFileHandleId = "123";
		String newFileHandleId = oldFileHandleId;
		
		FileHandleUpdateRequest updateRequest = new FileHandleUpdateRequest();
		
		updateRequest.setOldFileHandleId(oldFileHandleId);
		updateRequest.setNewFileHandleId(newFileHandleId);
		
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleById(any(), any())).thenReturn(AuthorizationStatus.authorized());
		
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.file);
		when(mockNodeDao.getFileHandleIdForVersion(any(), any())).thenReturn(oldFileHandleId);
	
		// Call under test
		nodeManager.updateNodeFileHandle(mockUserInfo, nodeId, versionNumber, updateRequest);
		
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
		verify(mockAuthManager).canAccessRawFileHandleById(mockUserInfo, newFileHandleId);
		
		verify(mockNodeDao).getNodeTypeById(nodeId);
		verify(mockNodeDao).lockNode(nodeId);
		verify(mockNodeDao).getFileHandleIdForVersion(nodeId, versionNumber);
		
		verifyNoMoreInteractions(mockAuthManager);
		verifyNoMoreInteractions(mockNodeDao);
		verifyNoMoreInteractions(mockFileHandleDao);
		verifyNoMoreInteractions(mockStsManager);
	}
	
	@Test
	public void testUpdateNodeFileHandleWithMismatchingMD5() {
		String nodeId = this.nodeId;
		Long versionNumber = 1L;
		String oldFileHandleId = "123";
		String newFileHandleId = "456";
		
		FileHandleUpdateRequest updateRequest = new FileHandleUpdateRequest();
		
		updateRequest.setOldFileHandleId(oldFileHandleId);
		updateRequest.setNewFileHandleId(newFileHandleId);
		
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleById(any(), any())).thenReturn(AuthorizationStatus.authorized());
		
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.file);
		when(mockNodeDao.getFileHandleIdForVersion(any(), any())).thenReturn(oldFileHandleId);
		when(mockFileHandleDao.isMatchingMD5(any(), any())).thenReturn(false);
		
		String errorMessage = assertThrows(ConflictingUpdateException.class, () -> {			
			// Call under test
			nodeManager.updateNodeFileHandle(mockUserInfo, nodeId, versionNumber, updateRequest);
		}).getMessage();
		
		assertEquals("The MD5 of the new file handle does not match the MD5 of the current file handle, a new version must be created.", errorMessage);
		
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
		verify(mockAuthManager).canAccessRawFileHandleById(mockUserInfo, newFileHandleId);
		
		verify(mockNodeDao).getNodeTypeById(nodeId);
		verify(mockNodeDao).lockNode(nodeId);
		verify(mockNodeDao).getFileHandleIdForVersion(nodeId, versionNumber);
		verify(mockFileHandleDao).isMatchingMD5(oldFileHandleId, newFileHandleId);
		
		verifyNoMoreInteractions(mockAuthManager);
		verifyNoMoreInteractions(mockNodeDao);
		verifyNoMoreInteractions(mockFileHandleDao);
		verifyNoMoreInteractions(mockStsManager);
	}
	
	@Test
	public void testUpdateNodeFileHandleWithUnsuccessfulUpdate() {
		String nodeId = this.nodeId;
		Long versionNumber = 1L;
		String oldFileHandleId = "123";
		String newFileHandleId = "456";
		
		FileHandleUpdateRequest updateRequest = new FileHandleUpdateRequest();
		
		updateRequest.setOldFileHandleId(oldFileHandleId);
		updateRequest.setNewFileHandleId(newFileHandleId);
		
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleById(any(), any())).thenReturn(AuthorizationStatus.authorized());
		
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.file);
		when(mockNodeDao.getFileHandleIdForVersion(any(), any())).thenReturn(oldFileHandleId);
		when(mockFileHandleDao.isMatchingMD5(any(), any())).thenReturn(true);
		when(mockNodeDao.getParentId(any())).thenReturn(parentId);
		doNothing().when(mockStsManager).validateCanAddFile(any(), any(), any());
		when(mockNodeDao.updateRevisionFileHandle(any(), any(), any())).thenReturn(false);
	
		String errorMesssage = assertThrows(ConflictingUpdateException.class, () -> {			
			// Call under test
			nodeManager.updateNodeFileHandle(mockUserInfo, nodeId, versionNumber, updateRequest);
		}).getMessage();
		
		assertEquals("Could not perform the update on node 123 with revision 1", errorMesssage);
		
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
		verify(mockAuthManager).canAccessRawFileHandleById(mockUserInfo, newFileHandleId);
		
		verify(mockNodeDao).getNodeTypeById(nodeId);
		verify(mockNodeDao).lockNode(nodeId);
		verify(mockNodeDao).getFileHandleIdForVersion(nodeId, versionNumber);
		verify(mockFileHandleDao).isMatchingMD5(oldFileHandleId, newFileHandleId);
		verify(mockNodeDao).getParentId(nodeId);
		verify(mockStsManager).validateCanAddFile(mockUserInfo, newFileHandleId, parentId);
		verify(mockNodeDao).touch(mockUserInfo.getId(), nodeId);
		verify(mockNodeDao).updateRevisionFileHandle(nodeId, versionNumber, newFileHandleId);
		
		verifyNoMoreInteractions(mockAuthManager);
		verifyNoMoreInteractions(mockNodeDao);
		verifyNoMoreInteractions(mockFileHandleDao);
		verifyNoMoreInteractions(mockStsManager);
		
	}
	
	@Test
	public void testUpdateNodeFileHandleWithStsValidationFailure() {
		String nodeId = this.nodeId;
		Long versionNumber = 1L;
		String oldFileHandleId = "123";
		String newFileHandleId = "456";
		
		FileHandleUpdateRequest updateRequest = new FileHandleUpdateRequest();
		
		updateRequest.setOldFileHandleId(oldFileHandleId);
		updateRequest.setNewFileHandleId(newFileHandleId);
		
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleById(any(), any())).thenReturn(AuthorizationStatus.authorized());
		
		when(mockNodeDao.getNodeTypeById(any())).thenReturn(EntityType.file);
		when(mockNodeDao.getFileHandleIdForVersion(any(), any())).thenReturn(oldFileHandleId);
		when(mockFileHandleDao.isMatchingMD5(any(), any())).thenReturn(true);
		when(mockNodeDao.getParentId(any())).thenReturn(parentId);
		
		IllegalArgumentException expectedException = new IllegalArgumentException("Some STS failure");
		
		doThrow(expectedException).when(mockStsManager).validateCanAddFile(any(), any(), any());
	
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			nodeManager.updateNodeFileHandle(mockUserInfo, nodeId, versionNumber, updateRequest);
		});
		
		assertEquals(expectedException, ex);
		
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockAuthManager).canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
		verify(mockAuthManager).canAccessRawFileHandleById(mockUserInfo, newFileHandleId);
		
		verify(mockNodeDao).getNodeTypeById(nodeId);
		verify(mockNodeDao).lockNode(nodeId);
		verify(mockNodeDao).getFileHandleIdForVersion(nodeId, versionNumber);
		verify(mockFileHandleDao).isMatchingMD5(oldFileHandleId, newFileHandleId);
		verify(mockNodeDao).getParentId(nodeId);
		verify(mockStsManager).validateCanAddFile(mockUserInfo, newFileHandleId, parentId);
		
		verifyNoMoreInteractions(mockAuthManager);
		verifyNoMoreInteractions(mockNodeDao);
		verifyNoMoreInteractions(mockFileHandleDao);
		verifyNoMoreInteractions(mockStsManager);
	}
	
}
