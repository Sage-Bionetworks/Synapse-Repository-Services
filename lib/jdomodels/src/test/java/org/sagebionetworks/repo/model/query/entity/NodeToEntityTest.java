package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.*;

import org.junit.Test;

public class NodeToEntityTest {
	
	@Test
	public void testTransformIds(){
		assertEquals(123L, NodeToEntity.id.transformerValue("syn123"));
		assertEquals(123L, NodeToEntity.parentId.transformerValue("syn123"));
		assertEquals(123L, NodeToEntity.projectId.transformerValue("syn123"));
		assertEquals(123L, NodeToEntity.benefactorId.transformerValue("syn123"));
	}

}
