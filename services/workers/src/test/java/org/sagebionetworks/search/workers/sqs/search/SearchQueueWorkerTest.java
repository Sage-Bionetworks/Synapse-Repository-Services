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
import org.sagebionetworks.repo.manager.search.SearchDocumentDriver;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.search.SearchDao;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.model.Message;

public class SearchQueueWorkerTest {
	
	private ProgressCallback<Message> mockCallback;
	private SearchDao mockSearchDao;
	private SearchDocumentDriver mockDocumentProvider;
	private V2WikiPageDao mockWikiPageDao;
	private WorkerLogger mockWorkerLogger;
	private SearchQueueWorker worker;
	
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
	}
	
	@Test
	public void testDelete() throws Exception{
		// create a few delete messages.
		Message message = MessageUtils.buildDeleteEntityMessage("one", "parent1", "1", "handle1");
		// call under test
		worker.run(mockCallback, message);
		// Delete should be called
		verify(mockSearchDao, times(1)).deleteDocument("one");
		// create should not be called
		verify(mockSearchDao, never()).createOrUpdateSearchDocument(anyListOf(Document.class));
	}
	
	@Test
	public void testCreate() throws Exception{
		// create a few delete messages.
		Message message = MessageUtils.buildCreateEntityMessage("one", "parent1", "etag1", "1", "handle1");
		Message message2 = MessageUtils.buildCreateEntityMessage("two", "parent2", "etag2", "1", "handle2");
		
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
		// create a few delete messages.
		Message message = MessageUtils.buildCreateEntityMessage("one", "parent1", "etag1", "1", "handle1");
		
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
		// create a few delete messages.
		Message message = MessageUtils.buildCreateEntityMessage("one", "parent1", "etag1", "1", "handle1");
		
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
		// Create some delete messages
		Message one = MessageUtils.buildDeleteEntityMessage("one", "parent1", "1", "handle1");
		Message two = MessageUtils.buildDeleteEntityMessage("two", "parent2", "2", "handle2");
		// Expected docIds to delete
		// Expected change message to fail
		ChangeMessage cMsg = MessageUtils.extractMessageBody(one);
		// Expected exception in for batch and retry
		Exception eBatch = new RuntimeException("Batch exception");
		Exception eRetry = new RuntimeException("Retry exception");
		// Generate an exception when calling the searchDao
		Mockito.doThrow(eRetry).when(mockSearchDao).deleteDocument("one");
		
		// call under test
		try {
			worker.run(mockCallback, one);
			fail();
		} catch (RecoverableMessageException e) {
			// expected
		}
		worker.run(mockCallback, two);
		
		// Verify that error logged for "one" and "two" went through
		verify(mockWorkerLogger, times(1)).logWorkerFailure(SearchQueueWorker.class, cMsg, eRetry, true);
		verify(mockSearchDao, times(1)).deleteDocument("two");
	}
	
	/**
	 * When we get an exception from SearchDao create/update, log it using the workerLogger
	 */
	@Test
	public void testLogCreateUpdateException() throws Exception {
		// Create some create msgs
		Message one = MessageUtils.buildCreateEntityMessage("one", "parent1", "etag1", "1", "handle1");
		Message two = MessageUtils.buildCreateEntityMessage("two", "parent2", "etag2", "2", "handle2");
		// These docs should exist in repository
		when(mockDocumentProvider.doesDocumentExist("one", "etag1")).thenReturn(true);
		when(mockDocumentProvider.doesDocumentExist("two", "etag2")).thenReturn(true);
		// These docs should not already exist in CloudSearch
		when(mockSearchDao.doesDocumentExist("one", "etag1")).thenReturn(false);
		when(mockSearchDao.doesDocumentExist("two", "etag2")).thenReturn(false);
		// Expected change message to fail
		ChangeMessage cMsg = MessageUtils.extractMessageBody(one);
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
			worker.run(mockCallback, one);
			fail();
		} catch (RecoverableMessageException e) {
			// expected
		}
		worker.run(mockCallback, two);
		
		// Verify that error logged for "one" and "two" went through
		verify(mockWorkerLogger, times(1)).logWorkerFailure(SearchQueueWorker.class, cMsg, eRetry, true);
		verify(mockSearchDao, times(1)).createOrUpdateSearchDocument(docTwo);
	}
}
