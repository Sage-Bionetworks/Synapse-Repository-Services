package org.sagebionetworks.repo.manager.search;

import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.search.CloudSearchClientException;
import org.sagebionetworks.search.CloudSearchClientProvider;
import org.sagebionetworks.search.CloudsSearchDomainClientAdapter;
import org.sagebionetworks.search.SearchConstants;
import org.sagebionetworks.search.SearchDao;
import org.sagebionetworks.search.SearchUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

public class SearchManagerImpl implements SearchManager{
	private static final Logger log = LogManager.getLogger(SearchManagerImpl.class.getName());

	@Autowired
	SearchDao searchDao;

	@Autowired
	SearchDocumentDriver searchDocumentDriver;

	CloudsSearchDomainClientAdapter cloudsearchDomainClient;//TODO: wire up

//	@Autowired
//	CloudSearchClientProvider cloudSearchClientProvider; //TODO: keep this in SearchDao or move search dao into here?


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
		SearchResults results;
		if (userInfo.isAdmin()){ //TODO: any way to make this look nicer?
			results = cloudsearchDomainClient.unfilteredSearch(searchQuery);
		}else{
			results = cloudsearchDomainClient.filteredSearch(searchQuery, userInfo.getGroups());
		}

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
}
