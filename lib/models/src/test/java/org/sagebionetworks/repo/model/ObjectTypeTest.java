package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ObjectTypeTest {

	@Test
	public void testGetNodeTypeForClass(){
		EntityType[] array = EntityType.values();
		for(EntityType type: array){
			assertNotNull(type.getClassForType());
			EntityType result = type.getNodeTypeForClass(type.getClassForType());
			assertEquals(type, result);
		}
	}
	
	@Test
	public void testGetTypeForId(){
		EntityType[] array = EntityType.values();
		for(EntityType type: array){
			assertNotNull(type.getId());
			EntityType result = type.getTypeForId(type.getId());
			assertEquals(type, result);
		}
	}
	
	@Test
	public void testDatasetValidParent(){
		EntityType[] expectedValid = new EntityType[]{EntityType.project, EntityType.folder};
		testValidParents(expectedValid,  EntityType.dataset);
	}
	
	@Test
	public void testProjectValidParent(){
		EntityType[] expectedValid = new EntityType[]{EntityType.project, null, EntityType.folder};
		testValidParents(expectedValid, EntityType.project);
	}
	
	@Test
	public void testLayerValidParent(){
		EntityType[] expectedValid = new EntityType[]{EntityType.dataset, EntityType.project, EntityType.folder};
		testValidParents(expectedValid, EntityType.layer);
	}
	
	@Test
	public void testPreviewValidParent(){
		EntityType[] expectedValid = new EntityType[]{ EntityType.layer, EntityType.folder};
		testValidParents(expectedValid, EntityType.preview);
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
