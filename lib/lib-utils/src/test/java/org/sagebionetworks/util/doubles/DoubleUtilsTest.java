package org.sagebionetworks.util.doubles;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

class DoubleUtilsTest {
	@Test
	public void testFromString_InvalidString(){
		assertThrows(NumberFormatException.class, ()->{
			DoubleUtils.fromString("NotADouble");
		});
	}

	@Test
	public void testFromString_nullString(){
		assertThrows(IllegalArgumentException.class, ()->{
			DoubleUtils.fromString(null);
		});
	}

	@Test
	public void testFromString_NaN() {

		Set<String> nanStringPermutations = Sets.newHashSet( "nan", "Nan", "NaN", "NAn", "NAN", "nAn", "nAN", "naN");
		assertEquals(8, nanStringPermutations.size()); // 2^3 = 8

		for (String s : nanStringPermutations) {
			assertTrue(Double.isNaN(DoubleUtils.fromString(s)));
		}
	}
	@Test
	public void testFromString_normalDoubles() {
		assertEquals(-1.2, DoubleUtils.fromString("-1.2"));
		assertEquals(0.0, DoubleUtils.fromString("0"));
		assertEquals(3.14159265359, DoubleUtils.fromString("3.14159265359"));
	}

	@Test
	public void testFromString_Infinity(){
		Set<String> infinity = Sets.newHashSet( "inFinItY", "+inFinity", "iNf", "+inF", "-InF", "-inFiNiTy");
		for (String s : infinity) {
			assertTrue(Double.isInfinite(DoubleUtils.fromString(s)));
		}
	}
}