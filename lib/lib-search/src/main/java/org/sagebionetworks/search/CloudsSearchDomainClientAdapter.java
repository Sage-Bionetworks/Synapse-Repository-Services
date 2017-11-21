package org.sagebionetworks.search;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient;
import com.amazonaws.services.cloudsearchdomain.model.Bucket;
import com.amazonaws.services.cloudsearchdomain.model.BucketInfo;
import com.amazonaws.services.cloudsearchdomain.model.Hits;
import com.amazonaws.services.cloudsearchdomain.model.SearchException;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsRequest;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.search.AwesomeSearchFactory;
import org.sagebionetworks.repo.model.search.Facet;
import org.sagebionetworks.repo.model.search.FacetConstraint;
import org.sagebionetworks.repo.model.search.FacetTypeNames;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.apache.commons.lang.math.NumberUtils; //TODO: are there different version of apache commons for different parts of the codebase? I can't use the lang3 library here but I can in SearchServiceImpl
import org.sagebionetworks.util.ValidateArgument;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



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
		checEndpointInitilaization();

		byte[] documentBytes = documents.getBytes();
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
		checEndpointInitilaization();

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
			}
			logger.error("search(): Failed for unexpected reasons (request="+request+") with status: " + e.getStatusCode());
			throw e;
		}
	}

	public boolean isInitialized(){
		return initialized;
	}

	private void checEndpointInitilaization(){
		if(!isInitialized()){
			throw new IllegalStateException("The endpoint is not yet initialized, please use setEndpoint() before calling this method"); //TODO: use different exception
		}
	}
}
