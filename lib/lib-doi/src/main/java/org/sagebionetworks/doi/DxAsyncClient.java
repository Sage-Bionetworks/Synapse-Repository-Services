package org.sagebionetworks.doi;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * Asynchronous client to the DOI name resolution service. This is to
 * check the "readiness" of the DOI.
 */
public class DxAsyncClient {

	public DxAsyncClient() {
		HttpClient httpClient = new DefaultHttpClient();
		httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
		this.httpClient = new RetryableHttpClient(httpClient);
		delay = 4 * 60 * 1000; // 4 minutes
		decay = 1 * 60 * 1000; // 1 minute
	}

	DxAsyncClient(long delay, long decay) {
		HttpClient httpClient = new DefaultHttpClient();
		httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
		this.httpClient = new RetryableHttpClient(httpClient);
		this.delay = delay;
		this.decay = decay;
	}

	public void resolve(final EzidDoi ezidDoi, final DxAsyncCallback callback) {

		if (ezidDoi == null) {
			throw new IllegalArgumentException("EZID DOI cannot be null.");
		}
		if (callback == null) {
			throw new IllegalArgumentException("Callback cannot be null.");
		}

		// Validate the DOI string is in the correct format, e.g. 'doi:10.7303/syn1720822.1'
		final String doiString = ezidDoi.getDoi();
		if (doiString == null) {
			throw new IllegalArgumentException("The DOI string cannot be null.");
		}
		final String doiPrefix = "doi:";
		if (!doiString.startsWith(doiPrefix)) {
			throw new IllegalArgumentException(
					"The DOI string, " + doiString + ", does not start with " + doiPrefix);
		}

		final String doi = doiString.substring(doiPrefix.length());
		Callable<String> callable = new Callable<String> () {
			@Override
			public String call() {
				return resolve(doi);
			}};

		String location = null;
		long delay = this.delay;
		long start = System.currentTimeMillis();
		while (location == null && delay > 0) {
			try {
				Future<String> future = executor.schedule(callable, delay, TimeUnit.MILLISECONDS);
				location = future.get();
			} catch (ExecutionException e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				callback.onError(ezidDoi, e);
				return;
			} catch (InterruptedException e) {
				callback.onError(ezidDoi, e);
				return;
			}
			delay = delay - decay;
		}

		if (location == null) {
			long totalTime = (System.currentTimeMillis() - start) / 1000;
			callback.onError(ezidDoi, new RuntimeException(
					"DOI " + doiString + " failed to resolve after " + totalTime + " seconds."));
		} else {
			callback.onSuccess(ezidDoi);
		}
	}

	/**
	 * Resolves the DOI to the location of the object.
	 *
	 * @return The location of the DOI or null if the DOI does not resolve
	 */
	private String resolve(String doi) {

		URI uri = URI.create(EzidConstants.DX_URL + doi);
		HttpGet get = new HttpGet(uri);
		HttpResponse response = httpClient.executeWithRetry(get);

		try {
			// Consume the response to close the connection
			EntityUtils.toString(response.getEntity());
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		final int status = response.getStatusLine().getStatusCode();
		if (status == HttpStatus.SC_SEE_OTHER) {
			// Success
			Header header = response.getFirstHeader("Location");
			String location = header.getValue();
			return location;
		}

		return null;
	}

	// If the thread pool is to have more than 1 thread,
	// the blocking client must also use a pool of connections.
	// The blocking client currently uses SingleClientConnManager.
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	private final RetryableHttpClient httpClient;
	private final long delay;
	private final long decay;
}
