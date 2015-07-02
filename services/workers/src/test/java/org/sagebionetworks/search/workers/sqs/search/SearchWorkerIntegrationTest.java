package org.sagebionetworks.search.workers.sqs.search;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dao.WikiPageKeyHelper;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.search.SearchDao;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;
import com.google.common.base.Predicate;

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
	private SearchDao searchDao;
	
	@Autowired
	private V2WikiPageDao wikiPageDao;
	
	@Autowired
	private FileHandleDao fileMetadataDao;
	
	@Autowired
	private FileHandleManager fileHandleManager;
	
	@Autowired
	private SemaphoreManager semphoreManager;
	
	@Autowired
	private AmazonS3Client s3Client;
	
	private UserInfo adminUserInfo;
	private Project project;
	private V2WikiPage rootPage;
	private WikiPageKey rootKey;
	
	private S3FileHandle markdownOne;
	String uuid;
	
	@Before
	public void before() throws Exception {
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
		// Only run this test if search is enabled
		Assume.assumeTrue(searchDao.isSearchEnabled());
		
		assertTrue(TimeUtils.waitFor(20000, 500, null, new Predicate<Void>() {
			@Override
			public boolean apply(Void input) {
				try {
					return searchDao.postInitialize();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}));

		// Now delete all documents in the search index.
		// wait for the searchindex to become available (we assume the queue is already there and only needs to be
		// checked once). It should go through within .5 seconds, but if not (aws no ready), it can take 30 seconds for
		// a retry
		assertTrue(TimeUtils.waitFor(65000, 100, null, new Predicate<Void>() {
			@Override
			public boolean apply(Void input) {
				try {
					searchDao.deleteAllDocuments();
					return true;
				} catch (ServiceUnavailableException e) {
					return false;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}));
		
		// Create a project
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		project = new Project();
		project.setName("SearchIntegrationTest.Project");
		// this should trigger create messaage.
		String id = entityManager.createEntity(adminUserInfo, project, null);
		project = entityManager.getEntity(adminUserInfo, id, Project.class);

		uuid = UUID.randomUUID().toString();
		
		// Zip up the markdown into a file with UUID
		String markdown = "markdown contents " + uuid;
		markdownOne = fileHandleManager.createCompressedFileFromString(""+adminUserInfo.getId(), new Date(), markdown);
	}

	private V2WikiPage createWikiPage(UserInfo info){
		// Create a wiki page that points to the markdown file
		V2WikiPage page = new  V2WikiPage();
		page.setTitle("rootTile");
		page.setMarkdownFileHandleId(markdownOne.getId());
		page.setCreatedBy(info.getId().toString());
		page.setCreatedOn(new Date());
		page.setModifiedBy(page.getCreatedBy());
		page.setModifiedOn(page.getCreatedOn());
		page.setEtag("Etag");
		return page;
	}
	
	@After
	public void after() throws DatastoreException, UnauthorizedException, NotFoundException{
		if(markdownOne != null) {
			// Clean up S3 File and S3FileHandle
			String markdownHandleId = markdownOne.getId();
			S3FileHandle markdownHandle = (S3FileHandle) fileMetadataDao.get(markdownHandleId);
			s3Client.deleteObject(markdownHandle.getBucketName(), markdownHandle.getKey());
			fileMetadataDao.delete(markdownHandleId);
		}
		
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
		rootPage = createWikiPage(adminUserInfo);
		rootPage = wikiPageDao.create(rootPage, new HashMap<String, FileHandle>(), project.getId(), ObjectType.ENTITY, new ArrayList<String>());
		rootKey = WikiPageKeyHelper.createWikiPageKey(project.getId(), ObjectType.ENTITY, rootPage.getId());
		// The only way to know for sure that the wikipage data is included in the project's description is to query for it.
		Thread.sleep(1000);
		waitForQuery("q="+uuid);
	}

	public void waitForPojectToAppearInSearch() throws Exception {
		long start = System.currentTimeMillis();
		while(!searchDao.doesDocumentExist(project.getId(), project.getEtag())){
			System.out.println("Waiting for entity "+project.getId()+" to appear in the search index...");
			Thread.sleep(5000);		
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Failed to a new Entity in the search index within the timeout period.",elapse < MAX_WAIT);
		}
	}
	
	public void waitForQuery(String query) throws Exception {
		long start = System.currentTimeMillis();
		while (searchDao.executeSearch(query).getHits().size() < 1) {
			System.out.println("Waiting for search query: "+query);
			Thread.sleep(5000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue(	"Failed to a new Entity in the search index within the timeout period.",elapse < MAX_WAIT);
		}
	}
}
