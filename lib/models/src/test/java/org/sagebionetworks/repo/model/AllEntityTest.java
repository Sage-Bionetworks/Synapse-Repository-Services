package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;

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
