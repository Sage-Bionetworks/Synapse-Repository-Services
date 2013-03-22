package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServlet;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DoiControllerAutowiredTest {

	private final String adminUser = TestUserDAO.ADMIN_USER_NAME;
	private final String testUser = TestUserDAO.TEST_USER_NAME;

	@Before
	public void before() throws Exception {
	}

	@After
	public void after() throws Exception {
	}

	@Test
	public void testPutGet() throws Exception {

		final String uri = UrlHelpers.ENTITY + "/" + 1 + UrlHelpers.DOI;

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, testUser);
		MockHttpServletResponse response = new MockHttpServletResponse();
		HttpServlet servlet = DispatchServletSingleton.getInstance();
		servlet.service(request, response);
		Assert.assertEquals(202, response.getStatus());

		request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, testUser);
		response = new MockHttpServletResponse();
		servlet = DispatchServletSingleton.getInstance();
		servlet.service(request, response);
		Assert.assertEquals(200, response.getStatus());
	}

	@Test
	public void testPutGetWithVersion() throws Exception {

		final String uri = UrlHelpers.ENTITY + "/" + 1 + UrlHelpers.VERSION + "/" + 1 + UrlHelpers.DOI;

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, testUser);
		MockHttpServletResponse response = new MockHttpServletResponse();
		HttpServlet servlet = DispatchServletSingleton.getInstance();
		servlet.service(request, response);
		Assert.assertEquals(202, response.getStatus());

		request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, testUser);
		response = new MockHttpServletResponse();
		servlet = DispatchServletSingleton.getInstance();
		servlet.service(request, response);
		Assert.assertEquals(200, response.getStatus());
	}
}
