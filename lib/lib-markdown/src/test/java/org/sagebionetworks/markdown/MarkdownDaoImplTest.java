package org.sagebionetworks.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.sagebionetworks.markdown.MarkdownDaoImpl.BASE_URL;
import static org.sagebionetworks.markdown.MarkdownDaoImpl.MARKDOWN;
import static org.sagebionetworks.markdown.MarkdownDaoImpl.OUTPUT;

import com.amazonaws.services.lambda.AWSLambdaClient;
import org.json.JSONObject;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class MarkdownDaoImplTest {

	@Mock
	AWSLambdaClient mockLambdaClient;

	private MarkdownDaoImpl dao;

	@BeforeEach
	public void before() {
		MockitoAnnotations.initMocks(this);
		dao = new MarkdownDaoImpl();
		dao.setSynapseBaseUrl("https://www.synapse.org");
		dao.setStack("dev");
		ReflectionTestUtils.setField(dao, "lambdaClient", mockLambdaClient);
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
		JSONObject request = new JSONObject();
		request.put(MARKDOWN, rawMarkdown);
		request.put(BASE_URL, "https://www.synapse.org");
		request.put(OUTPUT, "html");
		ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
		InvokeResult expectedResult = new InvokeResult();
		expectedResult.setFunctionError("Some error in the lambda");
		when(mockLambdaClient.invoke(captor.capture())).thenReturn(expectedResult);

		assertThrows(MarkdownClientException.class, () -> {
			dao.convertMarkdown(rawMarkdown, "html");
		});

		InvokeRequest invokeRequest = captor.getValue();
		ByteBuffer payloadBuffer = invokeRequest.getPayload();
		String payloadString = new String(payloadBuffer.array(), StandardCharsets.UTF_8);
		JSONObject jsonPayload = new JSONObject(payloadString);
		assertEquals(rawMarkdown, jsonPayload.getString(MARKDOWN));
		assertTrue(jsonPayload.has(OUTPUT));
		assertEquals("https://www.synapse.org", jsonPayload.getString(BASE_URL));
		assertEquals("dev-markdownit:prod", invokeRequest.getFunctionName());

	}

	@Test
	public void testConvertMarkdown() throws Exception {
		String rawMarkdown = "## a heading";
		String outputType = "html";
		JSONObject request = new JSONObject();
		request.put(MARKDOWN, rawMarkdown);
		request.put(BASE_URL, "https://www.synapse.org");
		request.put(OUTPUT, outputType);
		String result = "<h2 toc=\"true\">a heading</h2>\n";
		String response = "{\"result\":\"<h2 toc=\\\"true\\\">a heading</h2>\\n\"}";
		ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
		InvokeResult expectedResult = new InvokeResult();
		ByteBuffer payload = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
		expectedResult.setPayload(payload);

		when(mockLambdaClient.invoke(any(InvokeRequest.class))).thenReturn(expectedResult);

		dao.convertMarkdown(rawMarkdown, "html");

		verify(mockLambdaClient).invoke(captor.capture());
		InvokeRequest invokeRequest = captor.getValue();
		ByteBuffer payloadBuffer = invokeRequest.getPayload();
		String payloadString = new String(payloadBuffer.array(), StandardCharsets.UTF_8);
		JSONObject jsonResponse = new JSONObject(payloadString);
		assertEquals(rawMarkdown, jsonResponse.getString(MARKDOWN));
		assertTrue(jsonResponse.has(OUTPUT));
		assertEquals("https://www.synapse.org", jsonResponse.getString(BASE_URL));
		assertEquals("dev-markdownit:prod", invokeRequest.getFunctionName());
		assertEquals(result, dao.convertMarkdown(rawMarkdown, outputType));

	}


	@Test
	public void testConvertMarkdownNullOutput() throws Exception {
		String rawMarkdown = "## a heading";
		String outputType = null;
		JSONObject request = new JSONObject();
		request.put(MARKDOWN, rawMarkdown);
		request.put(BASE_URL, "https://www.synapse.org");
		request.put(OUTPUT, outputType);
		String result = "<h2 toc=\"true\">a heading</h2>\n";
		String response = "{\"result\":\"<h2 toc=\\\"true\\\">a heading</h2>\\n\"}";
		ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
		InvokeResult expectedResult = new InvokeResult();
		ByteBuffer payload = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
		expectedResult.setPayload(payload);

		when(mockLambdaClient.invoke(any(InvokeRequest.class))).thenReturn(expectedResult);

		String actualResult = dao.convertMarkdown(rawMarkdown, outputType);

		verify(mockLambdaClient).invoke(captor.capture());
		InvokeRequest invokeRequest = captor.getValue();
		ByteBuffer payloadBuffer = invokeRequest.getPayload();
		String payloadString = new String(payloadBuffer.array(), StandardCharsets.UTF_8);
		JSONObject jsonResponse = new JSONObject(payloadString);
		assertEquals(rawMarkdown, jsonResponse.getString(MARKDOWN));
		assertFalse(jsonResponse.has(OUTPUT));
		assertEquals("https://www.synapse.org", jsonResponse.getString(BASE_URL));
		assertEquals("dev-markdownit:prod", invokeRequest.getFunctionName());
		assertEquals(result, actualResult);

	}
}
