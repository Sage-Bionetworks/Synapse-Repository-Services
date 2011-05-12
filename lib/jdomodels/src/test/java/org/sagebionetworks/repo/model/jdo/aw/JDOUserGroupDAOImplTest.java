package org.sagebionetworks.repo.model.jdo.aw;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.jdo.JDOUserGroupDAO;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUser;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodles-test-context.xml" })
public class JDOUserGroupDAOImplTest {
	
	@Autowired
	JDOUserGroupDAO userGroupDAO;
	
	@Autowired
	JDOUserDAO userDAO;
	
	private Collection<String> users = new HashSet<String>();
	private Collection<String> userGroups = new HashSet<String>();

	@Before
	public void setUp() throws Exception {
		UserGroup g = new UserGroup();
		g.setName("Test group");
		userGroupDAO.create(g);
		userGroups.add(g.getId());
	}

	@After
	public void tearDown() throws Exception {
		for (String id : userGroups) {
			userGroupDAO.delete(id);
			userGroups.remove(id);
		}
		for (String id : users) {
			userDAO.delete(id);
			users.remove(id);
		}
	}

	@Test
	@Ignore
	public void testAddUser() throws Exception {
		String gId = userGroups.iterator().next();
		UserGroup g = userGroupDAO.get(gId);
		
		User u = new User();
		u.setUserId("TestUser");
//		userDAO.create(u);
		users.add(u.getId());
		userGroupDAO.addUser(g, Long.parseLong(u.getId()));
		Long uId2 =  userGroupDAO.getUsers(g).iterator().next();
		assertEquals(u.getId(), uId2);
	}

	@Test
	@Ignore
	public void testGetPublicGroup() {
		fail("Not yet implemented");
	}

	@Test
	@Ignore
	public void testGetIndividualGroup() {
		fail("Not yet implemented");
	}



	@Test
	public void testCreatableTypes() throws Exception {
		String gId = userGroups.iterator().next();
		UserGroup g = userGroupDAO.get(gId);
		Set<String> creatableTypes = new HashSet<String>(Arrays.asList(new String[]{"foo"}));
		userGroupDAO.setCreatableTypes(g, creatableTypes);
		UserGroup g2 =userGroupDAO.get(g.getId());
		assertFalse(g==g2);
		assertEquals(g.getId(), g2.getId());
		assertEquals(g.getName(), g2.getName());
		assertEquals(creatableTypes, userGroupDAO.getCreatableTypes(g2));
	}

}
