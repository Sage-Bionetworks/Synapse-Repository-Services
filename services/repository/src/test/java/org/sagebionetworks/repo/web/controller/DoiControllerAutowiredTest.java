package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.servlet.http.HttpServlet;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.doi.DoiClient;
import org.sagebionetworks.doi.EzidClient;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DoiAdminDao;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiStatus;
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

	@Autowired
	private EntityService entityService;
	
	@Autowired
	private DoiAdminDao doiAdminDao;
	
	@Autowired
	private UserManager userManager;
	
	private String testUser;
	private Entity entity;

	@Before
	public void before() throws Exception {
		testUser = userManager.getGroupName(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString());
		
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

		// Skip the test if the EZID server is down
		DoiClient doiClient = new EzidClient();
		if (!doiClient.isStatusOk()) {
			return;
		}

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
		assertEquals(doiPut.getCreatedBy(), doiGet.getCreatedBy());
		assertEquals(doiPut.getCreatedOn(), doiGet.getCreatedOn());
		assertEquals(doiPut.getObjectType(), doiGet.getObjectType());
		assertEquals(DoiStatus.IN_PROCESS, doiPut.getDoiStatus());
		assertNotNull(doiGet.getDoiStatus());
		assertEquals(doiPut.getEtag(), doiGet.getEtag());
		assertEquals(doiPut.getId(), doiGet.getId());
		assertEquals(doiPut.getObjectId(), doiGet.getObjectId());
		assertEquals(doiPut.getObjectVersion(), doiGet.getObjectVersion());
		assertEquals(doiPut.getUpdatedOn(), doiGet.getUpdatedOn());

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
		assertEquals(doiPut.getCreatedBy(), doiGet.getCreatedBy());
		assertEquals(doiPut.getCreatedOn(), doiGet.getCreatedOn());
		assertEquals(doiPut.getObjectType(), doiGet.getObjectType());
		assertEquals(DoiStatus.IN_PROCESS, doiPut.getDoiStatus());
		assertNotNull(doiGet.getDoiStatus());
		assertEquals(doiPut.getEtag(), doiGet.getEtag());
		assertEquals(doiPut.getId(), doiGet.getId());
		assertEquals(doiPut.getObjectId(), doiGet.getObjectId());
		assertEquals(doiPut.getObjectVersion(), doiGet.getObjectVersion());
		assertEquals(doiPut.getUpdatedOn(), doiGet.getUpdatedOn());
	}

	@Test
	public void testPutGetWithNonExistingNode() throws Exception {

		// Skip the test if the EZID server is down
		DoiClient doiClient = new EzidClient();
		if (!doiClient.isStatusOk()) {
			return;
		}

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
