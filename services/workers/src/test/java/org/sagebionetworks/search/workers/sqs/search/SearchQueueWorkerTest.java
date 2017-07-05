package org.sagebionetworks.search.workers.sqs.search;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.search.SearchDocumentDriver;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.search.SearchDao;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.model.Message;

public class SearchQueueWorkerTest {
	
	private ProgressCallback mockCallback;
	private SearchDao mockSearchDao;
	private SearchDocumentDriver mockDocumentProvider;
	private V2WikiPageDao mockWikiPageDao;
	private WorkerLogger mockWorkerLogger;
	private SearchQueueWorker worker;
	private ChangeMessage message;
	private ChangeMessage message2;
	
	@Before
	public void before(){
		mockCallback = Mockito.mock(ProgressCallback.class);
		mockSearchDao = Mockito.mock(SearchDao.class);
		mockDocumentProvider = Mockito.mock(SearchDocumentDriver.class);
		mockWikiPageDao = Mockito.mock(V2WikiPageDao.class);
		mockWorkerLogger = Mockito.mock(WorkerLogger.class);
		when(mockSearchDao.isSearchEnabled()).thenReturn(true);
		worker = new SearchQueueWorker();
		ReflectionTestUtils.setField(worker, "searchDao", mockSearchDao);
		ReflectionTestUtils.setField(worker, "searchDocumentDriver", mockDocumentProvider);
		ReflectionTestUtils.setField(worker, "wikPageDao", mockWikiPageDao);
		ReflectionTestUtils.setField(worker, "workerLogger", mockWorkerLogger);
		
		message = new ChangeMessage();
		message.setChangeType(ChangeType.CREATE);
		message.setObjectEtag("etag1");
		message.setObjectId("one");
		message.setObjectType(ObjectType.ENTITY);
		
		message2 = new ChangeMessage();
		message2.setChangeType(ChangeType.CREATE);
		message2.setObjectEtag("etag2");
		message2.setObjectId("two");
		message2.setObjectType(ObjectType.ENTITY);
	}
	
	@Test
	public void testDelete() throws Exception{
		// create a few delete messages.
		message.setChangeType(ChangeType.DELETE);
		// call under test
		worker.run(mockCallback, message);
		// Delete should be called
		verify(mockSearchDao, times(1)).deleteDocument("one");
		// create should not be called
		verify(mockSearchDao, never()).createOrUpdateSearchDocument(anyListOf(Document.class));
	}
	
	@Test
	public void testCreate() throws Exception{
		Document docOne = new Document();
		docOne.setId("one");
		when(mockDocumentProvider.formulateSearchDocument("one")).thenReturn(docOne);
		Document docTwo = new Document();
		docTwo.setId("two");
		when(mockDocumentProvider.formulateSearchDocument("two")).thenReturn(docTwo);
		
		// Create only occurs if the document exists in the repository
		when(mockDocumentProvider.doesDocumentExist("one", "etag1")).thenReturn(true);
		when(mockDocumentProvider.doesDocumentExist("two", "etag2")).thenReturn(true);
		
		// Create only occurs if it is not already in the search index
		when(mockSearchDao.doesDocumentExist("one", "etag1")).thenReturn(false);
		when(mockSearchDao.doesDocumentExist("two", "etag2")).thenReturn(false);
		
		// call under test
		worker.run(mockCallback, message);
		worker.run(mockCallback, message2);
	
		// Delete should be called
		verify(mockSearchDao, never()).deleteDocuments(anySetOf(String.class));
		// create should be called once
		verify(mockSearchDao, times(1)).createOrUpdateSearchDocument(docOne);
		verify(mockSearchDao, times(1)).createOrUpdateSearchDocument(docTwo);
	}
	
