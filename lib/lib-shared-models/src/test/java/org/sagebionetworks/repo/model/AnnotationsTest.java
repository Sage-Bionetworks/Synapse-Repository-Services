package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
		
		String json = EntityFactory.createJSONStringForEntity(anno);
		System.out.println(json);
		assertNotNull(json);
		Annotations clone = EntityFactory.createEntityFromJSONString(json, Annotations.class);
		System.out.println(EntityFactory.createJSONStringForEntity(clone));
		assertEquals(anno, clone);
		
	}

}
