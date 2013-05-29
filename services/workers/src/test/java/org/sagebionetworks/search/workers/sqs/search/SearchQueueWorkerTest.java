package org.sagebionetworks.search.workers.sqs.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.manager.search.SearchDocumentDriver;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.search.SearchDao;

import com.amazonaws.services.sqs.model.Message;

public class SearchQueueWorkerTest {
	
	private SearchDao mockSeachDao;
	private SearchDocumentDriver mockDocumentProvider;
	private WikiPageDao mockWikiPageDao;
	private List<Message> messageList;
	
	@Before
	public void before(){
		mockSeachDao = Mockito.mock(SearchDao.class);
		mockDocumentProvider = Mockito.mock(SearchDocumentDriver.class);
		mockWikiPageDao = Mockito.mock(WikiPageDao.class);
		messageList = new LinkedList<Message>();
	}
	
	@Test
	public void testDelete() throws Exception{
		// create a few delete messages.
		messageList.add(MessageUtils.buildDeleteEntityMessage("one", "parent1", "1", "handle1"));
		messageList.add(MessageUtils.buildDeleteEntityMessage("two", "parent2", "2", "handle2"));
		
		SearchQueueWorker worker = new SearchQueueWorker(mockSeachDao, mockDocumentProvider, messageList, mockWikiPageDao);
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messageList, results);
		
		HashSet<String> deleteSet = new HashSet<String>();
		deleteSet.add("one");
		deleteSet.add("two");
		// Delete should be called
		verify(mockSeachDao, times(1)).deleteDocuments(deleteSet);
		// create should not be called
		verify(mockSeachDao, never()).createOrUpdateSearchDocument(any(List.class));
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
		when(mockSeachDao.doesDocumentExist("one", "etag1")).thenReturn(false);
		when(mockSeachDao.doesDocumentExist("two", "etag2")).thenReturn(false);
		
		SearchQueueWorker worker = new SearchQueueWorker(mockSeachDao, mockDocumentProvider, messageList, mockWikiPageDao);
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messageList, results);
	
		// Delete should be called
		verify(mockSeachDao, never()).deleteDocuments(any(Set.class));
		// create should be called once
		verify(mockSeachDao, times(1)).createOrUpdateSearchDocument(expectedDocs);
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
		when(mockSeachDao.doesDocumentExist("one", "etag1")).thenReturn(true);

		SearchQueueWorker worker = new SearchQueueWorker(mockSeachDao, mockDocumentProvider, messageList, mockWikiPageDao);
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messageList, results);
		
		// Delete should be called
		verify(mockSeachDao, never()).deleteDocuments(any(Set.class));
		// create should not be called
		verify(mockSeachDao, never()).createOrUpdateSearchDocument(any(List.class));
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
		when(mockSeachDao.doesDocumentExist("one", "etag1")).thenReturn(false);

		SearchQueueWorker worker = new SearchQueueWorker(mockSeachDao, mockDocumentProvider, messageList, mockWikiPageDao);
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messageList, results);
		
		// Delete should be called
		verify(mockSeachDao, never()).deleteDocuments(any(Set.class));
		// create should not be called
		verify(mockSeachDao, never()).createOrUpdateSearchDocument(any(List.class));
		// We should not call doesDocumentExist() one time.
		verify(mockDocumentProvider, times(1)).doesDocumentExist("one", "etag1");
	}
}
