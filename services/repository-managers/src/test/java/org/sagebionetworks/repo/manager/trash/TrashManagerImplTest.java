package org.sagebionetworks.repo.manager.trash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.trash.TrashManagerImpl.MAX_IDS_TO_LOAD;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.dbo.trash.TrashCanDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class TrashManagerImplTest {
	private static final String FILE_HANDLE_ID = "test-file-handle-id";

	@Mock
	private AuthorizationManager mockAuthorizationManager;

	@Mock
	private NodeManager mockNodeManager;

	@Mock
	private NodeDAO mockNodeDAO;

	@Mock
	private AccessControlListDAO mockAclDAO;

	@Mock
	private ProjectSettingsManager mockProjectSettingsManager;

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
	private String newParentID;
	private Node testNode;
	private TrashedEntity nodeTrashedEntity;
	private String newEtag;

	private List<TrashedEntity> trashList;

	@BeforeEach
	public void setUp() throws Exception {

		userID = 12345L;
		userInfo = new UserInfo(false /* not admin */);
		userInfo.setId(userID);

		adminUserID = 67890L;
		adminUserInfo = new UserInfo(true);
		adminUserInfo.setId(adminUserID);

		nodeID = "syn420";
		nodeName = "testName.test";
		nodeParentID = "syn489";
		newParentID = "syn6789";
		testNode = new Node();
		testNode.setId(nodeID);
		testNode.setName(nodeName);
		testNode.setParentId(nodeParentID);
		testNode.setFileHandleId(FILE_HANDLE_ID);
		testNode.setNodeType(EntityType.file);
		nodeTrashedEntity = spy(new TrashedEntity());
		nodeTrashedEntity.setOriginalParentId(nodeParentID);
		nodeTrashedEntity.setEntityId(nodeID);
		nodeTrashedEntity.setEntityName(nodeName);
		nodeTrashedEntity.setDeletedOn(new Date(System.currentTimeMillis()));
		nodeTrashedEntity.setDeletedByPrincipalId(userInfo.getId().toString());

		trashList = new ArrayList<TrashedEntity>();

	}

	@Test
	public void doesEntityHaveTrashedChildren_True() {
		when(mockTrashCanDao.doesEntityHaveTrashedChildren(nodeParentID)).thenReturn(true);
		// Method under test.
		assertTrue(trashManager.doesEntityHaveTrashedChildren(nodeParentID));
	}

	@Test
	public void doesEntityHaveTrashedChildren_False() {
		when(mockTrashCanDao.doesEntityHaveTrashedChildren(nodeParentID)).thenReturn(false);
		// Method under test.
		assertFalse(trashManager.doesEntityHaveTrashedChildren(nodeParentID));
	}

	@Test
	public void testUpdateNodeForTrashCanNonContainer() {
		testNode.setNodeType(EntityType.file);
		testNode.setModifiedOn(new Date(0));
		testNode.setModifiedByPrincipalId(-1L);
		ArgumentCaptor<Node> nodeCapture = ArgumentCaptor.forClass(Node.class);
		ChangeType changeType = ChangeType.DELETE;
		// call under test
		trashManager.updateNodeForTrashCan(userInfo, testNode, changeType);
		verify(mockNodeDAO).touch(userInfo.getId(), testNode.getId(), changeType);
		// the entity is not a container so a message should not be sent.
		verify(mockTransactionalMessenger, never()).sendMessageAfterCommit(anyString(), any(ObjectType.class), any(ChangeType.class));
		verify(mockNodeDAO).updateNode(nodeCapture.capture());
		Node capturedNode = nodeCapture.getValue();
		assertNotNull(capturedNode);
		assertNotNull(capturedNode.getModifiedOn());
	}

	@Test
	public void testUpdateNodeForTrashCanContainer() {
		testNode.setNodeType(EntityType.folder);
		testNode.setModifiedOn(new Date(0));
		testNode.setModifiedByPrincipalId(-1L);
		ChangeType changeType = ChangeType.DELETE;
		// call under test
		trashManager.updateNodeForTrashCan(userInfo, testNode, changeType);
		verify(mockNodeDAO).touch(userInfo.getId(), testNode.getId(), changeType);
		// the entity is a container so a container event should be generated.
		verify(mockTransactionalMessenger).sendMessageAfterCommit(testNode.getId(), ObjectType.ENTITY_CONTAINER, changeType);
	}

	@Test
	public void testDeleteAllAclsInHierarchy() throws LimitExceededException {
		List<Long> parentIdsList = Lists.newArrayList(123L, 456L);
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

	@Test
	public void testDeleteAllAclsInHierarchyOverLimit() throws LimitExceededException {
		// setup limit exceeded.
		LimitExceededException exception = new LimitExceededException("too many");

		doThrow(exception).when(mockNodeDAO).getAllContainerIds(anyString(), anyInt());
		// call under test
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			trashManager.deleteAllAclsInHierarchy(nodeID);
		});
	}
	
	@Test
	public void testMoveToTrashWithNullUser() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			trashManager.moveToTrash(null, nodeID, false);
		});
	}

	@Test
	public void testMoveToTrashWithNullNodeID() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			trashManager.moveToTrash(userInfo, null, false);
		});
	}

	@Test
	public void testMoveToTrashNoAuthorization() {
		when(mockAuthorizationManager.canAccess(userInfo, nodeID, ObjectType.ENTITY, ACCESS_TYPE.DELETE))
				.thenReturn(AuthorizationStatus.accessDenied(""));

		when(mockNodeDAO.isNodeAvailable(anyString())).thenReturn(true);

		Assertions.assertThrows(UnauthorizedException.class, () -> {
			trashManager.moveToTrash(userInfo, nodeID, false);
		});
	}

	@Test
	public void testMoveToTrashAuthorized() {
		// setup
		when(mockNodeDAO.getNode(nodeID)).thenReturn(testNode);

		when(mockAuthorizationManager.canAccess(any(UserInfo.class), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class)))
				.thenReturn(AuthorizationStatus.authorized());

		when(mockNodeDAO.isNodeAvailable(anyString())).thenReturn(true);
		newEtag = "newEtag";
		when(mockNodeDAO.touch(any(Long.class), anyString(), any(ChangeType.class))).thenReturn(newEtag);
		when(mockNodeDAO.isNodeAvailable(anyString())).thenReturn(true);

		boolean priorityPurge = false;

		trashManager.moveToTrash(userInfo, nodeID, priorityPurge);

		verify(mockNodeDAO, times(1)).getNode(nodeID);
		verify(mockNodeDAO, times(1)).updateNode(testNode);

		verify(mockTrashCanDao, times(1)).create(userInfo.getId().toString(), nodeID, nodeName, nodeParentID, priorityPurge);

		verify(mockAclDAO).delete(anyListOf(Long.class), any(ObjectType.class));
	}

	@Test
	public void testRestoreFromTrashNullUser() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			trashManager.restoreFromTrash(null, nodeID, nodeParentID);// newParent (3rd one) is an optional parameter
		});
	}

	@Test
	public void testRestoreFromTrashNullNodeID() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			trashManager.restoreFromTrash(userInfo, null, nodeParentID);
		});
	}

	@Test
	public void testRestoreFromTrashBadNodeID() {
		final String badNodeID = "synFAKEID";
		when(mockTrashCanDao.getTrashedEntity(badNodeID)).thenReturn(null);

		Assertions.assertThrows(NotFoundException.class, () -> {
			trashManager.restoreFromTrash(userInfo, badNodeID, nodeParentID);
		});
	}

	@Test
	public void testRestoreFromTrashNotAdminAndNotDeletedByUser() {

		when(mockTrashCanDao.getTrashedEntity(nodeID)).thenReturn(nodeTrashedEntity);

		final String fakeDeletedByID = "synDEFINITELYNOTTHISUSER";
		nodeTrashedEntity.setDeletedByPrincipalId(fakeDeletedByID);

		Assertions.assertThrows(UnauthorizedException.class, () -> {
			trashManager.restoreFromTrash(userInfo, nodeID, nodeParentID);

		});

		// expected
		verify(mockTrashCanDao).getTrashedEntity(nodeID);
		verify(mockAuthorizationManager, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAuthorizationManager, never()).canUserMoveRestrictedEntity(any(UserInfo.class), anyString(), anyString());

	}

	@Test
	public void testRestoreFromTrashParentIDInTrash() {
		final String fakeNewParentID = "synFAKEPARENTID";
		when(mockNodeDAO.isNodeAvailable(fakeNewParentID)).thenReturn(false);
		
		when(mockTrashCanDao.getTrashedEntity(nodeID)).thenReturn(nodeTrashedEntity);

		Assertions.assertThrows(ParentInTrashCanException.class, () -> {
			trashManager.restoreFromTrash(userInfo, nodeID, fakeNewParentID);
		});
	}

	@Test
	public void testRestoreFromTrashUnauthourizedNewParent() {
		when(mockAuthorizationManager.canAccess(userInfo, nodeParentID, ObjectType.ENTITY, ACCESS_TYPE.CREATE))
				.thenReturn(AuthorizationStatus.accessDenied("I'm a teapot."));

		when(mockTrashCanDao.getTrashedEntity(nodeID)).thenReturn(nodeTrashedEntity);

		when(mockNodeDAO.isNodeAvailable(anyString())).thenReturn(true);

		Assertions.assertThrows(UnauthorizedException.class, () -> {
			trashManager.restoreFromTrash(userInfo, nodeID, nodeParentID);
		});

		// expected
		verify(mockTrashCanDao).getTrashedEntity(nodeID);
		verify(mockAuthorizationManager).canAccess(userInfo, nodeParentID, ObjectType.ENTITY, ACCESS_TYPE.CREATE);
		verify(mockAuthorizationManager, never()).canUserMoveRestrictedEntity(any(UserInfo.class), anyString(), anyString());

	}

	@Test
	public void testRestoreFromTrashUnauthorizedNodeID() {
		when(mockAuthorizationManager.canUserMoveRestrictedEntity(userInfo, nodeTrashedEntity.getOriginalParentId(), nodeParentID))
				.thenReturn(AuthorizationStatus.accessDenied("U can't touch this."));

		when(mockAuthorizationManager.canAccess(any(UserInfo.class), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class)))
				.thenReturn(AuthorizationStatus.authorized());

		when(mockTrashCanDao.getTrashedEntity(nodeID)).thenReturn(nodeTrashedEntity);

		when(mockNodeDAO.isNodeAvailable(anyString())).thenReturn(true);

		when(mockNodeDAO.getNode(nodeID)).thenReturn(testNode);

		Assertions.assertThrows(UnauthorizedException.class, () -> {
			trashManager.restoreFromTrash(userInfo, nodeID, nodeParentID);
		});
		// expected
		verify(mockTrashCanDao).getTrashedEntity(nodeID);
		verify(mockAuthorizationManager).canAccess(userInfo, nodeParentID, ObjectType.ENTITY, ACCESS_TYPE.CREATE);
		verify(mockAuthorizationManager).canUserMoveRestrictedEntity(userInfo, nodeTrashedEntity.getOriginalParentId(), nodeParentID);
		
	}

	@Test
	public void testRestoreFromTrashCan() {

		when(mockAuthorizationManager.canAccess(any(UserInfo.class), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class)))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAuthorizationManager.canUserMoveRestrictedEntity(any(UserInfo.class), anyString(), anyString()))
				.thenReturn(AuthorizationStatus.authorized());

		when(mockTrashCanDao.getTrashedEntity(nodeID)).thenReturn(nodeTrashedEntity);

		when(mockNodeDAO.isNodeAvailable(anyString())).thenReturn(true);

		when(mockNodeDAO.getNode(nodeID)).thenReturn(testNode);

		trashManager.restoreFromTrash(userInfo, nodeID, nodeParentID);

		verify(mockNodeDAO, times(1)).updateNode(testNode);
		verify(mockTrashCanDao).delete(Collections.singletonList(KeyFactory.stringToKey(nodeID)));
		verify(mockAclDAO, never()).create(any(AccessControlList.class), any(ObjectType.class));
	}

	/**
	 * Test restoring a project to root.
	 * 
	 */
	@Test
	public void testRestoreFromTrashProjectNewParentRoot() {

		when(mockAuthorizationManager.canAccess(any(UserInfo.class), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class)))
				.thenReturn(AuthorizationStatus.authorized());

		when(mockAuthorizationManager.canUserMoveRestrictedEntity(any(UserInfo.class), anyString(), anyString()))
				.thenReturn(AuthorizationStatus.authorized());

		when(mockTrashCanDao.getTrashedEntity(nodeID)).thenReturn(nodeTrashedEntity);

		when(mockNodeDAO.isNodeAvailable(anyString())).thenReturn(true);

		when(mockNodeDAO.getNode(nodeID)).thenReturn(testNode);

		testNode.setNodeType(EntityType.project);
		// call under test - move the entity to root.
		trashManager.restoreFromTrash(userInfo, nodeID, NodeUtils.ROOT_ENTITY_ID);

		verify(mockNodeDAO, times(1)).updateNode(testNode);
		verify(mockTrashCanDao).delete(Collections.singletonList(KeyFactory.stringToKey(nodeID)));
		// An ACL should be created for the project
		verify(mockAclDAO).create(any(AccessControlList.class), eq(ObjectType.ENTITY));
	}

	/**
	 * Test restoring a non-project to root.
	 * 
	 */
	@Test
	public void testRestoreFromTrashNonProjectNewParentRoot() {

		when(mockAuthorizationManager.canAccess(any(UserInfo.class), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class)))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAuthorizationManager.canUserMoveRestrictedEntity(any(UserInfo.class), anyString(), anyString()))
				.thenReturn(AuthorizationStatus.authorized());

		when(mockTrashCanDao.getTrashedEntity(nodeID)).thenReturn(nodeTrashedEntity);

		when(mockNodeDAO.isNodeAvailable(anyString())).thenReturn(true);

		when(mockNodeDAO.getNode(nodeID)).thenReturn(testNode);

		testNode.setNodeType(EntityType.folder);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// call under test - move the entity to root.
			trashManager.restoreFromTrash(userInfo, nodeID, NodeUtils.ROOT_ENTITY_ID);
		});
	}

	@Test
	public void testRestoreFromTrash_movedFileToParentWithNoProjectSettings() {
		// Mock dependencies.
		when(mockAuthorizationManager.canAccess(userInfo, newParentID, ObjectType.ENTITY, ACCESS_TYPE.CREATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAuthorizationManager.canUserMoveRestrictedEntity(userInfo, nodeParentID, newParentID))
				.thenReturn(AuthorizationStatus.authorized());

		when(mockTrashCanDao.getTrashedEntity(nodeID)).thenReturn(nodeTrashedEntity);
		when(mockNodeDAO.isNodeAvailable(newParentID)).thenReturn(true);

		testNode.setNodeType(EntityType.file);
		when(mockNodeDAO.getNode(nodeID)).thenReturn(testNode);

		when(mockProjectSettingsManager.getProjectSettingForNode(userInfo, newParentID, ProjectSettingsType.upload,
				UploadDestinationListSetting.class)).thenReturn(Optional.empty());

		// Method under test - Doesn't throw.
		trashManager.restoreFromTrash(userInfo, nodeID, newParentID);
	}

	@Test
	public void testRestoreFromTrash_movedFileToParentWithStsFalse() {
		// Mock dependencies.
		when(mockAuthorizationManager.canAccess(userInfo, newParentID, ObjectType.ENTITY, ACCESS_TYPE.CREATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAuthorizationManager.canUserMoveRestrictedEntity(userInfo, nodeParentID, newParentID))
				.thenReturn(AuthorizationStatus.authorized());

		when(mockTrashCanDao.getTrashedEntity(nodeID)).thenReturn(nodeTrashedEntity);
		when(mockNodeDAO.isNodeAvailable(newParentID)).thenReturn(true);

		testNode.setNodeType(EntityType.file);
		when(mockNodeDAO.getNode(nodeID)).thenReturn(testNode);

		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		when(mockProjectSettingsManager.getProjectSettingForNode(userInfo, newParentID, ProjectSettingsType.upload,
				UploadDestinationListSetting.class)).thenReturn(Optional.of(projectSetting));
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(false);

		// Method under test - Doesn't throw.
		trashManager.restoreFromTrash(userInfo, nodeID, newParentID);
	}

	@Test
	public void testRestoreFromTrash_movedFileToParentWithStsTrue() {
		// Mock dependencies.
		when(mockAuthorizationManager.canAccess(userInfo, newParentID, ObjectType.ENTITY, ACCESS_TYPE.CREATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAuthorizationManager.canUserMoveRestrictedEntity(userInfo, nodeParentID, newParentID))
				.thenReturn(AuthorizationStatus.authorized());

		when(mockTrashCanDao.getTrashedEntity(nodeID)).thenReturn(nodeTrashedEntity);
		when(mockNodeDAO.isNodeAvailable(newParentID)).thenReturn(true);

		testNode.setNodeType(EntityType.file);
		when(mockNodeDAO.getNode(nodeID)).thenReturn(testNode);

		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		when(mockProjectSettingsManager.getProjectSettingForNode(userInfo, newParentID, ProjectSettingsType.upload,
				UploadDestinationListSetting.class)).thenReturn(Optional.of(projectSetting));
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(true);

		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> trashManager.restoreFromTrash(userInfo,
				nodeID, newParentID));
		assertEquals("Entities can be restored to STS-enabled folders only if that were its original parent",
				ex.getMessage());
	}

	@Test
	public void testRestoreFromTrash_movedFolderToParentWithStsTrue() {
		// Mock dependencies.
		when(mockAuthorizationManager.canAccess(userInfo, newParentID, ObjectType.ENTITY, ACCESS_TYPE.CREATE))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAuthorizationManager.canUserMoveRestrictedEntity(userInfo, nodeParentID, newParentID))
				.thenReturn(AuthorizationStatus.authorized());

		when(mockTrashCanDao.getTrashedEntity(nodeID)).thenReturn(nodeTrashedEntity);
		when(mockNodeDAO.isNodeAvailable(newParentID)).thenReturn(true);

		testNode.setNodeType(EntityType.folder);
		when(mockNodeDAO.getNode(nodeID)).thenReturn(testNode);

		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		when(mockProjectSettingsManager.getProjectSettingForNode(userInfo, newParentID, ProjectSettingsType.upload,
				UploadDestinationListSetting.class)).thenReturn(Optional.of(projectSetting));
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(true);

		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> trashManager.restoreFromTrash(userInfo,
				nodeID, newParentID));
		assertEquals("Entities can be restored to STS-enabled folders only if that were its original parent",
				ex.getMessage());
	}

	@Test
	public void testListTrashedEntitiesNullCurrentUser() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			trashManager.listTrashedEntities(null, userInfo, 1, 1);
		});
	}

	@Test
	public void testListTrashedEntitiesNullOtherUser() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			trashManager.listTrashedEntities(userInfo, null, 1, 1);
		});
	}

	@Test
	public void testListTrashedEntitiesNegativeOffset() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			trashManager.listTrashedEntities(userInfo, userInfo, -1, 1);
		});
	}

	@Test
	public void testListTrashedEntitiesNegativeLimit() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			trashManager.listTrashedEntities(userInfo, userInfo, 1, -1);
		});
	}

	@Test
	public void testListTrashedEntitiesWhenCurrentUserIsNotAdminAndDifferentOtherUser() {
		final long tempUserID = 1234567890L;
		UserInfo tempUser = new UserInfo(false);
		tempUser.setId(tempUserID);

		Assertions.assertThrows(UnauthorizedException.class, () -> {
			trashManager.listTrashedEntities(userInfo, tempUser, 1, 1);
		});
	}

	@Test
	public void testListTrashedEntities() {
		final long limit = 1;
		final long offset = 0;
		when(mockTrashCanDao.listTrashedEntities(userInfo.getId().toString(), offset, limit)).thenReturn(trashList);
		List<TrashedEntity> results = trashManager.listTrashedEntities(userInfo, userInfo, offset, limit);
		assertEquals(trashList, results);
	}

	@Test
	public void testPurgeTrashNullList() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			trashManager.purgeTrash(adminUserInfo, null);
		});
	}

	@Test
	public void testPurgeTrashNullUser() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			trashManager.purgeTrash(null, Arrays.asList(1L));
		});
	}

	@Test
	public void testPurgeTrashUserNotAdmin() {
		Assertions.assertThrows(UnauthorizedException.class, () -> {
			trashManager.purgeTrash(userInfo, Arrays.asList(1L));
		});
	}

	@Test
	public void testPurgeTrash() {

		when(mockNodeDAO.deleteTree(nodeID, MAX_IDS_TO_LOAD)).thenReturn(true);

		List<Long> trashIDList = Collections.singletonList(KeyFactory.stringToKey(nodeID));

		trashManager.purgeTrash(adminUserInfo, trashIDList);

		verify(mockNodeDAO, times(1)).deleteTree(nodeID, MAX_IDS_TO_LOAD);
		verify(mockAclDAO, times(1)).delete(nodeID, ObjectType.ENTITY);
		verify(mockTrashCanDao, times(1)).delete(trashIDList);
	}

	@Test
	public void testGetTrashLeavesBefore() {
		final long daysBefore = 1;
		final long limit = 1234;
		List<Long> trashIdList = new ArrayList<Long>();
		when(mockTrashCanDao.getTrashLeavesIds(daysBefore, limit)).thenReturn(trashIdList);

		assertEquals(trashManager.getTrashLeavesBefore(daysBefore, limit), trashIdList);
		verify(mockTrashCanDao, times(1)).getTrashLeavesIds(daysBefore, limit);
	}

	@Test
	public void testMoveToTrashAlreadyTrashed() {
		// not available means deleted or does not exist
		when(mockNodeDAO.isNodeAvailable(nodeID)).thenReturn(false);
		// call under test
		trashManager.moveToTrash(userInfo, nodeID, false);
		verify(mockNodeDAO).isNodeAvailable(nodeID);
		verifyNoMoreInteractions(mockAuthorizationManager);
	}
	
	@Test
	public void testFlagForPurge() {
		
		when(mockTrashCanDao.getTrashedEntity(nodeID)).thenReturn(nodeTrashedEntity);
		
		trashManager.flagForPurge(userInfo, nodeID);
		
		verify(mockTrashCanDao).flagForPurge(Arrays.asList(KeyFactory.stringToKey(nodeID)));
	}
	
	@Test
	public void testFlagForPurgeAsAdmin() {
		
		when(mockTrashCanDao.getTrashedEntity(nodeID)).thenReturn(nodeTrashedEntity);
		
		trashManager.flagForPurge(adminUserInfo, nodeID);
		
		verify(mockTrashCanDao).flagForPurge(Arrays.asList(KeyFactory.stringToKey(nodeID)));
	}
	
	@Test
	public void testFlagForPurgeUnauthorized() {
		
		when(mockTrashCanDao.getTrashedEntity(nodeID)).thenReturn(nodeTrashedEntity);
		
		UserInfo tempUser = new UserInfo(false);
		tempUser.setId(123L);
		
		Assertions.assertThrows(UnauthorizedException.class, () -> {
			trashManager.flagForPurge(tempUser, nodeID);
		});
		
		verifyNoMoreInteractions(mockTrashCanDao);
	}

}
