package org.sagebionetworks.repo.manager.search;

import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.search.SearchDao;
import org.sagebionetworks.search.SearchUtil;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.search.SearchConstants.FIELD_ID;
import static org.sagebionetworks.search.SearchConstants.FIELD_PATH;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SearchManagerImplTest {

	@Mock
	private SearchDao mockSearchDao;
	@Mock
	private SearchDocumentDriver mockSearchDocumentDriver;

	private UserInfo nonAdminUserInfo;
	private String searchQuery;
	private SearchRequest searchRequest;

	private SearchManagerImpl searchManager;

	@Before
	public void before(){
		searchManager = new SearchManagerImpl();
		ReflectionTestUtils.setField(searchManager, "searchDao", mockSearchDao);
		ReflectionTestUtils.setField(searchManager, "searchDocumentDriver", mockSearchDocumentDriver);



		nonAdminUserInfo = new UserInfo(false, 990L);
		searchQuery = "q.parser=structured&q=(and 'RIP' 'Harambe')&return=id,freeze,mage,fun&interactive=deck";
		Set<Long> userGroups = new HashSet<Long>();
		userGroups.add(123L);
		userGroups.add(8008135L);
		nonAdminUserInfo.setGroups(userGroups);
		searchRequest = new SearchRequest();
	}

	@Test
	public void testProxySearchPath() throws Exception {
		// Prepare mock results
		SearchResults sample = new SearchResults();
		sample.setHits(new LinkedList<Hit>());
		Hit hit = new Hit();
		hit.setId("syn123");
		sample.getHits().add(hit);
		when(mockSearchDao.executeSearch(any(SearchRequest.class))).thenReturn(sample);
		// make sure the path is returned from the document driver
		when(mockSearchDocumentDriver.getEntityPath("syn123")).thenReturn(new EntityPath());

		SearchQuery query = new SearchQuery();
		query.setBooleanQuery(new LinkedList<KeyValue>());
		KeyValue kv = new KeyValue();
		kv.setKey(FIELD_ID);
		kv.setValue("syn123");
		query.getBooleanQuery().add(kv);

		// Path should not get passed along to the search index as it is not there anymore.
		query.setReturnFields(new LinkedList<String>());
		query.getReturnFields().add(FIELD_PATH);

		// Make the call
		SearchResults results = searchManager.proxySearch(nonAdminUserInfo, query);
		assertNotNull(results);
		assertNotNull(results.getHits());
		assertEquals(1, results.getHits().size());
		Hit returnedHit = results.getHits().get(0);
		// Path should get filled in since we asked for it.
		assertNotNull(returnedHit.getPath());
		// Validate that path was not passed along to the search index as it is not there.
		verify(mockSearchDao, times(1)).executeSearch(any(SearchRequest.class));
	}

	@Test
	public void testProxySearchNoPath() throws Exception {
		// Prepare mock results
		SearchResults sample = new SearchResults();
		sample.setHits(new LinkedList<Hit>());
		Hit hit = new Hit();
		hit.setId("syn123");
		sample.getHits().add(hit);
		when(mockSearchDao.executeSearch(any(SearchRequest.class))).thenReturn(sample);

		SearchQuery query = new SearchQuery();
		query.setBooleanQuery(new LinkedList<KeyValue>());
		KeyValue kv = new KeyValue();
		kv.setKey(FIELD_ID);
		kv.setValue("syn123");
		query.getBooleanQuery().add(kv);

		// Make the call
		SearchResults results = searchManager.proxySearch(nonAdminUserInfo, query);
		assertNotNull(results);
		assertNotNull(results.getHits());
		assertEquals(1, results.getHits().size());
		Hit returnedHit = results.getHits().get(0);
		// The path should not be returned unless requested.
		assertNull(returnedHit.getPath());
		verify(mockSearchDao, times(1)).executeSearch(any(SearchRequest.class));
	}

	@Test
	public void testFilterSeachForAuthorizationUserIsAdmin(){
		UserInfo adminUser = new UserInfo(true, 420L);
		SearchManagerImpl.filterSearchForAuthorization(adminUser, searchRequest);
		assertEquals(null, searchRequest.getFilterQuery());
	}

	@Test
	public void testFilterSearchForAuthorizationUserIsNotAdmin(){
		SearchManagerImpl.filterSearchForAuthorization(nonAdminUserInfo, searchRequest);
		assertEquals(SearchUtil.formulateAuthorizationFilter(nonAdminUserInfo), searchRequest.getFilterQuery());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFilterSearchForAuthorizationUserIsNotAdminFilterQueryAlreadyExists(){
		searchRequest.setFilterQuery("(or memes:'dank')");
		SearchManagerImpl.filterSearchForAuthorization(nonAdminUserInfo, searchRequest);
	}
}