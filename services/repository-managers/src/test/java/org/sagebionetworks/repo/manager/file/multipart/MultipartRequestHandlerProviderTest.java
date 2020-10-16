package org.sagebionetworks.repo.manager.file.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.file.MultiPartRequestType;
import org.sagebionetworks.repo.model.file.MultipartRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadCopyRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;

@ExtendWith(MockitoExtension.class)
public class MultipartRequestHandlerProviderTest {
	
	@Mock
	private MultipartRequestHandler<MultipartUploadRequest> mockRequestHandler;

	private MultipartRequestHandlerProviderImpl provider;
	
	@BeforeEach
	public void before() {
		when(mockRequestHandler.getRequestClass()).thenReturn(MultipartUploadRequest.class);
	
		provider = new MultipartRequestHandlerProviderImpl(Arrays.asList(mockRequestHandler));
	}
	
	@Test
	public void testProviderInitializationWithNullHandlers() {
		List<MultipartRequestHandler<? extends MultipartRequest>> handlers = null;
		
		String errorMessage = assertThrows(IllegalStateException.class, () -> {
			// Call under test
			provider = new MultipartRequestHandlerProviderImpl(handlers);
		}).getMessage();
		
		assertEquals("No multipart request handler found on the classpath.", errorMessage);
	}
	
	@Test
	public void testProviderInitializationWithNoHandlers() {
		List<MultipartRequestHandler<? extends MultipartRequest>> handlers = Collections.emptyList();
		
		String errorMessage = assertThrows(IllegalStateException.class, () -> {
			// Call under test
			provider = new MultipartRequestHandlerProviderImpl(handlers);
		}).getMessage();
		
		assertEquals("No multipart request handler found on the classpath.", errorMessage);
	}
	
	@Test
	public void testProviderInitialization() {
		List<MultipartRequestHandler<? extends MultipartRequest>> handlers = Arrays.asList(mockRequestHandler);
		
		// Call under test
		provider = new MultipartRequestHandlerProviderImpl(handlers);
	}
	
	@Test
	public void testGetHandlerForType() {
		
		// Call under test
		MultipartRequestHandler<? extends MultipartRequest> handler = provider.getHandlerForType(MultiPartRequestType.UPLOAD);
		
		assertEquals(mockRequestHandler, handler);
		
	}
	
	@Test
	public void testGetHandlerForTypeWithUnregistered() {
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test, note that for the test only the UPLOAD is registered
			provider.getHandlerForType(MultiPartRequestType.COPY);
		}).getMessage();
		
		assertEquals("Unsupported request type: org.sagebionetworks.repo.model.file.MultipartUploadCopyRequest", errorMessage);
		
	}
	
	@Test
	public void testGetHandlerForTypeWithNull() {
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			
			// Call under test
			provider.getHandlerForType(null);
		}).getMessage();
	
		assertEquals("The requestType is required.", errorMessage);
	}
	
	@Test
	public void testGetHandlerForClass() {
		// Call under test
		MultipartRequestHandler<? extends MultipartRequest> handler = provider.getHandlerForClass(MultipartUploadRequest.class);
		
		assertEquals(mockRequestHandler, handler);
	}
	
	@Test
	public void testGetHandlerForClassWithUnregistered() {
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test, note that for the test only the UPLOAD is registered
			provider.getHandlerForClass(MultipartUploadCopyRequest.class);
		}).getMessage();
		
		assertEquals("Unsupported request type: org.sagebionetworks.repo.model.file.MultipartUploadCopyRequest", errorMessage);
		
	}
	
	@Test
	public void testGetHandlerForClassWithNull() {
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			
			// Call under test
			provider.getHandlerForClass(null);
		}).getMessage();
	
		assertEquals("The requestClass is required.", errorMessage);
	}
	
}
