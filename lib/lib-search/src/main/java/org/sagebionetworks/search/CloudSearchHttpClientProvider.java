package org.sagebionetworks.search;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.utils.HttpClientHelper;

public class CloudSearchHttpClientProvider {
	
	private Long connectionTimeout;
	private Long socketTimeout;
	private HttpClient httpClient;
	
	public CloudSearchHttpClientProvider() {
		init();
	}
	
	public void setConnectionTimeout(Long n) {
		connectionTimeout =  n;
	}
	
	public void setSocketTimeout(Long n) {
		socketTimeout = n;
	}
	
	public HttpClient getHttpClient() {
		if (httpClient == null) {
			init();
		}
		return httpClient;
	}
	
	private void init() {
		ThreadSafeClientConnManager connectionManager;
		try {
			if (connectionTimeout == null) {
				connectionTimeout = 20*1000L;
			}
			if (socketTimeout == null) {
				socketTimeout = 20*1000L;
			}
			
			connectionManager = HttpClientHelper.createClientConnectionManager(true);
			connectionManager.setDefaultMaxPerRoute(StackConfiguration.getHttpClientMaxConnsPerRoute());
			HttpParams clientParams = new BasicHttpParams();
			clientParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,connectionTimeout);
			clientParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, socketTimeout);
			httpClient = new DefaultHttpClient(connectionManager, clientParams);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
