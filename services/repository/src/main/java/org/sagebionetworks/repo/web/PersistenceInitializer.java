package org.sagebionetworks.repo.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.Bootstrapper;
import org.sagebionetworks.repo.util.UserSynchronization;
import org.springframework.beans.factory.annotation.Autowired;

public class PersistenceInitializer implements ServletContextListener {
	@Autowired
	private CrowdAuthUtil crowdAuthUtil;

	@Autowired
	private DAOFactory daoFactory;
	
	@Autowired
	Bootstrapper modelBootstrapper;

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		try {
			modelBootstrapper.bootstrap();
			
			// perform use mirroring operation
			UserDAO userDAO = daoFactory.getUserDAO(AuthUtilConstants.ADMIN_USER_ID);
			UserSynchronization us = new UserSynchronization(userDAO, crowdAuthUtil);
			us.synchronizeUsers();
		} catch (Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e; else throw new RuntimeException(e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}

}
