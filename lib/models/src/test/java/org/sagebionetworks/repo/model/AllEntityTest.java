package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class AllEntityTest {
	
	@Test
	public void testCreateAllTypes() throws InstantiationException, IllegalAccessException{
		
		EntityType[] array = EntityType.values();
		for(EntityType type: array){
			assertNotNull(EntityTypeUtils.getClassForType(type));
			// Make sure we can create a new instance of this type
			Entity entity = EntityTypeUtils.getClassForType(type).newInstance();
			assertNotNull(entity);
		}
	}

}
