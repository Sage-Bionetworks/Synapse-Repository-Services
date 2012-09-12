package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.BatchUpdateException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.sagebionetworks.repo.manager.backup.migration.MigrationDriver;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.util.RandomAccessControlListUtil;
import org.sagebionetworks.repo.model.util.RandomNodeRevisionUtil;
import org.sagebionetworks.repo.model.util.RandomNodeUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.dao.DeadlockLoserDataAccessException;

/**
 * This is a unit test for NodeBackupDriverImpl.
 * @author jmhill
 *
 */
public class NodeBackupDriverImplTest {
	
	NodeBackupStub stubSource = null;
	NodeBackupStub stubDestination = null;;
	NodeBackupDriverImpl sourceDriver = null;
	NodeBackupDriverImpl destinationDriver = null;
	MigrationDriver mockMigrationDriver;
	
	@Before
	public void before(){
		// For this test we are using a stub node source, we are also using randomly generated nodes
		Random rand = new Random(5445);
		TreeNodeBackup root = NodeBackupDriverImplTest.generateRandomLeaf(rand, true, 3, 12);
		root.getNode().setParentId(null);
		// Create some children
		for(int i=0; i<8; i++){
			int numberRevs = rand.nextInt(5)+1;
			int numberAnnos = rand.nextInt(30)+5;
			boolean hasACL = rand.nextBoolean();
			TreeNodeBackup child = NodeBackupDriverImplTest.generateRandomLeaf(rand, hasACL, numberRevs, numberAnnos);
			root.getChildren().add(child);
			// some of these nodes should have children
			if(i % 2 == 0){
				TreeNodeBackup grandChild = NodeBackupDriverImplTest.generateRandomLeaf(rand, rand.nextBoolean(), rand.nextInt(3)+1, rand.nextInt(23)+4);
				child.getChildren().add(grandChild);
				// Add a great grand child to each
				TreeNodeBackup great = NodeBackupDriverImplTest.generateRandomLeaf(rand, rand.nextBoolean(), rand.nextInt(3)+1, rand.nextInt(23)+4);
				grandChild.getChildren().add(great);
			}
		}
//		// Add an entity type that no longer exits
//		TreeNodeBackup child = NodeBackupDriverImplTest.generateRandomLeaf(rand, true, 2, 3);
//		child.getNode().setNodeType("unknownType");
//		root.getChildren().add(child);
		stubSource = new NodeBackupStub(root);
		stubDestination = new NodeBackupStub();
		mockMigrationDriver = new MockMigrationDriver();
		sourceDriver = new NodeBackupDriverImpl(stubSource, mockMigrationDriver);
		destinationDriver = new NodeBackupDriverImpl(stubDestination, mockMigrationDriver) ;
	}
	
	@Test
	public void testIsNodeBackupFile(){
		assertTrue(NodeBackupDriverImpl.isNodeBackupFile("0/node.xml"));
		assertTrue(NodeBackupDriverImpl.isNodeBackupFile("0/5/6/node.xml"));
		assertFalse(NodeBackupDriverImpl.isNodeBackupFile("0/5/revisions/3.xml"));
		assertFalse(NodeBackupDriverImpl.isNodeBackupFile("anything"));
	}
	
	@Test
	public void testIsNodeRevisionFile(){
		assertTrue(NodeBackupDriverImpl.isNodeRevisionFile("0/9/10/11/revisions/1.xml"));
		assertTrue(NodeBackupDriverImpl.isNodeRevisionFile("0/revisions/2.xml"));
		assertFalse(NodeBackupDriverImpl.isNodeRevisionFile("0/4/node.xml"));
		assertFalse(NodeBackupDriverImpl.isNodeRevisionFile("anything"));
	}
	
	/**
	 * Generate a random leave node.
	 * @param rand
	 * @param hasACL
	 * @param numberRevs
	 * @param annoCount
	 * @return
	 */
	public static TreeNodeBackup generateRandomLeaf(Random rand, boolean hasACL, int numberRevs, int annoCount){
		TreeNodeBackup node = new TreeNodeBackup();
		node.setNode(RandomNodeUtil.generateRandom(rand));
		if(hasACL){
			node.setAcl(RandomAccessControlListUtil.generateRandom(rand));
		}
		for(int i=0; i<numberRevs; i++){
			NodeRevisionBackup rev = RandomNodeRevisionUtil.generateRandom(rand, annoCount);
			node.getRevisions().add(rev);
		}
		return node;
	}
	
