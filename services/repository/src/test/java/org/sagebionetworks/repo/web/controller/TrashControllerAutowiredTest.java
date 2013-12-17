package org.sagebionetworks.repo.web.controller;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServlet;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TrashControllerAutowiredTest {

	@Autowired
	private EntityService entityService;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private ServletTestHelper servletTestHelper;

	private Long adminUserId;
	private UserInfo adminUserInfo;
	private Long testUserId;
	private UserInfo testUserInfo;
	
	private Entity parent;
	private Entity child;

	@Before
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserInfo = userManager.getUserInfo(adminUserId);
		
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@");
		testUserId = userManager.createUser(user);
		testUserInfo = userManager.getUserInfo(testUserId);
		
		servletTestHelper.setUp();
		
		Assert.assertNotNull(this.entityService);
		parent = new Project();
		parent.setName("TrashControllerAutowiredTest.parent");
		HttpServlet dispatchServlet = DispatchServletSingleton.getInstance();
		parent = ServletTestHelper.createEntity(dispatchServlet, parent, testUserId);
		Assert.assertNotNull(parent);
		child = new Study();
		child.setName("TrashControllerAutowiredTest.child");
		child.setParentId(parent.getId());
		child.setEntityType(Study.class.getName());
		child = ServletTestHelper.createEntity(dispatchServlet, child, testUserId);
		Assert.assertNotNull(child);
		Assert.assertEquals(parent.getId(), child.getParentId());
		EntityHeader benefactor = entityService.getEntityBenefactor(child.getId(), testUserId, null);
		Assert.assertEquals(parent.getId(), benefactor.getId());
	}

	@After
	public void after() throws Exception {
		if (child != null) {
			entityService.deleteEntity(testUserId, child.getId());
		}
		if (parent != null) {
			entityService.deleteEntity(testUserId, parent.getId());
		}
		
		userManager.deletePrincipal(adminUserInfo, Long.parseLong(testUserInfo.getIndividualGroup().getId()));
	}

	@Test
	public void testPurge() throws Exception {
		// The trash can may not be empty before we put anything there
		// So we get base numbers first
		PaginatedResults<TrashedEntity> results = ServletTestHelper.getTrashCan(testUserId);
		long baseTotal = results.getTotalNumberOfResults();
		long baseCount = results.getResults().size();

		// Move the parent to the trash can
		ServletTestHelper.trashEntity(testUserId, parent.getId());

		// Purge the parent
		ServletTestHelper.purgeEntityInTrash(testUserId, parent.getId());

		// Both the parent and the child should be gone
		try {
			ServletTestHelper.getEntity(DispatchServletSingleton.getInstance(), Project.class, parent.getId(), testUserId);
		} catch (NotFoundException e) { }
		
		try {
			ServletTestHelper.getEntity(DispatchServletSingleton.getInstance(), Study.class, child.getId(), testUserId);
		} catch (NotFoundException e) { }

		// The trash can should be empty
		results = ServletTestHelper.getTrashCan(testUserId);
		Assert.assertEquals(baseTotal, results.getTotalNumberOfResults());
		Assert.assertEquals(baseCount, results.getResults().size());
		for (TrashedEntity trash : results.getResults()) {
			if (parent.getId().equals(trash.getEntityId())
					|| child.getId().equals(trash.getEntityId())) {
				Assert.fail();
			}
		}

		// Already purged, no need to clean
		child = null;
		parent = null;
	}

	@Test
	public void testPurgeAll() throws Exception {
		// Move the parent to the trash can
		ServletTestHelper.trashEntity(testUserId, parent.getId());
		
		// Purge the trash can
		ServletTestHelper.purgeTrash(testUserId);

		// Both the parent and the child should be gone
		try {
			ServletTestHelper.getEntity(DispatchServletSingleton.getInstance(), Project.class, parent.getId(), testUserId);
		} catch (NotFoundException e) { }
		
		try {
			ServletTestHelper.getEntity(DispatchServletSingleton.getInstance(), Study.class, child.getId(), testUserId);
		} catch (NotFoundException e) { }

		// The trash can should be empty
		PaginatedResults<TrashedEntity> results = ServletTestHelper.getTrashCan(testUserId);
		Assert.assertEquals(0, results.getTotalNumberOfResults());
		Assert.assertEquals(0, results.getResults().size());
		for (TrashedEntity trash : results.getResults()) {
			if (parent.getId().equals(trash.getEntityId())
					|| child.getId().equals(trash.getEntityId())) {
				Assert.fail();
			}
		}

		// Already purged, no need to clean
		child = null;
		parent = null;
	}

	@Test
	public void testRoundTrip() throws Exception {
		// The trash can may not be empty before we put anything there
		// So we get base numbers first
		PaginatedResults<TrashedEntity> results = ServletTestHelper.getTrashCan(testUserId);
		long baseTotal = results.getTotalNumberOfResults();
		long baseCount = results.getResults().size();

		// Move the parent to the trash can
		ServletTestHelper.trashEntity(testUserId, parent.getId());

		// Now the parent and the child should not be visible
		try {
			ServletTestHelper.getEntity(DispatchServletSingleton.getInstance(), Project.class, parent.getId(), testUserId);
		} catch (NotFoundException e) { }
		
		try {
			ServletTestHelper.getEntity(DispatchServletSingleton.getInstance(), Study.class, child.getId(), testUserId);
		} catch (NotFoundException e) { }

		// The parent and the child should be in the trash can
		results = ServletTestHelper.getTrashCan(testUserId);
		Assert.assertEquals(baseTotal + 2L, results.getTotalNumberOfResults());
		Assert.assertEquals(baseCount + 2L, results.getResults().size());
		Set<String> idSet = new HashSet<String>();
		for (TrashedEntity trash : results.getResults()) {
			idSet.add(trash.getEntityId());
		}
		Assert.assertTrue(idSet.contains(parent.getId()));
		Assert.assertTrue(idSet.contains(child.getId()));

		// Restore the parent
		ServletTestHelper.restoreEntity(testUserId, parent.getId());

		// Now the parent and the child should be visible again
		ServletTestHelper.getEntity(DispatchServletSingleton.getInstance(), Project.class, parent.getId(), testUserId);
		ServletTestHelper.getEntity(DispatchServletSingleton.getInstance(), Study.class, child.getId(), testUserId);

		// The parent and the child should not be in the trash can any more
		results = ServletTestHelper.getTrashCan(testUserId);
		Assert.assertEquals(baseTotal, results.getTotalNumberOfResults());
		Assert.assertEquals(baseCount, results.getResults().size());
		idSet = new HashSet<String>();
		for (TrashedEntity trash : results.getResults()) {
			idSet.add(trash.getEntityId());
		}
		Assert.assertFalse(idSet.contains(parent.getId()));
		Assert.assertFalse(idSet.contains(child.getId()));
	}

	@Test
	public void testAdmin() throws Exception {
		ServletTestHelper.adminPurgeTrash(adminUserId);

		PaginatedResults<TrashedEntity> results = ServletTestHelper.adminGetTrashCan(adminUserId);
		Assert.assertEquals(0, results.getTotalNumberOfResults());
		Assert.assertEquals(0, results.getResults().size());

		// Move the parent to the trash can
		ServletTestHelper.trashEntity(testUserId, parent.getId());

		results = ServletTestHelper.adminGetTrashCan(adminUserId);
		Assert.assertEquals(2, results.getTotalNumberOfResults());
		Assert.assertEquals(2, results.getResults().size());

		// Purge everything
		ServletTestHelper.adminPurgeTrash(adminUserId);

		// Both the parent and the child should be gone
		try {
			ServletTestHelper.getEntity(DispatchServletSingleton.getInstance(), Project.class, parent.getId(), adminUserId);
		} catch (NotFoundException e) { }
		
		try {
			ServletTestHelper.getEntity(DispatchServletSingleton.getInstance(), Study.class, child.getId(), adminUserId);
		} catch (NotFoundException e) { }

		// The trash can should be empty
		results = ServletTestHelper.adminGetTrashCan(adminUserId);
		Assert.assertEquals(0, results.getTotalNumberOfResults());
		Assert.assertEquals(0, results.getResults().size());

		// Already purged, no need to clean
		child = null;
		parent = null;
	}
}
