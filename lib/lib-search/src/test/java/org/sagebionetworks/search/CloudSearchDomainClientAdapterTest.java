package org.sagebionetworks.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.auth.BasicAWSCredentials;
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
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;

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
		cloudSearchDomainClientAdapter = new CloudsSearchDomainClientAdapter(new BasicAWSCredentials("FakeKey", "FakeKey")); //TODO:z fix
		ReflectionTestUtils.setField(cloudSearchDomainClientAdapter, "client", mockCloudSearchDomainClient);
		searchRequest = new SearchRequest().withQuery("aQuery");
	}

	/**
	 * setEndpoint() Tests
	 */

	@Test(expected = IllegalArgumentException.class)
	public void testSetEndpointNullEndpoint(){
		cloudSearchDomainClientAdapter.setEndpoint(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetEndpointEmptyStringEndpoint(){
		cloudSearchDomainClientAdapter.setEndpoint("");
	}

	/**
	 * search() Tests
	 */

	@Test(expected = IllegalArgumentException.class)
	public void testSearchNullRequest() throws CloudSearchClientException{
		cloudSearchDomainClientAdapter.search(null);
	}

	@Test(expected = IllegalStateException.class)
	public void testSearchBeforeEndpointSet() throws CloudSearchClientException{
		cloudSearchDomainClientAdapter.search(searchRequest);
	}


	@Test
	public void testSearchNoError() throws Exception {
		cloudSearchDomainClientAdapter.setEndpoint(endpoint);
		when(mockCloudSearchDomainClient.search(searchRequest)).thenReturn(mockResponse);

		//method under test
		SearchResults result = cloudSearchDomainClientAdapter.search(searchRequest);

		assertEquals(SearchUtil.convertToSynapseSearchResult(mockResponse), result);
		verify(mockCloudSearchDomainClient).search(searchRequest);
		verify(mockCloudSearchDomainClient).setEndpoint(endpoint);
	}

	@Test
	public void testSearchOnErrorCode5xx() throws Exception {
		cloudSearchDomainClientAdapter.setEndpoint(endpoint);

		when(mockedSearchException.getStatusCode()).thenReturn(504);
		when(mockCloudSearchDomainClient.search(searchRequest)).thenThrow(mockedSearchException);

		//method under test
		try {
			SearchResults result = cloudSearchDomainClientAdapter.search(searchRequest);
		} catch (SearchException e){
			assertEquals(mockedSearchException, e);
		}
		verify(mockCloudSearchDomainClient).search(searchRequest);
		verify(mockCloudSearchDomainClient).setEndpoint(endpoint);
	}

	@Test
	public void testSearchOnErrorCode4xx() throws Exception {
		cloudSearchDomainClientAdapter.setEndpoint(endpoint);

		String exceptionMessage = "Some message";
		when(mockedSearchException.getStatusCode()).thenReturn(400);
		when(mockedSearchException.getMessage()).thenReturn(exceptionMessage);
		when(mockCloudSearchDomainClient.search(searchRequest)).thenThrow(mockedSearchException);

		//method under test
		try {
			SearchResults result = cloudSearchDomainClientAdapter.search(searchRequest);
		} catch (CloudSearchClientException e){
			assertEquals(exceptionMessage, e.getMessage());
		}

		verify(mockCloudSearchDomainClient).search(searchRequest);
		verify(mockCloudSearchDomainClient).setEndpoint(endpoint);
	}

	/**
	 * sendDocument() Tests
	 */

	@Test(expected = IllegalArgumentException.class)
	public void testSendNullDocument(){
		cloudSearchDomainClientAdapter.sendDocuments(null);
	}

	@Test(expected = IllegalStateException.class)
	public void testSendDocumentBeforeEndpointSet() throws CloudSearchClientException{
		cloudSearchDomainClientAdapter.sendDocuments("omae wa mou shindeiru");
	}

	@Test
	public void testSendDocument() throws IOException {
		String documentString = "omae wa mou shindeiru";
		ArgumentCaptor<UploadDocumentsRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadDocumentsRequest.class);
		cloudSearchDomainClientAdapter.setEndpoint(endpoint);
		cloudSearchDomainClientAdapter.sendDocuments(documentString);
		verify(mockCloudSearchDomainClient).uploadDocuments(uploadRequestCaptor.capture());
		UploadDocumentsRequest capturedRequest = uploadRequestCaptor.getValue();


		byte[] documentBytes = documentString.getBytes();
		assertEquals("application/json", capturedRequest.getContentType());
		assertTrue(IOUtils.contentEquals(new ByteArrayInputStream(documentBytes), capturedRequest.getDocuments()));
		assertEquals(new Long(documentBytes.length), capturedRequest.getContentLength());
	}

}
