package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })

public class JDOUserGroupDAOImplTest {
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	List<String> groupsToDelete;

	@Before
	public void setUp() throws Exception {
		groupsToDelete = new ArrayList<String>();
		UserGroup ug = userGroupDAO.findGroup(GROUP_NAME, false);
		if(ug != null){
			userGroupDAO.delete(ug.getId());
		}
	}

	@After
	public void tearDown() throws Exception {
		if(groupsToDelete != null && userGroupDAO != null){
			for(String todelte: groupsToDelete){
				userGroupDAO.delete(todelte);
			}
		}
	}
	
	
	@Test
	public void findAnonymousUser() throws Exception {
		assertNotNull(userGroupDAO.findGroup(AuthorizationConstants.ANONYMOUS_USER_ID, true));
	}

	private static final String GROUP_NAME = "test-group";
	@Test
	public void testGetGroupsByNames() throws Exception {
		Collection<UserGroup> allGroups = null; 
		allGroups = userGroupDAO.getAll();
		int startingCount =  allGroups.size();
	
		Collection<String> groupNames = new HashSet<String>();
		groupNames.add(GROUP_NAME);
		Map<String,UserGroup> map = null;
		map = userGroupDAO.getGroupsByNames(groupNames);
		assertFalse(map.containsKey(GROUP_NAME));
		
		UserGroup group = new UserGroup();
		group.setName(GROUP_NAME);
		String groupId = userGroupDAO.create(group);
		assertNotNull(groupId);
		groupsToDelete.add(groupId);
		allGroups = userGroupDAO.getAll();
		assertEquals(allGroups.toString(), (startingCount+1), allGroups.size()); // now the new group should be there
			
		groupNames.clear();
		groupNames.add(GROUP_NAME);	
		map = userGroupDAO.getGroupsByNames(groupNames);
		assertTrue(groupNames.toString()+" -> "+map.toString(), map.containsKey(GROUP_NAME));
		
		
		groupNames.clear(); 
		// Add all of the default groups
		groupNames.add(AuthorizationConstants.DEFAULT_GROUPS.AUTHENTICATED_USERS.name());
		map = userGroupDAO.getGroupsByNames(groupNames);
		assertTrue(map.toString(), map.containsKey(AuthorizationConstants.DEFAULT_GROUPS.AUTHENTICATED_USERS.name()));

	}

}
