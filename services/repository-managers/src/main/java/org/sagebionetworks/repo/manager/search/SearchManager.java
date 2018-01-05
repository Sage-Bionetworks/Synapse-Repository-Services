package org.sagebionetworks.repo.manager.search;

import org.apache.http.client.ClientProtocolException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.search.CloudSearchClientException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public interface SearchManager {
	//TODO: documentation
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
	SearchResults proxySearch(UserInfo userInfo, SearchQuery searchQuery) throws UnsupportedEncodingException,
			ClientProtocolException, IOException, ServiceUnavailableException, CloudSearchClientException;

	void documentChangeMessage(ChangeMessage change) throws IOException, CloudSearchClientException;
}
