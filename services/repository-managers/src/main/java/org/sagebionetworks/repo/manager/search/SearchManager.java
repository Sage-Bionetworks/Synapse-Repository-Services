package org.sagebionetworks.repo.manager.search;

import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import org.apache.http.client.ClientProtocolException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.search.CloudSearchClientException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public interface SearchManager {
	//TODO: documentation

	SearchResults proxySearch(UserInfo userInfo, SearchQuery searchQuery) ;

	SearchResult rawSearch (SearchRequest searchRequest) ;

	void deleteAllDocuments() throws InterruptedException;

	boolean doesDocumentExist(String id, String etag) ;

	void createOrUpdateSearchDocument(Document document);

	void documentChangeMessage(ChangeMessage change) throws IOException;
}
