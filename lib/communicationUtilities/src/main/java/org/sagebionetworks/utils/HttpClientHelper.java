package org.sagebionetworks.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

/**
 * @author deflaux
 * 
 */
public class HttpClientHelper {

	/**
	 * 
	 */
	public static final int MAX_ALLOWED_DOWNLOAD_TO_STRING_LENGTH = 1024 * 1024;
	private static final int DEFAULT_CONNECT_TIMEOUT_MSEC = 500;
	private static final int DEFAULT_SOCKET_TIMEOUT_MSEC = 2000;
	private static final HttpClient httpClient;

	static {

		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory
				.getSocketFactory()));
		schemeRegistry.register(new Scheme("https", 443, SSLSocketFactory
				.getSocketFactory()));

		// TODO its unclear how to set a default for the timeout in milliseconds
		// used when retrieving an
		// instance of ManagedClientConnection from the ClientConnectionManager
		// since parameters are now deprecated for connection managers.
		ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager(
				schemeRegistry);

		HttpParams clientParams = new BasicHttpParams();
		clientParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,
				DEFAULT_CONNECT_TIMEOUT_MSEC);
		clientParams.setParameter(CoreConnectionPNames.SO_TIMEOUT,
				DEFAULT_SOCKET_TIMEOUT_MSEC);

		httpClient = new DefaultHttpClient(connectionManager, clientParams);
	}

	/**
	 * The ThreadSafeClientConnMangager here uses default configuration. Allow
	 * clients to get access to it in case they want to:
	 * <ul>
	 * <li>increase the max number of concurrent connections per endpoint
	 * <li>increase the max number of concurrent connections for a particular
	 * endpoint
	 * <li>increase the max total number of concurrent connections allowed
	 * <li>change the timeout for how long to wait for a connnection from the
	 * pool to become available
	 * 
	 * @return the ClientConnectionManager
	 */
	public static ClientConnectionManager getConnectionManager() {
		return httpClient.getConnectionManager();
	}

	/**
	 * Set the timeout in milliseconds until a connection is established. A
	 * timeout value of zero is interpreted as an infinite timeout. This will
	 * change the configuration for all requests.
	 * 
	 * @param milliseconds
	 */
	public static void setGlobalConnectionTimeout(int milliseconds) {
		httpClient.getParams().setParameter(
				CoreConnectionPNames.CONNECTION_TIMEOUT, milliseconds);
	}

	/**
	 * Set the socket timeout (SO_TIMEOUT) in milliseconds, which is the timeout
	 * for waiting for data or, put differently, a maximum period inactivity
	 * between two consecutive data packets). A timeout value of zero is
	 * interpreted as an infinite timeout. This will change the configuration
	 * for all requests.
	 * 
	 * @param milliseconds
	 */
	public static void setGlobalSocketTimeout(int milliseconds) {
		httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,
				milliseconds);
	}

	/**
	 * Perform a REST API request
	 * 
	 * @param requestUrl
	 * @param requestMethod
	 * @param requestContent
	 * @param requestHeaders
	 * @return response body
	 * @throws HttpClientHelperException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws HttpClientHelperException
	 * @throws IOException
	 */
	public static HttpResponse performRequest(String requestUrl,
			String requestMethod, String requestContent,
			Map<String, String> requestHeaders) throws ClientProtocolException,
			IOException, HttpClientHelperException {

		return performRequest(requestUrl, requestMethod, requestContent,
				requestHeaders, null);
	}

	/**
	 * Perform a REST API request, expecting a non-standard HTTP status
	 * 
	 * @param requestUrl
	 * @param requestMethod
	 * @param requestContent
	 * @param requestHeaders
	 * @param overridingExpectedResponseStatus
	 * @return response body
	 * @throws HttpClientHelperException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws HttpClientHelperException
	 * @throws IOException
	 */
	public static HttpResponse performRequestShouldFail(String requestUrl,
			String requestMethod, String requestContent,
			Map<String, String> requestHeaders,
			Integer overridingExpectedResponseStatus)
			throws ClientProtocolException, IOException,
			HttpClientHelperException {
		return performRequest(requestUrl, requestMethod, requestContent,
				requestHeaders, overridingExpectedResponseStatus);
	}

	public static HttpResponse performRequest(String requestUrl,
			String requestMethod, String requestContent,
			Map<String, String> requestHeaders,
			Integer overridingExpectedResponseStatus)
			throws ClientProtocolException, IOException,
			HttpClientHelperException {

		int defaultExpectedReponseStatus = 200;

		HttpRequestBase request = null;
		if (requestMethod.equals("GET")) {
			request = new HttpGet(requestUrl);
		} else if (requestMethod.equals("POST")) {
			request = new HttpPost(requestUrl);
			if (null != requestContent) {
				((HttpEntityEnclosingRequestBase) request)
						.setEntity(new StringEntity(requestContent));
			}
			defaultExpectedReponseStatus = 201;
		} else if (requestMethod.equals("PUT")) {
			request = new HttpPut(requestUrl);
			if (null != requestContent) {
				((HttpEntityEnclosingRequestBase) request)
						.setEntity(new StringEntity(requestContent));
			}
		} else if (requestMethod.equals("DELETE")) {
			request = new HttpDelete(requestUrl);
			defaultExpectedReponseStatus = 204;
		}

		int expectedResponseStatus = (null == overridingExpectedResponseStatus) ? defaultExpectedReponseStatus
				: overridingExpectedResponseStatus;

		for (Entry<String, String> header : requestHeaders.entrySet()) {
			request.setHeader(header.getKey(), header.getValue());
		}

		HttpResponse response = httpClient.execute(request);

		if (expectedResponseStatus != response.getStatusLine().getStatusCode()) {
			StringBuilder verboseMessage = new StringBuilder(
					"FAILURE: Expected " + defaultExpectedReponseStatus
							+ " but got "
							+ response.getStatusLine().getStatusCode()
							+ " for " + requestUrl);
			if (0 < requestHeaders.size()) {
				verboseMessage.append("\nHeaders: ");
				for (Entry<String, String> entry : requestHeaders.entrySet()) {
					verboseMessage.append("\n\t" + entry.getKey() + ": "
							+ entry.getValue());
				}
			}
			if (null != requestContent) {
				verboseMessage.append("\nRequest Content: " + requestContent);
			}
			String responseBody = (null != response.getEntity()) ? EntityUtils
					.toString(response.getEntity()) : null;
			verboseMessage.append("\nResponse Content: " + responseBody);
			throw new HttpClientHelperException(verboseMessage.toString(),
					response);
		}
		return response;
	}

	/**
	 * TODO - better error handling - more useful error diagnostics such as the
	 * body of the error response
	 * 
	 * @param requestUrl
	 * @return the contents of the file in a string
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 */
	public static String getFileContents(final String requestUrl)
			throws ClientProtocolException, IOException,
			HttpClientHelperException {
		String fileContents = null;

		HttpGet get = new HttpGet(requestUrl);
		HttpResponse response = httpClient.execute(get);
		if (300 <= response.getStatusLine().getStatusCode()) {
			throw new HttpClientHelperException(
					"Request(" + requestUrl + ") failed: "
							+ response.getStatusLine().getReasonPhrase(),
					response);
		}
		HttpEntity fileEntity = response.getEntity();
		if (null != fileEntity) {
			if (MAX_ALLOWED_DOWNLOAD_TO_STRING_LENGTH < fileEntity
					.getContentLength()) {
				throw new HttpClientHelperException("Requested content("
						+ requestUrl + ") is too large("
						+ fileEntity.getContentLength()
						+ "), download it to a file instead", response);
			}
			fileContents = EntityUtils.toString(fileEntity);
		}
		return fileContents;
	}

	/**
	 * TODO
	 * <ul>
	 * <li>large downloads (e.g., 5TB download from S3)
	 * <li>multipart downloads
	 * <li>restartable downloads
	 * </ul>
	 * 
	 * @param requestUrl
	 * @param filepath
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 */
	public static void downloadFile(final String requestUrl,
			final String filepath) throws ClientProtocolException, IOException,
			HttpClientHelperException {

		HttpGet get = new HttpGet(requestUrl);
		HttpResponse response = httpClient.execute(get);
		if (300 <= response.getStatusLine().getStatusCode()) {
			String errorMessage = "Request(" + requestUrl + ") failed: "
					+ response.getStatusLine().getReasonPhrase();
			HttpEntity responseEntity = response.getEntity();
			if (null != responseEntity) {
				errorMessage += EntityUtils.toString(responseEntity);
			}
			throw new HttpClientHelperException(errorMessage, response);
		}
		HttpEntity fileEntity = response.getEntity();
		if (null != fileEntity) {
			FileOutputStream fileOutputStream = new FileOutputStream(filepath);
			fileEntity.writeTo(fileOutputStream);
			fileOutputStream.close();
		}
	}

	/**
	 * TODO
	 * <ul>
	 * <li>large uploads (e.g., 5TB upload from S3)
	 * <li>multipart uploads
	 * <li>restartable uploads
	 * </ul>
	 * 
	 * @param requestUrl
	 * @param filepath
	 * @param contentType
	 * @param requestHeaders
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 */
	public static void uploadFile(final String requestUrl,
			final String filepath, final String contentType,
			Map<String, String> requestHeaders) throws ClientProtocolException,
			IOException, HttpClientHelperException {

		HttpPut put = new HttpPut(requestUrl);

		FileEntity fileEntity = new FileEntity(new File(filepath), contentType);
		put.setEntity(fileEntity);

		if (null != requestHeaders) {
			for (Entry<String, String> header : requestHeaders.entrySet()) {
				put.addHeader(header.getKey(), header.getValue());
			}
		}

		HttpResponse response = httpClient.execute(put);
		if (300 <= response.getStatusLine().getStatusCode()) {
			String errorMessage = "Request(" + requestUrl + ") failed: "
					+ response.getStatusLine().getReasonPhrase();
			HttpEntity responseEntity = response.getEntity();
			if (null != responseEntity) {
				errorMessage += EntityUtils.toString(responseEntity);
			}
			throw new HttpClientHelperException(errorMessage, response);
		}
	}
}
