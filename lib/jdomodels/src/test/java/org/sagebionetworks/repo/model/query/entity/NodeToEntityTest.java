package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NodeToEntityTest {
	
	@Test
	public void testTransformIds(){
		assertEquals(123L, NodeToEntity.id.transformerValue("syn123"));
		assertEquals(123L, NodeToEntity.parentId.transformerValue("syn123"));
		assertEquals(123L, NodeToEntity.projectId.transformerValue("syn123"));
		assertEquals(123L, NodeToEntity.benefactorId.transformerValue("syn123"));
	}
	
	@Test
	public void testIsNodeField(){
		for(NodeToEntity field: NodeToEntity.values()){
			assertTrue(NodeToEntity.isNodeField(field.name()));
		}
	}
	
	@Test
	public void testIsNodeFieldFalse(){
		assertFalse(NodeToEntity.isNodeField("foo"));
	}
	
	@Test
	public void testIsNodeFieldUsingFieldName(){
		for(NodeToEntity field: NodeToEntity.values()){
			assertTrue(NodeToEntity.isNodeField(field.nodeField.getFieldName()));
		}
	}

}
