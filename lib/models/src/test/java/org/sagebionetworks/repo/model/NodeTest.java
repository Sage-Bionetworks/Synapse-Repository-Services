package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Some fields on a Node cannot be set to null
 * @author jmhill
 *
 */
public class NodeTest {
	
	@Test(expected=IllegalArgumentException.class)
	public void testSetNullName(){
		Node node = new Node();
		node.setName(null);
	}
	@Test(expected=IllegalArgumentException.class)
	public void testSetCreatedByNull(){
		Node node = new Node();
		node.setCreatedBy(null);
	}
	@Test(expected=IllegalArgumentException.class)
	public void testSetCreatedOnNull(){
		Node node = new Node();
		node.setCreatedOn(null);
	}
	@Test(expected=IllegalArgumentException.class)
	public void testSetModifiedByNull(){
		Node node = new Node();
		node.setModifiedBy(null);
	}
	@Test(expected=IllegalArgumentException.class)
	public void testSetModifiedOnNull(){
		Node node = new Node();
		node.setModifiedOn(null);
	}
	@Test(expected=IllegalArgumentException.class)
	public void testSetETagNull(){
		Node node = new Node();
		node.setETag(null);
	}
	@Test(expected=IllegalArgumentException.class)
	public void testSetIdNull(){
		Node node = new Node();
		node.setId(null);
	}
	@Test(expected=IllegalArgumentException.class)
	public void testSetNodeTypeNull(){
		Node node = new Node();
		node.setNodeType(null);
	}
	
	@Test
	public void testSetDescriptionNull(){
		Node node = new Node();
		node.setDescription(null);
		assertNull(node.getDescription());
	}
	
	@Test
	public void testSetParentIdNull(){
		Node node = new Node();
		node.setParentId(null);
		assertNull(node.getParentId());
	}

}
