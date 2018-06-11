package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.doi.DoiClient;
import org.sagebionetworks.doi.EzidClient;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DoiAdminDao;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;

import junit.framework.Assert;

public class DoiControllerAutowiredTest extends AbstractAutowiredControllerTestBase {

	@Autowired
	private EntityService entityService;
	
	@Autowired
	private DoiAdminDao doiAdminDao;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	StackConfiguration stackConfiguration;
	
	
	private Long adminUserId;
	private Entity entity;

	@Before
	public void before() throws Exception {

		Assume.assumeTrue(stackConfiguration.getDoiEnabled());

		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		entity = new Project();
		entity.setName("DoiControllerAutowiredTest");
		entity = servletTestHelper.createEntity(dispatchServlet, entity, adminUserId);
		Assert.assertNotNull(entity);
	}

	@After
	public void after() throws Exception {
		if (stackConfiguration.getDoiEnabled()) {
			entityService.deleteEntity(adminUserId, entity.getId());
			doiAdminDao.clear();
		}
	}

	@Test
	public void testPutGet() throws Exception {

		// Skip the test if the EZID server is down
		DoiClient doiClient = new EzidClient();
		if (!doiClient.isStatusOk()) {
			return;
		}

		// Put without version
		Doi doiPut = servletTestHelper.putDoiWithoutVersion(adminUserId, entity.getId());
		assertNotNull(doiPut);

		// Get without version
		Doi doiGet = servletTestHelper.getDoiWithoutVersion(adminUserId, entity.getId());
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
		doiPut = servletTestHelper.putDoiWithVersion(adminUserId, entity.getId(), 1);
		assertNotNull(doiPut);

		// Get with version
		doiGet = servletTestHelper.getDoiWithVersion(adminUserId, entity.getId(), 1);
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
		
		String entityId = "syn324829389481";

		// Without version
		try {
			servletTestHelper.putDoiWithoutVersion(adminUserId, entityId);
		} catch (NotFoundException e) { }

		try {
			servletTestHelper.getDoiWithoutVersion(adminUserId, entityId);
		} catch (NotFoundException e) { }

		// With version
		try {
			servletTestHelper.putDoiWithVersion(adminUserId, entityId, 1);
		} catch (NotFoundException e) { }

		try {
			servletTestHelper.getDoiWithVersion(adminUserId, entityId, 1);
		} catch (NotFoundException e) { }
	}
}
