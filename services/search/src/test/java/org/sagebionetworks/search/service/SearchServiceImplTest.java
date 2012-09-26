package org.sagebionetworks.search.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.search.SearchConstants.FIELD_ID;
import static org.sagebionetworks.search.SearchConstants.FIELD_PATH;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;

import org.apache.http.client.ClientProtocolException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.search.SearchDao;
import org.sagebionetworks.utils.HttpClientHelperException;

/**
 * Test for SearchServiceImpl
 * @author John
 *
 */
public class SearchServiceImplTest {
	
	private SearchDao mockSearchDao;
	private UserManager mockUserManager;
	private SearchDocumentDriver mockSearchDocumentDriver;
	private SearchServiceImpl service;
	private UserInfo userInfo;
	
	
	@Before
	public void before(){
		mockSearchDao = Mockito.mock(SearchDao.class);
		mockUserManager = Mockito.mock(UserManager.class);
		mockSearchDocumentDriver = Mockito.mock(SearchDocumentDriver.class);
		service= new SearchServiceImpl(mockSearchDao, mockUserManager, mockSearchDocumentDriver);
		userInfo = new UserInfo(false);
		userInfo.setUser(new User());
		userInfo.setGroups(new LinkedList<UserGroup>());
		UserGroup ug = new UserGroup();
		ug.setId("999");
		userInfo.getGroups().add(ug);
	}
	
	@Test
	public void testProxySearchPath() throws UnsupportedEncodingException, ClientProtocolException, IOException, HttpClientHelperException, NotFoundException {
		// Prepare mock results
		SearchResults sample = new SearchResults();
		sample.setHits(new LinkedList<Hit>());
		Hit hit = new Hit();
		hit.setId("syn123");
		sample.getHits().add(hit);
		when(mockSearchDao.executeSearch(any(String.class))).thenReturn(sample);
		// make sure the path is returned from the document driver
		when(mockSearchDocumentDriver.getEntityPath("syn123")).thenReturn(new EntityPath());
		
		SearchQuery query = new SearchQuery();
		query.setBooleanQuery(new LinkedList<KeyValue>());
		KeyValue kv = new KeyValue();
		kv.setKey(FIELD_ID);
		kv.setValue("syn123");
		query.getBooleanQuery().add(kv);
		
		// Setup the expected query
		String serchQueryString = service.createQueryString(userInfo, query);
		// Path should not get passed along to the search index as it is not there anymore.
		query.setReturnFields(new LinkedList<String>());
		query.getReturnFields().add(FIELD_PATH);
		
		// Make the call
		SearchResults results = service.proxySearch(userInfo, query);
		assertNotNull(results);
		assertNotNull(results.getHits());
		assertNotNull(results.getHits().size() == 1);
		Hit returnedHit = results.getHits().get(0);
		// Path should get filled in since we asked for it.
		assertNotNull(returnedHit.getPath());
		// Validate that path was not passed along to the search index as it is not there.
		verify(mockSearchDao, times(1)).executeSearch(serchQueryString);
	}
	
	@Test
	public void testProxySearchNoPath() throws UnsupportedEncodingException, ClientProtocolException, IOException, HttpClientHelperException, NotFoundException {
		// Prepare mock results
		SearchResults sample = new SearchResults();
		sample.setHits(new LinkedList<Hit>());
		Hit hit = new Hit();
		hit.setId("syn123");
		sample.getHits().add(hit);
		when(mockSearchDao.executeSearch(any(String.class))).thenReturn(sample);
		
		SearchQuery query = new SearchQuery();
		query.setBooleanQuery(new LinkedList<KeyValue>());
		KeyValue kv = new KeyValue();
		kv.setKey(FIELD_ID);
		kv.setValue("syn123");
		query.getBooleanQuery().add(kv);
		
		// Setup the expected query
		String serchQueryString = service.createQueryString(userInfo, query);

		// Make the call
		SearchResults results = service.proxySearch(userInfo, query);
		assertNotNull(results);
		assertNotNull(results.getHits());
		assertNotNull(results.getHits().size() == 1);
		Hit returnedHit = results.getHits().get(0);
		// The path should not be returned unless requested.
		assertNull(returnedHit.getPath());
		verify(mockSearchDao, times(1)).executeSearch(serchQueryString);
	}

}
