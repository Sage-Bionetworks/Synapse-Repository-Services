package org.sagebionetworks.client;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.sagebionetworks.utils.HttpClientHelperException;

/**
 * Stub used to capture data sent to Synapse.
 * 
 * @author John
 *
 */
public class StubHttpClientProvider implements HttpClientProvider {
	
	Map<String, String> requestHeaders;
	HttpResponse response;
	/**
	 * Create a new stub with the expected response.
	 * @param response
	 */
	public StubHttpClientProvider(HttpResponse response){
		this.response = response;
	}

	@Override
	public void setGlobalConnectionTimeout(int defaultTimeoutMsec) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setGlobalSocketTimeout(int defaultTimeoutMsec) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void uploadFile(String requestUrl, String filepath,
			String contentType, Map<String, String> requestHeaders)
			throws ClientProtocolException, IOException,
			HttpClientHelperException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void putFile(String requestUrl, File toPut,
			Map<String, String> requestHeaders) throws ClientProtocolException,
			IOException, HttpClientHelperException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void downloadFile(String requestUrl, String filepath)
			throws ClientProtocolException, IOException,
			HttpClientHelperException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public HttpResponse performRequest(String string, String requestMethod,
			String requestContent, Map<String, String> requestHeaders)
			throws ClientProtocolException, IOException,
			HttpClientHelperException {
		// Capture the headers
		this.requestHeaders = requestHeaders;
		return response;
	}

	@Override
	public HttpResponse execute(HttpUriRequest request)
			throws ClientProtocolException, IOException {
		// We need to extract the headers for this case
		Header[] headers = request.getAllHeaders();
		this.requestHeaders = new HashMap<String, String>();
		if(headers != null){
			for(Header header: headers){
				requestHeaders.put(header.getName(), header.getValue());
			}
		}
		return response;
	}
	
	public Map<String, String> getSentRequestHeaders(){
		return this.requestHeaders;
	}

}
