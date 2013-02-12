package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test to convert from JDO to DTO
 * @author jmhill
 *
 */@RunWith(SpringJUnit4ClassRunner.class)
 @ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class NodeUtilsTest {
	
	@Autowired
	private UserGroupDAO userGroupDAO;

	
	@Test
	public void testRoundTrip() throws DatastoreException, InvalidModelException {
		Long createdById = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId());
		Node node = new Node();
		node.setName("myName");
		node.setDescription("someDescription");
		node.setId(KeyFactory.keyToString(101L));
//		node.setNodeType(ObjectType.project.name());
		node.setCreatedByPrincipalId(createdById);
		node.setModifiedByPrincipalId(createdById);
		node.setETag("1013");
		node.setCreatedOn(new Date(System.currentTimeMillis()+99));
		node.setModifiedOn(new Date(System.currentTimeMillis()+2993));
		// Set the version information
		node.setVersionComment("This is the first version of this object");
		node.setVersionLabel("1.0.1");
//		node.setVersionNumber("2");
		node.setReferences(new HashMap<String, Set<Reference>>());
		node.setActivityId("1234");
		node.setFileHandleId("9999888777");
		// Now create a revision for this node
		DBONode jdoNode = new DBONode();
		DBORevision jdoRev = new DBORevision();
		NodeUtils.updateFromDto(node, jdoNode, jdoRev, false);
		assertEquals("The user cannot change an eTag.", null, jdoNode.geteTag());
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
	public void testJDOParentId() throws DatastoreException{
		String createdById = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId();
		DBONode parent = new DBONode();
		parent.setId(new Long(123));
		DBONode child = new DBONode();
		child.setName("name");
		child.setParentId(parent.getId());
		child.setCreatedOn(System.currentTimeMillis());
		child.setCreatedBy(Long.parseLong(createdById));
		// Make sure the parent id goes to the child
		DBORevision rev = new DBORevision();
		rev.setModifiedBy(Long.parseLong(createdById));
		rev.setModifiedOn(System.currentTimeMillis());
		rev.setRevisionNumber(new Long(21));
		Node dto = NodeUtils.copyFromJDO(child, rev);
		assertNotNull(dto);
		assertEquals(KeyFactory.keyToString(parent.getId()), dto.getParentId());
		assertEquals(new Long(21), dto.getVersionNumber());
	}
	
	@Test
	public void testreplaceFromDto() throws DatastoreException, UnsupportedEncodingException{
		Long createdById = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId());
		Node node = new Node();
		node.setName("myName");
		node.setDescription("someDescription");
		node.setId("101");
		node.setNodeType(EntityType.project.name());
		node.setCreatedByPrincipalId(createdById);
		node.setETag("1013");
		node.setCreatedOn(new Date(10000));
		node.setVersionNumber(2L);
		node.setParentId("456");
		// Now replace all node data
		DBONode dboNode = new DBONode();
		dboNode.setId(101L);
		NodeUtils.replaceFromDto(node, dboNode);
		assertEquals("myName", dboNode.getName());
		assertNotNull(dboNode.getDescription());
		assertEquals("someDescription", new String(dboNode.getDescription(),"UTF-8"));
		assertEquals(createdById, dboNode.getCreatedBy());
		assertEquals(10000l, (long)dboNode.getCreatedOn());
		assertEquals(2l, (long)dboNode.getCurrentRevNumber());
		assertEquals("1013", dboNode.geteTag());
		assertEquals(EntityType.project.getId(), (short)dboNode.getNodeType());
		assertEquals(456l, (long)dboNode.getParentId());
		
		// Make sure parent ID can be null
		node.setParentId(null);
		NodeUtils.replaceFromDto(node, dboNode);
		assertEquals(null, dboNode.getParentId());
		
	}

}
