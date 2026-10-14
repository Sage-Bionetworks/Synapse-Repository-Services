package org.sagebionetworks.repo.manager.grid.row.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

/**
 * Tests the {@link Translator#translateLeniently} default methods, exercised
 * through the real {@link Translator} implementations (no mocking: these are
 * plain value objects).
 */
public class TranslatorTest {

	@Test
	public void testTranslateLenientlyWithNullAndRequiredPropertyIsNull() {
		// call under test
		ConValue con = new LongTranslator().translateLeniently(null, true);
		assertEquals(new ConValue(ConType.NULL, null), con);
	}

	@Test
	public void testTranslateLenientlyWithNullAndNotRequiredIsUndefined() {
		// call under test
		ConValue con = new LongTranslator().translateLeniently(null, false);
		assertEquals(new ConValue(ConType.UNDEFINED, null), con);
	}

	@Test
	public void testTranslateLenientlyWithBlankAndRequiredIsNull() {
		// call under test
		ConValue con = new LongTranslator().translateLeniently("", true);
		assertEquals(new ConValue(ConType.NULL, null), con);
	}

	@Test
	public void testTranslateLenientlyWithWhitespaceOnlyAndRequiredIsNull() {
		// call under test
		ConValue con = new LongTranslator().translateLeniently("   ", true);
		assertEquals(new ConValue(ConType.NULL, null), con);
	}

	@Test
	public void testTranslateLenientlyWithBlankAndNotRequiredIsUndefined() {
		// call under test
		ConValue con = new LongTranslator().translateLeniently("", false);
		assertEquals(new ConValue(ConType.UNDEFINED, null), con);
	}

	@Test
	public void testTranslateLenientlyWithUnparseableLongFallsBackToString() {
		// call under test
		ConValue con = new LongTranslator().translateLeniently("JH-2-009-518B9-A_1", true);
		assertEquals(new ConValue(ConType.STRING, "JH-2-009-518B9-A_1"), con);
	}

	@Test
	public void testTranslateLenientlyWithParseableLongTranslatesNormally() {
		// call under test
		ConValue con = new LongTranslator().translateLeniently("42", true);
		assertEquals(new ConValue(ConType.LONG, 42L), con);
	}

	@Test
	public void testTranslateLenientlyWithUnparseableDoubleFallsBackToString() {
		// call under test
		ConValue con = new DoubleTranslator().translateLeniently("not-a-double", true);
		assertEquals(new ConValue(ConType.STRING, "not-a-double"), con);
	}

	@Test
	public void testTranslateLenientlyWithUnparseableDateFallsBackToString() {
		// call under test
		ConValue con = new DateTranslator().translateLeniently("not-a-date", true);
		assertEquals(new ConValue(ConType.STRING, "not-a-date"), con);
	}

	@Test
	public void testTranslateLenientlyWithBlankDateIsNull() {
		// A blank cell in a DATE column carries no type information (it is legitimate
		// for the column to still be inferred as DATE), so it must not fall back to
		// the literal blank string.
		// call under test
		ConValue con = new DateTranslator().translateLeniently("", true);
		assertEquals(new ConValue(ConType.NULL, null), con);
	}

	@Test
	public void testTranslateLenientlyWithNonJsonFallsBackToString() {
		// call under test
		ConValue con = new JSONTranslator().translateLeniently("notJSON", true);
		assertEquals(new ConValue(ConType.STRING, "notJSON"), con);
	}

	@Test
	public void testTranslateLenientlyWithNoIsRequiredPropertyArgument() {
		// call under test
		ConValue con = new LongTranslator().translateLeniently("JH-2-009-518B9-A_1");
		assertEquals(new ConValue(ConType.STRING, "JH-2-009-518B9-A_1"), con);
	}

	@Test
	public void testTranslateLenientlyWithStringTranslatorMatchesStrictBehavior() {
		// StringTranslator never throws, so leniency must not change its behavior,
		// including for a blank value (which is a valid empty string, not "no value").
		// call under test
		ConValue con = new StringTranslator().translateLeniently("", true);
		assertEquals(new ConValue(ConType.STRING, ""), con);
	}

	@Test
	public void testTranslateLenientlyWithBooleanTranslatorMatchesStrictBehavior() {
		// BooleanTranslator never throws, so leniency must not change its behavior.
		// call under test
		ConValue con = new BooleanTranslator().translateLeniently("not-a-boolean", true);
		assertEquals(new ConValue(ConType.BOOLEAN, false), con);
	}

	@Test
	public void testTranslateLenientlyWithMalformedJsonArray() {
		// org.json throws JSONException, which is not an IllegalArgumentException, so
		// the narrowed catch must list it explicitly.
		// call under test
		ConValue con = new ArrayTranslator().translateLeniently("[1,2", true);
		assertEquals(new ConValue(ConType.STRING, "[1,2"), con);
	}

	@Test
	public void testTranslateLenientlyWithTranslatorDefect() {
		// Leniency tolerates a value that does not fit the column type, NOT a defect in
		// the translator itself — that must still fail loudly.
		Translator defective = string -> {
			throw new NullPointerException("boom");
		};
		// call under test
		assertThrows(NullPointerException.class, () -> defective.translateLeniently("anything", true));
	}

}
