package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
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
	
	private EntityBundle entityBundle;
	
	@Before
	public void setUp() {
		entityBundle = new EntityBundle();
	}
	
	@Test
	public void testAddProject() {
		testAddEntity(new Project(), Project.class);
	}
	
	@Test
	public void testAddStudy() {
		testAddEntity(new Study(), Study.class);
	}
	
	@Test
	public void testAddFolder() {
		testAddEntity(new Folder(), Folder.class);
	}
	
	@SuppressWarnings("rawtypes")
	private void testAddEntity(Entity original, Class clazz){
		entityBundle.setEntity(original);
		Entity retrieved = entityBundle.getEntity();
		assertNotNull("Entity was set / should not be null.", retrieved);
		assertTrue("Entity type was '" + retrieved.getClass().getName() + "'; Expected '" 
				+ clazz.getName(), retrieved.getClass().getName().equals(clazz.getName()));
	}
	
	@Test
	public void testAddAnnotations() {
		Annotations annotations = new Annotations();
		entityBundle.setAnnotations(annotations);
		Annotations retrieved = entityBundle.getAnnotations();
		assertNotNull("Annotations were set / should not be null", retrieved);
		assertTrue("Set/Retrieved annotations do not match original", retrieved.equals(annotations));
	}
		
	@Test
	public void testJSONRoundTrip() throws Exception{
		entityBundle = createDummyEntityBundle();
		
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		joa = entityBundle.writeToJSONObject(joa);
		String json = joa.toJSONString();
		assertNotNull(json);
		
		EntityBundle clone = new EntityBundle();
		clone.initializeFromJSONObject(joa.createNew(json));
		assertEquals(entityBundle, clone);		
	}
	
	/**
	 * Create an EntityBundle filled with dummy data
	 */
	public static EntityBundle createDummyEntityBundle() {
		AutoGenFactory autoGenFactory = new AutoGenFactory();
		
		// Entities
		Project project = (Project) autoGenFactory.newInstance(Project.class.getName());
		project.setName("Dummy Project");		
		
		// Permissions
		UserEntityPermissions permissions = (UserEntityPermissions) 
				autoGenFactory.newInstance(UserEntityPermissions.class.getName());
		permissions.setOwnerPrincipalId(123L);
		permissions.setCanView(true);
		
		// Path
		EntityPath path = (EntityPath) 
				autoGenFactory.newInstance(EntityPath.class.getName());
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
		AccessControlList acl = (AccessControlList) 
				autoGenFactory.newInstance(AccessControlList.class.getName());
		acl.setCreatedBy("John Doe");
		acl.setId("syn456");
		
		// Child Count
		Boolean hasChildren = true;
		
		// Annotations
		Annotations annotations = new Annotations();
		annotations.addAnnotation("key1", "value1");
		annotations.addAnnotation("key1", "value2");
		annotations.addAnnotation("key2", "value3");

		// Referencing Entities
		List<EntityHeader> rb = new ArrayList<EntityHeader>(NUM_PAGINATED_RESULTS);
		for (int i = 0; i < NUM_PAGINATED_RESULTS; i++) {
			EntityHeader eh = (EntityHeader) autoGenFactory.newInstance(EntityHeader.class.getName());
			eh.setId("syn" + i);
			eh.setName("EntityHeader " + i);
			eh.setType("Folder");
			rb.add(eh);
		}
		PaginatedResults<EntityHeader> referencedBy = 
			new PaginatedResults<EntityHeader>(
				"dummy_uri",
				rb,
				101,
				4,
				14,
				"name",
				true);
		
		List<AccessRequirement> accessRequirements = new ArrayList<AccessRequirement>();
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setEntityIds(Arrays.asList(new String[]{"101", "102"}));
		ar.setEntityType(TermsOfUseAccessRequirement.class.getName());
		ar.setTermsOfUse("foo");
		accessRequirements.add(ar);

		EntityBundle entityBundle = new EntityBundle();
		entityBundle.setEntity(project);
		entityBundle.setPermissions(permissions);
		entityBundle.setPath(path);
		entityBundle.setReferencedBy(referencedBy);
		entityBundle.setHasChildren(hasChildren);
		entityBundle.setAccessControlList(acl);
		entityBundle.setAccessRequirements(accessRequirements);
		entityBundle.setUnmetAccessRequirements(accessRequirements);
		
		return entityBundle;
	}

}