	@Test
	public void testRoundTrip() throws IOException, DatastoreException, NotFoundException, InterruptedException{
		// Create a temp file
		File temp = File.createTempFile("NodeBackupDriverImplTest", ".zip");
		try{
			// Try to write to the temp file
			Progress progress = new Progress();
			sourceDriver.writeBackup(temp, progress, null);
			System.out.println("Resulting file: "+temp.getAbsolutePath()+" with a size of: "+temp.length()+" bytes");
			assertTrue(temp.length() > 10);
			// They should start off as non equal
			assertFalse(stubSource.equals(stubDestination));
			// Now read push the backup
			progress = new Progress();
			destinationDriver.restoreFromBackup(temp, progress);
			// At this point all of the data should have migrated from the source to the destination
			assertEquals(stubSource, stubDestination);
		}finally{
			// Cleanup the file
			temp.delete();
		}
	}
	
	@Test
	public void testRoundTripSubSet() throws IOException, DatastoreException, NotFoundException, InterruptedException{
		// Create a temp file
		File temp = File.createTempFile("NodeBackupDriverImplTest", ".zip");
		try{
			// Try to write to the temp file
			Set<String> idsToBackup = new HashSet<String>();
			// Add the root node.
			idsToBackup.add(stubSource.getRoot().getNode().getId());
			
			Progress progress = new Progress();
			sourceDriver.writeBackup(temp, progress, idsToBackup);
			System.out.println("Resulting file: "+temp.getAbsolutePath()+" with a size of: "+temp.length()+" bytes");
			assertTrue(temp.length() > 10);
			// They should start off as non equal
			assertEquals(0, stubDestination.getTotalNodeCount());
			// Now read push the backup
			progress = new Progress();
			destinationDriver.restoreFromBackup(temp, progress);
			// At this point all of the data should have migrated from the source to the destination
			assertEquals(idsToBackup.size(), stubDestination.getTotalNodeCount());
			// Make sure we can find each of the children
			for(String id: idsToBackup){
				assertNotNull(stubDestination.getNode(id));
			}
			// Now backup and restore some children
			idsToBackup = new HashSet<String>();
			idsToBackup.add(stubSource.getRoot().getChildren().get(2));
			idsToBackup.add(stubSource.getRoot().getChildren().get(1));
			idsToBackup.add(stubSource.getRoot().getChildren().get(0));
			progress = new Progress();
			sourceDriver.writeBackup(temp, progress, idsToBackup);
			System.out.println("Resulting file: "+temp.getAbsolutePath()+" with a size of: "+temp.length()+" bytes");
			assertTrue(temp.length() > 10);
			// They should start off as non equal
			assertEquals(1, stubDestination.getTotalNodeCount());
			// Now read push the backup
			progress = new Progress();
			destinationDriver.restoreFromBackup(temp, progress);
			// Make sure we can find each of the children
			for(String id: idsToBackup){
				assertNotNull(stubDestination.getNode(id));
			}

		}finally{
			// Cleanup the file
			temp.delete();
		}
	}
	
	/**
	 * For this case we are testing that if the current root does not match what comes in from an update, that it gets replaces.
	 * @throws Exception
	 */
	@Test
	public void testReplaceRoot() throws Exception{
		// Create a temp file
		File temp = File.createTempFile("NodeBackupDriverImplTest", ".zip");
		try{
			// Try to write to the temp file
			Progress progress = new Progress();
			sourceDriver.writeBackup(temp, progress, null);
			String savedRootId = stubSource.getRootId();
			System.out.println("Resulting file: "+temp.getAbsolutePath()+" with a size of: "+temp.length()+" bytes");
			assertTrue(temp.length() > 10);
			// Now that we have a backup, create a new destination with datat
			TreeNodeBackup newRoot = NodeBackupDriverImplTest.generateRandomLeaf(new Random(5445), true, 3, 12);
			newRoot.getNode().setParentId(null);
			// This new root should get deleted
			stubDestination = new NodeBackupStub(newRoot, 10);
			destinationDriver = new NodeBackupDriverImpl(stubDestination, mockMigrationDriver);
			String newRootId = stubDestination.getRootId();
			// Now read push the backup
			progress = new Progress();
			destinationDriver.restoreFromBackup(temp, progress);
			// At this point all of the data should have migrated from the source to the destination
			assertFalse(newRootId.equals(stubDestination.getRootId()));
			assertEquals(savedRootId, stubDestination.getRootId());
			assertTrue("For this case the root node should have been replaced!", stubDestination.getWasCleared());
		}finally{
			// Cleanup the file
			temp.delete();
		}
	}
	
