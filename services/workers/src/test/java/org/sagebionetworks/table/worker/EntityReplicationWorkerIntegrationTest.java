package org.sagebionetworks.table.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntityReplicationWorkerIntegrationTest {
	
	private static final int MAX_WAIT_MS = 30*1000;
	
	@Autowired
	StackConfiguration config;
	@Autowired
	EntityManager entityManager;
	@Autowired
	TableEntityManager tableEntityManager;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	UserManager userManager;
	
	UserInfo adminUserInfo;
	Project project;
	
	@Before
	public void before(){
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		project = new Project();
		project.setName(UUID.randomUUID().toString());
		String projectId = entityManager.createEntity(adminUserInfo, project, null);
		project = entityManager.getEntity(adminUserInfo, projectId, Project.class);
	}
	
	@After
	public void after(){
		if(project != null){
			entityManager.deleteEntity(adminUserInfo, project.getId());
		}
	}

	@Test
	public void testReplication() throws InterruptedException{
		// Wait for the project data to be replicated
		EntityDTO entityDto = waitForEntityDto(project.getId());
		assertEquals(KeyFactory.stringToKey(project.getId()), entityDto.getId());
		assertEquals(project.getEtag(), entityDto.getEtag());
		assertEquals(project.getName(), entityDto.getName());
	}
	
	public EntityDTO waitForEntityDto(String entityId) throws InterruptedException{
		TableIndexDAO indexDao = tableConnectionFactory.getAllConnections().get(0);
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
