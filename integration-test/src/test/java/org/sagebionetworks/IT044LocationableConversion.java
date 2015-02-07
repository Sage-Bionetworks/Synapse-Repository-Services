package org.sagebionetworks;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.manager.EntityTypeConvertionError;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;

/**
 * Test for the service to convert locationables.
 * This test can be removed when we are done with conversion.
 * @author jhill
 *
 */
public class IT044LocationableConversion {
	
	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	
	private Project project;

	@BeforeClass
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
	}
	
	@Before
	public void before() throws SynapseException {
		// Create a project, this will own the file entity
		project = new Project();
		project = synapse.createEntity(project);
	}

	@After
	public void after() throws Exception {
		if(project != null){
			synapse.deleteEntity(project, true);
		}
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(userToDelete);
		} catch (SynapseException e) { }
	}
	
	@Test
	public void testConvertStudy() throws SynapseException{
		Study study = new Study();
		study.setParentId(project.getId());
		study.setDisease("cancer");
		LocationData ld = new LocationData();
		ld.setPath("http://www.google.com/somedoc");
		ld.setType(LocationTypeNames.external);
		study.setContentType("text/plain");
		study.setLocations(Arrays.asList(ld));
		study = adminSynapse.createEntity(study);
		// Convert it to a folder
		Folder folder = (Folder) adminSynapse.convertLocationableEntity(study);
		assertEquals(study.getId(), folder.getId());
		// It should have a child
		assertEquals(new Long(1), adminSynapse.getChildCount(folder.getId()));
	}
	
	@Test
	public void testConvertNonStudyLocationable() throws SynapseException{
		Data data = new Data();
		data.setParentId(project.getId());
		data.setDisease("cancer");
		LocationData ld = new LocationData();
		ld.setPath("http://www.google.com/somedoc");
		ld.setType(LocationTypeNames.external);
		data.setContentType("text/plain");
		data.setLocations(Arrays.asList(ld));
		data = adminSynapse.createEntity(data);
		// Convert it to a file
		FileEntity file = (FileEntity) adminSynapse.convertLocationableEntity(data);
		assertEquals(data.getId(), file.getId());
		// It should not have children
		assertEquals(new Long(0), adminSynapse.getChildCount(file.getId()));
	}
	
	@Test
	public void testConvertNoLocatoins() throws SynapseException{
		Data data = new Data();
		data.setParentId(project.getId());
		data.setDisease("cancer");
		data = adminSynapse.createEntity(data);
		
		try {
			adminSynapse.convertLocationableEntity(data);
			fail("should have failed");
		} catch (SynapseException e) {
			// The message should tell use what went wrong.
			assertEquals(EntityTypeConvertionError.LOCATIONABLE_HAS_NO_LOCATIONS.name(), e.getMessage());
		}

	}
}
