package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
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

	@Before
	public void before() throws NotFoundException, DatastoreException{
		mockNodeDao = Mockito.mock(NodeDAO.class);
		mockAuthDao = Mockito.mock(AuthorizationManager.class);
		// Say no to everything.
		mockAclDao = Mockito.mock(AccessControlListDAO.class);
		mockEntityBootstrapper = Mockito.mock(EntityBootstrapper.class);
		mockInheritanceManager = Mockito.mock(NodeInheritanceManager.class);
		mockActivityManager = Mockito.mock(ActivityManager.class);
		// Create the manager dao with mocked dependent daos.
		nodeManager = new NodeManagerImpl(mockNodeDao, mockAuthDao, mockAclDao, mockEntityBootstrapper, mockInheritanceManager, null, mockActivityManager);
		// The mocks user for tests
		mockNode = Mockito.mock(Node.class);
		when(mockNode.getNodeType()).thenReturn(EntityType.project.name());
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
		when(mockAuthDao.canCreate(mockUserInfo, mockNode)).thenReturn(false);
		// Should fail
		nodeManager.createNewNode(mockNode, mockUserInfo);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedCreateNewNodeFileHandle() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		// The user is allowed to create the file handle but not allowed to use the file handle.
		String fileHandleId = "123456";
		when(mockAuthDao.canCreate(mockUserInfo, mockNode)).thenReturn(true);
		when(mockAuthDao.canAccessRawFileHandleById(mockUserInfo, fileHandleId)).thenReturn(false);
		when(mockNode.getFileHandleId()).thenReturn(fileHandleId);
		// Should fail
		nodeManager.createNewNode(mockNode, mockUserInfo);
	}
	
	@Test 
	public void testAuthorizedCreateNewNodeFileHandle() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		// The user is allowed to create the file handle but not allowed to use the file handle.
		String fileHandleId = "123456";
		when(mockAuthDao.canCreate(mockUserInfo, mockNode)).thenReturn(true);
		when(mockAuthDao.canAccessRawFileHandleById(mockUserInfo, fileHandleId)).thenReturn(true);
		when(mockNode.getFileHandleId()).thenReturn(fileHandleId);
		when(mockEntityBootstrapper.getChildAclSchemeForPath(any(String.class))).thenReturn(ACL_SCHEME.INHERIT_FROM_PARENT);
		// Should fail
		nodeManager.createNewNode(mockNode, mockUserInfo);
		verify(mockNodeDao).createNew(mockNode);
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
		// The user can update the node.
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(true);
		// The old file handle does not match the new file handle.
		when(mockNodeDao.getFileHandleIdForCurrentVersion(mockNode.getId())).thenReturn(oldFileHandleId);
		// The user did not create the file handle.
		when(mockAuthDao.canAccessRawFileHandleById(mockUserInfo, fileHandleId)).thenReturn(false);
		when(mockNode.getFileHandleId()).thenReturn(fileHandleId);
		// Should fail
		try{
			nodeManager.update(mockUserInfo, mockNode);
			fail("Should have failed");
		}catch(UnauthorizedException e){
			assertTrue("The exception message should contain the file handle id",e.getMessage().indexOf(fileHandleId) > 0);
			assertTrue("The exception message should contain the user's id",e.getMessage().indexOf(mockUserInfo.getId().toString()) > 0);
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
		String fileHandleId = "123456";
		// The user can update the node.
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(true);
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(true);
		// The file handle was already set on this node so it is not changing with this update.
		when(mockNodeDao.getFileHandleIdForCurrentVersion(mockNode.getId())).thenReturn(fileHandleId);
		// If the user were to set this file handle it would fail as they are not the creator of the file handle.
		when(mockAuthDao.canAccessRawFileHandleById(mockUserInfo, fileHandleId)).thenReturn(false);
		when(mockNode.getFileHandleId()).thenReturn(fileHandleId);
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
		// The user can update the node.
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(true);
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(true);
		// The current file handle on the node does not match the new handle.
		when(mockNodeDao.getFileHandleIdForCurrentVersion(mockNode.getId())).thenReturn(oldFileHandleId);
		// The user can access the new file handle.
		when(mockAuthDao.canAccessRawFileHandleById(mockUserInfo, fileHandleId)).thenReturn(true);
		when(mockNode.getFileHandleId()).thenReturn(fileHandleId);
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
		String fileHandleId = "123456";
		// The user has access to read the node
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(true);
		// The user does not have permission to dowload the file
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD)).thenReturn(false);
		nodeManager.getFileHandleIdForCurrentVersion(mockUserInfo, mockNode.getId());
	}
	
	/**
	 * Not found when there is not file handle id.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test (expected=NotFoundException.class)
	public void testNotFoundGetFileHandle() throws DatastoreException, NotFoundException{
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(true);
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD)).thenReturn(true);
		when(mockNodeDao.getFileHandleIdForCurrentVersion( mockNode.getId())).thenReturn(null);
		nodeManager.getFileHandleIdForCurrentVersion(mockUserInfo, mockNode.getId());
	}
	
	@Test
	public void testGetFileHandleIdForCurrentVersion() throws DatastoreException, NotFoundException{
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(true);
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD)).thenReturn(true);
		String expectedFileHandleId = "999999";
		when(mockNodeDao.getFileHandleIdForCurrentVersion( mockNode.getId())).thenReturn(expectedFileHandleId);
		String handleId = nodeManager.getFileHandleIdForCurrentVersion(mockUserInfo, mockNode.getId());
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
		String fileHandleId = "123456";
		// The user does not have access to read the node
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(false);
		// The user does have permission to dowload the file
		when(mockAuthDao.canAccess(mockUserInfo, mockNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD)).thenReturn(false);
		nodeManager.getFileHandleIdForCurrentVersion(mockUserInfo, mockNode.getId());
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedDeleteNode() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.DELETE)).thenReturn(false);
		// Should fail
		nodeManager.delete(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedGetNode() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(false);
		// Should fail
		nodeManager.get(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedGetNodeForVersionNumber() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(false);
		// Should fail
		nodeManager.getNodeForVersionNumber(mockUserInfo, id, new Long(1));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedUpdate() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(false);
		// Should fail
		nodeManager.update(mockUserInfo, mockNode);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedUpdate2() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(false);
		// Should fail
		nodeManager.update(mockUserInfo, mockNode, mockNamed, true);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedGetAnnotations() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(false);
		// Should fail
		nodeManager.getAnnotations(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetGetAnnotationsForVersion() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(false);
		// Should fail
		nodeManager.getAnnotationsForVersion(mockUserInfo, id, new Long(2));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetUpdateAnnotations() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(false);
		// Should fail
		nodeManager.updateAnnotations(mockUserInfo, id, mockAnnotations, NamedAnnotations.NAME_SPACE_ADDITIONAL);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetGetChildren() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(false);
		// Should fail
		nodeManager.getChildren(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetGetAllVersionNumbersForNode() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(false);
		// Should fail
		nodeManager.getAllVersionNumbersForNode(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetGetNodeType() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(false);
		// Should fail
		nodeManager.getNodeType(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetDeleteVersion() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ObjectType.ENTITY, ACCESS_TYPE.DELETE)).thenReturn(false);
		// Should fail
		nodeManager.deleteVersion(mockUserInfo, id, new Long(12));
	}
	

}
