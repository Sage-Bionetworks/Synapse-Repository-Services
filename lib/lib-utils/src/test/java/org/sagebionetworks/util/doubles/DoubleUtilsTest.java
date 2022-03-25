package org.sagebionetworks.util.doubles;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

class DoubleUtilsTest {
	@Test
	public void testFromStringWithInvalidString(){
		assertThrows(NumberFormatException.class, ()->{
			DoubleUtils.fromString("NotADouble");
		});
	}

	@Test
	public void testFromStringWithNullString(){
		assertThrows(IllegalArgumentException.class, ()->{
			DoubleUtils.fromString(null);
		});
	}

	@Test
	public void testFromStringWithNaN() {

		Set<String> nanStringPermutations = Sets.newHashSet( "nan", "Nan", "NaN", "NAn", "NAN", "nAn", "nAN", "naN");
		assertEquals(8, nanStringPermutations.size()); // 2^3 = 8

		for (String s : nanStringPermutations) {
			assertTrue(Double.isNaN(DoubleUtils.fromString(s)));
		}
	}
	@Test
	public void testFromStringWithNormalDoubles() {
		assertEquals(-1.2, DoubleUtils.fromString("-1.2"));
		assertEquals(0.0, DoubleUtils.fromString("0"));
		assertEquals(3.14159265359, DoubleUtils.fromString("3.14159265359"));
	}

	@Test
	public void testFromStringWithInfinity(){
		Set<String> infinity = Sets.newHashSet( "inFinItY", "+inFinity", "iNf", "+inF", "-InF", "-inFiNiTy");
		for (String s : infinity) {
			assertTrue(Double.isInfinite(DoubleUtils.fromString(s)));
		}
	}
	
	@Test
	public void testToJSONStringWithNullInput() {
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			DoubleUtils.toJSONString(null);
		}).getMessage();
		
		assertEquals("The value is required.", message);
	}
	
	@Test
	public void testToJSONString() {
		Double value = 3.14;
		DoubleJSONStringWrapper expected = new DoubleJSONStringWrapper(value);
		assertEquals(expected, DoubleUtils.toJSONString(value));
	}
}