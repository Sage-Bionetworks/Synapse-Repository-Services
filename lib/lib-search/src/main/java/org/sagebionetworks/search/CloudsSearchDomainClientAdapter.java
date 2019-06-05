package org.sagebionetworks.search;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.util.ValidateArgument;

import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomain;
import com.amazonaws.services.cloudsearchdomain.model.AmazonCloudSearchDomainException;
import com.amazonaws.services.cloudsearchdomain.model.DocumentServiceException;
import com.amazonaws.services.cloudsearchdomain.model.SearchException;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsRequest;
import com.google.common.collect.Iterators;

/**
 * A wrapper for AWS's AmazonCloudSearchDomain. DO NOT INSTANTIATE.
 * Use CloudSearchClientProvider to get an instance of this class.
 */
public class CloudsSearchDomainClientAdapter {
	static private Logger logger = LogManager.getLogger(CloudsSearchDomainClientAdapter.class);

	private final AmazonCloudSearchDomain client;
	private final CloudSearchDocumentBatchIteratorProvider iteratorProvider;

	CloudsSearchDomainClientAdapter(AmazonCloudSearchDomain client, CloudSearchDocumentBatchIteratorProvider iteratorProvider){
		this.client = client;
		this.iteratorProvider = iteratorProvider;
	}

	public List<UploadDocumentsResult> sendDocuments(Iterator<Document> documents){
		ValidateArgument.required(documents, "documents");
		Iterator<CloudSearchDocumentBatch> searchDocumentFileIterator = iteratorProvider.getIterator(documents);

		List<UploadDocumentsResult> uploadDocumentsResults = new LinkedList<>();
		while(searchDocumentFileIterator.hasNext()) {
			Set<String> documentIds = null;
			try (CloudSearchDocumentBatch batch = searchDocumentFileIterator.next();
				 InputStream fileStream = batch.getNewInputStream();) {

				documentIds = batch.getDocumentIds();

				UploadDocumentsRequest request = new UploadDocumentsRequest()
						.withContentType("application/json")
						.withDocuments(fileStream)
						.withContentLength(batch.size());
				uploadDocumentsResults.add(client.uploadDocuments(request));
			} catch (DocumentServiceException e) {
				logger.error("The following documents failed to upload: " +  documentIds);
				documentIds = null;
				throw handleCloudSearchExceptions(e);
			} catch (IOException e){
				throw new TemporarilyUnavailableException(e);
			}
		}
		return uploadDocumentsResults;
	}

	public void sendDocument(Document document){
		ValidateArgument.required(document, "document");
		sendDocuments(Iterators.singletonIterator(document));
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
