package org.sagebionetworks.repo.web.controller.metadata;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AllTypesValidatorTest {

	@Autowired
	AllTypesValidator allTypesValidator;
	NodeDAO mockNodeDAO;
	
	@Before
	public void setUp() {
		mockNodeDAO = mock(NodeDAO.class);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNullEntity() throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException{
		EntityEvent mockEvent = mock(EntityEvent.class);
		allTypesValidator.validateEntity(null, mockEvent);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNullEvent() throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException{
		Study mockDataset =  mock(Study.class);
		allTypesValidator.validateEntity(mockDataset, null);
	}
	
	@Test
	public void testNullList() throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException{
		Project project = new Project();
		allTypesValidator.validateEntity(project, new EntityEvent(EventType.CREATE, null, null));
	}
	
	@Test
	public void testEmptyList() throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException{
		Project project = new Project();
		allTypesValidator.validateEntity(project, new EntityEvent(EventType.CREATE, new ArrayList<EntityHeader>(), null));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testProjectWithProjectParent() throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException{
		when(mockNodeDAO.isNodeRoot(eq("123"))).thenReturn(false);
		allTypesValidator.setNodeDAO(mockNodeDAO);
		
		String parentId = "123";
		String childId = "456";
		// This is our parent header
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(parentId);
		parentHeader.setName("name");
		parentHeader.setType(EntityType.project.getEntityType());
		List<EntityHeader> path = new ArrayList<EntityHeader>();
		path.add(parentHeader);
		
		Project project = new Project();
		project.setParentId(parentId);
		project.setId(childId);
		// This should be valid
		allTypesValidator.validateEntity(project, new EntityEvent(EventType.CREATE, path, null));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testProjectWithDatasetParent() throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException{
		when(mockNodeDAO.isNodeRoot(eq("123"))).thenReturn(false);
		allTypesValidator.setNodeDAO(mockNodeDAO);
		
		String parentId = "123";
		String childId = "456";
		// This is our parent header
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(parentId);
		parentHeader.setName("name");
		parentHeader.setType(EntityType.dataset.getEntityType());
		List<EntityHeader> path = new ArrayList<EntityHeader>();
		path.add(parentHeader);
		
		Project project = new Project();
		project.setParentId(parentId);
		project.setId(childId);
		// This should not be valid
		allTypesValidator.validateEntity(project, new EntityEvent(EventType.CREATE, path, null));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testFolderWithSelfParent() throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException{
		String parentId = "123";
		// This is our parent header
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(parentId);
		parentHeader.setName("name");
		parentHeader.setType(EntityType.folder.getEntityType());
		List<EntityHeader> path = new ArrayList<EntityHeader>();
		path.add(parentHeader);
		
		Project project = new Project();
		project.setParentId(parentId);
		project.setId(parentId);
		// This should not be valid
		allTypesValidator.validateEntity(project, new EntityEvent(EventType.CREATE, path, null));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testFolderWithSelfAncestor() throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException{
		String grandparentId = "123";
		String parentId = "456";
		
		// This is our grandparent header
		EntityHeader grandparentHeader = new EntityHeader();
		grandparentHeader.setId(grandparentId);
		grandparentHeader.setName("gp");
		grandparentHeader.setType(EntityType.folder.getEntityType());
		List<EntityHeader> path = new ArrayList<EntityHeader>();
		path.add(grandparentHeader);
		
		// This is our direct parent header
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(parentId);
		parentHeader.setName("p");
		parentHeader.setType(EntityType.folder.getEntityType());
		path.add(parentHeader);
				
		Project project = new Project();
		project.setParentId(parentId);
		project.setId(grandparentId);
		// This should not be valid
		allTypesValidator.validateEntity(project, new EntityEvent(EventType.CREATE, path, null));
	}
}