	/**
	 * For this case the root already exists and should not be deleted.
	 * @throws Exception
	 */
	@Test
	public void testUpdateRoot() throws Exception{
		// Create a temp file
		File temp = File.createTempFile("NodeBackupDriverImplTest", ".zip");
		try{
			// Try to write to the temp file
			Progress progress = new Progress();
			sourceDriver.writeBackup(temp, progress, null);
			String savedRootId = stubSource.getRootId();
			System.out.println("Resulting file: "+temp.getAbsolutePath()+" with a size of: "+temp.length()+" bytes");
			assertTrue(temp.length() > 10);
			// Now that we have a backup, create a new destination with datat
			TreeNodeBackup newRoot = NodeBackupDriverImplTest.generateRandomLeaf(new Random(5445), true, 3, 12);
			newRoot.getNode().setParentId(null);
			// Start the destination with the exact same id as the current
			stubDestination = new NodeBackupStub(newRoot, 0);
			
			destinationDriver = new NodeBackupDriverImpl(stubDestination, mockMigrationDriver);
			String newRootId = stubDestination.getRootId();
			// Now read push the backup
			progress = new Progress();
			destinationDriver.restoreFromBackup(temp, progress);
			// At this point all of the data should have migrated from the source to the destination
			assertTrue(newRootId.equals(stubDestination.getRootId()));
			assertEquals(savedRootId, stubDestination.getRootId());
			assertFalse("For this case the root node should have been updated and not replaced!", stubDestination.getWasCleared());
		}finally{
			// Cleanup the file
			temp.delete();
		}
	}

	@Test (expected=InterruptedException.class)
	public void testTerminateWrite() throws IOException, DatastoreException, NotFoundException, InterruptedException{
		// Create a temp file
		File temp = File.createTempFile("NodeBackupDriverImplTest", ".zip");
		try{
			// Try to write to the temp file
			Progress progress = new Progress();
			// This should trigger a termination
			progress.setTerminate(true);
			sourceDriver.writeBackup(temp, progress, null);
		}finally{
			// Cleanup the file
			temp.delete();
		}
	}
	
	@Test (expected=InterruptedException.class)
	public void testTerminateRestore() throws IOException, DatastoreException, NotFoundException, InterruptedException{
		// Create a temp file
		File temp = File.createTempFile("NodeBackupDriverImplTest", ".zip");
		try{
			// Try to write to the temp file
			Progress progress = new Progress();
			sourceDriver.writeBackup(temp, progress, null);
			// This should trigger a termination
			progress = new Progress();
			progress.setTerminate(true);
			sourceDriver.restoreFromBackup(temp, progress);
		}finally{
			// Cleanup the file
			temp.delete();
		}
	}
	
	@Test
	public void testUnknownType() throws IOException, DatastoreException, NotFoundException, InterruptedException{
		// create a root
		Random rand = new Random(5445);
		TreeNodeBackup root = NodeBackupDriverImplTest.generateRandomLeaf(rand, true, 3, 12);
		root.getNode().setParentId(null);
		// Add a child of an unknown type
		TreeNodeBackup child = NodeBackupDriverImplTest.generateRandomLeaf(rand, true, 2, 3);
		child.getNode().setNodeType("unknownType");
		root.getChildren().add(child);
		stubSource = new NodeBackupStub(root);
		stubDestination = new NodeBackupStub();
		sourceDriver = new NodeBackupDriverImpl(stubSource, mockMigrationDriver);
		destinationDriver = new NodeBackupDriverImpl(stubDestination, mockMigrationDriver);

		// Now test a round trip
		// Create a temp file
		File temp = File.createTempFile("NodeBackupDriverImplTest", ".zip");
		try{
			// Try to write to the temp file
			Progress progress = new Progress();
			sourceDriver.writeBackup(temp, progress, null);
			System.out.println("Resulting file: "+temp.getAbsolutePath()+" with a size of: "+temp.length()+" bytes");
			assertTrue(temp.length() > 10);
			// They should start off as non equal
			assertFalse(stubSource.equals(stubDestination));
			// Now read push the backup
			progress = new Progress();
			destinationDriver.restoreFromBackup(temp, progress);
			// The destination should have only the root.
			assertNotNull(stubDestination.getRoot());
			// It should not have any children
			assertEquals(1, stubDestination.getTotalNodeCount());
		}finally{
			// Cleanup the file
			temp.delete();
		}
	}
	
	@Test
	public void testCreateOrUpdateDeadLock() throws InterruptedException{
		// Mock the 
		NodeBackupManager mockManager = Mockito.mock(NodeBackupManager.class);
		MigrationDriver mockDriver = Mockito.mock(MigrationDriver.class);
		NodeBackupDriverImpl driver = new NodeBackupDriverImpl(mockManager, mockDriver);
		NodeBackup backup = new NodeBackup();
		List<NodeRevisionBackup> revs = new LinkedList<NodeRevisionBackup>();
		// Now simulate deadlock
		DeadlockLoserDataAccessException exception = new DeadlockLoserDataAccessException("Deadlock", new BatchUpdateException());
		doThrow(exception).when(mockManager).createOrUpdateNodeWithRevisions(backup, revs);
		try{
			driver.createOrUpdateNodeWithRevisions(backup, revs);
			fail("Expected a DeadlockLoserDataAccessException");
		}catch(DeadlockLoserDataAccessException e){
			// expected.
		}
		// Verify that we tried twice
		verify(mockManager, times(2)).createOrUpdateNodeWithRevisions(backup, revs);
	}
}
