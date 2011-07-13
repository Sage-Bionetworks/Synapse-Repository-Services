package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FieldTypeDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.query.FieldType;
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
		
	private final UserInfo mockUserInfo = new UserInfo(false);
	private final UserInfo anonUserInfo = new UserInfo(false);

	@Before
	public void before() throws Exception {


		mockNodeDao = Mockito.mock(NodeDAO.class);
		mockAuthDao = Mockito.mock(AuthorizationManager.class);
		mockFieldTypeDao = Mockito.mock(FieldTypeDAO.class);
		mockAclDao = Mockito.mock(AccessControlListDAO.class);
		// Create the manager dao with mocked dependent daos.
		nodeManager = new NodeManagerImpl(mockNodeDao, mockAuthDao, mockFieldTypeDao, mockAclDao);

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
		anonUser.setUserId(AuthUtilConstants.ANONYMOUS_USER_ID);
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
		NodeManagerImpl.validateNodeCreationData(AuthUtilConstants.ANONYMOUS_USER_ID, node);
		// the values should not have changed
		assertEquals(presetCreatedOn, node.getCreatedOn());
		assertEquals(presetCreatedBy, node.getCreatedBy());
	}
	
	@Test
	public void testValidateNodeCreatedDataWithNulls(){
		Node node = new Node();
		// Now validate the node
		NodeManagerImpl.validateNodeCreationData(AuthUtilConstants.ANONYMOUS_USER_ID, node);
		// the values should not have changed
		assertNotNull(node.getCreatedOn());
		assertEquals(AuthUtilConstants.ANONYMOUS_USER_ID, node.getCreatedBy());
	}
	
	@Test
	public void testValidateNodeModifiedDataWithPreset(){
		Node node = new Node();
		String presetModifiedBy = "modifiedByMe";
		Date presetModifiedOn = new Date(100L);
		node.setModifiedBy(presetModifiedBy);
		node.setModifiedOn(presetModifiedOn);
		// Now validate the node
		NodeManagerImpl.validateNodeModifiedData(AuthUtilConstants.ANONYMOUS_USER_ID, node);
		// the values should have changed
		assertTrue(!presetModifiedOn.equals( node.getModifiedOn()));
		assertTrue(!presetModifiedBy.equals( node.getModifiedBy()));
	}
	
	@Test
	public void testValidateNodeModifiedDataWithNulls(){
		Node node = new Node();
		// Now validate the node
		NodeManagerImpl.validateNodeModifiedData(AuthUtilConstants.ANONYMOUS_USER_ID, node);
		// the values should not have changed
		assertNotNull(node.getModifiedOn());
		assertEquals(AuthUtilConstants.ANONYMOUS_USER_ID, node.getModifiedBy());
	}
	
	@Test
	public void testCreateNode() throws Exception {
		// Test creating a new node with nothing but the name and type set
		Node newNode = new Node();
		newNode.setName("testCreateNode");
		newNode.setNodeType("someType");
		
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
		assertEquals(AuthUtilConstants.ANONYMOUS_USER_ID, processedNode.getCreatedBy());
		assertEquals(AuthUtilConstants.ANONYMOUS_USER_ID, processedNode.getModifiedBy());
		assertNotNull(processedNode.getModifiedOn());
		assertNotNull(processedNode.getModifiedBy());
	}

	
	@Test
	public void testGetAnnotations() throws NotFoundException, DatastoreException, UnauthorizedException{
		String id = "101";
		Annotations annos = new Annotations();
		annos.addAnnotation("stringKey", "a");
		annos.addAnnotation("longKey", Long.MAX_VALUE);
		when(mockNodeDao.getAnnotations(id)).thenReturn(annos);
		UserInfo userInfo = anonUserInfo;
		when(mockAuthDao.canAccess(userInfo, id, ACCESS_TYPE.READ)).thenReturn(true);
		Annotations copy = nodeManager.getAnnotations(userInfo, id);
		assertEquals(copy, annos);
	}
	
	@Test
	public void testValidateAnnoationsAssignedToAnotherType() throws Exception, DatastoreException, UnauthorizedException, ConflictingUpdateException{
		// To update the annotations 
		String id = "101";
		Annotations annos = new Annotations();
		String annotationName = "dateKeyKey";
		annos.setId(id);
		annos.setEtag("9");
		when(mockNodeDao.peekCurrentEtag("101")).thenReturn("9");
		when(mockNodeDao.lockNodeAndIncrementEtag("101", "9")).thenReturn("10");
		UserInfo userInfo = anonUserInfo;
		when(mockAuthDao.canAccess(userInfo, "101", ACCESS_TYPE.UPDATE)).thenReturn(true);
		// The mockFieldTypeDao with throw an exception i 
		annos.addAnnotation(annotationName, new Date(System.currentTimeMillis()));
		when(mockAuthDao.canAccess(userInfo, id, ACCESS_TYPE.READ)).thenReturn(true);
		nodeManager.updateAnnotations(userInfo,id,annos);
		// Make sure this annotation name is checked against FieldType.DATE_ATTRIBUTE.
		verify(mockFieldTypeDao, atLeastOnce()).addNewType(annotationName, FieldType.DATE_ATTRIBUTE);
	}
	
	@Test
	public void testValidateStringAnnotation() throws Exception, DatastoreException, UnauthorizedException, ConflictingUpdateException{
		// To update the annotations 
		Annotations annos = new Annotations();
		annos.setEtag("123");
		String annotationName = "stringKey";
		// The mockFieldTypeDao with throw an exception i 
		annos.addAnnotation(annotationName, "stringValue");
		nodeManager.validateAnnotations(annos);
		// Make sure this annotation name is checked against FieldType.DATE_ATTRIBUTE.
		verify(mockFieldTypeDao, atLeastOnce()).addNewType(annotationName, FieldType.STRING_ATTRIBUTE);
		// Should not have been called
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.DATE_ATTRIBUTE);
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.LONG_ATTRIBUTE);
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.DOUBLE_ATTRIBUTE);
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.BLOB_ATTRIBUTE);
	}
	
	@Test
	public void testValidateDoubleAnnotation() throws Exception, DatastoreException, UnauthorizedException, ConflictingUpdateException{
		// To update the annotations 
		Annotations annos = new Annotations();
		annos.setEtag("123");
		String annotationName = "doubleKey";
		// The mockFieldTypeDao with throw an exception i 
		annos.addAnnotation(annotationName, new Double(123.5));
		nodeManager.validateAnnotations(annos);
		// Make sure this annotation name is checked against FieldType.DATE_ATTRIBUTE.
		verify(mockFieldTypeDao, atLeastOnce()).addNewType(annotationName, FieldType.DOUBLE_ATTRIBUTE);
		// Should not have been called
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.DATE_ATTRIBUTE);
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.LONG_ATTRIBUTE);
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.STRING_ATTRIBUTE);
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.BLOB_ATTRIBUTE);
	}
	
	@Test
	public void testValidateLongAnnotation() throws Exception, DatastoreException, UnauthorizedException, ConflictingUpdateException{
		// To update the annotations 
		Annotations annos = new Annotations();
		annos.setEtag("123");
		String annotationName = "longKey";
		annos.addAnnotation(annotationName, new Long(1235));
		nodeManager.validateAnnotations(annos);
		// Make sure this annotation name is checked against FieldType.DATE_ATTRIBUTE.
		verify(mockFieldTypeDao, atLeastOnce()).addNewType(annotationName, FieldType.LONG_ATTRIBUTE);
		// Should not have been called
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.DATE_ATTRIBUTE);
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.DOUBLE_ATTRIBUTE);
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.STRING_ATTRIBUTE);
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.BLOB_ATTRIBUTE);
	}
	
	@Test
	public void testValidateDateAnnotation() throws Exception, DatastoreException, UnauthorizedException, ConflictingUpdateException{
		// To update the annotations 
		Annotations annos = new Annotations();
		annos.setEtag("123");
		String annotationName = "dateKey";
		annos.addAnnotation(annotationName, new Date(1235));
		nodeManager.validateAnnotations(annos);
		// Make sure this annotation name is checked against FieldType.DATE_ATTRIBUTE.
		verify(mockFieldTypeDao, atLeastOnce()).addNewType(annotationName, FieldType.DATE_ATTRIBUTE);
		// Should not have been called
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.LONG_ATTRIBUTE);
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.DOUBLE_ATTRIBUTE);
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.STRING_ATTRIBUTE);
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.BLOB_ATTRIBUTE);
	}
	
	@Test
	public void testValidateBlobAnnotation() throws Exception, DatastoreException, UnauthorizedException, ConflictingUpdateException{
		// To update the annotations 
		Annotations annos = new Annotations();
		annos.setEtag("123");
		String annotationName = "blobKey";
		// The mockFieldTypeDao with throw an exception i 
		annos.addAnnotation(annotationName, "Some very long string".getBytes("UTF-8"));
		nodeManager.validateAnnotations(annos);
		// Make sure this annotation name is checked against FieldType.DATE_ATTRIBUTE.
		verify(mockFieldTypeDao, atLeastOnce()).addNewType(annotationName, FieldType.BLOB_ATTRIBUTE);
		// Should not have been called
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.DATE_ATTRIBUTE);
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.LONG_ATTRIBUTE);
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.DOUBLE_ATTRIBUTE);
		verify(mockFieldTypeDao, never()).addNewType(annotationName, FieldType.STRING_ATTRIBUTE);
	}

}
