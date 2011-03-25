package org.sagebionetworks.repo.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.repo.model.jdo.JDOBootstrapperImpl;
import org.sagebionetworks.repo.model.jdo.JDODAOFactoryImpl;



public class UserGroupDAOTest {
//	private final LocalServiceTestHelper helper = new LocalServiceTestHelper(
//			new LocalDatastoreServiceTestConfig());

	@BeforeClass
	public static void beforeClass() throws Exception {
		// from
		// http://groups.google.com/group/google-appengine-java/browse_thread/thread/96baed75e3c30a58/00d5afb2e0445882?lnk=gst&q=DataNucleus+plugin#00d5afb2e0445882
		// This one caused all the WARNING and SEVERE logs about eclipse UI
		// elements
		Logger.getLogger("DataNucleus.Plugin").setLevel(Level.OFF);
		// This one logged the last couple INFOs about Persistence configuration
		Logger.getLogger("DataNucleus.Persistence").setLevel(Level.WARNING);
	}

	private DAOFactory fac;

	@Before
	public void setUp() throws Exception {
//		helper.setUp();
		fac = new JDODAOFactoryImpl();
		(new JDOBootstrapperImpl()).bootstrap(); // creat admin user, public group, etc.
	}

	@After
	public void tearDown() throws Exception {
//		helper.tearDown();
	}

	private User createUser(String userId) {
		User user = new User();
		user.setUserId(userId);
		user.setCreationDate(new Date());
		return user;
	}
	
	private UserGroup createUserGroup(String name) {
		UserGroup group = new UserGroup();
		group.setName(name);
		group.setCreationDate(new Date());
		return group;
	}

//	@Ignore
	@Test
	public void happyPath() throws Exception {
		// create user anonymously
		UserDAO anonymousUserDAO = fac.getUserDAO(null);
		User user = createUser("TestUser 1");
		anonymousUserDAO.create(user);
		
		// create group anonymously
		UserGroupDAO anonymousGroupDAO = fac.getUserGroupDAO(null);
		//anonymousGroupDAO.getCount();
		UserGroup group = createUserGroup("TestGroup");
		anonymousGroupDAO.create(group);
		
		Assert.assertNotNull(group.getId());
		// now 'TestUser 1' should have access to 'TestGroup', since it was created anonymously
		Assert.assertTrue(anonymousGroupDAO.hasAccess(group, "read"));
		Assert.assertTrue(anonymousGroupDAO.hasAccess(group, "change"));
		Assert.assertTrue(anonymousGroupDAO.hasAccess(group, "share"));
		
		// create group under a user's credentials
		UserGroupDAO userGroupDAO = fac.getUserGroupDAO(user.getUserId());
		UserGroup group2 = createUserGroup("TestGroup 2");
		//((JDOUserGroupDAOImpl)userGroupDAO).dumpAllAccess();
		userGroupDAO.create(group2);
		
		// now 'TestUser 1' should have access to 'TestGroup 2'
		Assert.assertNotNull(group2.getId());
		Assert.assertTrue(userGroupDAO.hasAccess(group2, "read"));
		Assert.assertTrue(userGroupDAO.hasAccess(group2, "change"));
		Assert.assertTrue(userGroupDAO.hasAccess(group2, "share"));
//		// and only the 'TestUser 1' group and admin group should have access
		Assert.assertEquals("id="+group2.getId()+" "+userGroupDAO.whoHasAccess(group2, "read"), 2, userGroupDAO.whoHasAccess(group2, "read").size());
		Set<String> expected = new HashSet<String>(Arrays.asList(new String[]{"TestUser 1", "Administrators"}));
		Set<String> actual = new HashSet<String>();
		for (UserGroup ug: userGroupDAO.whoHasAccess(group2, "read")) actual.add(ug.getName());
		Assert.assertEquals(expected, actual);
		// ... but the Public should have access to the anonymously created group
		expected = new HashSet<String>(Arrays.asList(new String[]{"Public", "Administrators"}));
		actual = new HashSet<String>();
		for (UserGroup ug: userGroupDAO.whoHasAccess(group, "read")) actual.add(ug.getName());
		Assert.assertEquals(expected, actual);
		
		// the 'anonymous' DAO shouldn't see the user's group or the admin group,
		// just the Public one and 'TestGroup'
		Assert.assertEquals(2, anonymousGroupDAO.getCount());
		Collection<UserGroup> publicGroups = anonymousGroupDAO.getInRange(0,100);
//		for (UserGroup ug : publicGroups) {
//			System.out.println(ug.getName()+" "+KeyFactory.stringToKey(ug.getId()));
//		}
		Assert.assertEquals(2, publicGroups.size());
		
		// anonymous DAO should be able to update user
		anonymousUserDAO.update(user);
		
		// but not one made with others' credentials
		UserDAO userDAO2 = fac.getUserDAO(user.getUserId());
		User user2 = createUser("TestUser 2");
		userDAO2.create(user2);
		
		try {
			anonymousUserDAO.update(user2);
			Assert.fail("UnauthorizedException expected!");
		} catch (UnauthorizedException ue) {
			// as expected
		}
		
		// try adding the 'TestUser 2' created by 'TestUser 1'
		// to 'TestGroup 2' as a user
		userGroupDAO.addUser(group2, user2);
		// now as a **resource**
		userGroupDAO.addResource(group2, user2, "read");
		// now get the users in the group
		Assert.assertEquals(1, userGroupDAO.getUsers(group2).size());
		
		
		// second user can't access first user's objects
			// try dao.whoHasAccess
			// try dao.hasAccess
		// create group, for the purpose of accessing the object
		// add second user to group
		// add object to group		
		
		// successfully access object
			// try dao.whoHasAccess
			// try dao.hasAccess
			// try dao.getInRange
		
		
	}
}
