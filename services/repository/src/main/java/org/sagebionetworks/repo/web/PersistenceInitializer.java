package org.sagebionetworks.repo.web;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.model.Bootstrapper;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.util.UserSynchronization;

public class PersistenceInitializer implements ServletContextListener {
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
			this.daoFactory = (DAOFactory)Class.forName(daoFactoryClassName).newInstance();
			String bootstrapperClassName = context.getInitParameter("org.sagebionetworks.repo.web.PersistenceInitializer.modelBootstrapper");
			this.modelBootstrapper = (Bootstrapper)Class.forName(bootstrapperClassName).newInstance();

			modelBootstrapper.bootstrap();
			
			// perform use mirroring operation
			UserDAO userDAO = daoFactory.getUserDAO(AuthUtilConstants.ADMIN_USER_ID);
			boolean acceptAllSSLCerts = Boolean.parseBoolean(context.getInitParameter("accept-all-ssl-certs"));
			if (acceptAllSSLCerts) CrowdAuthUtil.acceptAllCertificates();
			
			
			UserSynchronization us = new UserSynchronization(userDAO);
			// us.synchronizeUsers();
		} catch (Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e; else throw new RuntimeException(e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}

}
