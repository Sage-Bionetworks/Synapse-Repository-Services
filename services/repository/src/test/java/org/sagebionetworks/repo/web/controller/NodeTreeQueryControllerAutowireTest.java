package org.sagebionetworks.repo.web.controller;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServlet;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.dynamo.dao.DynamoAdminDao;
import org.sagebionetworks.dynamo.dao.nodetree.DboNodeLineage;
import org.sagebionetworks.dynamo.dao.nodetree.NodeTreeQueryDao;
import org.sagebionetworks.dynamo.dao.nodetree.NodeTreeUpdateDao;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class NodeTreeQueryControllerAutowireTest {

	@Autowired
	private EntityService entityService;

	@Autowired
	private DynamoAdminDao dynamoAdminDao;
	
	@Autowired
	private NodeTreeQueryDao nodeTreeQueryDao;
	
	@Autowired
	private NodeTreeUpdateDao nodeTreeUpdateDao;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private ServletTestHelper servletTestHelper;

	private Long adminUserId;
	private Entity parent;
	private Entity child;
	private List<EntityHeader> rootToChild;

	@Before
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		StackConfiguration config = new StackConfiguration();
		// These tests are not run if dynamo is disabled.
		Assume.assumeTrue(config.getDynamoEnabled());
		
		servletTestHelper.setUp();
		
		Assert.assertNotNull(this.entityService);
		Assert.assertNotNull(this.nodeTreeQueryDao);
		Assert.assertNotNull(this.nodeTreeUpdateDao);

		// Create the entities in RDS
		parent = new Project();
		parent.setName("NodeLineageQueryControllerAutowireTest.parent");
		HttpServlet dispatchServlet = DispatchServletSingleton.getInstance();
		parent = ServletTestHelper.createEntity(dispatchServlet, parent, adminUserId);
		Assert.assertNotNull(parent);
		child = new Study();
		child.setName("NodeLineageQueryControllerAutowireTest.child");
		child.setParentId(parent.getId());
		child.setEntityType(Study.class.getName());
		child = ServletTestHelper.createEntity(dispatchServlet, child, adminUserId);
		Assert.assertNotNull(child);
		Assert.assertEquals(parent.getId(), child.getParentId());
		rootToChild = this.entityService.getEntityPath(adminUserId, child.getId());

		// Clear dynamo first
		this.dynamoAdminDao.clear(DboNodeLineage.TABLE_NAME,
				DboNodeLineage.HASH_KEY_NAME, DboNodeLineage.RANGE_KEY_NAME);

		// Create the entities in dynamo
		this.nodeTreeUpdateDao.create(
				KeyFactory.stringToKey(rootToChild.get(0).getId()).toString(),
				KeyFactory.stringToKey(rootToChild.get(0).getId()).toString(),
				new Date());
		for (int i = 1; i < rootToChild.size(); i++) {
			EntityHeader p = rootToChild.get(i - 1);
			EntityHeader c = rootToChild.get(i);
			this.nodeTreeUpdateDao.create(
					KeyFactory.stringToKey(c.getId()).toString(),
					KeyFactory.stringToKey(p.getId()).toString(),
					new Date());
		}
	}

	@After
	public void after() throws Exception {
		StackConfiguration config = new StackConfiguration();
		// There is nothing to do if dynamo is disabled
		if(!config.getDynamoEnabled()) return;
		// Clear RDS
		if (child != null) {
			entityService.deleteEntity(adminUserId, child.getId());
		}
		if (parent != null) {
			entityService.deleteEntity(adminUserId, parent.getId());
		}
		// Clear dynamo
		this.dynamoAdminDao.clear(DboNodeLineage.TABLE_NAME,
				DboNodeLineage.HASH_KEY_NAME, DboNodeLineage.RANGE_KEY_NAME);
	}

	@Test
	public void test() throws Exception {
		// Get ancestors
		EntityIdList idList = ServletTestHelper.getAncestors(adminUserId, child.getId());
		
		Iterator<EntityId> idIterator = idList.getIdList().iterator();
		EntityId id = idIterator.next();
		Assert.assertNotNull(id);
		final String root = id.getId();
		id = idIterator.next();
		Assert.assertEquals(parent.getId(), id.getId());
		Assert.assertFalse(idIterator.hasNext());

		// Get parents
		id = ServletTestHelper.getParent(adminUserId, child.getId());
		Assert.assertEquals(parent.getId(), id.getId());

		// Get descendants
		idList = ServletTestHelper.getDescendants(adminUserId, root);

		idIterator = idList.getIdList().iterator();
		id = idIterator.next();
		Assert.assertEquals(parent.getId(), id.getId());
		id = idIterator.next();
		Assert.assertEquals(child.getId(), id.getId());
		Assert.assertFalse(idIterator.hasNext());

		// Get descendants with generation
		idList = ServletTestHelper.getDescendantsWithGeneration(adminUserId, root, 2);
		
		idIterator = idList.getIdList().iterator();
		id = idIterator.next();
		Assert.assertEquals(child.getId(), id.getId());
		Assert.assertFalse(idIterator.hasNext());

		// Get children
		idList = ServletTestHelper.getChildren(adminUserId, root);
		idIterator = idList.getIdList().iterator();
		id = idIterator.next();
		Assert.assertEquals(parent.getId(), id.getId());
		Assert.assertFalse(idIterator.hasNext());
	}
}
