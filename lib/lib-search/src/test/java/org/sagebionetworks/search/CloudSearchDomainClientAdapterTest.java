package org.sagebionetworks.search;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;

import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomain;
import com.amazonaws.services.cloudsearchdomain.model.AmazonCloudSearchDomainException;
import com.amazonaws.services.cloudsearchdomain.model.DocumentServiceException;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsRequest;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsResult;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
public class CloudSearchDomainClientAdapterTest {

	@Mock
	private AmazonCloudSearchDomain mockCloudSearchDomainClient;

	@Mock
	private CloudSearchDocumentBatchIteratorProvider mockProvider;

	@Mock
	private CloudSearchDocumentBatch mockDocumentBatch;

	@Mock
	private SearchResult mockResponse;

	@Mock
	private AmazonCloudSearchDomainException mockedSearchException;

	@Mock
	private CloudSearchLogger mockRecordLogger;

	@Captor
	ArgumentCaptor<UploadDocumentsRequest> uploadRequestCaptor;

	Long documentBatchSize = 42L;

	@Mock
	InputStream mockDocumentBatchInputStream;

	private CloudsSearchDomainClientAdapter cloudSearchDomainClientAdapter;

	private SearchRequest searchRequest;

	String endpoint = "http://www.ImALittleEndpoint.com";

	UploadDocumentsResult uploadDocumentsResult;


	List<CloudSearchDocumentLogRecord> logRecordList;

	@Before
	public void before() {
		cloudSearchDomainClientAdapter = new CloudsSearchDomainClientAdapter(mockCloudSearchDomainClient, mockProvider, mockRecordLogger);
		searchRequest = new SearchRequest().withQuery("aQuery");
		uploadDocumentsResult = new UploadDocumentsResult().withStatus("fakestatus");
		when(mockCloudSearchDomainClient.uploadDocuments(any(UploadDocumentsRequest.class))).thenReturn(uploadDocumentsResult);

		when(mockProvider.getIterator(any())).thenReturn(Iterators.singletonIterator(mockDocumentBatch));
		when(mockDocumentBatch.getNewInputStream()).thenReturn(mockDocumentBatchInputStream);
		when(mockDocumentBatch.size()).thenReturn(documentBatchSize);
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
	public void testSendDocument() throws IOException, InterruptedException {
		Document document = new Document();
		document.setId("syn123");
		document.setType(DocumentTypeNames.add);

		//method under test
		cloudSearchDomainClientAdapter.sendDocument(document);

		verify(mockCloudSearchDomainClient).uploadDocuments(uploadRequestCaptor.capture());
		UploadDocumentsRequest capturedRequest = uploadRequestCaptor.getValue();


		verify(mockDocumentBatch).getNewInputStream();
		verify(mockDocumentBatch).getDocumentIds();
		verify(mockDocumentBatch).size();
		assertEquals("application/json", capturedRequest.getContentType());

		assertEquals(mockDocumentBatchInputStream, capturedRequest.getDocuments());
		assertEquals(documentBatchSize, capturedRequest.getContentLength());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSendDocuments_nullIterator(){
		cloudSearchDomainClientAdapter.sendDocuments((Iterator<Document>) null);
	}

	@Test
	public void testSendDocuments_iteratorMultipleBatchResults() throws InterruptedException {
		Iterator<Document> iterator = Arrays.asList(new Document(), new Document()).iterator();

		when(mockProvider.getIterator(iterator)).thenReturn(Arrays.asList(mockDocumentBatch, mockDocumentBatch).iterator());
		//method under test
		cloudSearchDomainClientAdapter.sendDocuments(iterator);

		verify(mockDocumentBatch, times(2)).getDocumentIds();
		verify(mockDocumentBatch, times(2)).size();
		verify(mockDocumentBatch, times(2)).getNewInputStream();

		verify(mockCloudSearchDomainClient, times(2)).uploadDocuments(any());

		verify(mockRecordLogger, times(2)).currentBatchFinshed("fakestatus");
		verify(mockRecordLogger, times(2)).pushAllRecordsAndReset();
	}

	@Test
	public void testSendDocuments_sleepForMultipleDocumentsInABatch() throws InterruptedException {
		Iterator<Document> iterator = Collections.singletonList(new Document()).iterator();

		when(mockProvider.getIterator(iterator)).thenReturn(Collections.singletonList(mockDocumentBatch).iterator());
		//multiple documents in batch
		when(mockDocumentBatch.getDocumentIds()).thenReturn(Sets.newHashSet("fakeId","fakeid2"));
		//method under test
		cloudSearchDomainClientAdapter.sendDocuments(iterator);

		verify(mockDocumentBatch, times(1)).getDocumentIds();
	}

	@Test
	public void testSendDocuments_noSleepForSingleDocumentInABatch() throws InterruptedException {
		Iterator<Document> iterator = Collections.singletonList(new Document()).iterator();

		when(mockProvider.getIterator(iterator)).thenReturn(Collections.singletonList(mockDocumentBatch).iterator());

		//only one document in batch
		when(mockDocumentBatch.getDocumentIds()).thenReturn(Sets.newHashSet("fakeId"));
		//method under test
		cloudSearchDomainClientAdapter.sendDocuments(iterator);

		verify(mockDocumentBatch, times(1)).getDocumentIds();
	}

	@Test
	public void testSendDocuments_iteratorDocumentError(){
		String failedStatus = "failedStatus";
		when(mockCloudSearchDomainClient.uploadDocuments(any())).thenThrow(new DocumentServiceException("").withStatus(failedStatus));
		Iterator<Document> iterator = Arrays.asList(new Document(), new Document()).iterator();

		try {
			//method under test
			cloudSearchDomainClientAdapter.sendDocuments(iterator);
			fail();
		} catch (DocumentServiceException e){
			//expected
		}
		verify(mockRecordLogger).currentBatchFinshed(failedStatus);
		verify(mockRecordLogger).pushAllRecordsAndReset();
	}

	@Test (expected = TemporarilyUnavailableException.class)
	public void testSendDocuments_IOException() throws IOException {
		InputStream mockInputStream = mock(InputStream.class);
		when(mockDocumentBatch.getNewInputStream()).thenReturn(mockInputStream);
		doThrow(IOException.class).when(mockInputStream).close();

		//method under test
		cloudSearchDomainClientAdapter.sendDocuments(Collections.emptyIterator());
	}

}
