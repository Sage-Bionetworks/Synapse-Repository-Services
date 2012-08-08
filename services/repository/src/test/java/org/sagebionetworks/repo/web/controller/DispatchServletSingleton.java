package org.sagebionetworks.repo.web.controller;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.springframework.mock.web.MockServletConfig;
import org.springframework.web.servlet.DispatcherServlet;


public class DispatchServletSingleton {
	private static HttpServlet instance = null;
	protected DispatchServletSingleton() {
		
	}
	public static HttpServlet getInstance() throws ServletException {
		if (instance == null) {
			MockServletConfig servletConfig = new MockServletConfig("repository");
			servletConfig.addInitParameter("contextConfigLocation", "classpath:test-context.xml");
			instance = new DispatcherServlet();
			instance.init(servletConfig);
		}
		return instance;
	}
}
