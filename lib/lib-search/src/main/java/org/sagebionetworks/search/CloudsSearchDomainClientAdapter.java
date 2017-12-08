package org.sagebionetworks.search;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient;
import com.amazonaws.services.cloudsearchdomain.model.SearchException;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsRequest;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.util.ValidateArgument;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;


public class CloudsSearchDomainClientAdapter {
	static private Logger logger = LogManager.getLogger(CloudsSearchDomainClientAdapter.class);

	private boolean initialized;
	private AmazonCloudSearchDomainClient client;


	public CloudsSearchDomainClientAdapter(AWSCredentials awsCredentials){//TODO: maybe need to change constructor?
		this.client = new AmazonCloudSearchDomainClient(awsCredentials);
		this.initialized = false;
	}

	public void sendDocuments(String documents){
		ValidateArgument.required(documents, "documents");
		checkEndpointInitilaization();

		byte[] documentBytes = documents.getBytes(StandardCharsets.UTF_8);
		UploadDocumentsRequest request = new UploadDocumentsRequest()
										.withContentType("application/json")
										.withDocuments(new ByteArrayInputStream(documentBytes))
										.withContentLength((long) documentBytes.length);
		UploadDocumentsResult result = client.uploadDocuments(request);
	}

	public void setEndpoint(String endpoint){
		ValidateArgument.required(endpoint, "endpoint");
		ValidateArgument.requirement(!"".equals(endpoint), "endpoint must not be an empty String");

		client.setEndpoint(endpoint);
		initialized = true;
	}

	public SearchResults search(SearchRequest request) throws CloudSearchClientException{ //TODO: rename cloudsearch client exception?
		ValidateArgument.required(request, "request");
		checkEndpointInitilaization();

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

	public boolean isInitialized(){
		return initialized;
	}

	private void checkEndpointInitilaization(){
		if(!isInitialized()){
			throw new IllegalStateException("The endpoint is not yet initialized, please use setEndpoint() before calling this method");
		}
	}
}
