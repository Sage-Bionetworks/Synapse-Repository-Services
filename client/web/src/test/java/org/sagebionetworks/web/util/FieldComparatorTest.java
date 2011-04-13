package org.sagebionetworks.web.util;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.web.shared.Dataset;

/**
 * Simple test of the dataset DatasetComparator
 * @author jmhill
 *
 */
public class FieldComparatorTest {
	
	private static class TestClass{
		private String stringField = null;
		private Date dateField = null;
		private Integer integerField = null;
		public TestClass(String stringField, Date dateField,
				Integer integerField) {
			super();
			this.stringField = stringField;
			this.dateField = dateField;
			this.integerField = integerField;
		}
	}
	
	@Test
	public void testCompareString() throws Exception {
		// Create a few datset
		TestClass one = new TestClass("beta", new Date(0), new Integer(123));
		TestClass two = new TestClass("alpha", new Date(0), new Integer(123));
		TestClass allNull = new TestClass(null, null, null);
		
		// Now create a comparator on name
		FieldComparator<TestClass> comparator  = new FieldComparator<TestClass>(TestClass.class, "stringField");
		// Compare to null;
		int result = comparator.compare(one, allNull);
		assertEquals(1, result);
		result = comparator.compare(allNull, one);
		assertEquals(-1, result);
		result = comparator.compare(one, one);
		assertEquals(0, result);
		result = comparator.compare(allNull, allNull);
		assertEquals(0, result);
		result = comparator.compare(two, one);
		assertEquals(-1, result);
		result = comparator.compare(one, two);
		assertEquals(1, result);
		result = comparator.compare(one, null);
		assertEquals(1, result);
		result = comparator.compare(null, one);
		assertEquals(-1, result);
	}
	
	@Test
	public void testCompareDate() throws Exception {
		// Create a few datset
		TestClass one = new TestClass("beta", new Date(1), new Integer(123));
		TestClass two = new TestClass("alpha", new Date(2), new Integer(1234));
		TestClass allNull = new TestClass(null, null, null);
		
		// Now create a comparator on name
		FieldComparator<TestClass> comparator  = new FieldComparator<TestClass>(TestClass.class, "dateField");
		// Compare to null;
		int result = comparator.compare(one, allNull);
		assertEquals(1, result);
		result = comparator.compare(allNull, one);
		assertEquals(-1, result);
		result = comparator.compare(one, one);
		assertEquals(0, result);
		result = comparator.compare(allNull, allNull);
		assertEquals(0, result);
		result = comparator.compare(two, one);
		assertEquals(1, result);
		result = comparator.compare(one, two);
		assertEquals(-1, result);
	}

}
