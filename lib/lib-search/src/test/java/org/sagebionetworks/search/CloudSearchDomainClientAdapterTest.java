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
import org.sagebionetworks.repo.model.search.DocumentTypeNames;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;

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

	/**
	 * search() Tests
	 */

	@Test(expected = IllegalArgumentException.class)
	public void testSearchNullRequest(){
		cloudSearchDomainClientAdapter.rawSearch(null);
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
		String exceptionMessage = "Some message";

		when(mockedSearchException.getStatusCode()).thenReturn(504);
		when(mockedSearchException.getMessage()).thenReturn(exceptionMessage);
		when(mockCloudSearchDomainClient.search(searchRequest)).thenThrow(mockedSearchException);

		//method under test
		try {
			SearchResult result = cloudSearchDomainClientAdapter.rawSearch(searchRequest);
		} catch (CloudSearchServerException e){
			assertEquals(exceptionMessage, e.getMessage());
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
		document.setId("syn123");
		document.setType(DocumentTypeNames.add);
		ArgumentCaptor<UploadDocumentsRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadDocumentsRequest.class);

		//method under test
		cloudSearchDomainClientAdapter.sendDocument(document);

		verify(mockCloudSearchDomainClient).uploadDocuments(uploadRequestCaptor.capture());
		UploadDocumentsRequest capturedRequest = uploadRequestCaptor.getValue();

		byte[] documentBytes = SearchUtil.convertSearchDocumentsToJSONString(Collections.singletonList(document)).getBytes();
		assertEquals("application/json", capturedRequest.getContentType());

		//TODO: this assertTrue intermittently fails (about 1/10 times)
		assertTrue(IOUtils.contentEquals(new ByteArrayInputStream(documentBytes), capturedRequest.getDocuments()));
		assertEquals(new Long(documentBytes.length), capturedRequest.getContentLength());
	}

}
