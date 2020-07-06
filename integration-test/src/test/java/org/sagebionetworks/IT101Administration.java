package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseServerException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.migration.IdGeneratorExport;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.model.versionInfo.SynapseVersionInfo;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * This test will push data from a backup into Synapse
 * and make sure other methods in the admin client work 
 */
public class IT101Administration {

	private static StackConfiguration stackConfig;
	private static SynapseAdminClient adminSynapse;
	
	private List<Entity> toDelete = null;

	@BeforeAll
	public static void beforeClass() throws Exception {
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		stackConfig = StackConfigurationSingleton.singleton();
		
		adminSynapse.setUsername(stackConfig.getMigrationAdminUsername());
		adminSynapse.setApiKey(stackConfig.getMigrationAdminAPIKey());
	}
	
	@BeforeEach
	public void before()throws Exception {
		adminSynapse.removeAuthorizationHeader();
		adminSynapse.clearAllLocks();
		toDelete = new ArrayList<Entity>();
		// always restore the status
		if(adminSynapse != null){
			StackStatus status = new StackStatus();
			status.setStatus(StatusEnum.READ_WRITE);
			adminSynapse.updateCurrentStackStatus(status);
		}
	}
	
	@AfterEach
	public void after() throws Exception {
		if(adminSynapse != null && toDelete != null){
			for(Entity e: toDelete){
				adminSynapse.deleteEntity(e, true);
			}
		}
		// always restore the status
		if(adminSynapse != null){
			StackStatus status = new StackStatus();
			status.setStatus(StatusEnum.READ_WRITE);
			adminSynapse.updateCurrentStackStatus(status);
		}
	}

	
	/**
	 * This is a test for Bug PLFM-886
	 * @throws Exception
	 */
	@Test
	public void testReadOnlyMode() throws Exception {
		StackStatus status = adminSynapse.getCurrentStackStatus();
		assertNotNull(status);
		// The status shoudl be in read-write before the tests
		assertEquals(StatusEnum.READ_WRITE, status.getStatus());
		
		// Now create a project
		Project project = new Project();
		project = adminSynapse.createEntity(project);
		String projectId = project.getId();
		this.toDelete.add(project);
		
		// Now put the service in read only mode.
		status.setStatus(StatusEnum.READ_ONLY);
		status = adminSynapse.updateCurrentStackStatus(status);
		assertEquals(StatusEnum.READ_ONLY, status.getStatus());

		// Now we should be able get the version
		SynapseVersionInfo version = adminSynapse.getVersionInfo();
		assertNotNull(version);
		
		// Now we should not be able get the project
		try {
			adminSynapse.getEntity(projectId, Project.class);
		}catch(SynapseServerException e){
			assertTrue(e.getMessage().indexOf("Synapse is down for maintenance.") > -1);
		}
		// Updates should not work
		String newDescription = "Updating the description";
		project.setDescription(newDescription);
		try{
			adminSynapse.putEntity(project);
			fail("Updating an entity in read only mode should have failed");
		}catch(SynapseServerException e){
			assertTrue(e.getMessage().indexOf("Synapse is down for maintenance.") > -1);
		} finally {
			// put it back in read-write mode and try again
			status.setStatus(StatusEnum.READ_WRITE);
			status = adminSynapse.updateCurrentStackStatus(status);
			assertEquals(StatusEnum.READ_WRITE, status.getStatus());
			project = adminSynapse.putEntity(project);
			assertNotNull(project);
			assertEquals(newDescription, project.getDescription());
		}
	}
	
	@Test
	public void testListChangeMessages() throws SynapseException, JSONObjectAdapterException{
		ChangeMessages results = adminSynapse.listMessages(0l, ObjectType.ENTITY, 1l);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals(1, results.getList().size());
		System.out.println(results);
	}
	
	@Test
	public void testCreateOrUpdateChangeMessages() throws SynapseException, JSONObjectAdapterException {
		ChangeMessages expected = adminSynapse.listMessages(0l, ObjectType.ENTITY, 1l);
		ChangeMessages actual = adminSynapse.createOrUpdateChangeMessages(expected);
		assertEquals(expected.getList().size(), actual.getList().size());
		assertEquals(expected.getList().get(0).getObjectId(), actual.getList().get(0).getObjectId());
		assertEquals(expected.getList().get(0).getObjectType(), actual.getList().get(0).getObjectType());
	}
	
	@Test
	public void testClearAllLocks() throws SynapseException{
		adminSynapse.clearAllLocks();
	}
	
	@Test
	public void testCreateIdGeneratorExport() throws SynapseException {
		// call under test
		IdGeneratorExport export = adminSynapse.createIdGeneratorExport();
		assertNotNull(export);
		assertNotNull(export.getExportScript());
	}
	
	@Test
	public void testSendSQSMessageWithoutServiceCredentials() throws SynapseException {
		String queueName = "someQueue";
		String messageBody = "some message";
		
		// This service call cannot go through with standard API key auth
		String errorMessage = assertThrows(SynapseUnauthorizedException.class, () -> {			
			adminSynapse.sendSQSMessage(queueName, messageBody);
		}).getMessage();
		
		assertEquals("Missing required credentials in the authorization header.", errorMessage);
	}
	
	@Test
	public void testSendSQSMessageWithNonExistingQueue() throws SynapseException {

		String queueName = "nonExistingQueue";
		String messageBody = "some message";
		
		String adminKey = stackConfig.getServiceAuthKey(StackConfiguration.SERVICE_ADMIN);
		String adminSecret = stackConfig.getServiceAuthSecret(StackConfiguration.SERVICE_ADMIN);
		
		adminSynapse.setBasicAuthorizationCredentials(adminKey, adminSecret);
		
		String errorMessage = assertThrows(SynapseNotFoundException.class, () -> {			
			adminSynapse.sendSQSMessage(queueName, messageBody);
		}).getMessage();
		
		assertEquals("The queue referenced by " +queueName + " does not exist", errorMessage);
	}
	
	@Test
	public void testSendSQSMessage() throws Exception {
		
		// Emulate a change message (non existing entity) for replication
		ChangeMessage message = new ChangeMessage();
		
		message.setObjectType(ObjectType.ENTITY);
		message.setObjectId("-1");
		message.setChangeType(ChangeType.DELETE);	

		String queueName = "TABLE_ENTITY_REPLICATION";
		String messageBody = message.writeToJSONObject(new JSONObjectAdapterImpl()).toJSONString();
		
		String adminKey = stackConfig.getServiceAuthKey(StackConfiguration.SERVICE_ADMIN);
		String adminSecret = stackConfig.getServiceAuthSecret(StackConfiguration.SERVICE_ADMIN);
		
		adminSynapse.setBasicAuthorizationCredentials(adminKey, adminSecret);
		
		adminSynapse.sendSQSMessage(queueName, messageBody);
	}
	
 }
