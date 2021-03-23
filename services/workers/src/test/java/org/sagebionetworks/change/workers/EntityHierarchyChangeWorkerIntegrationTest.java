package org.sagebionetworks.change.workers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.EntityAclManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntityHierarchyChangeWorkerIntegrationTest {
	
	private static final int MAX_WAIT_MS = 60*1000*3;
	
	@Autowired
	StackConfiguration config;
		
	@Autowired
	EntityManager entityManager;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	EntityAclManager entityAclManager;
	
	TableIndexDAO indexDao;
	@Mock
	ProgressCallback mockProgressCallback;
	
	UserInfo adminUser;
	Long userId;
	Project project;
	Folder folder;
	Folder child;
	
	ViewObjectType viewObjectType;
	
	@Before
	public void before(){
		
		// this is still an integration test even though a mock progress is used.
		MockitoAnnotations.initMocks(this);
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId(); 
		adminUser = new UserInfo(true, userId);
		indexDao = tableConnectionFactory.getAllConnections().get(0);
		
		project = new Project();
		project.setName("parent");
		String id = entityManager.createEntity(adminUser, project, null);
		project = entityManager.getEntity(adminUser, id, Project.class);
		// create a folder
		folder = new Folder();
		folder.setName("folder");
		folder.setParentId(project.getId());
		id = entityManager.createEntity(adminUser, folder, null);
		folder = entityManager.getEntity(adminUser, id, Folder.class);
		// create a child
		child = new Folder();
		child.setName("child");
		child.setParentId(folder.getId());
		id = entityManager.createEntity(adminUser, child, null);
		child = entityManager.getEntity(adminUser, id, Folder.class);
		
		viewObjectType = ViewObjectType.ENTITY;
	}
	
	@After
	public void after(){
		if(project != null){
			entityManager.deleteEntity(adminUser, project.getId());
		}
	}
	
	@Test
	public void testRoundTrip() throws InterruptedException{
		// wait for the child to be replicated
		ObjectDataDTO replicatedChild = waitForEntityDto(child.getId());
		assertNotNull(replicatedChild);
		assertEquals(KeyFactory.stringToKey(project.getId()), replicatedChild.getBenefactorId());
		// Delete the replicated data
		indexDao.deleteObjectData(viewObjectType, Lists.newArrayList(KeyFactory.stringToKey(child.getId())));
		// Add an ACL to the folder to trigger a hierarchy change
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(folder.getId(), adminUser, new Date());
		entityAclManager.overrideInheritance(acl, adminUser);
		// the update should trigger the replication of the child
		replicatedChild = waitForEntityDto(child.getId());
		assertNotNull(replicatedChild);
		// the benefactor should now the folder.
		assertEquals(KeyFactory.stringToKey(folder.getId()), replicatedChild.getBenefactorId());
	}
	
	
	/**
	 * Helper to wait for replicated data to appear.
	 * @param entityId
	 * @return
	 * @throws InterruptedException
	 */
	public ObjectDataDTO waitForEntityDto(String entityId) throws InterruptedException{
		long startTimeMS = System.currentTimeMillis();
		while(true){
			ObjectDataDTO entityDto = indexDao.getObjectData(viewObjectType, KeyFactory.stringToKey(entityId));
			if(entityDto != null){
				return entityDto;
			}
			System.out.println("Waiting for entity data to be replicated for id: "+entityId);
			Thread.sleep(2000);
			assertTrue("Timed-out waiting for entity data to be replicated.",System.currentTimeMillis()-startTimeMS < MAX_WAIT_MS);
		}
	}

}
