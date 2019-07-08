package org.sagebionetworks.repo.manager.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.search.SearchConstants.FIELD_ID;
import static org.sagebionetworks.search.SearchConstants.FIELD_PATH;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.search.CloudSearchLogger;
import org.sagebionetworks.search.SearchDao;
import org.sagebionetworks.search.SearchUtil;
import org.springframework.test.context.ContextConfiguration;

import com.amazonaws.services.cloudsearchdomain.model.Hit;
import com.amazonaws.services.cloudsearchdomain.model.Hits;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SearchManagerImplTest {

	@Mock
	private SearchDao mockSearchDao;

	@Mock
	ChangeMessageToSearchDocumentTranslator mockTranslator;

	@Mock
	SearchDocumentDriver mockSearchDocumentDriver;
	
	@Mock
	CloudSearchLogger mockRecordLogger;

	@Captor
	ArgumentCaptor<Iterator<Document>> iteratorArgumentCaptor;

	private UserInfo nonAdminUserInfo;
	private SearchRequest searchRequest;

	@InjectMocks
	private SearchManagerImpl searchManager;

	Document doc1;

	@Before
	public void before(){
		nonAdminUserInfo = new UserInfo(false, 990L);
		Set<Long> userGroups = new HashSet<>();
		userGroups.add(123L);
		userGroups.add(8008135L);
		nonAdminUserInfo.setGroups(userGroups);
		searchRequest = new SearchRequest();

		doc1 = new Document();
		doc1.setId("syn1");
	}

	@Test
	public void testProxySearchPath() throws Exception {
		// Prepare mock results
		SearchResult sample = new SearchResult().withHits(new Hits());
		Hit hit = new Hit().withId("syn123");
		sample.getHits().withHit(hit);
		when(mockSearchDao.executeSearch(any(SearchRequest.class))).thenReturn(sample);
		// make sure the path is returned from the document driver
		when(mockSearchDocumentDriver.getEntityPath("syn123")).thenReturn(new EntityPath());

		SearchQuery query = new SearchQuery();
		query.setBooleanQuery(new LinkedList<>());
		KeyValue kv = new KeyValue();
		kv.setKey(FIELD_ID);
		kv.setValue("syn123");
		query.getBooleanQuery().add(kv);

		// Path should not get passed along to the search index as it is not there anymore.
		query.setReturnFields(Lists.newArrayList(FIELD_PATH));

		// Make the call
		SearchResults results = searchManager.proxySearch(nonAdminUserInfo, query);
		assertNotNull(results);
		assertNotNull(results.getHits());
		assertEquals(1, results.getHits().size());
		// Path should get filled in since we asked for it.
		assertNotNull(results.getHits().get(0).getPath());
		// Validate that path was not passed along to the search index as it is not there.
		verify(mockSearchDao, times(1)).executeSearch(any(SearchRequest.class));
		verify(mockSearchDocumentDriver,times(1)).getEntityPath("syn123");
	}

	@Test
	public void testProxySearchNoPath() throws Exception {
		// Prepare mock results
		SearchResult sample = new SearchResult().withHits(new Hits());
		Hit hit = new Hit().withId("syn123");
		sample.getHits().withHit(hit);
		when(mockSearchDao.executeSearch(any(SearchRequest.class))).thenReturn(sample);

		SearchQuery query = new SearchQuery();
		query.setBooleanQuery(new LinkedList<>());
		KeyValue kv = new KeyValue();
		kv.setKey(FIELD_ID);
		kv.setValue("syn123");
		query.getBooleanQuery().add(kv);

		// Make the call
		SearchResults results = searchManager.proxySearch(nonAdminUserInfo, query);
		assertNotNull(results);
		assertNotNull(results.getHits());
		assertEquals(1, results.getHits().size());
		// The path should not be returned unless requested.
		assertNull(results.getHits().get(0).getPath());
		verify(mockSearchDao, times(1)).executeSearch(any(SearchRequest.class));
		verify(mockSearchDocumentDriver,never()).getEntityPath(anyString());
	}

	@Test
	public void testFilterSearchForAuthorizationUserIsAdmin(){
		UserInfo adminUser = new UserInfo(true, 420L);
		SearchManagerImpl.filterSearchForAuthorization(adminUser, searchRequest);
		assertEquals(null, searchRequest.getFilterQuery());
	}

	@Test
	public void testFilterSearchForAuthorizationUserIsNotAdmin(){
		SearchManagerImpl.filterSearchForAuthorization(nonAdminUserInfo, searchRequest);
		assertEquals(SearchUtil.formulateAuthorizationFilter(nonAdminUserInfo), searchRequest.getFilterQuery());
	}


	@Test
	public void testDocumentChangeMessages(){
		Document doc2Null = null;

		Document doc3 = new Document();
		doc3.setId("syn3");

		when(mockTranslator.generateSearchDocumentIfNecessary(any(ChangeMessage.class))).thenReturn(doc1, doc2Null, doc3);

		List<ChangeMessage> messages = Arrays.asList(new ChangeMessage(), new ChangeMessage(), new ChangeMessage());
		//method under test
		searchManager.documentChangeMessages(messages);

		verify(mockSearchDao).sendDocuments(iteratorArgumentCaptor.capture());
		verify(mockRecordLogger).pushAllRecordsAndReset();

		//check that the document iterator now only contains non-null Documents
		Iterator<Document> generatedIterator = iteratorArgumentCaptor.getValue();
		List<Document> documentsInIterator = Lists.newArrayList(generatedIterator);
		assertEquals(2, documentsInIterator.size());
		assertEquals(doc1, documentsInIterator.get(0));
		assertEquals(doc3, documentsInIterator.get(1));
	}
	
	@Test
	public void testDocumentChangeMessagesError(){
		doThrow(new IllegalArgumentException("Fake failure")).when(mockSearchDao).sendDocuments(any(Iterator.class));
		List<ChangeMessage> messages = Arrays.asList(new ChangeMessage(), new ChangeMessage(), new ChangeMessage());
		try {
			//method under test
			searchManager.documentChangeMessages(messages);
			fail();
		} catch (IllegalArgumentException e) {
			// expected
		}
		// clear the records even on failure.
		verify(mockRecordLogger).pushAllRecordsAndReset();

	}
}