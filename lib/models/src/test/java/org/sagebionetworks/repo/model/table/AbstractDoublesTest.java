package org.sagebionetworks.repo.model.table;

import static org.junit.Assert.*;

import org.junit.Test;

public class AbstractDoublesTest {
	
	@Test (expected=NumberFormatException.class)
	public void testLookupValueNull(){
		AbstractDoubles.lookupValue(null);
	}
	
	@Test (expected=NumberFormatException.class)
	public void testLookupValueFinite(){
		AbstractDoubles.lookupValue("1.1");
	}
	
	@Test (expected=NumberFormatException.class)
	public void testLookupValueString(){
		AbstractDoubles.lookupValue("some string");
	}

	@Test
	public void testLookupValueNaN(){
		assertEquals(AbstractDoubles.NAN, AbstractDoubles.lookupValue("NaN"));
		assertEquals(AbstractDoubles.NAN, AbstractDoubles.lookupValue("NAN"));
		assertEquals(AbstractDoubles.NAN, AbstractDoubles.lookupValue("nan"));
	}
	
	@Test
	public void testLookupValuePositiveInfintiy(){
		//inf
		assertEquals(AbstractDoubles.POSITIVE_INFINITY, AbstractDoubles.lookupValue("inf"));
		assertEquals(AbstractDoubles.POSITIVE_INFINITY, AbstractDoubles.lookupValue("+inf"));
		assertEquals(AbstractDoubles.POSITIVE_INFINITY, AbstractDoubles.lookupValue("+INF"));
		assertEquals(AbstractDoubles.POSITIVE_INFINITY, AbstractDoubles.lookupValue("+inf"));
		// infinity
		assertEquals(AbstractDoubles.POSITIVE_INFINITY, AbstractDoubles.lookupValue("infinity"));
		assertEquals(AbstractDoubles.POSITIVE_INFINITY, AbstractDoubles.lookupValue("+infinity"));
		assertEquals(AbstractDoubles.POSITIVE_INFINITY, AbstractDoubles.lookupValue("+INFINITY"));
		assertEquals(AbstractDoubles.POSITIVE_INFINITY, AbstractDoubles.lookupValue("+Infinity"));
	}
	
	@Test
	public void testLookupValueNegativeInfintiy(){
		//inf
		assertEquals(AbstractDoubles.NEGATIVE_INFINITY, AbstractDoubles.lookupValue("-inf"));
		assertEquals(AbstractDoubles.NEGATIVE_INFINITY, AbstractDoubles.lookupValue("-INF"));
		// infinity
		assertEquals(AbstractDoubles.NEGATIVE_INFINITY, AbstractDoubles.lookupValue("-infinity"));
		assertEquals(AbstractDoubles.NEGATIVE_INFINITY, AbstractDoubles.lookupValue("-INFINITY"));
		assertEquals(AbstractDoubles.NEGATIVE_INFINITY, AbstractDoubles.lookupValue("-Infinity"));
	}
	
	@Test
	public void testLookupTypeDouble(){
		assertEquals(AbstractDoubles.POSITIVE_INFINITY, AbstractDoubles.lookupType(Double.POSITIVE_INFINITY));
		assertEquals(AbstractDoubles.NEGATIVE_INFINITY, AbstractDoubles.lookupType(Double.NEGATIVE_INFINITY));
		assertEquals(AbstractDoubles.NAN, AbstractDoubles.lookupType(Double.NaN));
	}
	
	@Test
	public void testGetValue(){
		assertEquals(new Double(Double.POSITIVE_INFINITY), new Double(AbstractDoubles.POSITIVE_INFINITY.getDoubleValue()));
		assertEquals(new Double(Double.NEGATIVE_INFINITY), new Double(AbstractDoubles.NEGATIVE_INFINITY.getDoubleValue()));
		assertEquals(new Double(Double.NaN), new Double(AbstractDoubles.NAN.getDoubleValue()));
	}
	
	@Test
	public void testGetStringValue(){
		assertEquals("Infinity", AbstractDoubles.POSITIVE_INFINITY.getEnumerationValue());
		assertEquals("-Infinity", AbstractDoubles.NEGATIVE_INFINITY.getEnumerationValue());
		assertEquals("NaN", AbstractDoubles.NAN.getEnumerationValue());
	}
	
	@Test
	public void testIsAbstractValue(){
		assertFalse(AbstractDoubles.isAbstractValue(null));
		assertFalse(AbstractDoubles.isAbstractValue(1.2));
		assertTrue(AbstractDoubles.isAbstractValue(Double.NaN));
		assertTrue(AbstractDoubles.isAbstractValue(Double.NEGATIVE_INFINITY));
		assertTrue(AbstractDoubles.isAbstractValue(Double.POSITIVE_INFINITY));
	}

}
