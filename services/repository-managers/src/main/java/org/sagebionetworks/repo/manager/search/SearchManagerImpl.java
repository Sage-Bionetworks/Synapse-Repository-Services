package org.sagebionetworks.repo.manager.search;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.search.SearchConstants;
import org.sagebionetworks.search.SearchDao;
import org.sagebionetworks.search.SearchUtil;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.google.common.collect.Iterators;

public class SearchManagerImpl implements SearchManager{
	private static final Logger log = LogManager.getLogger(SearchManagerImpl.class.getName());


	@Autowired
	SearchDocumentDriver searchDocumentDriver;

	@Autowired
	ChangeMessageToSearchDocumentTranslator translator;

	@Autowired
	SearchDao searchDao;

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
	public void documentChangeMessages(List<ChangeMessage> messages){
		Iterator<Document> documentIterator = Iterators.filter(
				Iterators.transform(messages.iterator(), translator::generateSearchDocumentIfNecessary),
				Objects::nonNull);
		searchDao.sendDocuments(documentIterator);
	}
}
