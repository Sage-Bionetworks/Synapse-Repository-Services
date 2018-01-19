package org.sagebionetworks.search;

import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient;
import com.amazonaws.services.cloudsearchdomain.model.AmazonCloudSearchDomainException;
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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CloudSearchDomainClientAdapterTest {

	@Mock
	private AmazonCloudSearchDomainClient mockCloudSearchDomainClient;

	@Mock
	private SearchResult mockResponse;

	@Mock
	private AmazonCloudSearchDomainException mockedSearchException;

	private CloudsSearchDomainClientAdapter cloudSearchDomainClientAdapter;

	private SearchRequest searchRequest;

	String endpoint = "http://www.ImALittleEndpoint.com";

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
	public void testSearchNullRequest() {
		cloudSearchDomainClientAdapter.rawSearch(null);
	}


	@Test
	public void testSearchNoError() throws Exception {
		when(mockCloudSearchDomainClient.search(searchRequest)).thenReturn(mockResponse);

		//method under test
		SearchResult result = cloudSearchDomainClientAdapter.rawSearch(searchRequest);

		assertEquals(mockResponse, result);
		verify(mockCloudSearchDomainClient).search(searchRequest);
	}

	@Test
	public void testHandleCloudSearchExceptionsErrorCode5xx() throws Exception {
		when(mockedSearchException.getStatusCode()).thenReturn(504);
		when(mockCloudSearchDomainClient.search(searchRequest)).thenThrow(mockedSearchException);

		//method under test
		RuntimeException resultException = cloudSearchDomainClientAdapter.handleCloudSearchExceptions(mockedSearchException);
		assertEquals(mockedSearchException, resultException);
	}

	@Test
	public void testHandleCloudSearchExceptionsErrorCode4xx() throws Exception {
		when(mockedSearchException.getStatusCode()).thenReturn(400);

		//method under test
		RuntimeException resultException = cloudSearchDomainClientAdapter.handleCloudSearchExceptions(mockedSearchException);

		assertThat(resultException, instanceOf(IllegalArgumentException.class));
		assertEquals(mockedSearchException, resultException.getCause());
	}

	/**
	 * createOrUpdateSearchDocument() Tests
	 */

	@Test(expected = IllegalArgumentException.class)
	public void testSendNullDocument() {
		cloudSearchDomainClientAdapter.sendDocument(null);
	}

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

		assertTrue(IOUtils.contentEquals(new ByteArrayInputStream(documentBytes), capturedRequest.getDocuments()));
		assertEquals(new Long(documentBytes.length), capturedRequest.getContentLength());
	}

}
