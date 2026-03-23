package org.sagebionetworks.repo.model.grid.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class Base36UtilsTest {

	public enum TestCase {
		ZERO(0, "0"),
		ONE(1, "1"),
		NINE(9, "9"),
		TEN(10, "a"),
		THIRTY_FIVE(35, "z"),
		THIRTY_SIX(36, "10"),
		THOUSAND(1000, "rs"),
		MILLION(1000000, "lfls");

		final long value;
		final String expected;

		TestCase(long value, String expected) {
			this.value = value;
			this.expected = expected;
		}
	}

	@ParameterizedTest
	@EnumSource(TestCase.class)
	public void testEncodeDecode(TestCase testCase) {
		assertEquals(testCase.expected, Base36Utils.encodeBase36(testCase.value));
		assertEquals(testCase.value, Base36Utils.decodeBase36(testCase.expected));
	}

	@Test
	public void testEncodeBase36NegativeValue() {
		assertThrows(IllegalArgumentException.class, () -> {
			Base36Utils.encodeBase36(-1);
		});
	}

	@Test
	public void testDecodeBase36EmptyString() {
		assertThrows(IllegalArgumentException.class, () -> {
			Base36Utils.decodeBase36("");
		});
	}

	@Test
	public void testDecodeBase36InvalidCharacter() {
		assertThrows(IllegalArgumentException.class, () -> {
			Base36Utils.decodeBase36("abc!");
		});
	}

	@Test
	public void testDecodeBase36Null() {
		assertThrows(IllegalArgumentException.class, () -> {
			Base36Utils.decodeBase36(null);
		});
	}

	@Test
	public void testDecodeCaseInsensitivity() {
		assertEquals(10, Base36Utils.decodeBase36("A"));
	}
}
