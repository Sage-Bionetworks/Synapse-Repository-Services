package org.sagebionetworks.repo.model;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.junit.Test;

/**
 * Test basic opperations of annaotations.
 * @author jmhill
 *
 */
public class AnnotationsTest {
	
	@Test
	public void testAddString(){
		Annotations anno = new Annotations();
		anno.addAnnotation("key1", "value1");
		anno.addAnnotation("key1", "value2");
		anno.addAnnotation("key2", "value3");
		Map<String, Collection<String>> map = anno.getStringAnnotations();
		assertNotNull(map);
		// There should be two collections, the first with two values
		assertEquals(2, map.size());
		Collection<String> valueone = map.get("key1");
		assertNotNull(valueone);
		assertEquals(2, valueone.size());
	}
	
	@Test
	public void testAddLong(){
		Annotations anno = new Annotations();
		anno.addAnnotation("key1", new Long(1));
		anno.addAnnotation("key1", new Long(2));
		anno.addAnnotation("key2", new Long(2));
		Map<String, Collection<Long>> map = anno.getLongAnnotations();
		assertNotNull(map);
		// There should be two collections, the first with two values
		assertEquals(2, map.size());
		Collection<Long> valueone = map.get("key1");
		assertNotNull(valueone);
		assertEquals(2, valueone.size());
	}
	
	@Test
	public void testAddDouble(){
		Annotations anno = new Annotations();
		anno.addAnnotation("key1", new Double(1.1));
		anno.addAnnotation("key1", new Double(2.2));
		anno.addAnnotation("key2", new Double(2.4));
		Map<String, Collection<Double>> map = anno.getDoubleAnnotations();
		assertNotNull(map);
		// There should be two collections, the first with two values
		assertEquals(2, map.size());
		Collection<Double> valueone = map.get("key1");
		assertNotNull(valueone);
		assertEquals(2, valueone.size());
	}
	
	@Test
	public void testAddDate(){
		Annotations anno = new Annotations();
		anno.addAnnotation("key1", new Date(123123L));
		anno.addAnnotation("key1", new Date(434345L));
		anno.addAnnotation("key2", new Date(345346L));
		Map<String, Collection<Date>> map = anno.getDateAnnotations();
		assertNotNull(map);
		// There should be two collections, the first with two values
		assertEquals(2, map.size());
		Collection<Date> valueone = map.get("key1");
		assertNotNull(valueone);
		assertEquals(2, valueone.size());
	}

}
