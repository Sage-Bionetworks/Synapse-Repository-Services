package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * Test basic operations of EntityBundles.
 * @author bkng
 *
 */
public class EntityBundleTest {
	
	private static final int NUM_PAGINATED_RESULTS = 5;
	
	private EntityBundle eb;
	private Project project;
	private Study study;
	private Folder folder;
	private Annotations annotations;
	private UserEntityPermissions permissions;
	private EntityPath path;
	private PaginatedResults<EntityHeader> referencedBy;
	private Long childCount;
	private AccessControlList acl;
	private PaginatedResults<UserProfile> users;
	private PaginatedResults<UserGroup> groups;
	
	private AutoGenFactory agf = new AutoGenFactory();
	
	@Before
	public void setUp() {
		eb = new EntityBundle();
		
		// Entities
		project = (Project) agf.newInstance(Project.class.getName());
		project.setName("Dummy Project");		
		study = (Study) agf.newInstance(Study.class.getName());
		study.setName("Dummy Study");
		folder = (Folder) agf.newInstance(Folder.class.getName());
		folder.setName("Dummy Folder");
		
		// Permissions
		permissions = (UserEntityPermissions) agf.newInstance(UserEntityPermissions.class.getName());
		permissions.setOwnerPrincipalId(123L);
		permissions.setCanView(true);
		
		// Path
		path = (EntityPath) agf.newInstance(EntityPath.class.getName());
		List<EntityHeader> pathHeaders = new ArrayList<EntityHeader>();		
		EntityHeader rootHeader = new EntityHeader();
		rootHeader.setId("1");
		rootHeader.setName("root");
		pathHeaders.add(rootHeader);		
		EntityHeader projHeader = new EntityHeader();
		projHeader.setId("2");
		projHeader.setName("project");
		pathHeaders.add(projHeader);		
		EntityHeader dsHeader = new EntityHeader();
		dsHeader.setId("3");
		dsHeader.setName("ds");
		pathHeaders.add(dsHeader);		
		path.setPath(pathHeaders);
		
		// Access Control List
		acl = (AccessControlList) agf.newInstance(AccessControlList.class.getName());
		acl.setCreatedBy("John Doe");
		acl.setId("syn456");
		
		// Child Count
		childCount = 12L;
		
		// Annotations
		annotations = new Annotations();
		annotations.addAnnotation("key1", "value1");
		annotations.addAnnotation("key1", "value2");
		annotations.addAnnotation("key2", "value3");

		// Referencing Entities
		List<EntityHeader> rb = new ArrayList<EntityHeader>(NUM_PAGINATED_RESULTS);
		for (int i = 0; i < NUM_PAGINATED_RESULTS; i++) {
			EntityHeader eh = (EntityHeader) agf.newInstance(EntityHeader.class.getName());
			eh.setId("syn" + i);
			eh.setName("EntityHeader " + i);
			eh.setType("Folder");
			rb.add(eh);
		}
		referencedBy = new PaginatedResults<EntityHeader>(
				"dummy_uri",
				rb,
				101,
				4,
				14,
				"name",
				true);
		
		// Users
		List<UserProfile> us = new ArrayList<UserProfile>(NUM_PAGINATED_RESULTS);
		for (int i = 0; i < NUM_PAGINATED_RESULTS; i++) {
			UserProfile up = (UserProfile) agf.newInstance(UserProfile.class.getName());
			up.setFirstName("First" + i);
			up.setLastName("Last" + i);
			us.add(up);
		}
		users = new PaginatedResults<UserProfile>(
				"dummy_uri",
				us,
				101,
				4,
				14,
				"name",
				true);
		
		// Groups
		List<UserGroup> gr = new ArrayList<UserGroup>(NUM_PAGINATED_RESULTS);
		for (int i = 0; i < NUM_PAGINATED_RESULTS; i++) {
			UserGroup ug = (UserGroup) agf.newInstance(UserGroup.class.getName());
			ug.setId("group" + i);
			ug.setName("name" + i);
			gr.add(ug);
		}
		groups = new PaginatedResults<UserGroup>(
				"dummy_uri",
				gr,
				101,
				4,
				14,
				"name",
				true);
	}
	
	@Test
	public void testAddProject() {
		testAddEntity(project, Project.class);
	}
	
	@Test
	public void testAddStudy() {
		testAddEntity(study, Study.class);
	}
	
	@Test
	public void testAddFolder() {
		testAddEntity(folder, Folder.class);
	}
	
	@SuppressWarnings("rawtypes")
	private void testAddEntity(Entity original, Class clazz){
		eb.setEntity(original);
		Entity retrieved = eb.getEntity();
		assertNotNull("Entity was set / should not be null.", retrieved);
		assertTrue("Entity type was '" + retrieved.getClass().getName() + "'; Expected '" 
				+ clazz.getName(), retrieved.getClass().getName().equals(clazz.getName()));
	}
	
	@Test
	public void testAddAnnotations() {
		eb.setAnnotations(annotations);
		Annotations retrieved = eb.getAnnotations();
		assertNotNull("Annotations were set / should not be null", retrieved);
		assertTrue("Set/Retrieved annotations do not match original", retrieved.equals(annotations));
	}
		
	@Test
	public void testJSONRoundTrip() throws Exception{
		eb.setEntity(project);
		eb.setPermissions(permissions);
		eb.setPath(path);
		eb.setReferencedBy(referencedBy);
		eb.setChildCount(childCount);
		eb.setAccessControlList(acl);
		eb.setUsers(users);
		eb.setGroups(groups);
		
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		joa = eb.writeToJSONObject(joa);
		String json = joa.toJSONString();
		System.out.println(json);
		assertNotNull(json);
		
		EntityBundle clone = new EntityBundle();
		clone.initializeFromJSONObject(joa.createNew(json));
		System.out.println(clone.toString());
		assertEquals(eb, clone);		
	}

}
