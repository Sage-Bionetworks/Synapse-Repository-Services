package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupInt;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOUserGroupDAOImplTest {
	
	/**
	 * Allows existing test to pass while refactoring UserGroup names to Principal names
	 */
	@Deprecated
	private static final String AUTHENTICATED_USERS = "AUTHENTICATED_USERS";
	
	@Autowired
	private UserGroupDAO userGroupDAO;
		
	@Autowired
	private UserProfileDAO userProfileDAO;
		
	private List<String> groupsToDelete;
	
	private static final String GROUP_NAME = "DBOUserGroup_TestGroup";

	@Before
	public void setUp() throws Exception {
		groupsToDelete = new ArrayList<String>();
		UserGroup ug = userGroupDAO.findGroup(GROUP_NAME, false);
		if (ug != null) {
			userGroupDAO.delete(ug.getId());
		}
	}

	@After
	public void tearDown() throws Exception {
		for (String todelete: groupsToDelete) {
			userGroupDAO.delete(todelete);
		}
	}
	
	@Test
	public void testRoundTrip() throws Exception {
		UserGroup group = new UserGroup();
		group.setName(GROUP_NAME);
		group.setIsIndividual(false);
		// Give it an ID
		String startingId = "123";
		group.setId(""+startingId);
		long initialCount = userGroupDAO.getCount();
		String groupId = userGroupDAO.create(group);
		assertNotNull(groupId);
		groupsToDelete.add(groupId);
		assertFalse("A new ID should have been issued to the principal",groupId.equals(startingId));
		UserGroup clone = userGroupDAO.get(groupId);
		assertEquals(groupId, clone.getId());
		assertEquals(GROUP_NAME, clone.getName());
		assertEquals(group.getIsIndividual(), clone.getIsIndividual());
		assertEquals(1+initialCount, userGroupDAO.getCount());
	}
	


	@Test
	public void testBootstrapUsers() throws DatastoreException, NotFoundException{
		List<UserGroupInt> boots = this.userGroupDAO.getBootstrapUsers();
		assertNotNull(boots);
		assertTrue(boots.size() >0);
		// Each should exist
		for(UserGroupInt bootUg: boots){
			assertTrue(userGroupDAO.doesPrincipalExist(bootUg.getName()));
			UserGroup ug = userGroupDAO.get(bootUg.getId());
			assertEquals(bootUg.getId(), ug.getId());
			assertEquals(bootUg.getName(), ug.getName());
		}
	}

}
