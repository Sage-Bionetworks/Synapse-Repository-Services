package org.sagebionetworks.search;

import static org.junit.Assert.*;

import org.junit.Test;

import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeast;

import org.sagebionetworks.repo.model.search.Document;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.cloudsearch.AmazonCloudSearchClient;

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
	CloudSearchClient client;
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
		
		client = new CloudSearchClient();
		
		SearchDaoImpl dao = new SearchDaoImpl();
		ReflectionTestUtils.setField(dao, "awsSearchClient", mockCloudSearchClient);
		ReflectionTestUtils.setField(dao, "searchDomainSetup", mockSearchDomainSetup);
		ReflectionTestUtils.setField(dao, "cloudHttpClient", client);
		
		dao.initialize();

		verify(mockSearchDomainSetup, never()).getDocumentEndpoint();
		verify(mockSearchDomainSetup, never()).getSearchEndpoint();
		
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
		
		client = new CloudSearchClient();
		
		SearchDaoImpl dao = new SearchDaoImpl();
		ReflectionTestUtils.setField(dao, "awsSearchClient", mockCloudSearchClient);
		ReflectionTestUtils.setField(dao, "searchDomainSetup", mockSearchDomainSetup);
		ReflectionTestUtils.setField(dao, "cloudHttpClient", client);
		
		dao.initialize();

		verify(mockSearchDomainSetup).getDocumentEndpoint();
		verify(mockSearchDomainSetup).getSearchEndpoint();
		assertEquals("http://docendpoint",client.getDocumentServiceEndpoint());
		assertEquals("http://searchendpoint", client.getSearchServiceEndpoint());

	}

	@Test
	public void testInitializePostInitException() throws Exception {
		mockCloudSearchClient = mock(AmazonCloudSearchClient.class);
		
		mockSearchDomainSetup = mock(SearchDomainSetupImpl.class);
		when(mockSearchDomainSetup.isSearchEnabled()).thenReturn(true);
		when(mockSearchDomainSetup.postInitialize()).thenThrow(new Exception("Some exception occured in SearchDomainSetup.postInitialize()!"));
		
		client = new CloudSearchClient();
		
		SearchDaoImpl dao = new SearchDaoImpl();
		ReflectionTestUtils.setField(dao, "awsSearchClient", mockCloudSearchClient);
		ReflectionTestUtils.setField(dao, "searchDomainSetup", mockSearchDomainSetup);
		ReflectionTestUtils.setField(dao, "cloudHttpClient", client);
		
		dao.initialize();

		verify(mockSearchDomainSetup, never()).getDocumentEndpoint();
		verify(mockSearchDomainSetup, never()).getSearchEndpoint();
		assertNull(client.getDocumentServiceEndpoint());
		assertNull(client.getSearchServiceEndpoint());
	}

}
