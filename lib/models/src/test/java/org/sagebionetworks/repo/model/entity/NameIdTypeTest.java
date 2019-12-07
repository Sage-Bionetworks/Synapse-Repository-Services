package org.sagebionetworks.repo.model.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;

import com.google.common.collect.Lists;

class NameIdTypeTest {

	@Test
	void testToEntityHeader() {
		List<NameIdType> input = Lists.newArrayList(
				new NameIdType().withName("a").withId("syn123").withType(Project.class.getName()),
				new NameIdType().withName("b").withId("syn456").withType(Folder.class.getName()));
		
		EntityHeader a = new EntityHeader();
		a.setName("a");
		a.setId("syn123");
		a.setType(Project.class.getName());
		EntityHeader b = new EntityHeader();
		b.setName("b");
		b.setId("syn456");
		b.setType(Folder.class.getName());
		List<EntityHeader> expected = Lists.newArrayList(a, b);
		// call under test
		List<EntityHeader> result = NameIdType.toEntityHeader(input);
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetEntityHeaderNull() {
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			NameIdType.toEntityHeader(null);
		});
	}

}
