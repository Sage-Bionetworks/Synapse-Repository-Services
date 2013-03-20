package org.sagebionetworks.doi;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
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
	private static final Integer TIME_OUT = Integer.valueOf(6000); // 6 seconds
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
	public void create(EzidMetadata metadata) {

		if (metadata == null) {
			throw new IllegalArgumentException("DOI metadata cannot be null.");
		}

		URI uri = URI.create(StackConfiguration.getEzidUrl() + "id/" + metadata.getDoi());
		HttpPut put = new HttpPut(uri);
		try {
			StringEntity requestEntity = new StringEntity(metadata.getMetadataAsString(), HTTP.PLAIN_TEXT_TYPE, "UTF-8");
			put.setEntity(requestEntity);
			HttpResponse response = client.execute(put);
			int status = response.getStatusLine().getStatusCode();
			if( (status / 100) != 2) {
				HttpEntity responseEntity = response.getEntity();
				String error = EntityUtils.toString(responseEntity);
				error = "HTTP Error: " + status + " Details: " + error;
				throw new RuntimeException(error);
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (ClientProtocolException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
