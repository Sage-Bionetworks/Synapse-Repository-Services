package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.BooleanResult;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is a an integration test for the DefaultController.
 *
 * @author jmhill
 *
 */
public class DefaultControllerAutowiredTest extends AbstractAutowiredControllerTestBase {

	@Autowired
	private EntityService entityService;
	// Used for cleanup
	@Autowired
	private NodeManager nodeManager;
	@Autowired
	private UserManager userManager;

	private Long userId;
	private Long otherUserId;
	private UserInfo adminUserInfo;
	private UserInfo otherUserInfo;

	private List<String> toDelete;

	@Before
	public void before() throws DatastoreException, NotFoundException {
		assertNotNull(nodeManager);
		toDelete = new ArrayList<String>();
		
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		// Map test objects to their urls
		// Make sure we have a valid user.
		adminUserInfo = userManager.getUserInfo(userId);
		UserInfo.validateUserInfo(adminUserInfo);
		
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		boolean acceptsTermsOfUse = true;
		otherUserId = userManager.createOrGetTestUser(adminUserInfo, user, acceptsTermsOfUse).getId();
		otherUserInfo = userManager.getUserInfo(otherUserId);
	}

	@After
	public void after() throws Exception {
		if (nodeManager != null && toDelete != null) {
			UserInfo userInfo = userManager.getUserInfo(userId);
			for (String idToDelete : toDelete) {
				try {
					nodeManager.delete(userInfo, idToDelete);
				} catch (NotFoundException e) {
					// nothing to do here
				} catch (DatastoreException e) {
					// nothing to do here.
				}
			}
		}
		
		userManager.deletePrincipal(adminUserInfo, Long.parseLong(otherUserInfo.getId().toString()));
	}

	@Test(expected = EntityInTrashCanException.class)
	public void testDelete() throws Exception {
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = servletTestHelper.createEntity(dispatchServlet, project, userId);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		servletTestHelper.deleteEntity(dispatchServlet, Project.class, clone.getId(), userId);
		// This should throw an exception
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		entityService.getEntity(userId, clone.getId(), Project.class);
	}

	@Test
	public void testUpdateEntityAcl() throws Exception {
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = servletTestHelper.createEntity(dispatchServlet, project, userId);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		AccessControlList acl = servletTestHelper.getEntityACL(dispatchServlet, clone.getId(), userId);
		assertNotNull(acl);
		acl = servletTestHelper.updateEntityAcl(dispatchServlet, clone.getId(), acl, userId);
		assertNotNull(acl);
	}

	@Test
	public void testCreateEntityAcl() throws Exception {
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = servletTestHelper.createEntity(dispatchServlet, project, userId);
		assertNotNull(clone);
		toDelete.add(clone.getId());

		// create a dataset in the project
		Folder ds = new Folder();
		ds.setName("testDataset");
		ds.setParentId(clone.getId());
		Folder dsClone = servletTestHelper.createEntity(dispatchServlet, ds, userId);
		assertNotNull(dsClone);
		toDelete.add(dsClone.getId());

		AccessControlList acl = null;
		try {
			acl = servletTestHelper.getEntityACL(dispatchServlet, dsClone.getId(), userId);
			fail("Should have failed to get the ACL of an inheriting node");
		} catch (ACLInheritanceException e) {
			// Get the ACL from the redirect
			acl = servletTestHelper.getEntityACL(dispatchServlet, e.getBenefactorId(), userId);
		}
		assertNotNull(acl);
		// the returned ACL should refer to the parent
		assertEquals(clone.getId(), acl.getId());

		// now switch to child

		acl.setId(null);
		AccessControlList childAcl = new AccessControlList();
		// We should be able to start with a null ID per PLFM-410
		childAcl.setId(null);
		childAcl.setResourceAccess(new HashSet<ResourceAccess>());
		// (Is this OK, or do we have to make new ResourceAccess objects inside?)
		// now POST to /dataset/{id}/acl with this acl as the body
		AccessControlList acl2 = servletTestHelper.createEntityACL(dispatchServlet, dsClone.getId(), childAcl, userId);
		// now retrieve the acl for the child. should get its own back
		AccessControlList acl3 = servletTestHelper.getEntityACL(dispatchServlet, dsClone.getId(), userId);
		assertEquals(dsClone.getId(), acl3.getId());

		// now delete the ACL (restore inheritance)
		servletTestHelper.deleteEntityACL(dispatchServlet, dsClone.getId(), userId);
		// try retrieving the ACL for the child

		// should get the parent's ACL
		AccessControlList acl4 = null;
		try{
			servletTestHelper.getEntityACL(dispatchServlet, dsClone.getId(), userId);
		}catch (ACLInheritanceException e){
			acl4 = servletTestHelper.getEntityACL(dispatchServlet, e.getBenefactorId(), userId);
		}

		assertNotNull(acl4);
		// the returned ACL should refer to the parent
		assertEquals(clone.getId(), acl4.getId());
	}

