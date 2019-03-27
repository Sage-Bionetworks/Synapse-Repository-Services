package org.sagebionetworks.repo.web.service.metadata;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.AbstractAutowiredControllerTestBase;
import org.sagebionetworks.util.ReflectionStaticTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class AllTypesValidatorTest extends AbstractAutowiredControllerTestBase {

	@Autowired
	AllTypesValidator allTypesValidator;

	NodeDAO mockNodeDAO;

	private Object oldNodeDao;
	
	@Before
	public void setUp() throws Exception {
		oldNodeDao = ReflectionStaticTestUtils.getField(allTypesValidator, "nodeDAO");
		mockNodeDAO = mock(NodeDAO.class);
	}

	@After
	public void tearDown() throws Exception {
		ReflectionStaticTestUtils.setField(allTypesValidator, "nodeDAO", oldNodeDao);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testNullEntity() throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException{
		EntityEvent mockEvent = mock(EntityEvent.class);
		allTypesValidator.validateEntity(null, mockEvent);
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
	public void testProjectWithProjectParent() throws Exception {
		when(mockNodeDAO.isNodeRoot(eq("123"))).thenReturn(false);
		ReflectionStaticTestUtils.setField(allTypesValidator, "nodeDAO", mockNodeDAO);
		
		String parentId = "123";
		String childId = "456";
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
		// This should be valid
		allTypesValidator.validateEntity(project, new EntityEvent(EventType.CREATE, path, null));
	}

	
	@Test (expected=IllegalArgumentException.class)
	public void testFolderWithSelfParent() throws Exception {
		when(mockNodeDAO.isNodeRoot(eq("123"))).thenReturn(false);
		ReflectionStaticTestUtils.setField(allTypesValidator, "nodeDAO", mockNodeDAO);
		String parentId = "123";
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
		// This should not be valid
		allTypesValidator.validateEntity(project, new EntityEvent(EventType.CREATE, path, null));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testFolderWithSelfAncestor() throws Exception {
		when(mockNodeDAO.isNodeRoot(eq("123"))).thenReturn(false);
		ReflectionStaticTestUtils.setField(allTypesValidator, "nodeDAO", mockNodeDAO);
		String grandparentId = "123";
		String parentId = "456";
		
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
		// This should not be valid
		allTypesValidator.validateEntity(project, new EntityEvent(EventType.CREATE, path, null));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testFolderNullParent() throws Exception {
		Folder folder = new Folder();
		folder.setId("123");
		folder.setParentId(null);
		// This should not be valid
		allTypesValidator.validateEntity(folder, new EntityEvent(EventType.CREATE, null, null));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testFolderRootParent() throws Exception {
		String rootId="456";
		when(mockNodeDAO.isNodeRoot(eq(rootId))).thenReturn(true);
		ReflectionStaticTestUtils.setField(allTypesValidator, "nodeDAO", mockNodeDAO);
				
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
		// This should not be valid
		allTypesValidator.validateEntity(folder, new EntityEvent(EventType.CREATE, path, null));
	}

}
