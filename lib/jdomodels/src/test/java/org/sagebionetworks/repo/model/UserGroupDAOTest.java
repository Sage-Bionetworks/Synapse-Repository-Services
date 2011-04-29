package org.sagebionetworks.repo.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.jdo.JDOBootstrapperImpl;
import org.sagebionetworks.repo.model.jdo.JDODAOFactoryImpl;
import org.sagebionetworks.repo.model.jdo.JDOUserGroupDAOImpl;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.PMF;
import org.sagebionetworks.repo.model.jdo.persistence.JDOResourceAccess;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUser;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUserGroup;


public class UserGroupDAOTest {
//	private final LocalServiceTestHelper helper = new LocalServiceTestHelper(
//			new LocalDatastoreServiceTestConfig());
	
	private static final Logger log = Logger
	.getLogger(UserGroupDAOTest.class.getName());


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
	private Collection<Long> userIds =null;
	private Collection<Long> groupIds = null;

	@Before
	public void setUp() throws Exception {
		userIds = new HashSet<Long>();
		groupIds = new HashSet<Long>();
		fac = new JDODAOFactoryImpl();
		(new JDOBootstrapperImpl()).bootstrap(); // creat admin user, public group, etc.
	}

	@After
	public void tearDown() throws Exception {
			UserDAO userDAO = fac.getUserDAO(AuthUtilConstants.ADMIN_USER_ID);
			for (Long id : userIds) {
				if (id!=null) userDAO.delete(KeyFactory.keyToString(id));
			}
			UserGroupDAO groupDAO = fac.getUserGroupDAO(AuthUtilConstants.ADMIN_USER_ID);
			for (Long id : groupIds) {
				if (id!=null) groupDAO.delete(KeyFactory.keyToString(id));
			}

			PersistenceManager pm = PMF.get();
			{
				JDOUserGroup publicGroup = JDOUserGroupDAOImpl.getPublicGroup(pm);
				// here are the publicly available resources:
				log.info("Public can access: "+publicGroup.getResourceAccess());
			}

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
	
	private static boolean isInGroup(JDOUserGroup group, String type, String id) throws Exception {
		Set<JDOResourceAccess> ras = group.getResourceAccess();
		boolean foundit=false;
		for (JDOResourceAccess ra : ras) {
			if (ra.getResourceType().equals(type) &&
					ra.getResourceId().longValue()==KeyFactory.stringToKey(id)) {
				foundit=true;
			}
		}
		return foundit;
	}
	
	@Test
	public void testCleanUpAccess() throws Exception {
		// get the public group
		PersistenceManager pm = PMF.get();
		{
			JDOUserGroup publicGroup = JDOUserGroupDAOImpl.getPublicGroup(pm);
			// here are the publicly available resources:
//			log.info("Public can access: "+publicGroup.getResourceAccess());
		}
		// create a resource, anonymously
		UserDAO anonymousUserDAO = fac.getUserDAO(null);
		User user = createUser("TestUser 1");
		anonymousUserDAO.create(user);
		this.userIds.add(KeyFactory.stringToKey(user.getId()));
		
		 pm = PMF.get();
		// here are the publicly available resources:
		JDOUserGroup publicGroup = JDOUserGroupDAOImpl.getPublicGroup(pm);
		// here are the publicly available resources:
//		log.info("After creating user, public can access: "+publicGroup.getResourceAccess());

		// the resource should be publicly accessible
//		log.info("type="+ JDOUser.class.getName());
//		log.info("id="+user.getId());
		Assert.assertTrue(isInGroup(publicGroup, JDOUser.class.getName(), user.getId()));
		
		// delete the resource
		// can't do it anonymously
		UserDAO adminUserDAO = fac.getUserDAO(AuthUtilConstants.ADMIN_USER_ID);
		adminUserDAO.delete(user.getId());
		this.userIds.remove(KeyFactory.stringToKey(user.getId()));
		
		// the resource should be gone from the public group
		 pm = PMF.get();
		 publicGroup = JDOUserGroupDAOImpl.getPublicGroup(pm);
		Assert.assertFalse(isInGroup(publicGroup, JDOUser.class.getName(), user.getId()));
	}

//	@Ignore
	@Test
	public void happyPath() throws Exception {
		// create user anonymously
		UserDAO anonymousUserDAO = fac.getUserDAO(null);
		User user = createUser("TestUser 1");
		anonymousUserDAO.create(user);
		this.userIds.add(KeyFactory.stringToKey(user.getId()));
		
		// Get a DAO with 'TestUser 1's credentials
		UserGroupDAO userGroupDAO = fac.getUserGroupDAO(user.getUserId());
		
//		log.info("Groups visible to "+user.getUserId()+": "+userGroupDAO.getInRange(0,100));
		UserGroupDAO adminUserGroupDAO = fac.getUserGroupDAO(AuthUtilConstants.ADMIN_USER_ID);
//		log.info("Groups visible to "+AuthUtilConstants.ADMIN_USER_ID+": "+adminUserGroupDAO.getInRange(0,100));
		
		// create group anonymously
		UserGroupDAO anonymousGroupDAO = fac.getUserGroupDAO(null);
		//anonymousGroupDAO.getCount();
		UserGroup group = createUserGroup("TestGroup");
		anonymousGroupDAO.create(group);
		this.groupIds.add(KeyFactory.stringToKey(group.getId()));
		
		Assert.assertNotNull(group.getId());
		// now anonymous (Public) should have access to 'TestGroup', since it was created anonymously
		Assert.assertTrue(anonymousGroupDAO.hasAccess(group, "read"));
		Assert.assertTrue(anonymousGroupDAO.hasAccess(group, "change"));
		Assert.assertTrue(anonymousGroupDAO.hasAccess(group, "share"));
		
		// now 'TestUser 1' should have access to 'TestGroup', since it was created anonymously
		Assert.assertTrue(userGroupDAO.hasAccess(group, "read"));
		Assert.assertTrue(userGroupDAO.hasAccess(group, "change"));
		Assert.assertTrue(userGroupDAO.hasAccess(group, "share"));
		
		// create group under 'TestUser 1's credentials
		UserGroup group2 = createUserGroup("TestGroup 2");
		//((JDOUserGroupDAOImpl)userGroupDAO).dumpAllAccess();
		userGroupDAO.create(group2);
		this.groupIds.add(KeyFactory.stringToKey(group2.getId()));
		
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
		this.userIds.add(KeyFactory.stringToKey(user2.getId()));
		
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
		userGroupDAO.addResource(group2, user2, Arrays.asList(new String[]{"read"}));
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
