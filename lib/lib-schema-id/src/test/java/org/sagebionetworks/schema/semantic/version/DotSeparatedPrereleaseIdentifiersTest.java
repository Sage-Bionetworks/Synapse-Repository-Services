package org.sagebionetworks.schema.semantic.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.schema.parser.ParseException;
import org.sagebionetworks.schema.parser.SchemaIdParser;

import nl.jqno.equalsverifier.EqualsVerifier;

public class DotSeparatedPrereleaseIdentifiersTest {

	DotSeparatedPrereleaseIdentifiers one;
	DotSeparatedPrereleaseIdentifiers two;
	PrereleaseIdentifier prereleaseIdentifier;

	@BeforeEach
	public void beforeEach() throws ParseException {
		one = parse("abc.xyz");
		two = parse("abc");
		prereleaseIdentifier = new PrereleaseIdentifier(new NumericIdentifier(123L));
	}

	@Test
	public void testNullPrereleaseIdentifier() {
		prereleaseIdentifier = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			new DotSeparatedPrereleaseIdentifiers(prereleaseIdentifier, two);
		}).getMessage();
		assertEquals("PrereleaseIdentifier cannot be null", message);
	}

	@Test
	public void testToStringWithDot() {
		DotSeparatedPrereleaseIdentifiers dotSep = new DotSeparatedPrereleaseIdentifiers(prereleaseIdentifier, two);
		assertEquals("123.abc", dotSep.toString());
	}

	@Test
	public void testToStringNoDott() {
		DotSeparatedPrereleaseIdentifiers dotSep = new DotSeparatedPrereleaseIdentifiers(prereleaseIdentifier, null);
		assertEquals("123", dotSep.toString());
	}

	@Test
	public void testHashAndEquals() {
		EqualsVerifier.forClass(DotSeparatedPrereleaseIdentifiers.class)
				.withPrefabValues(DotSeparatedPrereleaseIdentifiers.class, one, two).verify();
	}

	public DotSeparatedPrereleaseIdentifiers parse(String input) throws ParseException {
		SchemaIdParser parser = new SchemaIdParser(input);
		return parser.dotSeparatedPrereleaseIdentifiers();
	}
}
