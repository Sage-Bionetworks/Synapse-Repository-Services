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
	
	HttpClient httpClient;
	
	public HttpClientProviderImpl() {
		httpClient = HttpClientHelper.createNewClient(true);
	}
	
	public HttpClientProviderImpl(int connectTimeout, int socketTimeout) {
		httpClient = HttpClientHelper.createNewClient(true, connectTimeout, socketTimeout);
	}

	@Override
	public HttpResponse performRequest(String requestUrl, String requestMethod,
			String requestContent, Map<String, String> requestHeaders) throws ClientProtocolException, IOException {
		return HttpClientHelper.performRequest(httpClient, requestUrl, requestMethod, requestContent, requestHeaders);
	}

	@Override
	public void downloadFile(String requestUrl, String filepath, Map<String,String> headers) 
			throws ClientProtocolException, IOException, HttpClientHelperException {
		HttpClientHelper.downloadFile(httpClient, requestUrl, filepath, headers);
	}


	@Override
	public void putFile(String requestUrl, File toPut,
			Map<String, String> requestHeaders) throws ClientProtocolException,
			IOException, HttpClientHelperException {
		HttpClient client = httpClient;
		FileInputStream in = new FileInputStream(toPut);
		HttpClientHelper.putStream(client, requestUrl, in, toPut.length(), requestHeaders);
	}

	@Override
	public HttpResponse execute(HttpUriRequest request) throws ClientProtocolException, IOException {
		HttpClient client = httpClient;
		// Used to debug connnection counts
//		ThreadSafeClientConnManager poolManager = (ThreadSafeClientConnManager) client.getConnectionManager();
//		System.out.println("Used connections: "+poolManager.getConnectionsInPool());
		return client.execute(request);
	}

}
