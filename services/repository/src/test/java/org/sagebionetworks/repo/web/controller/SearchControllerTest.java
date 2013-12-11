package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.search.SearchDocumentDriver;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.search.SearchConstants;
import org.sagebionetworks.search.SearchDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test for the search controller
 * @author John
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SearchControllerTest {
	
	private static long MAX_WAIT = 1000*15;
	
	@Autowired
	private UserManager userManager;
	
	private String adminUsername;
	
	private ServiceProvider provider;
	private SearchDao searchDao;
	private SearchDocumentDriver documentProvider;
	private Project project;
	
	@Before
	public void before() throws Exception {
		adminUsername = userManager.getGroupName(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString());
		
		StackConfiguration config = new StackConfiguration();
		// Only run this test if search is enabled.
		Assume.assumeTrue(config.getSearchEnabled());
		
		provider = DispatchServletSingleton.getInstance().getWebApplicationContext().getBean(ServiceProvider.class);
		assertNotNull(provider);
		searchDao =  DispatchServletSingleton.getInstance().getWebApplicationContext().getBean(SearchDao.class);
		assertNotNull(searchDao);
		documentProvider = DispatchServletSingleton.getInstance().getWebApplicationContext().getBean(SearchDocumentDriver.class);
		assertNotNull(documentProvider);
		// Create an project
		project = new Project();
		project.setName("SearchControllerTest");
		project = provider.getEntityService().createEntity(adminUsername, project, null, new MockHttpServletRequest());
		// Push this to the serach index
		Document doc = documentProvider.formulateSearchDocument(project.getId());
		searchDao.createOrUpdateSearchDocument(doc);
		// Wait for it to show up
		long start = System.currentTimeMillis();
		while(!searchDao.doesDocumentExist(project.getId(), project.getEtag())){
			System.out.println("Waiting for entity to appearh in seach index: "+project.getId()+"...");
			Thread.sleep(1000);
			long elapse = System.currentTimeMillis()-start;
			assertTrue("Timed out waiting for a document to appear in the search index",elapse < MAX_WAIT);
		}
	}
	
	@After
	public void after()  throws Exception{
		if(provider != null && project != null){
			provider.getEntityService().deleteEntity(adminUsername, project.getId());
			searchDao.deleteAllDocuments();
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
		// the mock request
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.addHeader("Content-Type", "application/json");
		request.setRequestURI("/search");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, adminUsername);
		request.setContent(EntityFactory.createJSONStringForEntity(query)
				.getBytes("UTF-8"));
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.CREATED.value()) {
			throw new RuntimeException(response.getContentAsString());
		}
		SearchResults results = EntityFactory.createEntityFromJSONString(response.getContentAsString(), SearchResults.class);
		assertNotNull(results);
		assertNotNull(results.getHits());
		assertEquals(1l, results.getHits().size());
		Hit hit = results.getHits().get(0);
		assertNotNull(hit);
		assertEquals(project.getId(), hit.getId());
	}

}
