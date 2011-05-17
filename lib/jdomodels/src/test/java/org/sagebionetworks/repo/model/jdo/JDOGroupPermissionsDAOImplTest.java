package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.GroupPermissionsDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })

public class JDOGroupPermissionsDAOImplTest {
	
	@Autowired
	private GroupPermissionsDAO groupPermissionsDAO;

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		Collection<String> groupNames = new HashSet<String>();
		groupNames.add(GROUP_NAME);
		Map<String,UserGroup> map = groupPermissionsDAO.getGroupsByNames(groupNames);
		UserGroup toDelete = map.get(GROUP_NAME);
		if (toDelete!=null) {
			groupPermissionsDAO.delete(toDelete.getId());
		}
	}

	private static final String GROUP_NAME = "test-group";
	@Test
	public void testGetGroupsByNames() throws Exception {
		Collection<UserGroup> allGroups = null; 
		allGroups = groupPermissionsDAO.getAll();
		assertEquals(allGroups.toString(), 2, allGroups.size()); // Public and Administrators
	
		Collection<String> groupNames = new HashSet<String>();
		groupNames.add(GROUP_NAME);
		Map<String,UserGroup> map = null;
		map = groupPermissionsDAO.getGroupsByNames(groupNames);
		assertFalse(map.containsKey(GROUP_NAME));
		
		UserGroup group = new UserGroup();
		group.setName(GROUP_NAME);
		groupPermissionsDAO.create(group);
		
		allGroups = groupPermissionsDAO.getAll();
		assertEquals(allGroups.toString(), 3, allGroups.size()); // now the new group should be there
			
		groupNames.clear(); 	groupNames.add(GROUP_NAME);	
		map = groupPermissionsDAO.getGroupsByNames(groupNames);
		assertTrue(groupNames.toString()+" -> "+map.toString(), map.containsKey(GROUP_NAME));
		
		
		groupNames.clear(); groupNames.add(AuthorizationConstants.ADMIN_GROUP_NAME);
		map = groupPermissionsDAO.getGroupsByNames(groupNames);
		assertTrue(map.toString(), map.containsKey(AuthorizationConstants.ADMIN_GROUP_NAME));

	}

}
