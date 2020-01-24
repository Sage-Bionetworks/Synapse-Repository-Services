package org.sagebionetworks.repo.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class EntityTypeUtilsTest {

	@Test
	public void testGetNodeTypeForClass(){
		EntityType[] array = EntityType.values();
		for(EntityType type: array){
			assertNotNull(EntityTypeUtils.getClassForType(type));
			EntityType result = EntityTypeUtils.getEntityTypeForClass(EntityTypeUtils.getClassForType(type));
			assertEquals(type, result);
		}
	}
	
	
	
	@Test
	public void testProjectValidParent() {
		EntityType[] expectedValid = new EntityType[]{null};
		testValidParents(expectedValid, EntityType.project);
	}
	
	// Test for PLFM-3324
	@Test
	public void testIsValidParentTypeWithWithNullParent() {
		
		for (EntityType type : EntityType.values()) {
			boolean isValid = EntityTypeUtils.isValidParentType(type, null);
			boolean expected = false;
			// Only the project type can have a null parent
			if (EntityType.project == type) {
				expected = true;
			}
			assertEquals(expected, isValid);
		}
		
	}
	
	/**
	 * Helper to test the valid parents of an object type.
	 * @param expectedValid
	 * @param toTest
	 */
	private void testValidParents(EntityType[] expectedValid, EntityType toTest) {
		// Test expected
		for(EntityType expected: expectedValid){
			assertTrue(EntityTypeUtils.isValidParentType(toTest, expected));
		}
		// test invalid
		for(EntityType type: EntityType.values()){
			if(!arrayCcontains(expectedValid, type)){
				assertFalse(EntityTypeUtils.isValidParentType(toTest, type));
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
