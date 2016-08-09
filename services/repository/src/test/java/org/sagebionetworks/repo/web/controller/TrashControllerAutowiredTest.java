package org.sagebionetworks.repo.web.controller;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;

public class TrashControllerAutowiredTest extends AbstractAutowiredControllerTestBase {

	@Autowired
	private EntityService entityService;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
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
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		testUserId = userManager.createUser(user);
		groupMembersDAO.addMembers(
				BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(),
				Collections.singletonList(testUserId.toString()));
		testUserInfo = userManager.getUserInfo(testUserId);
		
		Assert.assertNotNull(this.entityService);
		parent = new Project();
		parent.setName("TrashControllerAutowiredTest.parent");
		parent = servletTestHelper.createEntity(dispatchServlet, parent, testUserId);
		Assert.assertNotNull(parent);
		child = new Folder();
		child.setName("TrashControllerAutowiredTest.child");
		child.setParentId(parent.getId());
		child.setEntityType(Folder.class.getName());
		child = servletTestHelper.createEntity(dispatchServlet, child, testUserId);
		Assert.assertNotNull(child);
		Assert.assertEquals(parent.getId(), child.getParentId());
		EntityHeader benefactor = entityService.getEntityBenefactor(child.getId(), testUserId, null);
		Assert.assertEquals(parent.getId(), benefactor.getId());
	}

	@After
	public void after() throws Exception {
		try {
			entityService.deleteEntity(testUserId, child.getId());
		}catch (NotFoundException e){
			//do nothing if already deleted
		}
		try {
			entityService.deleteEntity(testUserId, parent.getId());
		}catch (NotFoundException e){
			//do nothing if already deleted
		}
		
		userManager.deletePrincipal(adminUserInfo, testUserInfo.getId());
	}

	@Test
	public void testPurge() throws Exception {
		// The trash can may not be empty before we put anything there
		// So we get base numbers first
		PaginatedResults<TrashedEntity> results = servletTestHelper.getTrashCan(testUserId);
		long baseTotal = results.getTotalNumberOfResults();
		long baseCount = results.getResults().size();

		// Move the parent to the trash can
		servletTestHelper.trashEntity(testUserId, parent.getId());

		// Purge the parent
		servletTestHelper.purgeEntityInTrash(testUserId, parent.getId());

		// Both the parent and the child should be gone
		try {
			servletTestHelper.getEntity(dispatchServlet, Project.class, parent.getId(), testUserId);
		} catch (NotFoundException e) { }
		
		try {
			servletTestHelper.getEntity(dispatchServlet, Folder.class, child.getId(), testUserId);
		} catch (NotFoundException e) { }

		// The trash can should be empty
		results = servletTestHelper.getTrashCan(testUserId);
		Assert.assertEquals(baseTotal, results.getTotalNumberOfResults());
		Assert.assertEquals(baseCount, results.getResults().size());
		for (TrashedEntity trash : results.getResults()) {
			if (parent.getId().equals(trash.getEntityId())
					|| child.getId().equals(trash.getEntityId())) {
				Assert.fail();
			}
		}

		// Already purged, no need to clean
	}

	@Test
	public void testPurgeAll() throws Exception {
		// Move the parent to the trash can
		servletTestHelper.trashEntity(testUserId, parent.getId());
		
		// Purge the trash can
		servletTestHelper.purgeTrash(testUserId);

		// Both the parent and the child should be gone
		try {
			servletTestHelper.getEntity(dispatchServlet, Project.class, parent.getId(), testUserId);
		} catch (NotFoundException e) { }
		
		try {
			servletTestHelper.getEntity(dispatchServlet, Folder.class, child.getId(), testUserId);
		} catch (NotFoundException e) { }

		// The trash can should be empty
		PaginatedResults<TrashedEntity> results = servletTestHelper.getTrashCan(testUserId);
		Assert.assertEquals(0, results.getTotalNumberOfResults());
		Assert.assertEquals(0, results.getResults().size());
		for (TrashedEntity trash : results.getResults()) {
			if (parent.getId().equals(trash.getEntityId())
					|| child.getId().equals(trash.getEntityId())) {
				Assert.fail();
			}
		}

		// Already purged, no need to clean
	}

	@Test
	public void testRoundTrip() throws Exception {
		// The trash can may not be empty before we put anything there
		// So we get base numbers first
		PaginatedResults<TrashedEntity> results = servletTestHelper.getTrashCan(testUserId);
		long baseTotal = results.getTotalNumberOfResults();
		long baseCount = results.getResults().size();

		// Move the parent to the trash can
		servletTestHelper.trashEntity(testUserId, parent.getId());

		// Now the parent and the child should not be visible
		try {
			servletTestHelper.getEntity(dispatchServlet, Project.class, parent.getId(), testUserId);
		} catch (NotFoundException e) { }
		
		try {
			servletTestHelper.getEntity(dispatchServlet, Folder.class, child.getId(), testUserId);
		} catch (NotFoundException e) { }

		// The parent and the child should be in the trash can
		results = servletTestHelper.getTrashCan(testUserId);
		Assert.assertEquals(baseTotal + 2L, results.getTotalNumberOfResults());
		Assert.assertEquals(baseCount + 2L, results.getResults().size());
		Set<String> idSet = new HashSet<String>();
		for (TrashedEntity trash : results.getResults()) {
			idSet.add(trash.getEntityId());
		}
		Assert.assertTrue(idSet.contains(parent.getId()));
		Assert.assertTrue(idSet.contains(child.getId()));

		// Restore the parent
		servletTestHelper.restoreEntity(testUserId, parent.getId());

		// Now the parent and the child should be visible again
		servletTestHelper.getEntity(dispatchServlet, Project.class, parent.getId(), testUserId);
		servletTestHelper.getEntity(dispatchServlet, Folder.class, child.getId(), testUserId);

		// The parent and the child should not be in the trash can any more
		results = servletTestHelper.getTrashCan(testUserId);
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
		servletTestHelper.adminPurgeTrash(adminUserId);

		PaginatedResults<TrashedEntity> results = servletTestHelper.adminGetTrashCan(adminUserId);
		Assert.assertEquals(0, results.getTotalNumberOfResults());
		Assert.assertEquals(0, results.getResults().size());

		// Move the parent to the trash can
		servletTestHelper.trashEntity(testUserId, parent.getId());

		results = servletTestHelper.adminGetTrashCan(adminUserId);
		Assert.assertEquals(2, results.getTotalNumberOfResults());
		Assert.assertEquals(2, results.getResults().size());

		// Purge everything
		servletTestHelper.adminPurgeTrash(adminUserId);

		// Both the parent and the child should be gone
		try {
			servletTestHelper.getEntity(dispatchServlet, Project.class, parent.getId(), adminUserId);
		} catch (NotFoundException e) { }
		
		try {
			servletTestHelper.getEntity(dispatchServlet, Folder.class, child.getId(), adminUserId);
		} catch (NotFoundException e) { }

		// The trash can should be empty
		results = servletTestHelper.adminGetTrashCan(adminUserId);
		Assert.assertEquals(0, results.getTotalNumberOfResults());
		Assert.assertEquals(0, results.getResults().size());

		// Already purged, no need to clean
	}
	
	@Test
	public void testAdminLeaves() throws Exception{
		//reset trash can
		servletTestHelper.adminPurgeTrash(adminUserId);
		PaginatedResults<TrashedEntity> results = servletTestHelper.adminGetTrashCan(adminUserId);
		Assert.assertEquals(0, results.getTotalNumberOfResults());
		Assert.assertEquals(0, results.getResults().size());

		// Move the parent to the trash can
		servletTestHelper.trashEntity(testUserId, parent.getId());

		results = servletTestHelper.adminGetTrashCan(adminUserId);
		Assert.assertEquals(2, results.getTotalNumberOfResults());
		Assert.assertEquals(2, results.getResults().size());
		
		//purge leaves (i.e. the child)
		servletTestHelper.adminPurgeTrashLeaves(dispatchServlet, adminUserId, 0L, Long.parseLong(ServiceConstants.DEFAULT_DAYS_IN_TRASH_CAN));
		
		//make sure the parent is still in the trashcan
		results = servletTestHelper.adminGetTrashCan(adminUserId);
		Assert.assertEquals(1, results.getTotalNumberOfResults());
		Assert.assertEquals(1, results.getResults().size());
		Assert.assertEquals(parent.getId(), results.getResults().get(0).getEntityId() );
		
		
		
		//delete leaves again to clean up trash can
		servletTestHelper.adminPurgeTrashLeaves(dispatchServlet, adminUserId, 0L, Long.parseLong(ServiceConstants.DEFAULT_DAYS_IN_TRASH_CAN));
		
		results = servletTestHelper.adminGetTrashCan(adminUserId);
		Assert.assertEquals(0, results.getTotalNumberOfResults());
		Assert.assertEquals(0, results.getResults().size());
	}
}
