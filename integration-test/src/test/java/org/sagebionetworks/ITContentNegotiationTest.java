package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.VersionInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Test for https://sagebionetworks.jira.com/browse/PLFM-6435
 * 
 * @author Marco Marasca
 */
public class ITContentNegotiationTest {

	private static final String repoEndpoint = StackConfigurationSingleton.singleton().getRepositoryServiceEndpoint();
	private static RestTemplate restTemplate;

	@BeforeAll
	public static void beforeAll() {
		restTemplate = new RestTemplate();
	}
	
	@Test
	public void testUnsupportedAcceptHeader() throws SynapseException, URISyntaxException {
		RequestEntity<?> request = RequestEntity
			     .get(new URI(repoEndpoint + "/version"))
			     .accept(MediaType.APPLICATION_XML)
			     .build();
				
		HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
			// Call under test
			restTemplate.exchange(request, VersionInfo.class);
		});
		
		assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getStatusCode());
		assertTrue(ex.getMessage().contains("Could not find acceptable representation"));
		assertTrue(MediaType.APPLICATION_JSON.isCompatibleWith(ex.getResponseHeaders().getContentType()));
	}
	
	@Test
	public void testMalformedAcceptHeader() throws SynapseException, URISyntaxException {
		RequestEntity<?> request = RequestEntity
			     .get(new URI(repoEndpoint + "/version"))
			     .header(HttpHeaders.ACCEPT, "malformed")
			     .build();
				
		HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
			// Call under test
			restTemplate.exchange(request, VersionInfo.class);
		});
		
		assertEquals(HttpStatus.NOT_ACCEPTABLE, ex.getStatusCode());
		assertTrue(ex.getMessage().contains("Could not parse 'Accept' header [malformed]: Invalid mime type \\\"malformed\\\": does not contain '/'"));
		assertTrue(MediaType.APPLICATION_JSON.isCompatibleWith(ex.getResponseHeaders().getContentType()));
	}

	@Test
	public void testUnsupportedAcceptHeaderOnException() throws SynapseException, URISyntaxException {
		// Uses a POST that is not supported with an unsupported accept header, this should still return the 405 from the method not allowed
		RequestEntity<?> request = RequestEntity
			     .post(new URI(repoEndpoint + "/version"))
			     .accept(MediaType.APPLICATION_XML)
			     .build();
				
		HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
			// Call under test
			restTemplate.exchange(request, VersionInfo.class);
		});
		
		assertEquals(HttpStatus.METHOD_NOT_ALLOWED, ex.getStatusCode());
		assertTrue(ex.getMessage().contains("Request method 'POST' not supported"));
		assertTrue(MediaType.APPLICATION_JSON.isCompatibleWith(ex.getResponseHeaders().getContentType()));
	}
	
	@Test
	public void testSupportedAcceptHeaderOnException() throws SynapseException, URISyntaxException {
		// Uses a POST that is not supported with a supported (different from json accept header), this should still return the 405 from the method not allowed 
		// and the content type should be respected
		RequestEntity<?> request = RequestEntity
			     .post(new URI(repoEndpoint + "/version"))
			     .accept(MediaType.TEXT_PLAIN)
			     .build();
				
		HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
			// Call under test
			restTemplate.exchange(request, VersionInfo.class);
		});
		
		assertEquals(HttpStatus.METHOD_NOT_ALLOWED, ex.getStatusCode());
		assertTrue(ex.getMessage().contains("Request method 'POST' not supported"));
		assertTrue(MediaType.TEXT_PLAIN.isCompatibleWith(ex.getResponseHeaders().getContentType()));
	}
	
}
