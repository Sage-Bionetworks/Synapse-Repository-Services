package org.sagebionetworks.search.workers.sqs.search;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.asynchronous.workers.sqs.MessageReceiver;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.sagebionetworks.search.SearchDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This test validates that entity messages pushed to the topic propagate to the search queue,
 * then processed by the worker and pushed to the search index.
 * 
 * @author jmhill
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SearchWorkerIntegrationTest {
	
	public static final long MAX_WAIT = 60*1000; // one minute
	
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private UserProvider userProvider;
	@Autowired
	private MessageReceiver messageReceiver;
	@Autowired
	private SearchDao searchDao;
	private Project project;
	
	@Before
	public void before() throws Exception {
		// Before we start, make sure the search queue is empty
		emptySearchQueue();
		// Now delete all documents in the search index.
		searchDao.deleteAllDocuments();
		
		// Create a project
		UserInfo info = userProvider.getTestUserInfo();
		project = new Project();
		project.setName("SearchIntegrationTest.Project");
		// this should trigger create messaage.
		String id = entityManager.createEntity(info, project);
		project = entityManager.getEntity(info, id, Project.class);
	}

	/**
	 * Empty the search queue by processing all messages on the queue.
	 * @throws InterruptedException
	 */
	public void emptySearchQueue() throws InterruptedException {
		long start = System.currentTimeMillis();
		int count = 0;
		do{
			count = messageReceiver.triggerFired();
			System.out.println("Emptying the search message queue, there were at least: "+count+" messages on the queue");
			Thread.yield();
			long elapse = System.currentTimeMillis()-start;
			if(elapse > MAX_WAIT*2) throw new RuntimeException("Timedout waiting process all messages that were on the queue before the tests started.");
		}while(count > 0);
	}
	
	@After
	public void after() throws DatastoreException, UnauthorizedException, NotFoundException{
		if(project != null && entityManager != null && userProvider != null){
			entityManager.deleteEntity(userProvider.getTestAdminUserInfo(), project.getId());
		}
	}
	
	
	@Test
	public void testRoundTrip() throws Exception {
		// First run query
		// Execute the query
		long start = System.currentTimeMillis();
		while(!searchDao.doesDocumentExist(project.getId(), project.getEtag())){
			System.out.println("Waiting for entity "+project.getId()+" to appear in the search index...");
			Thread.sleep(5000);		
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Failed to a new Entity in the search index within the timeout period.",elapse < MAX_WAIT);
		}

	}
	
}
