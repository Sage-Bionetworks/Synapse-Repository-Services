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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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
import org.sagebionetworks.repo.model.doi.Doi;

/**
 * EZID DOI client.
 */
public class EzidClient implements DoiClient {

	private static final String REALM = "EZID";
	private static final Integer TIME_OUT = Integer.valueOf(9000); // 9 seconds
	private static final String USER_AGENT = "Synapse";
	private final HttpClient writeClient;
	private final HttpClient readClient;

	public EzidClient() {
		// Write client needs to set up authentication
		DefaultHttpClient httpClient = new DefaultHttpClient();
		AuthScope authScope = new AuthScope(
				AuthScope.ANY_HOST, AuthScope.ANY_PORT, REALM, AuthPolicy.BASIC);
		final String username = EzidConstants.EZID_USERNAME;
		final String password = EzidConstants.EZID_PASSWORD;
		Credentials credentials = new UsernamePasswordCredentials(username, password);
		httpClient.getCredentialsProvider().setCredentials(authScope, credentials);
		HttpParams params = httpClient.getParams();
		params.setParameter(CoreConnectionPNames.SO_TIMEOUT, TIME_OUT);
		params.setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);
		writeClient = httpClient;
		// Read client does not need authentication
		readClient = new DefaultHttpClient();
		readClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, TIME_OUT);
		readClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);
	}

	@Override
	public EzidDoi get(final String doi, final Doi doiDto) {

		if (doi == null) {
			throw new IllegalArgumentException("DOI string cannot be null.");
		}
		if (doiDto == null) {
			throw new IllegalArgumentException("DOI DTO cannot be null.");
		}

		URI uri = URI.create(EzidConstants.EZID_URL + "id/" + doi);
		HttpGet get = new HttpGet(uri);
		String response = executeWithRetry(get);
		EzidDoi result = new EzidDoi();
		result.setDoi(doi);
		result.setDto(doiDto);
		EzidMetadata metadata = new EzidMetadata();
		metadata.initFromString(response);
		result.setMetadata(metadata);
		return result;
	}

	@Override
	public void create(final EzidDoi doi) {

		if (doi == null) {
			throw new IllegalArgumentException("DOI cannot be null.");
		}

		URI uri = URI.create(EzidConstants.EZID_URL + "id/" + doi.getDoi());
		HttpPut put = new HttpPut(uri);
		try {
			StringEntity requestEntity = new StringEntity(doi.getMetadata().getMetadataAsString(), HTTP.PLAIN_TEXT_TYPE, "UTF-8");
			put.setEntity(requestEntity);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		executeWithRetry(put);
	}

	@Override
	public void update(EzidDoi doi) {

		if (doi == null) {
			throw new IllegalArgumentException("DOI cannot be null.");
		}

		URI uri = URI.create(EzidConstants.EZID_URL + "id/" + doi.getDoi());
		HttpPost post = new HttpPost(uri);
		try {
			StringEntity requestEntity = new StringEntity(doi.getMetadata().getMetadataAsString(), HTTP.PLAIN_TEXT_TYPE, "UTF-8");
			post.setEntity(requestEntity);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		executeWithRetry(post);
	}

	/** Retries max 3 times with exponential backoff */
	private String executeWithRetry(HttpUriRequest request) {
		return executeWithRetry(request, 0);
	}

	private String executeWithRetry(HttpUriRequest request, int retryCount) {

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
			if (status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED) {
				return responseStr;
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
			error = error + " " + responseStr;
			if (status == HttpStatus.SC_BAD_REQUEST) {
				if (error.toLowerCase().contains("identifier already exists")) {
					return responseStr;
				}
			}
			throw new RuntimeException(error);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private HttpResponse execute(HttpUriRequest request) {
		try {
			HttpResponse response = writeClient.execute(request);
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
