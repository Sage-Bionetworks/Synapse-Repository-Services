package org.sagebionetworks.rds.workers;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.asynchronous.workers.sqs.MessageReceiver;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This test validates that entity messages pushed to the topic propagate to the rds queue,
 * then processed by the worker and pushed to the search index.
 * 
 * @author jmhill
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class RdsWorkerIntegrationTest {
	
	public static final long MAX_WAIT = 60*1000; // one minute
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private NodeQueryDao nodeQueryDao;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private MessageReceiver rdsQueueMessageReveiver;
	
	private final String key = "some_annotation_key";
	private String uniqueValue;
	private Project project;
	
	@Before
	public void before() throws Exception {
		// Before we start, make sure the queue is empty
		emptyQueue();
		
		// Create a project
		UserInfo userInfo = userManager.getUserInfo(AuthorizationConstants.TEST_USER_NAME);
		project = new Project();
		project.setName("RdsWorkerIntegrationTest.Project");
		// this should trigger create message.
		String id = entityManager.createEntity(userInfo, project, null);
		project = entityManager.getEntity(userInfo, id, Project.class);
		// Add an annotation to query for
		Annotations annos = entityManager.getAnnotations(userInfo, id);
		uniqueValue = UUID.randomUUID().toString();
		annos.addAnnotation(key, uniqueValue);
		entityManager.updateAnnotations(userInfo, id, annos);
	}

	/**
	 * Empty the queue by processing all messages on the queue.
	 * @throws InterruptedException
	 */
	public void emptyQueue() throws InterruptedException {
		long start = System.currentTimeMillis();
		int count = 0;
		do{
			count = rdsQueueMessageReveiver.triggerFired();
			System.out.println("Emptying the search message queue, there were at least: "+count+" messages on the queue");
			Thread.yield();
			long elapse = System.currentTimeMillis()-start;
			if(elapse > MAX_WAIT*2) throw new RuntimeException("Timedout waiting process all messages that were on the queue before the tests started.");
		}while(count > 0);
	}
	
	@After
	public void after() throws Exception {
		UserInfo adminUserInfo = userManager.getUserInfo(AuthorizationConstants.ADMIN_USER_NAME);
		entityManager.deleteEntity(adminUserInfo, project.getId());
	}
	
	
	@Test
	public void testRoundTrip() throws Exception {
		// First run query
		UserInfo userInfo = userManager.getUserInfo(AuthorizationConstants.TEST_USER_NAME);
		BasicQuery query = new BasicQuery();
		query.addExpression(new Expression(new CompoundId(null, key), Comparator.EQUALS, uniqueValue));
		long start = System.currentTimeMillis();
		while(nodeQueryDao.executeCountQuery(query, userInfo)< 1){
			System.out.println("Waiting for annotations index to be updated for entity: "+project.getId());
			Thread.sleep(1000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for annotaion index to be updated for entity: "+project.getId(),elapse < MAX_WAIT);
		}
		System.out.println("Annotations index was updated for entity "+project.getId());
	}
	
}
