package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.BackupRestoreStatus;
import org.sagebionetworks.repo.model.BackupRestoreStatus.STATUS;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestoreFile;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.util.RandomAnnotationsUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class BackupControllerTest {
	
	private static final long TIMEOUT = 1000*60*1; // 1 minutes

	@Autowired
	public UserManager userManager;
	
	@Autowired
	public NodeManager nodeManager;
	
	private static HttpServlet dispatchServlet;
	
	@Autowired
	private UserProvider testUserProvider;
	
	private List<String> toDelete;
	private String adminUserName;

	@Before
	public void before() throws DatastoreException, NotFoundException {
		toDelete = new ArrayList<String>();
		adminUserName = testUserProvider.getTestAdminUserInfo().getUser().getUserId();
	}
	
	@BeforeClass
	public static void beforeClass() throws ServletException {
		// Setup the servlet once
		// Create a Spring MVC DispatcherServlet so that we can test our URL
		// mapping, request format, response format, and response status
		// code.
		MockServletConfig servletConfig = new MockServletConfig("repository");
		servletConfig.addInitParameter("contextConfigLocation",	"classpath:test-context.xml");
		dispatchServlet = new DispatcherServlet();
		dispatchServlet.init(servletConfig);

	}

	@After
	public void after() throws UnauthorizedException {
		if (nodeManager != null && toDelete != null) {
			for (String idToDelete : toDelete) {
				try {
					nodeManager.delete(testUserProvider.getTestAdminUserInfo(), idToDelete);
				} catch (NotFoundException e) {
					// nothing to do here
				} catch (DatastoreException e) {
					// nothing to do here.
				}
			}
		}
	}
	
	@Test
	public void testRoundTrip() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ServletException, IOException, InterruptedException{
		Node node = new Node();
		node.setName("BackupDaemonLauncherImplAutowireTest.testRoundTrip");
		node.setNodeType(ObjectType.project.name());
		UserInfo nonAdmin = testUserProvider.getTestAdminUserInfo();
		Annotations annos = RandomAnnotationsUtil.generateRandom(334, 50);
		NamedAnnotations named = new NamedAnnotations();
		named.put(NamedAnnotations.NAME_SPACE_ADDITIONAL, annos);
		String id = nodeManager.createNewNode(node, named, nonAdmin);
		assertNotNull(id);
		toDelete.add(id);
		// Fetch them back
		node = nodeManager.get(nonAdmin, id);
		named = nodeManager.getAnnotations(nonAdmin, id);
		annos = named.getAdditionalAnnotations();
		
		// Start a backup
		BackupRestoreStatus status = ServletTestHelper.startBackup(dispatchServlet, adminUserName);
		// Wait for it to finish
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
		RestoreFile file = new RestoreFile(fileName);
		status = ServletTestHelper.startRestore(dispatchServlet, adminUserName, file);
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
	public void testTermiante() throws ServletException, IOException, DatastoreException, NotFoundException, InterruptedException, UnauthorizedException{
		// Make sure we can terminate the job even though we are done
		BackupRestoreStatus status = ServletTestHelper.startBackup(dispatchServlet, adminUserName);
		// Wait for it to finish
		assertNotNull(status);
		assertNotNull(status.getId());
		
		// Make sure we can terminate the job even though it is done
		ServletTestHelper.terminateDaemon(dispatchServlet, adminUserName, status.getId());
		// The job is likely to complete before the termination takes effect so we cannot test for actually 
		// stopping the daemon as an integration test.  This is well covered as a unit test.
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
	 * @throws IOException 
	 * @throws ServletException 
	 */
	private BackupRestoreStatus waitForStatus(STATUS lookinFor, String id) throws DatastoreException, NotFoundException, InterruptedException, UnauthorizedException, ServletException, IOException{
		BackupRestoreStatus status = ServletTestHelper.getStatus(dispatchServlet, adminUserName, id);
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
			status = ServletTestHelper.getStatus(dispatchServlet, adminUserName, id);
			assertEquals(id, status.getId());
			System.out.println(status.printStatus());
			if(STATUS.FAILED != lookinFor && STATUS.FAILED.name().equals(status.getStatus())){
				fail("Unexpected failure: "+status.getErrorMessage()+" "+status.getErrorDetails());
			}
		}
		return status;
	}


}
