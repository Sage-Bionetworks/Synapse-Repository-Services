package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.util.JSONEntityUtil;

public class JSONEntityUtilTest {

	
	@Test
	public void testIsJSONEntity(){
		assertTrue(JSONEntityUtil.isJSONEntity(Project.class));
		assertFalse(JSONEntityUtil.isJSONEntity(Object.class));
	}
}
