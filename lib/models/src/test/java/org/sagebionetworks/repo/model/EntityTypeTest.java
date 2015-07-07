package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

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
			assertNotNull(EntityTypeUtils.getClassForType(type));
		}
	}
	
		
	@Test
	public void testGetNodeTypeForClass(){
		EntityType[] array = EntityType.values();
		for(EntityType type: array){
			assertNotNull(EntityTypeUtils.getClassForType(type));
			EntityType result = EntityTypeUtils.getEntityTypeForClass(EntityTypeUtils.getClassForType(type));
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
	
	@Test
	public void testProject(){
		assertNotNull(EntityType.project);
		assertEquals(Project.class, EntityTypeUtils.getClassForType(EntityType.project));
	}
	
	@Test
	public void testFolder(){
		assertNotNull(EntityType.folder);
		assertEquals(Folder.class, EntityTypeUtils.getClassForType(EntityType.folder));
	}


	@Test
	public void testProjectAlais(){
		Set<String> aliases = EntityTypeUtils.getAllAliases(EntityType.project);
		assertTrue(aliases.contains("project"));
		assertTrue(aliases.contains("entity"));
	}
	
	@Test
	public void testFolderAlais(){
		Set<String> aliases = EntityTypeUtils.getAllAliases(EntityType.folder);
		assertTrue(aliases.contains("folder"));
		assertTrue(aliases.contains("entity"));
	}
}
