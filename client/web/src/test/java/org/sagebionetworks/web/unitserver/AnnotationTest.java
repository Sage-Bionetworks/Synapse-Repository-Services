package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.web.shared.Annotations;

public class AnnotationTest {
	
	@Test
	public void testStrings(){
		// Create a new Annotations object
		Annotations anno = new Annotations();
		String key = "someKey";
		String value1 = "someValue1";
		String value2 = "someValue2";
		anno.addAnnotation(key, value1);
		anno.addAnnotation(key, value2);
		// Make sure we can find it
		Object result = anno.findFirstAnnotationValue(key);
		assertNotNull(result);
		assertTrue(result instanceof String);
		assertEquals(value1, (String)result);
		assertTrue(anno.getStringAnnotations().get(key).contains(value2));
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
		Long value1 = new Long(123);
		Long value2 = new Long(456);
		anno.addAnnotation(key, value1);
		anno.addAnnotation(key, value2);
		// Make sure we can find it
		Object result = anno.findFirstAnnotationValue(key);
		assertNotNull(result);
		assertTrue(result instanceof Long);
		assertEquals(value1, (Long)result);
		assertTrue(anno.getLongAnnotations().get(key).contains(value2));
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
