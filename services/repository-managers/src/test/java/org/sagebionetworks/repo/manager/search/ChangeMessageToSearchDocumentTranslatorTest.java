package org.sagebionetworks.repo.manager.search;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.search.SearchDao;

@RunWith(MockitoJUnitRunner.class)
public class ChangeMessageToSearchDocumentTranslatorTest{

	@Mock
	private SearchDao mockSearchDao;

	@Mock
	private SearchDocumentDriver mockSearchDocumentDriver;

	@Mock
	private V2WikiPageDao mockWikiPageDao;

	@InjectMocks
	private ChangeMessageToSearchDocumentTranslator translator;

	private ChangeMessage message;
	private ChangeMessage message2;

	@Before
	public void setUp(){
		//documentChangeMessage() test setup
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
	public void testGenerateSearchDocumentIfNecessary_ChangeTypeDelete() throws Exception{
		// create a few delete messages.
		message.setChangeType(ChangeType.DELETE);
		// call under test
		Document document = translator.generateSearchDocumentIfNecessary( message);

		Document expectedDocument = new Document();
		expectedDocument.setId(message.getObjectId());
		expectedDocument.setType(DocumentTypeNames.delete);
		assertEquals(expectedDocument, document);

		// Only a document needs to be generated
		verifyZeroInteractions(mockSearchDao, mockWikiPageDao, mockSearchDocumentDriver);
	}

	@Test
	public void testGenerateSearchDocumentIfNecessary_ChangeTypeCreate() throws Exception{
		Document docOne = new Document();
		docOne.setId("one");
		when(mockSearchDocumentDriver.formulateSearchDocument("one")).thenReturn(docOne);
		Document docTwo = new Document();
		docTwo.setId("two");
		when(mockSearchDocumentDriver.formulateSearchDocument("two")).thenReturn(docTwo);

		// Create only occurs if the document exists in the repository
		when(mockSearchDocumentDriver.doesNodeExist("one", "etag1")).thenReturn(true);
		when(mockSearchDocumentDriver.doesNodeExist("two", "etag2")).thenReturn(true);

		// Create only occurs if it is not already in the search index
		when(mockSearchDao.doesDocumentExist("one", "etag1")).thenReturn(false);
		when(mockSearchDao.doesDocumentExist("two", "etag2")).thenReturn(true);

		// call under test
		translator.generateSearchDocumentIfNecessary(message);
		translator.generateSearchDocumentIfNecessary(message2);

		verify(mockSearchDocumentDriver, times(1)).formulateSearchDocument("one");
		verify(mockSearchDocumentDriver, never()).formulateSearchDocument("two");

		verify(mockSearchDocumentDriver).doesNodeExist("one", "etag1");
		verify(mockSearchDocumentDriver, never()).doesNodeExist("two", "etag2");
	}

	/**
	 * When the document already exits in the search index with the same etag, we can ignore it.
	 */
	@Test
	public void testDocumentChangeMessageChangeTypeCreateAlreadyInSearchIndex() throws IOException {
		// Create only occurs if the document exists in the repository
		when(mockSearchDocumentDriver.doesNodeExist("one", "etag1")).thenReturn(true);
		// Create only occurs if it is not already in the search index
		when(mockSearchDao.doesDocumentExist("one", "etag1")).thenReturn(true);

		// call under test
		translator.generateSearchDocumentIfNecessary(message);

		// create should not be called
		verify(mockSearchDao, never()).createOrUpdateSearchDocument(any(Document.class));
		// We should not call doesNodeExist() on the repository when it already exists in the search index.
		verify(mockSearchDocumentDriver, never()).doesNodeExist("one", "etag1");
	}

	/**
	 * When the document already exits in the search index with the same etag, we can ignore it.
	 */
	@Test
	public void testDocumentChangeMessageChangeTypeCreateDoesNotExistInRepository() throws IOException {
		// Create only occurs if the document exists in the repository
		when(mockSearchDocumentDriver.doesNodeExist("one", "etag1")).thenReturn(false);
		// Create only occurs if it is not already in the search index
		when(mockSearchDao.doesDocumentExist("one", "etag1")).thenReturn(false);

		// call under test
		translator.generateSearchDocumentIfNecessary(message);

		// Delete should be called
		verify(mockSearchDao, never()).deleteDocuments(anySetOf(String.class));
		// create should not be called
		verify(mockSearchDao, never()).createOrUpdateSearchDocument(anyListOf(Document.class));
		// We should not call doesNodeExist() one time.
		verify(mockSearchDocumentDriver, times(1)).doesNodeExist("one", "etag1");
	}

}
