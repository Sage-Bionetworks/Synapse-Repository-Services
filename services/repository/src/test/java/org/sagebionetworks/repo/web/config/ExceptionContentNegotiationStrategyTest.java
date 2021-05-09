package org.sagebionetworks.repo.web.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

@ExtendWith(MockitoExtension.class)
public class ExceptionContentNegotiationStrategyTest {
	
	@Mock
	private NativeWebRequest mockRequest;
	
	private List<MediaType> supportedTypes;
	
	private ExceptionContentNegotiationStrategy strategy;
	
	@BeforeEach
	public void before() {
		supportedTypes = Collections.singletonList(MediaType.APPLICATION_JSON);
		strategy = new ExceptionContentNegotiationStrategy(supportedTypes);
	}
	
	@Test
	public void testResolveMediaTypesWithEmptyAcceptHeader() throws HttpMediaTypeNotAcceptableException {
		when(mockRequest.getHeaderValues(anyString())).thenReturn(new String[] {});
		
		// Call under test
		List<MediaType> result = strategy.resolveMediaTypes(mockRequest);
		
		assertEquals(Collections.singletonList(MediaType.ALL), result);
		
		verify(mockRequest).getHeaderValues(HttpHeaders.ACCEPT);
	}
	
	@Test
	public void testResolveMediaTypesWithNullAcceptHeader() throws HttpMediaTypeNotAcceptableException {
		when(mockRequest.getHeaderValues(anyString())).thenReturn(null);
		
		// Call under test
		List<MediaType> result = strategy.resolveMediaTypes(mockRequest);
		
		assertEquals(Collections.singletonList(MediaType.ALL), result);
		
		verify(mockRequest).getHeaderValues(HttpHeaders.ACCEPT);
	}
	
	@Test
	public void testResolveMediaTypesWithUnsuportedAcceptHeader() throws HttpMediaTypeNotAcceptableException {
		when(mockRequest.getHeaderValues(anyString())).thenReturn(new String[] {MediaType.APPLICATION_XML_VALUE});
		
		// Call under test
		List<MediaType> result = strategy.resolveMediaTypes(mockRequest);
		
		assertEquals(supportedTypes, result);
		
		verify(mockRequest).getHeaderValues(HttpHeaders.ACCEPT);
	}
	
	@Test
	public void testResolveMediaTypesWithMalformedAcceptHeader() throws HttpMediaTypeNotAcceptableException {
		when(mockRequest.getHeaderValues(anyString())).thenReturn(new String[] {"malformed"});
		
		// Call under test
		List<MediaType> result = strategy.resolveMediaTypes(mockRequest);
		
		assertEquals(supportedTypes, result);
		
		verify(mockRequest).getHeaderValues(HttpHeaders.ACCEPT);
	}
	
}
