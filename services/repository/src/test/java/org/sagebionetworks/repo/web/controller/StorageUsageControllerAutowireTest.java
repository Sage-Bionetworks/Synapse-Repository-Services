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
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.service.EntityService;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class StorageUsageControllerAutowireTest {

	@Autowired
	private EntityService entityController;

	private final String userName = TestUserDAO.ADMIN_USER_NAME;
	private Entity testEntity;

	@Before
	public void before() throws Exception {
		testEntity = new Project();
		testEntity.setName("projectForStorageUsageControllerTest");
		HttpServlet dispatchServlet = DispatchServletSingleton.getInstance();
		testEntity = ServletTestHelper.createEntity(dispatchServlet, testEntity, userName);
		Assert.assertNotNull(testEntity);
	}

	@After
	public void after() throws Exception {
		if (testEntity != null) {
			entityController.deleteEntity(userName, testEntity.getId());
		}
	}

	@Test
	public void testGrandTotals() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.STORAGE_SUMMARY);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, "0");

		MockHttpServletResponse response = new MockHttpServletResponse();

		HttpServlet servlet = DispatchServletSingleton.getInstance();
		servlet.service(request, response);

		Assert.assertEquals(200, response.getStatus());

		String jsonStr = response.getContentAsString();
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonStr);
		StorageUsageSummaryList sus = new StorageUsageSummaryList();
		sus.initializeFromJSONObject(adapter);
		Assert.assertNotNull(sus);
		Assert.assertEquals(0L, sus.getTotalSize().longValue());
		Assert.assertEquals(0, sus.getSummaryList().size());
	}

	@Test
	public void testAggregatedTotals() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.STORAGE_SUMMARY + "/0");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, "0");
		String aggregation = "storage_provider";
		aggregation += ServiceConstants.AGGREGATION_DIMENSION_VALUE_SEPARATOR;
		aggregation += "content_type";
		request.setParameter(ServiceConstants.AGGREGATION_DIMENSION, aggregation);

		MockHttpServletResponse response = new MockHttpServletResponse();

		HttpServlet servlet = DispatchServletSingleton.getInstance();
		servlet.service(request, response);

		Assert.assertEquals(200, response.getStatus());

		String jsonStr = response.getContentAsString();
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonStr);
		StorageUsageSummaryList sus = new StorageUsageSummaryList();
		sus.initializeFromJSONObject(adapter);
		Assert.assertNotNull(sus);
		Assert.assertEquals(0L, sus.getTotalSize().longValue());
		Assert.assertEquals(0, sus.getSummaryList().size());
	}

	@Test
	public void testItemizedUsage() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.STORAGE_DETAILS);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, "0");
		String aggregation = "storage_provider";
		aggregation += ServiceConstants.AGGREGATION_DIMENSION_VALUE_SEPARATOR;
		aggregation += "content_type";
		request.setParameter(ServiceConstants.AGGREGATION_DIMENSION, aggregation);

		MockHttpServletResponse response = new MockHttpServletResponse();

		HttpServlet servlet = DispatchServletSingleton.getInstance();
		servlet.service(request, response);

		Assert.assertEquals(200, response.getStatus());

		String jsonStr = response.getContentAsString();
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonStr);
		PaginatedResults<StorageUsage> results = new PaginatedResults<StorageUsage>();
		results.initializeFromJSONObject(adapter);
		Assert.assertNotNull(results);
	}
}
