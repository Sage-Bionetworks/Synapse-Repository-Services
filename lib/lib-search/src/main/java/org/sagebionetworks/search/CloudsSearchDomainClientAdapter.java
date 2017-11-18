package org.sagebionetworks.search;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient;
import com.amazonaws.services.cloudsearchdomain.model.Bucket;
import com.amazonaws.services.cloudsearchdomain.model.BucketInfo;
import com.amazonaws.services.cloudsearchdomain.model.Hits;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsRequest;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsResult;
import org.sagebionetworks.repo.model.search.AwesomeSearchFactory;
import org.sagebionetworks.repo.model.search.Facet;
import org.sagebionetworks.repo.model.search.FacetConstraint;
import org.sagebionetworks.repo.model.search.FacetTypeNames;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.apache.commons.lang.math.NumberUtils; //TODO: are there different version of apache commons for different parts of the codebase? I can't use the lang3 library here but I can in SearchServiceImpl
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudsSearchDomainClientAdapter {
	private boolean initialized;
	private AmazonCloudSearchDomainClient client;


	public CloudsSearchDomainClientAdapter(AWSCredentials awsCredentials){//TODO: maybe need to change constructor?
		this.client = new AmazonCloudSearchDomainClient(awsCredentials);
		this.initialized = false;
	}

	public void sendDocuments(String documents){
		checEndpointInitilaization();

		byte[] documentBytes = documents.getBytes();
		UploadDocumentsRequest request = new UploadDocumentsRequest()
										.withContentType("application/json")
										.withDocuments(new ByteArrayInputStream(documentBytes))
										.withContentLength((long) documentBytes.length);
		UploadDocumentsResult result = client.uploadDocuments(request);
	}

	public void setEndpoint(String endpoint){
		client.setEndpoint(endpoint);
		initialized = (endpoint != null && !"".equals(endpoint));
	}

	public SearchResults search(SearchRequest request){
		checEndpointInitilaization();
		return SearchUtil.convertToSynapseSearchResult(client.search(request));
	}

	public boolean isInitialized(){
		return initialized;
	}

	private void checEndpointInitilaization(){
		if(!isInitialized()){
			throw new IllegalArgumentException("The endpoint is not yet initialized, please use setEndpoint() before calling this method"); //TODO: use different exception
		}
	}
}
