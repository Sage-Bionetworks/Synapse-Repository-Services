package org.sagebionetworks.repo.model.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ObjectType;

@ExtendWith(MockitoExtension.class)
public class ViewScopeUtilsTest {
	
	@Test
	public void testMapFromObjectType() {
		ObjectType objectType = ObjectType.ENTITY;
		ViewObjectType expected = ViewObjectType.ENTITY;
		
		// Call under test
		Optional<ViewObjectType> result = ViewScopeUtils.map(objectType);
		
		assertTrue(result.isPresent());
		assertEquals(expected, result.get());
	}
	
	@Test
	public void testMapFromObjectTypeWithoutMapping() {
		ObjectType objectType = ObjectType.USER_PROFILE;
		
		// Call under test
		Optional<ViewObjectType> result = ViewScopeUtils.map(objectType);
		
		assertFalse(result.isPresent());
	}
	
	@Test
	public void testMapFromViewObjectType() {
		
		// All the view object types should have a mapping back to ObjectType
		for (ViewObjectType viewObjectType : ViewObjectType.values()) {
			
			// Call under test
			ObjectType result = ViewScopeUtils.map(viewObjectType);
			
			assertEquals(ObjectType.valueOf(viewObjectType.name()), result);
		}
		
	}

}
