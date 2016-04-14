package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.NodeManager.FileHandleReason;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACL_SCHEME;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.RequesterPaysSetting;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Validate that authorization checks are in place.
 * @author jmhill
 *
 */
public class NodeManagerAuthorizationTest {
	
	private NodeDAO mockNodeDao = null;
	private AuthorizationManager mockAuthDao = null;
	private NodeManagerImpl nodeManager = null;
	private AccessControlListDAO mockAclDao = null;
	private Node mockNode;
	private Annotations mockAnnotations;
	private NamedAnnotations mockNamed;
	private UserGroup mockUserGroup;
	private UserInfo mockUserInfo;	
	private EntityBootstrapper mockEntityBootstrapper;
	private NodeInheritanceManager mockInheritanceManager;
	private ActivityManager mockActivityManager;
	private ProjectSettingsManager mockProjectSettingsManager;;

	@Before
	public void before() throws NotFoundException, DatastoreException{
		mockNodeDao = Mockito.mock(NodeDAO.class);
		mockAuthDao = Mockito.mock(AuthorizationManager.class);
		// Say no to everything.
		mockAclDao = Mockito.mock(AccessControlListDAO.class);
		mockEntityBootstrapper = Mockito.mock(EntityBootstrapper.class);
		mockInheritanceManager = Mockito.mock(NodeInheritanceManager.class);
		mockActivityManager = Mockito.mock(ActivityManager.class);
		mockProjectSettingsManager = Mockito.mock(ProjectSettingsManager.class);
		// Create the manager dao with mocked dependent daos.
		nodeManager = new NodeManagerImpl(mockNodeDao, mockAuthDao, mockAclDao, mockEntityBootstrapper, mockInheritanceManager, null,
				mockActivityManager, mockProjectSettingsManager);
		// The mocks user for tests
		mockNode = Mockito.mock(Node.class);
		when(mockNode.getNodeType()).thenReturn(EntityType.project);
		when(mockNode.getName()).thenReturn("BobTheNode");
		mockAnnotations = Mockito.mock(Annotations.class);
		when(mockAnnotations.getEtag()).thenReturn("12");
		mockNamed = Mockito.mock(NamedAnnotations.class);
		when(mockNamed.getEtag()).thenReturn("12");

		// UserGroup
		mockUserGroup = Mockito.mock(UserGroup.class);
		when(mockUserGroup.getId()).thenReturn("123");
		mockUserInfo = new UserInfo(false, 123L);
		
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedCreateNewNode() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		when(mockAuthDao.canCreate(mockUserInfo, mockNode)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		// Should fail
		nodeManager.createNewNode(mockNode, mockUserInfo);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedCreateNewNodeFileHandle() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		// The user is allowed to create the file handle but not allowed to use the file handle.
		String fileHandleId = "123456";
		when(mockAuthDao.canCreate(mockUserInfo, mockNode)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthDao.canAccessRawFileHandleById(mockUserInfo, fileHandleId)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		when(mockNode.getFileHandleId()).thenReturn(fileHandleId);
		// Should fail
		nodeManager.createNewNode(mockNode, mockUserInfo);
	}
	
	@Test 
	public void testAuthorizedCreateNewNodeFileHandle() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		// The user is allowed to create the file handle but not allowed to use the file handle.
		String fileHandleId = "123456";
		when(mockAuthDao.canCreate(mockUserInfo, mockNode)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthDao.canAccessRawFileHandleById(mockUserInfo, fileHandleId)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockNode.getFileHandleId()).thenReturn(fileHandleId);
		when(mockEntityBootstrapper.getChildAclSchemeForPath(any(String.class))).thenReturn(ACL_SCHEME.INHERIT_FROM_PARENT);
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPLOAD)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockNodeDao.createNewNode(mockNode)).thenReturn(mockNode);
		// Should fail
		nodeManager.createNewNode(mockNode, mockUserInfo);
		verify(mockNodeDao).createNewNode(mockNode);
	}
	
	/**
	 * Test the case where the user has update permission on the node but they did not create the file handle so they cannot
	 * assign it to the node.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test
	public void testUnauthorizedUpdateNodeFileHandle() throws DatastoreException, NotFoundException{
		String fileHandleId = "123456";
		String oldFileHandleId = "9876";
		String parentId = "123";
		String nodeId = "456";
		when(mockNode.getId()).thenReturn(nodeId);
		// The user can update the node.
		when(mockAuthDao.canAccess(mockUserInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		// The old file handle does not match the new file handle.
		when(mockNodeDao.getFileHandleIdForVersion(mockNode.getId(), null)).thenReturn(oldFileHandleId);
		// The user did not create the file handle.
		when(mockAuthDao.canAccessRawFileHandleById(mockUserInfo, fileHandleId)).thenReturn(
				AuthorizationManagerUtil.accessDenied(mockUserInfo.getId().toString()+" cannot access "+fileHandleId));
		when(mockNode.getFileHandleId()).thenReturn(fileHandleId);
		when(mockNode.getParentId()).thenReturn(parentId);
		when(mockAuthDao.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(parentId))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		Node oldMockNode = mock(Node.class);
		when(oldMockNode.getParentId()).thenReturn(parentId);
		when(mockNodeDao.getNode(nodeId)).thenReturn(oldMockNode);
		// Should fail
		try{
			nodeManager.update(mockUserInfo, mockNode);
			fail("Should have failed");
		}catch(UnauthorizedException e){
			assertTrue("The exception message should contain the file handle id: "+e.getMessage(),e.getMessage().indexOf(fileHandleId) >= 0);
			assertTrue("The exception message should contain the user's id: "+e.getMessage(),e.getMessage().indexOf(mockUserInfo.getId().toString()) >= 0);
		}
	}
	
	/**
	 * Test the case where the user has update permission on a node that already had an file handle.
	 * In this case the file handle currently on the node was created by someone else so the current user would not
	 * be able to set it.  However, since the file handle is not changing, the user should be allowed to proceed with the update.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test
	public void testAuthorizedUpdateNodeFileHandleNotChanged() throws DatastoreException, NotFoundException{
		String parentId = "123";
		String nodeId = "456";
		when(mockNode.getId()).thenReturn(nodeId);
		String fileHandleId = "123456";
		// The user can update the node.
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		// The file handle was already set on this node so it is not changing with this update.
		when(mockNodeDao.getFileHandleIdForVersion(mockNode.getId(), null)).thenReturn(fileHandleId);
		// If the user were to set this file handle it would fail as they are not the creator of the file handle.
		when(mockAuthDao.canAccessRawFileHandleById(mockUserInfo, fileHandleId)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		when(mockNode.getFileHandleId()).thenReturn(fileHandleId);
		when(mockNode.getParentId()).thenReturn(parentId);
		when(mockAuthDao.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(parentId))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		Node oldMockNode = mock(Node.class);
		when(oldMockNode.getParentId()).thenReturn(parentId);
		when(mockNodeDao.getNode(nodeId)).thenReturn(oldMockNode);
		// Should fail
		nodeManager.update(mockUserInfo, mockNode);
		// The change should make it to the dao
		verify(mockNodeDao).updateNode(mockNode);
	}
	
	/**
	 * For this case the user has update permission on the node.  This update is changing the file handle
	 * and the user has permission to use the new file handle, so the update should succeed.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test
	public void testAuthorizedUpdateNodeFileHandleChanged() throws DatastoreException, NotFoundException{
		String fileHandleId = "123456";
		String oldFileHandleId = "9876";
		String parentId = "123";
		String nodeId = "456";
		when(mockNode.getId()).thenReturn(nodeId);
		// The user can update the node.
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		// The current file handle on the node does not match the new handle.
		when(mockNodeDao.getFileHandleIdForVersion(mockNode.getId(), null)).thenReturn(oldFileHandleId);
		// The user can access the new file handle.
		when(mockAuthDao.canAccessRawFileHandleById(mockUserInfo, fileHandleId)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockNode.getFileHandleId()).thenReturn(fileHandleId);
		when(mockNode.getParentId()).thenReturn(parentId);
		when(mockAuthDao.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(parentId))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthDao.canAccess(mockUserInfo, parentId, ObjectType.ENTITY, ACCESS_TYPE.UPLOAD)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		Node oldMockNode = mock(Node.class);
		when(oldMockNode.getParentId()).thenReturn(parentId);
		when(mockNodeDao.getNode(nodeId)).thenReturn(oldMockNode);
		// Should fail
		nodeManager.update(mockUserInfo, mockNode);
		// The change should make it to the dao
		verify(mockNodeDao).updateNode(mockNode);
	}
	
	/**
	 * For this case the user has read access on the node but not download access.
	 * 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedGetFileHandle1() throws DatastoreException, NotFoundException{
		// The user has access to read the node
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		// The user does not have permission to dowload the file
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		nodeManager.getFileHandleIdForVersion(mockUserInfo, mockNode.getId(), null, FileHandleReason.FOR_HANDLE_VIEW);
	}
	
	/**
	 * For this case the user has access, but it is requester pays.
	 * 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test(expected = UnauthorizedException.class)
	public void testRequesterPaysGetFileHandle() throws DatastoreException, NotFoundException {
		// The user has access to read the node
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(
				AuthorizationManagerUtil.AUTHORIZED);
		// The user does not have permission to dowload the file
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD)).thenReturn(
				AuthorizationManagerUtil.AUTHORIZED);
		RequesterPaysSetting setting = new RequesterPaysSetting();
		setting.setRequesterPays(true);
		when(
				mockProjectSettingsManager.getProjectSettingForNode(mockUserInfo, mockNode.getId(), ProjectSettingsType.requester_pays,
						RequesterPaysSetting.class)).thenReturn(setting);
		nodeManager.getFileHandleIdForVersion(mockUserInfo, mockNode.getId(), null, FileHandleReason.FOR_FILE_DOWNLOAD);
	}

	/**
	 * Not found when there is not file handle id.
	 * 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test (expected=NotFoundException.class)
	public void testNotFoundGetFileHandle() throws DatastoreException, NotFoundException{
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockNodeDao.getFileHandleIdForVersion(mockNode.getId(), null)).thenReturn(null);
		nodeManager.getFileHandleIdForVersion(mockUserInfo, mockNode.getId(), null, FileHandleReason.FOR_HANDLE_VIEW);
	}
	
	@Test
	public void testGetFileHandleIdForCurrentVersion() throws DatastoreException, NotFoundException{
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		String expectedFileHandleId = "999999";
		when(mockNodeDao.getFileHandleIdForVersion(mockNode.getId(), null)).thenReturn(expectedFileHandleId);
		String handleId = nodeManager.getFileHandleIdForVersion(mockUserInfo, mockNode.getId(), null, FileHandleReason.FOR_HANDLE_VIEW);
		assertEquals(expectedFileHandleId, handleId);
	}

	@Test
	public void testGetFileHandleIdForCurrentVersionRequesterPaysFalse() throws DatastoreException, NotFoundException {
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(
				AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD)).thenReturn(
				AuthorizationManagerUtil.AUTHORIZED);
		RequesterPaysSetting setting = new RequesterPaysSetting();
		setting.setRequesterPays(false);
		when(
				mockProjectSettingsManager.getProjectSettingForNode(mockUserInfo, mockNode.getId(), ProjectSettingsType.requester_pays,
						RequesterPaysSetting.class)).thenReturn(setting);
		String expectedFileHandleId = "999999";
		when(mockNodeDao.getFileHandleIdForVersion(mockNode.getId(), null)).thenReturn(expectedFileHandleId);
		String handleId = nodeManager.getFileHandleIdForVersion(mockUserInfo, mockNode.getId(), null, FileHandleReason.FOR_FILE_DOWNLOAD);
		assertEquals(expectedFileHandleId, handleId);
	}

	/**
	 * For this case the user has download access to the node but not read.
	 * 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedGetFileHandle2() throws DatastoreException, NotFoundException{
		// The user does not have access to read the node
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		// The user does have permission to dowload the file
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		nodeManager.getFileHandleIdForVersion(mockUserInfo, mockNode.getId(), null, FileHandleReason.FOR_HANDLE_VIEW);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedDeleteNode() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.DELETE)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		// Should fail
		nodeManager.delete(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedGetNode() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		// Should fail
		nodeManager.get(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedGetNodeForVersionNumber() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		// Should fail
		nodeManager.getNodeForVersionNumber(mockUserInfo, id, new Long(1));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedUpdate() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockNode.getId()).thenReturn(id);
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		// Should fail
		nodeManager.update(mockUserInfo, mockNode);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedUpdate2() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockNode.getId()).thenReturn(id);
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		// Should fail
		nodeManager.update(mockUserInfo, mockNode, mockNamed, true);
	}
	
	@Test
	public void testUnauthorizedUpdateDueToAccessRequirements() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		String parentId = "123";
		when(mockNode.getId()).thenReturn(id);
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockNode.getParentId()).thenReturn(parentId);
		// can't move due to access restrictions
		when(mockAuthDao.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(parentId))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		Node oldMockNode = mock(Node.class);
		when(oldMockNode.getParentId()).thenReturn(parentId);
		when(mockNodeDao.getNode(id)).thenReturn(oldMockNode);
		// OK!
		nodeManager.update(mockUserInfo, mockNode, null, true);
		// can't move due to access restrictions
		when(mockAuthDao.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(parentId))).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		try {
			// Should fail
			nodeManager.update(mockUserInfo, mockNode, null, true);
			fail("Excpected unauthorized exception");
		} catch (UnauthorizedException e) {
			// as expected
		}
		verify(mockAuthDao, times(2)).canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(parentId));
	}
	
	@Test
	public void testUnauthorizedUpdateDueToAliasChange() throws DatastoreException, InvalidModelException, NotFoundException,
			UnauthorizedException, ConflictingUpdateException {
		String id = "22";
		String parentId = "123";
		when(mockNode.getId()).thenReturn(id);
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockNode.getParentId()).thenReturn(parentId);
		// can't move due to access restrictions
		when(mockAuthDao.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(parentId))).thenReturn(
				AuthorizationManagerUtil.AUTHORIZED);
		Node oldMockNode = mock(Node.class);
		when(oldMockNode.getParentId()).thenReturn(parentId);
		when(oldMockNode.getAlias()).thenReturn("alias2");
		when(mockNodeDao.getNode(id)).thenReturn(oldMockNode);
		when(mockAuthDao.canChangeSettings(mockUserInfo, oldMockNode)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		// OK!
		nodeManager.update(mockUserInfo, mockNode, null, true);
		// can't change alias due to access restrictions
		when(mockAuthDao.canChangeSettings(mockUserInfo, oldMockNode)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		try {
			// Should fail
			nodeManager.update(mockUserInfo, mockNode, null, true);
			fail("Expected unauthorized exception");
		} catch (UnauthorizedException e) {
			// as expected
		}
		verify(mockAuthDao, times(2)).canChangeSettings(mockUserInfo, oldMockNode);
	}

	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedGetAnnotations() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		// Should fail
		nodeManager.getAnnotations(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetGetAnnotationsForVersion() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		// Should fail
		nodeManager.getAnnotationsForVersion(mockUserInfo, id, new Long(2));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetUpdateAnnotations() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		// Should fail
		nodeManager.updateAnnotations(mockUserInfo, id, mockAnnotations, NamedAnnotations.NAME_SPACE_ADDITIONAL);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetGetChildren() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		// Should fail
		nodeManager.getChildren(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetGetAllVersionNumbersForNode() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		// Should fail
		nodeManager.getAllVersionNumbersForNode(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetGetNodeType() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		// Should fail
		nodeManager.getNodeType(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetDeleteVersion() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.DELETE)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		// Should fail
		nodeManager.deleteVersion(mockUserInfo, id, new Long(12));
	}
	

}
