package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

/**
 * Test basic operations of annotations.
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
		Map<String, List<String>> map = anno.getStringAnnotations();
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
		Map<String, List<Long>> map = anno.getLongAnnotations();
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
		Map<String, List<Double>> map = anno.getDoubleAnnotations();
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
		Map<String, List<Date>> map = anno.getDateAnnotations();
		assertNotNull(map);
		// There should be two collections, the first with two values
		assertEquals(2, map.size());
		Collection<Date> valueone = map.get("key1");
		assertNotNull(valueone);
		assertEquals(2, valueone.size());
	}
	@Test
	public void testDeleteNonExistent() {
		Annotations anno = new Annotations();
		anno.addAnnotation("key1", "value1");
		List deleted = anno.deleteAnnotation("key2");
		assertNull(deleted);
		assertNotNull(anno.getSingleValue("key1"));
	}
	
	@Test
	public void testDeleteString() {
		Annotations anno = new Annotations();
		anno.addAnnotation("key1", "value1");
		anno.addAnnotation("key2", "value2.1");
		anno.addAnnotation("key2", "value2.2");
		anno.addAnnotation("key3", "value3");
		List<String> deleted = (List<String>)anno.deleteAnnotation("key1");
		assertNotNull(deleted);
		Map<String, List<String>> map = anno.getStringAnnotations();
		assertNull(map.get("key1"));
		assertNull(anno.getAllValues("key1"));
	}
	
	@Test
	public void testDeleteLong() {
		Annotations anno = new Annotations();
		anno.addAnnotation("key1", new Long(1));
		anno.addAnnotation("key2", new Long(2));
		anno.addAnnotation("key2", new Long(3));
		anno.addAnnotation("key3", new Long(4));
		List<Long> deleted = (List<Long>)anno.deleteAnnotation("key2");
		assertNotNull(deleted);
		assertEquals(2, deleted.size());
		Map<String, List<Long>> map = anno.getLongAnnotations();
		assertNull(map.get("key2"));
		assertNull(anno.getAllValues("key2"));
	}
	
	@Test
	public void testDeleteDouble() {
		Annotations anno = new Annotations();
		anno.addAnnotation("key1", new Double(1));
		anno.addAnnotation("key2", new Double(2));
		anno.addAnnotation("key2", new Double(3));
		anno.addAnnotation("key3", new Double(4));
		List<Double> deleted = (List<Double>)anno.deleteAnnotation("key2");
		assertNotNull(deleted);
		assertEquals(2, deleted.size());
		Map<String, List<Double>> map = anno.getDoubleAnnotations();
		assertNull(map.get("key2"));
		assertNull(anno.getAllValues("key2"));
	}
	
	@Test
	public void testDeleteDate() {
		Annotations anno = new Annotations();
		anno.addAnnotation("key1", new Date());
		anno.addAnnotation("key2", new Date());
		anno.addAnnotation("key2", new Date());
		anno.addAnnotation("key3", new Date());
		List<Date> deleted = (List<Date>)anno.deleteAnnotation("key2");
		assertNotNull(deleted);
		assertEquals(2, deleted.size());
		Map<String, List<Date>> map = anno.getDateAnnotations();
		assertNull(map.get("key2"));
		assertNull(anno.getAllValues("key2"));
	}
	
	@Test
	public void testGetSingleValue__results_returned() {
		Annotations anno = new Annotations();
		anno.addAnnotation("key1", "value1");
		anno.addAnnotation("key1", "value2");
		anno.addAnnotation("key2", "value3");
		String result = (String)anno.getSingleValue("key1");
		assertNotNull(result);
		assertEquals("value1", result);
	}

	@Test
	public void testGetSingleValue__no_result() {
		Annotations anno = new Annotations();
		String result = (String)anno.getSingleValue("key1");
		assertNull(result);
	}

	@Test
	public void testGetStringArray() {
		Annotations anno = new Annotations();
		anno.addAnnotation("key1", "value1");
		anno.addAnnotation("key1", "value2");
		anno.addAnnotation("key2", "value3");
		List<String> result = (List<String>)anno.getAllValues("key1");
		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals("value1", result.iterator().next());
	}
	
	@Test
	public void testGetLongArray() {
		Annotations anno = new Annotations();
		anno.addAnnotation("key1", new Long(1));
		anno.addAnnotation("key1", new Long(2));
		anno.addAnnotation("key2", new Long(3));
		Collection<Long> result = (Collection<Long>)anno.getAllValues("key1");
		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals(new Long(1), result.iterator().next());
	}
	
	@Test
	public void testGetDoubleArray() {
		Annotations anno = new Annotations();
		anno.addAnnotation("key1", new Double(1));
		anno.addAnnotation("key1", new Double(2));
		anno.addAnnotation("key2", new Double(3));
		List<Double> result = (List<Double>)anno.getAllValues("key1");
		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals(new Double(1), result.iterator().next());
	}
	
	@Test
	public void testGetDateArray() {
		Annotations anno = new Annotations();
		Date v = new Date();
		anno.addAnnotation("key1", v);
		anno.addAnnotation("key1", new Date());
		anno.addAnnotation("key2", new Date());
		List<Date> result = (List<Date>)anno.getAllValues("key1");
		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals(v, result.iterator().next());
	}

	@Test
	public void testJSONRoundTrip() throws Exception{
		Annotations anno = new Annotations();
		anno.setId("9810");
		anno.setEtag("456");
		anno.setCreationDate(new Date(10*1000));
		anno.setUri("http://localhost:8080/");
		anno.addAnnotation("byteArray", "This is a bigString".getBytes("UTF-8"));
		anno.addAnnotation("string", "This is a bigString");
		anno.addAnnotation("string", "2");
		anno.addAnnotation("date", new Date(1000));
		anno.addAnnotation("double", new Double(3.5));
		anno.addAnnotation("long", new Long(123));
		anno.getStringAnnotations().put("nullString", null);
		anno.getStringAnnotations().put("emptyList", new ArrayList<String>());
		
		String json = EntityFactory.createJSONStringForEntity(anno);
		assertNotNull(json);
		Annotations clone = EntityFactory.createEntityFromJSONString(json, Annotations.class);
		assertEquals(anno, clone);
		
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testReplaceDate() {
		java.sql.Date wrongDate = new java.sql.Date(123L);
		Annotations anno = new Annotations();
		anno.replaceAnnotation("wrong", wrongDate);
	}

}
