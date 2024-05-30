package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.dataaccess.AccessApprovalManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Validate that authorization checks are in place.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class NodeManagerAuthorizationTest {
	
	@Mock
	private NodeDAO mockNodeDao;
	@Mock
	private EntityAuthorizationManager mockAuthDao;
	@Mock
	private AccessControlListDAO mockAclDao;
	@Mock
	private Node mockNode;
	@Mock
	private Annotations mockUserAnnotations;
	@Mock
	private org.sagebionetworks.repo.model.Annotations mockEntityPropertyAnnotations;
	@Mock
	private UserInfo mockUserInfo;	
	@Mock
	private EntityBootstrapper mockEntityBootstrapper;
	@Mock
	private ActivityManager mockActivityManager;
	@Mock
	private ProjectSettingsManager mockProjectSettingsManager;
	@Mock
	private FileHandleAuthorizationManager mockfilehandleAuthorizationManager;
	@Mock
	private AccessApprovalManager mockAccessApprovalManager;
	@Mock
	private AccessControlListManager mockAclManager;
	@InjectMocks
	private NodeManagerImpl nodeManager;

	@Before
	public void before() throws NotFoundException, DatastoreException{
		String startEtag = "startEtag";
		// The mocks user for tests
		when(mockNode.getNodeType()).thenReturn(EntityType.project);
		when(mockNode.getName()).thenReturn("BobTheNode");
		when(mockNode.getETag()).thenReturn(startEtag);
		when(mockUserAnnotations.getEtag()).thenReturn(startEtag);
		when(mockNode.getParentId()).thenReturn("syn456");

		mockUserInfo = new UserInfo(false, 123L);
		
		when(mockNodeDao.lockNode(any(String.class))).thenReturn(startEtag);
		
		when(mockNodeDao.isNodeAvailable(any(String.class))).thenReturn(true);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedCreateNewNode() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		when(mockAuthDao.canCreate(mockNode.getParentId(), mockNode.getNodeType(), mockUserInfo)).thenReturn(AuthorizationStatus.accessDenied(""));
		// Should fail
		nodeManager.createNewNode(mockNode, mockUserInfo);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedCreateNewNodeFileHandle() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		// The user is allowed to create the file handle but not allowed to use the file handle.
		String fileHandleId = "123456";
		when(mockAuthDao.canCreate(mockNode.getParentId(), mockNode.getNodeType(), mockUserInfo)).thenReturn(AuthorizationStatus.authorized());
		when(mockfilehandleAuthorizationManager.canAccessRawFileHandleById(mockUserInfo, fileHandleId)).thenReturn(AuthorizationStatus.accessDenied(""));
		when(mockNode.getFileHandleId()).thenReturn(fileHandleId);
		// Should fail
		nodeManager.createNewNode(mockNode, mockUserInfo);
	}
	
	@Test 
	public void testAuthorizedCreateNewNodeFileHandle() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		// The user is allowed to create the file handle but not allowed to use the file handle.
		String fileHandleId = "123456";
		when(mockAuthDao.canCreate(mockNode.getParentId(), mockNode.getNodeType(), mockUserInfo)).thenReturn(AuthorizationStatus.authorized());
		when(mockfilehandleAuthorizationManager.canAccessRawFileHandleById(mockUserInfo, fileHandleId)).thenReturn(AuthorizationStatus.authorized());
		when(mockNode.getFileHandleId()).thenReturn(fileHandleId);
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
		when(mockAuthDao.hasAccess(mockUserInfo, nodeId, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		// The old file handle does not match the new file handle.
		when(mockNodeDao.getFileHandleIdForVersion(mockNode.getId(), null)).thenReturn(oldFileHandleId);
		// The user did not create the file handle.
		when(mockfilehandleAuthorizationManager.canAccessRawFileHandleById(mockUserInfo, fileHandleId)).thenReturn(
				AuthorizationStatus.accessDenied(mockUserInfo.getId().toString()+" cannot access "+fileHandleId));
		when(mockNode.getFileHandleId()).thenReturn(fileHandleId);
		when(mockNode.getParentId()).thenReturn(parentId);
		when(mockAccessApprovalManager.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(parentId))).thenReturn(AuthorizationStatus.authorized());
		Node oldMockNode = mock(Node.class);
		when(oldMockNode.getParentId()).thenReturn(parentId);
		when(mockNodeDao.getNode(nodeId)).thenReturn(oldMockNode);
		// Should fail
		try{
			nodeManager.update(mockUserInfo, mockNode, null, false);
			fail("Should have failed");
		}catch(UnauthorizedException e){
			assertTrue("The exception message should contain the file handle id: "+e.getMessage(), e.getMessage().contains(fileHandleId));
			assertTrue("The exception message should contain the user's id: "+e.getMessage(), e.getMessage().contains(mockUserInfo.getId().toString()));
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
		when(mockAuthDao.hasAccess(mockUserInfo, mockNode.getId(), ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthDao.hasAccess(mockUserInfo, mockNode.getId(), ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		// The file handle was already set on this node so it is not changing with this update.
		when(mockNodeDao.getFileHandleIdForVersion(mockNode.getId(), null)).thenReturn(fileHandleId);
		when(mockNode.getFileHandleId()).thenReturn(fileHandleId);
		when(mockNode.getParentId()).thenReturn(parentId);
		when(mockAccessApprovalManager.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(parentId))).thenReturn(AuthorizationStatus.authorized());
		Node oldMockNode = mock(Node.class);
		when(oldMockNode.getParentId()).thenReturn(parentId);
		when(mockNodeDao.getNode(nodeId)).thenReturn(oldMockNode);
		// Should fail
		nodeManager.update(mockUserInfo, mockNode, null, false);
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
		when(mockAuthDao.hasAccess(mockUserInfo, mockNode.getId(), ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthDao.hasAccess(mockUserInfo, mockNode.getId(), ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		// The current file handle on the node does not match the new handle.
		when(mockNodeDao.getFileHandleIdForVersion(mockNode.getId(), null)).thenReturn(oldFileHandleId);
		// The user can access the new file handle.
		when(mockfilehandleAuthorizationManager.canAccessRawFileHandleById(mockUserInfo, fileHandleId)).thenReturn(AuthorizationStatus.authorized());
		when(mockNode.getFileHandleId()).thenReturn(fileHandleId);
		when(mockNode.getParentId()).thenReturn(parentId);
		when(mockAccessApprovalManager.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(parentId))).thenReturn(AuthorizationStatus.authorized());
		Node oldMockNode = mock(Node.class);
		when(oldMockNode.getParentId()).thenReturn(parentId);
		when(mockNodeDao.getNode(nodeId)).thenReturn(oldMockNode);
		// Should fail
		nodeManager.update(mockUserInfo, mockNode, null, false);
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
		// The user does not have permission to dowload the file
		when(mockAuthDao.hasAccess(mockUserInfo, mockNode.getId(), ACCESS_TYPE.DOWNLOAD)).thenReturn(AuthorizationStatus.accessDenied(""));
		nodeManager.getFileHandleIdForVersion(mockUserInfo, mockNode.getId(), null);
	}

	/**
	 * Not found when there is not file handle id.
	 * 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test (expected=NotFoundException.class)
	public void testNotFoundGetFileHandle() throws DatastoreException, NotFoundException{
		when(mockAuthDao.hasAccess(mockUserInfo, mockNode.getId(), ACCESS_TYPE.DOWNLOAD)).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.getFileHandleIdForVersion(mockNode.getId(), null)).thenReturn(null);
		nodeManager.getFileHandleIdForVersion(mockUserInfo, mockNode.getId(), null);
	}
	
	@Test
	public void testGetFileHandleIdForCurrentVersion() throws DatastoreException, NotFoundException{
		when(mockAuthDao.hasAccess(mockUserInfo, mockNode.getId(), ACCESS_TYPE.DOWNLOAD)).thenReturn(AuthorizationStatus.authorized());
		String expectedFileHandleId = "999999";
		when(mockNodeDao.getFileHandleIdForVersion(mockNode.getId(), null)).thenReturn(expectedFileHandleId);
		String handleId = nodeManager.getFileHandleIdForVersion(mockUserInfo, mockNode.getId(), null);
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
		// The user does have permission to dowload the file
		when(mockAuthDao.hasAccess(mockUserInfo, mockNode.getId(), ACCESS_TYPE.DOWNLOAD)).thenReturn(AuthorizationStatus.accessDenied(""));
		nodeManager.getFileHandleIdForVersion(mockUserInfo, mockNode.getId(), null);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedDeleteNode() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		String id = "22";
		when(mockAuthDao.hasAccess(mockUserInfo, id, ACCESS_TYPE.DELETE)).thenReturn(AuthorizationStatus.accessDenied(""));
		// Should fail
		nodeManager.delete(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedGetNode() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		String id = "22";
		when(mockAuthDao.hasAccess(mockUserInfo, id, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied(""));
		// Should fail
		nodeManager.getNode(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedGetNodeForVersionNumber() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		String id = "22";
		when(mockAuthDao.hasAccess(mockUserInfo, id, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied(""));
		// Should fail
		nodeManager.getNodeForVersionNumber(mockUserInfo, id, 1L);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedUpdate() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockNode.getId()).thenReturn(id);
		when(mockAuthDao.hasAccess(mockUserInfo, id, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		// Should fail
		nodeManager.update(mockUserInfo, mockNode, null, false);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedUpdate2() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockNode.getId()).thenReturn(id);
		when(mockAuthDao.hasAccess(mockUserInfo, id, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		// Should fail
		nodeManager.update(mockUserInfo, mockNode, mockEntityPropertyAnnotations, true);
	}
	
	@Test
	public void testUnauthorizedUpdateDueToAccessRequirements() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		String parentId = "123";
		when(mockNode.getId()).thenReturn(id);
		when(mockAuthDao.hasAccess(mockUserInfo, id, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthDao.hasAccess(mockUserInfo, id, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		when(mockNode.getParentId()).thenReturn(parentId);
		// can't move due to access restrictions
		when(mockAccessApprovalManager.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(parentId))).thenReturn(AuthorizationStatus.authorized());
		Node oldMockNode = mock(Node.class);
		when(oldMockNode.getParentId()).thenReturn(parentId);
		when(mockNodeDao.getNode(id)).thenReturn(oldMockNode);
		// OK!
		nodeManager.update(mockUserInfo, mockNode, null, true);
		// can't move due to access restrictions
		when(mockAccessApprovalManager.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(parentId))).thenReturn(AuthorizationStatus.accessDenied(""));
		try {
			// Should fail
			nodeManager.update(mockUserInfo, mockNode, null, true);
			fail("Excpected unauthorized exception");
		} catch (UnauthorizedException e) {
			// as expected
		}
		verify(mockAccessApprovalManager, times(2)).canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(parentId));
	}
	
	@Test
	public void testUnauthorizedUpdateDueToAliasChange() throws DatastoreException, InvalidModelException, NotFoundException,
			UnauthorizedException, ConflictingUpdateException {
		String id = "22";
		String parentId = "123";
		when(mockNode.getId()).thenReturn(id);
		when(mockAuthDao.hasAccess(mockUserInfo, id, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		when(mockAuthDao.hasAccess(mockUserInfo, id, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		when(mockNode.getParentId()).thenReturn(parentId);
		// can't move due to access restrictions
		when(mockAccessApprovalManager.canUserMoveRestrictedEntity(eq(mockUserInfo), eq(parentId), eq(parentId))).thenReturn(
				AuthorizationStatus.authorized());
		Node oldMockNode = mock(Node.class);
		when(oldMockNode.getParentId()).thenReturn(parentId);
		when(oldMockNode.getAlias()).thenReturn("alias2");
		when(mockNodeDao.getNode(id)).thenReturn(oldMockNode);
		when(mockAuthDao.hasAccess(mockUserInfo, id, ACCESS_TYPE.CHANGE_SETTINGS)).thenReturn(AuthorizationStatus.authorized());
		// OK!
		nodeManager.update(mockUserInfo, mockNode, null, true);
		// can't change alias due to access restrictions
		when(mockAuthDao.hasAccess(mockUserInfo, id, ACCESS_TYPE.CHANGE_SETTINGS)).thenReturn(AuthorizationStatus.accessDenied(""));
		try {
			// Should fail
			nodeManager.update(mockUserInfo, mockNode, null, true);
			fail("Expected unauthorized exception");
		} catch (UnauthorizedException e) {
			// as expected
		}
		verify(mockAuthDao, times(2)).hasAccess(mockUserInfo, id, ACCESS_TYPE.CHANGE_SETTINGS);
	}

	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedGetUserAnnotations() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.hasAccess(mockUserInfo, id, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied(""));
		// Should fail
		nodeManager.getUserAnnotations(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetGetUserAnnotationsForVersion() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.hasAccess(mockUserInfo, id, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied(""));
		// Should fail
		nodeManager.getUserAnnotationsForVersion(mockUserInfo, id, 2L);
	}

	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedGetEntityPropertyAnnotations() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.hasAccess(mockUserInfo, id, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied(""));
		// Should fail
		nodeManager.getEntityPropertyAnnotations(mockUserInfo, id);
	}

	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetGetEntityPropertyAnnotationsForVersion() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.hasAccess(mockUserInfo, id, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied(""));
		// Should fail
		nodeManager.getEntityPropertyForVersion(mockUserInfo, id, 2L);
	}

	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetUpdateAnnotations() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.hasAccess(mockUserInfo, id, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		// Should fail
		nodeManager.updateUserAnnotations(mockUserInfo, id, mockUserAnnotations);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetGetNodeType() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.hasAccess(mockUserInfo, id, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied(""));
		// Should fail
		nodeManager.getNodeType(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetDeleteVersion() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.hasAccess(mockUserInfo, id, ACCESS_TYPE.DELETE)).thenReturn(AuthorizationStatus.accessDenied(""));
		// Should fail
		nodeManager.deleteVersion(mockUserInfo, id, 12L);
	}
	

}
