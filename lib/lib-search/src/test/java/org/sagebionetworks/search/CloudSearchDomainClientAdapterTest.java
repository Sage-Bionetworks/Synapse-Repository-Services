package org.sagebionetworks.search;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.sagebionetworks.search.CloudSearchDocumentGenerationAwsKinesisLogRecord.KINESIS_DATA_STREAM_NAME_SUFFIX;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.amazonaws.services.cloudsearchdomain.model.DocumentServiceException;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;

import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomain;
import com.amazonaws.services.cloudsearchdomain.model.AmazonCloudSearchDomainException;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsRequest;
import com.google.common.collect.Iterators;

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
	private AwsKinesisFirehoseLogger mockFirehoseLogger;

	@Captor
	ArgumentCaptor<UploadDocumentsRequest> uploadRequestCaptor;

	Long documentBatchSize = 42L;

	@Mock
	InputStream mockDocumentBatchInputStream;

	private CloudsSearchDomainClientAdapter cloudSearchDomainClientAdapter;

	private SearchRequest searchRequest;

	String endpoint = "http://www.ImALittleEndpoint.com";

	UploadDocumentsResult uploadDocumentsResult;


	List<CloudSearchDocumentGenerationAwsKinesisLogRecord> logRecordList;

	@Before
	public void before() {
		cloudSearchDomainClientAdapter = new CloudsSearchDomainClientAdapter(mockCloudSearchDomainClient, mockProvider, mockFirehoseLogger);
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
	public void testSendDocument() throws IOException {
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
	public void testSendDocuments_iteratorMultipleBatchResults(){
		setUpThreadLocalListForTest();
		Iterator<Document> iterator = Arrays.asList(new Document(), new Document()).iterator();

		when(mockProvider.getIterator(iterator)).thenReturn(Arrays.asList(mockDocumentBatch, mockDocumentBatch).iterator());

		//method under test
		cloudSearchDomainClientAdapter.sendDocuments(iterator);

		verify(mockDocumentBatch, times(2)).getDocumentIds();
		verify(mockDocumentBatch, times(2)).size();
		verify(mockDocumentBatch, times(2)).getNewInputStream();

		verify(mockCloudSearchDomainClient, times(2)).uploadDocuments(any());

		//threadLocalwas only setup once so on second iteration of the iterator loop, nothing is logged.
		// In real use, threadLocalList would once again have values added to it by the Iterator<Document> it relies on
		verifyThreadLocalListForTest();
	}

	@Test
	public void testSendDocuments_iteratorDocumentError(){
		setUpThreadLocalListForTest();
		when(mockCloudSearchDomainClient.uploadDocuments(any())).thenThrow(new DocumentServiceException("").withStatus("fakestatus"));
		Iterator<Document> iterator = Arrays.asList(new Document(), new Document()).iterator();

		try {
			//method under test
			cloudSearchDomainClientAdapter.sendDocuments(iterator);
			fail();
		} catch (DocumentServiceException e){
			//expected
		}

		verifyThreadLocalListForTest();
	}

	@Test (expected = TemporarilyUnavailableException.class)
	public void testSendDocuments_IOException() throws IOException {
		InputStream mockInputStream = mock(InputStream.class);
		when(mockDocumentBatch.getNewInputStream()).thenReturn(mockInputStream);
		doThrow(IOException.class).when(mockInputStream).close();

		//method under test
		cloudSearchDomainClientAdapter.sendDocuments(Collections.emptyIterator());
	}

	private void setUpThreadLocalListForTest(){
		CloudSearchDocumentGenerationAwsKinesisLogRecord record1 = new CloudSearchDocumentGenerationAwsKinesisLogRecord()
				.withChangeNumber(1);
		CloudSearchDocumentGenerationAwsKinesisLogRecord record2 = new CloudSearchDocumentGenerationAwsKinesisLogRecord()
				.withChangeNumber(2);

		logRecordList = Arrays.asList(record1, record2);
		for(CloudSearchDocumentGenerationAwsKinesisLogRecord record : logRecordList){
			assertNull("fakestatus", record.getDocumentBatchUpdateStatus());
			assertNull(record.getDocumentBatchUpdateTimestamp());
			assertNull(record.getDocumentBatchUUID());
		}

		CloudsSearchDomainClientAdapter.threadLocalRecordList.get().add(record1);
		CloudsSearchDomainClientAdapter.threadLocalRecordList.get().add(record2);

		//consume the stream passed into the argument so that the lazily evaluated Stream.map() can be executed
		doAnswer((invocationOnMock -> invocationOnMock.getArgument(1, Stream.class).collect(Collectors.toList())))
				.when(mockFirehoseLogger).logBatch(anyString(), any(Stream.class));
	}

	private void verifyThreadLocalListForTest(){
		//the threadlocal map should have been cleared upon sending log records
		assertTrue(CloudsSearchDomainClientAdapter.threadLocalRecordList.get().isEmpty());

		ArgumentCaptor<Stream<AwsKinesisLogRecord>> captor = ArgumentCaptor.forClass(Stream.class);
		verify(mockFirehoseLogger, times(1)).logBatch(eq(KINESIS_DATA_STREAM_NAME_SUFFIX), captor.capture());

		for(CloudSearchDocumentGenerationAwsKinesisLogRecord record : logRecordList){
			assertEquals("fakestatus", record.getDocumentBatchUpdateStatus());
			assertNotNull(record.getDocumentBatchUpdateTimestamp());
			assertNotNull(record.getDocumentBatchUUID());
		}
	}

}
