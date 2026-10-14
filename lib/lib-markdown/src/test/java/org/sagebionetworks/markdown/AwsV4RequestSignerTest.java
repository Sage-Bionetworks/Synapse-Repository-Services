package org.sagebionetworks.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;

@ExtendWith(MockitoExtension.class)
public class AwsV4RequestSignerTest {
	@Mock
	private AwsV4HttpSigner mockHttpSigner;

	private AwsV4RequestSigner signer;

	@BeforeEach
	public void before() {
		signer = new AwsV4RequestSigner(
			StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")),
			mockHttpSigner
		);
	}

	/**
	 * AwsV4HttpSigner returns headers via SdkHttpRequest.forEachHeader(), so a fake SignedRequest
	 * carrying the given headers is enough to stand in for a real signature.
	 */
	private static SignedRequest fakeSignedRequest(Map<String, String> headers) {
		SdkHttpRequest.Builder requestBuilder = SdkHttpRequest.builder()
			.method(SdkHttpMethod.POST)
			.uri(URI.create("https://example.com"));
		headers.forEach(requestBuilder::putHeader);
		return SignedRequest.builder().request(requestBuilder.build()).build();
	}

	@Test
	public void testSignReturnsSignedHeaders() throws Exception {
		String endpoint = "https://abc123.execute-api.us-east-1.amazonaws.com/prod/markdown";
		String payload = "{\"markdown\":\"## a heading\"}";
		byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

		Map<String, String> canned = new HashMap<>();
		canned.put("Authorization", "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20240101/us-east-1/execute-api/aws4_request, SignedHeaders=host;x-amz-date, Signature=abcdef0123456789");
		canned.put("X-Amz-Date", "20240101T000000Z");
		when(mockHttpSigner.sign(any(SignRequest.class))).thenReturn(fakeSignedRequest(canned));

		// call under test
		Map<String, String> signedHeaders = signer.sign(URI.create(endpoint), payloadBytes);

		assertNotNull(signedHeaders);
		assertFalse(signedHeaders.isEmpty());
	}

	@Test
	public void testSignIncludesAuthorizationHeader() throws Exception {
		String endpoint = "https://abc123.execute-api.us-east-1.amazonaws.com/prod/markdown";
		String payload = "{\"markdown\":\"## a heading\"}";
		byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

		Map<String, String> canned = new HashMap<>();
		canned.put("Authorization", "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20240101/us-east-1/execute-api/aws4_request, SignedHeaders=host;x-amz-date, Signature=abcdef0123456789");
		when(mockHttpSigner.sign(any(SignRequest.class))).thenReturn(fakeSignedRequest(canned));

		// call under test
		Map<String, String> signedHeaders = signer.sign(URI.create(endpoint), payloadBytes);

		// Verify the header returned by the signer is passed through unmodified
		assertEquals(canned.get("Authorization"), signedHeaders.get("Authorization"));
	}

	@Test
	public void testSignIncludesAmzDateHeader() throws Exception {
		String endpoint = "https://abc123.execute-api.us-east-1.amazonaws.com/prod/markdown";
		String payload = "{\"markdown\":\"## a heading\"}";
		byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

		Map<String, String> canned = new HashMap<>();
		canned.put("X-Amz-Date", "20240101T000000Z");
		when(mockHttpSigner.sign(any(SignRequest.class))).thenReturn(fakeSignedRequest(canned));

		// call under test
		Map<String, String> signedHeaders = signer.sign(URI.create(endpoint), payloadBytes);

		// Verify the header returned by the signer is passed through unmodified
		assertEquals(canned.get("X-Amz-Date"), signedHeaders.get("X-Amz-Date"));
	}

	@Test
	public void testSignWithSigningScope() throws Exception {
		String endpoint = "https://abc123.execute-api.us-east-1.amazonaws.com/prod/markdown";
		byte[] payloadBytes = "{\"markdown\":\"## a heading\"}".getBytes(StandardCharsets.UTF_8);

		Map<String, String> canned = new HashMap<>();
		canned.put("Authorization", "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/...");
		when(mockHttpSigner.sign(any(SignRequest.class))).thenReturn(fakeSignedRequest(canned));

		// call under test
		signer.sign(URI.create(endpoint), payloadBytes);

		// API Gateway rejects the request unless the signature is scoped to execute-api in the
		// region hosting the API, so the scope is fixed here rather than derived from the endpoint
		ArgumentCaptor<SignRequest> requestCaptor = ArgumentCaptor.forClass(SignRequest.class);
		verify(mockHttpSigner).sign(requestCaptor.capture());
		SignRequest capturedRequest = requestCaptor.getValue();
		assertEquals("execute-api", capturedRequest.property(AwsV4HttpSigner.SERVICE_SIGNING_NAME));
		assertEquals("us-east-1", capturedRequest.property(AwsV4HttpSigner.REGION_NAME));
	}

