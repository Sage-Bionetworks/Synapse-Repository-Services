package org.sagebionetworks.search;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;

/**
 * CloudSearch does not yet provide a Java SDK. This is the bare minimum needed
 * for Search Updates. If we find more uses for this, we'll move it to something
 * under platform/trunk/lib.
 * 
 * @author deflaux
 * 
 */
public class CloudSearchClient {

	static private Logger logger = LogManager.getLogger(CloudSearchClient.class);
	
	private static final Map<String, String> SEND_DOCUMENTS_REQUEST_HEADERS;
	static {
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.put("Content-Type", "application/json;charset=UTF-8");
		SEND_DOCUMENTS_REQUEST_HEADERS = Collections.unmodifiableMap(requestHeaders);
	}

	private final long MAX_BACKOFF_MS = 6400L;
	
	private SimpleHttpClient httpClient;
	private String searchServiceEndpoint;
	private String documentServiceEndpoint;

	public CloudSearchClient() {
	}
	
	public CloudSearchClient(String searchServiceEndpoint, String documentServiceEndpoint) {
		this.searchServiceEndpoint = searchServiceEndpoint;
		this.documentServiceEndpoint = documentServiceEndpoint;
	}
	
	public void _init() {
		this.httpClient = new SimpleHttpClientImpl(null);
	}
	
	public void setSearchServiceEndpoint(String endpoint) {
		this.searchServiceEndpoint = endpoint;
	}
	
	public String getSearchServiceEndpoint() {
		return this.searchServiceEndpoint;
	}

	public void setDocumentServiceEndpoint(String endpoint) {
		this.documentServiceEndpoint = endpoint;
	}
	
	public String getDocumentServiceEndpoint() {
		return this.documentServiceEndpoint;
	}

	public void sendDocuments(String documents) throws ClientProtocolException,
			IOException {
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setHeaders(SEND_DOCUMENTS_REQUEST_HEADERS);
		request.setUri(documentServiceEndpoint);
		httpClient.post(request, documents);
	}

	public String performSearch(String searchQuery) throws ClientProtocolException, IOException, CloudSearchClientException {
		String uri = searchServiceEndpoint + "?" + searchQuery;
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(uri);
		SimpleHttpResponse response;
		long backoffMs = 100L;
		do {
			response = httpClient.get(request);
			if (response.getStatusCode() == 200) {
				return response.getContent();
			}
			if (response.getStatusCode() == 507) {
				try {
					Thread.sleep(backoffMs);
				} catch (InterruptedException e) {
					// Continue
				}
			} else {
				// rethrow
				logger.error("performSearch(): Exception rethrown (url="+(uri==null?"null":uri)+")");
				throw(new CloudSearchClientException(response.getStatusCode(), "Fail to perform search."
						+ " Reason: "+response.getStatusReason()+". Content: "+response.getContent()));
			}
			backoffMs *= 2;
		} while (backoffMs < MAX_BACKOFF_MS);
		// If we're past the max backoff, throw the last 507 we got
		if (backoffMs >= MAX_BACKOFF_MS) {
			logger.error("performSearch(): Backoff exceeded (url="+(uri==null?"null":uri)+")");
			throw(new CloudSearchClientException(response.getStatusCode(), "Fail to perform search."
					+ " Reason: "+response.getStatusReason()+". Content: "+response.getContent()));
		}
		return response.getContent();
	}
}
