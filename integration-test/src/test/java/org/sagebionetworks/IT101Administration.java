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
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdministration;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.MigratableObjectCount;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * This test will push data from a backup into synapse.
 * 
 * @author jmhill
 * 
 */
public class IT101Administration {

	public static final long TEST_TIME_OUT = 1000 * 60 * 4; // Currently 4 mins

	private static SynapseAdministration synapse;
	private static AmazonS3Client s3Client;
	private static String bucket;
	
	private List<Entity> toDelete = null;

	@BeforeClass
	public static void beforeClass() throws Exception {
		// Use the synapse client to do some of the work for us.
		synapse = new SynapseAdministration();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.login(StackConfiguration.getIntegrationTestUserAdminName(),
				StackConfiguration.getIntegrationTestUserAdminPassword());

		System.out.println(StackConfiguration.getPortalEndpoint());
		String iamId = StackConfiguration.getIAMUserId();
		String iamKey = StackConfiguration.getIAMUserKey();
		if (iamId == null)
			throw new IllegalArgumentException("IAM id cannot be null");
		if (iamKey == null)
			throw new IllegalArgumentException("IAM key cannot be null");
		bucket = StackConfiguration.getSharedS3BackupBucket();
		if (bucket == null)
			throw new IllegalArgumentException("Bucket cannot be null null");
		AWSCredentials creds = new BasicAWSCredentials(iamId, iamKey);
		s3Client = new AmazonS3Client(creds);
	}
	
	@After
	public void after() throws Exception {
		if(synapse != null && toDelete != null){
			for(Entity e: toDelete){
				synapse.deleteEntity(e);
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
	public void testgetMigratableObjectCounts() throws Exception {
		PaginatedResults<MigratableObjectCount> rs = synapse.getMigratableObjectCounts();
		assertNotNull(rs);
		assertNotNull(rs.getResults());
		assertNotNull(rs.getTotalNumberOfResults());
		assertEquals(6, rs.getTotalNumberOfResults());
		
	}

}
