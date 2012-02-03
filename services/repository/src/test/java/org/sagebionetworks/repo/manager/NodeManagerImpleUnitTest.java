package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACL_SCHEME;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.FieldTypeDAO;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ReferenceDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
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
	private FieldTypeDAO mockFieldTypeDao = null;
	private EntityBootstrapper mockEntityBootstrapper;
	private NodeInheritanceManager mockNodeInheritanceManager = null;
	private ReferenceDao mockReferenceDao = null;
		
	private final UserInfo mockUserInfo = new UserInfo(false);
	private final UserInfo anonUserInfo = new UserInfo(false);

	@Before
	public void before() throws Exception {
		mockNodeDao = Mockito.mock(NodeDAO.class);
		mockAuthDao = Mockito.mock(AuthorizationManager.class);
		mockFieldTypeDao = Mockito.mock(FieldTypeDAO.class);
		mockAclDao = Mockito.mock(AccessControlListDAO.class);
		mockEntityBootstrapper = Mockito.mock(EntityBootstrapper.class);
		mockNodeInheritanceManager = Mockito.mock(NodeInheritanceManager.class);
		mockReferenceDao = Mockito.mock(ReferenceDao.class);
		// Create the manager dao with mocked dependent daos.
		nodeManager = new NodeManagerImpl(mockNodeDao, mockAuthDao, mockFieldTypeDao, mockAclDao, 
				mockEntityBootstrapper, mockNodeInheritanceManager, mockReferenceDao);

		UserGroup userGroup = new UserGroup();
		userGroup.setId("2");
		userGroup.setName("two");
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
		String presetCreatedBy = "createdByMe";
		Date presetCreatedOn = new Date(100L);
		node.setCreatedBy(presetCreatedBy);
		node.setCreatedOn(presetCreatedOn);
		// Now validate the node
		NodeManagerImpl.validateNodeCreationData(AuthorizationConstants.ANONYMOUS_USER_ID, node);
		// the values should not have changed
		assertEquals(presetCreatedOn, node.getCreatedOn());
		assertEquals(presetCreatedBy, node.getCreatedBy());
	}
	
	@Test
	public void testValidateNodeCreatedDataWithNulls(){
		Node node = new Node();
		// Now validate the node
		NodeManagerImpl.validateNodeCreationData(AuthorizationConstants.ANONYMOUS_USER_ID, node);
		// the values should not have changed
		assertNotNull(node.getCreatedOn());
		assertEquals(AuthorizationConstants.ANONYMOUS_USER_ID, node.getCreatedBy());
	}
	
	@Test
	public void testValidateNodeModifiedDataWithPreset(){
		Node node = new Node();
		String presetModifiedBy = "modifiedByMe";
		Date presetModifiedOn = new Date(100L);
		node.setModifiedBy(presetModifiedBy);
		node.setModifiedOn(presetModifiedOn);
		// Now validate the node
		NodeManagerImpl.validateNodeModifiedData(AuthorizationConstants.ANONYMOUS_USER_ID, node);
		// the values should have changed
		assertTrue(!presetModifiedOn.equals( node.getModifiedOn()));
		assertTrue(!presetModifiedBy.equals( node.getModifiedBy()));
	}
	
	@Test
	public void testValidateNodeModifiedDataWithNulls(){
		Node node = new Node();
		// Now validate the node
		NodeManagerImpl.validateNodeModifiedData(AuthorizationConstants.ANONYMOUS_USER_ID, node);
		// the values should not have changed
		assertNotNull(node.getModifiedOn());
		assertEquals(AuthorizationConstants.ANONYMOUS_USER_ID, node.getModifiedBy());
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
		assertEquals(AuthorizationConstants.ANONYMOUS_USER_ID, processedNode.getCreatedBy());
		assertEquals(AuthorizationConstants.ANONYMOUS_USER_ID, processedNode.getModifiedBy());
		assertNotNull(processedNode.getModifiedOn());
		assertNotNull(processedNode.getModifiedBy());
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
		List<EntityHeader> expected = new ArrayList<EntityHeader>();
		Long id = 101L;
		UserInfo userInfo = anonUserInfo;
		when(mockReferenceDao.getReferrers(id, userInfo)).thenReturn(expected);
		List<EntityHeader> entityHeaders = nodeManager.getEntityReferences(userInfo, ""+id);
	}
}
