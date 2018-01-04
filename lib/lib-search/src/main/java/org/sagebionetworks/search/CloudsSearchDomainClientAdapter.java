package org.sagebionetworks.search;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient;
import com.amazonaws.services.cloudsearchdomain.model.SearchException;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsRequest;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.util.ValidateArgument;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public class CloudsSearchDomainClientAdapter {
	static private Logger logger = LogManager.getLogger(CloudsSearchDomainClientAdapter.class);

	private AmazonCloudSearchDomainClient client;


	public CloudsSearchDomainClientAdapter(AmazonCloudSearchDomainClient client){//TODO: maybe need to change constructor?
		this.client = client;
	}

	public void sendDocuments(Document document){//TODO: test
		ValidateArgument.required(document, "document");
		sendDocuments(Collections.singletonList(document));
	}

	public void sendDocuments(List<Document> batch){
		sendDocuments(SearchUtil.convertSearchDocumentsToJSON(batch));
	}

	void sendDocuments(String documents){
		//TODO: private or package visibility?

		ValidateArgument.required(documents, "documents");

		byte[] documentBytes = documents.getBytes(StandardCharsets.UTF_8);
		UploadDocumentsRequest request = new UploadDocumentsRequest()
										.withContentType("application/json")
										.withDocuments(new ByteArrayInputStream(documentBytes))
										.withContentLength((long) documentBytes.length);
		UploadDocumentsResult result = client.uploadDocuments(request);
	}


	public SearchResults unfilteredSearch(SearchQuery searchQuery) throws CloudSearchClientException {
		SearchRequest searchRequest = SearchUtil.generateSearchRequest(searchQuery);
		return rawSearch(searchRequest);
	}

	/**
	 * Executes the searchQuery but filters limits results to only those that the user belongs.
	 * @param searchQuery
	 * @param userGroups Set of user group ids in which the user executing this query belongs. Preferably retrieved from UserInfo.getGroups()
	 * @return
	 */
	public SearchResults filteredSearch(SearchQuery searchQuery, Set<Long> userGroups) throws CloudSearchClientException {
		SearchRequest searchRequest = SearchUtil.generateSearchRequest(searchQuery);
		if (searchRequest.getFilterQuery() != null){
			throw new IllegalArgumentException("did not expect searchRequest to already contain a filterQuery");
		}
		searchRequest.setFilterQuery(SearchUtil.formulateAuthorizationFilter(userGroups));
		return rawSearch(searchRequest);
	}

	SearchResults rawSearch(SearchRequest request) throws CloudSearchClientException{ //TODO: rename cloudsearch client exception?
		//TODO: private or package visibility?
		ValidateArgument.required(request, "request");

		try{
			return SearchUtil.convertToSynapseSearchResult(client.search(request));
		}catch (SearchException e){
			int statusCode = e.getStatusCode();
			if(statusCode / 100 == 4){ //4xx status codes
				logger.error("search(): Exception rethrown (request="+request+") with status: " + e.getStatusCode());
				throw new CloudSearchClientException(statusCode, e.getMessage());
			} else if (statusCode / 100 == 5){ // 5xx status codes
				//The AWS API already has retry logic for 5xx status codes so getting one here means retries failed
				logger.error("search(): Failed after retries (request="+request+") with status: " + e.getStatusCode());
				throw e;
			}else {
				logger.error("search(): Failed for unexpected reasons (request=" + request + ") with status: " + e.getStatusCode());
				throw e;
			}
		}
	}

}
