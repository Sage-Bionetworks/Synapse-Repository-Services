package org.sagebionetworks.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient;
import com.amazonaws.services.cloudsearchdomain.model.SearchException;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsRequest;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.search.Document;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class CloudSearchDomainClientAdapterTest {

	@Mock
	AmazonCloudSearchDomainClient mockCloudSearchDomainClient;

	CloudsSearchDomainClientAdapter cloudSearchDomainClientAdapter;

	@Mock
	SearchResult mockResponse;

	@Mock
	SearchException mockedSearchException; //no idea how to change the .getStatusCode() without mocking

	SearchRequest searchRequest;

	String endpoint = "http://www.ImALittleEmdpoint.com";
	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		cloudSearchDomainClientAdapter = new CloudsSearchDomainClientAdapter(mockCloudSearchDomainClient);
		searchRequest = new SearchRequest().withQuery("aQuery");
	}

//	/**
//	 * setEndpoint() Tests
//	 */
//
//	@Test(expected = IllegalArgumentException.class)
//	public void testSetEndpointNullEndpoint(){
//		cloudSearchDomainClientAdapter.setEndpoint(null);
//	}
//
//	@Test(expected = IllegalArgumentException.class)
//	public void testSetEndpointEmptyStringEndpoint(){
//		cloudSearchDomainClientAdapter.setEndpoint("");
//	}

	/**
	 * search() Tests
	 */

	@Test(expected = IllegalArgumentException.class)
	public void testSearchNullRequest() throws CloudSearchClientException{
		cloudSearchDomainClientAdapter.rawSearch(null);
	}

	@Test(expected = IllegalStateException.class)
	public void testSearchBeforeEndpointSet() throws CloudSearchClientException{
		cloudSearchDomainClientAdapter.rawSearch(searchRequest);
	}


	@Test
	public void testSearchNoError() throws Exception {
//		cloudSearchDomainClientAdapter.setEndpoint(endpoint);
		when(mockCloudSearchDomainClient.search(searchRequest)).thenReturn(mockResponse);

		//method under test
		SearchResult result = cloudSearchDomainClientAdapter.rawSearch(searchRequest);

		assertEquals(mockResponse, result);
		verify(mockCloudSearchDomainClient).search(searchRequest);
//		verify(mockCloudSearchDomainClient).setEndpoint(endpoint);
	}

	@Test
	public void testSearchOnErrorCode5xx() throws Exception {
//		cloudSearchDomainClientAdapter.setEndpoint(endpoint);

		when(mockedSearchException.getStatusCode()).thenReturn(504);
		when(mockCloudSearchDomainClient.search(searchRequest)).thenThrow(mockedSearchException);

		//method under test
		try {
			SearchResult result = cloudSearchDomainClientAdapter.rawSearch(searchRequest);
		} catch (SearchException e){
			assertEquals(mockedSearchException, e);
		}
		verify(mockCloudSearchDomainClient).search(searchRequest);
//		verify(mockCloudSearchDomainClient).setEndpoint(endpoint);
	}

	@Test
	public void testSearchOnErrorCode4xx() throws Exception {
//		cloudSearchDomainClientAdapter.setEndpoint(endpoint);

		String exceptionMessage = "Some message";
		when(mockedSearchException.getStatusCode()).thenReturn(400);
		when(mockedSearchException.getMessage()).thenReturn(exceptionMessage);
		when(mockCloudSearchDomainClient.search(searchRequest)).thenThrow(mockedSearchException);

		//method under test
		try {
			SearchResult result = cloudSearchDomainClientAdapter.rawSearch(searchRequest);
		} catch (CloudSearchClientException e){
			assertEquals(exceptionMessage, e.getMessage());
		}

		verify(mockCloudSearchDomainClient).search(searchRequest);
//		verify(mockCloudSearchDomainClient).setEndpoint(endpoint);
	}

	/**
	 * createOrUpdateSearchDocument() Tests
	 */

	@Test(expected = IllegalArgumentException.class)
	public void testSendNullDocument(){
		cloudSearchDomainClientAdapter.sendDocument(null);
	}

//	@Test(expected = IllegalStateException.class)
//	public void testSendDocumentBeforeEndpointSet() throws CloudSearchClientException{
//		cloudSearchDomainClientAdapter.sendDocument("omae wa mou shindeiru");
//	}

	@Test
	public void testSendDocument() throws IOException {
		Document document = new Document();
		ArgumentCaptor<UploadDocumentsRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadDocumentsRequest.class);
//		cloudSearchDomainClientAdapter.setEndpoint(endpoint);
		cloudSearchDomainClientAdapter.sendDocument(document);
		verify(mockCloudSearchDomainClient).uploadDocuments(uploadRequestCaptor.capture());
		UploadDocumentsRequest capturedRequest = uploadRequestCaptor.getValue();


		byte[] documentBytes = document.toString().getBytes();
		assertEquals("application/json", capturedRequest.getContentType());
		assertTrue(IOUtils.contentEquals(new ByteArrayInputStream(documentBytes), capturedRequest.getDocuments()));
		assertEquals(new Long(documentBytes.length), capturedRequest.getContentLength());
	}

}
