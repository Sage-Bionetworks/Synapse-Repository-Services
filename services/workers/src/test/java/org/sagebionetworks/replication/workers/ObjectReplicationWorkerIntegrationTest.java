package org.sagebionetworks.replication.workers;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ObjectReplicationWorkerIntegrationTest {
	
	private static final int MAX_WAIT_MS = 2* 60 *1000;
	
	@Autowired
	EntityManager entityManager;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	UserManager userManager;
	
	UserInfo adminUserInfo;
	Project project;
	
	@BeforeEach
	public void before(){
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		project = new Project();
		project.setName(UUID.randomUUID().toString());
		String projectId = entityManager.createEntity(adminUserInfo, project, null);
		project = entityManager.getEntity(adminUserInfo, projectId, Project.class);
	}
	
	@AfterEach
	public void after(){
		if(project != null){
			entityManager.deleteEntity(adminUserInfo, project.getId());
		}
	}

	@Test
	public void testReplication() throws InterruptedException{
		// Wait for the project data to be replicated
		ObjectDataDTO entityDto = waitForEntityDto(project.getId());
		assertEquals(KeyFactory.stringToKey(project.getId()), entityDto.getId());
		assertEquals(project.getEtag(), entityDto.getEtag());
		assertEquals(project.getName(), entityDto.getName());
	}
	
	public ObjectDataDTO waitForEntityDto(String entityId) throws InterruptedException{
		TableIndexDAO indexDao = tableConnectionFactory.getAllConnections().get(0);
		long startTimeMS = System.currentTimeMillis();
		while(true){
			ObjectDataDTO entityDto = indexDao.getObjectData(ViewObjectType.ENTITY, KeyFactory.stringToKey(entityId));
			if(entityDto != null){
				return entityDto;
			}
			System.out.println("Waiting for entity data to be replicated for id: "+entityId);
			Thread.sleep(2000);
			assertTrue(System.currentTimeMillis()-startTimeMS < MAX_WAIT_MS, "Timed-out waiting for entity data to be replicated.");
		}
	}
}
