package org.sagebionetworks.repo.model.jdo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;
import java.util.Set;

import org.junit.Test;

/**
 * Test the basic methods of the annoations object
 * @author jmhill
 *
 */
public class JDOAnnotationsTest {
	
	@Test
	public void testAddRemoveString(){
		JDOAnnotations annotations = JDOAnnotations.newJDOAnnotations();
		// Add a string
		String keyOne = "key";
		String valueOne = "valueOne";
		annotations.add(keyOne, valueOne);
		annotations.add("key2", "value2");
		Set<JDOStringAnnotation> set = annotations.getStringAnnotations();
		assertNotNull(set);
		assertEquals(2, set.size());
		// Remove the first
		annotations.remove(keyOne, valueOne);
		set = annotations.getStringAnnotations();
		assertNotNull(set);
		assertEquals(1, set.size());
	}
	
	@Test
	public void testAddRemoveDates(){
		JDOAnnotations annotations = JDOAnnotations.newJDOAnnotations();
		// Add a string
		String keyOne = "key";
		Date valueOne = new Date(System.currentTimeMillis());
		annotations.add(keyOne, valueOne);
		annotations.add("key2", new Date(System.currentTimeMillis()+30));
		Set<JDODateAnnotation> set = annotations.getDateAnnotations();
		assertNotNull(set);
		assertEquals(2, set.size());
		// Remove the first
		annotations.remove(keyOne, new Date(valueOne.getTime()));
		set = annotations.getDateAnnotations();
		assertNotNull(set);
		assertEquals(1, set.size());
	}
	
	@Test
	public void testAddRemoveLong(){
		JDOAnnotations annotations = JDOAnnotations.newJDOAnnotations();
		// Add a string
		String keyOne = "key";
		Long valueOne = new Long(System.currentTimeMillis());
		annotations.add(keyOne, valueOne);
		annotations.add("key2", new Long(123));
		Set<JDOLongAnnotation> set = annotations.getLongAnnotations();
		assertNotNull(set);
		assertEquals(2, set.size());
		// Remove the first
		annotations.remove(keyOne, new Long(valueOne));
		set = annotations.getLongAnnotations();
		assertNotNull(set);
		assertEquals(1, set.size());
	}
	
	@Test
	public void testAddRemoveDouble(){
		JDOAnnotations annotations = JDOAnnotations.newJDOAnnotations();
		// Add a string
		String keyOne = "key";
		Double valueOne = new Double(System.currentTimeMillis());
		annotations.add(keyOne, valueOne);
		annotations.add("key2", 123.44);
		Set<JDODoubleAnnotation> set = annotations.getDoubleAnnotations();
		assertNotNull(set);
		assertEquals(2, set.size());
		// Remove the first
		annotations.remove(keyOne, new Double(valueOne));
		set = annotations.getDoubleAnnotations();
		assertNotNull(set);
		assertEquals(1, set.size());
	}
}
