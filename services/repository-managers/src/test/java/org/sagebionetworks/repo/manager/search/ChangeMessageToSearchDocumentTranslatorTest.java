package org.sagebionetworks.repo.manager.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dao.WikiPageKeyHelper;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.search.CloudSearchDocumentLogRecord;
import org.sagebionetworks.search.CloudSearchLogger;
import org.sagebionetworks.search.DocumentAction;
import org.sagebionetworks.search.SearchDao;

@RunWith(MockitoJUnitRunner.class)
public class ChangeMessageToSearchDocumentTranslatorTest{

	@Mock
	private SearchDao mockSearchDao;

	@Mock
	private SearchDocumentDriver mockSearchDocumentDriver;

	@Mock
	private V2WikiPageDao mockWikiPageDao;
	
	@Mock
	private CloudSearchDocumentLogRecord mocKRecord;
	
	@Mock
	private CloudSearchLogger mockRecordLogger;

	@InjectMocks
	private ChangeMessageToSearchDocumentTranslator translator;

	private ChangeMessage message;
	private Document docOne;
	private long changeNumber;
	private String synapseId;
	private String etag;
	String wikiId;
	WikiPageKey wikiKey;

	@Before
	public void setUp(){
		changeNumber = 111L;
		synapseId = "112233";
		etag = "etag1";


		//documentChangeMessage() test setup
		message = new ChangeMessage();
		message.setChangeType(ChangeType.CREATE);
		message.setObjectId(synapseId);
		message.setObjectType(ObjectType.ENTITY);
		message.setChangeNumber(changeNumber);


		docOne = new Document();
		docOne.setId(synapseId);
		when(mockSearchDocumentDriver.formulateSearchDocument(synapseId)).thenReturn(docOne);
		
		when(mockSearchDocumentDriver.doesEntityExistInRepository(synapseId)).thenReturn(true);
		when(mockSearchDao.doesDocumentExistInSearchIndex(synapseId, etag)).thenReturn(false);
		
		wikiId = "987";
		wikiKey = WikiPageKeyHelper.createWikiPageKey(synapseId, ObjectType.ENTITY, wikiId);
		when(mockWikiPageDao.lookupWikiKey(wikiId)).thenReturn(wikiKey);
		when(mockRecordLogger.startRecordForChangeMessage(any(ChangeMessage.class))).thenReturn(mocKRecord);
	}
	
	@Test
	public void testEntityChange() {
		// call under test
		Document doc = translator.entityChange(synapseId, mocKRecord);
		assertEquals(docOne, doc);
		verify(mocKRecord).withAction(DocumentAction.CREATE_OR_UPDATE);
		verify(mockSearchDocumentDriver).formulateSearchDocument(synapseId);
	}
	
	@Test
	public void testEntityChangeEntityDoesNotExist() {
		// documents that do not exist do not have an etag.
		when(mockSearchDocumentDriver.doesEntityExistInRepository(synapseId)).thenReturn(false);
		// call under test
		Document doc = translator.entityChange(synapseId, mocKRecord);
		Document expectedDocument = new Document();
		expectedDocument.setId(message.getObjectId());
		expectedDocument.setType(DocumentTypeNames.delete);
		assertEquals(expectedDocument, doc);
		verify(mocKRecord).withAction(DocumentAction.DELETE);
		verify(mockSearchDocumentDriver, never()).formulateSearchDocument(anyString());
	}
	
	@Test
	public void testWikiChange() {
		//when a wiki is updated, the entity to which it is associated may still have the same etag
		when(mockSearchDao.doesDocumentExistInSearchIndex(synapseId, etag)).thenReturn(true);

		String wikiId = "987";
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(synapseId, ObjectType.ENTITY, wikiId);
		when(mockWikiPageDao.lookupWikiKey(wikiId)).thenReturn(key);
		
		// call under test
		Document doc = translator.wikiChange(wikiId, mocKRecord);
		assertEquals(docOne, doc);
		verify(mockWikiPageDao).lookupWikiKey(wikiId);
		verify(mocKRecord).withAction(DocumentAction.CREATE_OR_UPDATE);
		verify(mocKRecord).withWikiOwner(synapseId);
		verify(mockSearchDocumentDriver).formulateSearchDocument(synapseId);
	}

	@Test
	public void testWikiChangeWikiNotFound() {
		String wikiId = "987";
		when(mockWikiPageDao.lookupWikiKey(wikiId)).thenThrow(new NotFoundException());

		// call under test
		Document doc = translator.wikiChange(wikiId, mocKRecord);
		assertNull(doc);
		verify(mocKRecord).withAction(DocumentAction.IGNORE);
		verify(mockSearchDocumentDriver, never()).formulateSearchDocument(anyString());
	}
	
	@Test
	public void testGenerateSearchDocumentIfNecessaryEntity() {
		// call under test
		Document doc = translator.generateSearchDocumentIfNecessary(message);
		assertEquals(docOne, doc);
		verify(mockRecordLogger).startRecordForChangeMessage(message);
		verify(mockWikiPageDao, never()).lockForUpdate(anyString());
	}
	
	@Test
	public void testGenerateSearchDocumentIfNecessaryWiki() {
		String wikiId = "987";
		WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(synapseId, ObjectType.ENTITY, wikiId);
		when(mockWikiPageDao.lookupWikiKey(wikiId)).thenReturn(key);
		message.setObjectId(wikiId);
		message.setObjectType(ObjectType.WIKI);
		// call under test
		Document doc = translator.generateSearchDocumentIfNecessary(message);
		assertEquals(docOne, doc);
		verify(mockRecordLogger).startRecordForChangeMessage(message);
		verify(mockWikiPageDao).lookupWikiKey(wikiId);
	}
}
