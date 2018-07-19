package org.sagebionetworks.doi.datacite;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.doi.v2.DataciteMetadata;
import org.sagebionetworks.repo.model.doi.v2.Doi;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.doi.datacite.DataciteClientImpl.registerDoiRequestBody;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:doi-test-spb.xml"})
public class DataciteClientImplTest {

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
	private final String CONFIG_URL = "http://URL.com/";
	private final String CONFIG_USR = "usr";
	private final String CONFIG_PWD = "pwd";


	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		DataciteClientConfig config = new DataciteClientConfig();
		config.setDataciteUrl(CONFIG_URL);
		config.setUsername(CONFIG_USR);
		config.setPassword(CONFIG_PWD);
		dataciteClient = new DataciteClientImpl();
		dataciteClient.setConfig(config);
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
		assertEquals(CONFIG_URL+"metadata/"+ URI, requestCaptor.getValue().getUri());
		verify(mockResponse, times(1)).getContent();
		verify(mockXmlTranslator, times(1)).translate("test string");
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testGetIoException() throws Exception {
		when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenThrow(new IOException());
		dataciteClient.get(URI);
	}

	@Test(expected = NotFoundException.class)
	public void testGetNoContent() throws Exception {
		when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);
		dataciteClient.get(URI);
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testGetUnauthorized() throws Exception {
		when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_UNAUTHORIZED);
		dataciteClient.get(URI);
	}

	@Test(expected = NotFoundException.class)
	public void testGetForbidden() throws Exception {
		when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_FORBIDDEN);
		dataciteClient.get(URI);
	}

	@Test(expected = NotFoundException.class)
	public void testGetNotFound() throws Exception {
		when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
		dataciteClient.get(URI);
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testGetInternalServerError() throws Exception {
		when(mockHttpClient.get(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
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
		assertEquals(CONFIG_URL+"metadata/", requestCaptor.getValue().getUri());
		assertEquals("test xml", stringCaptor.getValue());
		verify(mockMetadataTranslator, times(1)).translate(any(DataciteMetadata.class), eq(URI));
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testRegisterMetadataIoException() throws Exception {
		when(mockMetadataTranslator.translate(any(Doi.class), any(String.class))).thenReturn("<test xml>");
		when(mockHttpClient.post(any(SimpleHttpRequest.class), any(String.class))).thenThrow(new IOException());
		dataciteClient.registerMetadata(metadata, URI);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRegisterMetadataBadRequest() throws Exception {
		when(mockMetadataTranslator.translate(any(Doi.class), any(String.class))).thenReturn("<test xml>");
		when(mockHttpClient.post(any(SimpleHttpRequest.class), any(String.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
		dataciteClient.registerMetadata(metadata, URI);
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testRegisterMetadataUnauthorized() throws Exception {
		when(mockMetadataTranslator.translate(any(Doi.class), any(String.class))).thenReturn("<test xml>");
		when(mockHttpClient.post(any(SimpleHttpRequest.class), any(String.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_UNAUTHORIZED);
		dataciteClient.registerMetadata(metadata, URI);
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testRegisterMetadataForbidden() throws Exception {
		when(mockMetadataTranslator.translate(any(Doi.class), any(String.class))).thenReturn("<test xml>");
		when(mockHttpClient.post(any(SimpleHttpRequest.class), any(String.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_FORBIDDEN);
		dataciteClient.registerMetadata(metadata, URI);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRegisterMetadataUnsupportedMediaType() throws Exception {
		when(mockMetadataTranslator.translate(any(Doi.class), any(String.class))).thenReturn("<test xml>");
		when(mockHttpClient.post(any(SimpleHttpRequest.class), any(String.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
		dataciteClient.registerMetadata(metadata, URI);
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testRegisterMetadataInternalServerError() throws Exception {
		when(mockMetadataTranslator.translate(any(Doi.class), any(String.class))).thenReturn("<test xml>");
		when(mockHttpClient.post(any(SimpleHttpRequest.class), any(String.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
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
		assertEquals(CONFIG_URL+"doi/" + URI, requestCaptor.getValue().getUri());
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testRegisterDoiIoException() throws Exception {
		when(mockHttpClient.put(any(SimpleHttpRequest.class), any(String.class))).thenThrow(new IOException());
		dataciteClient.registerDoi(URI, URL);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRegisterDoiBadRequest() throws Exception {
		when(mockHttpClient.put(any(SimpleHttpRequest.class), any(String.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
		dataciteClient.registerDoi(URI, URL);
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testRegisterDoiUnauthorized() throws Exception {
		when(mockHttpClient.put(any(SimpleHttpRequest.class), any(String.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_UNAUTHORIZED);
		dataciteClient.registerDoi(URI, URL);
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testRegisterDoiForbidden() throws Exception {
		when(mockHttpClient.put(any(SimpleHttpRequest.class), any(String.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_FORBIDDEN);
		dataciteClient.registerDoi(URI, URL);
	}

	@Test(expected = ForbiddenException.class)
	public void testRegisterDoiPreconditionFailed() throws Exception {
		when(mockHttpClient.put(any(SimpleHttpRequest.class), any(String.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_PRECONDITION_FAILED);
		dataciteClient.registerDoi(URI, URL);
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testRegisterDoiInternalServerError() throws Exception {
		when(mockHttpClient.put(any(SimpleHttpRequest.class), any(String.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
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
		assertEquals(CONFIG_URL+"metadata/" + URI, requestCaptor.getValue().getUri());
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testDeactivateIoException() throws Exception {
		when(mockHttpClient.delete(any(SimpleHttpRequest.class))).thenThrow(new IOException());
		dataciteClient.deactivate(URI);
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testDeactivateUnauthorized() throws Exception {
		when(mockHttpClient.delete(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_UNAUTHORIZED);
		dataciteClient.deactivate(URI);
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testDeactivateForbidden() throws Exception {
		when(mockHttpClient.delete(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_FORBIDDEN);
		dataciteClient.deactivate(URI);
	}

	@Test(expected = ServiceUnavailableException.class)
	public void testDeactivateInternalServerError() throws Exception {
		when(mockHttpClient.delete(any(SimpleHttpRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
		dataciteClient.deactivate(URI);
	}

	@Test
	public void registerDoiRequestBodyTest() {
		assertEquals("doi="+ URI +"\nurl=" + URL, registerDoiRequestBody(URI, URL));
	}

	@Test
	public void testCreate() throws Exception {
		dataciteClient = mock(DataciteClientImpl.class);
		doCallRealMethod().when(dataciteClient).create(any(DataciteMetadata.class), any(String.class), any(String.class));

		dataciteClient.create(doi, URI, URL);
		verify(dataciteClient, times(1)).registerMetadata(any(DataciteMetadata.class), any(String.class));
		verify(dataciteClient, times(1)).registerDoi(any(String.class), any(String.class));
	}

	@Test
	public void testUpdate() throws Exception {
		dataciteClient = mock(DataciteClientImpl.class);
		doCallRealMethod().when(dataciteClient).update(any(DataciteMetadata.class), any(String.class));

		dataciteClient.update(doi, URI);
		verify(dataciteClient, times(1)).registerMetadata(any(DataciteMetadata.class), any(String.class));
	}
}
