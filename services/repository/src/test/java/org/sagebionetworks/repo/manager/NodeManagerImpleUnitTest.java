package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This is the unit test version of this class.
 * 
 * @author jmhill
 *
 */
public class NodeManagerImpleUnitTest {
	
	private NodeDAO mockNodeDao = null;
	private AuthorizationDAO mockAuthDao = null;
	private NodeManagerImpl nodeManager = null;
	
	@Before
	public void before(){
		mockNodeDao = Mockito.mock(NodeDAO.class);
		mockAuthDao = Mockito.mock(AuthorizationDAO.class);
		// Create the manager dao with mocked dependent daos.
		nodeManager = new NodeManagerImpl(mockNodeDao, mockAuthDao);
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
		node.setType("some type");
		NodeManagerImpl.validateNode(node);
	}
	
	@Test
	public void testValidateUsernameNull(){
		// Null user names should be treated as ANNONYMOUS
		String validated = NodeManagerImpl.validateUsername(null);
		assertEquals(NodeManagerImpl.ANNONYMOUS, validated);
	}
	
	@Test
	public void testValidateUsernameEmpty(){
		// Empty user names should be treated as ANNONYMOUS
		String validated = NodeManagerImpl.validateUsername(" ");
		assertEquals(NodeManagerImpl.ANNONYMOUS, validated);
	}
	
	@Test
	public void testValidateUsernameTrim(){
		// Empty user names should be treated as ANNONYMOUS
		String userName = "someUserName";
		// Add some white space to the name
		String validated = NodeManagerImpl.validateUsername("  \n"+userName+" ");
		assertEquals(userName, validated);
	}
	@Test
	public void testValidateNodeCreatedDataWithPreset(){
		Node node = new Node();
		String presetCreatedBy = "createdByMe";
		Date presetCreatedOn = new Date(100L);
		node.setCreatedBy(presetCreatedBy);
		node.setCreatedOn(presetCreatedOn);
		// Now validate the node
		NodeManagerImpl.validateNodeCreationData(NodeManagerImpl.ANNONYMOUS, node);
		// the values should not have changed
		assertEquals(presetCreatedOn, node.getCreatedOn());
		assertEquals(presetCreatedBy, node.getCreatedBy());
	}
	
	@Test
	public void testValidateNodeCreatedDataWithNulls(){
		Node node = new Node();
		String presetCreatedBy = null;
		Date presetCreatedOn = null;
		node.setCreatedBy(presetCreatedBy);
		node.setCreatedOn(presetCreatedOn);
		// Now validate the node
		NodeManagerImpl.validateNodeCreationData(NodeManagerImpl.ANNONYMOUS, node);
		// the values should not have changed
		assertNotNull(node.getCreatedOn());
		assertEquals(NodeManagerImpl.ANNONYMOUS, node.getCreatedBy());
	}
	
	@Test
	public void testValidateNodeModifiedDataWithPreset(){
		Node node = new Node();
		String presetModifiedBy = "modifiedByMe";
		Date presetModifiedOn = new Date(100L);
		node.setModifiedBy(presetModifiedBy);
		node.setModifiedOn(presetModifiedOn);
		// Now validate the node
		NodeManagerImpl.validateNodeModifiedData(NodeManagerImpl.ANNONYMOUS, node);
		// the values should not have changed
		assertEquals(presetModifiedOn, node.getModifiedOn());
		assertEquals(presetModifiedBy, node.getModifiedBy());
	}
	
	@Test
	public void testValidateNodeModifiedDataWithNulls(){
		Node node = new Node();
		String presetModifiedBy = null;
		Date presetModifiedOn = null;
		node.setModifiedBy(presetModifiedBy);
		node.setModifiedOn(presetModifiedOn);
		// Now validate the node
		NodeManagerImpl.validateNodeModifiedData(NodeManagerImpl.ANNONYMOUS, node);
		// the values should not have changed
		assertNotNull(node.getModifiedOn());
		assertEquals(NodeManagerImpl.ANNONYMOUS, node.getModifiedBy());
	}
	
	@Test
	public void testCreateNode() throws Exception {
		// Test creating a new node with nothing but the name and type set
		Node newNode = new Node();
		newNode.setName("testCreateNode");
		newNode.setType("someType");
		// Sure the mock is ready.
		ArgumentCaptor<Node> argument = ArgumentCaptor.forClass(Node.class);
		when(mockNodeDao.createNew(argument.capture())).thenReturn("101");
		// Make the actual call
		String id = nodeManager.createNewNode(newNode, null);
		// Now validate that t
		assertEquals("101", id);
		Node processedNode = argument.getValue();
		assertNotNull(processedNode);
		assertEquals(NodeManagerImpl.ANNONYMOUS, processedNode.getCreatedBy());
		assertEquals(NodeManagerImpl.ANNONYMOUS, processedNode.getModifiedBy());
		assertNotNull(processedNode.getModifiedOn());
		assertNotNull(processedNode.getModifiedBy());
	}
	
	@Test(expected=ConflictingUpdateException.class)
	public void testValidateETagAndLockNodeConfilict() throws ConflictingUpdateException{
		String nodeId = "101";
		String eTag = "899";
		when(mockNodeDao.getETagForUpdate(nodeId)).thenReturn(new Long(900));
		// Since the eTag will not match an exception should be thrown.
		nodeManager.validateETagAndLockNode(nodeId, eTag);
	}
	
	@Test
	public void testValidateETagAndLockNodePass() throws ConflictingUpdateException{
		String nodeId = "101";
		String eTag = "899";
		when(mockNodeDao.getETagForUpdate(nodeId)).thenReturn(new Long(899));
		// Since the eTag will not match an exception should be thrown.
		String nextTag = nodeManager.validateETagAndLockNode(nodeId, eTag);
		assertNotNull(nextTag);
		assertEquals("900",nextTag);
	}
	
	@Test
	public void testUpdate() throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException{
		// Test that we can update a node.
		Node newNode = new Node();
		newNode.setName("testUpdate");
		newNode.setId("101");
		newNode.seteTag("9");;
		newNode.setType("someType");
		when(mockNodeDao.getETagForUpdate("101")).thenReturn(new Long(9));
		// Make the actual call
		Node result = nodeManager.update("updateUser", newNode);
		assertNotNull(result);
		assertEquals("updateUser", result.getModifiedBy());
		assertNotNull(result.getModifiedOn());
		// The eTag should have been incremented
		assertNotNull("10", result.geteTag());
	}
	
	@Test
	public void testGetAnnotations() throws NotFoundException, DatastoreException, UnauthorizedException{
		String id = "101";
		Annotations annos = new Annotations();
		annos.addAnnotation("stringKey", "a");
		annos.addAnnotation("longKey", Long.MAX_VALUE);
		when(mockNodeDao.getAnnotations(id)).thenReturn(annos);
		Annotations copy = nodeManager.getAnnotations(null, id);
		assertEquals(copy, annos);
	}
	
	
	@Test
	public void testUpdateAnnotations() throws NotFoundException, DatastoreException, UnauthorizedException{
		// To update the annotations 
		String id = "101";
		Annotations annos = new Annotations();
		annos.addAnnotation("stringKey", "b");
		annos.addAnnotation("longKey", Long.MIN_VALUE);
		when(mockNodeDao.getAnnotations(id)).thenReturn(annos);
		Annotations copy = nodeManager.getAnnotations(null, id);
		assertEquals(copy, annos);
	}


}
