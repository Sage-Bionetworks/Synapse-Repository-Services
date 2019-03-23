package org.sagebionetworks.repo.manager.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
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
	private Document docOne;

	@Before
	public void setUp(){
		//documentChangeMessage() test setup
		message = new ChangeMessage();
		message.setChangeType(ChangeType.CREATE);
		message.setObjectEtag("etag1");
		message.setObjectId("one");
		message.setObjectType(ObjectType.ENTITY);

		docOne = new Document();
		docOne.setId("one");
		when(mockSearchDocumentDriver.formulateSearchDocument("one")).thenReturn(docOne);
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

		// Create only occurs if the document exists in the repository
		when(mockSearchDocumentDriver.doesNodeExist("one", "etag1")).thenReturn(true);

		// Create only occurs if it is not already in the search index
		when(mockSearchDao.doesDocumentExist("one", "etag1")).thenReturn(false);

		// call under test
		Document generatedDoc = translator.generateSearchDocumentIfNecessary(message);

		assertEquals(docOne, generatedDoc);

		verify(mockSearchDocumentDriver).formulateSearchDocument("one");

		verify(mockSearchDocumentDriver).doesNodeExist("one", "etag1");
	}

	/**
	 * When the document already exits in the search index with the same etag, we can ignore it.
	 */
	@Test
	public void testGenerateSearchDocumentIfNecessary_ChangeTypeCreateAlreadyInSearchIndex() throws IOException {
		// Create only occurs if the document exists in the repository
		when(mockSearchDocumentDriver.doesNodeExist("one", "etag1")).thenReturn(true);
		// Create only occurs if it is not already in the search index
		when(mockSearchDao.doesDocumentExist("one", "etag1")).thenReturn(true);

		// call under test
		Document generatedDoc = translator.generateSearchDocumentIfNecessary(message);
		assertNull(generatedDoc);

		// We should not call doesNodeExist() on the repository when it already exists in the search index.
		verify(mockSearchDocumentDriver, never()).doesNodeExist("one", "etag1");
	}

	/**
	 * When the document already exits in the search index with the same etag, we can ignore it.
	 */
	@Test
	public void testGenerateSearchDocumentIfNecessary_ChangeTypeCreateDoesNotExistInRepository() throws IOException {
		// Create only occurs if the document exists in the repository
		when(mockSearchDocumentDriver.doesNodeExist("one", "etag1")).thenReturn(false);
		// Create only occurs if it is not already in the search index
		when(mockSearchDao.doesDocumentExist("one", "etag1")).thenReturn(false);

		// call under test
		Document generatedDoc = translator.generateSearchDocumentIfNecessary(message);
		assertNull(generatedDoc);

		// We should not call doesNodeExist() one time.
		verify(mockSearchDocumentDriver).doesNodeExist("one", "etag1");
	}

	@Test
	public void testGenerateSearchDocumentIfNecessary_ObjectTypeIsWikiAndOwnerIsEntity(){
		String wikiOwnerId = "one";

		message.setObjectType(ObjectType.WIKI);
		message.setObjectId("wiki");

		WikiPageKey wikiPageKey = new WikiPageKey();
		wikiPageKey.setOwnerObjectId(wikiOwnerId);
		wikiPageKey.setOwnerObjectType(ObjectType.ENTITY);

		when(mockWikiPageDao.lookupWikiKey("wiki")).thenReturn(wikiPageKey);
		when(mockSearchDao.doesDocumentExist("one", null)).thenReturn(false);

		//method under test
		Document generatedDoc = translator.generateSearchDocumentIfNecessary(message);
		assertEquals(docOne, generatedDoc);

		verify(mockSearchDao).doesDocumentExist("one", null);
		verify(mockSearchDocumentDriver, never()).doesNodeExist(anyString(), anyString());
		verify(mockSearchDocumentDriver).formulateSearchDocument("one");
	}


	@Test
	public void testGenerateSearchDocumentIfNecessary_ObjectTypeIsWikiAndOwnerIsNotEntity(){
		String wikiOwnerId = "one";

		message.setObjectType(ObjectType.WIKI);
		message.setObjectId("wiki");

		WikiPageKey wikiPageKey = new WikiPageKey();
		wikiPageKey.setOwnerObjectId(wikiOwnerId);
		wikiPageKey.setOwnerObjectType(ObjectType.FILE);
		when(mockWikiPageDao.lookupWikiKey("wiki")).thenReturn(wikiPageKey);

		//method under test
		Document generatedDoc = translator.generateSearchDocumentIfNecessary(message);
		assertNull(generatedDoc);

		verifyZeroInteractions(mockSearchDao, mockSearchDocumentDriver);
	}

}
