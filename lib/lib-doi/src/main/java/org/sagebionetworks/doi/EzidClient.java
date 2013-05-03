package org.sagebionetworks.doi;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
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
	private final RetryableHttpClient writeClient;
	private final RetryableHttpClient readClient;

	public EzidClient() {
		// Write client needs to set up authentication
		final DefaultHttpClient httpClientW = new DefaultHttpClient();
		AuthScope authScope = new AuthScope(
				AuthScope.ANY_HOST, AuthScope.ANY_PORT, REALM, AuthPolicy.BASIC);
		final String username = EzidConstants.EZID_USERNAME;
		final String password = EzidConstants.EZID_PASSWORD;
		Credentials credentials = new UsernamePasswordCredentials(username, password);
		httpClientW.getCredentialsProvider().setCredentials(authScope, credentials);
		HttpParams params = httpClientW.getParams();
		params.setParameter(CoreConnectionPNames.SO_TIMEOUT, TIME_OUT);
		params.setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);
		writeClient = new RetryableHttpClient(httpClientW);
		// Read client does not need authentication
		final DefaultHttpClient httpClientR = new DefaultHttpClient();
		httpClientR.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, TIME_OUT);
		httpClientR.getParams().setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);
		readClient = new RetryableHttpClient(httpClientR);
	}

	@Override
	public EzidDoi get(final EzidDoi ezidDoi) {

		if (ezidDoi == null) {
			throw new IllegalArgumentException("EZID DOI cannot be null.");
		}
		final String doi = ezidDoi.getDoi();
		if (doi == null) {
			throw new IllegalArgumentException("DOI string cannot be null.");
		}
		final Doi doiDto = ezidDoi.getDto();
		if (doiDto == null) {
			throw new IllegalArgumentException("DOI DTO cannot be null.");
		}

		URI uri = URI.create(EzidConstants.EZID_URL + "id/" + doi);
		HttpGet get = new HttpGet(uri);
		HttpResponse response = readClient.executeWithRetry(get);

		String responseString = "";
		try {
			// Must consume the response to close the connection
			responseString = EntityUtils.toString(response.getEntity());
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		final int status = response.getStatusLine().getStatusCode();
		if (status != HttpStatus.SC_OK) {
			// If the doi does not exist, EZID does not return
			// HttpStatus.SC_NOT_FOUND as of now. Instead it returns
			// HttpStatus.SC_BAD_REQUEST "no such identifier".
			String error = status + " ";
			error = error + " " + responseString;
			throw new RuntimeException(error);
		}

		EzidDoi result = new EzidDoi();
		result.setDoi(doi);
		result.setDto(doiDto);
		EzidMetadata metadata = new EzidMetadata();
		metadata.initFromString(responseString);
		result.setMetadata(metadata);
		return result;
	}

	@Override
	public void create(final EzidDoi ezidDoi) {

		if (ezidDoi == null) {
			throw new IllegalArgumentException("DOI cannot be null.");
		}

		URI uri = URI.create(EzidConstants.EZID_URL + "id/" + ezidDoi.getDoi());
		HttpPut put = new HttpPut(uri);
		try {
			StringEntity requestEntity = new StringEntity(
					ezidDoi.getMetadata().getMetadataAsString(), HTTP.PLAIN_TEXT_TYPE, "UTF-8");
			put.setEntity(requestEntity);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		HttpResponse response = writeClient.executeWithRetry(put);

		String responseString = "";
		try {
			// Must consume the response to close the connection
			responseString = EntityUtils.toString(response.getEntity());
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		final int status = response.getStatusLine().getStatusCode();
		if (status != HttpStatus.SC_CREATED) {
			if (status == HttpStatus.SC_BAD_REQUEST) {
				if (responseString.toLowerCase().contains("identifier already exists")) {
					return;
				}
				try {
					get(ezidDoi);
					return; // Already exists
				} catch (RuntimeException e) {
					String error = "DOI " + ezidDoi.getDoi();
					error += " got 400 BAD_REQUEST but does not already exits.";
					throw new RuntimeException(error);
				}
			}
			String error = status + " ";
			error = error + " " + responseString;
			throw new RuntimeException(error);
		}
	}

	@Override
	public void update(EzidDoi ezidDoi) {

		if (ezidDoi == null) {
			throw new IllegalArgumentException("DOI cannot be null.");
		}

		URI uri = URI.create(EzidConstants.EZID_URL + "id/" + ezidDoi.getDoi());
		HttpPost post = new HttpPost(uri);
		try {
			StringEntity requestEntity = new StringEntity(
					ezidDoi.getMetadata().getMetadataAsString(),
					HTTP.PLAIN_TEXT_TYPE, "UTF-8");
			post.setEntity(requestEntity);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		HttpResponse response = writeClient.executeWithRetry(post);

		try {
			// Must consume the response to close the connection
			final String responseStr = EntityUtils.toString(response.getEntity());
			final int status = response.getStatusLine().getStatusCode();
			if (status != HttpStatus.SC_OK) {
				String error = status + " ";
				error = error + " " + responseStr;
				throw new RuntimeException(error);
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
