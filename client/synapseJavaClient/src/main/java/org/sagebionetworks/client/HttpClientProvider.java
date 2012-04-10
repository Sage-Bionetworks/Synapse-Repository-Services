package org.sagebionetworks.client;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.sagebionetworks.utils.HttpClientHelperException;

/**
 * Provides an abstraction for the HttpClient operations.
 * This allows us to unit test Synapse.
 * 
 * @author jmhill
 *
 */
public interface HttpClientProvider {

	/**
	 * 
	 * @param defaultTimeoutMsec
	 */
	public void setGlobalConnectionTimeout(int defaultTimeoutMsec);

	/**
	 * 
	 * @param defaultTimeoutMsec
	 */
	public void setGlobalSocketTimeout(int defaultTimeoutMsec);

	/**
	 * Upload a file.
	 * 
	 * @param requestUrl
	 * @param filepath
	 * @param contentType
	 * @param requestHeaders
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 */
	public void uploadFile(String requestUrl, String filepath, String contentType,	Map<String, String> requestHeaders) throws ClientProtocolException, IOException, HttpClientHelperException;
	
	
	/**
	 * Upload a file.
	 * 
	 * @param requestUrl
	 * @param filepath
	 * @param contentType
	 * @param requestHeaders
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 */
	public void putFile(String requestUrl, File toPut, Map<String, String> requestHeaders) throws ClientProtocolException, IOException, HttpClientHelperException;

	/**
	 * Download a file.
	 * @param requestUrl
	 * @param filepath
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException 
	 */
	public void downloadFile(final String requestUrl, final String filepath) throws ClientProtocolException, IOException, HttpClientHelperException;

	/**
	 * Perform an HTTP request.
	 * @param string
	 * @param requestMethod
	 * @param requestContent
	 * @param requestHeaders
	 * @return the response
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 */
	public HttpResponse performRequest(String string, String requestMethod,	String requestContent, Map<String, String> requestHeaders) throws ClientProtocolException, IOException, HttpClientHelperException;

}
