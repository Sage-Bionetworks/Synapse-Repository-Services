package org.sagebionetworks.doi;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.sagebionetworks.StackConfiguration;

/**
 * EZID DOI client.
 */
public class EzidClient implements DoiClient {

	private static final String REALM = "EZID";
	private static final Integer TIME_OUT = Integer.valueOf(9000); // 9 seconds
	private static final String USER_AGENT = "Synapse";
	private final HttpClient client;

	public EzidClient() {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		AuthScope authScope = new AuthScope(
				AuthScope.ANY_HOST, AuthScope.ANY_PORT, REALM, AuthPolicy.BASIC);
		final String username = StackConfiguration.getEzidUsername();
		final String password = StackConfiguration.getEzidPassword();
		Credentials credentials = new UsernamePasswordCredentials(username, password);
		httpClient.getCredentialsProvider().setCredentials(authScope, credentials);
		HttpParams params = httpClient.getParams();
		params.setParameter(CoreConnectionPNames.SO_TIMEOUT, TIME_OUT);
		params.setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);
		client = httpClient;
	}

	@Override
	public void create(EzidDoi doi) {

		if (doi == null) {
			throw new IllegalArgumentException("DOI cannot be null.");
		}

		URI uri = URI.create(StackConfiguration.getEzidUrl() + "id/" + doi.getDoi());
		HttpPut put = new HttpPut(uri);
		try {
			StringEntity requestEntity = new StringEntity(doi.getMetadata().getMetadataAsString(), HTTP.PLAIN_TEXT_TYPE, "UTF-8");
			put.setEntity(requestEntity);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		executeWithRetry(put);
	}

	/** Retries max 3 times with exponential backoff */
	private void executeWithRetry(HttpUriRequest request) {
		executeWithRetry(request, 0);
	}

	private void executeWithRetry(HttpUriRequest request, int retryCount) {

		// Pause to do the exponential backoff
		if (retryCount > 0) {
			long scale = 100L;
			long delay = (1L << retryCount) * scale; // 200, 400, 800
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		final HttpResponse response = execute(request);

		try {

			// Consume the response to close the connection
			final String responseStr = EntityUtils.toString(response.getEntity());

			// Success
			final int status = response.getStatusLine().getStatusCode();
			if (status == HttpStatus.SC_CREATED) {
				return;
			}

			// Retry 500 and 503 at most 3 times with exponential backoff
			if (status == HttpStatus.SC_INTERNAL_SERVER_ERROR
					|| status == HttpStatus.SC_SERVICE_UNAVAILABLE) {
				EntityUtils.consume(response.getEntity());
				if (isRetryable(request) && retryCount < 3) {
					retryCount++;
					executeWithRetry(request, retryCount);
				}
			}

			// Error
			String error = status + " " + response.getStatusLine().getReasonPhrase();
			error = " " + responseStr;
			if (status == HttpStatus.SC_BAD_REQUEST) {
				if (error.toLowerCase().contains("identifier already exists")) {
					return;
				}
			}
			throw new RuntimeException(error);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private HttpResponse execute(HttpUriRequest request) {
		try {
			HttpResponse response = client.execute(request);
			return response;
		} catch (ClientProtocolException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private boolean isRetryable(HttpRequest request) {
		if (request instanceof HttpEntityEnclosingRequest) {
			HttpEntity entity = ((HttpEntityEnclosingRequest)request).getEntity();
			if (entity != null && !entity.isRepeatable()) {
				return false;
			}
		}
		return true;
	}
}
