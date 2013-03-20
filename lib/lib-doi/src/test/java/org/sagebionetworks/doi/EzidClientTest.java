package org.sagebionetworks.doi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Random;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
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
		assertEquals(Integer.valueOf(6000), httpClient.getParams().getParameter(CoreConnectionPNames.SO_TIMEOUT));
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
		// This is an EZID test "domain"
		// Salt it with a random id
		Random r = new Random();
		String id = Integer.toHexString(r.nextInt());
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
}
