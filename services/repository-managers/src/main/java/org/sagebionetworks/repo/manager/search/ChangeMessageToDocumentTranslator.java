package org.sagebionetworks.repo.manager.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.search.SearchDao;
import org.springframework.beans.factory.annotation.Autowired;

public class ChangeMessageToDocumentTranslator {
	private static final Logger log = LogManager.getLogger(ChangeMessageToDocumentTranslator.class.getName());


	@Autowired
	SearchDocumentDriver searchDocumentDriver;

	@Autowired
	V2WikiPageDao wikiPageDao;

	@Autowired
	SearchDao searchDao;

	Document generateSearchDocumentIfNecessary(ChangeMessage change) {
		// We only care about entity messages as this time
		if (ObjectType.ENTITY == change.getObjectType()) {
			// Is this a create or update
			if (ChangeType.CREATE == change.getChangeType()
					|| ChangeType.UPDATE == change.getChangeType()) {
				return createUpdateDocument(change.getObjectId(), change.getObjectEtag());
			} else if (ChangeType.DELETE == change.getChangeType()) {
				return createDeleteDocument(change.getObjectId());
			} else {
				throw new IllegalArgumentException("Unknown change type: "
						+ change.getChangeType());
			}
		}
		// Is this a wikipage?
		if (ObjectType.WIKI == change.getObjectType()) {
			// Lookup the owner of the page
			try {
				WikiPageKey key = wikiPageDao
						.lookupWikiKey(change.getObjectId());
				// If the owner of the wiki is a an entity then pass along the
				// message.
				if (ObjectType.ENTITY == key.getOwnerObjectType()) {
					// We need the current document etag
					ChangeMessage newMessage = new ChangeMessage();
					newMessage.setChangeType(ChangeType.UPDATE);
					newMessage.setObjectId(key.getOwnerObjectId());
					newMessage.setObjectType(ObjectType.ENTITY);
					newMessage.setObjectEtag(null);
					return createUpdateDocument(change.getObjectId(), change.getObjectEtag());
				}
			} catch (NotFoundException e) {
				// Nothing to do if the wiki does not exist
				log.debug("Wiki not found for id: " + change.getObjectId()
						+ " Message:" + e.getMessage());
			}
		}
		return null;
	}

	private Document createUpdateDocument(String entityId, String entityEtag) {
		// We want to ignore this message if a document with this ID and Etag
		// already exists in the search index.
		if (!searchDao.doesDocumentExist(entityId, entityEtag)) {
			// We want to ignore this message if a document with this ID and
			// Etag are not in the repository as it is an
			// old message.
			if (entityEtag == null || searchDocumentDriver.doesNodeExist( entityId, entityEtag)) {
				try {
					return searchDocumentDriver.formulateSearchDocument(entityId);
				} catch (NotFoundException e) {
					// There is nothing to do if it does not exist
					log.debug("Node not found for id: "
							+ entityId + " Message:"
							+ e.getMessage());
				}
			}
		}
		return null;
	}


	private Document createDeleteDocument(String entityId) {
		Document document = new Document();
		document.setType(DocumentTypeNames.delete);
		document.setId(entityId);
		return document;
	}
}
