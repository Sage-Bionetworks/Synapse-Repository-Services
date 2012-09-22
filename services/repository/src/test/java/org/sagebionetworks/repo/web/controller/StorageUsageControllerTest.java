package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServlet;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class StorageUsageControllerTest {

	@Test
	public void testGrandTotal() throws Exception {

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
		Assert.assertEquals("0", sus.getUserId());
		Assert.assertEquals(0L, sus.getGrandTotal().longValue());
		Assert.assertEquals(0, sus.getSummaryList().size());
	}

	@Test
	public void testAggregatedTotals() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.STORAGE_SUMMARY + "/0");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, "0");
		request.setParameter(ServiceConstants.STORAGE_DIMENSION_1_PARAM, "storage_provider");
		request.setParameter(ServiceConstants.STORAGE_DIMENSION_2_PARAM, "content_type");

		MockHttpServletResponse response = new MockHttpServletResponse();

		HttpServlet servlet = DispatchServletSingleton.getInstance();
		servlet.service(request, response);

		Assert.assertEquals(200, response.getStatus());

		String jsonStr = response.getContentAsString();
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonStr);
		StorageUsageSummaryList sus = new StorageUsageSummaryList();
		sus.initializeFromJSONObject(adapter);
		Assert.assertNotNull(sus);
		Assert.assertEquals("0", sus.getUserId());
		Assert.assertEquals(0L, sus.getGrandTotal().longValue());
		Assert.assertEquals(0, sus.getSummaryList().size());
	}

	@Test
	public void testItemizedUsage() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.STORAGE_DETAILS);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, "0");
		request.setParameter(ServiceConstants.STORAGE_DIMENSION_1_PARAM, "storage_provider");
		request.setParameter(ServiceConstants.STORAGE_DIMENSION_2_PARAM, "content_type");

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
