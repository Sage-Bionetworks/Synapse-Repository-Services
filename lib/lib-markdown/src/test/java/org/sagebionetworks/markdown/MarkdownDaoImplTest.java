package org.sagebionetworks.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.sagebionetworks.markdown.MarkdownDaoImpl.BASE_URL;
import static org.sagebionetworks.markdown.MarkdownDaoImpl.MARKDOWN;
import static org.sagebionetworks.markdown.MarkdownDaoImpl.OUTPUT;

import org.json.JSONObject;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

@ExtendWith(MockitoExtension.class)
public class MarkdownDaoImplTest {

	@Mock
	LambdaClient mockLambdaClient;

	private MarkdownDaoImpl dao;

	@BeforeEach
	public void before() {
		dao = new MarkdownDaoImpl(mockLambdaClient, "https://www.synapse.org", "dev");
	}

	@Test
	public void testConvertMarkdownWithNullMarkdown() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			dao.convertMarkdown(null, null);
		});
	}

	@Test
	public void testConvertMarkdownWithError() throws Exception {
		String rawMarkdown = "## a heading";
		ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
		InvokeResponse expectedResponse = InvokeResponse.builder().functionError("Some error in the lambda").build();
		when(mockLambdaClient.invoke(captor.capture())).thenReturn(expectedResponse);

		assertThrows(MarkdownClientException.class, () -> {
			dao.convertMarkdown(rawMarkdown, "html");
		});

		InvokeRequest invokeRequest = captor.getValue();
		SdkBytes payloadBuffer = invokeRequest.payload();
		String payloadString = payloadBuffer.asUtf8String();
		JSONObject jsonPayload = new JSONObject(payloadString);
		assertEquals(rawMarkdown, jsonPayload.getString(MARKDOWN));
		assertTrue(jsonPayload.has(OUTPUT));
		assertEquals("https://www.synapse.org", jsonPayload.getString(BASE_URL));
		assertEquals("dev-markdownit:prod", invokeRequest.functionName());

	}

	@Test
	public void testConvertMarkdown() throws Exception {
		String rawMarkdown = "## a heading";
		String outputType = "html";
		String result = "<h2 toc=\"true\">a heading</h2>\n";
		String response = "{\"result\":\"<h2 toc=\\\"true\\\">a heading</h2>\\n\"}";
		ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
		SdkBytes payload = SdkBytes.fromUtf8String(response);
		InvokeResponse expectedResponse = InvokeResponse.builder().payload(payload).build();

		when(mockLambdaClient.invoke(any(InvokeRequest.class))).thenReturn(expectedResponse);

		dao.convertMarkdown(rawMarkdown, "html");

		verify(mockLambdaClient).invoke(captor.capture());
		InvokeRequest invokeRequest = captor.getValue();
		SdkBytes payloadBuffer = invokeRequest.payload();
		String payloadString = payloadBuffer.asUtf8String();
		JSONObject jsonResponse = new JSONObject(payloadString);
		assertEquals(rawMarkdown, jsonResponse.getString(MARKDOWN));
		assertTrue(jsonResponse.has(OUTPUT));
		assertEquals("https://www.synapse.org", jsonResponse.getString(BASE_URL));
		assertEquals("dev-markdownit:prod", invokeRequest.functionName());
		assertEquals(result, dao.convertMarkdown(rawMarkdown, outputType));

	}


	@Test
	public void testConvertMarkdownNullOutput() throws Exception {
		String rawMarkdown = "## a heading";
		String outputType = null;
		String result = "<h2 toc=\"true\">a heading</h2>\n";
		String response = "{\"result\":\"<h2 toc=\\\"true\\\">a heading</h2>\\n\"}";
		ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
		SdkBytes payload = SdkBytes.fromUtf8String(response);
		InvokeResponse expectedResponse = InvokeResponse.builder().payload(payload).build();

		when(mockLambdaClient.invoke(any(InvokeRequest.class))).thenReturn(expectedResponse);

		String actualResult = dao.convertMarkdown(rawMarkdown, outputType);

		verify(mockLambdaClient).invoke(captor.capture());
		InvokeRequest invokeRequest = captor.getValue();
		SdkBytes payloadBuffer = invokeRequest.payload();
		String payloadString = payloadBuffer.asUtf8String();
		JSONObject jsonResponse = new JSONObject(payloadString);
		assertEquals(rawMarkdown, jsonResponse.getString(MARKDOWN));
		assertFalse(jsonResponse.has(OUTPUT));
		assertEquals("https://www.synapse.org", jsonResponse.getString(BASE_URL));
		assertEquals("dev-markdownit:prod", invokeRequest.functionName());
		assertEquals(result, actualResult);

	}
}
