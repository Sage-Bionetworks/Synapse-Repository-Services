package org.sagebionetworks.utils;

import org.apache.http.client.HttpClient;

/**
 * The default HTTP client singleton.
 * 
 * @author jmhill
 *
 */
public class DefaultHttpClientSingleton {

	/**
	 * Follows the Lazy Loaded singleton pattern (see: http://stackoverflow.com/questions/70689/efficient-way-to-implement-singleton-pattern-in-java)
	 *
	 */
	private static class SingletonHolder {
		// This client will verify all certificates and host names.
		private static final boolean  verifySSLCertificates = true;
		private static final HttpClient singleton = HttpClientHelper.createNewClient(verifySSLCertificates);
	}
	
	/**
	 * Get the singleton instance.
	 * @return
	 */
	public static HttpClient getInstance(){
		return SingletonHolder.singleton;
	}
}
