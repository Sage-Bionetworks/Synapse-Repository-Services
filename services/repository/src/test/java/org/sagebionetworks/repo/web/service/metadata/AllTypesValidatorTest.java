package org.sagebionetworks.repo.web.service.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class AllTypesValidatorTest {

	@Mock
	private NodeDAO mockNodeDAO;

	@InjectMocks
	private AllTypesValidatorImpl allTypesValidator;
	
	@Mock
	private EntityEvent mockEvent;

	@Test
	public void testNullEntity() throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException {
		Entity entity = null;
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			allTypesValidator.validateEntity(entity, mockEvent);
		});
	}

	@Test
	public void testNullList() throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException {
		Project project = new Project();

		// This should work
		allTypesValidator.validateEntity(project, new EntityEvent(EventType.CREATE, null, null));
	}

	@Test
	public void testEmptyList() throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException {
		Project project = new Project();
		allTypesValidator.validateEntity(project, new EntityEvent(EventType.CREATE, new ArrayList<EntityHeader>(), null));
	}

	@Test
	public void testProjectWithProjectParent() throws Exception {
		String parentId = "123";
		String childId = "456";

		when(mockNodeDAO.isNodeRoot(parentId)).thenReturn(false);

		// This is our parent header
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(parentId);
		parentHeader.setName("name");
		parentHeader.setType(Project.class.getName());
		List<EntityHeader> path = new ArrayList<EntityHeader>();
		path.add(parentHeader);

		Project project = new Project();
		project.setParentId(parentId);
		project.setId(childId);

		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			allTypesValidator.validateEntity(project, new EntityEvent(EventType.CREATE, path, null));
		});
	}

	@Test
	public void testFolderWithSelfParent() throws Exception {
		String parentId = "123";

		when(mockNodeDAO.isNodeRoot(parentId)).thenReturn(false);

		// This is our parent header
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(parentId);
		parentHeader.setName("name");
		parentHeader.setType(Folder.class.getName());
		List<EntityHeader> path = new ArrayList<EntityHeader>();
		path.add(parentHeader);

		Project project = new Project();
		project.setParentId(parentId);
		project.setId(parentId);

		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			allTypesValidator.validateEntity(project, new EntityEvent(EventType.CREATE, path, null));
		});
	}

	@Test
	public void testFolderWithSelfAncestor() throws Exception {
		String grandparentId = "123";
		String parentId = "456";

		when(mockNodeDAO.isNodeRoot(parentId)).thenReturn(false);
		

		// This is our grandparent header
		EntityHeader grandparentHeader = new EntityHeader();
		grandparentHeader.setId(grandparentId);
		grandparentHeader.setName("gp");
		grandparentHeader.setType(Folder.class.getName());
		List<EntityHeader> path = new ArrayList<EntityHeader>();
		path.add(grandparentHeader);

		// This is our direct parent header
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(parentId);
		parentHeader.setName("p");
		parentHeader.setType(Folder.class.getName());
		path.add(parentHeader);

		Project project = new Project();
		project.setParentId(parentId);
		project.setId(grandparentId);

		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			allTypesValidator.validateEntity(project, new EntityEvent(EventType.CREATE, path, null));
		});
	}

	@Test
	public void testFolderNullParent() throws Exception {
		Folder folder = new Folder();
		folder.setId("123");
		folder.setParentId(null);

		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			allTypesValidator.validateEntity(folder, new EntityEvent(EventType.CREATE, null, null));
		});
	}

	@Test
	public void testFolderRootParent() throws Exception {
		String rootId = "456";
		
		when(mockNodeDAO.isNodeRoot(rootId)).thenReturn(true);
		
		List<EntityHeader> path = new ArrayList<EntityHeader>();
		// This is our direct parent header
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(rootId);
		parentHeader.setName("p");
		parentHeader.setType(Folder.class.getName());
		path.add(parentHeader);

		Folder folder = new Folder();
		folder.setId("123");
		folder.setParentId(rootId);
		
		assertThrows(IllegalArgumentException.class, ()-> {
			// This should not be valid
			allTypesValidator.validateEntity(folder, new EntityEvent(EventType.CREATE, path, null));
		});
	}
	
	@Test
	public void testValidateCreateEntityWithNullParent() throws Exception {
		for (EntityType type : EntityType.values()) {
			
			Class<? extends Entity> clazz = EntityTypeUtils.getClassForType(type);
			
			Entity entity = clazz.newInstance();
			EntityEvent event = new EntityEvent(EventType.CREATE, null, null);
			
			if (EntityType.project == type) {
				// Call under test, should be fine for project
				allTypesValidator.validateEntity(entity, event);
			} else {
				IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()-> {
					// Call under test, should throw for any other type
					allTypesValidator.validateEntity(entity, event);
				});
				String expectedMessage = "Entity type: " + clazz.getName() + " cannot have a parent of type: null";
				assertEquals(expectedMessage, ex.getMessage());
			}
		}
	}

}
