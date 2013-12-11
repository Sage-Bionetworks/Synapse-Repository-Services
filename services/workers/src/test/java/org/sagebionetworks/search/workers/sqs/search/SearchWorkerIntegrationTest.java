package org.sagebionetworks.search.workers.sqs.search;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.apache.http.client.ClientProtocolException;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.asynchronous.workers.sqs.MessageReceiver;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.search.SearchDao;
import org.sagebionetworks.utils.HttpClientHelperException;
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
	private UserManager userManager;
	
	@Autowired
	private MessageReceiver searchQueueMessageReveiver;
	
	@Autowired
	private SearchDao searchDao;
	
	@Autowired
	private WikiPageDao wikiPageDao;
	
	private UserInfo adminUserInfo;
	private Project project;
	private WikiPage rootPage;
	private WikiPageKey rootKey;
	
	@Before
	public void before() throws Exception {
		// Only run this test if search is enabled
		Assume.assumeTrue(searchDao.isSearchEnabled());
		// Before we start, make sure the search queue is empty
		emptySearchQueue();
		// Now delete all documents in the search index.
		searchDao.deleteAllDocuments();
		
		// Create a project
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		project = new Project();
		project.setName("SearchIntegrationTest.Project");
		// this should trigger create messaage.
		String id = entityManager.createEntity(adminUserInfo, project, null);
		project = entityManager.getEntity(adminUserInfo, id, Project.class);

	}

	private WikiPage createWikiPage(UserInfo info){
		WikiPage page = new  WikiPage();
		page.setTitle("rootTile");
		page.setMarkdown("rootMarkdown");
		page.setCreatedBy(info.getIndividualGroup().getId());
		page.setCreatedOn(new Date());
		page.setModifiedBy(page.getCreatedBy());
		page.setModifiedOn(page.getCreatedOn());
		page.setEtag("Etag");
		return page;
	}
	/**
	 * Empty the search queue by processing all messages on the queue.
	 * @throws InterruptedException
	 */
	public void emptySearchQueue() throws InterruptedException {
		long start = System.currentTimeMillis();
		int count = 0;
		do{
			count = searchQueueMessageReveiver.triggerFired();
			System.out.println("Emptying the search message queue, there were at least: "+count+" messages on the queue");
			Thread.yield();
			long elapse = System.currentTimeMillis()-start;
			if(elapse > MAX_WAIT*2) throw new RuntimeException("Timedout waiting process all messages that were on the queue before the tests started.");
		}while(count > 0);
	}
	
	@After
	public void after() throws DatastoreException, UnauthorizedException, NotFoundException{
		if (project != null){
			entityManager.deleteEntity(adminUserInfo, project.getId());
		}
		if(rootKey != null){
			wikiPageDao.delete(rootKey);
		}
	}
	
	
	@Test
	public void testRoundTrip() throws Exception {
		// Wait for the project to appear.
		waitForPojectToAppearInSearch();
				
		// Now add a wikpage
		// Create a wiki page
		rootPage = createWikiPage(adminUserInfo);
		rootPage.setTitle("rootTile");
		String uuid = UUID.randomUUID().toString();
		rootPage.setMarkdown(" "+uuid);
		rootPage = wikiPageDao.create(rootPage, new HashMap<String, FileHandle>(), project.getId(), ObjectType.ENTITY);
		rootKey = new WikiPageKey(project.getId(), ObjectType.ENTITY, rootPage.getId());
		// The only way to know for sure that the wikipage data is included in the project's description is to query for it.
		Thread.sleep(1000);
		waitForQuery("q="+uuid);
	}

	public void waitForPojectToAppearInSearch() throws ClientProtocolException,
			IOException, HttpClientHelperException, InterruptedException {
		long start = System.currentTimeMillis();
		while(!searchDao.doesDocumentExist(project.getId(), project.getEtag())){
			System.out.println("Waiting for entity "+project.getId()+" to appear in the search index...");
			Thread.sleep(5000);		
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Failed to a new Entity in the search index within the timeout period.",elapse < MAX_WAIT);
		}
	}
	
	public void waitForQuery(String query) throws ClientProtocolException,
			IOException, HttpClientHelperException, InterruptedException {
		long start = System.currentTimeMillis();
		while (searchDao.executeSearch(query).getHits().size() < 1) {
			System.out.println("Waiting for search query: "+query);
			Thread.sleep(5000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue(	"Failed to a new Entity in the search index within the timeout period.",elapse < MAX_WAIT);
		}
	}
}
