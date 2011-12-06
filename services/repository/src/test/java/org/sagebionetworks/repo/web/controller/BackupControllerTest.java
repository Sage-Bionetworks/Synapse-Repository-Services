package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.RestoreFile;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.registry.backup.BackupSubmission;
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
	
	/**
	 * This test will attempt to backup the entire repository and then restore it.
	 */
	@Test
	public void testRoundTrip() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ServletException, IOException, InterruptedException{
		UserInfo nonAdmin = testUserProvider.getTestAdminUserInfo();

		Node nodeWithAnnotations = new Node();
		nodeWithAnnotations.setName("BackupDaemonLauncherImplAutowireTest.testRoundTrip Annotations");
		nodeWithAnnotations.setNodeType(EntityType.project.name());
		Annotations annos = RandomAnnotationsUtil.generateRandom(334, 50);
		NamedAnnotations named = new NamedAnnotations();
		named.put(NamedAnnotations.NAME_SPACE_ADDITIONAL, annos);
		String idOfNodeWithAnnotations = nodeManager.createNewNode(nodeWithAnnotations, named, nonAdmin);
		assertNotNull(idOfNodeWithAnnotations);
		toDelete.add(idOfNodeWithAnnotations);

		Node nodeWithRefs = new Node();
		nodeWithRefs.setName("BackupDaemonLauncherImplAutowireTest.testRoundTrip References");
		nodeWithRefs.setNodeType(EntityType.project.name());
		Reference reference = new Reference();
		reference.setTargetId(idOfNodeWithAnnotations);
		reference.setTargetVersionNumber(42L);
		Set<Reference> referenceGroup = new HashSet<Reference>();
		referenceGroup.add(reference);
		Map<String, Set<Reference>> referenceGroups = new HashMap<String, Set<Reference>>();
		referenceGroups.put("backedUpRefs", referenceGroup);
		nodeWithRefs.setReferences(referenceGroups);
		String idOfNodeWithRefs = nodeManager.createNewNode(nodeWithRefs, new NamedAnnotations(), nonAdmin);
		assertNotNull(idOfNodeWithRefs);
		toDelete.add(idOfNodeWithRefs);

		
		// Fetch them back
		nodeWithAnnotations = nodeManager.get(nonAdmin, idOfNodeWithAnnotations);
		named = nodeManager.getAnnotations(nonAdmin, idOfNodeWithAnnotations);
		annos = named.getAdditionalAnnotations();
		nodeWithRefs = nodeManager.get(nonAdmin, idOfNodeWithRefs);
		
		// Start a backup
		BackupRestoreStatus status = ServletTestHelper.startBackup(dispatchServlet, adminUserName, null);
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
		
		// Now delete the nodes
		nodeManager.delete(nonAdmin, idOfNodeWithAnnotations);
		nodeManager.delete(nonAdmin, idOfNodeWithRefs);
		
		// Now restore the nodes from the backup
		RestoreFile file = new RestoreFile(fileName);
		status = ServletTestHelper.startRestore(dispatchServlet, adminUserName, file);
		assertNotNull(status);
		assertNotNull(status.getId());
		// Wait for it finish
		status = waitForStatus(STATUS.COMPLETED, status.getId());
		assertNotNull(status.getBackupUrl());
		System.out.println(status.getBackupUrl());
		// Now make sure the nodes are resurrected
		Node nodeWithAnnotationsClone = nodeManager.get(nonAdmin, idOfNodeWithAnnotations);
		assertEquals(nodeWithAnnotations, nodeWithAnnotationsClone);
		NamedAnnotations namedClone = nodeManager.getAnnotations(nonAdmin, idOfNodeWithAnnotations);
		Annotations annosClone = namedClone.getAdditionalAnnotations();
		assertEquals(annos, annosClone);
		Node nodeWithRefsClone = nodeManager.get(nonAdmin, idOfNodeWithRefs);
		assertEquals(referenceGroups, nodeWithRefsClone.getReferences());
	
	}
	
	/**
	 * This test attempts to create a backup of a single node and restore it.  This should not trigger an full backup or restore.
	 */
	@Test
	public void testBatchRoundTrip() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ServletException, IOException, InterruptedException{
		UserInfo nonAdmin = testUserProvider.getTestAdminUserInfo();

		Node nodeWithAnnotations = new Node();
		nodeWithAnnotations.setName("testBatchRoundTripAnnotations");
		nodeWithAnnotations.setNodeType(EntityType.project.name());
		Annotations annos = RandomAnnotationsUtil.generateRandom(334, 50);
		NamedAnnotations named = new NamedAnnotations();
		named.put(NamedAnnotations.NAME_SPACE_ADDITIONAL, annos);
		String idOfNodeWithAnnotations = nodeManager.createNewNode(nodeWithAnnotations, named, nonAdmin);
		assertNotNull(idOfNodeWithAnnotations);
		toDelete.add(idOfNodeWithAnnotations);

		Node nodeWithRefs = new Node();
		nodeWithRefs.setName("testBatchRoundTrip References");
		nodeWithRefs.setNodeType(EntityType.project.name());
		Reference reference = new Reference();
		reference.setTargetId(idOfNodeWithAnnotations);
		reference.setTargetVersionNumber(42L);
		Set<Reference> referenceGroup = new HashSet<Reference>();
		referenceGroup.add(reference);
		Map<String, Set<Reference>> referenceGroups = new HashMap<String, Set<Reference>>();
		referenceGroups.put("backedUpRefs", referenceGroup);
		nodeWithRefs.setReferences(referenceGroups);
		String idOfNodeWithRefs = nodeManager.createNewNode(nodeWithRefs, new NamedAnnotations(), nonAdmin);
		assertNotNull(idOfNodeWithRefs);
		toDelete.add(idOfNodeWithRefs);

		
		// Fetch them back
		nodeWithAnnotations = nodeManager.get(nonAdmin, idOfNodeWithAnnotations);
		named = nodeManager.getAnnotations(nonAdmin, idOfNodeWithAnnotations);
		annos = named.getAdditionalAnnotations();
		nodeWithRefs = nodeManager.get(nonAdmin, idOfNodeWithRefs);
		
		// Start a backup
		BackupSubmission submission = new BackupSubmission();
		submission.setEntityIdsToBackup(new HashSet<String>());
		// We are just going to backup the node with references.
		submission.getEntityIdsToBackup().add(idOfNodeWithRefs);
		BackupRestoreStatus status = ServletTestHelper.startBackup(dispatchServlet, adminUserName, submission);
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
		
		// Now delete both nodes that we created.
		nodeManager.delete(nonAdmin, idOfNodeWithAnnotations);
		nodeManager.delete(nonAdmin, idOfNodeWithRefs);
		
		// Now restore the nodes from the backup
		RestoreFile file = new RestoreFile(fileName);
		// The backup should only restore a single node.
		status = ServletTestHelper.startRestore(dispatchServlet, adminUserName, file);
		assertNotNull(status);
		assertNotNull(status.getId());
		// Wait for it finish
		status = waitForStatus(STATUS.COMPLETED, status.getId());
		assertNotNull(status.getBackupUrl());
		System.out.println(status.getBackupUrl());
		// Now make sure the nodes are resurrected
		try{
			nodeManager.get(nonAdmin, idOfNodeWithAnnotations);
			fail("The first node was not included in the restore so it should no longer exist");
		}catch(NotFoundException e){
			// This is expected
		}
		Node nodeWithRefsClone = nodeManager.get(nonAdmin, idOfNodeWithRefs);
		assertEquals(referenceGroups, nodeWithRefsClone.getReferences());
	}
	
	/**
	 * This test attempts to create a backup of a single node and restore it.
	 * In this case the the node being restore will still exist with changes.
	 * We expect that existing node be restore to it same state as when the backup was taken.
	 */
	@Test
	public void testRestoreExisting() throws Exception{
		UserInfo nonAdmin = testUserProvider.getTestAdminUserInfo();

		Node nodeWithAnnotations = new Node();
		nodeWithAnnotations.setName("testRestoreExisting");
		nodeWithAnnotations.setNodeType(EntityType.project.name());
		// Add a single annotation to a node.
		Annotations annos = new Annotations();
		String stringKey = "someStringKey";
		String value = "this is the starting string value";
		annos.addAnnotation(stringKey, value);
		NamedAnnotations named = new NamedAnnotations();
		named.put(NamedAnnotations.NAME_SPACE_ADDITIONAL, annos);
		String idOfNodeWithAnnotations = nodeManager.createNewNode(nodeWithAnnotations, named, nonAdmin);
		assertNotNull(idOfNodeWithAnnotations);
		toDelete.add(idOfNodeWithAnnotations);
		
		// Fetch them back
		nodeWithAnnotations = nodeManager.get(nonAdmin, idOfNodeWithAnnotations);
		named = nodeManager.getAnnotations(nonAdmin, idOfNodeWithAnnotations);
		annos = named.getAdditionalAnnotations();
		
		// Start a backup
		BackupSubmission submission = new BackupSubmission();
		submission.setEntityIdsToBackup(new HashSet<String>());
		// We are just creating a backup of a single node.
		submission.getEntityIdsToBackup().add(idOfNodeWithAnnotations);
		BackupRestoreStatus status = ServletTestHelper.startBackup(dispatchServlet, adminUserName, submission);
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
		
		// Now that we have our backup copy, change the node in the repository.
		String valueChangedAfterBackup = "this is the value that was set after a backup copy was made.";
		annos.replaceAnnotation(stringKey, valueChangedAfterBackup);
		annos = nodeManager.updateAnnotations(nonAdmin, idOfNodeWithAnnotations, annos, NamedAnnotations.NAME_SPACE_ADDITIONAL);
		assertEquals(valueChangedAfterBackup, annos.getSingleValue(stringKey));
		
		// Now restore this node from the backup.  The results should be replacing the 
		// with its original value.
		// the value from before 
		RestoreFile file = new RestoreFile(fileName);
		// The backup should only restore a single node.
		status = ServletTestHelper.startRestore(dispatchServlet, adminUserName, file);
		assertNotNull(status);
		assertNotNull(status.getId());
		// Wait for it finish
		status = waitForStatus(STATUS.COMPLETED, status.getId());
		assertNotNull(status.getBackupUrl());
		System.out.println(status.getBackupUrl());
		// Now fetch the nodee
		Node backFromRestore = nodeManager.get(nonAdmin, idOfNodeWithAnnotations);
		assertNotNull(backFromRestore);
		NamedAnnotations namedAfterRestore = nodeManager.getAnnotations(nonAdmin, idOfNodeWithAnnotations);
		Annotations annosAfterRestore = namedAfterRestore.getAdditionalAnnotations();
		assertNotNull(annosAfterRestore);
		// Make sure the value is the same as the original value.
		assertEquals(value, annosAfterRestore.getSingleValue(stringKey));
	}
	
	@Test
	public void testTermianteBackup() throws ServletException, IOException, DatastoreException, NotFoundException, InterruptedException, UnauthorizedException{
		// Make sure we can terminate the job even though we are done
		BackupRestoreStatus status = ServletTestHelper.startBackup(dispatchServlet, adminUserName, null);
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
		BackupRestoreStatus status = ServletTestHelper.getDaemonStatus(dispatchServlet, adminUserName, id);
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
			status = ServletTestHelper.getDaemonStatus(dispatchServlet, adminUserName, id);
			assertEquals(id, status.getId());
			System.out.println(status.printStatus());
			if(STATUS.FAILED != lookinFor && STATUS.FAILED.name().equals(status.getStatus())){
				fail("Unexpected failure: "+status.getErrorMessage()+" "+status.getErrorDetails());
			}
		}
		return status;
	}


}
