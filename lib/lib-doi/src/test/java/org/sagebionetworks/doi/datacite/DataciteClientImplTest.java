package org.sagebionetworks.doi.datacite;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.doi.datacite.DataciteClientImpl.handleHttpErrorCode;
import static org.sagebionetworks.doi.datacite.DataciteClientImpl.registerDoiRequestBody;

import java.io.IOException;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.doi.v2.DataciteMetadata;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(locations = {"classpath:doi-test-spb.xml"})
public class DataciteClientImplTest {

	@Mock
	private DataciteClientConfig config;
	@Mock
	private SimpleHttpClient mockHttpClient;
	@Mock
	private SimpleHttpResponse mockResponse;
	@Mock
	private DataciteXmlTranslator mockXmlTranslator;
	@Mock
	private DataciteMetadataTranslator mockMetadataTranslator;
	@Captor
	private ArgumentCaptor<SimpleHttpRequest> requestCaptor;
	@Captor
	private ArgumentCaptor<String> stringCaptor;

	private DataciteClientImpl dataciteClient;
	private Doi doi;
	private DataciteMetadata metadata;
	private final String URI = "10.9999/syn1234test";
	private final String URL = "sftp://synapse.gov/data";
	private final String CONFIG_URL = "url.com";
	private final String CONFIG_USR = "usr";
	private final String CONFIG_PWD = "pwd";


	@Before
	public void before() {
		when(config.getUsername()).thenReturn(CONFIG_USR);
		when(config.getPassword()).thenReturn(CONFIG_PWD);
		when(config.getDataciteDomain()).thenReturn(CONFIG_URL);
		dataciteClient = new DataciteClientImpl(config);
		ReflectionTestUtils.setField(dataciteClient, "client", mockHttpClient);
		ReflectionTestUtils.setField(dataciteClient, "xmlTranslator", mockXmlTranslator);
		ReflectionTestUtils.setField(dataciteClient, "metadataTranslator", mockMetadataTranslator);
		doi = new Doi();
		metadata = new Doi();
	}

	@Test
	public void testGetSuccess() throws Exception {
		when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);

		when(mockResponse.getContent()).thenReturn("test string");
		when(mockXmlTranslator.translate(any(String.class))).thenReturn(doi);

		metadata = dataciteClient.get(URI);

		// Ensure the client made a get call, retrieved the content of that call, and translated it.
		verify(mockHttpClient).get(requestCaptor.capture());
		assertEquals("Synapse", requestCaptor.getValue().getHeaders().get(HttpHeaders.USER_AGENT));
		assertEquals(CONFIG_USR + ":" + CONFIG_PWD, requestCaptor.getValue().getHeaders().get(HttpHeaders.AUTHORIZATION));
		assertEquals("https://" + CONFIG_URL + "/metadata/"+ URI, requestCaptor.getValue().getUri());
		verify(mockResponse, times(1)).getContent();
		verify(mockXmlTranslator, times(1)).translate("test string");
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testGetIoException() throws Exception {
		when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenThrow(new IOException());
		dataciteClient.get(URI);
	}

