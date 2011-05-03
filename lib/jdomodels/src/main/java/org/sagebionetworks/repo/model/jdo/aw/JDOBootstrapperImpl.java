package org.sagebionetworks.repo.model.jdo.aw;

import java.util.Date;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUser;
import org.sagebionetworks.repo.model.jdo.persistence.JDOUserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoTemplate;

public class JDOBootstrapperImpl implements JDOBootstrapper {
	/**
	 * This method is called at server start up and takes care of initializing users
	 * (and their groups).  In particular, creates an admin user and an administrative group.
	 * The method has no authorization restraints and is not be exposed by any web service.
	 * 
	 */
	
	@Autowired
	JdoTemplate jdoTemplate;
	
	@Autowired
	JDOUserDAO userDAO;
	
	@Autowired
	JDOUserGroupDAO groupDAO;
	

	
	
	public void bootstrap() throws Exception {
		// ensure public group is created
		JDOUserGroup pg = groupDAO.getPublicGroup();
		if (pg==null) {
			groupDAO.createPublicGroup();				
		}
		// ensure admin user is created
		JDOUser adminUser = userDAO.getUser(AuthUtilConstants.ADMIN_USER_ID);
		if (adminUser==null) {
			adminUser = new JDOUser();
			adminUser.setCreationDate(new Date());
			adminUser.setUserId(AuthUtilConstants.ADMIN_USER_ID);
		jdoTemplate.makePersistent(adminUser);
		}
		// ensure admin group is created, and that 'admin' is a member
		JDOUserGroup ag = groupDAO.getAdminGroup();
		if (ag==null) {
			groupDAO.createAdminGroup();
			ag = groupDAO.getAdminGroup();
		}
		groupDAO.addUser(ag, adminUser);
				
	}




	@Override
	public void afterPropertiesSet() throws Exception {
		bootstrap();
	}
}
