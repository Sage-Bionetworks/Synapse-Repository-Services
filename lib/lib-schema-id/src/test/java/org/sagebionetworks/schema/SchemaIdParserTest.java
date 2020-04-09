package org.sagebionetworks.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.schema.parser.ParseException;
import org.sagebionetworks.schema.parser.SchemaIdParser;
import org.sagebionetworks.schema.semantic.version.AlphanumericIdentifier;
import org.sagebionetworks.schema.semantic.version.Build;
import org.sagebionetworks.schema.semantic.version.BuildIdentifier;
import org.sagebionetworks.schema.semantic.version.NumericIdentifier;
import org.sagebionetworks.schema.semantic.version.Prerelease;
import org.sagebionetworks.schema.semantic.version.PrereleaseIdentifier;
import org.sagebionetworks.schema.semantic.version.SemanticVersion;
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
		assertThrows(ParseException.class, ()->{
			parser.versionCore();
		});
	}
	
	@Test
	public void testVersionCoreMinorLeadingZeror() throws ParseException {
		SchemaIdParser parser = new SchemaIdParser("1.05.2");
		assertThrows(ParseException.class, ()->{
			parser.versionCore();
		});
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
		assertThrows(ParseException.class, ()->{
			parser.alphanumericIdentifier();
		});
	}
	
	@Test
	public void testAlphaNumericStartWithZero() throws ParseException {
		SchemaIdParser parser = new SchemaIdParser("0123");
		assertThrows(ParseException.class, ()->{
			parser.alphanumericIdentifier();
		});
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
	
	@Test
	public void testPrerelease() throws ParseException {
		testPrerelease("alpha");
		testPrerelease("alpha.1");
		testPrerelease("0.3.7");
		testPrerelease("x.7.z.92");
	}
	
	public void testPrerelease(String input) throws ParseException {
		SchemaIdParser parser = new SchemaIdParser(input);
		Prerelease prerelease = parser.prerelease();
		assertEquals(prerelease.toString(), input);
	}
	
	@Test
	public void testBuildIdentifier() throws ParseException {
		testBuildIdentifier("alpha");
		testBuildIdentifier("123");
		testBuildIdentifier("0123");
	}
	
	public void testBuildIdentifier(String input) throws ParseException {
		SchemaIdParser parser = new SchemaIdParser(input);
		BuildIdentifier buildIdentifier = parser.buildIdentifier();
		assertEquals(buildIdentifier.toString(), input);
	}
	
	@Test
	public void testBuild() throws ParseException {
		testBuild("001");
		testBuild("20130313144700");
		testBuild("exp.sha.5114f85");
	}
	
	public void testBuild(String input) throws ParseException {
		SchemaIdParser parser = new SchemaIdParser(input);
		Build build = parser.build();
		assertEquals(build.toString(), input);
	}
	
	@Test
	public void testSemanticVersion() throws ParseException {
		testSemanticVersion("0.0.0");
		testSemanticVersion("1.23.456");
		testSemanticVersion("1.23.456-x.7.z.92");
		testSemanticVersion("1.23.456-x.7.z.92+exp.sha.5114f85");
	}
	
	public void testSemanticVersion(String input) throws ParseException {
		SchemaIdParser parser = new SchemaIdParser(input);
		SemanticVersion semanticVersion = parser.semanticVersion();
		assertEquals(input, semanticVersion.toString());
	}
}
