package org.sagebionetworks.markdown;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {MarkdownConfig.class})
public class MarkdownDaoImplIntegrationTest {

	@Autowired
	MarkdownDao dao;

	@Test
	void testSimpleText() throws Exception {
		String rawMarkdown = "## a heading";
		String outputType = "html";
		String expectedResult = "<h2 toc=\"true\">a heading</h2>\n";
		String result = dao.convertMarkdown(rawMarkdown, outputType);
		assertEquals(expectedResult, result);
	}

	@Test
	void testEntityId() throws Exception {
			String rawMarkdown = "testing Synapse link [Research Communities](#!Synapse:syn3722562/wiki/219258)";
		String outputType = "html";
		String result = "<p>testing Synapse link <a href=\"/Synapse:syn3722562/wiki/219258\">Research Communities</a></p>\n";
		assertEquals(result, dao.convertMarkdown(rawMarkdown, outputType));
	}
}
