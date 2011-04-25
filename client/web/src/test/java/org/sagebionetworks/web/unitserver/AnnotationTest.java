package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.web.shared.Annotations;

public class AnnotationTest {
	
	@Test
	public void testStrings(){
		// Create a new Annotations object
		Annotations anno = new Annotations();
		String key = "someKey";
		String value = "someValue";
		anno.addAnnotation(key, value);
		// Make sure we can find it
		Object result = anno.findFirstAnnotationValue(key);
		assertNotNull(result);
		assertTrue(result instanceof String);
		assertEquals(value, (String)result);
	}
	
	@Test
	public void testDate(){
		// Create a new Annotations object
		Annotations anno = new Annotations();
		String key = "someKey";
		Date value = new Date(System.currentTimeMillis());
		anno.addAnnotation(key, value);
		// Make sure we can find it
		Object result = anno.findFirstAnnotationValue(key);
		assertNotNull(result);
		assertTrue(result instanceof Date);
		assertEquals(value, (Date)result);
	}
	
	@Test
	public void testLong(){
		// Create a new Annotations object
		Annotations anno = new Annotations();
		String key = "someKey";
		Long value = new Long(123);
		anno.addAnnotation(key, value);
		// Make sure we can find it
		Object result = anno.findFirstAnnotationValue(key);
		assertNotNull(result);
		assertTrue(result instanceof Long);
		assertEquals(value, (Long)result);
	}
	
	@Test
	public void testDouble(){
		// Create a new Annotations object
		Annotations anno = new Annotations();
		String key = "someKey";
		Double value = new Double(123.3);
		anno.addAnnotation(key, value);
		// Make sure we can find it
		Object result = anno.findFirstAnnotationValue( key);
		assertNotNull(result);
		assertTrue(result instanceof Double);
		assertEquals(value, (Double)result);
	}

}
