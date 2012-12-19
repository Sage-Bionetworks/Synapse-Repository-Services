package org.sagebionetworks.logging;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

public class LoggingEncoderTest {

	private static String testString = "abcDEF123%?&=";
	private static String testEncodedString = "abcDEF123%25%3F%26%3D";

	@Test
	public void testEncode() throws Exception {
		assertEquals(testEncodedString, LoggingEncoder.encode(testString));
	}

	@Test
	public void testDecode() throws Exception {
		assertEquals(testString, LoggingEncoder.decode(testEncodedString));
	}

	@Test
	public void testDoEncodeDecode() throws Exception {
		for (char i = 32; i < 127; ++i) {
			doEncodeDecodeTest(i);
		}
	}

	private void doEncodeDecodeTest(char chr) throws IOException {
		StringBuilder encoder = new StringBuilder();
		LoggingEncoder.doEncode(encoder, chr);
		assertEquals(String.format("%%%02X", (int)chr), encoder.toString());
		StringBuilder decoder = new StringBuilder();

		LoggingEncoder.doDecode(decoder, new StringReader(encoder.toString().substring(1)));
		assertEquals(Character.toString(chr), decoder.toString());
	}
}
