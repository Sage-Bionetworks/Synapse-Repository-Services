package org.sagebionetworks.doi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Calendar;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HTTP;
import org.junit.Test;
import org.sagebionetworks.StackConfiguration;

public class EzidClientTest {

	@Test
	public void testConstructor() throws Exception {
		DoiClient client = new EzidClient();
		assertNotNull(client);
		Field field = EzidClient.class.getDeclaredField("client");
		assertNotNull(field);
		field.setAccessible(true);
		DefaultHttpClient httpClient = (DefaultHttpClient)field.get(client);
		assertNotNull(httpClient);
		assertEquals(Integer.valueOf(9000), httpClient.getParams().getParameter(CoreConnectionPNames.SO_TIMEOUT));
		assertEquals("Synapse", httpClient.getParams().getParameter(CoreProtocolPNames.USER_AGENT));
		AuthScope authScope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, "EZID", AuthPolicy.BASIC);
		Credentials credentials = httpClient.getCredentialsProvider().getCredentials(authScope);
		assertNotNull(credentials);
		assertEquals(StackConfiguration.getEzidUsername(), credentials.getUserPrincipal().getName());
		assertEquals(StackConfiguration.getEzidPassword(), credentials.getPassword());
	}

	@Test
	public void testCreate() throws Exception {
		final EzidMetadata metadata = new EzidMetadata();
		// Create a random ID in the EZID test "domain"
		String id = Integer.toHexString(random.nextInt());
		final String doi = "doi:10.5072/FK2." + id;
		metadata.setDoi(doi);
		final String target = "https://synapse.sagebase.org/";
		metadata.setTarget(target);
		final String creator = "Test, Something";
		metadata.setCreator(creator);
		final String title = "This is a test";
		metadata.setTitle(title);
		final String publisher = "Sage Bionetworks";
		metadata.setPublisher(publisher);
		final int year = Calendar.getInstance().get(Calendar.YEAR);
		metadata.setPublicationYear(year);
		// Create
		DoiClient client = new EzidClient();
		client.create(metadata);
	}

	@Test(expected=RuntimeException.class)
	public void testCreateWithException() throws Exception {
		final EzidMetadata metadata = new EzidMetadata();
		String id = Integer.toHexString(random.nextInt());
		// Invalid doi
		final String doi = "doi:10.99999/test.invalid." + id;
		metadata.setDoi(doi);
		final String target = "https://synapse.sagebase.org/";
		metadata.setTarget(target);
		final String creator = "Test, Something";
		metadata.setCreator(creator);
		final String title = "This is a test";
		metadata.setTitle(title);
		final String publisher = "Sage Bionetworks";
		metadata.setPublisher(publisher);
		final int year = Calendar.getInstance().get(Calendar.YEAR);
		metadata.setPublicationYear(year);
		// Create
		DoiClient client = new EzidClient();
		client.create(metadata);
	}

	@Test
	public void testCreateRetry() throws Exception {

		// Create test data
		final EzidMetadata metadata = new EzidMetadata();
		String id = Integer.toHexString(random.nextInt());
		final String doi = "doi:10.5072/FK2." + id;
		metadata.setDoi(doi);
		final String target = "https://synapse.sagebase.org/";
		metadata.setTarget(target);
		final String creator = "Test, Something";
		metadata.setCreator(creator);
		final String title = "This is a test";
		metadata.setTitle(title);
		final String publisher = "Sage Bionetworks";
		metadata.setPublisher(publisher);
		final int year = Calendar.getInstance().get(Calendar.YEAR);
		metadata.setPublicationYear(year);

		// Create request from the test data
		URI uri = URI.create(StackConfiguration.getEzidUrl() + "id/" + metadata.getDoi());
		HttpPut httpPut = new HttpPut(uri);
		StringEntity requestEntity = new StringEntity(metadata.getMetadataAsString(), HTTP.PLAIN_TEXT_TYPE, "UTF-8");
		httpPut.setEntity(requestEntity);

		// Mock 503
		StatusLine mockStatus = mock(StatusLine.class);
		when(mockStatus.getStatusCode()).thenReturn(HttpStatus.SC_SERVICE_UNAVAILABLE);
		HttpResponse mockResponse = mock(HttpResponse.class);
		when(mockResponse.getStatusLine()).thenReturn(mockStatus);
		HttpEntity httpEntity = new StringEntity("503 error");
		when(mockResponse.getEntity()).thenReturn(httpEntity);
		HttpClient mockClient = mock(HttpClient.class);
		when(mockClient.execute(httpPut)).thenReturn(mockResponse);

		// "Inject" the mock client
		DoiClient doiClient = new EzidClient();
		assertNotNull(doiClient);
		Field field = EzidClient.class.getDeclaredField("client");
		assertNotNull(field);
		field.setAccessible(true);
		field.set(doiClient, mockClient);
		Method method = EzidClient.class.getDeclaredMethod("executeWithRetry", HttpUriRequest.class);
		method.setAccessible(true);

		try {
			method.invoke(doiClient, httpPut);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			assertTrue(cause instanceof RuntimeException);
			assertTrue(cause.getMessage().contains("503"));
		}

		verify(mockClient.execute(httpPut), times(3));
	}

	private final Random random = new Random();
}
