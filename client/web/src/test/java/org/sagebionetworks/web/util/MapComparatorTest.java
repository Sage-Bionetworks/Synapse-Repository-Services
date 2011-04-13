package org.sagebionetworks.web.util;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * Basic test for the map comparator.
 * @author jmhill
 *
 */
public class MapComparatorTest {
	
	String key;
	MapComarator comp;
	Map<String, Object> one;
	Map<String, Object> two;
	
	@Before
	public void setup(){
		key = "someKey";
		comp = new MapComarator(key);
		one = new HashMap<String, Object>();
		two = new HashMap<String, Object>();
	}
	
	@Test
	public void testNulls(){
		int result = comp.compare(null, null);
		assertEquals(0, result);
		// Now make one non-null
		result = comp.compare(one, null);
		assertEquals(1, result);
		// Flip
		result = comp.compare(null, two);
		assertEquals(-1, result);
		
		// now two but both with null values
		result = comp.compare(one, two);
		assertEquals(0, result);
		
		// set a value on the first
		// now two but both with null values
		one = new HashMap<String, Object>();
		one.put(key, "not null");
		two = new HashMap<String, Object>();
		result = comp.compare(one, two);
		assertEquals(1, result);
		
		// flip
		one = new HashMap<String, Object>();
		two = new HashMap<String, Object>();
		two.put(key, "not null");
		result = comp.compare(one, two);
		assertEquals(-1, result);
	}
	
	@Test
	public void testEquals(){
		one.put(key, new Integer(1));
		two.put(key, new Integer(1));
		int result = comp.compare(one, two);
		assertEquals(0, result);
	}
	
	@Test
	public void testGeater(){
		one.put(key, new Integer(100));
		two.put(key, new Integer(1));
		int result = comp.compare(one, two);
		assertEquals(1, result);
	}
	
	@Test
	public void testLesser(){
		one.put(key, new Integer(1));
		two.put(key, new Integer(99));
		int result = comp.compare(one, two);
		assertEquals(-1, result);
	}
	
	@Test
	public void testArraysEqual(){
		one.put(key, new String[] {"a","b","c"});
		two.put(key, new String[] {"a","b","c"});
		int result = comp.compare(one, two);
		assertEquals(0, result);
	}
	
	@Test
	public void testArraysGeaterOnSize(){
		one.put(key, new String[] {"a","b","c","d"});
		two.put(key, new String[] {"a","b","c"});
		int result = comp.compare(one, two);
		assertEquals(1, result);
	}
	@Test
	public void testArraysLesserOnSize(){
		one.put(key, new String[] {"a","b",});
		two.put(key, new String[] {"a","b","c"});
		int result = comp.compare(one, two);
		assertEquals(-1, result);
	}
	
	@Test
	public void testArraysGeaterOnValue(){
		one.put(key, new String[] {"a","b","b"});
		two.put(key, new String[] {"a","b","a"});
		int result = comp.compare(one, two);
		assertEquals(1, result);
	}
	
	@Test
	public void testArraysLesserOnValue(){
		one.put(key, new String[] {"a","b","a"});
		two.put(key, new String[] {"a","b","b"});
		int result = comp.compare(one, two);
		assertEquals(-1, result);
	}

}
