package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;


import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.search.SearchDocumentDriver;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.search.CloudSearchClientException;
import org.sagebionetworks.search.SearchConstants;
import org.sagebionetworks.search.SearchDao;
import org.sagebionetworks.search.SearchUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * CloudSearch search service.
 * @author deflaux
 * 
 */
//TODO: more class documentation?
public class SearchServiceImpl implements SearchService {
	private static final Logger log = LogManager.getLogger(SearchServiceImpl.class
			.getName());
	
	@Autowired
	SearchDao searchDao;

	@Autowired
	UserManager userManager;
	
	@Autowired
	private SearchDocumentDriver searchDocumentDriver;

	public SearchServiceImpl(){}
	/**
	 * For tests
	 * @param searchDao
	 * @param userManager
	 * @param searchDocumentDriver
	 */
	public SearchServiceImpl(SearchDao searchDao, UserManager userManager,
			SearchDocumentDriver searchDocumentDriver) {
		super();
		this.searchDao = searchDao;
		this.userManager = userManager;
		this.searchDocumentDriver = searchDocumentDriver;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.SearchService#proxySearch(java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public @ResponseBody
	SearchResults proxySearch(Long userId, SearchQuery searchQuery) 
			throws ClientProtocolException,	IOException, DatastoreException,
			NotFoundException, ServiceUnavailableException, CloudSearchClientException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return proxySearch(userInfo, searchQuery);
	}

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
		SearchRequest searchRequest =  SearchUtil.generateSearchRequest(searchQuery, userInfo);
		SearchResults results = searchDao.executeSearch(searchRequest);
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
	public void addReturnDataToHits(List<Hit> hits, boolean includePath) {
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
