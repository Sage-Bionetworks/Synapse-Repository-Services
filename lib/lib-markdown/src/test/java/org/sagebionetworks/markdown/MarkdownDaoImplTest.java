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
	public void testConvertToHtmlWithNullMarkdown() {
		dao.convertToHtml(null);
	}

	@Test
	public void testConvertToHtmlWithNullResponse() throws Exception {
		String rawMarkdown = "## a heading";
		String request = "{\"markdown\":\"## a heading\"}";
		when(mockMarkdownClient.requestMarkdownConversion(request)).thenReturn(null);
		assertNull(dao.convertToHtml(rawMarkdown));
	}

	@Test
	public void testConvertToHtml() throws Exception {
		String rawMarkdown = "## a heading";
		String request = "{\"markdown\":\"## a heading\"}";
		String html = "<h2 toc=\"true\">a heading</h2>\n";
		String response = "{\"html\":\"<h2 toc=\\\"true\\\">a heading</h2>\\n\"}";
		when(mockMarkdownClient.requestMarkdownConversion(request)).thenReturn(response);
		assertEquals(html, dao.convertToHtml(rawMarkdown));
	}
}
