package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashSet;
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
			assertNotNull(type.getClassForType());
		}
	}
	
		
	@Test
	public void testGetNodeTypeForClass(){
		EntityType[] array = EntityType.values();
		for(EntityType type: array){
			assertNotNull(type.getClassForType());
			EntityType result = EntityType.getEntityTypeForClass(type.getClassForType());
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
		assertEquals(Project.class, EntityType.project.getClassForType());
	}
	
	@Test
	public void testFolder(){
		assertNotNull(EntityType.folder);
		assertEquals(Folder.class, EntityType.folder.getClassForType());
	}


	@Test
	public void testProjectAlais(){
		LinkedHashSet<String> expected = new LinkedHashSet<String>();
		Set<String> aliases = EntityType.project.getAllAliases();
		assertTrue(aliases.contains("project"));
		assertTrue(aliases.contains("entity"));
	}
	
	@Test
	public void testFolderAlais(){
		LinkedHashSet<String> expected = new LinkedHashSet<String>();
		Set<String> aliases = EntityType.folder.getAllAliases();
		assertTrue(aliases.contains("folder"));
		assertTrue(aliases.contains("entity"));
	}
}
