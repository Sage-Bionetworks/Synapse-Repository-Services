package org.sagebionetworks.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = {MarkdownClientConfiguration.class})
public class MarkdownDaoImplIntegrationTest {

	@Autowired
	MarkdownDao dao;

	@Test
	public void testSimpleText() throws Exception {
		String rawMarkdown = "## a heading";
		String outputType = "html";
		String result = "<h2 toc=\"true\">a heading</h2>\n";
		assertEquals(result, dao.convertMarkdown(rawMarkdown, outputType));
	}

	@Test
	public void testEntityId() throws Exception {
		String rawMarkdown = "testing Synapse link [Research Communities](#!Synapse:syn3722562/wiki/219258)";
		String outputType = "html";
		String result = "<p>testing Synapse link <a href=\"/Synapse:syn3722562/wiki/219258\">Research Communities</a></p>\n";
		assertEquals(result, dao.convertMarkdown(rawMarkdown, outputType));
	}

	@Test
	public void testNullPayload() {
		assertThrows(IllegalArgumentException.class, () -> {
			dao.convertMarkdown(null, "html");
		});
	}
}
