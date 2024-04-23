package org.sagebionetworks.client.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseClientException;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.PartPresignedUrl;

@ExtendWith(MockitoExtension.class)
public class PartUploadCallableTest {

	@Mock
	private File mockFile;

	@Mock
	private SynapseClient mockSynapseClient;

	@Mock
	private CloseableHttpClient mockHttpClient;

	@Mock
	private CloseableHttpResponse mockHttpResponse;

	@Mock
	private StatusLine mockStatusLine;

	@Mock
	private FileInputStream mockFileInputStream;

	@Captor
	private ArgumentCaptor<HttpPut> httpPutCaptor;

	private FilePartRequest request;

	@BeforeEach
	public void before() {
		request = new FilePartRequest().setFile(mockFile).setHttpClient(mockHttpClient)
				.setSynapseClient(mockSynapseClient).setPartLength(101L).setPartNumber(1L).setPartOffset(33L)
				.setUploadId("555");
	}

	@Test
	public void testSkipBytes() throws IOException {
		
		when(mockFileInputStream.skip(anyLong())).thenReturn(30L,1L,2L,7L);
		
		// call under test
		PartUploadCallable.skipBytes(mockFileInputStream, 40);
		
		verify(mockFileInputStream, times(4)).skip(anyLong());
		verify(mockFileInputStream).skip(40L);
		verify(mockFileInputStream).skip(10L);
		verify(mockFileInputStream).skip(9L);
		verify(mockFileInputStream).skip(7L);
	}

	@Test
	public void testSkipBytesWithOverLimit() throws IOException {
		
		when(mockFileInputStream.skip(anyLong())).thenReturn(1L);
		
		String message = assertThrows(IOException.class, ()->{
			
			// call under test
			PartUploadCallable.skipBytes(mockFileInputStream, 101);
		}).getMessage();
		
		assertEquals("Failed to skip to file offset 101 after 100 tries", message);
		verify(mockFileInputStream, times(100)).skip(anyLong());
	}

	@Test
	public void testCall() throws Exception {

		PartUploadCallable callable = Mockito.spy(new PartUploadCallable(request));
		PartPresignedUrl part = new PartPresignedUrl().setUploadPresignedUrl("http://some.com/get/url")
				.setPartNumber(request.getPartNumber());
		when(mockSynapseClient.getMultipartPresignedUrlBatch(any()))
				.thenReturn(new BatchPresignedUploadUrlResponse().setPartPresignedUrls(listOf(part)));
		AddPartResponse expectedResponse = new AddPartResponse().setUploadId(request.getUploadId())
				.setPartNumber(request.getPartNumber());
		when(mockSynapseClient.addPartToMultipartUpload(any(), anyInt(), any())).thenReturn(expectedResponse);
		String md5 = "md5";
		doReturn("md5").when(callable).putToUrl(any());

		// call under test
		AddPartResponse response = callable.call();
		assertEquals(expectedResponse, response);

		verify(mockSynapseClient).getMultipartPresignedUrlBatch(new BatchPresignedUploadUrlRequest()
				.setUploadId(request.getUploadId()).setPartNumbers(listOf(request.getPartNumber())));
		verify(callable).putToUrl(part);
		verify(mockSynapseClient).addPartToMultipartUpload(request.getUploadId(), 1, md5);

	}

	@Test
	public void testPutToUrl() throws Exception {
		File temp = File.createTempFile("tetPutToUrl", ".bin");
		try {
			byte bytes[] = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
			FileUtils.writeByteArrayToFile(temp, bytes);
			request.setFile(temp);
			request.setPartOffset(3L);
			request.setPartLength(5L);

			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(bytes, request.getPartOffset().intValue(), request.getPartLength().intValue());
			String expectedMD5 = new String(Hex.encodeHex(digest.digest()));

			PartUploadCallable callable = new PartUploadCallable(request);

			String url = "https://foo.bar/okay";
			Map<String, String> headers = new HashMap<>(1);
			headers.put("someKey", "someValue");
			PartPresignedUrl partUrl = new PartPresignedUrl().setPartNumber(3L).setUploadPresignedUrl(url)
					.setSignedHeaders(headers);

			when(mockStatusLine.getStatusCode()).thenReturn(200);
			when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
			setupHttpClientExecute(url);

			// call under test
			String resultMD5 = callable.putToUrl(partUrl);

			assertEquals(expectedMD5, resultMD5);

			verify(mockHttpResponse).close();
			verify(mockHttpClient).execute((HttpUriRequest) httpPutCaptor.capture());
			HttpPut put = httpPutCaptor.getValue();
			assertEquals(url, put.getURI().toString());
			assertEquals(request.getPartLength(), put.getEntity().getContentLength());
			Header[] resultHeaders = put.getAllHeaders();
			assertEquals(1, resultHeaders.length);
			assertEquals("someKey", resultHeaders[0].getName());
			assertEquals("someValue", resultHeaders[0].getValue());
		} finally {
			temp.delete();
		}

	}
	
