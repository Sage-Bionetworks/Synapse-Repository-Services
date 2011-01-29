package org.sagebionetworks.web.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Simple test for the Random string builder.
 * 
 * @author jmhill
 *
 */
public class RandomStringsTest {
	
	@Test
	public void testRandomWord(){
		StringBuilder builder = new StringBuilder();
		// Generate the word
		RandomStrings.generateRandomWord(builder, 10);
		String word = builder.toString();
		assertNotNull(word);
		System.out.println(word);
		assertEquals(10, word.length());
	}
	
	@Test
	public void testRandomString(){
		StringBuilder builder = new StringBuilder();
		// Generate the word
		RandomStrings.generateRandomString(builder, 5, 15);
		String string = builder.toString();
		assertNotNull(string);
		System.out.println(string);
		// There should be 5 words
		String[] split = string.split(" ");
		assertNotNull(split);
		assertEquals(5, split.length);
	}
	
	@Test
	public void testOtherRandomString(){
		String string = RandomStrings.generateRandomString(1, 10);
		assertNotNull(string);
		System.out.println(string);
		// Trimming should not change it size.
		int size = string.length();
		int afterTrim = string.trim().length();
		assertEquals("The random strings should not have leading or trailing spaces.",size, afterTrim);
		
	}
	
	@Test
	public void testRandomUrl(){
		String url = RandomStrings.generateRandomUrl(5, 8);
		assertNotNull(url);
		System.out.println(url);
	}

}
