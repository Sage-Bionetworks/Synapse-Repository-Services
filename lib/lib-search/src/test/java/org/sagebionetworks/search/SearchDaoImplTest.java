package org.sagebionetworks.search;

import static org.junit.Assert.*;

import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.when;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Matchers.any;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.cloudsearchv2.AmazonCloudSearchClient;
import com.amazonaws.services.cloudsearchv2.model.DomainStatus;

import org.sagebionetworks.repo.web.ServiceUnavailableException;


/**
 * Unit test for the search dao.
 * 
 * @author jmhill
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SearchDaoImplTest {
	@Mock
	AmazonCloudSearchClient mockCloudSearchClient;
	@Mock
	SearchDomainSetup mockSearchDomainSetup;
	@Mock
	CloudsSearchDomainClientAdapter mockCloudSearchDomainClient;
	SearchDaoImpl dao;

	@Before
	public void setUp(){
		dao = new SearchDaoImpl();
		ReflectionTestUtils.setField(dao, "awsSearchClient", mockCloudSearchClient);
		ReflectionTestUtils.setField(dao, "searchDomainSetup", mockSearchDomainSetup);
		ReflectionTestUtils.setField(dao, "cloudSearchClientAdapter", mockCloudSearchDomainClient);
	}
	
	@Test
	public void testPrepareDocument(){
		Document doc = new Document();
		doc.setId("123");
		// This should prepare the document to be sent
		SearchDaoImpl.prepareDocument(doc);
		assertNotNull(doc.getFields());
		assertEquals("The document ID must be set in the fields when ",doc.getId(), doc.getFields().getId());
		assertNotNull("A version was not set.",doc.getVersion());
	}
	
	@Test(expected=ServiceUnavailableException.class)
	public void testInitializePostInitFalse() throws Exception {
		when(mockSearchDomainSetup.isSearchEnabled()).thenReturn(true);
		when(mockSearchDomainSetup.postInitialize()).thenReturn(false);
		
		dao.initialize();

		verify(mockSearchDomainSetup, never()).getDomainSearchEndpoint();

		// Note: calls to mockSearchDomainSetup.getDomainStatus() will all return null
		//       Should throw svc unavailable
		dao.executeSearch(new SearchRequest().withQuery("someSearch"));
	}

	@Test
	public void testInitializePostInitTrue() throws Exception {
		//TODO: fix

		when(mockSearchDomainSetup.isSearchEnabled()).thenReturn(true);
		when(mockSearchDomainSetup.postInitialize()).thenReturn(true);
		when(mockSearchDomainSetup.getDomainSearchEndpoint()).thenReturn("http://searchendpoint");
		DomainStatus expectedStatus1 = new DomainStatus().withProcessing(false);
		when(mockSearchDomainSetup.getDomainStatus()).thenReturn(expectedStatus1);
		
		SearchResults searchResults = new SearchResults();

		when(mockCloudSearchDomainClient.search(any(SearchRequest.class))).thenReturn(new SearchResults());
		when(mockCloudSearchDomainClient.isInitialized()).thenReturn(true);

		
		dao.initialize();

		verify(mockSearchDomainSetup).getDomainSearchEndpoint();
		assertEquals(true, mockCloudSearchDomainClient.isInitialized());
		
		SearchResults results = dao.executeSearch(new SearchRequest().withQuery("someSearch"));
		assertNotNull(results);
	}

	@Test
	public void testInitializePostInitException() throws Exception {

		when(mockSearchDomainSetup.isSearchEnabled()).thenReturn(true);
		when(mockSearchDomainSetup.postInitialize()).thenThrow(new Exception("Some exception occured in SearchDomainSetup.postInitialize()!"));

		dao.initialize();

		verify(mockSearchDomainSetup, never()).getDomainSearchEndpoint();
		assertFalse(mockCloudSearchDomainClient.isInitialized());
	}

}
