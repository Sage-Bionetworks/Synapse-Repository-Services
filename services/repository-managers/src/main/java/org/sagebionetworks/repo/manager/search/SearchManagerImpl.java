package org.sagebionetworks.repo.manager.search;

import com.amazonaws.services.cloudsearchdomain.model.QueryParser;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.DatastoreException;
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
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.search.CloudSearchClientException;
import org.sagebionetworks.search.CloudSearchClientProvider;
import org.sagebionetworks.search.CloudsSearchDomainClientAdapter;
import org.sagebionetworks.search.SearchConstants;
import org.sagebionetworks.search.SearchDaoImpl;
import org.sagebionetworks.search.SearchUtil;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import static org.sagebionetworks.search.SearchConstants.FIELD_ID;

public class SearchManagerImpl implements SearchManager{
	private static final Logger log = LogManager.getLogger(SearchManagerImpl.class.getName());


	@Autowired
	SearchDocumentDriver searchDocumentDriver;

	@Autowired
	SearchDaoImpl searchDao;

	@Autowired
	V2WikiPageDao wikiPageDao; //TODO: searchDocumentDriver also autowires this. look to combine????

	@Override
	/**
	 * @param userInfo
	 * @param searchQuery
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws ServiceUnavailableException
	 * @throws CloudSearchClientException
	 */
	public SearchResults proxySearch(UserInfo userInfo, SearchQuery searchQuery) throws UnsupportedEncodingException,
			ClientProtocolException, IOException, ServiceUnavailableException, CloudSearchClientException {

		boolean includePath = false;
		if(searchQuery.getReturnFields() != null && searchQuery.getReturnFields().contains(SearchConstants.FIELD_PATH)){
			includePath = true;
			// We do not want to pass path along to the search index as it is not there.
			searchQuery.getReturnFields().remove(SearchConstants.FIELD_PATH);
		}
		// Create the query string
		SearchRequest searchRequest =  SearchUtil.generateSearchRequest(searchQuery);
		if (!userInfo.isAdmin()){
			searchRequest.setFilterQuery(SearchUtil.formulateAuthorizationFilter(userInfo));
		}
		SearchResults results = SearchUtil.convertToSynapseSearchResult(searchDao.executeSearch(searchRequest));
		// Add any extra return results to the hits
		if(results != null && results.getHits() != null){
			addReturnDataToHits(results.getHits(), includePath);
		}
		return results;
	}

	/**
	 * Add extra return results to the hit list.
	 * @param hits
	 * @param includePath
	 */
	public void addReturnDataToHits(List<Hit> hits, boolean includePath) { //TODO: move in as field of path.
		List<Hit> toRemove = new LinkedList<Hit>();
		if(hits != null){
			// For each hit we need to add the path
			for(Hit hit: hits){
				if(includePath){
					try {
						EntityPath path = searchDocumentDriver.getEntityPath(hit.getId());
						hit.setPath(path);
					} catch (NotFoundException e) {
						// Add a warning and remove it from the hits
						log.warn("Found a search document that did not exist in the reposiroty: "+hit, e);
						// We need to remove this from the hits
						toRemove.add(hit);
					}
				}
			}
		}
		if(!toRemove.isEmpty()){
			hits.removeAll(toRemove);
		}
	}



	/********
	 * Worker related stuff below
	 */

	@Override
	public void documentChangeMessage(ChangeMessage change) throws IOException, CloudSearchClientException { //TODO: document
		// We only care about entity messages as this time
		if (ObjectType.ENTITY == change.getObjectType()) {
			// Is this a create or update
			if (ChangeType.CREATE == change.getChangeType()
					|| ChangeType.UPDATE == change.getChangeType()) {
				processCreateUpdate(change);
			} else if (ChangeType.DELETE == change.getChangeType()) {
				searchDao.deleteDocument(change.getObjectId()); //TODO: convert to document
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

	private void processCreateUpdate(ChangeMessage change) throws IOException, CloudSearchClientException {
		Document newDoc = getDocFromMessage(change);
		if (newDoc != null) {
			searchDao.sendDocument(newDoc);
		}
	}

	private Document getDocFromMessage(ChangeMessage changeMessage) throws CloudSearchClientException, IOException {
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
