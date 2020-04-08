package org.sagebionetworks.schema;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.schema.parser.ParseException;
import org.sagebionetworks.schema.parser.SchemaIdParser;
import org.sagebionetworks.schema.semantic.version.AlphanumericIdentifier;
import org.sagebionetworks.schema.semantic.version.NumericIdentifier;
import org.sagebionetworks.schema.semantic.version.PrereleaseIdentifier;
import org.sagebionetworks.schema.semantic.version.VersionCore;

public class SchemaIdParserTest {
	

	@Test
	public void testNumericIdentifier() throws ParseException {
		// test all numbers from 0 to 100
		for(long i = 0; i<100; i++) {
			numericTest(i, Long.toString(i));
		}
	}
	
	void numericTest(Long expected, String input) throws ParseException {
		SchemaIdParser parser = new SchemaIdParser(input);
		NumericIdentifier numericId = parser.numericIdentifier();
		assertEquals(new NumericIdentifier(expected), numericId);
	}
	
	@Test
	public void testVersionCore() throws ParseException {
		SchemaIdParser parser = new SchemaIdParser("1.0.2");
		VersionCore core = parser.versionCore();
		assertNotNull(core);
		assertEquals(1L, core.getMajor().getValue());
		assertEquals(0L, core.getMinor().getValue());
		assertEquals(2L, core.getPatch().getValue());
	}
	
	@Test
	public void testVersionCoreAllZeros() throws ParseException {
		SchemaIdParser parser = new SchemaIdParser("0.0.0");
		VersionCore core = parser.versionCore();
		assertNotNull(core);
		assertEquals(0L, core.getMajor().getValue());
		assertEquals(0L, core.getMinor().getValue());
		assertEquals(0L, core.getPatch().getValue());
	}
	
	@Test
	public void testVersionCoreMajorLeadingZeror() throws ParseException {
		SchemaIdParser parser = new SchemaIdParser("01.0.2");
		String message = assertThrows(ParseException.class, ()->{
			parser.versionCore();
		}).getMessage();
		assertEquals("Encountered \" <positive_digit> \"1 \"\" at line 1, column 2.\r\n" + 
				"Was expecting:\r\n" + 
				"    \".\" ...\r\n" + 
				"    ", message);
	}
	
	@Test
	public void testVersionCoreMinorLeadingZeror() throws ParseException {
		SchemaIdParser parser = new SchemaIdParser("1.05.2");
		String message = assertThrows(ParseException.class, ()->{
			parser.versionCore();
		}).getMessage();
		assertEquals("Encountered \" <positive_digit> \"5 \"\" at line 1, column 4.\r\n" + 
				"Was expecting:\r\n" + 
				"    \".\" ...\r\n" + 
				"    ", message);
	}
	
	@Test
	public void testVersionCorePatchLeadingZeror() throws ParseException {
		SchemaIdParser parser = new SchemaIdParser("1.5.02");
		// while this will parse everything after the zero is lost, so it would fail in a larger context
		VersionCore core = parser.versionCore();
		// confirm the 2 is lost
		assertEquals("1.5.0", core.toString());
	}
	
	@Test
	public void testAlphaNumeric() throws ParseException {
		testAlphanumericIdentifier("-");
		testAlphanumericIdentifier("a");
		testAlphanumericIdentifier("a1123");
		testAlphanumericIdentifier("abcdefghijklmnopqurstuvwxyz");
		testAlphanumericIdentifier("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
		testAlphanumericIdentifier("z1-z2");
	}
	
	public void testAlphanumericIdentifier(String toTest) throws ParseException {
		SchemaIdParser parser = new SchemaIdParser(toTest);
		AlphanumericIdentifier alphanumeric = parser.alphanumericIdentifier();
		assertEquals(toTest, alphanumeric.toString());
	}
	
	@Test
	public void testAlphaNumericStartWithNumber() throws ParseException {
		SchemaIdParser parser = new SchemaIdParser("9abc");
		String message = assertThrows(ParseException.class, ()->{
			parser.alphanumericIdentifier();
		}).getMessage();
		assertEquals("Encountered \" <positive_digit> \"9 \"\" at line 1, column 1.\r\n" + 
				"Was expecting one of:\r\n" + 
				"    <letter> ...\r\n" + 
				"    \"-\" ...\r\n" + 
				"    ", message);
	}
	
	@Test
	public void testAlphaNumericStartWithZero() throws ParseException {
		SchemaIdParser parser = new SchemaIdParser("0123");
		String message = assertThrows(ParseException.class, ()->{
			parser.alphanumericIdentifier();
		}).getMessage();
		assertEquals("Encountered \" \"0\" \"0 \"\" at line 1, column 1.\r\n" + 
				"Was expecting one of:\r\n" + 
				"    <letter> ...\r\n" + 
				"    \"-\" ...\r\n" + 
				"    ", message);
	}
	
	@Test
	public void testPrereleaseIdentifier() throws ParseException {
		testPrereleaseIdentifier("123");
		testPrereleaseIdentifier("-abc");
	}
	
	public void testPrereleaseIdentifier(String input) throws ParseException {
		SchemaIdParser parser = new SchemaIdParser(input);
		PrereleaseIdentifier prereleaseIdentifier = parser.prereleaseIdentifier();
		assertEquals(prereleaseIdentifier.toString(), input);
	}
	
	@Test
	public void testPrereleaseIdentifierStartWithZero() throws ParseException {
		SchemaIdParser parser = new SchemaIdParser("045");
		// while this does not fail it does not parse anything after the zero
		PrereleaseIdentifier prereleaseIdentifier = parser.prereleaseIdentifier();
		// digits after the zero are lost
		assertEquals("0", prereleaseIdentifier.toString());
	}
	
}
