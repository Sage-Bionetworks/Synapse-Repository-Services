package org.sagebionetworks.repo.model;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class ObjectTypeTest {

	@Test (expected=IllegalArgumentException.class)
	public void testgetFirstTypeInUrlUknonw(){
		// This should throw an exception
		ObjectType type = ObjectType.getFirstTypeInUrl("/some/uknown/url");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testgetLastTypeInUrlUknonw(){
		// This should throw an exception
		ObjectType type = ObjectType.getLastTypeInUrl("/some/uknown/url");
	}
	
	
	@Test
	public void testAllTypeForLastAndFirstUrl(){
		// Make sure we can find any child type given any combination of parent and child
		ObjectType[] array = ObjectType.values();
		for(ObjectType parent: array){
			for(ObjectType child: array){
				String prifix = "/repo/v1";
				String url = prifix+parent.getUrlPrefix()+"/12"+child.getUrlPrefix();
//				System.out.println(url);
				ObjectType resultChild = ObjectType.getLastTypeInUrl(url);
				assertEquals(child, resultChild);
				ObjectType resultParent = ObjectType.getFirstTypeInUrl(url);
				assertEquals(parent, resultParent);
			}
		}
	}
	
	@Test
	public void testGetNodeTypeForClass(){
		ObjectType[] array = ObjectType.values();
		for(ObjectType type: array){
			assertNotNull(type.getClassForType());
			ObjectType result = type.getNodeTypeForClass(type.getClassForType());
			assertEquals(type, result);
		}
	}
	
	@Test
	public void testGetTypeForId(){
		ObjectType[] array = ObjectType.values();
		for(ObjectType type: array){
			assertNotNull(type.getId());
			ObjectType result = type.getTypeForId(type.getId());
			assertEquals(type, result);
		}
	}
	
	@Test
	public void testDatasetValidParent(){
		ObjectType[] expectedValid = new ObjectType[]{ObjectType.project};
		testValidParents(expectedValid,  ObjectType.dataset);
	}
	
	@Test
	public void testProjectValidParent(){
		ObjectType[] expectedValid = new ObjectType[]{ObjectType.project, null};
		testValidParents(expectedValid, ObjectType.project);
	}
	
	@Test
	public void testLayerValidParent(){
		ObjectType[] expectedValid = new ObjectType[]{ObjectType.dataset};
		testValidParents(expectedValid, ObjectType.layer);
	}
	
	@Test
	public void testLocationValidParent(){
		ObjectType[] expectedValid = new ObjectType[]{ObjectType.dataset, ObjectType.layer};
		testValidParents(expectedValid, ObjectType.location);
	}
	
	@Test
	public void testPreviewValidParent(){
		ObjectType[] expectedValid = new ObjectType[]{ ObjectType.layer};
		testValidParents(expectedValid, ObjectType.preview);
	}
	
	@Test
	public void testEulaValidParent(){
		ObjectType[] expectedValid = new ObjectType[]{ ObjectType.project};
		testValidParents(expectedValid, ObjectType.eula);
	}
	
	/**
	 * Helper to test the valid parents of an object type.
	 * @param expectedValid
	 * @param toTest
	 */
	private void testValidParents(ObjectType[] expectedValid, ObjectType toTest) {
		// Test expected
		for(ObjectType expected: expectedValid){
			assertTrue(toTest.isValidParentType(expected));
		}
		// test invalid
		for(ObjectType type: ObjectType.values()){
			if(!arrayCcontains(expectedValid, type)){
				assertFalse(toTest.isValidParentType(type));
			}
		}
	}
	
	private static boolean arrayCcontains(ObjectType[] array, ObjectType contains){
		for(ObjectType ob: array){
			if(ob == contains) return true;
		}
		return false;
	}
}
