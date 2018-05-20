package org.sagebionetworks.search;

import static org.junit.Assert.*;

import com.amazonaws.services.cloudsearchdomain.model.Hit;
import com.amazonaws.services.cloudsearchdomain.model.Hits;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.google.common.collect.Sets;
import org.apache.commons.collections.ArrayStack;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;

import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.cloudsearchv2.AmazonCloudSearch;
import com.amazonaws.services.cloudsearchv2.model.DomainStatus;

import org.sagebionetworks.repo.web.ServiceUnavailableException;

import javax.print.Doc;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * Unit test for the search dao.
 * 
 * @author jmhill
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SearchDaoImplTest {
	@Mock
	private CloudSearchClientProvider mockCloudSearchClientProvider;
	@Mock
	private CloudsSearchDomainClientAdapter mockCloudSearchDomainClient;

	@Spy
	private SearchDaoImpl dao = new SearchDaoImpl(); //variable must be initialized here for Spy to work

	private SearchResult searchResult;

	private ArgumentCaptor<SearchRequest> requestArgumentCaptor;

	@Before
	public void setUp(){
		ReflectionTestUtils.setField(dao, "cloudSearchClientProvider", mockCloudSearchClientProvider);
		when(mockCloudSearchClientProvider.getCloudSearchClient()).thenReturn(mockCloudSearchDomainClient);

		requestArgumentCaptor = ArgumentCaptor.forClass(SearchRequest.class);
		searchResult = new SearchResult();
		when(mockCloudSearchDomainClient.rawSearch(requestArgumentCaptor.capture())).thenReturn(searchResult);
	}


	///////////////////////////
	// deleteDocuments() tests
	///////////////////////////

	@Test(expected = IllegalArgumentException.class)
	public void testDeleteDocumentsNullDocIds(){
		dao.deleteDocuments(null);
	}

	@Test
	public void testDeleteDocumentsEmptyIdSet(){
		dao.deleteDocuments(new HashSet<>());
		verify(mockCloudSearchClientProvider, never()).getCloudSearchClient();
		verify(mockCloudSearchDomainClient, never()).sendDocuments(any());
	}

	@Test
	public void testDeleteDocumentsMultipleIds(){
		String id1 = "syn123";
		String id2 = "syn456";

		//Expected documents
		Document expectedDoc1 = new Document();
		expectedDoc1.setId(id1);
		expectedDoc1.setType(DocumentTypeNames.delete);
		Document expectedDoc2 = new Document();
		expectedDoc2.setId(id2);
		expectedDoc2.setType(DocumentTypeNames.delete);

		//method under test
		dao.deleteDocuments(new LinkedHashSet<>(Arrays.asList(id1, id2)));

		verify(dao, times(1)).createOrUpdateSearchDocument(Arrays.asList(expectedDoc1, expectedDoc2));
	}

	/////////////////////////////////////////
	// createOrUpdateSearchDocument() tests
	/////////////////////////////////////////

	@Test (expected = IllegalArgumentException.class)
	public void testCreateOrUpdateSearchDocumentNullDocument(){
		dao.createOrUpdateSearchDocument((Document) null);
	}

	@Test
	public void testCreateOrUpdateSearchDocumentSingleDocument(){
		Document doc = new Document();
		dao.createOrUpdateSearchDocument(doc);
		verify(dao, times(1)).createOrUpdateSearchDocument(Collections.singletonList(doc));
	}

	@Test
	public void testCreateOrUpdateSearchDocumentUsingList(){
		List<Document> docList = new LinkedList<>();
		dao.createOrUpdateSearchDocument(docList);
		verify(mockCloudSearchClientProvider,times(1)).getCloudSearchClient();
		verify(mockCloudSearchDomainClient, times(1)).sendDocuments(docList);
	}

	/////////////////////////
	// executeSearch() tests
	/////////////////////////

	@Test
	public void testExecuteSearch()  {
		SearchRequest searchRequest = new SearchRequest();
		SearchResult result = dao.executeSearch(searchRequest);
		assertEquals(result, searchResult);
		verify(mockCloudSearchClientProvider,times(1)).getCloudSearchClient();
		verify(mockCloudSearchDomainClient, times(1)).rawSearch(new SearchRequest());
	}

	///////////////////////
	// doesDocumentExist()
	///////////////////////
	@Test(expected = IllegalArgumentException.class)
	public void testDoesDocumentExistNullId()  {
		dao.doesDocumentExist(null, "etag");
	}

	@Test
	public void testDoesDocumentExistReturnsTrue(){
		helperTestDoesDocumentExist(1L, true);
	}

	@Test
	public void testDoesDocumentExistReturnsFalse(){
		helperTestDoesDocumentExist(0L, false);
	}

	private void helperTestDoesDocumentExist(long numFoundHits, boolean expectedBooleanResult){
		searchResult.withHits(new Hits().withFound(numFoundHits));

		boolean result = dao.doesDocumentExist("syn123", "etagerino");
		assertEquals(expectedBooleanResult, result);

		SearchRequest capturedRequest = requestArgumentCaptor.getValue();
		verify(dao,times(1)).executeSearch(capturedRequest);
		assertEquals("(and _id:'syn123' etag:'etagerino')", capturedRequest.getQuery());
	}

	//////////////////////////////
	// listSearchDocuments() test
	//////////////////////////////

	@Test
	public void testListSearchDocuments()  {
		long limit = 42;
		long offset = 420;
		SearchResult result = dao.listSearchDocuments(limit, offset);

		assertEquals(searchResult, result);
		SearchRequest capturedRequest = requestArgumentCaptor.getValue();
		verify(dao, times(1)).executeSearch(capturedRequest);
		assertEquals("matchall", capturedRequest.getQuery());
		assertEquals((Long) limit, capturedRequest.getSize());
		assertEquals((Long) offset, capturedRequest.getStart());
	}

	////////////////////////////
	// deleteAllDocuments() test
	////////////////////////////

	@Test
	public void testDeleteAllDocuments() throws InterruptedException {
		String hitId1 = "syn123";
		String hitId2 = "syn456";

		//because deleteAllDocuments will stop only once no more documents are found,
		// we must provide different SearchResult on each call to listSearchDocuments()
		doAnswer(new Answer() {
			private Iterator<SearchResult> searchResultIterator = Arrays.asList(
					//first result w/ 2 hits
					new SearchResult().withHits(new Hits().withFound(2L)
					.withHit(new Hit().withId(hitId1), new Hit().withId(hitId2))),
					//second result w/ 0 hits
					new SearchResult().withHits(new Hits().withFound(0L))
			).iterator();

			@Override
			public SearchResult answer(InvocationOnMock invocationOnMock) throws Throwable {
				return searchResultIterator.next();
			}
		}).when(dao).listSearchDocuments(anyLong(), anyLong());

		ArgumentCaptor<Set> idSetCaptor = ArgumentCaptor.forClass(Set.class);
		doNothing().when(dao).deleteDocuments(idSetCaptor.capture());

		dao.deleteAllDocuments();

		verify(dao, times(2)).listSearchDocuments(1000,0);
		Set<String> capturedIdSet = idSetCaptor.getValue();
		verify(dao, times(1)).deleteDocuments(capturedIdSet);
		assertEquals(Sets.newHashSet(hitId1, hitId2), capturedIdSet);
	}

}
