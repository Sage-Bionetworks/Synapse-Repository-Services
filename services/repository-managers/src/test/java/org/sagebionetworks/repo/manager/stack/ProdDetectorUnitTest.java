package org.sagebionetworks.repo.manager.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.versionInfo.SynapseVersionInfo;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;

import com.google.common.collect.ImmutableMap;

@ExtendWith(MockitoExtension.class)
public class ProdDetectorUnitTest {

	@Mock
	private StackConfiguration mockConfiguration;

	@Mock
	private SimpleHttpClient mockHttpClient;

	@Mock
	private LoggerProvider mockLogProvider;

	@InjectMocks
	private ProdDetectorImpl prodDetector;

	@Mock
	private SimpleHttpRequest mockRequest;

	@Mock
	private SimpleHttpResponse mockResponse;
	
	@Mock
	private SynapseVersionInfo mockVersionInfo;

	@Mock
	private Logger mockLogger;

	private static final String PROD_ENDPOINT = "https://prod.synapse.org/repo/v1";
	private static final String PROD_VERSION_ENDPOINT = PROD_ENDPOINT + ProdDetectorImpl.VERSION_INFO_ENDPOINT;
	private static final String CURRENT_VERSION = "318.0";

	@BeforeEach
	public void before() {
		when(mockConfiguration.getRepositoryServiceProdEndpoint()).thenReturn(PROD_ENDPOINT);
		when(mockConfiguration.getStackInstance()).thenReturn(CURRENT_VERSION);
		when(mockLogProvider.getLogger(any())).thenReturn(mockLogger);

		// Emulate the PostConstruct call
		prodDetector.init();
	}

	@Test
	public void testSendVersionRequest() throws Exception {

		when(mockHttpClient.get(any())).thenReturn(mockResponse);

		SimpleHttpRequest expectedRequest = new SimpleHttpRequest();

		expectedRequest.setUri(PROD_VERSION_ENDPOINT);
		expectedRequest.setHeaders(ImmutableMap.of(
				"User-Agent", "SynapseRepositoryStack/" + CURRENT_VERSION,
				"Accept", "application/json", 
				"Cache-Control", "no-cache")
		);

		// Call under test
		Optional<SimpleHttpResponse> result = prodDetector.sendVersionRequest();

		verify(mockHttpClient).get(expectedRequest);

		assertTrue(result.isPresent());
		assertEquals(mockResponse, result.get());
		verifyZeroInteractions(mockLogger);

	}

	@Test
	public void testSendVersionRequestWithException() throws Exception {

		IOException ex = new IOException("Some error");

		doThrow(ex).when(mockHttpClient).get(any());

		// Call under test
		Optional<SimpleHttpResponse> result = prodDetector.sendVersionRequest();

		assertFalse(result.isPresent());
		verify(mockLogger).error(ProdDetectorImpl.FAILED_REQUEST_MSG, PROD_VERSION_ENDPOINT, "Some error", ex);

	}

	@Test
	public void testParseVersionResponse() throws Exception {

		int statusCode = 200;
		String responseBody = "{ \"version\":\"" + CURRENT_VERSION + "\" }";

		when(mockResponse.getStatusCode()).thenReturn(statusCode);
		when(mockResponse.getContent()).thenReturn(responseBody);

		SynapseVersionInfo expected = new SynapseVersionInfo();
		expected.setVersion(CURRENT_VERSION);

		// Call under test
		Optional<SynapseVersionInfo> result = prodDetector.parseVersionResponse(mockResponse);

		assertTrue(result.isPresent());
		assertEquals(expected, result.get());
		verifyZeroInteractions(mockLogger);

	}

	@Test
	public void testParseVersionResponseWithEmptyResponse() throws Exception {

		int statusCode = 200;
		String responseBody = " ";

		when(mockResponse.getStatusCode()).thenReturn(statusCode);
		when(mockResponse.getContent()).thenReturn(responseBody);

		// Call under test
		Optional<SynapseVersionInfo> result = prodDetector.parseVersionResponse(mockResponse);

		assertFalse(result.isPresent());
		verify(mockLogger).error(ProdDetectorImpl.FAILED_REQUEST_MSG + " (Status Code: " + statusCode + ")",
				PROD_VERSION_ENDPOINT, "Respose body was empty", null);
	}

	@Test
	public void testParseVersionResponseWithWrongStatusCode() throws Exception {

		int statusCode = 400;

		when(mockResponse.getStatusCode()).thenReturn(statusCode);
		when(mockResponse.getStatusReason()).thenReturn("Some reason");

		// Call under test
		Optional<SynapseVersionInfo> result = prodDetector.parseVersionResponse(mockResponse);

		assertFalse(result.isPresent());
		verify(mockResponse, never()).getContent();
		verify(mockLogger).error(ProdDetectorImpl.FAILED_REQUEST_MSG + " (Status Code: " + statusCode + ")",
				PROD_VERSION_ENDPOINT, "Some reason", null);
	}
	
