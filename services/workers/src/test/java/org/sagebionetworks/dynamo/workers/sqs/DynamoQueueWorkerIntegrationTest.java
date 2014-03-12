package org.sagebionetworks.dynamo.workers.sqs;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.sqs.MessageReceiver;
import org.sagebionetworks.dynamo.dao.DynamoAdminDao;
import org.sagebionetworks.dynamo.dao.nodetree.DboNodeLineage;
import org.sagebionetworks.dynamo.dao.nodetree.NodeTreeQueryDao;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DynamoQueueWorkerIntegrationTest {

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private UserManager userManager;

	@Autowired
	private DynamoAdminDao dynamoAdminDao;

	@Autowired
	private NodeTreeQueryDao nodeTreeQueryDao;

	@Autowired
	private MessageReceiver dynamoQueueMessageRemover;

	private Project project;
	private UserInfo adminUserInfo;

	@Before
	public void before() throws Exception {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		StackConfiguration config = new StackConfiguration();
		// These tests are not run if dynamo is disabled.
		Assume.assumeTrue(config.getDynamoEnabled());
		// Empty the dynamo queue
		int count = dynamoQueueMessageRemover.triggerFired();
		while (count > 0) {
			count = dynamoQueueMessageRemover.triggerFired();
		}

		// Clear dynamo by removing the root
		dynamoAdminDao.clear(DboNodeLineage.TABLE_NAME,
				DboNodeLineage.HASH_KEY_NAME, DboNodeLineage.RANGE_KEY_NAME);

		// Create a project
		project = new Project();
		project.setName("DynamoQueueWorkerIntegrationTest.Project");
		
		// This should trigger create message.
		String id = entityManager.createEntity(adminUserInfo, project, null);
		project = entityManager.getEntity(adminUserInfo, id, Project.class);
		Assert.assertNotNull(project);
	}

	@After
	public void after() throws Exception {
		StackConfiguration config = new StackConfiguration();
		// There is nothing to do if dynamo is disabled
		if(!config.getDynamoEnabled()) return;
		
		// Remove the project
		if (project != null) {
			entityManager.deleteEntity(adminUserInfo, project.getId());
		}
		// Try to empty the queue
		int count = dynamoQueueMessageRemover.triggerFired();
		while (count > 0) {
			count = dynamoQueueMessageRemover.triggerFired();
		}
		// Clear Dynamo
		dynamoAdminDao.clear(DboNodeLineage.TABLE_NAME,
				DboNodeLineage.HASH_KEY_NAME, DboNodeLineage.RANGE_KEY_NAME);
	}

	@Test
	public void testRoundTrip() throws Exception {
		List<String> results = null;
		int i = 0;
		do {
			// Pause 1 second for eventual consistency
			// Wait at most 60 seconds
			Thread.sleep(2000);
			results = nodeTreeQueryDao.getAncestors(KeyFactory
					.stringToKey(project.getId()).toString());
			i++;
		} while (i < 30 && results.size() == 0);

		Assert.assertNotNull(results);
		Assert.assertTrue(results.size() > 0);
		List<EntityHeader> path = entityManager.getEntityPath(adminUserInfo, project.getId());
		EntityHeader expectedRoot = path.get(0);
		Assert.assertEquals(KeyFactory.stringToKey(expectedRoot.getId())
				.toString(), results.get(0));
	}
}
