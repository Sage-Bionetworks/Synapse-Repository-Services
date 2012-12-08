package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServlet;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class NodeLineageQueryControllerAutowireTest {

	private static final long MAX_WAIT = 60 * 1000; // 1 minute

	@Autowired
	private EntityService entityService;

	private final String userName = TestUserDAO.ADMIN_USER_NAME;
	private Entity testEntityParent;
	private Entity testEntityChild;

	@Before
	public void before() throws Exception {
		testEntityParent = new Project();
		testEntityParent.setName("NodeLineageQueryControllerAutowireTest.parent");
		HttpServlet dispatchServlet = DispatchServletSingleton.getInstance();
		testEntityParent = ServletTestHelper.createEntity(dispatchServlet, testEntityParent, userName);
		Assert.assertNotNull(testEntityParent);
		testEntityChild = new Study();
		testEntityChild.setName("NodeLineageQueryControllerAutowireTest.child");
		testEntityChild.setParentId(testEntityParent.getId());
		testEntityChild.setEntityType(Study.class.getName());
		testEntityChild = ServletTestHelper.createEntity(dispatchServlet, testEntityChild, userName);
		Assert.assertNotNull(testEntityChild);
	}

	@After
	public void after() throws Exception {
		if (testEntityChild != null) {
			entityService.deleteEntity(userName, testEntityChild.getId());
		}
		if (testEntityParent != null) {
			entityService.deleteEntity(userName, testEntityParent.getId());
		}
	}

	@Test
	public void test() throws Exception {

		// getRoot()
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY_ROOT);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		MockHttpServletResponse response = new MockHttpServletResponse();

		HttpServlet servlet = DispatchServletSingleton.getInstance();
		servlet.service(request, response);
		Assert.assertEquals(200, response.getStatus());

		long start = System.currentTimeMillis();
		long end = start;
		String str = response.getContentAsString();
		while (str == null && (end - start) < MAX_WAIT) {
			Thread.sleep(5000L);
			servlet.service(request, response);
			str = response.getContentAsString();
			end = System.currentTimeMillis();
		}
		Assert.assertNotNull(str);

		// getAncestors()
		request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY + "/" + testEntityChild.getId() + "/ancestors");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		str = response.getContentAsString();
		Assert.assertNotNull(str);

		// getParent()
		request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY + "/" + testEntityChild.getId() + "/parent");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		str = response.getContentAsString();
		Assert.assertNotNull(str);

		// getDecendants()
		request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY + "/" + "syn4489" + "/descendants");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		str = response.getContentAsString();
		Assert.assertNotNull(str);

		// getDecendantsWithGeneration()
		request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY + "/" + "syn4489" + "/descendants/2");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		str = response.getContentAsString();
		Assert.assertNotNull(str);

		// getChildren()
		request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY + "/" + "syn4489" + "/children");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		str = response.getContentAsString();
		Assert.assertNotNull(str);
	}
}
