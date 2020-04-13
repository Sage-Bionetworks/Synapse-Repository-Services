package org.sagebionetworks.repo.web.controller;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Base class for autowired controller tests
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public abstract class AbstractAutowiredControllerTestBase implements ApplicationContextAware {

	@Autowired
	protected ServletTestHelper servletTestHelper;

	@Autowired
	private OIDCTokenHelper oidcTokenHelper;

	protected DispatcherServlet dispatchServlet;

	protected EntityServletTestHelper entityServletHelper;

	private ApplicationContext parentContext;

	// injected by spring
	public void setApplicationContext(ApplicationContext parentContext) {
		this.parentContext = parentContext;
	}

	@Before
	public void beforeBase() throws Exception {
		MockServletConfig servletConfig = new MockServletConfig("repository");
		servletConfig.addInitParameter("contextConfigLocation", "classpath:test-empty-context.xml");
		StaticWebApplicationContext webApplicationContext = new StaticWebApplicationContext();
		webApplicationContext.setParent(parentContext);
		webApplicationContext.refresh();
		servletConfig.getServletContext().setAttribute(WebApplicationContext.class.getName() + ".ROOT", webApplicationContext);
		dispatchServlet = new DispatcherServlet();
		dispatchServlet.init(servletConfig);
		servletTestHelper.setUp(dispatchServlet);
		entityServletHelper = new EntityServletTestHelper(dispatchServlet, oidcTokenHelper);
	}

	@After
	public void afterBase() throws Exception {
		servletTestHelper.tearDown();
	}

	public ServletTestHelper getServletTestHelper() {
		return servletTestHelper;
	}

	public DispatcherServlet getDispatcherServlet() {
		return dispatchServlet;
	}

	public EntityServletTestHelper getEntityServletTestHelper() {
		return entityServletHelper;
	}
}
