package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

import com.google.common.collect.Lists;

/**
 * Test to convert from JDO to DTO
 * @author jmhill
 *
 */
public class NodeUtilsTest {
	
	private Long createdById;
	
	@Before 
	public void before() {
		createdById = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	}

	@Test
	public void testRoundTrip() throws DatastoreException, InvalidModelException {
		Node node = new Node();
		node.setName("myName");
		node.setId(KeyFactory.keyToString(101L));
		// This is an update round trip and the type cannot be changed on an update.
		node.setCreatedByPrincipalId(createdById);
		node.setModifiedByPrincipalId(createdById);
		node.setETag("1013");
		node.setCreatedOn(new Date(System.currentTimeMillis()+99));
		node.setModifiedOn(new Date(System.currentTimeMillis()+2993));
		// Set the version information
		node.setVersionComment("This is the first version of this object");
		node.setVersionLabel("1.0.1");
		node.setReference(new Reference());
		node.setActivityId("1234");
		node.setFileHandleId("9999888777");
		List<String> columnIds = new LinkedList<String>();
		columnIds.add("2");
		columnIds.add("1");
		node.setColumnModelIds(columnIds);
		node.setScopeIds(Lists.newArrayList("8","9"));
		// Now create a revision for this node
		DBONode jdoNode = new DBONode();
		DBORevision jdoRev = new DBORevision();
		NodeUtils.updateFromDto(node, jdoNode, jdoRev, false);
		assertEquals("The user cannot change an eTag.", null, jdoNode.getEtag());
		// Set it to make sure the copy works
		jdoNode.seteTag("1013");
		
		// Make a copy form the jdo
		Node copy = NodeUtils.copyFromJDO(jdoNode, jdoRev);
		assertNotNull(copy);
		assertNotNull(copy.getFileHandleId());
		// It should match
		assertEquals(node, copy);
	}
	
	@Test
	public void testExcessivelyLongComment() throws DatastoreException, InvalidModelException {
		Node node = new Node();
		node.setModifiedByPrincipalId(createdById);
		node.setModifiedOn(new Date(System.currentTimeMillis()+2993));
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<DBORevision.MAX_COMMENT_LENGTH+1; i++) sb.append(" ");
		node.setVersionComment(sb.toString());
		// Now create a revision for this node
		DBONode jdoNode = new DBONode();
		
		DBORevision jdoRev = new DBORevision();
		try {
			NodeUtils.updateFromDto(node, jdoNode, jdoRev, false);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}
	
	@Test
	public void testJDOParentId() throws DatastoreException{
		DBONode parent = new DBONode();
		parent.setId(new Long(123));
		DBONode child = new DBONode();
		child.setName("name");
		child.setParentId(parent.getId());
		child.setCreatedOn(System.currentTimeMillis());
		child.setCreatedBy(createdById);
		// Make sure the parent id goes to the child
		DBORevision rev = new DBORevision();
		rev.setModifiedBy(createdById);
		rev.setModifiedOn(System.currentTimeMillis());
		rev.setRevisionNumber(new Long(21));
		Node dto = NodeUtils.copyFromJDO(child, rev);
		assertNotNull(dto);
		assertEquals(KeyFactory.keyToString(parent.getId()), dto.getParentId());
		assertEquals(new Long(21), dto.getVersionNumber());
	}
	
	@Test
	public void nullNode() {
		assertFalse(NodeUtils.isValidNode(null));
	}
	
	@Test
	public void validNode() {
		Node node = createValidNode();
		assertTrue(NodeUtils.isValidNode(node));
	}
		
	private Node createValidNode() {
		Node node = new Node();
		node.setCreatedByPrincipalId(123L);
		node.setCreatedOn(new Date());
		node.setETag("etag");
		node.setId("456");
		node.setModifiedByPrincipalId(123L);
		node.setModifiedOn(new Date());
		node.setName("new node");
		node.setNodeType(EntityType.folder);
		return node;
	}
	
	@Test
	public void testNodeWithNullName(){
		Node node = createValidNode();
		node.setName(null);
		assertFalse(NodeUtils.isValidNode(node));
	}

	@Test
	public void testNodeWithCreatedByNull(){
		Node node = createValidNode();
		node.setCreatedByPrincipalId(null);
		assertFalse(NodeUtils.isValidNode(node));
	}
	@Test
	public void testNodeWithCreatedOnNull(){
		Node node = createValidNode();
		node.setCreatedOn(null);
		assertFalse(NodeUtils.isValidNode(node));
	}
	@Test
	public void testNodeWithModifiedByNull(){
		Node node = createValidNode();
		node.setModifiedByPrincipalId(null);
		assertFalse(NodeUtils.isValidNode(node));
	}
	@Test
	public void testNodeWithModifiedOnNull(){
		Node node = createValidNode();
		node.setModifiedOn(null);
		assertFalse(NodeUtils.isValidNode(node));
	}
	@Test
	public void testNodeWithETagNull(){
		Node node = createValidNode();
		node.setETag(null);
		assertFalse(NodeUtils.isValidNode(node));
	}
	@Test
	public void testNodeWithIdNull(){
		Node node = createValidNode();
		node.setId(null);
		assertFalse(NodeUtils.isValidNode(node));
	}
	@Test
	public void testNodeWithNodeTypeNull(){
		Node node = createValidNode();
		node.setNodeType(null);
		assertFalse(NodeUtils.isValidNode(node));
	}
	
	@Test
	public void testIdListToBytesAndBytesToIdList(){
		List<String> idList = Lists.newArrayList("syn123", "456");
		byte[] bytes = NodeUtils.createByteForIdList(idList);
		List<String> results = NodeUtils.createIdListFromBytes(bytes);
		List<String> expected = Lists.newArrayList("123","456");
		assertEquals(expected, results);
	}
	
	@Test
	public void testIdListToBytesAndBytesToIdListEmpty(){
		List<String> idList = Lists.newArrayList();
		byte[] bytes = NodeUtils.createByteForIdList(idList);
		List<String> results = NodeUtils.createIdListFromBytes(bytes);
		List<String> expected = Lists.newArrayList();
		assertEquals(expected, results);
	}
	
	@Test
	public void testIsProjectOrFolder(){
		for(EntityType type: EntityType.values()){
			if(EntityType.project.equals(type)){
				assertTrue(NodeUtils.isProjectOrFolder(type));
			}else if(EntityType.folder.equals(type)){
				assertTrue(NodeUtils.isProjectOrFolder(type));
			}else{
				assertFalse(NodeUtils.isProjectOrFolder(type));
			}
		}
	}
	
	@Test
	public void testIsRootEntityId(){
		String rootId = StackConfigurationSingleton.singleton().getRootFolderEntityId();
		assertTrue(NodeUtils.isRootEntityId(rootId));
		assertFalse(NodeUtils.isRootEntityId(rootId+"1"));
		Long rootLong = KeyFactory.stringToKey(rootId);
		assertTrue(NodeUtils.isRootEntityId(""+rootLong));
		assertFalse(NodeUtils.isRootEntityId(""+rootLong+1));
	}
}
