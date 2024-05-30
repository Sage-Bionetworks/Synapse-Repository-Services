package org.sagebionetworks.simpleHttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SimpleHttpClientImplUnitTest {

	@Mock
	private CloseableHttpClient mockHttpClient;
	@Mock
	private CloseableHttpResponse mockResponse;
	@Mock
	private StreamProvider mockProvider;
	@Mock
	private StatusLine mockStatusLine;
	@Mock
	private CookieStore mockCookieStore;

	@InjectMocks
	private SimpleHttpClientImpl simpleHttpClient = new SimpleHttpClientImpl();

	private SimpleHttpRequest request;
	private SimpleHttpResponse response;
	private List<Header> responseHeaders;

	private final String cookieDomain = "my.domain.com";
	private final String cookieName = "cookieName";
	private final String cookieValue = "cookieValue";

	BasicClientCookie matchingDomainNonMatchingNameCookie;
	BasicClientCookie nonMatchingDomainMatchingNameCookie;
	BasicClientCookie matchingDomainAndNameCookie;

	@BeforeEach
	public void before() throws Exception {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("name", "value");
		request = new SimpleHttpRequest();
		request.setUri("uri");
		request.setHeaders(headers);
		responseHeaders = new LinkedList<Header>();
		response = new SimpleHttpResponse(HttpStatus.SC_OK, "reason", null, responseHeaders);

		matchingDomainNonMatchingNameCookie = new BasicClientCookie("nonMatchingDomainCookieName","nonMatchingNameCookieValue");
		matchingDomainNonMatchingNameCookie.setDomain(cookieDomain);
		nonMatchingDomainMatchingNameCookie = new BasicClientCookie(cookieName, "nonMatchingDomainCookieValue");
		nonMatchingDomainMatchingNameCookie.setDomain("other.domain.com");
		matchingDomainAndNameCookie = new BasicClientCookie(cookieName, cookieValue);
		matchingDomainAndNameCookie.setDomain(cookieDomain);
	}

	@Test
	public void testCopyHeadersWithNullSimpleHttpRequest() {
		assertThrows(IllegalArgumentException.class, () -> {			
			SimpleHttpClientImpl.copyHeaders(null, new HttpGet());
		});
	}

	@Test
	public void testCopyHeadersWithNullHttpUriRequest() {
		assertThrows(IllegalArgumentException.class, () -> {
			SimpleHttpClientImpl.copyHeaders(new SimpleHttpRequest(), null);
		});
	}

	@Test
	public void testCopyHeadersWithEmptyHeaders() {
		HttpGet mockHttpGet = Mockito.mock(HttpGet.class);
		SimpleHttpClientImpl.copyHeaders(new SimpleHttpRequest(), mockHttpGet);
		verifyZeroInteractions(mockHttpGet);
	}

	@Test
	public void testCopyHeaders() {
		String name1 = "name1";
		String name2 = "name2";
		String value1 = "value1";
		String value2 = "value2";
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(name1, value1);
		headers.put(name2, value2);
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setHeaders(headers);
		HttpGet mockHttpGet = Mockito.mock(HttpGet.class);
		SimpleHttpClientImpl.copyHeaders(request, mockHttpGet);
		verify(mockHttpGet).addHeader(name1, value1);
		verify(mockHttpGet).addHeader(name2, value2);
		verifyNoMoreInteractions(mockHttpGet);
	}

	@Test
	public void testValidateSimpleHttpRequestWithNullRequest() {
		assertThrows(IllegalArgumentException.class, () -> {
			SimpleHttpClientImpl.validateSimpleHttpRequest(null);
		});
	}

	@Test
	public void testValidateSimpleHttpRequestWithNullUri() {
		assertThrows(IllegalArgumentException.class, () -> {
			SimpleHttpClientImpl.validateSimpleHttpRequest(new SimpleHttpRequest());
		});
	}

	@Test
	public void testValidateSimpleHttpRequestWithValidRequest() {
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri("uri");
		SimpleHttpClientImpl.validateSimpleHttpRequest(request);
	}

	@Test
	public void testGet() throws Exception {
		setupResponseMock();
		assertEquals(response, simpleHttpClient.get(request));
		ArgumentCaptor<HttpGet> captor = ArgumentCaptor.forClass(HttpGet.class);
		verify(mockHttpClient).execute(captor.capture());
		HttpGet captured = captor.getValue();
		assertEquals(request.getUri(), captured.getURI().toString());
		assertEquals("value", captured.getHeaders("name")[0].getValue());
		verify(mockResponse).close();
	}

	@Test
	public void testOptions() throws Exception {
		setupResponseMock();
		assertEquals(response, simpleHttpClient.options(request));
		ArgumentCaptor<HttpOptions> captor = ArgumentCaptor.forClass(HttpOptions.class);
		verify(mockHttpClient).execute(captor.capture());
		HttpOptions captured = captor.getValue();
		assertEquals(request.getUri(), captured.getURI().toString());
		assertEquals("value", captured.getHeaders("name")[0].getValue());
		verify(mockResponse).close();
	}

	@Test
	public void testPostWithNullBody() throws Exception {
		setupResponseMock();
		assertEquals(response, simpleHttpClient.post(request, null));
		ArgumentCaptor<HttpPost> captor = ArgumentCaptor.forClass(HttpPost.class);
		verify(mockHttpClient).execute(captor.capture());
		HttpPost captured = captor.getValue();
		assertEquals(request.getUri(), captured.getURI().toString());
		assertNull(captured.getEntity());
		assertEquals("value", captured.getHeaders("name")[0].getValue());
		verify(mockResponse).close();
	}

	@Test
	public void testPost() throws Exception {
		setupResponseMock();
		String body = "body";
		assertEquals(response, simpleHttpClient.post(request, body));
		ArgumentCaptor<HttpPost> captor = ArgumentCaptor.forClass(HttpPost.class);
		verify(mockHttpClient).execute(captor.capture());
		HttpPost captured = captor.getValue();
		assertEquals(request.getUri(), captured.getURI().toString());
		assertEquals(body, EntityUtils.toString(captured.getEntity()));
		assertEquals("value", captured.getHeaders("name")[0].getValue());
		verify(mockResponse).close();
	}

	@Test
	public void testPutWithNullBody() throws Exception {
		setupResponseMock();
		assertEquals(response, simpleHttpClient.put(request, null));
		ArgumentCaptor<HttpPut> captor = ArgumentCaptor.forClass(HttpPut.class);
		verify(mockHttpClient).execute(captor.capture());
		HttpPut captured = captor.getValue();
		assertEquals(request.getUri(), captured.getURI().toString());
		assertNull(captured.getEntity());
		assertEquals("value", captured.getHeaders("name")[0].getValue());
		verify(mockResponse).close();
	}

	@Test
	public void testPut() throws Exception {
		setupResponseMock();
		String body = "body";
		assertEquals(response, simpleHttpClient.put(request, body));
		ArgumentCaptor<HttpPut> captor = ArgumentCaptor.forClass(HttpPut.class);
		verify(mockHttpClient).execute(captor.capture());
		HttpPut captured = captor.getValue();
		assertEquals(request.getUri(), captured.getURI().toString());
		assertEquals(body, EntityUtils.toString(captured.getEntity()));
		assertEquals("value", captured.getHeaders("name")[0].getValue());
		verify(mockResponse).close();
	}

	@Test
	public void testDelete() throws Exception {
		setupResponseMock();
		assertEquals(response, simpleHttpClient.delete(request));
		ArgumentCaptor<HttpDelete> captor = ArgumentCaptor.forClass(HttpDelete.class);
		verify(mockHttpClient).execute(captor.capture());
		HttpDelete captured = captor.getValue();
		assertEquals(request.getUri(), captured.getURI().toString());
		assertEquals("value", captured.getHeaders("name")[0].getValue());
		verify(mockResponse).close();
	}

	@Test
	public void testPutFileWithNullFile() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			simpleHttpClient.putFile(request, null);
		});
	}

	@Test
	public void testPutFile() throws Exception {
		setupResponseMock();
		File mockFile = Mockito.mock(File.class);
		assertEquals(response, simpleHttpClient.putFile(request, mockFile));
		ArgumentCaptor<HttpPut> captor = ArgumentCaptor.forClass(HttpPut.class);
		verify(mockHttpClient).execute(captor.capture());
		HttpPut captured = captor.getValue();
		assertEquals(request.getUri(), captured.getURI().toString());
		assertTrue(captured.getEntity() instanceof FileEntity);
		assertEquals("value", captured.getHeaders("name")[0].getValue());
		verify(mockResponse).close();
	}


	@Test
	public void testPutToURLWithNullInputStream() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {			
			simpleHttpClient.putToURL(request, null, 0L);
		});
	}

	@Test
	public void testPutToURL() throws Exception {
		setupResponseMock();
		
		InputStream mockFile = Mockito.mock(InputStream.class);
		assertEquals(response, simpleHttpClient.putToURL(request, mockFile, 0L));
		ArgumentCaptor<HttpPut> captor = ArgumentCaptor.forClass(HttpPut.class);
		verify(mockHttpClient).execute(captor.capture());
		HttpPut captured = captor.getValue();
		assertEquals(request.getUri(), captured.getURI().toString());
		assertTrue(captured.getEntity() instanceof InputStreamEntity);
		assertFalse(captured.getEntity().isChunked());
		assertEquals(captured.getEntity().getContentLength(), 0L);
		assertEquals("value", captured.getHeaders("name")[0].getValue());
		verify(mockResponse).close();
	}

	@Test
	public void testDownloadFileWithNullFile() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {			
			simpleHttpClient.getFile(request, null);
		});
	}

	/*
	 * PLFM-4349
	 */
	@Test
	public void testGetFileFailedWithReasonInJson() throws Exception {
		setupResponseMock();
		File mockFile = Mockito.mock(File.class);
		FileOutputStream mockStream = Mockito.mock(FileOutputStream.class);
		when(mockProvider.getFileOutputStream(mockFile)).thenReturn(mockStream);
		when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_FORBIDDEN);
		response = new SimpleHttpResponse(HttpStatus.SC_FORBIDDEN, "reason", null, responseHeaders);
		assertEquals(response, simpleHttpClient.getFile(request, mockFile));
		ArgumentCaptor<HttpGet> captor = ArgumentCaptor.forClass(HttpGet.class);
		verify(mockHttpClient).execute(captor.capture());
		HttpGet captured = captor.getValue();
		assertEquals(request.getUri(), captured.getURI().toString());
		assertEquals("value", captured.getHeaders("name")[0].getValue());
		verify(mockResponse).close();
		verify(mockStream).close();
	}

	@Test
	public void testGetFileWithException() throws Exception {
		when(mockHttpClient.execute(any(HttpUriRequest.class))).thenReturn(mockResponse);
		File mockFile = Mockito.mock(File.class);
		FileOutputStream mockStream = Mockito.mock(FileOutputStream.class);
		when(mockProvider.getFileOutputStream(mockFile)).thenReturn(mockStream);
		when(mockResponse.getEntity()).thenThrow(new RuntimeException());
		try {
			simpleHttpClient.getFile(request, mockFile);
		} catch (RuntimeException e) {
			// expected
		}
		ArgumentCaptor<HttpGet> captor = ArgumentCaptor.forClass(HttpGet.class);
		verify(mockHttpClient).execute(captor.capture());
		HttpGet captured = captor.getValue();
		assertEquals(request.getUri(), captured.getURI().toString());
		assertEquals("value", captured.getHeaders("name")[0].getValue());
		verify(mockResponse).close();
		verify(mockStream).close();
	}

	@Test
	public void testGetFile() throws Exception {
		setupResponseMock();
		File mockFile = Mockito.mock(File.class);
		FileOutputStream mockStream = Mockito.mock(FileOutputStream.class);
		when(mockProvider.getFileOutputStream(mockFile)).thenReturn(mockStream);
		assertEquals(response, simpleHttpClient.getFile(request, mockFile));
		ArgumentCaptor<HttpGet> captor = ArgumentCaptor.forClass(HttpGet.class);
		verify(mockHttpClient).execute(captor.capture());
		HttpGet captured = captor.getValue();
		assertEquals(request.getUri(), captured.getURI().toString());
		assertEquals("value", captured.getHeaders("name")[0].getValue());
		verify(mockResponse).close();
		verify(mockStream).close();
	}

	@Test
	public void testExecuteWithNullRequest() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			simpleHttpClient.execute(null);
		});
	}

	@Test
	public void testExecuteWithNullResponseBody() throws Exception {
		setupResponseMock();
		assertEquals(response, simpleHttpClient.execute(new HttpGet("uri")));
		ArgumentCaptor<HttpGet> captor = ArgumentCaptor.forClass(HttpGet.class);
		verify(mockHttpClient).execute(captor.capture());
		HttpGet captured = captor.getValue();
		assertEquals("uri", captured.getURI().toString());
		verify(mockResponse).close();
	}

	@Test
	public void testExecuteWithResponseBody() throws Exception {
		setupResponseMock();
		HttpEntity mockHttpEntity = Mockito.mock(HttpEntity.class);
		when(mockResponse.getEntity()).thenReturn(mockHttpEntity);
		when(mockHttpEntity.getContent()).thenReturn(new ByteArrayInputStream("content".getBytes()));
		response = new SimpleHttpResponse(HttpStatus.SC_OK, "reason", "content", responseHeaders);
		assertEquals(response, simpleHttpClient.execute(new HttpGet("uri")));
		ArgumentCaptor<HttpGet> captor = ArgumentCaptor.forClass(HttpGet.class);
		verify(mockHttpClient).execute(captor.capture());
		HttpGet captured = captor.getValue();
		assertEquals("uri", captured.getURI().toString());
		verify(mockResponse).close();
	}

	@Test
	public void testConvertHeadersWithNull() {
		assertNull(SimpleHttpClientImpl.convertHeaders(null));
	}

	@Test
	public void testConvertHeadersWithEmptyArray() {
		assertEquals(new LinkedList<Header>(),
				SimpleHttpClientImpl.convertHeaders(new org.apache.http.Header[]{}));
	}

	@Test
	public void testConvertHeaders() {
		org.apache.http.Header header1 = new BasicHeader("name", "value");
		org.apache.http.Header header2 = new BasicHeader("name2", "value2");
		org.apache.http.Header[] toConvert = new org.apache.http.Header[]{header1, header2};
		List<Header> converted = new LinkedList<Header>();
		converted.add(new Header("name", "value"));
		converted.add(new Header("name2", "value2"));
		assertEquals(converted, SimpleHttpClientImpl.convertHeaders(toConvert));
	}

	@Test
	public void testExtractContentTypeWithNullRequest() {
		assertThrows(IllegalArgumentException.class, () -> {
			SimpleHttpClientImpl.extractContentType(null);
		});
	}

	@Test
	public void testExtractContentTypeWithNullHeader() {
		request = new SimpleHttpRequest();
		assertEquals(ContentType.APPLICATION_JSON,
				SimpleHttpClientImpl.extractContentType(request));
		assertEquals(ContentType.APPLICATION_JSON.toString(),
				request.getHeaders().get("Content-Type"));
	}

	@Test
	public void testExtractContentTypeWithEmptyContentTypeHeader() {
		assertEquals(ContentType.APPLICATION_JSON,
				SimpleHttpClientImpl.extractContentType(request));
		assertEquals(ContentType.APPLICATION_JSON.toString(),
				request.getHeaders().get("Content-Type"));
	}

	@Test
	public void testExtractContentTypeWithExistingContentTypeHeader() {
		request.getHeaders().put("Content-Type", ContentType.TEXT_PLAIN.toString());
		assertEquals(ContentType.TEXT_PLAIN.getCharset(),
				SimpleHttpClientImpl.extractContentType(request).getCharset());
		assertEquals(ContentType.TEXT_PLAIN.getMimeType(),
				SimpleHttpClientImpl.extractContentType(request).getMimeType());
		assertEquals(ContentType.TEXT_PLAIN.toString(),
				request.getHeaders().get("Content-Type"));
	}

	@Test
	public void testExtractContentTypeWithContentTypeHeaderWithoutCharset() {
		request.getHeaders().put("Content-Type", "text/plain");
		assertEquals(Charset.forName("UTF-8"),
				SimpleHttpClientImpl.extractContentType(request).getCharset());
		assertEquals("text/plain",
				SimpleHttpClientImpl.extractContentType(request).getMimeType());
		assertEquals("text/plain; charset=UTF-8",
				request.getHeaders().get("Content-Type"));
	}

	@Test
	public void testExtractContentTypeWithInvalidContentTypeHeader() {
		request.getHeaders().put("Content-Type", "");
		assertEquals(ContentType.APPLICATION_JSON,
				SimpleHttpClientImpl.extractContentType(request));
		assertEquals(ContentType.APPLICATION_JSON.toString(),
				request.getHeaders().get("Content-Type"));
	}

	@Test
	public void testGetFirstCookieValue_nullDomain(){
		assertThrows(IllegalArgumentException.class, () -> {
			simpleHttpClient.getFirstCookieValue(cookieDomain, null);
		});
	}

	@Test
	public void testGetFirstCookieValue_nullName(){
		assertThrows(IllegalArgumentException.class, () -> {
			simpleHttpClient.getFirstCookieValue(null, cookieName);
		});
	}

	@Test
	public void testGetFirstCookieValue_nullList(){
		when(mockCookieStore.getCookies()).thenReturn(null);

		//method under test
		String result = simpleHttpClient.getFirstCookieValue(cookieDomain, cookieName);

		verify(mockCookieStore).getCookies();
		verifyNoMoreInteractions(mockCookieStore);

		assertNull(result);
	}

	@Test
	public void testGetFirstCookieValue_noMatches(){
		when(mockCookieStore.getCookies()).thenReturn(Arrays.asList(matchingDomainNonMatchingNameCookie, nonMatchingDomainMatchingNameCookie));

		//method under test
		String result = simpleHttpClient.getFirstCookieValue(cookieDomain, cookieName);

		verify(mockCookieStore).getCookies();
		verifyNoMoreInteractions(mockCookieStore);

		assertNull(result);
	}


	@Test
	public void testGetFirstCookieValue_foundMatch(){
		when(mockCookieStore.getCookies()).thenReturn(Arrays.asList(matchingDomainNonMatchingNameCookie, matchingDomainAndNameCookie));

		//method under test
		String result = simpleHttpClient.getFirstCookieValue(cookieDomain, cookieName);

		verify(mockCookieStore).getCookies();
		verifyNoMoreInteractions(mockCookieStore);

		assertEquals(cookieValue, result);
	}


	@Test
	public void testAddCookie_nullDomain(){
		assertThrows(IllegalArgumentException.class, () -> {
			simpleHttpClient.addCookie(cookieDomain, null, cookieValue);
		});
	}

	@Test
	public void testAddCookie_nullName(){
		assertThrows(IllegalArgumentException.class, () -> {
			simpleHttpClient.addCookie(null, cookieName, cookieValue);
		});
	}

	@Test
	public void testAddCookie(){
		//method under test
		simpleHttpClient.addCookie(cookieDomain, cookieName, cookieValue);

		ArgumentCaptor<Cookie> cookieArgumentCaptor = ArgumentCaptor.forClass(Cookie.class);
		verify(mockCookieStore).addCookie(cookieArgumentCaptor.capture());
		verifyNoMoreInteractions(mockCookieStore);
		Cookie capturedCookie = cookieArgumentCaptor.getValue();

		assertEquals(cookieDomain, capturedCookie.getDomain());
		assertEquals(cookieName, capturedCookie.getName());
		assertEquals(cookieValue, capturedCookie.getValue());
	}

	@Test
	public void testClearAllCookies(){
		//method under test
		simpleHttpClient.clearAllCookies();
		verify(mockCookieStore).clear();
		verifyNoMoreInteractions(mockCookieStore);
	}
	
	private void setupResponseMock() throws IOException {
		when(mockHttpClient.execute(any(HttpUriRequest.class))).thenReturn(mockResponse);
		when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
		when(mockStatusLine.getReasonPhrase()).thenReturn("reason");
		when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
		when(mockResponse.getAllHeaders()).thenReturn(new org.apache.http.Header[]{});
	}
}
