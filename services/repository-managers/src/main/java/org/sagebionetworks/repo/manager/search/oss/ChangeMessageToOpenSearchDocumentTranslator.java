package org.sagebionetworks.repo.manager.search.oss;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.search.SearchDocumentDriver;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class ChangeMessageToOpenSearchDocumentTranslator {
    private static final Logger log = LogManager.getLogger(ChangeMessageToOpenSearchDocumentTranslator.class.getName());

    private SearchDocumentDriver searchDocumentDriver;

    private V2WikiPageDao wikiPageDao;

    public ChangeMessageToOpenSearchDocumentTranslator(SearchDocumentDriver searchDocumentDriver, V2WikiPageDao wikiPageDao) {
        this.searchDocumentDriver = searchDocumentDriver;
        this.wikiPageDao = wikiPageDao;
    }

    public Optional<Document> generateSearchDocumentIfNecessary(ChangeMessage change) {
        switch (change.getObjectType()) {
            case ENTITY:
                return entityChange(change.getObjectId());
            case WIKI:
                return wikiChange(change.getObjectId());
            default:
                throw new IllegalArgumentException("Unknown change type: " + change.getObjectType());
        }
    }

    /**
     * Create a change document for the given wiki.
     *
     * @param entityId
     * @return
     */
    Optional<Document> entityChange(String entityId) {
        if (!searchDocumentDriver.doesEntityExistInRepository(entityId)) {
            return createDeleteDocument(entityId);
        } else {
            return Optional.of(searchDocumentDriver.formulateSearchDocument(entityId));
        }
    }


    /**
     * Wiki changes are converted into entity changes.
     *
     * @param wikiId
     * @return
     */
    Optional<Document> wikiChange(String wikiId) {
        // Lookup the owner of the page
        try {
            WikiPageKey key = wikiPageDao.lookupWikiKey(wikiId);
            // If the owner of the wiki is a an entity then pass along the
            // message.
            if (ObjectType.ENTITY == key.getOwnerObjectType()) {
                return entityChange(key.getOwnerObjectId());
            }
        } catch (NotFoundException e) {
            // Nothing to do if the wiki does not exist
            log.info("Wiki not found for id: " + wikiId + " Message: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Create a document to be deleted.
     *
     * @param entityId
     * @return
     */
    Optional<Document> createDeleteDocument(String entityId) {
        Document document = new Document();
        document.setType(DocumentTypeNames.delete);
        document.setId(entityId);
        return Optional.of(document);
    }
}
