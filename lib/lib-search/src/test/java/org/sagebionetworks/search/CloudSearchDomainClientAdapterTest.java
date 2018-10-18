package org.sagebionetworks.search;

import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomain;
import com.amazonaws.services.cloudsearchdomain.model.AmazonCloudSearchDomainException;
import com.amazonaws.services.cloudsearchdomain.model.DocumentServiceException;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsRequest;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsResult;
import org.apache.commons.io.IOUtils;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CloudSearchDomainClientAdapterTest {

	@Mock
	private AmazonCloudSearchDomain mockCloudSearchDomainClient;

	@Mock
	private SearchResult mockResponse;

	@Mock
	private AmazonCloudSearchDomainException mockedSearchException;

	@Captor
	ArgumentCaptor<UploadDocumentsRequest> uploadRequestCaptor;

	private CloudsSearchDomainClientAdapter cloudSearchDomainClientAdapter;

	private SearchRequest searchRequest;

	String endpoint = "http://www.ImALittleEndpoint.com";

	Document deleteDocument;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		cloudSearchDomainClientAdapter = new CloudsSearchDomainClientAdapter(mockCloudSearchDomainClient);
		searchRequest = new SearchRequest().withQuery("aQuery");

		deleteDocument = new Document();
		deleteDocument.setId("syn123");
		deleteDocument.setType(DocumentTypeNames.delete);
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
	 * sendDocument() Tests
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

		//method under test
		cloudSearchDomainClientAdapter.sendDocument(document);

		verify(mockCloudSearchDomainClient).uploadDocuments(uploadRequestCaptor.capture());
		UploadDocumentsRequest capturedRequest = uploadRequestCaptor.getValue();
		byte[] documentBytes = "[{\"type\":\"add\",\"id\":\"syn123\",\"fields\":{}}]".getBytes(StandardCharsets.UTF_8);
		assertEquals("application/json", capturedRequest.getContentType());

		assertTrue(IOUtils.contentEquals(new ByteArrayInputStream(documentBytes), capturedRequest.getDocuments()));
		assertEquals(new Long(documentBytes.length), capturedRequest.getContentLength());
	}

	@Test
	public void testSendDocument400LevelError(){
		DocumentServiceException exception = mock(DocumentServiceException.class);
		when(exception.getStatusCode()).thenReturn(400);
		when(mockCloudSearchDomainClient.uploadDocuments(any(UploadDocumentsRequest.class))).thenThrow(exception);
		Document document = new Document();
		document.setId("syn123");
		document.setType(DocumentTypeNames.add);
		try {
			cloudSearchDomainClientAdapter.sendDocument(document);
			fail();
		}catch (IllegalArgumentException e){
			//expected
			assertEquals("syn123 search documents could not be uploaded.", e.getMessage());
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSendDocuments_nullIterator(){
		cloudSearchDomainClientAdapter.sendDocuments((Iterator<Document>) null);
	}

	@Test
	public void testSendDocuments_iterator(){
		Iterator<Document> iterator = Collections.singletonList(deleteDocument).iterator();

		//because the File used by the method under test will be deleted after upload completes,
		//capture the document string by copying it out when the tested method is run
		StringBuilder uploadedDocument = new StringBuilder();
		when(mockCloudSearchDomainClient.uploadDocuments(any(UploadDocumentsRequest.class))).thenAnswer(
				(InvocationOnMock invocation) -> {
					UploadDocumentsRequest request = invocation.getArgumentAt(0, UploadDocumentsRequest.class);
					//use StringBuilder to copy document value out
					uploadedDocument.append(IOUtils.toString(request.getDocuments(), "UTF-8"));
					return new UploadDocumentsResult();
				}
			);

		//method under test
		cloudSearchDomainClientAdapter.sendDocuments(iterator);


		verify(mockCloudSearchDomainClient).uploadDocuments(uploadRequestCaptor.capture());

		String expectedJson = "[{\"type\":\"delete\",\"id\":\"syn123\"}]";
		//verify expected document would be sent
		assertEquals(expectedJson, uploadedDocument.toString());
		UploadDocumentsRequest uploadRequest = uploadRequestCaptor.getValue();
		assertEquals(new Long(expectedJson.getBytes(StandardCharsets.UTF_8).length), uploadRequest.getContentLength());
		assertEquals("application/json", uploadRequest.getContentType());
	}

}
