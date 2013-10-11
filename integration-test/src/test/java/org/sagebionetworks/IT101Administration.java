package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.sagebionetworks.repo.model.migration.CrowdMigrationResult;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * This test will push data from a backup into Synapse
 * and make sure other methods in the admin client work 
 */
public class IT101Administration {

	private static SynapseAdminClientImpl synapse;
	
	private List<Entity> toDelete = null;

	@BeforeClass
	public static void beforeClass() throws Exception {
		// Use the synapse client to do some of the work for us.
		synapse = new SynapseAdminClientImpl();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.login(StackConfiguration.getIntegrationTestUserAdminName(),
				StackConfiguration.getIntegrationTestUserAdminPassword());
	}
	
	@After
	public void after() throws Exception {
		if(synapse != null && toDelete != null){
			for(Entity e: toDelete){
				synapse.deleteAndPurgeEntity(e);
			}
		}
		// always restore the status
		if(synapse != null){
			StackStatus status = new StackStatus();
			status.setStatus(StatusEnum.READ_WRITE);
			synapse.updateCurrentStackStatus(status);
		}
	}
	
	@Before
	public void before()throws Exception {
		synapse.login(StackConfiguration.getIntegrationTestUserAdminName(),
				StackConfiguration.getIntegrationTestUserAdminPassword());
		toDelete = new ArrayList<Entity>();
		// always restore the status
		if(synapse != null){
			StackStatus status = new StackStatus();
			status.setStatus(StatusEnum.READ_WRITE);
			synapse.updateCurrentStackStatus(status);
		}
	}

	
	/**
	 * This is a test for Bug PLFM-886
	 * @throws Exception
	 */
	@Test
	public void testReadOnlyMode() throws Exception {
		StackStatus status = synapse.getCurrentStackStatus();
		assertNotNull(status);
		// The status shoudl be in read-write before the tests
		assertEquals(StatusEnum.READ_WRITE, status.getStatus());
		
		// Now create a project
		Project project = new Project();
		project = synapse.createEntity(project);
		String projectId = project.getId();
		this.toDelete.add(project);
		
		// Now put the service in read only mode.
		status.setStatus(StatusEnum.READ_ONLY);
		status = synapse.updateCurrentStackStatus(status);
		assertEquals(StatusEnum.READ_ONLY, status.getStatus());
		
		// Now we should be able get the project
		project = synapse.getEntity(projectId, Project.class);
		assertNotNull(project);
		// Updates should not work
		String newDescription = "Updating the description";
		project.setDescription(newDescription);
		try{
			synapse.putEntity(project);
			fail("Updating an entity in read only mode should have failed");
		}catch(SynapseServiceException e){
			assertTrue(e.getMessage().indexOf("Synapse is in READ_ONLY mode for maintenance") > -1);
		}
		// put it back in read-write mode and try again
		status.setStatus(StatusEnum.READ_WRITE);
		status = synapse.updateCurrentStackStatus(status);
		assertEquals(StatusEnum.READ_WRITE, status.getStatus());
		project = synapse.putEntity(project);
		assertNotNull(project);
		assertEquals(newDescription, project.getDescription());
	}
	
	@Test
	public void testListChangeMessages() throws SynapseException, JSONObjectAdapterException{
		ChangeMessages results = synapse.listMessages(0l, ObjectType.ENTITY, 1l);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals(1, results.getList().size());
		System.out.println(results);
	}
	
	@Test
	public void testPublishMessages() throws SynapseException, JSONObjectAdapterException{
		StackConfiguration config = new StackConfiguration();
		PublishResults results = synapse.publishChangeMessages(config.getRdsUpdateQueueName(), 0L,  ObjectType.ENTITY, 1l);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals(1, results.getList().size());
		System.out.println(results);
	}

	/**
	 * Makes sure the Java admin client method works, but little else
	 */
	@Deprecated
	@Test
	public void testMigrateFromCrowd() throws Exception {
		PaginatedResults<CrowdMigrationResult> results = synapse.migrateFromCrowd(10, 0);
		assertTrue(results.getResults().size() > 0);
	}
}
