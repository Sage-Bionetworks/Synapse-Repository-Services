package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AsynchronousDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.BooleanResult;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Step;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
	@Autowired
	private AsynchronousDAO asynchronousDAO;

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
		otherUserId = userManager.createUser(user);
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
		entityService.getEntity(userId, clone.getId(), mockRequest, Project.class);
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
		assertNotNull(acl.getUri());
		acl = servletTestHelper.updateEntityAcl(dispatchServlet, clone.getId(), acl, userId);
		assertNotNull(acl);
		assertNotNull(acl.getUri());
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
		Study ds = new Study();
		ds.setName("testDataset");
		ds.setParentId(clone.getId());
		Study dsClone = servletTestHelper.createEntity(dispatchServlet, ds, userId);
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
		assertNotNull(acl.getUri());

		// now switch to child

		acl.setId(null);
		AccessControlList childAcl = new AccessControlList();
		// We should be able to start with a null ID per PLFM-410
		childAcl.setId(null);
		childAcl.setResourceAccess(new HashSet<ResourceAccess>());
		// (Is this OK, or do we have to make new ResourceAccess objects inside?)
		// now POST to /dataset/{id}/acl with this acl as the body
		AccessControlList acl2 = servletTestHelper.createEntityACL(dispatchServlet, dsClone.getId(), childAcl, userId);
		assertNotNull(acl2.getUri());
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

	/**
	 * This is a test for PLFM-431.  If you try to get an object where they type does not match the ID
	 * an exception should be thrown.
	 */
	// Not needed if everything is /entity/
	@Ignore
	@Test (expected=IllegalArgumentException.class)
	public void testTypeDoesNotMatchId() throws Exception {
		// First create a project as a non-admin
		Project project = new Project();
		// Make sure we can still set a name to null.  The name should then match the ID.
		project.setName(null);
		Project clone = servletTestHelper.createEntity(dispatchServlet, project, otherUserId);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		// Now try to get the project as a dataset
		Object wrong = servletTestHelper.getEntity(dispatchServlet, Study.class, clone.getId(), otherUserId);

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
		assertEquals(EntityType.project.getEntityType(), type.getType());
		assertEquals(clone.getId(), type.getName());
		assertEquals(clone.getId(), type.getId());
	}

	@Test
	public void testGetEntityBenefactor() throws Exception {
		Project project = new Project();
		project.setName(null);
		project = servletTestHelper.createEntity(dispatchServlet, project, otherUserId);
		;
		toDelete.add(project.getId());
		// Create a dataset
		Study ds = new Study();
		ds.setParentId(project.getId());
		ds = servletTestHelper.createEntity(dispatchServlet, ds, userId);
		assertNotNull(ds);
		toDelete.add(ds.getId());

		// Now get the permission information for the project
		EntityHeader benefactor = servletTestHelper.getEntityBenefactor(dispatchServlet, project.getId(), Project.class, otherUserId);
		assertNotNull(benefactor);
		// The project should be its own benefactor
		assertEquals(project.getId(), benefactor.getId());
		assertEquals(EntityType.project.getEntityType(), benefactor.getType());
		assertEquals(project.getName(), benefactor.getName());

		// Now check the dataset
		benefactor = servletTestHelper.getEntityBenefactor(dispatchServlet, ds.getId(), Study.class, otherUserId);
		assertNotNull(benefactor);
		// The project should be the dataset's benefactor
		assertEquals(project.getId(), benefactor.getId());
		assertEquals(EntityType.project.getEntityType(), benefactor.getType());
		assertEquals(project.getName(), benefactor.getName());

	}

	@Test(expected=IllegalArgumentException.class)
	public void testAclUpdateWithChildType() throws Exception {
		Project project = new Project();
		project.setName(null);
		project = servletTestHelper.createEntity(dispatchServlet, project, otherUserId);
		;
		toDelete.add(project.getId());
		// Create a dataset
		Study ds = new Study();
		ds.setParentId(project.getId());
		ds = servletTestHelper.createEntity(dispatchServlet, ds, userId);
		assertNotNull(ds);
		toDelete.add(ds.getId());

		// Get the ACL for the project
		AccessControlList projectAcl = servletTestHelper.getEntityACL(dispatchServlet, project.getId(), otherUserId);

		// Now attempt to update the ACL as the dataset
		projectAcl = servletTestHelper.updateEntityAcl(dispatchServlet, ds.getId(), projectAcl, otherUserId);
	}

	@Test
	public void testGetEntityReferences() throws Exception , JSONException, NotFoundException {
		// Create project
		Project project = new Project();
		project.setName("testProject");
		Project projectClone = servletTestHelper.createEntity(dispatchServlet, project, userId);
		toDelete.add(projectClone.getId());
		Study dataset = new Study();
		dataset.setName("testDataset");
		dataset.setParentId(projectClone.getId());
		Study datasetClone = servletTestHelper.createEntity(dispatchServlet, dataset, userId);
		toDelete.add(datasetClone.getId());
		// Create a layer
		Data layer = new Data();
		layer.setName("testLayer");
		layer.setVersionNumber((Long)1L);
		layer.setParentId(datasetClone.getId());
		layer.setType(LayerTypeNames.E);
		Data clone = servletTestHelper.createEntity(dispatchServlet, layer, userId);
		assertEquals((Long)1L, clone.getVersionNumber());
		assertNotNull(clone);
		toDelete.add(clone.getId());

		// get references to object
		PaginatedResults<EntityHeader> prs = servletTestHelper.getEntityReferences(dispatchServlet, clone.getId(), userId);
		List<EntityHeader> ehs = prs.getResults();
		assertEquals(0, prs.getTotalNumberOfResults());
		assertEquals(0, ehs.size());

		// add step
		Step step = null;
		Step stepClone = null;
		{
			step = new Step();
			Reference ref = new Reference();
			ref.setTargetId(clone.getId());
			Set<Reference> refs = new HashSet<Reference>();
			refs.add(ref);
			step.setInput(refs);
			stepClone = servletTestHelper.createEntity(dispatchServlet, step, userId);
			toDelete.add(stepClone.getId());
			Set<Reference> refs2 = stepClone.getInput();
			assertEquals(1, refs2.size());
			Reference ref2 = refs2.iterator().next();
			// NOTE:  Since we don't specify the version of the target, it is automatically set to the current version!
			assertEquals(clone.getVersionNumber(), ref2.getTargetVersionNumber());
		}
		// manual update
		updateAnnotationsAndReferences();
		prs = servletTestHelper.getEntityReferences(dispatchServlet, clone.getId(), userId);
		ehs = prs.getResults();
		assertEquals(1, ehs.size());
		assertEquals(stepClone.getId(), ehs.iterator().next().getId());

		// try referencing a specific, nonexistent version
		Long v = clone.getVersionNumber();
		{
			step = new Step();
			Reference ref = new Reference();
			ref.setTargetId(clone.getId());
			ref.setTargetVersionNumber(v+1);
			Set<Reference> refs = new HashSet<Reference>();
			refs.add(ref);
			step.setInput(refs);
			stepClone = servletTestHelper.createEntity(dispatchServlet, step, userId);
			toDelete.add(stepClone.getId());
		}
		// manual update
		updateAnnotationsAndReferences();
		// both Steps refer to some version of the Project
		prs = servletTestHelper.getEntityReferences(dispatchServlet, clone.getId(), userId);
		ehs = prs.getResults();
		assertEquals(ehs.toString(), 2, ehs.size());
		// manual update
		updateAnnotationsAndReferences();
		// only one step refers to version 1 of the Project
		prs = servletTestHelper.getEntityReferences(dispatchServlet, clone.getId(), v, userId);
		ehs = prs.getResults();
		assertEquals(ehs.toString(), 1, ehs.size());
		// manual update
		updateAnnotationsAndReferences();
		// No Step refers to version 100 of the Project
		prs = servletTestHelper.getEntityReferences(dispatchServlet, clone.getId(), v + 99, userId);
		ehs = prs.getResults();
		assertEquals(0, ehs.size());

	}

	/**
	 * Since we have moved the annotation updates to an asynchronous process we need to manually
	 * update the annotations of all nodes for this test. See PLFM-1548
	 * 
	 * @throws NotFoundException
	 */
	public void updateAnnotationsAndReferences() throws NotFoundException {
		for(String id: toDelete){
			asynchronousDAO.createEntity(id);
		}
	}

	@Test
	public void testForPLFM_1096() throws Exception {
		Project project = new Project();
		project.setName(null);
		project = servletTestHelper.createEntity(dispatchServlet, project, otherUserId);
		;
		toDelete.add(project.getId());
		// Create a dataset
		Study ds = new Study();
		ds.setParentId(project.getId());
		ds = servletTestHelper.createEntity(dispatchServlet, ds, userId);
		assertNotNull(ds);
		toDelete.add(ds.getId());
		
		// Create a layer
		Data data = new Data();
		data.setParentId(project.getId());
		data = servletTestHelper.createEntity(dispatchServlet, data, userId);
		assertNotNull(data);
		toDelete.add(data.getId());
		// Make sure we can find both
		QueryResults<Map<String, Object>> qr = servletTestHelper.query(dispatchServlet,
				"select * from study where parentId=='" + project.getId() + "'", userId);
		assertNotNull(qr);
		assertEquals(1, qr.getTotalNumberOfResults());
		assertEquals(ds.getId(), qr.getResults().get(0).get("study.id"));
		
		// Make sure we can find both
		qr = servletTestHelper.query(dispatchServlet, "select * from data where parentId=='" + project.getId() + "'", userId);
		assertNotNull(qr);
		assertEquals(1, qr.getTotalNumberOfResults());
		assertEquals(data.getId(), qr.getResults().get(0).get("data.id"));
		// Make sure we can find both with versionable
		qr = servletTestHelper.query(dispatchServlet, "select * from versionable where parentId=='" + project.getId() + "'", userId);
		assertNotNull(qr);
		assertEquals(2, qr.getTotalNumberOfResults());

	}
}
