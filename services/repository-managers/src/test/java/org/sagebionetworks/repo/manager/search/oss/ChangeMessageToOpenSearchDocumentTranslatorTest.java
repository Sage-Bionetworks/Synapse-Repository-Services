package org.sagebionetworks.repo.manager.search.oss;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.search.SearchDocumentDriver;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.web.NotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ChangeMessageToOpenSearchDocumentTranslatorTest {

    @InjectMocks
    ChangeMessageToOpenSearchDocumentTranslator messageToOpenSearchDocumentTranslator;

    @Mock
    private SearchDocumentDriver mockSearchDocumentDriver;

    @Mock
    private V2WikiPageDao mockWikiPageDao;

    private ChangeMessage changeMessage;
    private Document document;

    @BeforeEach
    public void before() {
        String synId = "syn123";
        changeMessage = new ChangeMessage().setChangeType(ChangeType.CREATE).setObjectType(ObjectType.ENTITY).setObjectId(synId);
        document = new Document().setType(DocumentTypeNames.add).setId(synId)
                .setFields(new DocumentFields().setName("test").setEtag("abc"));
    }

    @Test
    public void testEntityADDGenerateSearchDocumentIfNecessary() {
        when(mockSearchDocumentDriver.doesEntityExistInRepository(any())).thenReturn(true);
        when(mockSearchDocumentDriver.formulateSearchDocument(anyString())).thenReturn(document);

        // call under test
        Document doc = messageToOpenSearchDocumentTranslator.generateSearchDocumentIfNecessary(changeMessage).get();
        assertEquals(document, doc);
    }

    @Test
    public void testEntityDeleteGenerateSearchDocumentIfNecessary() {
        document.setType(DocumentTypeNames.delete);
        when(mockSearchDocumentDriver.doesEntityExistInRepository(any())).thenReturn(false);

        // call under test
        Document doc = messageToOpenSearchDocumentTranslator.generateSearchDocumentIfNecessary(changeMessage).get();
        assertEquals(document.getId(), doc.getId());
        assertEquals(DocumentTypeNames.delete, doc.getType());
        assertNull(doc.getFields());
    }

    @Test
    public void testWikiADDGenerateSearchDocumentIfNecessary() {
        changeMessage.setObjectType(ObjectType.WIKI);
        when(mockWikiPageDao.lookupWikiKey(anyString())).thenReturn(new WikiPageKey()
                .setOwnerObjectId(document.getId()).setOwnerObjectType(ObjectType.ENTITY));

        when(mockSearchDocumentDriver.doesEntityExistInRepository(any())).thenReturn(true);
        when(mockSearchDocumentDriver.formulateSearchDocument(anyString())).thenReturn(document);

        // call under test
        Document doc = messageToOpenSearchDocumentTranslator.generateSearchDocumentIfNecessary(changeMessage).get();
        assertEquals(document, doc);
    }

    @Test
    public void testWikiDeleteGenerateSearchDocumentIfNecessary() {
        changeMessage.setObjectType(ObjectType.WIKI);
        document.setType(DocumentTypeNames.delete);

        when(mockWikiPageDao.lookupWikiKey(anyString())).thenReturn(new WikiPageKey()
                .setOwnerObjectId(document.getId()).setOwnerObjectType(ObjectType.ENTITY));

        when(mockSearchDocumentDriver.doesEntityExistInRepository(any())).thenReturn(false);

        // call under test
        Document doc = messageToOpenSearchDocumentTranslator.generateSearchDocumentIfNecessary(changeMessage).get();
        assertEquals(document.getId(), doc.getId());
        assertEquals(DocumentTypeNames.delete, doc.getType());
        assertNull(doc.getFields());
    }

    @Test
    public void testWikiGenerateSearchDocumentIfNecessaryWithoutOwnerObjectTypeEntity() {
        changeMessage.setObjectType(ObjectType.WIKI);
        document.setType(DocumentTypeNames.delete);

        when(mockWikiPageDao.lookupWikiKey(anyString())).thenReturn(new WikiPageKey()
                .setOwnerObjectId(document.getId()).setOwnerObjectType(ObjectType.WIKI));

        // call under test
        assertTrue(messageToOpenSearchDocumentTranslator.generateSearchDocumentIfNecessary(changeMessage).isEmpty());
    }

    @Test
    public void testWikiGenerateSearchDocumentIfNecessaryGenerateEmptyDocOnNotfoundException() {
        changeMessage.setObjectType(ObjectType.WIKI);
        document.setType(DocumentTypeNames.delete);

        when(mockWikiPageDao.lookupWikiKey(anyString())).thenThrow(NotFoundException.class);

        // call under test
        assertTrue(messageToOpenSearchDocumentTranslator.generateSearchDocumentIfNecessary(changeMessage).isEmpty());
    }

    @Test
    public void testGenerateSearchDocumentIfNecessaryWithWrongType() {
        changeMessage.setObjectType(ObjectType.ENTITY_VIEW);

        String message = assertThrows(IllegalArgumentException.class, () -> {
            //call under test
            assertNull(messageToOpenSearchDocumentTranslator.generateSearchDocumentIfNecessary(changeMessage));
        }).getMessage();

        assertEquals("Unknown change type: ENTITY_VIEW", message);
    }

}
