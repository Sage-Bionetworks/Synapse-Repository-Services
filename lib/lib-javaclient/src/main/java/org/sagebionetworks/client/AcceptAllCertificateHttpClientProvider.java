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
 * This client provider will accept all SSL certificates.  This should not be your first choice!
 * Only use this client if you trust the HTTPS URL to be used.
 * 
 * @author jmhill
 *
 */
public class AcceptAllCertificateHttpClientProvider implements HttpClientProvider {
	
	/**
	 * Follows the Lazy Loaded singleton pattern (see: http://stackoverflow.com/questions/70689/efficient-way-to-implement-singleton-pattern-in-java)
	 *
	 */
	private static class SingletonHolder {
		// This client will allow any certificate.
		private static final boolean  verifySSLCertificates = false;
		private static final HttpClient singleton = HttpClientHelper.createNewClient(verifySSLCertificates);
	}
	
	/**
	 * Get the client singleton.
	 * @return
	 */
	private HttpClient getSingleton(){
		return SingletonHolder.singleton;
	}

	
	@Override
	public HttpResponse performRequest(String requestUrl, String requestMethod,
			String requestContent, Map<String, String> requestHeaders)
			throws ClientProtocolException, IOException {
		return HttpClientHelper.performRequest(getSingleton(), requestUrl, requestMethod, requestContent, requestHeaders);
	}


	@Override
	public void downloadFile(String requestUrl, String filepath, Map<String, String> headers)
			throws ClientProtocolException, IOException,
			HttpClientHelperException {
		HttpClientHelper.downloadFile(getSingleton(), requestUrl, filepath, headers);
	}



	@Override
	public void putFile(String requestUrl, File toPut,
			Map<String, String> requestHeaders) throws ClientProtocolException,
			IOException, HttpClientHelperException {
		HttpClient client = getSingleton();
		FileInputStream in = new FileInputStream(toPut);
		HttpClientHelper.putStream(client, requestUrl, in, toPut.length(), requestHeaders);
	}


	@Override
	public HttpResponse execute(HttpUriRequest request)
			throws ClientProtocolException, IOException {
		HttpClient client = DefaultHttpClientSingleton.getInstance();
		return client.execute(request);
	}

}
