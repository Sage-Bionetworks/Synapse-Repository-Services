package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.util.JSONEntityUtil;
import org.sagebionetworks.sample.ExampleContainer;

public class JSONEntityUtilTest {

	
	@Test
	public void testIsJSONEntity(){
		assertTrue(JSONEntityUtil.isJSONEntity(ExampleContainer.class));
		assertFalse(JSONEntityUtil.isJSONEntity(Object.class));
	}

	@Test
	public void testGetSchema(){
		assertEquals(FileEntity.EFFECTIVE_SCHEMA, JSONEntityUtil.getJSONSchema(FileEntity.class));
	}
}
