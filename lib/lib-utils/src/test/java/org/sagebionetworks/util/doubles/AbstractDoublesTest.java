package org.sagebionetworks.util.doubles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AbstractDoublesTest {
	
	@Test (expected=IllegalArgumentException.class)
	public void testLookupValueStringNull(){
		AbstractDouble.lookupType((String)null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testLookupValueDoubleNull(){
		AbstractDouble.lookupType((Double)null);
	}
	
	@Test (expected=NumberFormatException.class)
	public void testLookupValueFinite(){
		AbstractDouble.lookupType("1.1");
	}
	
	@Test (expected=NumberFormatException.class)
	public void testLookupValueString(){
		AbstractDouble.lookupType("some string");
	}

	@Test
	public void testLookupValueNaN(){
		assertEquals(AbstractDouble.NAN, AbstractDouble.lookupType("NaN"));
		assertEquals(AbstractDouble.NAN, AbstractDouble.lookupType("NAN"));
		assertEquals(AbstractDouble.NAN, AbstractDouble.lookupType("nan"));
	}
	
	@Test
	public void testLookupValuePositiveInfintiy(){
		//inf
		assertEquals(AbstractDouble.POSITIVE_INFINITY, AbstractDouble.lookupType("inf"));
		assertEquals(AbstractDouble.POSITIVE_INFINITY, AbstractDouble.lookupType("+inf"));
		assertEquals(AbstractDouble.POSITIVE_INFINITY, AbstractDouble.lookupType("+INF"));
		assertEquals(AbstractDouble.POSITIVE_INFINITY, AbstractDouble.lookupType("+inf"));
		// infinity
		assertEquals(AbstractDouble.POSITIVE_INFINITY, AbstractDouble.lookupType("infinity"));
		assertEquals(AbstractDouble.POSITIVE_INFINITY, AbstractDouble.lookupType("+infinity"));
		assertEquals(AbstractDouble.POSITIVE_INFINITY, AbstractDouble.lookupType("+INFINITY"));
		assertEquals(AbstractDouble.POSITIVE_INFINITY, AbstractDouble.lookupType("+Infinity"));
	}
	
	@Test
	public void testLookupValueNegativeInfintiy(){
		//inf
		assertEquals(AbstractDouble.NEGATIVE_INFINITY, AbstractDouble.lookupType("-inf"));
		assertEquals(AbstractDouble.NEGATIVE_INFINITY, AbstractDouble.lookupType("-INF"));
		// infinity
		assertEquals(AbstractDouble.NEGATIVE_INFINITY, AbstractDouble.lookupType("-infinity"));
		assertEquals(AbstractDouble.NEGATIVE_INFINITY, AbstractDouble.lookupType("-INFINITY"));
		assertEquals(AbstractDouble.NEGATIVE_INFINITY, AbstractDouble.lookupType("-Infinity"));
	}
	
	@Test
	public void testLookupTypeDouble(){
		assertEquals(AbstractDouble.POSITIVE_INFINITY, AbstractDouble.lookupType(Double.POSITIVE_INFINITY));
		assertEquals(AbstractDouble.NEGATIVE_INFINITY, AbstractDouble.lookupType(Double.NEGATIVE_INFINITY));
		assertEquals(AbstractDouble.NAN, AbstractDouble.lookupType(Double.NaN));
	}
	
	@Test
	public void testGetValue(){
		assertEquals(new Double(Double.POSITIVE_INFINITY), new Double(AbstractDouble.POSITIVE_INFINITY.getDoubleValue()));
		assertEquals(new Double(Double.NEGATIVE_INFINITY), new Double(AbstractDouble.NEGATIVE_INFINITY.getDoubleValue()));
		assertEquals(new Double(Double.NaN), new Double(AbstractDouble.NAN.getDoubleValue()));
	}
	
	@Test
	public void testGetStringValue(){
		assertEquals("Infinity", AbstractDouble.POSITIVE_INFINITY.getEnumerationValue());
		assertEquals("-Infinity", AbstractDouble.NEGATIVE_INFINITY.getEnumerationValue());
		assertEquals("NaN", AbstractDouble.NAN.getEnumerationValue());
	}
	
	@Test
	public void testIsAbstractValue(){
		assertFalse(AbstractDouble.isAbstractValue(null));
		assertFalse(AbstractDouble.isAbstractValue(1.2));
		assertTrue(AbstractDouble.isAbstractValue(Double.NaN));
		assertTrue(AbstractDouble.isAbstractValue(Double.NEGATIVE_INFINITY));
		assertTrue(AbstractDouble.isAbstractValue(Double.POSITIVE_INFINITY));
	}

}
