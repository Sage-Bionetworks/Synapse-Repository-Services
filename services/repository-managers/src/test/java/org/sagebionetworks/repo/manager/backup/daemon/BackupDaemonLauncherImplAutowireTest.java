package org.sagebionetworks.repo.manager.backup.daemon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.DaemonStatusUtil;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
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
	public void testNonAdminUserGetStatus() throws UnauthorizedException, DatastoreException, NotFoundException{
		UserInfo nonAdmin = testUserProvider.getTestUserInfo();
		// A non-admin should not be able to start the daemon
		backupDaemonLauncher.getStatus(nonAdmin, "123");
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
	private BackupRestoreStatus waitForStatus(DaemonStatus lookinFor, String id) throws DatastoreException, NotFoundException, InterruptedException, UnauthorizedException{
		BackupRestoreStatus status = backupDaemonLauncher.getStatus(testUserProvider.getTestAdminUserInfo(), id);
		long start = System.currentTimeMillis();
		long elapse = 0;
		while(!lookinFor.equals(status.getStatus())){
			// Wait for it to complete
			Thread.sleep(1000);
			long end =  System.currentTimeMillis();
			elapse = end-start;
			if(elapse > TIMEOUT){
				fail("Timmed out waiting for the backup deamon to finish");
			}
			status = backupDaemonLauncher.getStatus(testUserProvider.getTestAdminUserInfo(), id);
			assertEquals(id, status.getId());
			System.out.println(DaemonStatusUtil.printStatus(status));
			if(DaemonStatus.FAILED != lookinFor && DaemonStatus.FAILED.equals(status.getStatus())){
				fail("Unexpected failure: "+status.getErrorMessage()+" "+status.getErrorDetails());
			}
		}
		return status;
	}
	

}
