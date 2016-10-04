package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.web.bind.annotation.ResponseBody;

public interface SearchService {

	/**
	 * @param userId
	 * @param searchQuery
	 * @param request
	 * @return search results from CloudSearch
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws ServiceUnavailableException
	 */
	public @ResponseBody
	SearchResults proxySearch(Long userId, SearchQuery searchQuery) throws ClientProtocolException, IOException, HttpClientHelperException,
			DatastoreException, NotFoundException, ServiceUnavailableException;

}