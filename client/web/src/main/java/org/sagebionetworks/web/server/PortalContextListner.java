package org.sagebionetworks.web.server;

import javax.servlet.ServletContextEvent;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;

/**
 * This is where we register out ServletModule with Guice-servlet.
 * 
 * @author John
 *
 */
public class PortalContextListner extends GuiceServletContextListener {

	@Override
	protected Injector getInjector() {
		// This is where we get to set the injector for our servlets
		return Guice.createInjector(new PortalServletModule());
	}


}
