package org.sagebionetworks.markdown;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.util.EntityUtils;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.beans.factory.annotation.Autowired;

public class MarkdownClient {

	@Autowired
	private MarkdownHttpClientProvider markdownHttpClientProvider;
	private HttpClient httpClient;
	private String markdownServiceEndpoint;
	private static final Map<String, String> DEFAULT_REQUEST_HEADERS;

	public static final String MARKDOWN_TO_HTML = "/markdown2html";
	static {
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.put("Content-Type", "application/json");
		DEFAULT_REQUEST_HEADERS = Collections.unmodifiableMap(requestHeaders);
	}

	public void _init() {
		if (markdownHttpClientProvider == null) {
			throw new RuntimeException("MarkdownHttpClientProvider is null in MarkdownClient._init()");
		}
		this.httpClient = markdownHttpClientProvider.getHttpClient();
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
	 * @throws HttpClientHelperException 
	 */
	public String requestMarkdownConversion(String requestContent) throws ClientProtocolException, IOException, HttpClientHelperException {
		String url = markdownServiceEndpoint+MARKDOWN_TO_HTML;
		HttpResponse response = HttpClientHelper.performRequest(httpClient, url, "POST", requestContent, DEFAULT_REQUEST_HEADERS);
		int statusCode = response.getStatusLine().getStatusCode();
		HttpEntity responseEntity = response.getEntity();
		String result = EntityUtils.toString(responseEntity);
		if (statusCode == 200) {
			return result;
		} else {
			String message = "Fail to request markdown conversion for request: "+requestContent;
			throw new HttpClientHelperException(message, statusCode, result);
		}
	}

	public String getMarkdownServiceEndpoint() {
		return markdownServiceEndpoint;
	}

	public void setMarkdownServiceEndpoint(String markdownServiceEndpoint) {
		this.markdownServiceEndpoint = markdownServiceEndpoint;
	}
}
