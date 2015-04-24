package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

public class ObjectTypeTest {

	@Test
	public void testGetNodeTypeForClass(){
		EntityType[] array = EntityType.values();
		for(EntityType type: array){
			assertNotNull(type.getClassForType());
			EntityType result = type.getEntityTypeForClass(type.getClassForType());
			assertEquals(type, result);
		}
	}
	
	
	
	@Test
	public void testProjectValidParent(){
		EntityType[] expectedValid = new EntityType[]{null};
		testValidParents(expectedValid, EntityType.project);
	}
	
	/**
	 * Helper to test the valid parents of an object type.
	 * @param expectedValid
	 * @param toTest
	 */
	private void testValidParents(EntityType[] expectedValid, EntityType toTest) {
		// Test expected
		for(EntityType expected: expectedValid){
			assertTrue(toTest.isValidParentType(expected));
		}
		// test invalid
		for(EntityType type: EntityType.values()){
			if(!arrayCcontains(expectedValid, type)){
				assertFalse(toTest.isValidParentType(type));
			}
		}
	}
	
	private static boolean arrayCcontains(EntityType[] array, EntityType contains){
		for(EntityType ob: array){
			if(ob == contains) return true;
		}
		return false;
	}
}
