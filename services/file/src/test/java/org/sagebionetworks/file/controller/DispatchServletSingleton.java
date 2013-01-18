package org.sagebionetworks.file.controller;
import javax.servlet.ServletException;

import org.springframework.mock.web.MockServletConfig;
import org.springframework.web.servlet.DispatcherServlet;


public class DispatchServletSingleton {
	private static DispatcherServlet instance = null;
	protected DispatchServletSingleton() {
		
	}
	public static DispatcherServlet getInstance() throws ServletException {
		if (instance == null) {
			MockServletConfig servletConfig = new MockServletConfig("repository");
			servletConfig.addInitParameter("contextConfigLocation", "classpath:test-context.xml");
			instance = new DispatcherServlet();
			instance.init(servletConfig);
		}
		return instance;
	}
}
