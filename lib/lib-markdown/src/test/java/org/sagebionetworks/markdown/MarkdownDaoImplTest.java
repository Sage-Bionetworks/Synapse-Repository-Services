package org.sagebionetworks.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.markdown.MarkdownDaoImpl.MARKDOWN;
import static org.sagebionetworks.markdown.MarkdownDaoImpl.OUTPUT;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MarkdownDaoImplTest {
	@Mock
	MarkdownClient mockMarkdownClient;

	private MarkdownDaoImpl dao;

	@BeforeEach
	public void before() {
		dao = new MarkdownDaoImpl(mockMarkdownClient, "https://synapse.org");
	}

	@Test
	public void testConvertMarkdownWithNullMarkdown() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			dao.convertMarkdown(null, null);
		});
		verify(mockMarkdownClient, never()).requestMarkdownConversion(any(String.class));
	}

	@Test
	public void testConvertMarkdownWithClientException() throws Exception {
		String rawMarkdown = "## a heading";
		JSONObject request = new JSONObject();
		request.put(MARKDOWN, rawMarkdown);
		request.put(MarkdownDaoImpl.BASE_URL, "https://synapse.org");
		when(mockMarkdownClient.requestMarkdownConversion(request.toString())).thenThrow(new MarkdownClientException(500,""));
		assertThrows(MarkdownClientException.class, () -> {
			dao.convertMarkdown(rawMarkdown, null);
		});
	}

	@Test
	public void testConvertMarkdownWithMissingResultKey() throws Exception {
		String rawMarkdown = "## a heading";
		JSONObject request = new JSONObject();
		request.put(MARKDOWN, rawMarkdown);
		request.put(MarkdownDaoImpl.BASE_URL, "https://synapse.org");
		when(mockMarkdownClient.requestMarkdownConversion(request.toString())).thenReturn("{}");
		assertThrows(JSONException.class, () -> {
			dao.convertMarkdown(rawMarkdown, null);
		});
	}

	@Test
	public void testConvertMarkdown() throws Exception {
		String rawMarkdown = "## a heading";
		String outputType = "html";
		JSONObject request = new JSONObject();
		request.put(MARKDOWN, rawMarkdown);
		request.put(MarkdownDaoImpl.BASE_URL, "https://synapse.org");
		request.put(OUTPUT, outputType);
		String result = "<h2 toc=\"true\">a heading</h2>\n";
		String response = "{\"result\":\"<h2 toc=\\\"true\\\">a heading</h2>\\n\"}";
		when(mockMarkdownClient.requestMarkdownConversion(request.toString())).thenReturn(response);
		assertEquals(result, dao.convertMarkdown(rawMarkdown, outputType));
	}

	@Test
	public void testGetSynapseBaseUrl() {
		assertEquals("https://synapse.org", dao.getSynapseBaseUrl());
	}
}