	/**
	 * When the document already exits in the search index with the same etag, we can ignore it.
	 * @throws Exception
	 */
	@Test
	public void testCreateAlreadyInSearchIndex() throws Exception{
		// Create only occurs if the document exists in the repository
		when(mockDocumentProvider.doesDocumentExist("one", "etag1")).thenReturn(true);
		// Create only occurs if it is not already in the search index
		when(mockSearchDao.doesDocumentExist("one", "etag1")).thenReturn(true);

		// call under test
		worker.run(mockCallback, message);
		
		// Delete should be called
		verify(mockSearchDao, never()).deleteDocuments(anySetOf(String.class));
		// create should not be called
		verify(mockSearchDao, never()).createOrUpdateSearchDocument(any(Document.class));
		// We should not call doesDocumentExist() on the repository when it already exists in the search index.
		verify(mockDocumentProvider, never()).doesDocumentExist("one", "etag1");
	}
	
	/**
	 * When the document already exits in the search index with the same etag, we can ignore it.
	 * @throws Exception
	 */
	@Test
	public void testCreateDoesNotExistInReposiroty() throws Exception{
		// Create only occurs if the document exists in the repository
		when(mockDocumentProvider.doesDocumentExist("one", "etag1")).thenReturn(false);
		// Create only occurs if it is not already in the search index
		when(mockSearchDao.doesDocumentExist("one", "etag1")).thenReturn(false);

		// call under test
		worker.run(mockCallback, message);
		
		// Delete should be called
		verify(mockSearchDao, never()).deleteDocuments(anySetOf(String.class));
		// create should not be called
		verify(mockSearchDao, never()).createOrUpdateSearchDocument(anyListOf(Document.class));
		// We should not call doesDocumentExist() one time.
		verify(mockDocumentProvider, times(1)).doesDocumentExist("one", "etag1");
	}
	
	/**
	 * When we get an exception from SearchDao delete, log it using the workerLogger
	 * @throws Exception 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test
	public void testLogDeleteException() throws Exception {
		message.setChangeType(ChangeType.DELETE);
		message2.setChangeType(ChangeType.DELETE);
		// Expected exception in for batch and retry
		Exception eBatch = new RuntimeException("Batch exception");
		Exception eRetry = new RuntimeException("Retry exception");
		// Generate an exception when calling the searchDao
		Mockito.doThrow(eRetry).when(mockSearchDao).deleteDocument("one");
		
		// call under test
		try {
			worker.run(mockCallback, message);
			fail();
		} catch (RecoverableMessageException e) {
			// expected
		}
		worker.run(mockCallback, message2);
		
		// Verify that error logged for "one" and "two" went through
		verify(mockWorkerLogger, times(1)).logWorkerFailure(SearchQueueWorker.class, message, eRetry, true);
		verify(mockSearchDao, times(1)).deleteDocument("two");
	}
	
	/**
	 * When we get an exception from SearchDao create/update, log it using the workerLogger
	 */
	@Test
	public void testLogCreateUpdateException() throws Exception {
		// These docs should exist in repository
		when(mockDocumentProvider.doesDocumentExist("one", "etag1")).thenReturn(true);
		when(mockDocumentProvider.doesDocumentExist("two", "etag2")).thenReturn(true);
		// These docs should not already exist in CloudSearch
		when(mockSearchDao.doesDocumentExist("one", "etag1")).thenReturn(false);
		when(mockSearchDao.doesDocumentExist("two", "etag2")).thenReturn(false);
		// Expected search documents
		Document docOne = new Document();
		docOne.setId("one");
		Document docTwo = new Document();
		docTwo.setId("two");
		when(mockDocumentProvider.formulateSearchDocument("one")).thenReturn(docOne);
		when(mockDocumentProvider.formulateSearchDocument("two")).thenReturn(docTwo);

		
		// Generate an exception when calling the searchDao
		Exception eRetry = new RuntimeException("Retry exception");
		Mockito.doThrow(eRetry).when(mockSearchDao).createOrUpdateSearchDocument(docOne);
		
		// call under test
		try {
			worker.run(mockCallback, message);
			fail();
		} catch (RecoverableMessageException e) {
			// expected
		}
		worker.run(mockCallback, message2);
		
		// Verify that error logged for "one" and "two" went through
		verify(mockWorkerLogger, times(1)).logWorkerFailure(SearchQueueWorker.class, message, eRetry, true);
		verify(mockSearchDao, times(1)).createOrUpdateSearchDocument(docTwo);
	}
}
