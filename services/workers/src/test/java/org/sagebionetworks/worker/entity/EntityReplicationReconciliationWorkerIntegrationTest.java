package org.sagebionetworks.worker.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.entity.ReplicationMessageManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntityReplicationReconciliationWorkerIntegrationTest {
	
	private static final int MAX_WAIT_MS = 30*1000;
	
	@Autowired
	StackConfiguration config;
	@Autowired
	EntityManager entityManager;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	UserManager userManager;
	@Autowired
	ReplicationMessageManager replicationMessageManager;
	
	@Mock
	ProgressCallback mockProgressCallback;
	
	TableIndexDAO indexDao;
	
	UserInfo adminUserInfo;
	Project project;
	
	String projectId;
	Long projectIdLong;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		project = new Project();
		project.setName(UUID.randomUUID().toString());
		projectId = entityManager.createEntity(adminUserInfo, project, null);
		project = entityManager.getEntity(adminUserInfo, projectId, Project.class);
		projectIdLong = KeyFactory.stringToKey(projectId);
		indexDao = tableConnectionFactory.getAllConnections().get(0);
		// ensure a sycn can occur.
		indexDao.truncateReplicationSyncExpiration();
	}
	
	@After
	public void after(){
		if(project != null){
			entityManager.deleteEntity(adminUserInfo, project.getId());
		}
	}
	
	@Test
	public void testReconciliation() throws Exception{
		// wait for the project to replicate from the entity creation event
		EntityDTO dto = waitForEntityDto(projectId);
		assertNotNull(dto);
		assertEquals(projectIdLong, dto.getId());
		
		// Simulate out-of-synch by deleting the project's replication data
		indexDao.deleteEntityData(mockProgressCallback, Lists.newArrayList(projectIdLong));
		
		// trigger the reconciliation of the container.
		Long projectParent = KeyFactory.stringToKey(project.getParentId());
		List<Long> scope = Arrays.asList(projectParent);
		replicationMessageManager.pushContainerIdsToReconciliationQueue(scope);
		
		// wait for reconciliation to restore the deleted data.
		dto = waitForEntityDto(projectId);
		assertNotNull(dto);
		assertEquals(projectIdLong, dto.getId());
	}
	
	/**
	 * Helper to wait for an entity's replication data to appear.
	 * 
	 * @param entityId
	 * @return
	 * @throws InterruptedException
	 */
	public EntityDTO waitForEntityDto(String entityId) throws InterruptedException{
		long startTimeMS = System.currentTimeMillis();
		while(true){
			EntityDTO entityDto = indexDao.getEntityData(KeyFactory.stringToKey(entityId));
			if(entityDto != null){
				return entityDto;
			}
			System.out.println("Waiting for entity data to be replicated for id: "+entityId);
			Thread.sleep(2000);
			assertTrue("Timed-out waiting for entity data to be replicated.",System.currentTimeMillis()-startTimeMS < MAX_WAIT_MS);
		}
	}
}
