package org.sagebionetworks.repo.web.controller;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServlet;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.dynamo.dao.NodeTreeDao;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
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
public class NodeLineageQueryControllerAutowireTest {

	@Autowired
	private EntityService entityService;

	@Autowired
	private NodeTreeDao nodeTreeDao;

	private final String testUser = TestUserDAO.ADMIN_USER_NAME;
	private Entity parent;
	private Entity child;
	private List<EntityHeader> rootToChild;

	@Before
	public void before() throws Exception {

		Assert.assertNotNull(this.entityService);
		Assert.assertNotNull(this.nodeTreeDao);

		// Create the entities in RDS
		parent = new Project();
		parent.setName("NodeLineageQueryControllerAutowireTest.parent");
		HttpServlet dispatchServlet = DispatchServletSingleton.getInstance();
		parent = ServletTestHelper.createEntity(dispatchServlet, parent, testUser);
		Assert.assertNotNull(parent);
		child = new Study();
		child.setName("NodeLineageQueryControllerAutowireTest.child");
		child.setParentId(parent.getId());
		child.setEntityType(Study.class.getName());
		child = ServletTestHelper.createEntity(dispatchServlet, child, testUser);
		Assert.assertNotNull(child);
		Assert.assertEquals(parent.getId(), child.getParentId());
		rootToChild = this.entityService.getEntityPath(testUser, child.getId());

		// Clear dynamo first
		String root = this.nodeTreeDao.getRoot();
		if (root != null) {
			this.nodeTreeDao.delete(root, new Date());
		}

		// Create the entities in dynamo
		this.nodeTreeDao.create(
				KeyFactory.stringToKey(rootToChild.get(0).getId()).toString(),
				KeyFactory.stringToKey(rootToChild.get(0).getId()).toString(),
				new Date());
		for (int i = 1; i < rootToChild.size(); i++) {
			EntityHeader p = rootToChild.get(i - 1);
			EntityHeader c = rootToChild.get(i);
			this.nodeTreeDao.create(
					KeyFactory.stringToKey(c.getId()).toString(),
					KeyFactory.stringToKey(p.getId()).toString(),
					new Date());
		}		
	}

	@After
	public void after() throws Exception {
		// Clear RDS
		if (child != null) {
			entityService.deleteEntity(testUser, child.getId());
		}
		if (parent != null) {
			entityService.deleteEntity(testUser, parent.getId());
		}
		// Clear dynamo
		String root = this.nodeTreeDao.getRoot();
		if (root != null) {
			this.nodeTreeDao.delete(root, new Date());
		}
	}

	@Test
	public void test() throws Exception {

		// getRoot()
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY_ROOT);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, testUser);
		MockHttpServletResponse response = new MockHttpServletResponse();
		HttpServlet servlet = DispatchServletSingleton.getInstance();
		servlet.service(request, response);

		Assert.assertEquals(200, response.getStatus());
		String jsonStr = response.getContentAsString();
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonStr);
		EntityId id = new EntityId();
		id.initializeFromJSONObject(adapter);
		String root = this.rootToChild.get(0).getId();
		Assert.assertEquals(root, id.getId());

		// getAncestors()
		request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY + "/" + child.getId() + "/ancestors");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, testUser);
		response = new MockHttpServletResponse();
		servlet.service(request, response);

		Assert.assertEquals(200, response.getStatus());
		jsonStr = response.getContentAsString();
		adapter = new JSONObjectAdapterImpl(jsonStr);
		EntityIdList idList = new EntityIdList();
		idList.initializeFromJSONObject(adapter);
		Iterator<EntityId> idIterator = idList.getIdList().iterator();
		id = idIterator.next();
		Assert.assertEquals(root, id.getId());
		id = idIterator.next();
		Assert.assertEquals(parent.getId(), id.getId());
		Assert.assertFalse(idIterator.hasNext());

		// getParent()
		request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY + "/" + child.getId() + "/parent");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, testUser);
		response = new MockHttpServletResponse();
		servlet.service(request, response);

		Assert.assertEquals(200, response.getStatus());
		jsonStr = response.getContentAsString();
		adapter = new JSONObjectAdapterImpl(jsonStr);
		id = new EntityId();
		id.initializeFromJSONObject(adapter);
		Assert.assertEquals(parent.getId(), id.getId());

		// getDecendants()
		request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY + "/" + root + "/descendants");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, testUser);
		response = new MockHttpServletResponse();
		servlet.service(request, response);

		Assert.assertEquals(200, response.getStatus());
		jsonStr = response.getContentAsString();
		adapter = new JSONObjectAdapterImpl(jsonStr);
		idList = new EntityIdList();
		idList.initializeFromJSONObject(adapter);
		idIterator = idList.getIdList().iterator();
		id = idIterator.next();
		Assert.assertEquals(parent.getId(), id.getId());
		id = idIterator.next();
		Assert.assertEquals(child.getId(), id.getId());
		Assert.assertFalse(idIterator.hasNext());

		// getDecendantsWithGeneration()
		request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY + "/" + root + "/descendants/2");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, testUser);
		response = new MockHttpServletResponse();
		servlet.service(request, response);

		Assert.assertEquals(200, response.getStatus());
		jsonStr = response.getContentAsString();
		adapter = new JSONObjectAdapterImpl(jsonStr);
		idList = new EntityIdList();
		idList.initializeFromJSONObject(adapter);
		idIterator = idList.getIdList().iterator();
		id = idIterator.next();
		Assert.assertEquals(child.getId(), id.getId());
		Assert.assertFalse(idIterator.hasNext());

		// getChildren()
		request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY + "/" + root + "/children");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, testUser);
		response = new MockHttpServletResponse();
		servlet.service(request, response);

		Assert.assertEquals(200, response.getStatus());
		jsonStr = response.getContentAsString();
		adapter = new JSONObjectAdapterImpl(jsonStr);
		idList = new EntityIdList();
		idList.initializeFromJSONObject(adapter);
		idIterator = idList.getIdList().iterator();
		id = idIterator.next();
		Assert.assertEquals(parent.getId(), id.getId());
		Assert.assertFalse(idIterator.hasNext());
	}
}
