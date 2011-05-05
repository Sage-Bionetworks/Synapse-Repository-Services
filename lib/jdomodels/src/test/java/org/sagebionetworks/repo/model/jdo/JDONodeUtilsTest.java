package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;

/**
 * Test to convert from JDO to DTO
 * @author jmhill
 *
 */
public class JDONodeUtilsTest {
	
	@Test
	public void testRoundTrip(){
		Node node = new Node();
		node.setName("myName");
		node.setDescription("someDescription");
		node.setId("101");
		node.setType("someType");
		node.setCreatedBy("createdByMe");
		node.setModifiedBy("modifiedByMe");
		node.setETag("1013");
		node.setCreatedOn(new Date(System.currentTimeMillis()+99));
		node.setModifiedOn(new Date(System.currentTimeMillis()+2993));
		JDONode jdo = JDONodeUtils.copyFromDto(node);
		assertNotNull(jdo);
		// Make a copy form the jdo
		Node copy = JDONodeUtils.copyFromJDO(jdo);
		assertNotNull(copy);
		// It should match
		assertEquals(copy, node);
	}
	
	@Test
	public void testJDOParentId(){
		JDONode parent = new JDONode();
		parent.setId(new Long(123));
		JDONode child = new JDONode();
		child.setParent(parent);
		// Make sure the parent id goes to the child
		Node dto = JDONodeUtils.copyFromJDO(child);
		assertNotNull(dto);
		assertEquals(parent.getId().toString(), dto.getParentId());
	}

}
