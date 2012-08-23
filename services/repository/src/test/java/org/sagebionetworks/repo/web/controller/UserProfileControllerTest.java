package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class UserProfileControllerTest {
	
	@Autowired
	ServletTestHelper testHelper;
	
	@Before
	public void before() throws Exception{
		testHelper.setUp();
	}
	
	@Test
	public void testGetUserGroupHeadersNoFilter() throws ServletException, IOException{
		String prefix = "";
		int limit = 15;
		int offset = 0;
		
		@SuppressWarnings("static-access")
		UserGroupHeaderResponsePage response = testHelper.getUserGroupHeadersByPrefix(prefix, limit, offset);
		assertNotNull(response);
		List<UserGroupHeader> children = response.getChildren();
		assertNotNull(children);
		
		// Should find AUTHENTICATED_USERS group in results.
		Set<String> names = new HashSet<String>();		
		for (UserGroupHeader ugh : children) {
			names.add(ugh.getDisplayName());
		}
		assertTrue("Expected 'AUTHENTICATED_USERS' group, but was not found.", names.contains("AUTHENTICATED_USERS"));
	}
	
	
	
	@Test
	public void testGeUserGroupHeadersWithFilter() throws ServletException, IOException{
		String prefix = "dev";
		int limit = 10;
		int offset = 0;
		
		@SuppressWarnings("static-access")
		UserGroupHeaderResponsePage response = testHelper.getUserGroupHeadersByPrefix(prefix, limit, offset);
		assertNotNull(response);
		List<UserGroupHeader> children = response.getChildren();
		assertNotNull(children);
		
		// Verify prefix filtering.
		for (UserGroupHeader ugh : children) {
			if (!ugh.getDisplayName().toLowerCase().startsWith(prefix.toLowerCase()))
				fail("Invalid user/group returned: '" + ugh.getDisplayName() + "' does not match prefix '" + prefix +"'.");
		}
		
	}
	
}
