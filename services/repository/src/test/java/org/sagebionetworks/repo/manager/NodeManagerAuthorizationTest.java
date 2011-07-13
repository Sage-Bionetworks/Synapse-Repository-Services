package org.sagebionetworks.repo.manager;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FieldTypeDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
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
	private FieldTypeDAO mockFieldTypeDao = null;
	private Node mockNode;
	private Annotations mockAnnotations;
	private User mockUser;
	private UserGroup mockUserGroup;
	private UserInfo mockUserInfo;	

	@Before
	public void before() throws NotFoundException, DatastoreException{
		mockNodeDao = Mockito.mock(NodeDAO.class);
		mockAuthDao = Mockito.mock(AuthorizationManager.class);
		// Say no to everything.
		mockFieldTypeDao = Mockito.mock(FieldTypeDAO.class);
		mockAclDao = Mockito.mock(AccessControlListDAO.class);
		// Create the manager dao with mocked dependent daos.
		nodeManager = new NodeManagerImpl(mockNodeDao, mockAuthDao, mockFieldTypeDao, mockAclDao);
		// The mocks user for tests
		mockNode = Mockito.mock(Node.class);
		when(mockNode.getNodeType()).thenReturn(ObjectType.project.name());
		when(mockNode.getName()).thenReturn("BobTheNode");
		mockAnnotations = Mockito.mock(Annotations.class);
		when(mockAnnotations.getEtag()).thenReturn("12");
		// Mock user
		mockUser = Mockito.mock(User.class);
		when(mockUser.getId()).thenReturn("12");
		when(mockUser.getUserId()).thenReturn("Max");
		// UserGroup
		mockUserGroup = Mockito.mock(UserGroup.class);
		when(mockUserGroup.getId()).thenReturn("123");
		when(mockUserGroup.getName()).thenReturn("GroupNameAlpha");
		mockUserInfo = Mockito.mock(UserInfo.class);
		when(mockUserInfo.getUser()).thenReturn(mockUser);
		when(mockUserInfo.getIndividualGroup()).thenReturn(mockUserGroup);
		
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedCreateNewNode() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		when(mockAuthDao.canCreate(mockUserInfo, mockNode)).thenReturn(false);
		// Should fail
		nodeManager.createNewNode(mockNode, mockUserInfo);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedDeleteNode() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ACCESS_TYPE.DELETE)).thenReturn(false);
		// Should fail
		nodeManager.delete(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedGetNode() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ACCESS_TYPE.READ)).thenReturn(false);
		// Should fail
		nodeManager.get(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedGetNodeForVersionNumber() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ACCESS_TYPE.READ)).thenReturn(false);
		// Should fail
		nodeManager.getNodeForVersionNumber(mockUserInfo, id, new Long(1));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedUpdate() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ACCESS_TYPE.UPDATE)).thenReturn(false);
		// Should fail
		nodeManager.update(mockUserInfo, mockNode);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedUpdate2() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ACCESS_TYPE.UPDATE)).thenReturn(false);
		// Should fail
		nodeManager.update(mockUserInfo, mockNode, mockAnnotations, true);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedGetAnnotations() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ACCESS_TYPE.READ)).thenReturn(false);
		// Should fail
		nodeManager.getAnnotations(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetGetAnnotationsForVersion() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ACCESS_TYPE.READ)).thenReturn(false);
		// Should fail
		nodeManager.getAnnotationsForVersion(mockUserInfo, id, new Long(2));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetUpdateAnnotations() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ACCESS_TYPE.UPDATE)).thenReturn(false);
		// Should fail
		nodeManager.updateAnnotations(mockUserInfo, id, mockAnnotations);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetGetChildren() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ACCESS_TYPE.READ)).thenReturn(false);
		// Should fail
		nodeManager.getChildren(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetGetAllVersionNumbersForNode() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ACCESS_TYPE.READ)).thenReturn(false);
		// Should fail
		nodeManager.getAllVersionNumbersForNode(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetGetNodeType() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ACCESS_TYPE.READ)).thenReturn(false);
		// Should fail
		nodeManager.getNodeType(mockUserInfo, id);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUnauthorizedetDeleteVersion() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		String id = "22";
		when(mockAuthDao.canAccess(mockUserInfo, id, ACCESS_TYPE.DELETE)).thenReturn(false);
		// Should fail
		nodeManager.deleteVersion(mockUserInfo, id, new Long(12));
	}
	

}
