package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


import com.amazonaws.services.cloudsearchdomain.model.Bucket;
import com.amazonaws.services.cloudsearchdomain.model.BucketInfo;
import com.amazonaws.services.cloudsearchdomain.model.Hits;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.search.SearchDocumentDriver;
import org.sagebionetworks.repo.manager.search.SearchHelper;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.Facet;
import org.sagebionetworks.repo.model.search.FacetConstraint;
import org.sagebionetworks.repo.model.search.FacetTypeNames;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.search.CloudSearchClientException;
import org.sagebionetworks.search.SearchConstants;
import org.sagebionetworks.search.SearchDao;
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
			throws ClientProtocolException,	IOException, DatastoreException,
			NotFoundException, ServiceUnavailableException, CloudSearchClientException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return proxySearchAwsApi(userInfo, searchQuery);
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
		String cleanedSearchQuery = createQueryString(userInfo, searchQuery);
		SearchResults results = searchDao.executeSearch(cleanedSearchQuery);
		// Add any extra return results to the hits
		if(results != null && results.getHits() != null){
			addReturnDataToHits(results.getHits(), includePath);
		}
		return results;
	}

	@Override
	public @ResponseBody
	SearchResults proxySearchAwsApi(UserInfo userInfo, SearchQuery searchQuery){
		SearchRequest searchRequest =  SearchUtil.generateSearchRequest(searchQuery, userInfo);
		SearchResult result = searchDao.executeCloudSearchDomainSearch(searchRequest);
		return convertToSynapseSearchResult(result);
	}


	public SearchResults convertToSynapseSearchResult(SearchResult cloudSearchResult){
		SearchResults synapseSearchResults = new SearchResults();

		//Handle Translating of facets
		Map<String, BucketInfo> facetMap = cloudSearchResult.getFacets();
		if(facetMap != null) {
			List<Facet> facetList = new ArrayList<>();

			for (Map.Entry<String, BucketInfo> facetInfo : facetMap.entrySet()) {//iterate over each facet

				String facetName = facetInfo.getKey();

				FacetTypeNames facetType = FACET_TYPES.get(facetName);
				if (facetType == null) {
					throw new IllegalArgumentException(
							"facet "
									+ facetName
									+ " is not properly configured, add it to the facet type map");
				}

				Facet synapseFacet = new Facet();
				synapseFacet.setName(facetName;
				synapseFacet.setType(facetType);
				//Note: min and max are never set since the frontend never makes use of them and so the results won't ever have them.

				BucketInfo bucketInfo = facetInfo.getValue();
				List<FacetConstraint> facetConstraints = new ArrayList<>();
				for (Bucket bucket: bucketInfo.getBuckets()){
					FacetConstraint facetConstraint = new FacetConstraint();
					facetConstraint.setValue(bucket.getValue());
					facetConstraint.setCount(bucket.getCount());
				}
				synapseFacet.setConstraints(facetConstraints);

				facetList.add(synapseFacet);
			}
			synapseSearchResults.setFacets(facetList);
		}

		Hits hits = cloudSearchResult.getHits();

		synapseSearchResults.setFound(hits.getFound());
		synapseSearchResults.setStart(hits.getStart());

		//class names clashing feelsbadman
		List<org.sagebionetworks.repo.model.search.Hit> hitList = new ArrayList<>();
		for(com.amazonaws.services.cloudsearchdomain.model.Hit cloudSearchHit : hits.getHit()){
			org.sagebionetworks.repo.model.search.Hit synapseHit = new org.sagebionetworks.repo.model.search.Hit();

		}
		synapseSearchResults.setHits(hitList);

		System.out.println(cloudSearchResult);
		return synapseSearchResults;
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
		String searchQueryString = SearchUtil.generateStructuredQueryString(searchQuery);
		searchQueryString = filterSeachForAuthorization(userInfo, searchQueryString);
		// Merge boolean queries as needed and escape them
		String cleanedSearchQuery = SearchHelper.cleanUpSearchQueries(searchQueryString);
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
