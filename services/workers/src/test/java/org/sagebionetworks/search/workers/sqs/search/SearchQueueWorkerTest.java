package org.sagebionetworks.search.workers.sqs.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.manager.search.SearchDocumentDriver;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.search.SearchDao;

import com.amazonaws.services.sqs.model.Message;
import com.google.common.collect.Sets;

public class SearchQueueWorkerTest {
	
	private SearchDao mockSearchDao;
	private SearchDocumentDriver mockDocumentProvider;
	private V2WikiPageDao mockWikiPageDao;
	private List<Message> messageList;
	private WorkerLogger mockWorkerLogger;
	
	@Before
	public void before(){
		mockSearchDao = Mockito.mock(SearchDao.class);
		mockDocumentProvider = Mockito.mock(SearchDocumentDriver.class);
		mockWikiPageDao = Mockito.mock(V2WikiPageDao.class);
		messageList = new LinkedList<Message>();
		mockWorkerLogger = Mockito.mock(WorkerLogger.class);
		when(mockSearchDao.isSearchEnabled()).thenReturn(true);
	}
	
	@Test
	public void testDelete() throws Exception{
		// create a few delete messages.
		messageList.add(MessageUtils.buildDeleteEntityMessage("one", "parent1", "1", "handle1"));
		messageList.add(MessageUtils.buildDeleteEntityMessage("two", "parent2", "2", "handle2"));
		
		SearchQueueWorker worker = new SearchQueueWorker(mockSearchDao, mockDocumentProvider, messageList, mockWikiPageDao, mockWorkerLogger);
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messageList.size(), results.size());
		// order is not the same
		assertEquals(Sets.newHashSet(messageList), Sets.newHashSet(results));
		
