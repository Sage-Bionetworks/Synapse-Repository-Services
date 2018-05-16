package org.sagebionetworks.search;

import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomain;
import com.amazonaws.services.cloudsearchdomain.model.AmazonCloudSearchDomainException;
import com.amazonaws.services.cloudsearchdomain.model.DocumentServiceException;
import com.amazonaws.services.cloudsearchdomain.model.SearchException;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsRequest;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.util.ValidateArgument;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * A wrapper for AWS's AmazonCloudSearchDomain. DO NOT INSTANTIATE.
 * Use CloudSearchClientProvider to get an instance of this class.
 */
public class CloudsSearchDomainClientAdapter {
	static private Logger logger = LogManager.getLogger(CloudsSearchDomainClientAdapter.class);

	private AmazonCloudSearchDomain client;


	CloudsSearchDomainClientAdapter(AmazonCloudSearchDomain client){
		this.client = client;
	}

	public void sendDocument(Document document){
		ValidateArgument.required(document, "document");
		sendDocuments(Collections.singletonList(document));
	}

	public void sendDocuments(List<Document> batch){
		ValidateArgument.required(batch, "batch");
		ValidateArgument.requirement(!batch.isEmpty(), "List<Document> batch cannot be empty");


		String documents = SearchUtil.convertSearchDocumentsToJSONString(batch);
		byte[] documentBytes = documents.getBytes(StandardCharsets.UTF_8);
		UploadDocumentsRequest request = new UploadDocumentsRequest()
										.withContentType("application/json")
										.withDocuments(new ByteArrayInputStream(documentBytes))
										.withContentLength((long) documentBytes.length);
		try {
			UploadDocumentsResult result = client.uploadDocuments(request);
		} catch (DocumentServiceException e){
			throw handleCloudSearchExceptions(e);
		}
	}

	SearchResult rawSearch(SearchRequest request) {
		ValidateArgument.required(request, "request");

		try{
			return client.search(request);
		}catch (SearchException e){
			throw handleCloudSearchExceptions(e);
		}
	}

	RuntimeException handleCloudSearchExceptions(AmazonCloudSearchDomainException e){
		int statusCode = e.getStatusCode();
		if(statusCode / 100 == 4){ //4xx status codes
			return new IllegalArgumentException(e);
		} else if (statusCode / 100 == 5){ // 5xx status codes
			// The AWS API already has retry logic for 5xx status codes so getting one here means retries failed
			// AmazonCloudSearchDomainException is a subclass of AmazonServiceException,
			// which is already handled by BaseController and mapped to a 502 HTTP error
			return e;
		}else {
			logger.warn("Failed for unexpected reasons with status: " + e.getStatusCode());
			return e;
		}
	}

}
