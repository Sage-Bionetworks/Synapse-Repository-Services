package org.sagebionetworks.markdown;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

public class MarkdownDaoImplTest {
	@Mock
	MarkdownClient mockMarkdownClient;

	private MarkdownDaoImpl dao;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		dao = new MarkdownDaoImpl();
		ReflectionTestUtils.setField(dao, "markdownClient", mockMarkdownClient);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testConvertMarkdownWithNullMarkdown() {
		dao.convertMarkdown(null, null);
	}

	@Test
	public void testConvertMarkdownWithNullResponse() throws Exception {
		String rawMarkdown = "## a heading";
		String request = "{\"markdown\":\"## a heading\"}";
		when(mockMarkdownClient.requestMarkdownConversion(request)).thenReturn(null);
		assertNull(dao.convertMarkdown(rawMarkdown, null));
	}

	@Test
	public void testConvertMarkdown() throws Exception {
		String rawMarkdown = "## a heading";
		String outputType = "html";
		String request = "{\"markdown\":\"## a heading\",\"output\":\"html\"}";
		String result = "<h2 toc=\"true\">a heading</h2>\n";
		String response = "{\"result\":\"<h2 toc=\\\"true\\\">a heading</h2>\\n\"}";
		when(mockMarkdownClient.requestMarkdownConversion(request)).thenReturn(response);
		assertEquals(result, dao.convertMarkdown(rawMarkdown, outputType));
	}
}
