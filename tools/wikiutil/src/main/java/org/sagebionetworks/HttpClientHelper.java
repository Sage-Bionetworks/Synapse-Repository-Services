package org.sagebionetworks;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;

/**
 */
public class HttpClientHelper {

	private static final HttpClient webClient;

	static {
		final MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
		webClient = new HttpClient(connectionManager);
		webClient.getHttpConnectionManager().getParams().setSoTimeout(5000);
		webClient.getHttpConnectionManager().getParams().setConnectionTimeout(
				5000);
	}

	/**
	 * @param requestUrl
	 * @param requestMethod
	 * @param requestContent
	 * @param requestHeaders
	 * @return response body
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	public static String performRequest(URL requestUrl, String requestMethod,
			String requestContent, Map<String,String> requestHeaders) throws Exception {

		int expectedReponseStatus = 200;
		
		HttpMethodBase method = null;
		if (requestMethod.equals("GET")) {
			method = new GetMethod(requestUrl.getPath());
		} else if (requestMethod.equals("POST")) {
			method = new PostMethod(requestUrl.getPath());
			if (null != requestContent) {
				((EntityEnclosingMethod) method).setRequestBody(requestContent);
			}
		} else if (requestMethod.equals("PUT")) {
			method = new PutMethod(requestUrl.getPath());
			if (null != requestContent) {
				((EntityEnclosingMethod) method).setRequestBody(requestContent);
			}
			expectedReponseStatus = 201;
		} else if (requestMethod.equals("DELETE")) {
			method = new DeleteMethod(requestUrl.getPath());
			expectedReponseStatus = 204;
		}

		method.setQueryString(requestUrl.getQuery());

		for(Entry<String, String> header : requestHeaders.entrySet()) {
			method.addRequestHeader(header.getKey(), header.getValue());
		}

		HostConfiguration hostConfig = new HostConfiguration();
		hostConfig.setHost(requestUrl.getHost(), requestUrl.getPort(), requestUrl.getProtocol());
		
		int responseStatus = webClient.executeMethod(hostConfig, method);
		assertEquals(expectedReponseStatus, responseStatus);

		return method.getResponseBodyAsString();
	}
}
