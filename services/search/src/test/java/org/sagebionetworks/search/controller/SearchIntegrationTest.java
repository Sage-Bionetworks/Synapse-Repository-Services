package org.sagebionetworks.search.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.LinkedList;

import javax.servlet.ServletException;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.quartz.SchedulerException;
import org.sagebionetworks.asynchronous.workers.sqs.MessageReceiver;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.search.SearchConstants;
import org.sagebionetworks.search.SearchDao;
import org.springframework.beans.BeansException;

/**
 * End to end test for the search services.
 * @author jmhill
 *
 */
@Ignore // See: PLFM-1522
public class SearchIntegrationTest {
	
	public static final long MAX_WAIT = 60*1000; // one minute
	
	private EntityManager entityManager;
	private UserProvider userProvider;
	private MessageReceiver messageReceiver;
	private SearchDao searchDao;
	private Project project;
	
	@Before
	public void before() throws BeansException, ServletException, SchedulerException, DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, InterruptedException{
		entityManager = (EntityManager) DispatchServletSingleton.getInstance().getWebApplicationContext().getBean(EntityManager.class);
		assertNotNull(entityManager);
		userProvider = DispatchServletSingleton.getInstance().getWebApplicationContext().getBean(UserProvider.class);
		assertNotNull(userProvider);
		messageReceiver = DispatchServletSingleton.getInstance().getWebApplicationContext().getBean(MessageReceiver.class);
		searchDao = DispatchServletSingleton.getInstance().getWebApplicationContext().getBean(SearchDao.class);
		// Before we start, make sure the search queue is empty
		emptySearchQueue();
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
	public void testBadSearch() throws ServletException, IOException, JSONException, JSONObjectAdapterException, InterruptedException {
		// First run query
		SearchQuery query = new SearchQuery();
		query.setBooleanQuery(new LinkedList<KeyValue>());
		KeyValue kv = new KeyValue();
		kv.setKey("ugh");
		kv.setValue(project.getId());
		query.getBooleanQuery().add(kv);
		// this should throw an error
		try{
			ServletTestHelper.getSearchResults(TestUserDAO.TEST_USER_NAME, query);
			fail("This was a bad query");
		}catch(RuntimeException e){
			// did we get the expected message.
			assertTrue(e.getMessage().indexOf("'ugh' is not defined in the metadata for this collection") > 0);
			assertFalse("The error message contains the URL of the search index", e.getMessage().indexOf("http://search") > 0);
		}
	}
	
	
	@Test
	public void testRoundTrip() throws ServletException, IOException, JSONException, JSONObjectAdapterException, InterruptedException {
		// First run query
		SearchQuery query = new SearchQuery();
		query.setBooleanQuery(new LinkedList<KeyValue>());
		KeyValue kv = new KeyValue();
		kv.setKey(SearchConstants.FIELD_ID);
		kv.setValue(project.getId());
		query.getBooleanQuery().add(kv);
		// Execute the query
		SearchResults results = null;
		long start = System.currentTimeMillis();
		do{
			results = ServletTestHelper.getSearchResults(TestUserDAO.TEST_USER_NAME, query);
			assertNotNull(results);
			if(results.getHits().size() < 1){
				System.out.println("Waiting for search index to update...");
				Thread.sleep(5000);				
			}
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Failed to a new Entity in the search index within the timeout period.",elapse < MAX_WAIT);
		}while(results.getHits().size() < 1);
		// Validate the results are what we expect
		assertEquals(1, results.getHits().size());
		Hit hit = results.getHits().get(0);
		System.out.println(results);
		assertEquals(project.getId(), hit.getId());
	}
	
}
