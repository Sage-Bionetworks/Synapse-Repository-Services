package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServlet;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.sagebionetworks.repo.web.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class StorageUsageControllerAutowireTest {

	@Autowired
	private EntityService entityController;
	
	@Autowired
	private ServletTestHelper servletTestHelper;
	
	private Long adminUserId;
	private Entity testEntity;

	@Before
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		servletTestHelper.setUp();
		
		testEntity = new Project();
		testEntity.setName("projectForStorageUsageControllerTest");
		HttpServlet dispatchServlet = DispatchServletSingleton.getInstance();
		testEntity = ServletTestHelper.createEntity(dispatchServlet, testEntity, adminUserId);
		Assert.assertNotNull(testEntity);
		
	}

	@After
	public void after() throws Exception {
		if (testEntity != null) {
			entityController.deleteEntity(adminUserId, testEntity.getId());
		}
	}

	@Test
	public void testGrandTotals() throws Exception {
		StorageUsageSummaryList sus = ServletTestHelper.getStorageUsageGrandTotal(adminUserId);
		Assert.assertNotNull(sus);
		Assert.assertEquals(0L, sus.getTotalSize().longValue());
		Assert.assertEquals(0, sus.getSummaryList().size());
	}

	@Test
	public void testAggregatedTotals() throws Exception {
		String aggregation = "storage_provider";
		aggregation += ServiceConstants.AGGREGATION_DIMENSION_VALUE_SEPARATOR;
		aggregation += "content_type";
		StorageUsageSummaryList sus = ServletTestHelper.getStorageUsageAggregatedTotal(adminUserId, aggregation);
		Assert.assertNotNull(sus);
		Assert.assertEquals(0L, sus.getTotalSize().longValue());
		Assert.assertEquals(0, sus.getSummaryList().size());
	}

	@Test
	public void testItemizedUsage() throws Exception {
		String aggregation = "storage_provider";
		aggregation += ServiceConstants.AGGREGATION_DIMENSION_VALUE_SEPARATOR;
		aggregation += "content_type";
		PaginatedResults<StorageUsage> results = ServletTestHelper.getStorageUsageItemized(adminUserId, aggregation);
		Assert.assertNotNull(results);
	}
}
