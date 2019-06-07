package org.sagebionetworks.repo.manager.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
import org.sagebionetworks.search.CloudSearchDocumentGenerationAwsKinesisLogRecord;
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
	private long changeNumber;
	private String synapseId;
	private String etag;

	@Before
	public void setUp(){
		changeNumber = 111L;
		synapseId = "syn112233";
		etag = "etag1";


		//documentChangeMessage() test setup
		message = new ChangeMessage();
		message.setChangeType(ChangeType.CREATE);
		message.setObjectEtag(etag);
		message.setObjectId(synapseId);
		message.setObjectType(ObjectType.ENTITY);
		message.setChangeNumber(changeNumber);


		docOne = new Document();
		docOne.setId(synapseId);
		when(mockSearchDocumentDriver.formulateSearchDocument(synapseId)).thenReturn(docOne);

		//clear the threadlocal log record map since each test will add to it
		ChangeMessageToSearchDocumentTranslator.threadLocalRecordList.get().clear();
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
		verifyThreadLocalMap(ChangeType.DELETE, null, etag);
	}

	@Test
	public void testGenerateSearchDocumentIfNecessary_ChangeTypeCreate() throws Exception{

		// Create only occurs if the document exists in the repository
		when(mockSearchDocumentDriver.doesNodeExist(synapseId, etag)).thenReturn(true);

		// Create only occurs if it is not already in the search index
		when(mockSearchDao.doesDocumentExist(synapseId, etag)).thenReturn(false);

		// call under test
		Document generatedDoc = translator.generateSearchDocumentIfNecessary(message);

		assertEquals(docOne, generatedDoc);

		verify(mockSearchDocumentDriver).formulateSearchDocument(synapseId);

		verify(mockSearchDocumentDriver).doesNodeExist(synapseId, etag);
		verifyThreadLocalMap(ChangeType.CREATE, false, etag);
	}
	@Test
	public void testGenerateSearchDocumentIfNecessary_ChangeTypeUpdate() throws Exception{
		message.setChangeType(ChangeType.UPDATE);

		// Create only occurs if the document exists in the repository
		when(mockSearchDocumentDriver.doesNodeExist(synapseId, etag)).thenReturn(true);

		// Create only occurs if it is not already in the search index
		when(mockSearchDao.doesDocumentExist(synapseId, etag)).thenReturn(false);

		// call under test
		Document generatedDoc = translator.generateSearchDocumentIfNecessary(message);

		assertEquals(docOne, generatedDoc);

		verify(mockSearchDocumentDriver).formulateSearchDocument(synapseId);

		verify(mockSearchDocumentDriver).doesNodeExist(synapseId, etag);
		verifyThreadLocalMap(ChangeType.UPDATE, false, etag);
	}


	/**
	 * When the document already exits in the search index with the same etag, we can ignore it.
	 */
	@Test
	public void testGenerateSearchDocumentIfNecessary_ChangeTypeCreateAlreadyInSearchIndex() throws IOException {
		// Create only occurs if the document exists in the repository
		when(mockSearchDocumentDriver.doesNodeExist(synapseId, etag)).thenReturn(true);
		// Create only occurs if it is not already in the search index
		when(mockSearchDao.doesDocumentExist(synapseId, etag)).thenReturn(true);

		// call under test
		Document generatedDoc = translator.generateSearchDocumentIfNecessary(message);
		assertNull(generatedDoc);

		// We should not call doesNodeExist() on the repository when it already exists in the search index.
		verify(mockSearchDocumentDriver, never()).doesNodeExist(synapseId, etag);
		verifyThreadLocalMap(ChangeType.CREATE, true, etag);
	}

	/**
	 * When the document already exits in the search index with the same etag, we can ignore it.
	 */
	@Test
	public void testGenerateSearchDocumentIfNecessary_ChangeTypeCreateDoesNotExistInRepository() throws IOException {
		// Create only occurs if the document exists in the repository
		when(mockSearchDocumentDriver.doesNodeExist(synapseId, etag)).thenReturn(false);
		// Create only occurs if it is not already in the search index
		when(mockSearchDao.doesDocumentExist(synapseId, etag)).thenReturn(false);

		// call under test
		Document generatedDoc = translator.generateSearchDocumentIfNecessary(message);
		assertNull(generatedDoc);

		// We should not call doesNodeExist() one time.
		verify(mockSearchDocumentDriver).doesNodeExist(synapseId, etag);
		verifyThreadLocalMap(ChangeType.CREATE, false, etag);
	}

	@Test
	public void testGenerateSearchDocumentIfNecessary_ObjectTypeIsWikiAndOwnerIsEntity(){
		String wikiOwnerId = synapseId;

		message.setObjectType(ObjectType.WIKI);
		message.setObjectId("wiki");

		WikiPageKey wikiPageKey = new WikiPageKey();
		wikiPageKey.setOwnerObjectId(wikiOwnerId);
		wikiPageKey.setOwnerObjectType(ObjectType.ENTITY);

		when(mockWikiPageDao.lookupWikiKey("wiki")).thenReturn(wikiPageKey);
		when(mockSearchDao.doesDocumentExist(synapseId, null)).thenReturn(false);

		//method under test
		Document generatedDoc = translator.generateSearchDocumentIfNecessary(message);
		assertEquals(docOne, generatedDoc);

		verify(mockSearchDao).doesDocumentExist(synapseId, null);
		verify(mockSearchDocumentDriver, never()).doesNodeExist(anyString(), anyString());
		verify(mockSearchDocumentDriver).formulateSearchDocument(synapseId);

		verifyThreadLocalMap(ChangeType.UPDATE, false, null);
	}


	@Test
	public void testGenerateSearchDocumentIfNecessary_ObjectTypeIsWikiAndOwnerIsNotEntity(){
		String wikiOwnerId = synapseId;

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
		assertTrue(ChangeMessageToSearchDocumentTranslator.threadLocalRecordList.get().isEmpty());
	}

	private void verifyThreadLocalMap(ChangeType changeType, Boolean doesDocumentExist, String etag){
		List<CloudSearchDocumentGenerationAwsKinesisLogRecord> threadLocalMap = ChangeMessageToSearchDocumentTranslator.threadLocalRecordList.get();
		assertEquals(1, threadLocalMap.size());
		CloudSearchDocumentGenerationAwsKinesisLogRecord record = threadLocalMap.get(0);
		assertEquals((Long) changeNumber, record.getChangeNumber());
		assertEquals(synapseId, record.getSynapseId());
		assertEquals(etag, record.getEtag());
		assertEquals(changeType, changeType);
		assertEquals(doesDocumentExist, record.isExistsOnIndex());
	}
}