	@Test
	public void testParseVersionResponseWithWrongStatusCodeAndNoReason() throws Exception {

		int statusCode = 400;

		when(mockResponse.getStatusCode()).thenReturn(statusCode);
		when(mockResponse.getStatusReason()).thenReturn("");
		when(mockResponse.getContent()).thenReturn("Some content");

		// Call under test
		Optional<SynapseVersionInfo> result = prodDetector.parseVersionResponse(mockResponse);

		assertFalse(result.isPresent());
		verify(mockLogger).error(ProdDetectorImpl.FAILED_REQUEST_MSG + " (Status Code: " + statusCode + ")",
				PROD_VERSION_ENDPOINT, "Some content", null);
	}

	@Test
	public void testParseVersionResponseWithParsingException() throws Exception {

		int statusCode = 200;
		String responseBody = "malformed-response";

		when(mockResponse.getStatusCode()).thenReturn(statusCode);
		when(mockResponse.getContent()).thenReturn(responseBody);

		// Call under test
		Optional<SynapseVersionInfo> result = prodDetector.parseVersionResponse(mockResponse);

		assertFalse(result.isPresent());

		ArgumentCaptor<JSONObjectAdapterException> exCaptor = ArgumentCaptor.forClass(JSONObjectAdapterException.class);

		String errorMessage = "org.json.JSONException: A JSONObject text must begin with '{' at 1 [character 2 line 1]";

		verify(mockLogger).error(eq(ProdDetectorImpl.FAILED_REQUEST_MSG + " (Status Code: " + statusCode + ")"),
				eq(PROD_VERSION_ENDPOINT), eq(errorMessage), exCaptor.capture());

		assertEquals(errorMessage, exCaptor.getValue().getMessage());
	}
	
	@Test
	public void testIsProductionStackWithSameVersion() throws Exception {
		// We use a spy since all other methods are already tested and we can mock them out
		ProdDetectorImpl prodDetectorSpy = Mockito.spy(prodDetector);
		
		when(mockVersionInfo.getStackInstance()).thenReturn(CURRENT_VERSION);

		doReturn(Optional.of(mockResponse)).when(prodDetectorSpy).sendVersionRequest();
		doReturn(Optional.of(mockVersionInfo)).when(prodDetectorSpy).parseVersionResponse(mockResponse);
		
		// Call under test
		Optional<Boolean> result = prodDetectorSpy.isProductionStack();
		
		assertEquals(Optional.of(Boolean.TRUE), result);
	}
	
	@Test
	public void testIsProductionStackWithDifferentVersion() throws Exception {
		// We use a spy since all other methods are already tested and we can mock them out
		ProdDetectorImpl prodDetectorSpy = Mockito.spy(prodDetector);
		
		when(mockVersionInfo.getStackInstance()).thenReturn(CURRENT_VERSION + 1);

		doReturn(Optional.of(mockResponse)).when(prodDetectorSpy).sendVersionRequest();
		doReturn(Optional.of(mockVersionInfo)).when(prodDetectorSpy).parseVersionResponse(mockResponse);
		
		// Call under test
		Optional<Boolean> result = prodDetectorSpy.isProductionStack();
		
		assertEquals(Optional.of(Boolean.FALSE), result);
	}
	
	@Test
	public void testIsProductionStackWithEmptyResponse() throws Exception {
		// We use a spy since all other methods are already tested and we can mock them out
		ProdDetectorImpl prodDetectorSpy = Mockito.spy(prodDetector);

		doReturn(Optional.empty()).when(prodDetectorSpy).sendVersionRequest();
		
		// Call under test
		Optional<Boolean> result = prodDetectorSpy.isProductionStack();
		
		assertEquals(Optional.empty(), result);
		verify(prodDetectorSpy, never()).parseVersionResponse(any());
	}
	
	@Test
	public void testIsProductionStackWithEmptyParsing() throws Exception {
		// We use a spy since all other methods are already tested and we can mock them out
		ProdDetectorImpl prodDetectorSpy = Mockito.spy(prodDetector);

		doReturn(Optional.of(mockResponse)).when(prodDetectorSpy).sendVersionRequest();
		doReturn(Optional.empty()).when(prodDetectorSpy).parseVersionResponse(any());
		
		// Call under test
		Optional<Boolean> result = prodDetectorSpy.isProductionStack();
		
		assertEquals(Optional.empty(), result);
	}

}