	@Test
	public void registerMetadataSuccessTest() throws Exception {
		when(mockHttpClient.post(any(SimpleHttpRequest.class), any(String.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);
		when(mockMetadataTranslator.translate(any(DataciteMetadata.class), any(String.class))).thenReturn("test xml");

		dataciteClient.registerMetadata(metadata, URI);

		// Ensure the client made a post call with translated xml.
		verify(mockHttpClient).post(requestCaptor.capture(), stringCaptor.capture());
		assertEquals("Synapse", requestCaptor.getValue().getHeaders().get(HttpHeaders.USER_AGENT));
		assertEquals(CONFIG_USR + ":" + CONFIG_PWD, requestCaptor.getValue().getHeaders().get(HttpHeaders.AUTHORIZATION));
		assertEquals("application/xml;charset=UTF-8", requestCaptor.getValue().getHeaders().get(HttpHeaders.CONTENT_TYPE));
		assertEquals("https://" + CONFIG_URL+"/metadata/", requestCaptor.getValue().getUri());
		assertEquals("test xml", stringCaptor.getValue());
		verify(mockMetadataTranslator, times(1)).translate(any(DataciteMetadata.class), eq(URI));
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testRegisterMetadataIoException() throws Exception {
		when(mockMetadataTranslator.translate(any(Doi.class), any(String.class))).thenReturn("<test xml>");
		when(mockHttpClient.post(any(SimpleHttpRequest.class), any(String.class))).thenThrow(new IOException());
		dataciteClient.registerMetadata(metadata, URI);
	}

	@Test
	public void registerDoiSuccessTest() throws Exception {
		when(mockHttpClient.put(any(SimpleHttpRequest.class), any(String.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);

		dataciteClient.registerDoi(URI, URL);

		// Ensure the client made a put call.
		verify(mockHttpClient).put(requestCaptor.capture(), any(String.class));
		assertEquals("Synapse", requestCaptor.getValue().getHeaders().get(HttpHeaders.USER_AGENT));
		assertEquals(CONFIG_USR + ":" + CONFIG_PWD, requestCaptor.getValue().getHeaders().get(HttpHeaders.AUTHORIZATION));
		assertEquals("text/plain;charset=UTF-8", requestCaptor.getValue().getHeaders().get(HttpHeaders.CONTENT_TYPE));
		assertEquals("https://" + CONFIG_URL + "/doi/" + URI, requestCaptor.getValue().getUri());
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testRegisterDoiIoException() throws Exception {
		when(mockHttpClient.put(any(SimpleHttpRequest.class), any(String.class))).thenThrow(new IOException());
		dataciteClient.registerDoi(URI, URL);
	}

	@Test
	public void deactivateSuccessTest() throws Exception {
		when(mockHttpClient.delete(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);

		dataciteClient.deactivate(URI);

		// Ensure the client made a put call.
		verify(mockHttpClient).delete(requestCaptor.capture());
		assertEquals("Synapse", requestCaptor.getValue().getHeaders().get(HttpHeaders.USER_AGENT));
		assertEquals(CONFIG_USR + ":" + CONFIG_PWD, requestCaptor.getValue().getHeaders().get(HttpHeaders.AUTHORIZATION));
		assertEquals("https://" + CONFIG_URL+ "/metadata/" + URI, requestCaptor.getValue().getUri());
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testDeactivateIoException() throws Exception {
		when(mockHttpClient.delete(any(SimpleHttpRequest.class))).thenThrow(new IOException());
		dataciteClient.deactivate(URI);
	}

	@Test
	public void registerDoiRequestBodyTest() {
		assertEquals("doi="+ URI +"\nurl=" + URL, registerDoiRequestBody(URI, URL));
	}

	@Test(expected = RuntimeException.class)
	public void testNoContent() throws Exception {
		handleHttpErrorCode(HttpStatus.SC_NO_CONTENT);
	}

	@Test(expected = RuntimeException.class)
	public void testUnauthorized() throws Exception {
		handleHttpErrorCode(HttpStatus.SC_UNAUTHORIZED);
	}

	@Test(expected = RuntimeException.class)
	public void testForbidden() throws Exception {
		handleHttpErrorCode(HttpStatus.SC_FORBIDDEN);
	}

	@Test(expected = NotFoundException.class)
	public void testNotFound() throws Exception {
		handleHttpErrorCode(HttpStatus.SC_NOT_FOUND);
	}

	@Test(expected = RuntimeException.class)
	public void testBadRequest() throws Exception {
		handleHttpErrorCode(HttpStatus.SC_BAD_REQUEST);
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testUnsupportedMediaType() throws Exception {
		handleHttpErrorCode(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
	}

	@Test(expected = NotFoundException.class)
	public void testRegisterDoiPreconditionFailed() throws Exception {
		handleHttpErrorCode(HttpStatus.SC_PRECONDITION_FAILED);
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testInternalServerError() throws Exception {
		handleHttpErrorCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testServiceUnavailable() throws Exception {
		handleHttpErrorCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
	}
}
