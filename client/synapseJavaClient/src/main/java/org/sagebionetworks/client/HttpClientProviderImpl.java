package org.sagebionetworks.client;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;

/**
 * 
 * @author jmhill
 *
 */
public class HttpClientProviderImpl implements HttpClientProvider {

	@Override
	public void setGlobalConnectionTimeout(int defaultTimeoutMsec) {
		HttpClientHelper.setGlobalConnectionTimeout(defaultTimeoutMsec);	
	}

	@Override
	public void setGlobalSocketTimeout(int defaultTimeoutMsec) {
		HttpClientHelper.setGlobalSocketTimeout(defaultTimeoutMsec);
	}

	@Override
	public void uploadFile(String requestUrl, String filepath, String contentType,
			Map<String, String> requestHeaders) throws ClientProtocolException, IOException, HttpClientHelperException {
		HttpClientHelper.uploadFile(requestUrl, filepath, contentType, requestHeaders);
	}

	@Override
	public HttpResponse performRequest(String requestUrl, String requestMethod,
			String requestContent, Map<String, String> requestHeaders) throws ClientProtocolException, IOException, HttpClientHelperException {
		return HttpClientHelper.performRequest(requestUrl, requestMethod, requestContent, requestHeaders);
	}

}
