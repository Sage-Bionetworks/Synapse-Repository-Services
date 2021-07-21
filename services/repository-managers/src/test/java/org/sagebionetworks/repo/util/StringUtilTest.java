package org.sagebionetworks.repo.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class StringUtilTest {
	
	String[] input = {"", "foo", "foo@bar.com", "foobar@bazblue.com", "foo@bar@bazblue.com"};
	String[] expectedOutput = {"", "foo", "f...o@bar.com", "foo...r@bazblue.com", "foo...r@bazblue.com"};

	@Test
	public void testObfuscateEmailAddress() {
		for (int i = 0; i < input.length; i++) {
			String actualOutput = StringUtil.obfuscateEmailAddress(input[i]);
			assertEquals(expectedOutput[i], actualOutput, "Obfuscation failed");
		}
	}
	
	@Test
	public void testLinesWithEmtptyInput() {
		String input = "";
		
		Stream<String> result = StringUtil.lines(input);
		
		assertEquals(Arrays.asList(""), result.collect(Collectors.toList()));
	}
	
	@Test
	public void testLinesWithSingleLine() {
		String input = "test";
		
		Stream<String> result = StringUtil.lines(input);
		
		assertEquals(Arrays.asList("test"), result.collect(Collectors.toList()));
	}	
	
	@Test
	public void testLinesWithMultiLine() {
		String input = "test\ntest";
		
		Stream<String> result = StringUtil.lines(input);
		
		assertEquals(Arrays.asList("test", "test"), result.collect(Collectors.toList()));
	}
	
	@Test
	public void testLinesWithMultiLineWin() {
		String input = "test\r\ntest";
		
		Stream<String> result = StringUtil.lines(input);
		
		assertEquals(Arrays.asList("test", "test"), result.collect(Collectors.toList()));
	}	
	
	@Test
	public void testLinesWithMultiLineOldMac() {
		String input = "test\rtest";
		
		Stream<String> result = StringUtil.lines(input);
		
		assertEquals(Arrays.asList("test", "test"), result.collect(Collectors.toList()));
	}
	
	@Test
	public void testLinesWithMultiLineEmpty() {
		String input = "test\n\ntest";
		
		Stream<String> result = StringUtil.lines(input);
		
		assertEquals(Arrays.asList("test", "", "test"), result.collect(Collectors.toList()));
	}

	@Test
	public void testLinesWithNullInput() {
		String input = null;
		
		Stream<String> result = StringUtil.lines(input);
		
		assertEquals(Collections.emptyList(), result.collect(Collectors.toList()));
	}
}
