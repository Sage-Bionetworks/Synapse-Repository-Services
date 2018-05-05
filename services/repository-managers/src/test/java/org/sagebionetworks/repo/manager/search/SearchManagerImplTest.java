package org.sagebionetworks.repo.manager.search;

import com.amazonaws.services.cloudsearchdomain.model.Hit;
import com.amazonaws.services.cloudsearchdomain.model.Hits;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.search.SearchDao;
import org.sagebionetworks.search.SearchUtil;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
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
	private SearchRequest searchRequest;

	private SearchManagerImpl searchManager;


	private ChangeMessage message;
	private ChangeMessage message2;

	@Before
	public void before(){
		searchManager = new SearchManagerImpl();
		ReflectionTestUtils.setField(searchManager, "searchDao", mockSearchDao);
		ReflectionTestUtils.setField(searchManager, "searchDocumentDriver", mockSearchDocumentDriver);



		nonAdminUserInfo = new UserInfo(false, 990L);
		Set<Long> userGroups = new HashSet<>();
		userGroups.add(123L);
		userGroups.add(8008135L);
		nonAdminUserInfo.setGroups(userGroups);
		searchRequest = new SearchRequest();


		//documentChangeMessage() test setup
		message = new ChangeMessage();
		message.setChangeType(ChangeType.CREATE);
		message.setObjectEtag("etag1");
		message.setObjectId("one");
		message.setObjectType(ObjectType.ENTITY);

		message2 = new ChangeMessage();
		message2.setChangeType(ChangeType.CREATE);
		message2.setObjectEtag("etag2");
		message2.setObjectId("two");
		message2.setObjectType(ObjectType.ENTITY);
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
	public void testdocumentChangeMessageChangeTypeDelete() throws Exception{
		// create a few delete messages.
		message.setChangeType(ChangeType.DELETE);
		// call under test
		searchManager.documentChangeMessage( message);
		// Delete should be called
		verify(mockSearchDao, times(1)).deleteDocument("one");
		// create should not be called
		verify(mockSearchDao, never()).createOrUpdateSearchDocument(anyListOf(Document.class));
	}

	@Test
	public void testdocumentChangeMessageChangeTypeCreate() throws Exception{
		Document docOne = new Document();
		docOne.setId("one");
		when(mockSearchDocumentDriver.formulateSearchDocument("one")).thenReturn(docOne);
		Document docTwo = new Document();
		docTwo.setId("two");
		when(mockSearchDocumentDriver.formulateSearchDocument("two")).thenReturn(docTwo);

		// Create only occurs if the document exists in the repository
		when(mockSearchDocumentDriver.doesNodeExist("one", "etag1")).thenReturn(true);
		when(mockSearchDocumentDriver.doesNodeExist("two", "etag2")).thenReturn(true);

		// Create only occurs if it is not already in the search index
		when(mockSearchDao.doesDocumentExist("one", "etag1")).thenReturn(false);
		when(mockSearchDao.doesDocumentExist("two", "etag2")).thenReturn(false);

		// call under test
		searchManager.documentChangeMessage( message);
		searchManager.documentChangeMessage( message2);

		// Delete should be called
		verify(mockSearchDao, never()).deleteDocuments(anySetOf(String.class));
		// create should be called once
		verify(mockSearchDao, times(1)).createOrUpdateSearchDocument(docOne);
		verify(mockSearchDao, times(1)).createOrUpdateSearchDocument(docTwo);
	}

	/**
	 * When the document already exits in the search index with the same etag, we can ignore it.
	 */
	@Test
	public void testDocumentChangeMessageChangeTypeCreateAlreadyInSearchIndex() throws IOException {
		// Create only occurs if the document exists in the repository
		when(mockSearchDocumentDriver.doesNodeExist("one", "etag1")).thenReturn(true);
		// Create only occurs if it is not already in the search index
		when(mockSearchDao.doesDocumentExist("one", "etag1")).thenReturn(true);

		// call under test
		searchManager.documentChangeMessage(message);

		// Delete should be called
		verify(mockSearchDao, never()).deleteDocuments(anySetOf(String.class));
		// create should not be called
		verify(mockSearchDao, never()).createOrUpdateSearchDocument(any(Document.class));
		// We should not call doesNodeExist() on the repository when it already exists in the search index.
		verify(mockSearchDocumentDriver, never()).doesNodeExist("one", "etag1");
	}

	/**
	 * When the document already exits in the search index with the same etag, we can ignore it.
	 */
	@Test
	public void testDocumentChangeMessageChangeTypeCreateDoesNotExistInRepository() throws IOException {
		// Create only occurs if the document exists in the repository
		when(mockSearchDocumentDriver.doesNodeExist("one", "etag1")).thenReturn(false);
		// Create only occurs if it is not already in the search index
		when(mockSearchDao.doesDocumentExist("one", "etag1")).thenReturn(false);

		// call under test
		searchManager.documentChangeMessage( message);

		// Delete should be called
		verify(mockSearchDao, never()).deleteDocuments(anySetOf(String.class));
		// create should not be called
		verify(mockSearchDao, never()).createOrUpdateSearchDocument(anyListOf(Document.class));
		// We should not call doesNodeExist() one time.
		verify(mockSearchDocumentDriver, times(1)).doesNodeExist("one", "etag1");
	}
}