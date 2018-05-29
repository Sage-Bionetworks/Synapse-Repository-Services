package org.sagebionetworks.repo.manager.search;

import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.search.SearchConstants;
import org.sagebionetworks.search.SearchDao;
import org.sagebionetworks.search.SearchUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class SearchManagerImpl implements SearchManager{
	private static final Logger log = LogManager.getLogger(SearchManagerImpl.class.getName());


	@Autowired
	SearchDocumentDriver searchDocumentDriver;

	@Autowired
	SearchDao searchDao;

	@Autowired
	V2WikiPageDao wikiPageDao;

	@Override
	public SearchResults proxySearch(UserInfo userInfo, SearchQuery searchQuery) {
		boolean includePath = false;
		if(searchQuery.getReturnFields() != null){
			// We do not want to pass FIELD_PATH along to the search index as it is not there. So we remove that field
			// and use includePath to indicate that the FIELD_PATH was requested.
			//List<T>.remove() returns a boolean indicating whether the return fields previously contained FIELD_PATH
			includePath = searchQuery.getReturnFields().remove(SearchConstants.FIELD_PATH);
		}
		// Create the query string
		SearchRequest searchRequest =  SearchUtil.generateSearchRequest(searchQuery);
		filterSearchForAuthorization(userInfo, searchRequest);
		SearchResults results = SearchUtil.convertToSynapseSearchResult(searchDao.executeSearch(searchRequest));
		// Add any extra return results to the hits
		if(includePath && results != null && results.getHits() != null){
			//FIELD_PATH is resolved here after search results are retrieved from cloudwatch
			addReturnDataToHits(results.getHits());
		}
		return results;
	}

	static void filterSearchForAuthorization(UserInfo userInfo, SearchRequest searchRequest) {
		if (!userInfo.isAdmin()){
			SearchUtil.addAuthorizationFilter(searchRequest, userInfo);
		}
	}

	@Override
	public SearchResult rawSearch(SearchRequest searchRequest) {
		return searchDao.executeSearch(searchRequest);
	}

	/**
	 * Add extra return results to the hit list.
	 * @param hits
	 */
	public void addReturnDataToHits(List<Hit> hits) {
		if(hits != null){
			// For each hit we need to add the path
			List<Hit> toRemove = new LinkedList<>();
			for(Hit hit: hits){
				try {
					EntityPath path = searchDocumentDriver.getEntityPath(hit.getId());
					hit.setPath(path);
				} catch (NotFoundException e) {
					// Add a warning and remove it from the hits
					log.warn("Found a search document that did not exist in the repository: "+hit);
					// We need to remove this from the hits
					toRemove.add(hit);
				}
			}
			hits.removeAll(toRemove);
		}
	}

	//TODO: maybe move these functions from dao into manager
	@Override
	public void deleteAllDocuments() throws InterruptedException{
		searchDao.deleteAllDocuments();
	}

	@Override
	public boolean doesDocumentExist(String id, String etag) {
		return searchDao.doesDocumentExist(id, etag);
	}

	@Override
	public void createOrUpdateSearchDocument(Document document){
		searchDao.createOrUpdateSearchDocument(document);
	}

	/********
	 * Worker related stuff below
	 */

	@Override
	public void documentChangeMessage(ChangeMessage change) throws IOException{
		// We only care about entity messages as this time
		if (ObjectType.ENTITY == change.getObjectType()) {
			// Is this a create or update
			if (ChangeType.CREATE == change.getChangeType()
					|| ChangeType.UPDATE == change.getChangeType()) {
				processCreateUpdate(change);
			} else if (ChangeType.DELETE == change.getChangeType()) {
				searchDao.deleteDocument(change.getObjectId());
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
					processCreateUpdate(newMessage);
				}
			} catch (NotFoundException e) {
				// Nothing to do if the wiki does not exist
				log.debug("Wiki not found for id: " + change.getObjectId()
						+ " Message:" + e.getMessage());
			}
		}
	}

	private void processCreateUpdate(ChangeMessage change) throws IOException{
		Document newDoc = getDocFromMessage(change);
		if (newDoc != null) {
			searchDao.createOrUpdateSearchDocument(newDoc);
		}
	}

	private Document getDocFromMessage(ChangeMessage changeMessage) throws IOException {
		// We want to ignore this message if a document with this ID and Etag
		// already exists in the search index.
		if (!searchDao.doesDocumentExist(changeMessage.getObjectId(),
				changeMessage.getObjectEtag())) {
			// We want to ignore this message if a document with this ID and
			// Etag are not in the repository as it is an
			// old message.
			if (changeMessage.getObjectEtag() == null
					|| searchDocumentDriver.doesNodeExist(
						changeMessage.getObjectId(),
						changeMessage.getObjectEtag())) {
				try {
					return searchDocumentDriver
							.formulateSearchDocument(changeMessage
									.getObjectId());
				} catch (NotFoundException e) {
					// There is nothing to do if it does not exist
					log.debug("Node not found for id: "
							+ changeMessage.getObjectId() + " Message:"
							+ e.getMessage());
				}
			}
		}
		return null;
	}
}
