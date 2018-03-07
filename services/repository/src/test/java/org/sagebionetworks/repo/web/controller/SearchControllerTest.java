package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.UUID;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.search.SearchDocumentDriver;
import org.sagebionetworks.repo.manager.search.SearchManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.repo.web.service.EntityService;
import org.sagebionetworks.search.CloudSearchClientProvider;
import org.sagebionetworks.search.SearchConstants;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

import com.google.common.base.Predicate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test for the search controller
 * @author John
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class SearchControllerTest extends AbstractAutowiredControllerTestBase {	
	private Long adminUserId;

	@Autowired
	private EntityService entityService;

	@Autowired
	private SearchDocumentDriver documentProvider;

	@Autowired
	private CloudSearchClientProvider cloudSearchClientProvider;

	@Autowired
	private SearchManager searchManager;

	private Project project;


	
	@Before
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		// Only run this test if search is enabled.
		Assume.assumeTrue(cloudSearchClientProvider.isSearchEnabled());

		// wait for search initialization
		assertTrue(TimeUtils.waitFor(600000, 1000, null, new Predicate<Void>() {
			@Override
			public boolean apply(Void input) {
				try {
					return cloudSearchClientProvider.getCloudSearchClient() != null;
				} catch (TemporarilyUnavailableException e) {
					//not ready yet so ignore...
					return false;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}));

		// Create an project
		project = new Project();
		project.setName("SearchControllerTest" + UUID.randomUUID());
		project = entityService.createEntity(adminUserId, project, null, new MockHttpServletRequest());
		// Push this to the serach index
		Document doc = documentProvider.formulateSearchDocument(project.getId());
		searchManager.createOrUpdateSearchDocument(doc);
		// Wait for it to show up
		assertTrue(TimeUtils.waitFor(60000, 1000, null, new Predicate<Void>() {
			@Override
			public boolean apply(Void input) {
				try {
					System.out.println("Waiting for entity to appear in seach index: " + project.getId() + "...");
					return searchManager.doesDocumentExist(project.getId(), project.getEtag());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}));
	}
	
	@After
	public void after()  throws Exception{
		if(project != null){
			entityService.deleteEntity(adminUserId, project.getId());
			searchManager.deleteAllDocuments();
		}
	}
	
	@Test
	public void testSearch() throws Exception {
		// Build a simple query.
		SearchQuery query = new SearchQuery();
		query.setBooleanQuery(new LinkedList<KeyValue>());
		KeyValue kv = new KeyValue();
		kv.setKey(SearchConstants.FIELD_ID);
		kv.setValue(project.getId());
		query.getBooleanQuery().add(kv);
		
		SearchResults results = servletTestHelper.getSearchResults(adminUserId, query);
		assertNotNull(results);
		assertNotNull(results.getHits());
		assertEquals(1l, results.getHits().size());
		Hit hit = results.getHits().get(0);
		assertNotNull(hit);
		assertEquals(project.getId(), hit.getId());
	}
}
