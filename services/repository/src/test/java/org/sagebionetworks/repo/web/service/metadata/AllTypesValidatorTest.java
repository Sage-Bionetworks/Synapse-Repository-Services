package org.sagebionetworks.repo.web.service.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
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
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class AllTypesValidatorTest {
	private static final String PARENT_ENTITY_ID = "parent-entity-id";

	@Mock
	private NodeDAO mockNodeDAO;

	@Mock
	private ProjectSettingsManager mockProjectSettingsManager;

	@Mock
	private UserInfo mockUser;

	@InjectMocks
	private AllTypesValidatorImpl allTypesValidator;

	private UploadDestinationListSetting projectSetting;

	@BeforeEach
	public void beforeEach() {
		projectSetting = new UploadDestinationListSetting();
	}

	@Test
	public void testNullEntity() {
		Entity entity = null;
		EntityEvent event = new EntityEvent();

		// Call under test
		assertThrows(IllegalArgumentException.class, () -> allTypesValidator.validateEntity(entity, event),
				"Entity is required.");
	}

	@Test
	public void testNullEvent() {
		Entity entity = new Project();
		EntityEvent event = null;

		// Method under test.
		assertThrows(IllegalArgumentException.class, () -> allTypesValidator.validateEntity(entity, event),
				"Event is required.");
	}

	@Test
	public void testEverythingNull() {
		// Set nulls for everything to test a default base case.
		Project project = new Project();
		project.setDescription(null);
		project.setName(null);
		project.setParentId(null);

		EntityEvent event = new EntityEvent();

		// Method under test - Does not throw.
		allTypesValidator.validateEntity(project, event);
	}

	@Test
	public void testNameAndDescriptionAtLimit() {
		Project project = new Project();
		project.setDescription(StringUtils.repeat("a", AllTypesValidatorImpl.MAX_DESCRIPTION_CHARS));
		project.setName(StringUtils.repeat("b", AllTypesValidatorImpl.MAX_NAME_CHARS));

		EntityEvent event = new EntityEvent();

		// Method under test - Does not throw.
		allTypesValidator.validateEntity(project, event);
	}

	@Test
	public void testNameOverLimit() {
		Project project = new Project();
		project.setName(StringUtils.repeat("b", AllTypesValidatorImpl.MAX_NAME_CHARS + 1));

		EntityEvent event = new EntityEvent();

		// Method under test - Will throw.
		assertThrows(IllegalArgumentException.class, () -> allTypesValidator.validateEntity(project, event),
				"Name must be " + AllTypesValidatorImpl.MAX_NAME_CHARS + " characters or less");
	}

	@Test
	public void testDescriptionOverLimit() {
		Project project = new Project();
		project.setDescription(StringUtils.repeat("a", AllTypesValidatorImpl.MAX_DESCRIPTION_CHARS + 1));

		EntityEvent event = new EntityEvent();

		// Method under test - Will throw.
		assertThrows(IllegalArgumentException.class, () -> allTypesValidator.validateEntity(project, event),
				"Description must be " + AllTypesValidatorImpl.MAX_NAME_CHARS + " characters or less");
	}

	@Test
	public void testEmptyList() throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException {
		Project project = new Project();
		allTypesValidator.validateEntity(project, new EntityEvent(EventType.CREATE, new ArrayList<>(), null));
	}

	@Test
	public void testProjectWithProjectParent() {
		String parentId = "123";
		String childId = "456";

		when(mockNodeDAO.isNodeRoot(parentId)).thenReturn(false);

		// This is our parent header
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(parentId);
		parentHeader.setName("name");
		parentHeader.setType(Project.class.getName());
		List<EntityHeader> path = new ArrayList<>();
		path.add(parentHeader);

		Project project = new Project();
		project.setParentId(parentId);
		project.setId(childId);

		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			allTypesValidator.validateEntity(project, new EntityEvent(EventType.CREATE, path, null));
		},
				"Entity type: org.sagebionetworks.repo.model.Project cannot have a parent of type: org.sagebionetworks.repo.model.Project");
	}

	@Test
	public void cannotAddTableToStsParent() {
		// Mock dependencies.
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class))
				.thenReturn(Optional.of(projectSetting));
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(true);

		// Set up parent.
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(PARENT_ENTITY_ID);
		parentHeader.setType(Folder.class.getName());
		List<EntityHeader> path = ImmutableList.of(parentHeader);

		TableEntity tableEntity = new TableEntity();
		tableEntity.setParentId(PARENT_ENTITY_ID);

		EntityEvent event = new EntityEvent(EventType.CREATE, path, mockUser);

		// Method under test - Throws.
		assertThrows(IllegalArgumentException.class, () -> allTypesValidator.validateEntity(tableEntity, event),
				"Can only create Files and Folders inside STS-enabled folders");
	}

	@Test
	public void createEntity_CanAddTableToNonStsParent() {
		// Mock dependencies.
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class))
				.thenReturn(Optional.of(projectSetting));
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(false);

		// Set up parent.
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(PARENT_ENTITY_ID);
		parentHeader.setType(Folder.class.getName());
		List<EntityHeader> path = ImmutableList.of(parentHeader);

		TableEntity tableEntity = new TableEntity();
		tableEntity.setParentId(PARENT_ENTITY_ID);

		EntityEvent event = new EntityEvent(EventType.CREATE, path, mockUser);

		// Method under test - Does not throw.
		allTypesValidator.validateEntity(tableEntity, event);
	}

	@Test
	public void createEntity_CanAddTableToParentWithoutProjectSetting() {
		// Mock dependencies.
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class)).thenReturn(Optional.empty());

		// Set up parent.
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(PARENT_ENTITY_ID);
		parentHeader.setType(Folder.class.getName());
		List<EntityHeader> path = ImmutableList.of(parentHeader);

		TableEntity tableEntity = new TableEntity();
		tableEntity.setParentId(PARENT_ENTITY_ID);

		EntityEvent event = new EntityEvent(EventType.CREATE, path, mockUser);

		// Method under test - Does not throw.
		allTypesValidator.validateEntity(tableEntity, event);
	}

	@Test
	public void testFolderWithSelfParent() {
		String parentId = "123";

		when(mockNodeDAO.isNodeRoot(parentId)).thenReturn(false);

		// This is our parent header
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(parentId);
		parentHeader.setName("name");
		parentHeader.setType(Folder.class.getName());
		List<EntityHeader> path = new ArrayList<>();
		path.add(parentHeader);

		Folder folder = new Folder();
		folder.setParentId(parentId);
		folder.setId(parentId);

		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			allTypesValidator.validateEntity(folder, new EntityEvent(EventType.UPDATE, path, null));
		},
				"Invalid hierarchy: an entity cannot be an ancestor of itself");
	}

	@Test
	public void testFolderWithSelfAncestor() {
		String grandparentId = "123";
		String parentId = "456";

		when(mockNodeDAO.isNodeRoot(parentId)).thenReturn(false);
		

		// This is our grandparent header
		EntityHeader grandparentHeader = new EntityHeader();
		grandparentHeader.setId(grandparentId);
		grandparentHeader.setName("gp");
		grandparentHeader.setType(Folder.class.getName());
		List<EntityHeader> path = new ArrayList<>();
		path.add(grandparentHeader);

		// This is our direct parent header
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(parentId);
		parentHeader.setName("p");
		parentHeader.setType(Folder.class.getName());
		path.add(parentHeader);

		Folder folder = new Folder();
		folder.setParentId(parentId);
		folder.setId(parentId);

		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			allTypesValidator.validateEntity(folder, new EntityEvent(EventType.UPDATE, path, null));
		},
				"Invalid hierarchy: an entity cannot be an ancestor of itself");
	}

	// Test for PLFM-5873
	@Test
	public void testFolderNullParent() {
		Folder folder = new Folder();
		folder.setId("123");
		folder.setParentId(null);

		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			allTypesValidator.validateEntity(folder, new EntityEvent(EventType.CREATE, null, null));
		},
				"Entity type: org.sagebionetworks.repo.model.Folder cannot have a parent of type: null");
	}

	@Test
	public void testFolderRootParent() {
		String rootId = "456";
		
		when(mockNodeDAO.isNodeRoot(rootId)).thenReturn(true);
		
		List<EntityHeader> path = new ArrayList<>();
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
		},
				"Entity type: org.sagebionetworks.repo.model.Folder cannot have a parent of type: null");
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
