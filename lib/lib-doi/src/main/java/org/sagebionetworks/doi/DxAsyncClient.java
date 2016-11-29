package org.sagebionetworks.doi;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;

/**
 * Asynchronous client to the DOI name resolution service. This is to
 * check the "readiness" of the DOI.
 */
public class DxAsyncClient {

	public static final String DOI_API_HANDLES_URL = "http://doi.org/api/handles/";
	public static final String URL_PARAM = "?type=URL";
	private static final String VALUES = "values";
	private static final String DATA = "data";
	private static final String VALUE = "value";

	public DxAsyncClient() {
		this.simpleHttpClient = new SimpleHttpClientImpl();
		delay = 4 * 60 * 1000; // 4 minutes
		decay = 1 * 60 * 1000; // 1 minute
	}

	DxAsyncClient(long delay, long decay) {
		this.simpleHttpClient = new SimpleHttpClientImpl();
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
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					long start = System.currentTimeMillis();
					String location = resolveWithRetries(doi);
					long totalTime = System.currentTimeMillis() - start;
					if (location == null) {
						callback.onError(ezidDoi, new RuntimeException(
								"DOI " + doiString + " failed to resolve after "
								+ totalTime + " seconds."));
					} else {
						callback.onSuccess(ezidDoi);
					}
				} catch (InterruptedException | IOException e) {
					callback.onError(ezidDoi, e);
				}
			}
		});
	}

	private String resolveWithRetries(String doi) throws InterruptedException, ClientProtocolException, IOException {
		String location = null;
		long delay = this.delay;
		while (location == null && delay > 0) {
			Thread.sleep(delay);
			location = resolve(doi);
			delay = delay - decay;
		}
		return location;
	}

	/**
	 * Resolves the DOI to the location of the object.
	 *
	 * @return The location of the DOI or null if the DOI does not resolve
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws JSONException 
	 */
	private String resolve(String doi) throws ClientProtocolException, IOException {

		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(DOI_API_HANDLES_URL + doi + URL_PARAM);
		SimpleHttpResponse response = simpleHttpClient.get(request);

		final int status = response.getStatusCode();
		if (status == HttpStatus.SC_OK) {
			String location = getLocation(response.getContent());
			return location;
		}

		return null;
	}

	public static String getLocation(String content) {
		if (content == null) {
			return null;
		}
		try {
			JSONObject response = new JSONObject(content);
			return response.getJSONArray(VALUES).getJSONObject(0)
					.getJSONObject(DATA).getString(VALUE);
		} catch (JSONException e) {
			return null;
		}
	}

	// If the thread pool is to have more than 1 thread,
	// the blocking client must also use a pool of connections.
	// The blocking client currently uses SingleClientConnManager.
	private final ExecutorService executor = Executors.newFixedThreadPool(1);
	private final SimpleHttpClient simpleHttpClient;
	private final long delay;
	private final long decay;
}
