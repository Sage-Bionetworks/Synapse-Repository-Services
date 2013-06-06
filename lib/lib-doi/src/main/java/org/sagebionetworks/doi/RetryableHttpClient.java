package org.sagebionetworks.doi;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;

/**
 * Retries, on 500 INTERNAL_SERVER_ERROR and 503 SERVICE_UNAVAILABLE,
 * a HTTP request at most a number of times with exponential back-off
 */
class RetryableHttpClient {

	/** Retry at most 3 times with delays at 100 ms, 200 ms, 400 ms. */
	RetryableHttpClient(HttpClient httpClient) {

		if (httpClient == null) {
			throw new IllegalArgumentException("HTTP client cannot be null.");
		}

		this.httpClient = httpClient;
		this.firstRetryDelay = 100L;
		this.maxNumberOfRetries = 3;
	}

	/**
	 * @param firstRetryDelay The delay of the first retry in milliseconds. Delays of
	 * 							later retries are exponentially backed off.
	 */
	RetryableHttpClient(HttpClient httpClient, long firstRetryDelay, int maxNumberOfRetries) {

		if (httpClient == null) {
			throw new IllegalArgumentException("HTTP client cannot be null.");
		}
		if (firstRetryDelay <= 0) {
			throw new IllegalArgumentException(
					"Delay of first retry is out of range: " + firstRetryDelay);
		}

		this.httpClient = httpClient;
		this.firstRetryDelay = firstRetryDelay;
		this.maxNumberOfRetries = maxNumberOfRetries;
	}

	HttpResponse executeWithRetry(HttpUriRequest request) {
		return executeWithRetry(request, 0);
	}

	private HttpResponse executeWithRetry(HttpUriRequest request, int retryCount) {
		final HttpResponse response = execute(request);
		final int status = response.getStatusLine().getStatusCode();
		try {
			// Retry 500 and 503 at most 3 times with exponential backoff
			if (status == HttpStatus.SC_INTERNAL_SERVER_ERROR
					|| status == HttpStatus.SC_SERVICE_UNAVAILABLE) {
				if (isRetryable(request) && retryCount < maxNumberOfRetries) {
					// Consume the response and close the connection before retrying
					EntityUtils.consume(response.getEntity());
					retryCount++;
					pause(retryCount);
					return executeWithRetry(request, retryCount);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return response;
	}

	private HttpResponse execute(HttpUriRequest request) {
		try {
			HttpResponse response = httpClient.execute(request);
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

	private void pause(int retryCount) {
		// Compute the delay with exponential back-off
		long delay = (1L << retryCount) * firstRetryDelay;
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private final HttpClient httpClient;
	private final long firstRetryDelay; // in milliseconds
	private final int maxNumberOfRetries;
}
