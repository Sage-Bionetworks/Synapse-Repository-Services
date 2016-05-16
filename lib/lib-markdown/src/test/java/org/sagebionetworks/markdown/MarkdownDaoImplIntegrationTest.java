package org.sagebionetworks.markdown;

import static org.junit.Assert.*;

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
	public void test() {
		String rawMarkdown = "## a heading";
		String outputType = "html";
		String result = "<h2 toc=\"true\">a heading</h2>\n";
		assertEquals(result, dao.convertMarkdown(rawMarkdown, outputType));
	}

}
