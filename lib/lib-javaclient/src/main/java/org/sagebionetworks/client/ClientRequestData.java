package org.sagebionetworks.client;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;

public class ClientRequestData {

	//input
	String requestUrl, requestMethod, requestContent;
	Map<String, String> requestHeaders;
	
	//output
	AtomicReference<HttpResponse> response = new AtomicReference<HttpResponse>();
	AtomicReference<ClientProtocolException> clientProtocolException = new AtomicReference<ClientProtocolException>();
	AtomicReference<IOException> ioException = new AtomicReference<IOException>();
	
	public ClientRequestData(String requestUrl, String requestMethod,
			String requestContent, Map<String, String> requestHeaders) {
		super();
		this.requestUrl = requestUrl;
		this.requestMethod = requestMethod;
		this.requestContent = requestContent;
		this.requestHeaders = requestHeaders;
	}

}
