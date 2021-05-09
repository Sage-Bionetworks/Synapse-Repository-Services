package org.sagebionetworks.repo.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.Link;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;

public class TrashControllerAutowiredTest extends AbstractAutowiredControllerJunit5TestBase {

	@Autowired
	private EntityService entityService;
	
	@Autowired
	private TrashManager trashManager;
	
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
	private Entity child2;

	private List<Long> toPurge;
	
	@BeforeEach
	public void before() throws Exception {
		toPurge = new ArrayList<>();
		
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserInfo = userManager.getUserInfo(adminUserId);
		
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		boolean acceptsTermsOfUse = true;
		testUserId = userManager.createOrGetTestUser(adminUserInfo, user, acceptsTermsOfUse).getId();
		groupMembersDAO.addMembers(
				BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(),
				Collections.singletonList(testUserId.toString()));
		testUserInfo = userManager.getUserInfo(testUserId);
		
		assertNotNull(this.entityService);
		parent = new Project();
		parent.setName("TrashControllerAutowiredTest.parent" + UUID.randomUUID().toString());
		parent = servletTestHelper.createEntity(dispatchServlet, parent, testUserId);
		assertNotNull(parent);
		child = new Folder();
		child.setName("TrashControllerAutowiredTest.child" + UUID.randomUUID().toString());
		child.setParentId(parent.getId());
		child = servletTestHelper.createEntity(dispatchServlet, child, testUserId);
		assertNotNull(child);
		assertEquals(parent.getId(), child.getParentId());
		EntityHeader benefactor = entityService.getEntityBenefactor(child.getId(), testUserId);
		assertEquals(parent.getId(), benefactor.getId());
		
		toPurge.add(KeyFactory.stringToKey(child.getId()));
		toPurge.add(KeyFactory.stringToKey(parent.getId()));
		
	}

	@AfterEach
	public void after() throws Exception {
		trashManager.purgeTrash(adminUserInfo, toPurge);
		userManager.deletePrincipal(adminUserInfo, testUserInfo.getId());
	}

	@Test
	public void testFlagForPurge() throws Exception {
		// The trash can may not be empty before we put anything there
		// So we get base numbers first
		PaginatedResults<TrashedEntity> results = servletTestHelper.getTrashCan(testUserId);
		long baseTotal = results.getTotalNumberOfResults();
		long baseCount = results.getResults().size();

		// Move the parent to the trash can
		servletTestHelper.trashEntity(testUserId, parent.getId());

		// Purge the parent
		servletTestHelper.flagEntityForPurge(testUserId, parent.getId());

		// Both the parent and the child should be gone
		assertThrows(NotFoundException.class, () -> {
			servletTestHelper.getEntity(dispatchServlet, Project.class, parent.getId(), testUserId);
		});
		
		assertThrows(NotFoundException.class, () -> {
			servletTestHelper.getEntity(dispatchServlet, Folder.class, child.getId(), testUserId);
		});

		// The trash can should be empty
		results = servletTestHelper.getTrashCan(testUserId);
		
		assertEquals(baseTotal, results.getTotalNumberOfResults());
		assertEquals(baseCount, results.getResults().size());
		
		for (TrashedEntity trash : results.getResults()) {
			assertFalse(parent.getId().equals(trash.getEntityId()) || child.getId().equals(trash.getEntityId()));
		}

	}

	@Test
	public void testPurgeLink() throws Exception { // PLFM-4655
		child2 = new Link();
		child2.setName("TrashControllerAutowiredTest.link" + UUID.randomUUID().toString());
		child2.setParentId(parent.getId());
		child2 = servletTestHelper.createEntity(dispatchServlet, child2, testUserId);
		
		toPurge.add(KeyFactory.stringToKey(child2.getId()));

		// Move the Link to the trash can
		servletTestHelper.trashEntity(testUserId, child2.getId());

		// Purge the Link
		servletTestHelper.flagEntityForPurge(testUserId, child2.getId());

		// Link should be gone
		assertThrows(NotFoundException.class, () -> {
			servletTestHelper.getEntity(dispatchServlet, Link.class, child2.getId(), testUserId);
		});
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
		assertThrows(NotFoundException.class, () -> {
			servletTestHelper.getEntity(dispatchServlet, Project.class, parent.getId(), testUserId);
		});
		
		assertThrows(NotFoundException.class, () -> {
			servletTestHelper.getEntity(dispatchServlet, Folder.class, child.getId(), testUserId);
		});

		// The parent and the child should be in the trash can
		results = servletTestHelper.getTrashCan(testUserId);
		assertEquals(baseTotal + 1L, results.getTotalNumberOfResults());
		assertEquals(baseCount + 1L, results.getResults().size());
		Set<String> idSet = new HashSet<String>();
		for (TrashedEntity trash : results.getResults()) {
			idSet.add(trash.getEntityId());
		}
		assertTrue(idSet.contains(parent.getId()));

		// Restore the parent
		servletTestHelper.restoreEntity(testUserId, parent.getId());

		// Now the parent and the child should be visible again
		servletTestHelper.getEntity(dispatchServlet, Project.class, parent.getId(), testUserId);
		servletTestHelper.getEntity(dispatchServlet, Folder.class, child.getId(), testUserId);

		// The parent and the child should not be in the trash can any more
		results = servletTestHelper.getTrashCan(testUserId);
		assertEquals(baseTotal, results.getTotalNumberOfResults());
		assertEquals(baseCount, results.getResults().size());
		idSet = new HashSet<String>();
		for (TrashedEntity trash : results.getResults()) {
			idSet.add(trash.getEntityId());
		}
		assertFalse(idSet.contains(parent.getId()));
		assertFalse(idSet.contains(child.getId()));
	}
}
