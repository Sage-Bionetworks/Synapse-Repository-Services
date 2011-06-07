package org.sagebionetworks.repo.model;

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
}
