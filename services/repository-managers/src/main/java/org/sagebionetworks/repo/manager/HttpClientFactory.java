package org.sagebionetworks.repo.manager;

import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientConfig;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientImpl;

public class HttpClientFactory {
	private static SimpleHttpClient client;
	
	private static final Integer TIME_OUT = 30 * 1000; // 30 seconds

	static {
		SimpleHttpClientConfig httpClientConfig = new SimpleHttpClientConfig();
		httpClientConfig.setSocketTimeoutMs(TIME_OUT);
		client = new SimpleHttpClientImpl(httpClientConfig);
	}

	public static SimpleHttpClient createHttpClient() {
		return client;
	}

}
