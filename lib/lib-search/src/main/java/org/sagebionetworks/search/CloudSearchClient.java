package org.sagebionetworks.search;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;

/**
 * CloudSearch does not yet provide a Java SDK. This is the bare minimum needed
 * for Search Updates. If we find more uses for this, we'll move it to something
 * under platform/trunk/lib.
 * 
 * @author deflaux
 * 
 */
public class CloudSearchClient {

	private static final Map<String, String> SEND_DOCUMENTS_REQUEST_HEADERS;
	static {
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.put("Content-Type", "application/json;charset=UTF-8");
		SEND_DOCUMENTS_REQUEST_HEADERS = Collections.unmodifiableMap(requestHeaders);
	}

	private HttpClient httpClient;
	private String searchServiceEndpoint;
	private String documentServiceEndpoint;

	public CloudSearchClient(HttpClient httpClient,
			String searchServiceEndpoint, String documentServiceEndpoint) {
		this.httpClient = httpClient;
		this.searchServiceEndpoint = searchServiceEndpoint;
		this.documentServiceEndpoint = documentServiceEndpoint;
	}

	public void sendDocuments(String documents) throws ClientProtocolException,
			IOException, HttpClientHelperException {
		HttpClientHelper.postContent(httpClient, documentServiceEndpoint,
				documents, SEND_DOCUMENTS_REQUEST_HEADERS);
	}

	public void sendDocuments(InputStream stream, long length) throws ClientProtocolException, IOException, HttpClientHelperException {
		HttpClientHelper.postStream(httpClient, documentServiceEndpoint,
				stream, length, SEND_DOCUMENTS_REQUEST_HEADERS);
	}

	public String performSearch(String searchQuery) throws ClientProtocolException, IOException, HttpClientHelperException {
		String url = searchServiceEndpoint + "?" + searchQuery;
		return HttpClientHelper.getContent(httpClient, url);
	}
	
}
