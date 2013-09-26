package org.sagebionetworks.dynamo.workers.sqs;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.asynchronous.workers.sqs.MessageReceiver;
import org.sagebionetworks.dynamo.dao.DynamoAdminDao;
import org.sagebionetworks.dynamo.dao.nodetree.DboNodeLineage;
import org.sagebionetworks.dynamo.dao.nodetree.NodeTreeQueryDao;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
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

	@Before
	public void before() throws Exception {
		// Empty the dynamo queue
		int count = dynamoQueueMessageRemover.triggerFired();
		while (count > 0) {
			count = dynamoQueueMessageRemover.triggerFired();
		}

		// Clear dynamo by removing the root
		dynamoAdminDao.clear(DboNodeLineage.TABLE_NAME,
				DboNodeLineage.HASH_KEY_NAME, DboNodeLineage.RANGE_KEY_NAME);

		// Create a project
		UserInfo userInfo = userManager.getUserInfo(AuthorizationConstants.TEST_USER_NAME);
		project = new Project();
		project.setName("DynamoQueueWorkerIntegrationTest.Project");
		
		// This should trigger create message.
		String id = entityManager.createEntity(userInfo, project, null);
		project = entityManager.getEntity(userInfo, id, Project.class);
		Assert.assertNotNull(project);
	}

	@After
	public void after() throws Exception {
		// Remove the project
		if (project != null) {
			entityManager.deleteEntity(
					userManager.getUserInfo(AuthorizationConstants.ADMIN_USER_NAME),
					project.getId());
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
		UserInfo userInfo = userManager.getUserInfo(AuthorizationConstants.TEST_USER_NAME);
		List<EntityHeader> path = entityManager.getEntityPath(userInfo,
				project.getId());
		EntityHeader expectedRoot = path.get(0);
		Assert.assertEquals(KeyFactory.stringToKey(expectedRoot.getId())
				.toString(), results.get(0));
	}
}
