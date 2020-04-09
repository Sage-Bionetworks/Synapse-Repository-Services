package org.sagebionetworks.schema.semantic.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.schema.parser.ParseException;
import org.sagebionetworks.schema.parser.SchemaIdParser;

import nl.jqno.equalsverifier.EqualsVerifier;

public class DotSeparatedBuildIdentifiersTest {

	DotSeparatedBuildIdentifiers one;
	DotSeparatedBuildIdentifiers two;
	BuildIdentifier buildIdentifier;

	@BeforeEach
	public void beforeEach() throws ParseException {
		one = parse("abc.xyz");
		two = parse("abc");
		buildIdentifier = new BuildIdentifier("123xyz");
	}

	@Test
	public void testNullBuildIdentifier() {
		buildIdentifier = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			new DotSeparatedBuildIdentifiers(buildIdentifier, two);
		}).getMessage();
		assertEquals("BuildIdentifier cannot be null", message);
	}

	@Test
	public void testToStringWithDot() {
		DotSeparatedBuildIdentifiers dotSep = new DotSeparatedBuildIdentifiers(buildIdentifier, two);
		assertEquals("123xyz.abc", dotSep.toString());
	}

	@Test
	public void testToStringNoDott() {
		DotSeparatedBuildIdentifiers dotSep = new DotSeparatedBuildIdentifiers(buildIdentifier, null);
		assertEquals("123xyz", dotSep.toString());
	}

	@Test
	public void testHashAndEquals() {
		EqualsVerifier.forClass(DotSeparatedBuildIdentifiers.class)
				.withPrefabValues(DotSeparatedBuildIdentifiers.class, one, two).verify();
	}

	public DotSeparatedBuildIdentifiers parse(String input) throws ParseException {
		SchemaIdParser parser = new SchemaIdParser(input);
		return parser.dotSeparatedBuildIdentifiers();
	}
}
