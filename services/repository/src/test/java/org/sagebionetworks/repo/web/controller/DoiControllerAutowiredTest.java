package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.servlet.http.HttpServlet;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DoiAdminDao;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.service.EntityService;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DoiControllerAutowiredTest {

	@Autowired private EntityService entityService;
	@Autowired private DoiAdminDao doiAdminDao;
	private final String testUser = TestUserDAO.TEST_USER_NAME;
	private Entity entity;

	@Before
	public void before() throws Exception {
		entity = new Project();
		entity.setName("DoiControllerAutowiredTest");
		HttpServlet dispatchServlet = DispatchServletSingleton.getInstance();
		entity = ServletTestHelper.createEntity(dispatchServlet, entity, testUser);
		Assert.assertNotNull(entity);
	}

	@After
	public void after() throws Exception {
		entityService.deleteEntity(testUser, entity.getId());
		doiAdminDao.clear();
	}

	@Test
	public void testPutGet() throws Exception {

		// Put without version
		String uri = UrlHelpers.ENTITY + "/" + entity.getId() + UrlHelpers.DOI;
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, testUser);
		MockHttpServletResponse response = new MockHttpServletResponse();
		HttpServlet servlet = DispatchServletSingleton.getInstance();
		servlet.service(request, response);
		Assert.assertEquals(HttpStatus.ACCEPTED.value(), response.getStatus());
		String jsonStr = response.getContentAsString();
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonStr);
		Doi doiPut = new Doi();
		doiPut.initializeFromJSONObject(adapter);
		assertNotNull(doiPut);

		// Get without version
		request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, testUser);
		response = new MockHttpServletResponse();
		servlet = DispatchServletSingleton.getInstance();
		servlet.service(request, response);
		Assert.assertEquals(HttpStatus.OK.value(), response.getStatus());
		jsonStr = response.getContentAsString();
		adapter = new JSONObjectAdapterImpl(jsonStr);
		Doi doiGet = new Doi();
		doiGet.initializeFromJSONObject(adapter);
		assertNotNull(doiGet);
		assertEquals(doiPut.getCreatedBy(), doiPut.getCreatedBy());
		assertEquals(doiPut.getCreatedOn(), doiPut.getCreatedOn());
		assertEquals(doiPut.getDoiObjectType(), doiPut.getDoiObjectType());
		assertEquals(doiPut.getDoiStatus(), doiPut.getDoiStatus());
		assertEquals(doiPut.getEtag(), doiPut.getEtag());
		assertEquals(doiPut.getId(), doiPut.getId());
		assertEquals(doiPut.getObjectId(), doiPut.getObjectId());
		assertEquals(doiPut.getObjectVersion(), doiPut.getObjectVersion());
		assertEquals(doiPut.getUpdatedOn(), doiPut.getUpdatedOn());

		// Put with version
		uri = UrlHelpers.ENTITY + "/" + entity.getId() + UrlHelpers.VERSION + "/" + 1 + UrlHelpers.DOI;
		request = new MockHttpServletRequest();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, testUser);
		response = new MockHttpServletResponse();
		servlet = DispatchServletSingleton.getInstance();
		servlet.service(request, response);
		Assert.assertEquals(HttpStatus.ACCEPTED.value(), response.getStatus());
		jsonStr = response.getContentAsString();
		adapter = new JSONObjectAdapterImpl(jsonStr);
		doiPut = new Doi();
		doiPut.initializeFromJSONObject(adapter);
		assertNotNull(doiPut);

		// Get with version
		request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, testUser);
		response = new MockHttpServletResponse();
		servlet = DispatchServletSingleton.getInstance();
		servlet.service(request, response);
		Assert.assertEquals(HttpStatus.OK.value(), response.getStatus());
		jsonStr = response.getContentAsString();
		adapter = new JSONObjectAdapterImpl(jsonStr);
		doiGet = new Doi();
		doiGet.initializeFromJSONObject(adapter);
		assertNotNull(doiGet);
		assertEquals(doiPut.getCreatedBy(), doiPut.getCreatedBy());
		assertEquals(doiPut.getCreatedOn(), doiPut.getCreatedOn());
		assertEquals(doiPut.getDoiObjectType(), doiPut.getDoiObjectType());
		assertEquals(doiPut.getDoiStatus(), doiPut.getDoiStatus());
		assertEquals(doiPut.getEtag(), doiPut.getEtag());
		assertEquals(doiPut.getId(), doiPut.getId());
		assertEquals(doiPut.getObjectId(), doiPut.getObjectId());
		assertEquals(doiPut.getObjectVersion(), doiPut.getObjectVersion());
		assertEquals(doiPut.getUpdatedOn(), doiPut.getUpdatedOn());
	}

	@Test
	public void testPutGetWithNonExistingNode() throws Exception {

		String uri = UrlHelpers.ENTITY + "/" + "syn324829389481" + UrlHelpers.DOI;
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, testUser);
		MockHttpServletResponse response = new MockHttpServletResponse();
		HttpServlet servlet = DispatchServletSingleton.getInstance();
		servlet.service(request, response);
		Assert.assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
		request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, testUser);
		response = new MockHttpServletResponse();
		servlet = DispatchServletSingleton.getInstance();
		servlet.service(request, response);
		Assert.assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());

		uri = UrlHelpers.ENTITY + "/" + "syn324829389481" + UrlHelpers.VERSION + "/" + 1 + UrlHelpers.DOI;
		request = new MockHttpServletRequest();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, testUser);
		response = new MockHttpServletResponse();
		servlet = DispatchServletSingleton.getInstance();
		servlet.service(request, response);
		Assert.assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
		request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, testUser);
		response = new MockHttpServletResponse();
		servlet = DispatchServletSingleton.getInstance();
		servlet.service(request, response);
		Assert.assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
	}
}
