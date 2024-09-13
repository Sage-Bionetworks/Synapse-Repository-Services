package org.sagebionetworks.search.workers.sqs.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.EntityAclManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.search.SearchManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.agent.AgentAccessLevel;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dao.WikiPageKeyHelper;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.service.AgentService;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.search.CloudSearchClientProvider;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.base.Predicate;

/**
 * This test validates that entity messages pushed to the topic propagate to the search queue,
 * then processed by the worker and pushed to the search index.
 * 
 *
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SearchWorkerIntegrationTest {
	
	private static final long MAX_WAIT = 5 * 60*1000; // 5 minutes
	private static final long CHECK_TIME = 2000;
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private EntityAclManager entityAclManager;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private CloudSearchClientProvider searchProvider;
	
	@Autowired
	private V2WikiPageDao wikiPageDao;
	
	@Autowired
	private FileHandleDao fileMetadataDao;
	
	@Autowired
	private FileHandleManager fileHandleManager;
	
	@Autowired
	private SemaphoreManager semphoreManager;
	
	@Autowired
	private SynapseS3Client s3Client;

	@Autowired
	private SearchManager searchManager;
	
	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;
	
	@Autowired
	private AgentService agentService;
	
	private UserInfo adminUserInfo;
	private UserInfo anotherUser;
	private Project project;
	private WikiPageKey rootKey;
	
	private S3FileHandle markdownOne;
	private String uuid;
	
	@BeforeEach
	public void before() throws Exception {
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
		// Only run this test if search is enabled
		Assumptions.assumeTrue(searchProvider.isSearchEnabled());
		
		assertTrue(TimeUtils.waitFor(20000, 500, null, new Predicate<Void>() {
			@Override
			public boolean apply(Void input) {
				try {
					return searchProvider.getCloudSearchClient() != null;
				} catch (TemporarilyUnavailableException e) {
					//not ready yet so ignore...
					return false;
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
					searchManager.deleteAllDocuments();
					return true;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}));
		
		// Create a project
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		project = new Project();
		project.setName("SearchIntegrationTest.Project" + UUID.randomUUID());
		// this should trigger create messaage.
		String id = entityManager.createEntity(adminUserInfo, project, null);
		project = entityManager.getEntity(adminUserInfo, id, Project.class);

		uuid = UUID.randomUUID().toString();
		
		// Zip up the markdown into a file with UUID
		String markdown = "markdown contents \u000c\f\u0019with control characters " + uuid;
		markdownOne = fileHandleManager.createCompressedFileFromString(""+adminUserInfo.getId(), new Date(), markdown);
		
		String userName = UUID.randomUUID().toString();
		
		anotherUser = userManager.createOrGetTestUser(adminUserInfo, new NewUser().setUserName(userName).setEmail(userName + "@foo.org"), true);
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
	
	@AfterEach
	public void after() throws DatastoreException, UnauthorizedException, NotFoundException{
		if(rootKey != null){
			wikiPageDao.delete(rootKey);
		}

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
		
		if (anotherUser != null) {
			userManager.deletePrincipal(adminUserInfo, anotherUser.getId());
		}
	}
	
	@Test
	public void testBuildSearchWithReadOnlyMode() throws Exception {
		asyncHelper.runInReadOnlyMode(() -> {
			// Wait for the project to appear.
			waitForPojectToAppearInSearch();
			return 0;
		});
	}
	
	@Test
	public void testRoundTrip() throws Exception {
		// Wait for the project to appear.
		waitForPojectToAppearInSearch();
				
		// Now add a wikpage
		V2WikiPage rootPage = createWikiPage(adminUserInfo);
		rootPage = wikiPageDao.create(rootPage, new HashMap<String, FileHandle>(), project.getId(), ObjectType.ENTITY, new ArrayList<String>());
		rootKey = WikiPageKeyHelper.createWikiPageKey(project.getId(), ObjectType.ENTITY, rootPage.getId());
		// The only way to know for sure that the wikipage data is included in the project's description is to query for it.
		Thread.sleep(1000);
		
		waitForQuery(adminUserInfo, uuid);
		
		// can the agent find this document?
		AgentSession session = agentService.createSession(adminUserInfo.getId(),
				new CreateAgentSessionRequest().setAgentAccessLevel(AgentAccessLevel.READ_YOUR_PRIVATE_DATA));

		assertNotNull(session);
		// an empty request will return an empty response.
		String chatRequest = String.format("I would like to search for the term: '%s' in Synapse.", uuid);

		asyncHelper.assertJobResponse(adminUserInfo,
				new AgentChatRequest().setSessionId(session.getSessionId()).setChatText(chatRequest),
				(AgentChatResponse response) -> {
					assertNotNull(response);
					assertEquals(session.getSessionId(), response.getSessionId());
					System.out.println(response.getResponseText());
					// if the agent was able to find this project, its id should be in the response.
					assertTrue(response.getResponseText().contains(project.getId()),
							"Failed to find the project ID in the agent's response");
				}, MAX_WAIT).getResponse();
		
	}
	
	// Test for PLFM-6868
	@Test
	public void testProjectAclUpdate() throws Exception {
		// Wait for the project to appear.
		waitForPojectToAppearInSearch();
		
		// We search by project name
		String searchTerm = project.getName();
		
		// The admin should find the project
		waitForQuery(adminUserInfo, searchTerm);
		
		// No results for the user since the project is not shared
		waitForQuery(anotherUser, searchTerm, 0L);
		
		// Now share the project with the user
		AccessControlList acl = entityAclManager.getACL(project.getId(), adminUserInfo);
		
		acl.getResourceAccess().add(new ResourceAccess().setPrincipalId(anotherUser.getId()).setAccessType(Collections.singleton(ACCESS_TYPE.READ)));
		
		// Update the ACL, this should propagate the change so that the document is visible to the user
		entityAclManager.updateACL(acl, adminUserInfo);
		
		// The user should eventually find the project
		waitForQuery(anotherUser, searchTerm);
		
		
	}

	public void waitForPojectToAppearInSearch() throws Exception {
		TimeUtils.waitFor(MAX_WAIT, CHECK_TIME, () -> {
			System.out.println("Waiting for entity "+project.getId()+" to appear in the search index...");
			return Pair.create(searchManager.doesDocumentExist(project.getId(), project.getEtag()), null);
		});
	}
	
	public void waitForQuery(UserInfo user, String term) throws Exception {
		waitForQuery(user, term, 1L);
	}
	
	public void waitForQuery(UserInfo user, String term, Long expectedResultCount) throws Exception {
		SearchQuery searchQuery = searchQuerybyTerm(term);
		TimeUtils.waitFor(MAX_WAIT, CHECK_TIME, () -> {
			System.out.println("Waiting for search query: "+searchQuery);
			return Pair.create(searchManager.proxySearch(user, searchQuery).getFound() == expectedResultCount, null);
		});
	}
	
	private static SearchQuery searchQuerybyTerm(String term) {
		return new SearchQuery().setQueryTerm(Arrays.asList(term));
	}
}