	@Test
	public void testPutToUrlWithNullHeaders() throws Exception {
		File temp = File.createTempFile("tetPutToUrl", ".bin");
		try {
			byte bytes[] = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
			FileUtils.writeByteArrayToFile(temp, bytes);
			request.setFile(temp);
			request.setPartOffset(3L);
			request.setPartLength(5L);

			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(bytes, request.getPartOffset().intValue(), request.getPartLength().intValue());
			String expectedMD5 = new String(Hex.encodeHex(digest.digest()));

			PartUploadCallable callable = new PartUploadCallable(request);

			String url = "https://foo.bar/okay";
			PartPresignedUrl partUrl = new PartPresignedUrl().setPartNumber(3L).setUploadPresignedUrl(url)
					.setSignedHeaders(null);

			when(mockStatusLine.getStatusCode()).thenReturn(200);
			when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
			setupHttpClientExecute(url);

			// call under test
			String resultMD5 = callable.putToUrl(partUrl);

			assertEquals(expectedMD5, resultMD5);

			verify(mockHttpResponse).close();
			verify(mockHttpClient).execute((HttpUriRequest) httpPutCaptor.capture());
			HttpPut put = httpPutCaptor.getValue();
			assertEquals(url, put.getURI().toString());
			assertEquals(request.getPartLength(), put.getEntity().getContentLength());
			Header[] resultHeaders = put.getAllHeaders();
			assertEquals(0, resultHeaders.length);
		} finally {
			temp.delete();
		}

	}

	@Test
	public void testPutToUrlWithError() throws Exception {
		File temp = File.createTempFile("tetPutToUrl", ".bin");
		try {
			byte bytes[] = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
			FileUtils.writeByteArrayToFile(temp, bytes);
			request.setFile(temp);
			request.setPartOffset(3L);
			request.setPartLength(5L);

			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(bytes, request.getPartOffset().intValue(), request.getPartLength().intValue());

			PartUploadCallable callable = new PartUploadCallable(request);

			String url = "https://foo.bar/okay";
			Map<String, String> headers = new HashMap<>(1);
			headers.put("someKey", "someValue");
			PartPresignedUrl partUrl = new PartPresignedUrl().setPartNumber(3L).setUploadPresignedUrl(url)
					.setSignedHeaders(headers);

			when(mockStatusLine.getStatusCode()).thenReturn(500);
			when(mockStatusLine.getReasonPhrase()).thenReturn("something went wrong");
			when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
			setupHttpClientExecute(url);
			
			String message = assertThrows(SynapseClientException.class, ()->{
				// call under test
				callable.putToUrl(partUrl);
			}).getMessage();
			assertEquals("PUT failed code: 500 reason: 'something went wrong'", message);

			verify(mockHttpResponse).close();
			verify(mockHttpClient).execute((HttpUriRequest) httpPutCaptor.capture());
			HttpPut put = httpPutCaptor.getValue();
			assertEquals(url, put.getURI().toString());
			assertEquals(request.getPartLength(), put.getEntity().getContentLength());
			Header[] resultHeaders = put.getAllHeaders();
			assertEquals(1, resultHeaders.length);
			assertEquals("someKey", resultHeaders[0].getName());
			assertEquals("someValue", resultHeaders[0].getValue());
		} finally {
			temp.delete();
		}
	}
	
	@Test
	public void testPartUploadCallableWithNullRequest() {
		request = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			new PartUploadCallable(request);
		}).getMessage();
		assertEquals("request is required.", message);
	}
	
	@Test
	public void testPartUploadCallableWithNullFile() {
		request.setFile(null);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			new PartUploadCallable(request);
		}).getMessage();
		assertEquals("request.file is required.", message);
	}
	
	@Test
	public void testPartUploadCallableWithNullHttpClient() {
		request.setHttpClient(null);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			new PartUploadCallable(request);
		}).getMessage();
		assertEquals("request.httpClient is required.", message);
	}
	
	@Test
	public void testPartUploadCallableWithNullPartLength() {
		request.setPartLength(null);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			new PartUploadCallable(request);
		}).getMessage();
		assertEquals("request.partLength is required.", message);
	}
	
	@Test
	public void testPartUploadCallableWithNullPartNumber() {
		request.setPartNumber(null);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			new PartUploadCallable(request);
		}).getMessage();
		assertEquals("request.partNumber is required.", message);
	}
	
	@Test
	public void testPartUploadCallableWithNullPartOffset() {
		request.setPartOffset(null);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			new PartUploadCallable(request);
		}).getMessage();
		assertEquals("request.partOffset is required.", message);
	}
	
	@Test
	public void testPartUploadCallableWithNullClient() {
		request.setSynapseClient(null);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			new PartUploadCallable(request);
		}).getMessage();
		assertEquals("request.synapseclient is required.", message);
	}
	
	@Test
	public void testPartUploadCallableWithNullUploadId() {
		request.setUploadId(null);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			new PartUploadCallable(request);
		}).getMessage();
		assertEquals("request.uploadId is required.", message);
	}


	void setupHttpClientExecute(String url) throws IOException, ClientProtocolException {
		doAnswer((i) -> {
			HttpPut httpPut = i.getArgument(0);
			InputStreamEntity entity = (InputStreamEntity) httpPut.getEntity();
			assertEquals(new URL(url).toURI(), httpPut.getURI());
			byte[] buffer = new byte[1024];
			entity.getContent().read(buffer, 0, (int) entity.getContentLength());
			return mockHttpResponse;
		}).when(mockHttpClient).execute(any());
	}

	static <E> List<E> listOf(E e) {
		return Stream.of(e).collect(Collectors.toList());
	}
}
