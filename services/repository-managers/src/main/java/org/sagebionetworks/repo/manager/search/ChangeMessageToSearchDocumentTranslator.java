package org.sagebionetworks.repo.manager.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.search.CloudSearchDocumentLogRecord;
import org.sagebionetworks.search.CloudSearchLogger;
import org.sagebionetworks.search.DocumentAction;
import org.sagebionetworks.search.SearchDao;
import org.springframework.beans.factory.annotation.Autowired;

public class ChangeMessageToSearchDocumentTranslator {
	private static final Logger log = LogManager.getLogger(ChangeMessageToSearchDocumentTranslator.class.getName());

	@Autowired
	SearchDocumentDriver searchDocumentDriver;

	@Autowired
	V2WikiPageDao wikiPageDao;

	@Autowired
	SearchDao searchDao;
	
	@Autowired
	CloudSearchLogger recordLogger;

	Document generateSearchDocumentIfNecessary(ChangeMessage change) {
		// start a log record for this message.
		CloudSearchDocumentLogRecord record = recordLogger.startRecordForChangeMessage(change);
		switch (change.getObjectType()) {
		case ENTITY:
			return entityChange(change.getObjectId(), record);
		case WIKI:
			return wikiChange(change.getObjectId(), record);
		default:
			throw new IllegalArgumentException("Unknown change type: " + change.getChangeType());
		}
	}

	/**
	 * Wiki changes are converted into entity changes.
	 * @param wikiId
	 * @param record
	 * @return
	 */
	Document wikiChange(String wikiId, CloudSearchDocumentLogRecord record) {
		// Lookup the owner of the page
		try {
			WikiPageKey key = wikiPageDao.lookupWikiKey(wikiId);
			// If the owner of the wiki is a an entity then pass along the
			// message.
			if (ObjectType.ENTITY == key.getOwnerObjectType()) {
				record.withWikiOwner(key.getOwnerObjectId());
				return entityChange(key.getOwnerObjectId(), record);
			}
		} catch (NotFoundException e) {
			// Nothing to do if the wiki does not exist
			log.info("Wiki not found for id: " + wikiId + " Message: " + e.getMessage());
		}
		// this change will be ignored.
		record.withAction(DocumentAction.IGNORE);
		return null;
	}

	/**
	 * Create a change document for the given wiki.
	 * 
	 * @param entityId
	 * @param record
	 * @return
	 */
	Document entityChange(String entityId, CloudSearchDocumentLogRecord record) {
		if(!searchDocumentDriver.doesEntityExistInRepository(entityId)) {
			record.withAction(DocumentAction.DELETE);
			return createDeleteDocument(entityId);
		}else {
			record.withAction(DocumentAction.CREATE_OR_UPDATE);
			return searchDocumentDriver.formulateSearchDocument(entityId);
		}
	}

	/**
	 * Create a document to be deleted.
	 * @param entityId
	 * @return
	 */
	Document createDeleteDocument(String entityId) {
		Document document = new Document();
		document.setType(DocumentTypeNames.delete);
		document.setId(entityId);
		return document;
	}
}
