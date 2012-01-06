package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import org.junit.Test;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;

/**
 * Test to convert from JDO to DTO
 * @author jmhill
 *
 */
public class JDONodeUtilsTest {
	
	
	@Test
	public void testRoundTrip() throws DatastoreException{
		Node node = new Node();
		node.setName("myName");
		node.setDescription("someDescription");
		node.setId("101");
//		node.setNodeType(ObjectType.project.name());
		node.setCreatedBy("createdByMe");
		node.setModifiedBy("modifiedByMe");
		node.setETag("1013");
		node.setCreatedOn(new Date(System.currentTimeMillis()+99));
		node.setModifiedOn(new Date(System.currentTimeMillis()+2993));
		// Set the version information
		node.setVersionComment("This is the first version of this object");
		node.setVersionLabel("1.0.1");
//		node.setVersionNumber("2");
		node.setReferences(new HashMap<String, Set<Reference>>());
		// Now create a revision for this node
		DBONode jdoNode = new DBONode();
		DBORevision jdoRev = new DBORevision();
		JDONodeUtils.updateFromDto(node, jdoNode, jdoRev);
		assertEquals("The user cannot change an eTag.", null, jdoNode.geteTag());
		// Set it to make sure the copy works
		jdoNode.seteTag(new Long(1013));
		
		// Make a copy form the jdo
		Node copy = JDONodeUtils.copyFromJDO(jdoNode, jdoRev);
		assertNotNull(copy);
		// It should match
		assertEquals(copy, node);
	}
	
	@Test
	public void testJDOParentId() throws DatastoreException{
		DBONode parent = new DBONode();
		parent.setId(new Long(123));
		DBONode child = new DBONode();
		child.setName("name");
		child.setParentId(parent.getId());
		child.setCreatedOn(System.currentTimeMillis());
		child.setCreatedBy("createdBy");
		// Make sure the parent id goes to the child
		DBORevision rev = new DBORevision();
		rev.setModifiedBy("modifiedBy");
		rev.setModifiedOn(System.currentTimeMillis());
		rev.setRevisionNumber(new Long(21));
		Node dto = JDONodeUtils.copyFromJDO(child, rev);
		assertNotNull(dto);
		assertEquals(parent.getId().toString(), dto.getParentId());
		assertEquals(new Long(21), dto.getVersionNumber());
	}

}
