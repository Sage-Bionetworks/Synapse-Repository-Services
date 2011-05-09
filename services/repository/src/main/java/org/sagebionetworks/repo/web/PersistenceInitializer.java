package org.sagebionetworks.repo.web;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.model.Bootstrapper;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.util.UserSynchronization;
import org.sagebionetworks.repo.web.controller.BaseController;

public class PersistenceInitializer implements ServletContextListener {
	
	private static final Logger log = Logger.getLogger(PersistenceInitializer.class
			.getName());

	// can't inject dependencies in a ServletContextListener
	// http://stackoverflow.com/questions/4746041/spring-injecting-a-dependency-into-a-servletcontextlistener
	//@Autowired
	private DAOFactory daoFactory = null;
	
	//@Autowired
	Bootstrapper modelBootstrapper = null;

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		try {
			ServletContext context = sce.getServletContext();
			String daoFactoryClassName = context.getInitParameter("org.sagebionetworks.repo.web.PersistenceInitializer.daoFactory");
			String bootstrapperClassName = context.getInitParameter("org.sagebionetworks.repo.web.PersistenceInitializer.modelBootstrapper");
			boolean acceptAllSSLCerts = Boolean.parseBoolean(context.getInitParameter("accept-all-ssl-certs"));
			contextInitialized(daoFactoryClassName, bootstrapperClassName, acceptAllSSLCerts);
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage());
			if (e instanceof RuntimeException) throw (RuntimeException)e; else throw new RuntimeException(e);
		}
	}
	
	// this is broken out from the above to facilitate testing
	public void contextInitialized(String daoFactoryClassName, String bootstrapperClassName, boolean acceptAllSSLCerts) throws Exception {
			this.daoFactory = (DAOFactory)Class.forName(daoFactoryClassName).newInstance();
			this.modelBootstrapper = (Bootstrapper)Class.forName(bootstrapperClassName).newInstance();

			modelBootstrapper.bootstrap();
			
			// perform use mirroring operation
			UserDAO userDAO = daoFactory.getUserDAO(AuthUtilConstants.ADMIN_USER_ID);
			if (acceptAllSSLCerts) CrowdAuthUtil.acceptAllCertificates();
			
			
			UserSynchronization us = new UserSynchronization(userDAO);
			//us.synchronizeUsers();
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}

}
