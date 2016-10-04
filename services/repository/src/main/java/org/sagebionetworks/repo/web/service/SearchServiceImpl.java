package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;


import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.search.SearchDocumentDriver;
import org.sagebionetworks.repo.manager.search.SearchHelper;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.search.SearchConstants;
import org.sagebionetworks.search.SearchDao;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * CloudSearch search controller. It currently offers two methods:
 * <ol>
 * <li>/search appends a authorization filter to the user's search and reformats the result
 * </ol>
 * 
 * @author deflaux
 * 
 */
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
			throws ClientProtocolException,	IOException, HttpClientHelperException,
			DatastoreException, NotFoundException, ServiceUnavailableException {
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
	 * @throws HttpClientHelperException
	 * @throws ServiceUnavailableException
	 */
	public SearchResults proxySearch(UserInfo userInfo, SearchQuery searchQuery) throws UnsupportedEncodingException,
			ClientProtocolException, IOException, HttpClientHelperException, ServiceUnavailableException {
		boolean includePath = false;
		if(searchQuery.getReturnFields() != null && searchQuery.getReturnFields().contains(SearchConstants.FIELD_PATH)){
			includePath = true;
			// We do not want to pass path along to the search index as it is not there.
			searchQuery.getReturnFields().remove(SearchConstants.FIELD_PATH);
		}
		// Create the query string
		String cleanedSearchQuery = createQueryString(userInfo, searchQuery);
		SearchResults results = searchDao.executeSearch(cleanedSearchQuery);
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
	/**
	 * @param userInfo
	 * @param searchQuery
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public String createQueryString(UserInfo userInfo, SearchQuery searchQuery)
			throws UnsupportedEncodingException {
		String serchQueryString = SearchUtil.generateStructuredQueryString(searchQuery);
		serchQueryString = filterSeachForAuthorization(userInfo, serchQueryString);
		// Merge boolean queries as needed and escape them
		String cleanedSearchQuery = SearchHelper.cleanUpSearchQueries(serchQueryString);
		return cleanedSearchQuery;
	}

	/**
	 * @param userInfo
	 * @param searchQuery
	 * @return
	 */
	public static String filterSeachForAuthorization(UserInfo userInfo,String searchQuery) {
		if(userInfo == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if (!userInfo.isAdmin()) {
			String[] splitQuery = searchQuery.split("q=");
			String actualQuery = splitQuery[1];
			int otherParametersIndex = actualQuery.indexOf("&");
			if(otherParametersIndex == -1){
				//no other parameters 
				otherParametersIndex = actualQuery.length();
			}
			searchQuery =  splitQuery[0] + "q=( and "+ actualQuery.substring(0,otherParametersIndex) + " " + SearchHelper.formulateAuthorizationFilter(userInfo) + ")" + actualQuery.substring(otherParametersIndex);
		}
		return searchQuery;
	}
}
