package org.sagebionetworks.registry;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sagebionetworks.Entity;

public class AllEntityTest {
	
	@Test
	public void testCreateAllTypes() throws InstantiationException, IllegalAccessException{
		
		EntityType[] array = EntityType.values();
		for(EntityType type: array){
			assertNotNull(type.getClassForType());
			// Make sure we can create a new instance of this type
			Entity entity = type.getClassForType().newInstance();
			assertNotNull(entity);
		}
	}

}
