package org.sagebionetworks.markdown;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.springframework.lang.Nullable;

public class MarkdownClient {

	private final SimpleHttpClient simpleHttpClient;
	private final String markdownServiceEndpoint;
	private final RequestSigner requestSigner;
	private static final Map<String, String> DEFAULT_REQUEST_HEADERS;

	static {
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.put("Content-Type", "application/json");
		DEFAULT_REQUEST_HEADERS = Collections.unmodifiableMap(requestHeaders);
	}

	public MarkdownClient(
			String markdownServiceEndpoint,
			RequestSigner requestSigner,
			@Nullable SimpleHttpClient simpleHttpClient) {
		this.markdownServiceEndpoint = markdownServiceEndpoint;
		this.requestSigner = requestSigner;
		this.simpleHttpClient = (simpleHttpClient != null)
			? simpleHttpClient
			: new SimpleHttpClientImpl();
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
	public String requestMarkdownConversion(String requestContent) throws MarkdownClientException {
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(markdownServiceEndpoint);

		byte[] payload = requestContent.getBytes(StandardCharsets.UTF_8);
		Map<String, String> signedHeaders = requestSigner.sign(URI.create(markdownServiceEndpoint), payload);

		Map<String, String> headers = new HashMap<>(DEFAULT_REQUEST_HEADERS);
		headers.putAll(signedHeaders);
		request.setHeaders(headers);

		try {
			SimpleHttpResponse response = simpleHttpClient.post(request, requestContent);
			if (response.getStatusCode() == 200) {
				return response.getContent();
			} else {
				String message = "Fail to request markdown conversion for request: "+requestContent;
				throw new MarkdownClientException(response.getStatusCode(), message);
			}
		} catch (IOException  e) {
			throw new MarkdownClientException(e);
		}
	}

	public String getMarkdownServiceEndpoint() {
		return markdownServiceEndpoint;
	}

	public SimpleHttpClient getSimpleHttpClient() {
		return simpleHttpClient;
	}
}