	@Test
	public void testSignWithMethodAndPayload() throws Exception {
		String endpoint = "https://abc123.execute-api.us-east-1.amazonaws.com/prod/markdown";
		String payload = "{\"markdown\":\"## a heading\"}";
		byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

		Map<String, String> canned = new HashMap<>();
		canned.put("Authorization", "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/...");
		when(mockHttpSigner.sign(any(SignRequest.class))).thenReturn(fakeSignedRequest(canned));

		// call under test
		signer.sign(URI.create(endpoint), payloadBytes);

		// The signature covers the method and a hash of the body, so both must reach the signer intact
		ArgumentCaptor<SignRequest> requestCaptor = ArgumentCaptor.forClass(SignRequest.class);
		verify(mockHttpSigner).sign(requestCaptor.capture());
		SignRequest capturedRequest = requestCaptor.getValue();
		assertEquals(SdkHttpMethod.POST, capturedRequest.request().method());
		ContentStreamProvider capturedPayload = (ContentStreamProvider) capturedRequest.payload().get();
		assertEquals(payload, new String(capturedPayload.newStream().readAllBytes(), StandardCharsets.UTF_8));
	}

	@Test
	public void testSignExcludesHostHeader() throws Exception {
		String endpoint = "https://abc123.execute-api.us-east-1.amazonaws.com/prod/markdown";
		String payload = "{\"markdown\":\"## a heading\"}";
		byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

		Map<String, String> canned = new HashMap<>();
		canned.put("Host", "abc123.execute-api.us-east-1.amazonaws.com");
		canned.put("Authorization", "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/...");
		canned.put("X-Amz-Date", "20240101T000000Z");
		when(mockHttpSigner.sign(any(SignRequest.class))).thenReturn(fakeSignedRequest(canned));

		// call under test
		Map<String, String> signedHeaders = signer.sign(URI.create(endpoint), payloadBytes);

		assertFalse(signedHeaders.containsKey("Host"));
		assertFalse(signedHeaders.containsKey("host"));
		// Sanity check the other headers weren't dropped along with Host
		assertTrue(signedHeaders.containsKey("Authorization"));
		assertTrue(signedHeaders.containsKey("X-Amz-Date"));
	}

	@Test
	public void testSignReturnsUnmodifiableMap() throws Exception {
		String endpoint = "https://abc123.execute-api.us-east-1.amazonaws.com/prod/markdown";
		String payload = "{\"markdown\":\"## a heading\"}";
		byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

		Map<String, String> canned = new HashMap<>();
		canned.put("Authorization", "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/...");
		when(mockHttpSigner.sign(any(SignRequest.class))).thenReturn(fakeSignedRequest(canned));

		// call under test
		Map<String, String> signedHeaders = signer.sign(URI.create(endpoint), payloadBytes);

		// Callers (e.g. MarkdownClient) must copy this map before mutating it
		assertThrows(UnsupportedOperationException.class, () -> signedHeaders.put("foo", "bar"));
	}

	@Test
	public void testSignWithEmptyPayload() throws Exception {
		String endpoint = "https://abc123.execute-api.us-east-1.amazonaws.com/prod/markdown";
		byte[] payloadBytes = new byte[0];

		Map<String, String> canned = new HashMap<>();
		canned.put("Authorization", "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/...");
		canned.put("X-Amz-Date", "20240101T000000Z");
		when(mockHttpSigner.sign(any(SignRequest.class))).thenReturn(fakeSignedRequest(canned));

		// call under test
		Map<String, String> signedHeaders = signer.sign(URI.create(endpoint), payloadBytes);

		assertNotNull(signedHeaders.get("Authorization"));
		assertNotNull(signedHeaders.get("X-Amz-Date"));

		// Verify the empty payload was still passed through to the signer rather than skipped
		ArgumentCaptor<SignRequest> requestCaptor = ArgumentCaptor.forClass(SignRequest.class);
		verify(mockHttpSigner).sign(requestCaptor.capture());
		ContentStreamProvider capturedPayload = (ContentStreamProvider) requestCaptor.getValue().payload().get();
		assertEquals(0, capturedPayload.newStream().readAllBytes().length);
	}

	@Test
	public void testSignWithDifferentEndpoints() throws Exception {
		String endpoint1 = "https://service1.execute-api.us-east-1.amazonaws.com/prod/markdown";
		String endpoint2 = "https://service2.execute-api.us-west-2.amazonaws.com/v1/markdown";
		String payload = "{\"markdown\":\"test\"}";
		byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

		Map<String, String> canned = new HashMap<>();
		canned.put("Authorization", "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/...");
		when(mockHttpSigner.sign(any(SignRequest.class))).thenReturn(fakeSignedRequest(canned));

		// call under test - each endpoint is signed independently
		signer.sign(URI.create(endpoint1), payloadBytes);
		signer.sign(URI.create(endpoint2), payloadBytes);

		// Verify each call was signed with its own endpoint rather than a cached/shared one
		ArgumentCaptor<SignRequest> requestCaptor = ArgumentCaptor.forClass(SignRequest.class);
		verify(mockHttpSigner, times(2)).sign(requestCaptor.capture());
		assertEquals(URI.create(endpoint1), requestCaptor.getAllValues().get(0).request().getUri());
		assertEquals(URI.create(endpoint2), requestCaptor.getAllValues().get(1).request().getUri());
	}
}
