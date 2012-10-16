package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.EntityType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntityManagerUtilsTest {
	
//	@Test
//	public void testGetAllKeys() throws Exception {
//		Annotations annots = new Annotations();
//		List<Date> ld = new ArrayList<Date>();
//		ld.add(new Date("01/01/2000"));
//		annots.getDateAnnotations().put("dateKey1", ld);
//		List<Double> ldbl = new ArrayList<Double>();
//		ldbl.add(new Double(3.1416));
//		annots.getDoubleAnnotations().put("doubleKey1", ldbl);
//		List<Long> ll = new ArrayList<Long>();
//		ll.add(new Long(1000));
//		annots.getLongAnnotations().put("longKey1", ll);
//		List<String> ls = new ArrayList<String>();
//		ls.add(new String("abcdef"));
//		annots.getStringAnnotations().put("stringKey1", ls);
//		Collection<String> keys = EntityManagerUtils.getAllKeys(annots);
//		assertTrue(keys.contains("dateKey1"));
//		assertTrue(keys.contains("doubleKey1"));
//		assertTrue(keys.contains("longKey1"));
//		assertTrue(keys.contains("stringKey1"));
//	}
//	
	@Test
	public void testGetListOfPrimaryFieldsToMove() throws Exception {
		Set<String> srcKeys = new HashSet<String>();
		srcKeys.add("key1");
		srcKeys.add("key2");
		srcKeys.add("name");
		
		Set<String> toMove = EntityManagerUtils.getSetOfPrimaryFieldsToMove(srcKeys, "org.sagebionetworks.repo.model.Study");
		assertTrue(toMove.contains("key1"));
		assertTrue(toMove.contains("key2"));
		assertTrue(! toMove.contains("name"));		
	}
	
	
	
	@Test
	public void testMoveFieldsFromPrimaryToAdditionals1() throws Exception {
		Annotations primaryAnnots = new Annotations();
		Annotations additionalAnnots = new Annotations();
		Set<String> keysToMove = new HashSet<String>();
		
		// Simple test, no merge
		for (int k = 0; k < 10; k++) {
			keysToMove.add("keyToMove" + k);			
		}
		for (int k = 0; k < 10; k+=2) {
			primaryAnnots.addAnnotation("keyToMove" + k, "stringValue" + k);
			primaryAnnots.addAnnotation("keyToMove" + (k + 1), new Long(k+1));
			primaryAnnots.addAnnotation("primaryKeyToStay" + k, "stringValue" + k);
			primaryAnnots.addAnnotation("primaryKeyToStay" + (k + 1), new Long(k + 1));
		}
		for (int k = 0; k < 5; k++) {
			additionalAnnots.addAnnotation("additionalKey" + k, "stringValue" + k);
		}
		EntityManagerUtils.moveFieldsFromPrimaryToAdditionals(primaryAnnots, additionalAnnots, keysToMove);
		for (int k = 0; k < 10; k+=2) {
			// Keys are deleted from src
			assertFalse(primaryAnnots.getStringAnnotations().containsKey("keyToMove" + k));
			assertFalse(primaryAnnots.getLongAnnotations().containsKey("keyToMove" + (k + 1)));
			// Keys are added to dest
			assertTrue(additionalAnnots.getStringAnnotations().containsKey("keyToMove" + k));
			assertTrue(additionalAnnots.getLongAnnotations().containsKey("keyToMove" + (k + 1)));
			// Other keys still in place
			assertTrue(primaryAnnots.getStringAnnotations().containsKey("primaryKeyToStay" + k));
			assertTrue(primaryAnnots.getLongAnnotations().containsKey("primaryKeyToStay" + (k + 1)));
		}
		for (int k = 0; k < 5; k++) {
			assertTrue(additionalAnnots.getStringAnnotations().containsKey("additionalKey" + k));
		}		
	}
	
	@Test
	public void testMoveFieldsFromPrimaryToAdditionals2() throws Exception {
		Annotations primaryAnnots = new Annotations();
		Annotations additionalAnnots = new Annotations();
		Set<String> keysToMove = new HashSet<String>();
		
		// Add merges into additionals, same type
		for (int k = 0; k < 10; k++) {
			keysToMove.add("keyToMove" + k);			
		}
		for (int k = 0; k < 10; k+=2) {
			primaryAnnots.addAnnotation("keyToMove" + k, "stringValue" + k);
			primaryAnnots.addAnnotation("keyToMove" + (k + 1), new Long(k+1));
			primaryAnnots.addAnnotation("primaryKeyToStay" + k, "stringValue" + k);
			primaryAnnots.addAnnotation("primaryKeyToStay" + (k + 1), new Long(k + 1));
		}
		// This time, keyToMove1..9 already exist in additionals
		for (int k = 0; k < 10; k+=2) {
			// 
			additionalAnnots.addAnnotation("additionalKey" + k, "stringValue" + k);
			additionalAnnots.addAnnotation("keyToMove" + (k + 1), new Long(2 * (k + 1)));
		}
		EntityManagerUtils.moveFieldsFromPrimaryToAdditionals(primaryAnnots, additionalAnnots, keysToMove);
		for (int k = 0; k < 10; k+=2) {
			// Keys are deleted from src
			assertFalse(primaryAnnots.getStringAnnotations().containsKey("keyToMove" + k));
			assertFalse(primaryAnnots.getLongAnnotations().containsKey("keyToMove" + (k + 1)));
			// Keys are added to dest
			assertTrue(additionalAnnots.getStringAnnotations().containsKey("keyToMove" + k));
			assertTrue(additionalAnnots.getLongAnnotations().containsKey("keyToMove" + (k + 1)));
			// Other keys still in place
			assertTrue(primaryAnnots.getStringAnnotations().containsKey("primaryKeyToStay" + k));
			assertTrue(primaryAnnots.getLongAnnotations().containsKey("primaryKeyToStay" + (k + 1)));
		}
		for (int k = 0; k < 10; k+=2) {
			// 1,3,5,7,9 should have 2 elements
			assertEquals(1, additionalAnnots.getStringAnnotations().get("keyToMove" + k).size());
			assertEquals(2, additionalAnnots.getLongAnnotations().get("keyToMove" + (k + 1)).size());
		}
	}
	
	@Test
	public void testMoveFieldsFromPrimaryToAdditionals3() throws Exception {
		Annotations primaryAnnots = new Annotations();
		Annotations additionalAnnots = new Annotations();
		Set<String> keysToMove = new HashSet<String>();
		
		// Add merges into additionals, mixed type
		for (int k = 0; k < 10; k++) {
			keysToMove.add("keyToMove" + k);			
		}
		// 0,2,4,6,8 are string and double
		for (int k = 0; k < 10; k+=2) {
			primaryAnnots.addAnnotation("keyToMove" + k, "stringValue" + k);
			primaryAnnots.addAnnotation("keyToMove" + (k + 1), new Long(k+1));
			primaryAnnots.addAnnotation("keyToMove" + k, new Double(k * 3.1416));
			primaryAnnots.addAnnotation("primaryKeyToStay" + k, "stringValue" + k);
			primaryAnnots.addAnnotation("primaryKeyToStay" + (k + 1), new Long(k + 1));
		}
		// This time, keyToMove1..9 already exist in additionals
		for (int k = 0; k < 10; k+=2) {
			// 
			additionalAnnots.addAnnotation("additionalKey" + k, "stringValue" + k);
			additionalAnnots.addAnnotation("keyToMove" + (k + 1), new Long(2 * (k + 1)));
		}
		EntityManagerUtils.moveFieldsFromPrimaryToAdditionals(primaryAnnots, additionalAnnots, keysToMove);
		for (int k = 0; k < 10; k+=2) {
			// Keys are deleted from src
			assertFalse(primaryAnnots.getStringAnnotations().containsKey("keyToMove" + k));
			assertFalse(primaryAnnots.getLongAnnotations().containsKey("keyToMove" + (k + 1)));
			assertFalse(primaryAnnots.getDoubleAnnotations().containsKey("keyToMove" + k));
			// Keys are added to dest
			assertTrue(additionalAnnots.getStringAnnotations().containsKey("keyToMove" + k));
			assertTrue(additionalAnnots.getLongAnnotations().containsKey("keyToMove" + (k + 1)));
			// Other keys still in place
			assertTrue(primaryAnnots.getStringAnnotations().containsKey("primaryKeyToStay" + k));
			assertTrue(primaryAnnots.getLongAnnotations().containsKey("primaryKeyToStay" + (k + 1)));
		}
		for (int k = 0; k < 10; k+=2) {
			// 1,3,5,7,9 should have 2 elements
			assertEquals(1, additionalAnnots.getStringAnnotations().get("keyToMove" + k).size());
			assertEquals(2, additionalAnnots.getLongAnnotations().get("keyToMove" + (k + 1)).size());
			assertEquals(1, additionalAnnots.getDoubleAnnotations().get("keyToMove" + k).size());
		}
	}
	
	// TODO: Change register.json and enable this test
	@Ignore
	@Test
	public void testIsValidTypeChange() throws Exception {
		// TODO: Expand to cover all combos?
		boolean v;
		String s;
		// Should not be able to go to project
		// Note: Might work but not for the right reason...
		s = "project";
		v = EntityManagerUtils.isValidTypeChange("folder", s);
		assertFalse(v);
		// Should be able to go from dataset/study to folder
		s = "folder";
		v = EntityManagerUtils.isValidTypeChange("dataset", s);
		assertFalse(v);
		// Should be able to go from phenotypedata to data
		s = "layer";
		v = EntityManagerUtils.isValidTypeChange("phenotypedata", s);
		assertTrue(v);
		// Should be able to go from data to genomicdata
		s = "genomicdata";
		v = EntityManagerUtils.isValidTypeChange("layer", s);
		assertTrue(v);
		// Should not be able to go from Locationable to non-Locationable
		s = "folder";
		v = EntityManagerUtils.isValidTypeChange("layer", s);
		assertFalse(v);
	}
	


}