	@Test
	public void testHasAccess() throws Exception {
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = servletTestHelper.createEntity(dispatchServlet, project, userId);
		assertNotNull(clone);
		toDelete.add(clone.getId());

		String accessType = ACCESS_TYPE.READ.name();
		assertEquals(new BooleanResult(true), servletTestHelper.hasAccess(dispatchServlet, Project.class, clone.getId(), userId, accessType));

		assertEquals(new BooleanResult(false), 
				servletTestHelper.hasAccess(dispatchServlet, Project.class, clone.getId(), otherUserId, accessType));
	}

	/**
	 * This is a test for PLFM-473.
	 * @throws IOException
	 * @throws ServletException
	 */
	@Test
	public void testProjectUpdate() throws Exception {
		// Frist create a project as a non-admin
		Project project = new Project();
		// Make sure we can still set a name to null.  The name should then match the ID.
		project.setName(null);
		Project clone = servletTestHelper.createEntity(dispatchServlet, project, otherUserId);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		assertEquals("The name should match the ID when the name is set to null", clone.getId(), clone.getName());
		// Now make sure this user can update
		String newName = "testProjectUpdatePLFM-473-updated";
		clone.setName("testProjectUpdatePLFM-473-updated");
		clone = servletTestHelper.updateEntity(dispatchServlet, clone, otherUserId);
		clone = servletTestHelper.getEntity(dispatchServlet, Project.class, clone.getId(), otherUserId);
		assertEquals(newName, clone.getName());

	}

	@Test
	public void testGetEntityType() throws Exception {
		Project project = new Project();
		project.setName(null);
		Project clone = servletTestHelper.createEntity(dispatchServlet, project, otherUserId);
		assertNotNull(clone);
		toDelete.add(clone.getId());

		EntityHeader type = servletTestHelper.getEntityType(dispatchServlet, clone.getId(), otherUserId);
		assertNotNull(type);
		assertEquals(EntityTypeUtils.getEntityTypeClassName(EntityType.project), type.getType());
		assertEquals(clone.getId(), type.getName());
		assertEquals(clone.getId(), type.getId());
	}

	@Test
	public void testGetEntityBenefactor() throws Exception {
		Project project = new Project();
		project.setName(null);
		project = servletTestHelper.createEntity(dispatchServlet, project, otherUserId);
		toDelete.add(project.getId());
		// Create a dataset
		Folder ds = new Folder();
		ds.setParentId(project.getId());
		ds = servletTestHelper.createEntity(dispatchServlet, ds, userId);
		assertNotNull(ds);
		toDelete.add(ds.getId());

		// Now get the permission information for the project
		EntityHeader benefactor = servletTestHelper.getEntityBenefactor(dispatchServlet, project.getId(), Project.class, otherUserId);
		assertNotNull(benefactor);
		// The project should be its own benefactor
		assertEquals(project.getId(), benefactor.getId());
		assertEquals(EntityTypeUtils.getEntityTypeClassName(EntityType.project), benefactor.getType());
		assertEquals(project.getName(), benefactor.getName());

		// Now check the dataset
		benefactor = servletTestHelper.getEntityBenefactor(dispatchServlet, ds.getId(), Folder.class, otherUserId);
		assertNotNull(benefactor);
		// The project should be the dataset's benefactor
		assertEquals(project.getId(), benefactor.getId());
		assertEquals(EntityTypeUtils.getEntityTypeClassName(EntityType.project), benefactor.getType());
		assertEquals(project.getName(), benefactor.getName());

	}

	@Test(expected=IllegalArgumentException.class)
	public void testAclUpdateWithChildType() throws Exception {
		Project project = new Project();
		project.setName(null);
		project = servletTestHelper.createEntity(dispatchServlet, project, otherUserId);
		toDelete.add(project.getId());
		// Create a dataset
		Folder ds = new Folder();
		ds.setParentId(project.getId());
		ds = servletTestHelper.createEntity(dispatchServlet, ds, userId);
		assertNotNull(ds);
		toDelete.add(ds.getId());

		// Get the ACL for the project
		AccessControlList projectAcl = servletTestHelper.getEntityACL(dispatchServlet, project.getId(), otherUserId);

		// Now attempt to update the ACL as the dataset
		projectAcl = servletTestHelper.updateEntityAcl(dispatchServlet, ds.getId(), projectAcl, otherUserId);
	}

}
