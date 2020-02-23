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
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class AllTypesValidatorTest {
	private static final long ENTITY_ID = 123;
	private static final String ENTITY_ID_STR = KeyFactory.keyToString(ENTITY_ID);
	private static final long PARENT_ENTITY_ID = 456;
	private static final String PARENT_ENTITY_ID_STR = KeyFactory.keyToString(PARENT_ENTITY_ID);

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
		Exception ex = assertThrows(IllegalArgumentException.class, () -> allTypesValidator.validateEntity(entity, event));
		assertEquals("Entity is required.", ex.getMessage());
	}

	@Test
	public void testNullEvent() {
		Entity entity = new Project();
		EntityEvent event = null;

		// Method under test.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> allTypesValidator.validateEntity(entity, event));
		assertEquals("Event is required.", ex.getMessage());
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
		Exception ex = assertThrows(IllegalArgumentException.class, () -> allTypesValidator.validateEntity(project, event));
		assertEquals("Name must be " + AllTypesValidatorImpl.MAX_NAME_CHARS + " characters or less", ex.getMessage());
	}

	@Test
	public void testDescriptionOverLimit() {
		Project project = new Project();
		project.setDescription(StringUtils.repeat("a", AllTypesValidatorImpl.MAX_DESCRIPTION_CHARS + 1));

		EntityEvent event = new EntityEvent();

		// Method under test - Will throw.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> allTypesValidator.validateEntity(project, event));
		assertEquals("Description must be " + AllTypesValidatorImpl.MAX_DESCRIPTION_CHARS + " characters or less", ex.getMessage());
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

		Exception ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			allTypesValidator.validateEntity(project, new EntityEvent(EventType.CREATE, path, null));
		});
		assertEquals("Entity type: org.sagebionetworks.repo.model.Project cannot have a parent of type: org.sagebionetworks.repo.model.Project",
				ex.getMessage());
	}

	@Test
	public void cannotAddTableToStsParent() {
		// Mock dependencies.
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID_STR,
				ProjectSettingsType.upload, UploadDestinationListSetting.class))
				.thenReturn(Optional.of(projectSetting));
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(true);

		// Set up parent.
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(PARENT_ENTITY_ID_STR);
		parentHeader.setType(Folder.class.getName());
		List<EntityHeader> path = ImmutableList.of(parentHeader);

		TableEntity tableEntity = new TableEntity();
		tableEntity.setParentId(PARENT_ENTITY_ID_STR);

		EntityEvent event = new EntityEvent(EventType.CREATE, path, mockUser);

		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> allTypesValidator.validateEntity(tableEntity, event));
		assertEquals("Can only create Files and Folders inside STS-enabled folders", ex.getMessage());
	}

	@Test
	public void createEntity_CanAddTableToNonStsParent() {
		// Mock dependencies.
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID_STR,
				ProjectSettingsType.upload, UploadDestinationListSetting.class))
				.thenReturn(Optional.of(projectSetting));
		when(mockProjectSettingsManager.isStsStorageLocationSetting(projectSetting)).thenReturn(false);

		// Set up parent.
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(PARENT_ENTITY_ID_STR);
		parentHeader.setType(Folder.class.getName());
		List<EntityHeader> path = ImmutableList.of(parentHeader);

		TableEntity tableEntity = new TableEntity();
		tableEntity.setParentId(PARENT_ENTITY_ID_STR);

		EntityEvent event = new EntityEvent(EventType.CREATE, path, mockUser);

		// Method under test - Does not throw.
		allTypesValidator.validateEntity(tableEntity, event);
	}

	@Test
	public void createEntity_CanAddTableToParentWithoutProjectSetting() {
		// Mock dependencies.
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID_STR,
				ProjectSettingsType.upload, UploadDestinationListSetting.class)).thenReturn(Optional.empty());

		// Set up parent.
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(PARENT_ENTITY_ID_STR);
		parentHeader.setType(Folder.class.getName());
		List<EntityHeader> path = ImmutableList.of(parentHeader);

		TableEntity tableEntity = new TableEntity();
		tableEntity.setParentId(PARENT_ENTITY_ID_STR);

		EntityEvent event = new EntityEvent(EventType.CREATE, path, mockUser);

		// Method under test - Does not throw.
		allTypesValidator.validateEntity(tableEntity, event);
	}

	@Test
	public void canUpdate() {
		// Mock dependencies.
		when(mockNodeDAO.doesNodeExist(ENTITY_ID)).thenReturn(true);

		// Set up parent.
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(PARENT_ENTITY_ID_STR);
		parentHeader.setType(Project.class.getName());
		List<EntityHeader> path = ImmutableList.of(parentHeader);

		Folder folder = new Folder();
		folder.setId(ENTITY_ID_STR);
		folder.setParentId(PARENT_ENTITY_ID_STR);

		EntityEvent event = new EntityEvent(EventType.UPDATE, path, mockUser);

		// Method under test - Does not throw.
		allTypesValidator.validateEntity(folder, event);
	}

	@Test
	public void update_OldNodeDoesntExist() {
		// Mock dependencies.
		when(mockNodeDAO.doesNodeExist(ENTITY_ID)).thenReturn(false);

		// Set up parent.
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(PARENT_ENTITY_ID_STR);
		parentHeader.setType(Project.class.getName());
		List<EntityHeader> path = ImmutableList.of(parentHeader);

		Folder folder = new Folder();
		folder.setId(ENTITY_ID_STR);
		folder.setParentId(PARENT_ENTITY_ID_STR);

		EntityEvent event = new EntityEvent(EventType.UPDATE, path, mockUser);

		// Method under test - Throws.
		Exception ex = assertThrows(NotFoundException.class, () -> allTypesValidator.validateEntity(folder, event));
		assertEquals("Entity " + ENTITY_ID_STR + " does not exist", ex.getMessage());
	}

	@Test
	public void testFolderWithSelfParent() {
		when(mockNodeDAO.isNodeRoot(ENTITY_ID_STR)).thenReturn(false);
		when(mockNodeDAO.doesNodeExist(ENTITY_ID)).thenReturn(true);

		// This is our parent header
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(ENTITY_ID_STR);
		parentHeader.setName("name");
		parentHeader.setType(Folder.class.getName());
		List<EntityHeader> path = new ArrayList<>();
		path.add(parentHeader);

		Folder folder = new Folder();
		folder.setParentId(ENTITY_ID_STR);
		folder.setId(ENTITY_ID_STR);

		Exception ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			allTypesValidator.validateEntity(folder, new EntityEvent(EventType.UPDATE, path, null));
		});
		assertEquals("Invalid hierarchy: an entity cannot be an ancestor of itself", ex.getMessage());
	}

	@Test
	public void testFolderWithSelfAncestor() {
		when(mockNodeDAO.isNodeRoot(PARENT_ENTITY_ID_STR)).thenReturn(false);
		when(mockNodeDAO.doesNodeExist(ENTITY_ID)).thenReturn(true);

		// This is our grandparent header
		EntityHeader grandparentHeader = new EntityHeader();
		grandparentHeader.setId(ENTITY_ID_STR);
		grandparentHeader.setName("gp");
		grandparentHeader.setType(Folder.class.getName());
		List<EntityHeader> path = new ArrayList<>();
		path.add(grandparentHeader);

		// This is our direct parent header
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(PARENT_ENTITY_ID_STR);
		parentHeader.setName("p");
		parentHeader.setType(Folder.class.getName());
		path.add(parentHeader);

		Folder folder = new Folder();
		folder.setParentId(PARENT_ENTITY_ID_STR);
		folder.setId(ENTITY_ID_STR);

		Exception ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			allTypesValidator.validateEntity(folder, new EntityEvent(EventType.UPDATE, path, null));
		});
		assertEquals("Invalid hierarchy: an entity cannot be an ancestor of itself", ex.getMessage());
	}

	// Test for PLFM-5873
	@Test
	public void testFolderNullParent() {
		Folder folder = new Folder();
		folder.setId("123");
		folder.setParentId(null);

		Exception ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			allTypesValidator.validateEntity(folder, new EntityEvent(EventType.CREATE, null, null));
		});
		assertEquals("Entity type: org.sagebionetworks.repo.model.Folder cannot have a parent of type: null", ex.getMessage());
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
		
		Exception ex = assertThrows(IllegalArgumentException.class, ()-> {
			// This should not be valid
			allTypesValidator.validateEntity(folder, new EntityEvent(EventType.CREATE, path, null));
		});
		assertEquals("Entity type: org.sagebionetworks.repo.model.Folder cannot have a parent of type: null",
				ex.getMessage());
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
