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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AsynchronousDAO;
import org.sagebionetworks.repo.model.BooleanResult;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Step;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.UserInfo;
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
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DefaultControllerAutowiredTest {

	@Autowired
	private EntityService entityService;
	// Used for cleanup
	@Autowired
	private NodeManager nodeManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private AsynchronousDAO asynchronousDAO;

	static private Log log = LogFactory
			.getLog(DefaultControllerAutowiredTest.class);

	private static HttpServlet dispatchServlet;

	private String userName = TestUserDAO.ADMIN_USER_NAME;
	private UserInfo testUser;

	private List<String> toDelete;

	@Before
	public void before() throws DatastoreException, NotFoundException {
		assertNotNull(nodeManager);
		toDelete = new ArrayList<String>();
		// Map test objects to their urls
		// Make sure we have a valid user.
		testUser = userManager.getUserInfo(userName);
		UserInfo.validateUserInfo(testUser);
	}

	@After
	public void after() throws Exception {
		if (nodeManager != null && toDelete != null) {
			UserInfo userInfo = userManager.getUserInfo(userName);
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
	}

	@BeforeClass
	public static void beforeClass() throws ServletException {
		dispatchServlet = DispatchServletSingleton.getInstance();
	}

	@Test(expected = EntityInTrashCanException.class)
	public void testDelete() throws Exception {
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		ServletTestHelper.deleteEntity(dispatchServlet, Project.class, clone.getId(), userName);
		// This should throw an exception
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		entityService.getEntity(userName, clone.getId(), mockRequest, Project.class);
	}

	@Test
	public void testUpdateEntityAcl() throws ServletException, IOException, ACLInheritanceException{
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		AccessControlList acl = ServletTestHelper.getEntityACL(dispatchServlet, clone.getId(), userName);
		assertNotNull(acl);
		assertNotNull(acl.getUri());
		acl = ServletTestHelper.updateEntityAcl(dispatchServlet, clone.getId(), acl, userName);
		assertNotNull(acl);
		assertNotNull(acl.getUri());
	}

	@Test
	public void testCreateEntityAcl() throws ServletException, IOException, ACLInheritanceException{
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		assertNotNull(clone);
		toDelete.add(clone.getId());

		// create a dataset in the project
		Study ds = new Study();
		ds.setName("testDataset");
		ds.setParentId(clone.getId());
		Study dsClone = ServletTestHelper.createEntity(dispatchServlet, ds, userName);
		assertNotNull(dsClone);
		toDelete.add(dsClone.getId());

		AccessControlList acl = null;
		try {
			acl = ServletTestHelper.getEntityACL(dispatchServlet, dsClone.getId(), userName);
			fail("Should have failed to get the ACL of an inheriting node");
		} catch (ACLInheritanceException e) {
			// Get the ACL from the redirect
			acl = ServletTestHelper.getEntityACL(dispatchServlet, e.getBenefactorId(), userName);
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
		AccessControlList acl2 = ServletTestHelper.createEntityACL(dispatchServlet, dsClone.getId(), childAcl, userName);
		assertNotNull(acl2.getUri());
		// now retrieve the acl for the child. should get its own back
		AccessControlList acl3 = ServletTestHelper.getEntityACL(dispatchServlet, dsClone.getId(), userName);
		assertEquals(dsClone.getId(), acl3.getId());

		// now delete the ACL (restore inheritance)
		ServletTestHelper.deleteEntityACL(dispatchServlet, dsClone.getId(), userName);
		// try retrieving the ACL for the child

		// should get the parent's ACL
		AccessControlList acl4 = null;
		try{
			 ServletTestHelper.getEntityACL(dispatchServlet, dsClone.getId(), userName);
		}catch (ACLInheritanceException e){
			acl4 = ServletTestHelper.getEntityACL(dispatchServlet, e.getBenefactorId(), userName);
		}

		assertNotNull(acl4);
		// the returned ACL should refer to the parent
		assertEquals(clone.getId(), acl4.getId());
	}

	@Test
	public void testHasAccess() throws ServletException, IOException{
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		assertNotNull(clone);
		toDelete.add(clone.getId());

		String userId = userName;
		String accessType = ACCESS_TYPE.READ.name();
		assertEquals(new BooleanResult(true), ServletTestHelper.hasAccess(dispatchServlet, Project.class, clone.getId(), userId, accessType));

		userId = "foo"; // arbitrary user shouldn't have access
		assertEquals(new BooleanResult(false), ServletTestHelper.hasAccess(dispatchServlet, Project.class, clone.getId(), userId, accessType));
	}

	/**
	 * This is a test for PLFM-473.
	 * @throws IOException
	 * @throws ServletException
	 */
	@Test
	public void testProjectUpdate() throws ServletException, IOException{
		// Frist create a project as a non-admin
		Project project = new Project();
		// Make sure we can still set a name to null.  The name should then match the ID.
		project.setName(null);
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, TestUserDAO.TEST_USER_NAME);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		assertEquals("The name should match the ID when the name is set to null", clone.getId(), clone.getName());
		// Now make sure this user can update
		String newName = "testProjectUpdatePLFM-473-updated";
		clone.setName("testProjectUpdatePLFM-473-updated");
		clone = ServletTestHelper.updateEntity(dispatchServlet, clone, TestUserDAO.TEST_USER_NAME);
		clone = ServletTestHelper.getEntity(dispatchServlet, Project.class, clone.getId(),  TestUserDAO.TEST_USER_NAME);
		assertEquals(newName, clone.getName());

	}

	/**
	 * This is a test for PLFM-431.  If you try to get an object where they type does not match the ID
	 * an exception should be thrown.
	 * @throws IOException
	 * @throws ServletException
	 */
	// Not needed if everything is /entity/
	@Ignore
	@Test (expected=ServletTestHelperException.class)
	public void testTypeDoesNotMatchId() throws ServletException, IOException{
		// First create a project as a non-admin
		Project project = new Project();
		// Make sure we can still set a name to null.  The name should then match the ID.
		project.setName(null);
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, TestUserDAO.TEST_USER_NAME);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		// Now try to get the project as a dataset
		Object wrong = ServletTestHelper.getEntity(dispatchServlet, Study.class, clone.getId(),  TestUserDAO.TEST_USER_NAME);

	}

	@Test
	public void testGetEntityType() throws ServletException, IOException{
		Project project = new Project();
		project.setName(null);
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, TestUserDAO.TEST_USER_NAME);
		assertNotNull(clone);
		toDelete.add(clone.getId());

		EntityHeader type = ServletTestHelper.getEntityType(dispatchServlet, clone.getId(), TestUserDAO.TEST_USER_NAME);
		assertNotNull(type);
		assertEquals(EntityType.project.getEntityType(), type.getType());
		assertEquals(clone.getId(), type.getName());
		assertEquals(clone.getId(), type.getId());
	}

	@Test
	public void testGetEntityBenefactor() throws ServletException, IOException{
		Project project = new Project();
		project.setName(null);
		project = ServletTestHelper.createEntity(dispatchServlet, project, TestUserDAO.TEST_USER_NAME);;
		toDelete.add(project.getId());
		// Create a dataset
		Study ds = new Study();
		ds.setParentId(project.getId());
		ds = ServletTestHelper.createEntity(dispatchServlet, ds, userName);
		assertNotNull(ds);
		toDelete.add(ds.getId());

		// Now get the permission information for the project
		EntityHeader benefactor = ServletTestHelper.getEntityBenefactor(dispatchServlet, project.getId(), Project.class, TestUserDAO.TEST_USER_NAME);
		assertNotNull(benefactor);
		// The project should be its own benefactor
		assertEquals(project.getId(), benefactor.getId());
		assertEquals(EntityType.project.getEntityType(), benefactor.getType());
		assertEquals(project.getName(), benefactor.getName());

		// Now check the dataset
		benefactor = ServletTestHelper.getEntityBenefactor(dispatchServlet, ds.getId(), Study.class, TestUserDAO.TEST_USER_NAME);
		assertNotNull(benefactor);
		// The project should be the dataset's benefactor
		assertEquals(project.getId(), benefactor.getId());
		assertEquals(EntityType.project.getEntityType(), benefactor.getType());
		assertEquals(project.getName(), benefactor.getName());

	}

	@Test (expected=ServletTestHelperException.class)
	public void testAclUpdateWithChildType() throws ServletException, IOException, ACLInheritanceException{
		Project project = new Project();
		project.setName(null);
		project = ServletTestHelper.createEntity(dispatchServlet, project, TestUserDAO.TEST_USER_NAME);;
		toDelete.add(project.getId());
		// Create a dataset
		Study ds = new Study();
		ds.setParentId(project.getId());
		ds = ServletTestHelper.createEntity(dispatchServlet, ds, userName);
		assertNotNull(ds);
		toDelete.add(ds.getId());

		// Get the ACL for the project
		AccessControlList projectAcl = ServletTestHelper.getEntityACL(dispatchServlet, project.getId(), TestUserDAO.TEST_USER_NAME);

		// Now attempt to update the ACL as the dataset.
		projectAcl = ServletTestHelper.updateEntityAcl(dispatchServlet, ds.getId(), projectAcl, TestUserDAO.TEST_USER_NAME);

	}

	@Test
	public void testGetEntityReferences() throws ServletException, IOException, JSONException, NotFoundException {
		// Create project
		Project project = new Project();
		project.setName("testProject");
		Project projectClone = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		toDelete.add(projectClone.getId());
		Study dataset = new Study();
		dataset.setName("testDataset");
		dataset.setParentId(projectClone.getId());
		Study datasetClone = ServletTestHelper.createEntity(dispatchServlet, dataset, userName);
		toDelete.add(datasetClone.getId());
		// Create a layer
		Data layer = new Data();
		layer.setName("testLayer");
		layer.setVersionNumber((Long)1L);
		layer.setParentId(datasetClone.getId());
		layer.setType(LayerTypeNames.E);
		Data clone = ServletTestHelper.createEntity(dispatchServlet, layer, userName);
		assertEquals((Long)1L, clone.getVersionNumber());
		assertNotNull(clone);
		toDelete.add(clone.getId());

		String userId = userName;

		// get references to object
		PaginatedResults<EntityHeader> prs = ServletTestHelper.getEntityReferences(dispatchServlet, clone.getId(), userId);
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
			stepClone = ServletTestHelper.createEntity(dispatchServlet, step, userName);
			toDelete.add(stepClone.getId());
			Set<Reference> refs2 = stepClone.getInput();
			assertEquals(1, refs2.size());
			Reference ref2 = refs2.iterator().next();
			// NOTE:  Since we don't specify the version of the target, it is automatically set to the current version!
			assertEquals(clone.getVersionNumber(), ref2.getTargetVersionNumber());
		}
		// manual update
		updateAnnotationsAndReferences();
		prs = ServletTestHelper.getEntityReferences(dispatchServlet, clone.getId(), userId);
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
			stepClone = ServletTestHelper.createEntity(dispatchServlet, step, userName);
			toDelete.add(stepClone.getId());
		}
		// manual update
		updateAnnotationsAndReferences();
		// both Steps refer to some version of the Project
		prs = ServletTestHelper.getEntityReferences(dispatchServlet, clone.getId(), userId);
		ehs = prs.getResults();
		assertEquals(ehs.toString(), 2, ehs.size());
		// manual update
		updateAnnotationsAndReferences();
		// only one step refers to version 1 of the Project
		prs = ServletTestHelper.getEntityReferences(dispatchServlet, clone.getId(), v, userId);
		ehs = prs.getResults();
		assertEquals(ehs.toString(), 1, ehs.size());
		// manual update
		updateAnnotationsAndReferences();
		// No Step refers to version 100 of the Project
		prs = ServletTestHelper.getEntityReferences(dispatchServlet, clone.getId(), v+99, userId);
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
	public void testForPLFM_1096() throws ServletException, IOException, ACLInheritanceException{
		Project project = new Project();
		project.setName(null);
		project = ServletTestHelper.createEntity(dispatchServlet, project, TestUserDAO.TEST_USER_NAME);;
		toDelete.add(project.getId());
		// Create a dataset
		Study ds = new Study();
		ds.setParentId(project.getId());
		ds = ServletTestHelper.createEntity(dispatchServlet, ds, userName);
		assertNotNull(ds);
		toDelete.add(ds.getId());
		
		// Create a layer
		Data data = new Data();
		data.setParentId(project.getId());
		data = ServletTestHelper.createEntity(dispatchServlet, data, userName);
		assertNotNull(data);
		toDelete.add(data.getId());
		// Make sure we can find both
		QueryResults<Map<String,Object>> qr = ServletTestHelper.query(dispatchServlet, "select * from study where parentId=='"+project.getId()+"'", userName);
		assertNotNull(qr);
		assertEquals(1, qr.getTotalNumberOfResults());
		assertEquals(ds.getId(), qr.getResults().get(0).get("study.id"));
		
		// Make sure we can find both
		qr = ServletTestHelper.query(dispatchServlet, "select * from data where parentId=='"+project.getId()+"'", userName);
		assertNotNull(qr);
		assertEquals(1, qr.getTotalNumberOfResults());
		assertEquals(data.getId(), qr.getResults().get(0).get("data.id"));
		// Make sure we can find both with versionable
		qr = ServletTestHelper.query(dispatchServlet, "select * from versionable where parentId=='"+project.getId()+"'", userName);
		assertNotNull(qr);
		assertEquals(2, qr.getTotalNumberOfResults());

	}
}
