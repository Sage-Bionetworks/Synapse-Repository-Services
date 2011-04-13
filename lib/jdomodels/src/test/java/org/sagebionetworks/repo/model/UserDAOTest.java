package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.jdo.JDOBootstrapperImpl;
import org.sagebionetworks.repo.model.jdo.JDODAOFactoryImpl;
import org.sagebionetworks.repo.model.jdo.PMF;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUser;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUserGroup;


public class UserDAOTest {
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
		PersistenceManager pm = null;
		try {
			pm = PMF.get();
			Transaction tx = pm.currentTransaction();
			for (Long id : userIds) {
				tx.begin();
				if (id!=null) pm.deletePersistent(pm.getObjectById(JDOUser.class, id));
				tx.commit();
			}
			for (Long id : groupIds) {
				tx.begin();
				if (id!=null) pm.deletePersistent(pm.getObjectById(JDOUserGroup.class, id));
				tx.commit();
			}
		} finally {
			if (pm != null)
				pm.close();
		}
	}

	private User createUser(String userId) {
		User user = new User();
		user.setUserId(userId);
		user.setCreationDate(new Date());
		return user;
	}
	
	@Test
	public void testUser() throws Exception {
		UserDAO userDAO = fac.getUserDAO(AuthUtilConstants.ADMIN_USER_ID);
		// get all users in the system
		Collection<User> users = userDAO.getInRange(0, 100);
		// since 'set up' created the admin user, there must be at least one user in the system
		assertTrue(users.toString(), users.size()>0);
	}
	

}
