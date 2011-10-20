package org.sagebionetworks.registry;

import static org.junit.Assert.*;

import java.util.HashSet;

import org.junit.Test;

public class EntityTypeTest {
	
	@Test
	public void testValues(){
		// Make sure we can get all of the values
		EntityType[] values = EntityType.values();
		assertNotNull(values);
	}
	
	@Test
	public void testGetClass(){
		EntityType[] values = EntityType.values();
		assertNotNull(values);
		for(EntityType type: values){
			assertNotNull(type.getClassForType());
		}
	}
	
	@Test
	public void testGetId(){
		EntityType[] values = EntityType.values();
		assertNotNull(values);
		HashSet<Short> ids = new HashSet<Short>();
		for(EntityType type: values){
			assertTrue(ids.add(type.getId()));
		}
		assertEquals(values.length, ids.size());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testgetFirstTypeInUrlUnknown(){
		// This should throw an exception
		EntityType type = EntityType.getFirstTypeInUrl("/some/uknown/url");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testgetLastTypeInUrlUnknown(){
		// This should throw an exception
		EntityType type = EntityType.getLastTypeInUrl("/some/uknown/url");
	}
	
	@Test
	public void testAllTypeForLastAndFirstUrl(){
		// Make sure we can find any child type given any combination of parent and child
		EntityType[] array = EntityType.values();
		for(EntityType parent: array){
			for(EntityType child: array){
				String prifix = "/repo/v1";
				String url = prifix+parent.getUrlPrefix()+"/12"+child.getUrlPrefix();
//				System.out.println(url);
				EntityType resultChild = EntityType.getLastTypeInUrl(url);
				assertEquals(child, resultChild);
				EntityType resultParent = EntityType.getFirstTypeInUrl(url);
				assertEquals(parent, resultParent);
			}
		}
	}
	
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
	
	/**
	 * This test has a limited life span.  After we complete the conversion from EntityType to EntityType
	 * we can delete this test.
	 */
	@Test
	public void testValuesMatchObjecType(){
		// Make sure we can get all of the values
		EntityType[] values = EntityType.values();
		assertNotNull(values);
		assertEquals("Expected one EntityType for each EntityType",EntityType.values().length,values.length);
	}

}