		HashSet<String> deleteSet = new HashSet<String>();
		deleteSet.add("one");
		deleteSet.add("two");
		// Delete should be called
		verify(mockSearchDao, times(1)).deleteDocuments(deleteSet);
		// create should not be called
		verify(mockSearchDao, never()).createOrUpdateSearchDocument(anyListOf(Document.class));
	}
	
	@Test
	public void testCreate() throws Exception{
		// create a few delete messages.
		messageList.add(MessageUtils.buildCreateEntityMessage("one", "parent1", "etag1", "1", "handle1"));
		messageList.add(MessageUtils.buildCreateEntityMessage("two", "parent2", "etag2", "1", "handle2"));
		
		Document docOne = new Document();
		docOne.setId("one");
		when(mockDocumentProvider.formulateSearchDocument("one")).thenReturn(docOne);
		Document docTwo = new Document();
		docTwo.setId("two");
		when(mockDocumentProvider.formulateSearchDocument("two")).thenReturn(docTwo);
		List<Document> expectedDocs = new LinkedList<Document>();
		expectedDocs.add(docOne);
		expectedDocs.add(docTwo);
		
		// Create only occurs if the document exists in the repository
		when(mockDocumentProvider.doesDocumentExist("one", "etag1")).thenReturn(true);
		when(mockDocumentProvider.doesDocumentExist("two", "etag2")).thenReturn(true);
		
		// Create only occurs if it is not already in the search index
		when(mockSearchDao.doesDocumentExist("one", "etag1")).thenReturn(false);
		when(mockSearchDao.doesDocumentExist("two", "etag2")).thenReturn(false);
		
		SearchQueueWorker worker = new SearchQueueWorker(mockSearchDao, mockDocumentProvider, messageList, mockWikiPageDao, mockWorkerLogger);
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messageList.size(), results.size());
		// order is not the same
		assertEquals(Sets.newHashSet(messageList), Sets.newHashSet(results));
	
		// Delete should be called
		verify(mockSearchDao, never()).deleteDocuments(anySetOf(String.class));
		// create should be called once
		verify(mockSearchDao, times(1)).createOrUpdateSearchDocument(expectedDocs);
	}
	
	/**
	 * When the document already exits in the search index with the same etag, we can ignore it.
	 * @throws Exception
	 */
	@Test
	public void testCreateAlreadyInSearchIndex() throws Exception{
		// create a few delete messages.
		messageList.add(MessageUtils.buildCreateEntityMessage("one", "parent1", "etag1", "1", "handle1"));
		
		// Create only occurs if the document exists in the repository
		when(mockDocumentProvider.doesDocumentExist("one", "etag1")).thenReturn(true);
		// Create only occurs if it is not already in the search index
		when(mockSearchDao.doesDocumentExist("one", "etag1")).thenReturn(true);

		SearchQueueWorker worker = new SearchQueueWorker(mockSearchDao, mockDocumentProvider, messageList, mockWikiPageDao, mockWorkerLogger);
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messageList, results);
		
		// Delete should be called
		verify(mockSearchDao, never()).deleteDocuments(anySetOf(String.class));
		// create should not be called
		verify(mockSearchDao, never()).createOrUpdateSearchDocument(anyListOf(Document.class));
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
		messageList.add(MessageUtils.buildCreateEntityMessage("one", "parent1", "etag1", "1", "handle1"));
		
		// Create only occurs if the document exists in the repository
		when(mockDocumentProvider.doesDocumentExist("one", "etag1")).thenReturn(false);
		// Create only occurs if it is not already in the search index
		when(mockSearchDao.doesDocumentExist("one", "etag1")).thenReturn(false);

		SearchQueueWorker worker = new SearchQueueWorker(mockSearchDao, mockDocumentProvider, messageList, mockWikiPageDao, mockWorkerLogger);
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messageList, results);
		
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
		messageList.add(MessageUtils.buildDeleteEntityMessage("one", "parent1", "1", "handle1"));
		messageList.add(MessageUtils.buildDeleteEntityMessage("two", "parent2", "2", "handle2"));
		// Expected docIds to delete
		Set<String> docIdsToDelete = new HashSet<String>();
		docIdsToDelete.add("one");
		docIdsToDelete.add("two");
		// Expected change message to fail
		ChangeMessage cMsg = MessageUtils.extractMessageBody(messageList.get(0));
		// Expected exception in for batch and retry
		Exception eBatch = new RuntimeException("Batch exception");
		Exception eRetry = new RuntimeException("Retry exception");
		// Generate an exception when calling the searchDao
		Mockito.doThrow(eBatch).when(mockSearchDao).deleteDocuments(docIdsToDelete);
		Mockito.doThrow(eRetry).when(mockSearchDao).deleteDocument("one");
		
		SearchQueueWorker worker = new SearchQueueWorker(mockSearchDao, mockDocumentProvider, messageList, mockWikiPageDao, mockWorkerLogger);
		List<Message> results = worker.call();
		assertEquals(1, results.size());
		assertEquals("2", results.get(0).getMessageId());
		
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
		messageList.add(MessageUtils.buildCreateEntityMessage("one", "parent1", "etag1", "1", "handle1"));
		messageList.add(MessageUtils.buildCreateEntityMessage("two", "parent2", "etag2", "2", "handle2"));
		// These docs should exist in repository
		when(mockDocumentProvider.doesDocumentExist("one", "etag1")).thenReturn(true);
		when(mockDocumentProvider.doesDocumentExist("two", "etag2")).thenReturn(true);
		// These docs should not already exist in CloudSearch
		when(mockSearchDao.doesDocumentExist("one", "etag1")).thenReturn(false);
		when(mockSearchDao.doesDocumentExist("two", "etag2")).thenReturn(false);
		// Expected change message to fail
		ChangeMessage cMsg = MessageUtils.extractMessageBody(messageList.get(0));
		// Expected search documents
		Document docOne = new Document();
		docOne.setId("one");
		Document docTwo = new Document();
		docTwo.setId("two");
		when(mockDocumentProvider.formulateSearchDocument("one")).thenReturn(docOne);
		when(mockDocumentProvider.formulateSearchDocument("two")).thenReturn(docTwo);
		//
		List<Document> docsToCreate = new LinkedList<Document>();
		docsToCreate.add(docOne);
		docsToCreate.add(docTwo);
		
		// Generate an exception when calling the searchDao
		Exception eBatch = new RuntimeException("Batch exception");
		Exception eRetry = new RuntimeException("Retry exception");
		Mockito.doThrow(eBatch).when(mockSearchDao).createOrUpdateSearchDocument(docsToCreate);
		Mockito.doThrow(eRetry).when(mockSearchDao).createOrUpdateSearchDocument(docOne);
		
		SearchQueueWorker worker = new SearchQueueWorker(mockSearchDao, mockDocumentProvider, messageList, mockWikiPageDao, mockWorkerLogger);
		List<Message> results = worker.call();
		assertEquals(1, results.size());
		assertEquals("2", results.get(0).getMessageId());
		
		// Verify that error logged for "one" and "two" went through
		verify(mockWorkerLogger, times(1)).logWorkerFailure(SearchQueueWorker.class, cMsg, eRetry, true);
		verify(mockSearchDao, times(1)).createOrUpdateSearchDocument(docTwo);
	}
}
