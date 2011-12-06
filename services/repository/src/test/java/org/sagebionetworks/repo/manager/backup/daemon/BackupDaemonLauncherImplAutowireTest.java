package org.sagebionetworks.repo.manager.backup.daemon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.BackupRestoreStatus;
import org.sagebionetworks.repo.model.BackupRestoreStatus.STATUS;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.util.RandomAnnotationsUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class BackupDaemonLauncherImplAutowireTest {
	
	/**
	 * The maximum time to wait for the test to finish
	 */
	private static final long TIMEOUT = 1000*60*1; // 1 minutes

	@Autowired
	private BackupDaemonLauncher backupDaemonLauncher;
	
	@Autowired
	private UserProvider testUserProvider;
	
	@Autowired
	public NodeManager nodeManager;
	
	List<String> nodesToDelete = null;
	
	@Before
	public void before(){
		nodesToDelete = new ArrayList<String>();
	}
	
	@After
	public void after() throws NotFoundException, DatastoreException, UnauthorizedException{
		if(nodesToDelete != null){
			for(String id: nodesToDelete){
				nodeManager.delete(testUserProvider.getTestAdminUserInfo(), id);
			}
		}
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testNonAdminUserStartBackup() throws UnauthorizedException, DatastoreException{
		UserInfo nonAdmin = testUserProvider.getTestUserInfo();
		// A non-admin should not be able to start the daemon
		backupDaemonLauncher.startBackup(nonAdmin, null);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testNonAdminUserStartRestore() throws UnauthorizedException, DatastoreException{
		UserInfo nonAdmin = testUserProvider.getTestUserInfo();
		// A non-admin should not be able to start the daemon
		backupDaemonLauncher.startRestore(nonAdmin, "SomeFile");
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testNonAdminUserGetStatus() throws UnauthorizedException, DatastoreException, NotFoundException{
		UserInfo nonAdmin = testUserProvider.getTestUserInfo();
		// A non-admin should not be able to start the daemon
		backupDaemonLauncher.getStatus(nonAdmin, "123");
	}
	
	@Test
	public void testRoundTrip() throws UnauthorizedException, DatastoreException, NotFoundException, InterruptedException, InvalidModelException{
		// First create a node using random datat
		Node node = new Node();
		node.setName("BackupDaemonLauncherImplAutowireTest.testRoundTrip");
		node.setNodeType(EntityType.project.name());
		UserInfo nonAdmin = testUserProvider.getTestAdminUserInfo();
		Annotations annos = RandomAnnotationsUtil.generateRandom(12334, 100);
		NamedAnnotations named = new NamedAnnotations();
		named.put(NamedAnnotations.NAME_SPACE_ADDITIONAL, annos);
		String id = nodeManager.createNewNode(node, named, nonAdmin);
		assertNotNull(id);
		nodesToDelete.add(id);
		// Fetch them back
		node = nodeManager.get(nonAdmin, id);
		named = nodeManager.getAnnotations(nonAdmin, id);
		annos = named.getAdditionalAnnotations();
		
		
		// First start the backup daemon as an administrator
		UserInfo admin = testUserProvider.getTestAdminUserInfo();
		BackupRestoreStatus status = backupDaemonLauncher.startBackup(admin, null);
		assertNotNull(status);
		assertNotNull(status.getId());
		// Wait for it finish
		status = waitForStatus(STATUS.COMPLETED, status.getId());
		assertNotNull(status.getBackupUrl());
		String fullUrl = status.getBackupUrl();
		System.out.println(fullUrl);
		int index = fullUrl.lastIndexOf("/");
		String fileName = status.getBackupUrl().substring(index+1, fullUrl.length());
		
		// Now delete the node
		nodeManager.delete(nonAdmin, id);
		
		// Now restore the node from the backup
		status = backupDaemonLauncher.startRestore(admin, fileName);
		assertNotNull(status);
		assertNotNull(status.getId());
		// Wait for it finish
		status = waitForStatus(STATUS.COMPLETED, status.getId());
		assertNotNull(status.getBackupUrl());
		System.out.println(status.getBackupUrl());
		// Now make sure the node it back.
		Node nodeClone = nodeManager.get(nonAdmin, id);
		assertEquals(node, nodeClone);
		NamedAnnotations namedClone = nodeManager.getAnnotations(nonAdmin, id);
		Annotations annosClone = namedClone.getAdditionalAnnotations();
		assertEquals(annos, annosClone);
	}
	
	@Test
	public void testBatchRoundTrip() throws UnauthorizedException, DatastoreException, NotFoundException, InterruptedException, InvalidModelException{
		// First create a node using random datat
		Node node = new Node();
		node.setName("BackupDaemonLauncherImplAutowireTest.testBatchRoundTrip");
		node.setNodeType(EntityType.project.name());
		UserInfo nonAdmin = testUserProvider.getTestAdminUserInfo();
		Annotations annos = RandomAnnotationsUtil.generateRandom(12334, 100);
		NamedAnnotations named = new NamedAnnotations();
		named.put(NamedAnnotations.NAME_SPACE_ADDITIONAL, annos);
		String id = nodeManager.createNewNode(node, named, nonAdmin);
		assertNotNull(id);
		nodesToDelete.add(id);
		// Fetch them back
		node = nodeManager.get(nonAdmin, id);
		named = nodeManager.getAnnotations(nonAdmin, id);
		annos = named.getAdditionalAnnotations();
		
		
		// First start the backup daemon as an administrator
		UserInfo admin = testUserProvider.getTestAdminUserInfo();
		HashSet<String> batch = new HashSet<String>();
		batch.add(id);
		BackupRestoreStatus status = backupDaemonLauncher.startBackup(admin, batch);
		assertNotNull(status);
		assertNotNull(status.getId());
		// Wait for it finish
		status = waitForStatus(STATUS.COMPLETED, status.getId());
		assertNotNull(status.getBackupUrl());
		String fullUrl = status.getBackupUrl();
		System.out.println(fullUrl);
		int index = fullUrl.lastIndexOf("/");
		String fileName = status.getBackupUrl().substring(index+1, fullUrl.length());
		
		// Now delete the node
		nodeManager.delete(nonAdmin, id);
		
		// Now restore the node from the backup
		status = backupDaemonLauncher.startRestore(admin, fileName);
		assertNotNull(status);
		assertNotNull(status.getId());
		// Wait for it finish
		status = waitForStatus(STATUS.COMPLETED, status.getId());
		assertNotNull(status.getBackupUrl());
		System.out.println(status.getBackupUrl());
		// Now make sure the node it back.
		Node nodeClone = nodeManager.get(nonAdmin, id);
		assertEquals(node, nodeClone);
		NamedAnnotations namedClone = nodeManager.getAnnotations(nonAdmin, id);
		Annotations annosClone = namedClone.getAdditionalAnnotations();
		assertEquals(annos, annosClone);
	}
	
	
	/**
	 * Helper method to wait for a given status of the Daemon
	 * @param lookinFor
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InterruptedException
	 * @throws UnauthorizedException 
	 */
	private BackupRestoreStatus waitForStatus(STATUS lookinFor, String id) throws DatastoreException, NotFoundException, InterruptedException, UnauthorizedException{
		BackupRestoreStatus status = backupDaemonLauncher.getStatus(testUserProvider.getTestAdminUserInfo(), id);
		long start = System.currentTimeMillis();
		long elapse = 0;
		while(!lookinFor.name().equals(status.getStatus())){
			// Wait for it to complete
			Thread.sleep(1000);
			long end =  System.currentTimeMillis();
			elapse = end-start;
			if(elapse > TIMEOUT){
				fail("Timmed out waiting for the backup deamon to finish");
			}
			status = backupDaemonLauncher.getStatus(testUserProvider.getTestAdminUserInfo(), id);
			assertEquals(id, status.getId());
			System.out.println(status.printStatus());
			if(STATUS.FAILED != lookinFor && STATUS.FAILED.name().equals(status.getStatus())){
				fail("Unexpected failure: "+status.getErrorMessage()+" "+status.getErrorDetails());
			}
		}
		return status;
	}
	

}
