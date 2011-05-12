package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAnnotations;
import org.sagebionetworks.repo.model.jdo.persistence.JDODateAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDODoubleAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOLongAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOStringAnnotation;

/**
 * Basic test for converting between JDOs and DTOs.
 * 
 * @author jmhill
 *
 */
public class JDOAnnotationsUtilsTest {
	
	JDOAnnotations owner;
	
	@Before
	public void before(){
		// Each test starts with a new owner
		owner = new JDOAnnotations();
	}
	
	@Test
	public void testCreateAnnotaionString(){
		String key = "someKey";
		String value = "someValue";
		Object result = JDOAnnotationsUtils.createAnnotaion(owner, key, value);
		assertNotNull(result);
		assertTrue(result instanceof JDOStringAnnotation);
		JDOStringAnnotation anno = (JDOStringAnnotation) result;
		assertEquals(owner, anno.getOwner());
		assertEquals(key, anno.getAttribute());
		assertEquals(value, anno.getValue());
	}
	
	@Test
	public void testCreateAnnotaionDate(){
		String key = "someKey";
		Date value = new Date(System.currentTimeMillis());
		Object result = JDOAnnotationsUtils.createAnnotaion(owner, key, value);
		assertNotNull(result);
		assertTrue(result instanceof JDODateAnnotation);
		JDODateAnnotation anno = (JDODateAnnotation) result;
		assertEquals(owner, anno.getOwner());
		assertEquals(key, anno.getAttribute());
		assertEquals(value, anno.getValue());
	}
	
	@Test
	public void testCreateAnnotaionLong(){
		String key = "someKey";
		Long value = new Long(System.currentTimeMillis());
		Object result = JDOAnnotationsUtils.createAnnotaion(owner, key, value);
		assertNotNull(result);
		assertTrue(result instanceof JDOLongAnnotation);
		JDOLongAnnotation anno = (JDOLongAnnotation) result;
		assertEquals(owner, anno.getOwner());
		assertEquals(key, anno.getAttribute());
		assertEquals(value, anno.getValue());
	}
	
	@Test
	public void testCreateAnnotaionDouble(){
		String key = "someKey";
		Double value = new Double(1234.567);
		Object result = JDOAnnotationsUtils.createAnnotaion(owner, key, value);
		assertNotNull(result);
		assertTrue(result instanceof JDODoubleAnnotation);
		JDODoubleAnnotation anno = (JDODoubleAnnotation) result;
		assertEquals(owner, anno.getOwner());
		assertEquals(key, anno.getAttribute());
		assertEquals(value, anno.getValue());
	}
	
	@Test
	public void testCreateFromMap(){
		Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();
		// populate a test map
		// One
		Collection<String> valueCollection = new ArrayList<String>();
		valueCollection.add("one");
		valueCollection.add("two");
		map.put("firstKey", valueCollection);
		// Two
		valueCollection = new ArrayList<String>();
		valueCollection.add("a");
		map.put("secondKey", valueCollection);
		Set<? extends JDOAnnotation<String>> result = JDOAnnotationsUtils.createFromMap(owner, map);
		assertNotNull(result);
		Set<JDOStringAnnotation> set;
		try{
			set = (Set<JDOStringAnnotation>) result;
			assertEquals(3, set.size());
		}catch (ClassCastException e){
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testCreateFromSet(){
		Set<JDOLongAnnotation> set  = new HashSet<JDOLongAnnotation>();
		set.add(new JDOLongAnnotation("keyOne", new Long(101)));
		set.add(new JDOLongAnnotation("keyOne", new Long(102)));
		set.add(new JDOLongAnnotation("keyTwo", new Long(42)));
		// Convert it to a map
		Map<String, Collection<Long>> map = JDOAnnotationsUtils.createFromSet(set);
		assertNotNull(map);
		// There should be two values int the map, and the first value should have a collection with 2 values.
		assertEquals(2, map.size());
		Collection<Long> one = map.get("keyOne");
		assertNotNull(one);
		assertEquals(2, one.size());
	}
	
	@Test
	public void testRoundTrip(){
		Annotations dto = new Annotations();
		dto.addAnnotation("stringOne", "one");
		dto.addAnnotation("StringTwo", "3");
		dto.addAnnotation("longOne", new Long(324));
		dto.addAnnotation("doubleOne", new Double(32.4));
		dto.addAnnotation("dateOne", new Date(System.currentTimeMillis()));
		// Now create the jdo
		JDOAnnotations jdo = JDOAnnotationsUtils.createFromDTO(dto);
		assertNotNull(jdo);
		Annotations dtoCopy = JDOAnnotationsUtils.createFromJDO(jdo);
		assertNotNull(dtoCopy);
		// The copy should match the original
		assertEquals(dto, dtoCopy);
	}

}
