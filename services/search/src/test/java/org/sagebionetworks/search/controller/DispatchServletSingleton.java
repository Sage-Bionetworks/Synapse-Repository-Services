package org.sagebionetworks.search.controller;

import javax.servlet.ServletException;

import org.springframework.mock.web.MockServletConfig;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * DispatchServlet Singleton used for integration testing.
 * @author jmhill
 *
 */
public class DispatchServletSingleton {
	private static DispatcherServlet instance = null;
	protected DispatchServletSingleton() {
		
	}
	public static DispatcherServlet getInstance() throws ServletException {
		if (instance == null) {
			MockServletConfig servletConfig = new MockServletConfig("search");
			servletConfig.addInitParameter("contextConfigLocation", "classpath:test-context.xml");
			instance = new DispatcherServlet();
			instance.init(servletConfig);
		}
		return instance;
	}
}
