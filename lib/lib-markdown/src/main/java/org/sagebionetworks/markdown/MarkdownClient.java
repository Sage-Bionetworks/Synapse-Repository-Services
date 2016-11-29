package org.sagebionetworks.markdown;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;

public class MarkdownClient {

	private SimpleHttpClient simpleHttpClient;
	private String markdownServiceEndpoint;
	private static final Map<String, String> DEFAULT_REQUEST_HEADERS;

	public static final String MARKDOWN_TO_HTML = "/markdown2html";
	static {
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.put("Content-Type", "application/json");
		DEFAULT_REQUEST_HEADERS = Collections.unmodifiableMap(requestHeaders);
	}

	public void _init() {
		if (simpleHttpClient == null) {
			simpleHttpClient = new SimpleHttpClientImpl();
		}
	}

	/**
	 * Takes a json string requestContent (ex. {"markdown":"## a heading"})
	 * Makes a call to the markdown server to convert the raw markdown to html
	 * Return the json string representation of the response (ex. {"result":"<h2 toc=\"true\">a heading</h2>\n"})
	 * 
	 * @param requestContent
	 * @return
	 * @throws ClientProtocolException 
	 * @throws IOException
	 * @throws MarkdownClientException 
	 */
	public String requestMarkdownConversion(String requestContent) throws ClientProtocolException, IOException, MarkdownClientException {
		String uri = markdownServiceEndpoint+MARKDOWN_TO_HTML;
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(uri);
		request.setHeaders(DEFAULT_REQUEST_HEADERS);
		SimpleHttpResponse response = simpleHttpClient.post(request , requestContent);
		if (response.getStatusCode() == 200) {
			return response.getContent();
		} else {
			String message = "Fail to request markdown conversion for request: "+requestContent;
			throw new MarkdownClientException(response.getStatusCode(), message);
		}
	}

	public String getMarkdownServiceEndpoint() {
		return markdownServiceEndpoint;
	}

	public void setMarkdownServiceEndpoint(String markdownServiceEndpoint) {
		this.markdownServiceEndpoint = markdownServiceEndpoint;
	}
}
