package org.sagebionetworks.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.schema.id.DotSeparatedAlphaNumeric;
import org.sagebionetworks.schema.id.OrganizationName;
import org.sagebionetworks.schema.id.SchemaId;
import org.sagebionetworks.schema.id.SchemaName;
import org.sagebionetworks.schema.parser.ParseException;
import org.sagebionetworks.schema.parser.SchemaIdParser;
import org.sagebionetworks.schema.semantic.version.AlphanumericIdentifier;
import org.sagebionetworks.schema.semantic.version.Build;
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
		testAlphanumericIdentifier("a");
		testAlphanumericIdentifier("a1123");
		testAlphanumericIdentifier("aaa123");
		testAlphanumericIdentifier("abcdefghijklmnopqurstuvwxyz");
		testAlphanumericIdentifier("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
	}
	
	public void testAlphanumericIdentifier(String toTest) throws ParseException {
		SchemaIdParser parser = new SchemaIdParser(toTest);
		AlphanumericIdentifier alphanumeric = parser.alphanumericIdentifier();
		assertEquals(toTest, alphanumeric.toString());
	}
	
	@Test
	public void testAlphaNumericWithDash() throws ParseException {
		SchemaIdParser parser = new SchemaIdParser("-");
		assertThrows(ParseException.class, ()->{
			parser.alphanumericIdentifier();
		});
	}
	
	@Test
	public void testAlphaNumericWithSlash() throws ParseException {
		SchemaIdParser parser = new SchemaIdParser("/");
		assertThrows(ParseException.class, ()->{
			parser.alphanumericIdentifier();
		});
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
	public void testAlphaNumericWithZero() throws ParseException {
		SchemaIdParser parser = new SchemaIdParser("0");
		assertThrows(ParseException.class, ()->{
			parser.alphanumericIdentifier();
		});
	}
	
	@Test
	public void testPrereleaseIdentifier() throws ParseException {
		testPrereleaseIdentifier("123");
		testPrereleaseIdentifier("abc");
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
		testSemanticVersion("1.23.456+exp.sha.5114f85");
	}
	
	@Test
	public void testSemanticVersionWithDash() {
		assertThrows(ParseException.class, ()->{
			testSemanticVersion("-");
		});
	}
	
	public void testSemanticVersion(String input) throws ParseException {
		SchemaIdParser parser = new SchemaIdParser(input);
		SemanticVersion semanticVersion = parser.semanticVersion();
		assertEquals(input, semanticVersion.toString());
	}
	
	@Test
	public void testDotSeparatedAlphanumeric() throws ParseException {
		testDotSeparatedAlphanumeric("abc");
		testDotSeparatedAlphanumeric("abc.xyz");
		testDotSeparatedAlphanumeric("a1.b3.c4123");
	}
	
	@Test
	public void testDotSeparatedAlphanumericStrartDot() {
		assertThrows(ParseException.class, ()->{
			testDotSeparatedAlphanumeric(".abc");
		});
	}
	
	@Test
	public void testDotSeparatedAlphanumericEndDot() {
		assertThrows(ParseException.class, ()->{
			testDotSeparatedAlphanumeric("abc.");
		});
	}
	
	public void testDotSeparatedAlphanumeric(String input) throws ParseException {
		SchemaIdParser parser = new SchemaIdParser(input);
		DotSeparatedAlphaNumeric dotSeparatedAlphaNumeric = parser.dotSeparatedAlphaNumeric();
		assertEquals(input, dotSeparatedAlphaNumeric.toString());
	}
	
	@Test
	public void testDotSeparatedAlphanumericStartsWithNumber() {
		assertThrows(ParseException.class, ()->{
			testDotSeparatedAlphanumeric("1abc");
		});
	}
	
	@Test
	public void testDotSeparatedAlphanumericStartsWithZero() {
		assertThrows(ParseException.class, ()->{
			testDotSeparatedAlphanumeric("0");
		});
	}
	
	@Test
	public void testOrganziationName() throws ParseException {
		testOrganizationName("foo");
		testOrganizationName("foo.bar.a1");
	}
	
	@Test
	public void testOrganziationNameStartWithDot() {
		assertThrows(ParseException.class, ()->{
			testOrganizationName(".abc");
		});
	}
	
	@Test
	public void testOrganziationNameEndWithDot() {
		assertThrows(ParseException.class, ()->{
			testOrganizationName("abc.");
		});
	}
	
	@Test
	public void testOrganziationNameContainsNumers() {
		assertThrows(ParseException.class, ()->{
			testOrganizationName("foo.123.bar");
		});
	}
	
	@Test
	public void testOrganziationNameContainsZero() {
		assertThrows(ParseException.class, ()->{
			testOrganizationName("foo.0.bar");
		});
	}
	
	public void testOrganizationName(String input) throws ParseException {
		SchemaIdParser parser = new SchemaIdParser(input);
		OrganizationName organizationName = parser.organizationName();
		assertEquals(input, organizationName.toString());
	}
	
	@Test
	public void testSchemaName() throws ParseException {
		testSchemaName("SomeClass");
		testSchemaName("repo.model.SomeClass");
	}
	
	public void testSchemaName(String input) throws ParseException {
		SchemaIdParser parser = new SchemaIdParser(input);
		SchemaName schemaName = parser.schemaName();
		assertEquals(input, schemaName.toString());
	}
	
	@Test
	public void testSchemaIdNoVersion() throws ParseException {
		SchemaId id = testSchemaId("org.myorg-path.SomeClass");
		assertNotNull(id);
		assertNotNull(id.getOrganizationName());
		assertEquals("org.myorg",id.getOrganizationName().toString());
		assertNotNull(id.getSchemaName());
		assertEquals("path.SomeClass", id.getSchemaName().toString());
		assertNull(id.getSemanticVersion());
	}
	
	@Test
	public void testSchemaIdWithVersion() throws ParseException {
		SchemaId id = testSchemaId("org.myorg-path.SomeClass-1.2.3-alpha+1234f");
		assertNotNull(id);
		assertNotNull(id.getOrganizationName());
		assertEquals("org.myorg",id.getOrganizationName().toString());
		assertNotNull(id.getSchemaName());
		assertEquals("path.SomeClass", id.getSchemaName().toString());
		assertNotNull(id.getSemanticVersion());
		assertEquals("1.2.3-alpha+1234f", id.getSemanticVersion().toString());
	}
	
	@Test
	public void testSchemaIdWhiteSpace() throws ParseException {
		SchemaIdParser parser = new SchemaIdParser("\n  org.myorg-path.SomeClass-1.2.3-alpha+1234f \t");
		SchemaId schemaId = parser.schemaId();
		assertEquals("org.myorg-path.SomeClass-1.2.3-alpha+1234f", schemaId.toString());
	}
	
	@Test
	public void testSchemaIdWithVersionVersionPatchLeadingZero() throws ParseException {
		assertThrows(ParseException.class, ()->{
			testSchemaId("org.myorg-path.SomeClass-1.2.03");
		});
	}
	
	@Test
	public void testSchemaIdWithVersionVersionEndSlash() throws ParseException {
		assertThrows(ParseException.class, ()->{
			testSchemaId("org.myorg-path.SomeClass-");
		});
	}
	
	@Test
	public void testSchemaIdWithVersionVersionEndDot() throws ParseException {
		assertThrows(ParseException.class, ()->{
			testSchemaId("org.myorg-path.SomeClass.");
		});
	}
	
	public SchemaId testSchemaId(String input) throws ParseException {
		SchemaIdParser parser = new SchemaIdParser(input);
		SchemaId schemaId = parser.schemaId();
		assertEquals(input, schemaId.toString());
		return schemaId;
	}
	
	@Test
	public void testParseSchemaId() {
		SchemaId id = SchemaIdParser.parseSchemaId("org.valid-name");
		assertNotNull(id);
		assertEquals("org.valid-name", id.toString());
	}
	
	@Test
	public void testParseSchemaIdInvalid() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, ()->{
			SchemaIdParser.parseSchemaId("org.valid-name-0");
		});
		assertTrue(exception.getMessage().startsWith("Invalid '$id' : 'org.valid-name-0'"));
		assertTrue(exception.getCause() instanceof ParseException);
	}
	
	@Test
	public void testParseSchemaIdNullId() {
		String id = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			SchemaIdParser.parseSchemaId(id);
		}).getMessage();
		assertEquals("$id cannot be null", message);
	}
	
	@Test
	public void testParseOrganizationName() {
		OrganizationName orgName = SchemaIdParser.parseOrganizationName("org.valid");
		assertNotNull(orgName);
		assertEquals("org.valid", orgName.toString());
	}
	
	@Test
	public void testParseOrganizationNameInvalid() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, ()->{
			SchemaIdParser.parseOrganizationName("org.valid.0");
		});
		assertTrue(exception.getMessage().startsWith("Invalid 'organizationName' : 'org.valid.0"));
		assertTrue(exception.getCause() instanceof ParseException);
	}
	
	@Test
	public void testParseOrganizationNameContainsSlash() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, ()->{
			SchemaIdParser.parseOrganizationName("org.valid/bar");
		});
		assertTrue(exception.getMessage().startsWith("Invalid 'organizationName' : 'org.valid/bar"));
	}
	
	@Test
	public void testParseOrganizationNameContainsDash() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, ()->{
			SchemaIdParser.parseOrganizationName("org.valid-bar");
		});
		assertTrue(exception.getMessage().startsWith("Invalid 'organizationName' : 'org.valid-bar"));
	}
	
	@Test
	public void testParseOrganizationNameNull() {
		String name = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			SchemaIdParser.parseOrganizationName(name);
		}).getMessage();
		assertEquals("Organization name cannot be null", message);
	}
	
	@Test
	public void testParseSchemaIdFull() {
		SchemaId id = SchemaIdParser.parseSchemaId("test.integeration.organization-integration.test.Schema.json-1.45.67-alpha+beta");
		assertNotNull(id);
		assertEquals("test.integeration.organization-integration.test.Schema.json-1.45.67-alpha+beta", id.toString());
	}
	
	@Test
	public void testExtractRelative() {
		String start = "https://repo-prod.prod.sagebase.org/repo/v1/schema/type/registered/test.integeration.organization-integration.test.Schema.json-1.45.67-alpha+beta";
		String relative = SchemaIdParser.extractRelative$id(start);
		assertEquals("test.integeration.organization-integration.test.Schema.json-1.45.67-alpha+beta", relative);
	}
	
	@Test
	public void testExtractRelativeWithRelative() {
		String start = "test.integeration.organization-integration.test.Schema.json-1.45.67-alpha+beta";
		String relative = SchemaIdParser.extractRelative$id(start);
		assertEquals("test.integeration.organization-integration.test.Schema.json-1.45.67-alpha+beta", relative);
	}
	
	@Test
	public void testExtractRelativeWithNull() {
		String start = null;
		String relative = SchemaIdParser.extractRelative$id(start);
		assertEquals(null, relative);
	}
	
	@Test
	public void testParseSchemaIdWithAbsoluteFull() {
		String fullId = "https://repo-prod.prod.sagebase.org/repo/v1/schema/type/registered/test.integeration.organization-integration.test.Schema.json-1.45.67-alpha+beta";
		SchemaId id = SchemaIdParser.parseSchemaId(fullId);
		assertNotNull(id);
		assertEquals("test.integeration.organization-integration.test.Schema.json-1.45.67-alpha+beta", id.toString());
	}

}
