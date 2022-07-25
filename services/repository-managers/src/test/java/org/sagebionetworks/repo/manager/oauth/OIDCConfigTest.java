package org.sagebionetworks.repo.manager.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class OIDCConfigTest {
	
	@Mock
	private SimpleHttpClient mockHttpClient;
	
	@Mock
	private SimpleHttpResponse mockResponse;
	
	private String uri;
	
	private OIDCConfig config;
	
	@BeforeEach
	public void before() {
		MockitoAnnotations.initMocks(this);
		
		uri = "https://baseUrl.org";
		config = new OIDCConfig(mockHttpClient, uri);
	}

	@Test
	public void testGetConfigProperty() throws IOException {
		String propertyName = "propertyName";
		String propertyValue = "propertyValue";
		
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK.value());
		when(mockResponse.getContent()).thenReturn("{\"propertyName\":\"propertyValue\"}");
		when(mockHttpClient.get(any())).thenReturn(mockResponse);
		
		// Call under test
		String result = config.getConfigProperty(propertyName);
		
		assertEquals(propertyValue, result);
		
		SimpleHttpRequest expectedRequest = new SimpleHttpRequest();
		expectedRequest.setUri(uri);
		
		verify(mockHttpClient).get(expectedRequest);
		
		// Calling again should not send another request
		config.getConfigProperty(propertyName);
		
		verifyNoMoreInteractions(mockHttpClient);
	}
	
	@Test
	public void testGetConfigPropertyWithIOExcetion() throws IOException {
		String propertyName = "propertyName";
		
		IOException ex = new IOException("Something went wrong");
		
		when(mockHttpClient.get(any())).thenThrow(ex);
		
		IllegalStateException result = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			config.getConfigProperty(propertyName);
		});
		
		assertEquals("Could not fetch discovery document from " + uri + ": Something went wrong", result.getMessage());
		assertEquals(ex, result.getCause());
				
		SimpleHttpRequest expectedRequest = new SimpleHttpRequest();
		expectedRequest.setUri(uri);
		
		verify(mockHttpClient).get(expectedRequest);
	}
	
	@Test
	public void testGetConfigPropertyWithBadStatus() throws IOException {
		String propertyName = "propertyName";
		
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST.value());
		when(mockResponse.getStatusReason()).thenReturn("Something went wrong");
		when(mockHttpClient.get(any())).thenReturn(mockResponse);
		
		IllegalStateException result = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			config.getConfigProperty(propertyName);
		});
		
		assertEquals("Could not fetch discovery document: 400 (Reason: Something went wrong)", result.getMessage());
				
		SimpleHttpRequest expectedRequest = new SimpleHttpRequest();
		expectedRequest.setUri(uri);
		
		verify(mockHttpClient).get(expectedRequest);
	}
	
	@Test
	public void testGetConfigPropertyWithBadStatusNoReason() throws IOException {
		String propertyName = "propertyName";
		
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST.value());
		when(mockHttpClient.get(any())).thenReturn(mockResponse);
		
		IllegalStateException result = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			config.getConfigProperty(propertyName);
		});
		
		assertEquals("Could not fetch discovery document: 400", result.getMessage());
				
		SimpleHttpRequest expectedRequest = new SimpleHttpRequest();
		expectedRequest.setUri(uri);
		
		verify(mockHttpClient).get(expectedRequest);
	}
	
	@Test
	public void testGetConfigPropertyWithNoProperty() throws IOException {
		String propertyName = "propertyName";
		
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK.value());
		when(mockResponse.getContent()).thenReturn("{\"differentPropertyName\":\"propertyValue\"}");
		when(mockHttpClient.get(any())).thenReturn(mockResponse);
		
		IllegalStateException result = assertThrows(IllegalStateException.class, () -> {		
			// Call under test
			config.getConfigProperty(propertyName);
		});
		
		assertEquals("Could not fetch property name propertyName from discovery document: {\"differentPropertyName\":\"propertyValue\"}", result.getMessage());
		
		SimpleHttpRequest expectedRequest = new SimpleHttpRequest();
		expectedRequest.setUri(uri);
		
		verify(mockHttpClient).get(expectedRequest);
	}
	
	@Test
	public void testGetAuthorizationEndpoint() {
		String propertyValue = "propertyValue";
		
		OIDCConfig config = Mockito.spy(this.config);
		
		doReturn(propertyValue).when(config).getConfigProperty(any());
		
		// Call under test
		
		String result = config.getAuthorizationEndpoint();
		
		assertEquals(propertyValue, result);
		
		verify(config).getConfigProperty(OIDCConfig.PROPERTY_AUTH_ENDPOINT);
	}
	
	@Test
	public void testGetTokenEndpoint() {
		String propertyValue = "propertyValue";
		
		OIDCConfig config = Mockito.spy(this.config);
		
		doReturn(propertyValue).when(config).getConfigProperty(any());
		
		// Call under test
		
		String result = config.getTokenEndpoint();
		
		assertEquals(propertyValue, result);
		
		verify(config).getConfigProperty(OIDCConfig.PROPERTY_TOKEN_ENDPOINT);
	}
	
	@Test
	public void testGetUserinfoEndpoint() {
		String propertyValue = "propertyValue";
		
		OIDCConfig config = Mockito.spy(this.config);
		
		doReturn(propertyValue).when(config).getConfigProperty(any());
		
		// Call under test
		
		String result = config.getUserInfoEndpoint();
		
		assertEquals(propertyValue, result);
		
		verify(config).getConfigProperty(OIDCConfig.PROPERTY_USERINFO_ENDPOINT);
	}

}
