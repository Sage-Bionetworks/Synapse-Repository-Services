package org.sagebionetworks.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.sagebionetworks.utils.DefaultHttpClientSingleton;
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
		HttpClientHelper.setGlobalConnectionTimeout(DefaultHttpClientSingleton.getInstance(), defaultTimeoutMsec);	
	}

	@Override
	public void setGlobalSocketTimeout(int defaultTimeoutMsec) {
		HttpClientHelper.setGlobalSocketTimeout(DefaultHttpClientSingleton.getInstance(), defaultTimeoutMsec);
	}

	@Override
	public void uploadFile(String requestUrl, String filepath, String contentType,
			Map<String, String> requestHeaders) throws ClientProtocolException, IOException, HttpClientHelperException {
		HttpClientHelper.uploadFile(DefaultHttpClientSingleton.getInstance(), requestUrl, filepath, contentType, requestHeaders);
	}

	@Override
	public HttpResponse performRequest(String requestUrl, String requestMethod,
			String requestContent, Map<String, String> requestHeaders) throws ClientProtocolException, IOException, HttpClientHelperException {
		return HttpClientHelper.performRequest(DefaultHttpClientSingleton.getInstance(), requestUrl, requestMethod, requestContent, requestHeaders);
	}

	@Override
	public void downloadFile(String requestUrl, String filepath)throws ClientProtocolException, IOException, HttpClientHelperException {
		HttpClientHelper.downloadFile(DefaultHttpClientSingleton.getInstance(), requestUrl, filepath);
	}


	@Override
	public void putFile(String requestUrl, File toPut,
			Map<String, String> requestHeaders) throws ClientProtocolException,
			IOException, HttpClientHelperException {
		HttpClient client = DefaultHttpClientSingleton.getInstance();
		FileInputStream in = new FileInputStream(toPut);
		HttpClientHelper.putStream(client, requestUrl, in, toPut.length(), requestHeaders);
	}

	@Override
	public HttpResponse execute(HttpUriRequest request) throws ClientProtocolException, IOException {
		HttpClient client = DefaultHttpClientSingleton.getInstance();
		return client.execute(request);
	}

}
