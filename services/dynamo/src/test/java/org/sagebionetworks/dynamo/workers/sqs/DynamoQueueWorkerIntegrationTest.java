package org.sagebionetworks.dynamo.workers.sqs;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.asynchronous.workers.sqs.MessageReceiver;
import org.sagebionetworks.dynamo.dao.NodeTreeDao;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DynamoQueueWorkerIntegrationTest {

	public static final long MAX_WAIT = 60 * 1000; // one minute

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private UserProvider userProvider;

	@Autowired
	private NodeTreeDao nodeTreeDao;

	@Autowired
	private MessageReceiver dynamoQueueMessageRetriever;

	private Project project;

	@Before
	public void before() throws Exception {

		Assert.assertNotNull(this.entityManager);
		Assert.assertNotNull(this.userProvider);
		Assert.assertNotNull(this.nodeTreeDao);
		Assert.assertNotNull(this.dynamoQueueMessageRetriever);

		// Empty the dynamo queue
		int count = this.dynamoQueueMessageRetriever.triggerFired();
		while (count > 0) {
			count = this.dynamoQueueMessageRetriever.triggerFired();
		}

		// Clear dynamo by removing the root
		String root = this.nodeTreeDao.getRoot();
		if (root != null) {
			this.nodeTreeDao.delete(root, new Date());
		}

		// Create a project
		UserInfo userInfo = this.userProvider.getTestUserInfo();
		this.project = new Project();
		this.project.setName("DynamoQueueWorkerIntegrationTest.Project");
		// This should trigger create message.
		String id = this.entityManager.createEntity(userInfo, project, null);
		this.project = this.entityManager.getEntity(userInfo, id, Project.class);
		Assert.assertNotNull(this.project);
	}

	@After
	public void after() throws Exception {
		// Try to empty the queue
		int count = this.dynamoQueueMessageRetriever.triggerFired();
		while (count > 0) {
			count = this.dynamoQueueMessageRetriever.triggerFired();
		}
		// Remove the project
		if (this.project != null) {
			this.entityManager.deleteEntity(
					this.userProvider.getTestAdminUserInfo(), this.project.getId());
		}
		// Clear Dynamo
		String root = this.nodeTreeDao.getRoot();
		if (root != null) {
			this.nodeTreeDao.delete(root, new Date());
		}
	}

	@Test
	public void testRoundTrip() throws Exception {
		for (int i = 0; i < 6; i++) {
			// Pause 1 second for eventual consistency
			// Wait at most 9 seconds
			Thread.sleep(1500);
			List<String> results = this.nodeTreeDao.getAncestors(
					KeyFactory.stringToKey(this.project.getId()).toString());
			Assert.assertNotNull(results);
			if (results.size() > 0) {
				// At least the root as the ancestor
				String root = this.nodeTreeDao.getRoot();
				Assert.assertNotNull(root);
				Assert.assertEquals(root, results.get(0));
				break;
			}
		}
	}
}
