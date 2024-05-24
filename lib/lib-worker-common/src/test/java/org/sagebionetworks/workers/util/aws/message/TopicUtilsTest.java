package org.sagebionetworks.workers.util.aws.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class TopicUtilsTest {

	@Test
	public void generateSourceArnWithNull() {
		assertThrows(IllegalArgumentException.class, () -> {			
			TopicUtils.generateSourceArn(null);
		});
	}
	
	@Test
	public void generateSourceArnWithEmptyList() {
		assertThrows(IllegalArgumentException.class, () -> {
			TopicUtils.generateSourceArn(new ArrayList<String>());
		});
	}
	
	@Test
	public void generateSourceArnWithValidLists() {
		assertEquals("\"one\"", TopicUtils.generateSourceArn(Arrays.asList("one")));
		assertEquals("[ \"one\", \"two\" ]", TopicUtils.generateSourceArn(Arrays.asList("one","two")));
	}
	
	@Test
	public void containsAllTopicsTest() {
		assertTrue(TopicUtils.containsAllTopics(null, new ArrayList<String>()));
		assertTrue(TopicUtils.containsAllTopics("{one}", Arrays.asList("one")));
		assertTrue(TopicUtils.containsAllTopics("{one,two,three}", Arrays.asList("one","three","two")));
		assertFalse(TopicUtils.containsAllTopics("{one,two}", Arrays.asList("one","three","two")));
	}
}
