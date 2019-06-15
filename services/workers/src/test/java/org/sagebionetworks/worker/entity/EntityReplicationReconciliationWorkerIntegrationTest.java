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
import org.sagebionetworks.repo.model.Folder;
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
	 * With PLFM_5352, there are cases where the benefactor can be out-of-date in the entity replication table.
	 * The reconciliation process should detect and repair these cases.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testPLFM_5352() throws InterruptedException {
		// Add a folder to the project
		Folder folder = addHierarchyToProject();
		// wait for the folder to replicated
		EntityDTO dto = waitForEntityDto(folder.getId());
		assertNotNull(dto);
		// simulate a stale benefactor on the folder
		indexDao.deleteEntityData(null, Lists.newArrayList(KeyFactory.stringToKey(folder.getId())));
		dto.setBenefactorId(dto.getParentId());
		indexDao.addEntityData(null, Lists.newArrayList(dto));
		
		// trigger the reconciliation of the container.
		List<Long> scope = KeyFactory.stringToKey(Lists.newArrayList(project.getParentId(), folder.getParentId(), folder.getId()));
		replicationMessageManager.pushContainerIdsToReconciliationQueue(scope);
		
		// Wait for the benefactor to be fixed
		Long expectedBenefactor = projectIdLong;
		dto = waitForEntityDto(folder.getId(), expectedBenefactor);
		assertNotNull(dto);
	}
	
	/**
	 * Add a folder and a file to the project.
	 * hierarchy 
	 * @return
	 */
	public Folder addHierarchyToProject() {
		// Add a folder to the project
		Folder folder = new Folder();
		folder.setName(UUID.randomUUID().toString());
		folder.setParentId(projectId);
		String folderIdString = entityManager.createEntity(adminUserInfo, folder, null);
		folder = entityManager.getEntity(adminUserInfo, folderIdString, Folder.class);
		
		// add a child folder
		Folder child = new Folder();
		child.setName(UUID.randomUUID().toString());
		child.setParentId(folder.getId());
		String childId = entityManager.createEntity(adminUserInfo, child, null);
		return entityManager.getEntity(adminUserInfo, childId, Folder.class);
	}
	
	/**
	 * Helper to wait for an entity's replication data to appear.
	 * 
	 * @param entityId
	 * @return
	 * @throws InterruptedException
	 */
	public EntityDTO waitForEntityDto(String entityId) throws InterruptedException{
		Long expectedBenefactor = null;
		return waitForEntityDto(entityId, expectedBenefactor);
	}
	
	/**
	 * Helper to wait for an entity's replication data to appear.
	 * 
	 * @param entityId
	 * @param When not null, will wait for the given entity benefactor to match the provided value.
	 * @return
	 * @throws InterruptedException
	 */
	public EntityDTO waitForEntityDto(String entityId, Long expectedBenefactor) throws InterruptedException{
		long startTimeMS = System.currentTimeMillis();
		while(true){
			EntityDTO entityDto = indexDao.getEntityData(KeyFactory.stringToKey(entityId));
			if(entityDto != null){
				if(expectedBenefactor == null || expectedBenefactor.equals(entityDto.getBenefactorId())) {
					return entityDto;
				}
			}
			System.out.println("Waiting for entity data to be replicated for id: "+entityId);
			Thread.sleep(2000);
			assertTrue("Timed-out waiting for entity data to be replicated.",System.currentTimeMillis()-startTimeMS < MAX_WAIT_MS);
		}
	}
}
