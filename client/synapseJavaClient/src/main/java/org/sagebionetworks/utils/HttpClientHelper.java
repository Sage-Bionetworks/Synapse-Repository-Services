package org.sagebionetworks.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * TODO
 * - improve the code to better handle large request entities
 */
public class HttpClientHelper {

	private static final HttpClient webClient;
	private static final long MAX_ALLOWED_DOWNLOAD_TO_STRING_LENGTH = 1024 * 64;

	static {
		final MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
		webClient = new HttpClient(connectionManager);
		webClient.getHttpConnectionManager().getParams().setSoTimeout(5000);
		webClient.getHttpConnectionManager().getParams().setConnectionTimeout(
				5000);
	}

	/**
	 * Perform a REST API request
	 * 
	 * @param requestUrl
	 * @param requestMethod
	 * @param requestContent
	 * @param requestHeaders
	 * @return response body
	 * @throws Exception
	 */
	public static String performRequest(URL requestUrl, String requestMethod,
			String requestContent, Map<String, String> requestHeaders)
			throws Exception {

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
	 * @throws Exception
	 */
	public static String performRequestShouldFail(URL requestUrl,
			String requestMethod, String requestContent,
			Map<String, String> requestHeaders,
			Integer overridingExpectedResponseStatus) throws Exception {
		return performRequest(requestUrl, requestMethod, requestContent,
				requestHeaders, overridingExpectedResponseStatus);
	}

	@SuppressWarnings("deprecation")
	private static String performRequest(URL requestUrl, String requestMethod,
			String requestContent, Map<String, String> requestHeaders,
			Integer overridingExpectedResponseStatus) throws HttpException, IOException, HttpClientHelperException {

		int defaultExpectedReponseStatus = 200;

		HttpMethodBase method = null;
		if (requestMethod.equals("GET")) {
			method = new GetMethod(requestUrl.getPath());
		} else if (requestMethod.equals("POST")) {
			method = new PostMethod(requestUrl.getPath());
			if (null != requestContent) {
				((EntityEnclosingMethod) method).setRequestBody(requestContent);
			}
			defaultExpectedReponseStatus = 201;
		} else if (requestMethod.equals("PUT")) {
			method = new PutMethod(requestUrl.getPath());
			if (null != requestContent) {
				((EntityEnclosingMethod) method).setRequestBody(requestContent);
			}
		} else if (requestMethod.equals("DELETE")) {
			method = new DeleteMethod(requestUrl.getPath());
			defaultExpectedReponseStatus = 204;
		}

		int expectedResponseStatus = (null == overridingExpectedResponseStatus) ? defaultExpectedReponseStatus
				: overridingExpectedResponseStatus;

		method.setQueryString(requestUrl.getQuery());

		for (Entry<String, String> header : requestHeaders.entrySet()) {
			method.addRequestHeader(header.getKey(), header.getValue());
		}

		HostConfiguration hostConfig = new HostConfiguration();
		hostConfig.setHost(requestUrl.getHost(), requestUrl.getPort(),
				requestUrl.getProtocol());

		int responseStatus = webClient.executeMethod(hostConfig, method);

		if (expectedResponseStatus != responseStatus) {
			StringBuilder verboseMessage = new StringBuilder(
					"FAILURE: Expected " + defaultExpectedReponseStatus
							+ " but got " + responseStatus + " for "
							+ requestUrl);
			if (0 < requestHeaders.size()) {
				verboseMessage.append("\nHeaders: ");
				for (Entry<String, String> entry : requestHeaders.entrySet()) {
					verboseMessage.append("\n\t" + entry.getKey() + ": " + entry.getValue());
				}
			}
			if (null != requestContent) {
				verboseMessage.append("\nRequest Content: " + requestContent);
			}
			verboseMessage.append("\nResponse Content: " + method.getResponseBodyAsString());
			throw new HttpClientHelperException(verboseMessage.toString(), method);
		}
		return method.getResponseBodyAsString();
	}

	/**
	 * TODO - better error handling - more useful error diagnostics such as the
	 * body of the error response
	 * 
	 * @param requestUrl
	 * @return the contents of the file in a string
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static String getFileContents(final String requestUrl)
			throws ClientProtocolException, IOException {
		String fileContents = null;

		HttpGet get = new HttpGet(requestUrl);
		DefaultHttpClient client = new DefaultHttpClient();
		HttpResponse response = client.execute(get);
		if (300 <= response.getStatusLine().getStatusCode()) {
			throw new IllegalArgumentException("Request(" + requestUrl
					+ ") failed: " + response.getStatusLine().getReasonPhrase());
		}
		HttpEntity fileEntity = response.getEntity();
		if (null != fileEntity) {
			if (MAX_ALLOWED_DOWNLOAD_TO_STRING_LENGTH < fileEntity
					.getContentLength()) {
				throw new IllegalArgumentException("Requested content("
						+ requestUrl + ") is too large("
						+ fileEntity.getContentLength()
						+ "), download it to a file instead");
			}
			fileContents = EntityUtils.toString(fileEntity);
		}
		return fileContents;
	}

	/**
	 * TODO - large downloads (e.g., 5TB download from S3) - multipart downloads
	 * - restartable downloads - more useful error diagnostics such as the body
	 * of the error response
	 * 
	 * @param requestUrl
	 * @param filePath
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static void downloadFile(final String requestUrl,
			final String filePath) throws ClientProtocolException, IOException {

		HttpGet get = new HttpGet(requestUrl);
		DefaultHttpClient client = new DefaultHttpClient();
		HttpResponse response = client.execute(get);
		if (300 <= response.getStatusLine().getStatusCode()) {
			throw new IllegalArgumentException("Request(" + requestUrl
					+ ") failed: " + response.getStatusLine().getReasonPhrase());
		}
		HttpEntity fileEntity = response.getEntity();
		if (null != fileEntity) {
			FileOutputStream fileOutputStream = new FileOutputStream(filePath);
			fileEntity.writeTo(fileOutputStream);
			fileOutputStream.close();
		}
	}
}
