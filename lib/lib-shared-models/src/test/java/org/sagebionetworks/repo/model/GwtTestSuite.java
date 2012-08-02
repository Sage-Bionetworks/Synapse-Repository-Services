package org.sagebionetworks.repo.model;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.sagebionetworks.gwt.client.schema.adapter.GwtAdapterFactory;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.registry.EntityRegistry;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;


/**
 * Since the GWT test are so slow to start and we could not get the GWTTestSuite to work,
 * we put all GWT tests in one class.
 * @author jmhill
 *
 */
public class GwtTestSuite extends GWTTestCase {
	
	private static final int NUM_PAGINATED_RESULTS = 5;

	/**
	 * Must refer to a valid module that sources this class.
	 */
	public String getModuleName() { 
		return "org.sagebionetworks.repo.SharedSynpaseDTOs";
	}
	
	String registerJson = null;
	
	@Override
	public void gwtSetUp() {

	}
	
	@Override
	public String toString() {
		return "GwtTestSuite for Module: "+getModuleName();
	}
	
	@Test
	public void testAnnotationsRoundTrip() throws JSONObjectAdapterException {
		Annotations annos = new Annotations();
		annos.addAnnotation("string", "one");
		annos.addAnnotation("string", "two");
		annos.addAnnotation("long", new Long(123));
		annos.addAnnotation("double", new Double(123.456));
		annos.addAnnotation("date", new Date());
		
		// Write it to GWT
		GwtAdapterFactory factory = new GwtAdapterFactory();
		JSONObjectAdapter adapter = factory.createNew();
		annos.writeToJSONObject(adapter);
		String json = adapter.toJSONString();
		adapter = factory.createNew(json);
		
		// Clone it
		Annotations clone = new Annotations();
		clone.initializeFromJSONObject(adapter);
		assertEquals(annos, clone);		
	}
	
	@Test
	public void testEntityBundleRoundTrip() throws JSONObjectAdapterException {
		EntityBundle entityBundle = createDummyEntityBundle();
		
		// Write it to GWT
		GwtAdapterFactory factory = new GwtAdapterFactory();
		JSONObjectAdapter adapter = factory.createNew();
		entityBundle.writeToJSONObject(adapter);
		String json = adapter.toJSONString();
		adapter = factory.createNew(json);
		
		// Clone it
		EntityBundle clone = new EntityBundle();
		clone.initializeFromJSONObject(adapter);
		assertEquals(entityBundle, clone);		
	}
	
	/**
	 * Make sure we can load the register
	 * @throws JSONObjectAdapterException 
	 * @throws UnsupportedEncodingException 
	 */
	@Test
	public void testRegister() throws JSONObjectAdapterException, UnsupportedEncodingException {
		RegisterConstants constants = GWT.create(RegisterConstants.class);
		// Load the Regiseter json
		GwtAdapterFactory factory = new GwtAdapterFactory();
		String base64String = constants.getRegisterJson();
		String decoded =  new String(Base64.decodeBase64(base64String.getBytes("UTF-8")), "UTF-8");
		assertNotNull(decoded);
		// Decode it
		JSONObjectAdapter adapter = factory.createNew(decoded);
		assertTrue(adapter.has("entityTypes"));
		EntityRegistry registry = new EntityRegistry();
		registry.initializeFromJSONObject(adapter);
	}	
	
	@Test
	public void serviceConstantsTest() throws JSONObjectAdapterException, UnsupportedEncodingException{
		assertNotNull(ServiceConstants.DEFAULT_PAGINATION_OFFSET);
	}
	
	/**
	 * Create an EntityBundle filled with dummy data
	 */
	private EntityBundle createDummyEntityBundle() {
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
		Long childCount = 12L;
		
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
		
		// Users
		List<UserProfile> us = new ArrayList<UserProfile>(NUM_PAGINATED_RESULTS);
		for (int i = 0; i < NUM_PAGINATED_RESULTS; i++) {
			UserProfile up = (UserProfile) autoGenFactory.newInstance(UserProfile.class.getName());
			up.setFirstName("First" + i);
			up.setLastName("Last" + i);
			us.add(up);
		}
		PaginatedResults<UserProfile> users = 
			new PaginatedResults<UserProfile>(
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
			UserGroup ug = (UserGroup) autoGenFactory.newInstance(UserGroup.class.getName());
			ug.setId("group" + i);
			ug.setName("name" + i);
			gr.add(ug);
		}
		PaginatedResults<UserGroup> groups = new PaginatedResults<UserGroup>(
				"dummy_uri",
				gr,
				101,
				4,
				14,
				"name",
				true);

		EntityBundle entityBundle = new EntityBundle();
		entityBundle.setEntity(project);
		entityBundle.setPermissions(permissions);
		entityBundle.setPath(path);
		entityBundle.setReferencedBy(referencedBy);
		entityBundle.setChildCount(childCount);
		entityBundle.setAccessControlList(acl);
		entityBundle.setUsers(users);
		entityBundle.setGroups(groups);
		
		return entityBundle;
	}
	
}
