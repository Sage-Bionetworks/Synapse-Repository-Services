package org.sagebionetworks.repo.model.jdo;

import java.util.Collection;
import java.util.Date;

import javax.jdo.PersistenceManager;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.Bootstrapper;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUser;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUserGroup;

public class JDOBootstrapperImpl implements Bootstrapper {
	/**
	 * This method is called at server start up and takes care of initializing users
	 * (and their groups).  In particular, creates an admin user and an administrative group.
	 * The method has no authorization restraints and is not be exposed by any web service.
	 * 
	 */
	public void bootstrap() throws Exception {
		PersistenceManager pm = PMF.get();
		// ensure public group is created
		JDOUserGroupDAOImpl groupDAO = new JDOUserGroupDAOImpl(AuthUtilConstants.ADMIN_USER_ID);
		JDOUserGroup pg = JDOUserGroupDAOImpl.getPublicGroup(pm);
		if (pg==null) {
			groupDAO.createPublicGroup(pm);				
		}
		JDOUserDAOImpl userDAO = new JDOUserDAOImpl(null/*AuthUtilConstants.ADMIN_USER_ID*/);
		// ensure admin user is created
		JDOUser adminUser = userDAO.getUser(AuthUtilConstants.ADMIN_USER_ID, pm);
		if (adminUser==null) {
			adminUser = new JDOUser();
			adminUser.setCreationDate(new Date());
			adminUser.setUserId(AuthUtilConstants.ADMIN_USER_ID);
			pm.makePersistent(adminUser);
		}
		// ensure admin group is created, and that 'admin' is a member
		JDOUserGroup ag = JDOUserGroupDAOImpl.getAdminGroup(pm);
		if (ag==null) {
			groupDAO.createAdminGroup(pm);
			ag = JDOUserGroupDAOImpl.getAdminGroup(pm);
		}
		groupDAO.addUser(ag, adminUser, pm);
		
		// Don't think that an explicit 'anonymous user' has to be created...
//		// ensure that anonymous user is created
//		JDOUser anonUser = userDAO.getUser(AuthUtilConstants.ANONYMOUS_USER_ID, pm);
//		if (anonUser==null) {
//			anonUser = new JDOUser();
//			anonUser.setCreationDate(new Date());
//			anonUser.setUserId(AuthUtilConstants.ANONYMOUS_USER_ID);
//			pm.makePersistent(anonUser);
//		}
//		// ensure admin group is created, and that 'admin' is a member
//		JDOUserGroup ang = JDOUserGroupDAOImpl.getSystemGroup(AuthUtilConstants.ANONYMOUS_USER_ID, true, pm);
//		if (ang==null) {
//			ang = groupDAO.createIndividualGroup(pm, anonUser);
//		}
		
	}
}
