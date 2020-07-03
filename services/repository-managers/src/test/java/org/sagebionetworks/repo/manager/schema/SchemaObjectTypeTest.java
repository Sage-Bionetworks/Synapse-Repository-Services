package org.sagebionetworks.repo.manager.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;

public class SchemaObjectTypeTest {

	@Test
	public void testValueOfConcreteTypeWithFile() {
		// Call under test
		SchemaObjectType type = SchemaObjectType.valueOfConcreteType(FileEntity.class.getName());
		assertNotNull(type);
		assertEquals(FileEntity.class.getName(), type.getConcreteType());
	}
	
	@Test
	public void testValueOfConcreteTypeWithFolder() {
		// Call under test
		SchemaObjectType type = SchemaObjectType.valueOfConcreteType(Folder.class.getName());
		assertNotNull(type);
		assertEquals(Folder.class.getName(), type.getConcreteType());
	}
	
	@Test
	public void testValueOfConcreteTypeWithProject() {
		// Call under test
		SchemaObjectType type = SchemaObjectType.valueOfConcreteType(Project.class.getName());
		assertNotNull(type);
		assertEquals(Project.class.getName(), type.getConcreteType());
	}
	
	@Test
	public void testValueOfConcreteTypeWithUnknown() {
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			SchemaObjectType.valueOfConcreteType("something.unknown");
		}).getMessage();
		assertEquals("Unknown concreteType: something.unknown", message);
	}
}
