package org.sagebionetworks.search;

import static org.junit.Assert.*;

import org.junit.Test;

import static org.mockito.Mockito.when;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

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
public class SearchDaoImplTest {
	AmazonCloudSearchClient mockCloudSearchClient;
	SearchDomainSetup mockSearchDomainSetup;
	CloudSearchClient mockHttpclient;
	SearchDaoImpl dao;
	
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
		mockCloudSearchClient = mock(AmazonCloudSearchClient.class);
		
		mockSearchDomainSetup = mock(SearchDomainSetupImpl.class);
		when(mockSearchDomainSetup.isSearchEnabled()).thenReturn(true);
		when(mockSearchDomainSetup.postInitialize()).thenReturn(false);
		
		mockHttpclient = mock(CloudSearchClient.class);
		
		SearchDaoImpl dao = new SearchDaoImpl();
		ReflectionTestUtils.setField(dao, "awsSearchClient", mockCloudSearchClient);
		ReflectionTestUtils.setField(dao, "searchDomainSetup", mockSearchDomainSetup);
		ReflectionTestUtils.setField(dao, "cloudHttpClient", mockHttpclient);
		
		dao.initialize();

		verify(mockSearchDomainSetup, never()).getDocumentEndpoint();
		verify(mockSearchDomainSetup, never()).getSearchEndpoint();
		
		// Note: calls to mockSearchDomainSetup.getDomainStatus() will all return null
		//       Should throw svc unavailable
		dao.executeSearch("someSearch");
	}

	@Test
	public void testInitializePostInitTrue() throws Exception {
		mockCloudSearchClient = mock(AmazonCloudSearchClient.class);
		
		mockSearchDomainSetup = mock(SearchDomainSetupImpl.class);
		when(mockSearchDomainSetup.isSearchEnabled()).thenReturn(true);
		when(mockSearchDomainSetup.postInitialize()).thenReturn(true);
		when(mockSearchDomainSetup.getDocumentEndpoint()).thenReturn("http://docendpoint");
		when(mockSearchDomainSetup.getSearchEndpoint()).thenReturn("http://searchendpoint");
		DomainStatus expectedStatus1 = new DomainStatus().withProcessing(false);
		when(mockSearchDomainSetup.getDomainStatus()).thenReturn(expectedStatus1);
		
		mockHttpclient = mock(CloudSearchClient.class);
		when(mockHttpclient.performSearch(anyString())).thenReturn("{\"rank\":\"-text_relevance\",\"match-expr\":\"(and 'prostate,cancer' modified_on:1368973180..1429453180 (or acl:'test-user@sagebase.org' acl:'AUTHENTICATED_USERS' acl:'PUBLIC' acl:'test-group'))\",\"hits\":{\"found\":0,\"start\":0,\"hit\":[]},\"facets\":{},\"info\":{\"rid\":\"6ddcaa561c05c4cc85ddb10cb46568af0024f6e4f534231d657d53613aed2d4ea69ed14f5fdff3d1951b339a661631f4\",\"time-ms\":3,\"cpu-time-ms\":0}}");
		when(mockHttpclient.getDocumentServiceEndpoint()).thenReturn("http://docendpoint");
		when(mockHttpclient.getSearchServiceEndpoint()).thenReturn("http://searchendpoint");
		
		SearchDaoImpl dao = new SearchDaoImpl();
		ReflectionTestUtils.setField(dao, "awsSearchClient", mockCloudSearchClient);
		ReflectionTestUtils.setField(dao, "searchDomainSetup", mockSearchDomainSetup);
		ReflectionTestUtils.setField(dao, "cloudHttpClient", mockHttpclient);
		
		dao.initialize();

		verify(mockSearchDomainSetup).getDocumentEndpoint();
		verify(mockSearchDomainSetup).getSearchEndpoint();
		assertEquals("http://docendpoint",mockHttpclient.getDocumentServiceEndpoint());
		assertEquals("http://searchendpoint", mockHttpclient.getSearchServiceEndpoint());
		
		SearchResults results = dao.executeSearch("someSearch");
		assertNotNull(results);
	}

	@Test
	public void testInitializePostInitException() throws Exception {
		mockCloudSearchClient = mock(AmazonCloudSearchClient.class);
		
		mockSearchDomainSetup = mock(SearchDomainSetupImpl.class);
		when(mockSearchDomainSetup.isSearchEnabled()).thenReturn(true);
		when(mockSearchDomainSetup.postInitialize()).thenThrow(new Exception("Some exception occured in SearchDomainSetup.postInitialize()!"));
		
		mockHttpclient = mock(CloudSearchClient.class);
		
		SearchDaoImpl dao = new SearchDaoImpl();
		ReflectionTestUtils.setField(dao, "awsSearchClient", mockCloudSearchClient);
		ReflectionTestUtils.setField(dao, "searchDomainSetup", mockSearchDomainSetup);
		ReflectionTestUtils.setField(dao, "cloudHttpClient", mockHttpclient);
		
		dao.initialize();

		verify(mockSearchDomainSetup, never()).getDocumentEndpoint();
		verify(mockSearchDomainSetup, never()).getSearchEndpoint();
		assertNull(mockHttpclient.getDocumentServiceEndpoint());
		assertNull(mockHttpclient.getSearchServiceEndpoint());
	}

}
