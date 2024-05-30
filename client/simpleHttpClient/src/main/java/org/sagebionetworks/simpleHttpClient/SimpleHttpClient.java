package org.sagebionetworks.simpleHttpClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.ClientProtocolException;

public interface SimpleHttpClient {

	/**
	 * Performs a GET request
	 * 
	 * @param request
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	SimpleHttpResponse get(SimpleHttpRequest request) throws ClientProtocolException, IOException;

	/**
	 * Performs an OPTIONS request
	 * 
	 * @param request
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	SimpleHttpResponse options(SimpleHttpRequest request) throws ClientProtocolException, IOException;

	/**
	 * Performs a POST request
	 * 
	 * @param request
	 * @param requestBody
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	SimpleHttpResponse post(SimpleHttpRequest request, String requestBody) throws ClientProtocolException, IOException;

	/**
	 * Performs a PUT request
	 * 
	 * @param request
	 * @param requestBody
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	SimpleHttpResponse put(SimpleHttpRequest request, String requestBody) throws ClientProtocolException, IOException;

	/**
	 * Performs a DELETE request
	 * 
	 * @param request
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	SimpleHttpResponse delete(SimpleHttpRequest request) throws ClientProtocolException, IOException;

	/**
	 * Performs an file upload
	 * 
	 * @param request
	 * @param toUpload
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	SimpleHttpResponse putFile(SimpleHttpRequest request, File toUpload) throws ClientProtocolException, IOException;

	/**
	 * Performs a file download
	 * 
	 * @param request
	 * @param result
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	SimpleHttpResponse getFile(SimpleHttpRequest request, File result) throws ClientProtocolException, IOException;

	/**
	 * Performs an file upload from an inputStream
	 * 
	 * @param request
	 * @param toUpload
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	SimpleHttpResponse putToURL(SimpleHttpRequest request, InputStream toUpload, long inputLength) throws ClientProtocolException, IOException;

	/**
	 * Gets the first cookie belonging to the domain with specified name. This ignores the path.
	 * @param domain
	 * @param name
	 * @return
	 */
	String getFirstCookieValue(String domain, String name);

	/**
	 * Adds a cookie to the domain's root path (e.g. "somedomain.com/", NOT "somedomain.com/some/path") with the specified name and value.
	 * @param domain
	 * @param name
	 * @param value
	 */
	void addCookie(String domain, String name, String value);

	/**
	 * Clears all cookies that are currently stored
	 */
	void clearAllCookies();
}
