package org.sagebionetworks.client;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
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
	public void setGlobalConnectionTimeout(int defaultTimeoutMsec) {
		HttpClientHelper.setGlobalConnectionTimeout(getSingleton(), defaultTimeoutMsec);
	}

	@Override
	public void setGlobalSocketTimeout(int defaultTimeoutMsec) {
		HttpClientHelper.setGlobalSocketTimeout(getSingleton(), defaultTimeoutMsec);
	}

	@Override
	public void uploadFile(String requestUrl, String filepath,
			String contentType, Map<String, String> requestHeaders)
			throws ClientProtocolException, IOException,
			HttpClientHelperException {
		HttpClientHelper.uploadFile(getSingleton(), requestUrl, filepath, contentType, requestHeaders);
	}

	@Override
	public HttpResponse performRequest(String requestUrl, String requestMethod,
			String requestContent, Map<String, String> requestHeaders)
			throws ClientProtocolException, IOException,
			HttpClientHelperException {
		return HttpClientHelper.performRequest(getSingleton(), requestUrl, requestMethod, requestContent, requestHeaders);
	}


	@Override
	public void downloadFile(String requestUrl, String filepath)
			throws ClientProtocolException, IOException,
			HttpClientHelperException {
		HttpClientHelper.downloadFile(getSingleton(), requestUrl, filepath);
	}

}
