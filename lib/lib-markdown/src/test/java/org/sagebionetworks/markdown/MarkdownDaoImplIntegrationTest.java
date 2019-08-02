package org.sagebionetworks.markdown;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
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
		String rawMarkdown = "syn12345";
		String outputType = "html";
		String result = "<p><a href=\"https://www.synapse.org/#!Synapse:syn12345\" target=\"_blank\" ref=\"noopener noreferrer\">syn12345</a></p>\n";
		assertEquals(result, dao.convertMarkdown(rawMarkdown, outputType));
	}
}
