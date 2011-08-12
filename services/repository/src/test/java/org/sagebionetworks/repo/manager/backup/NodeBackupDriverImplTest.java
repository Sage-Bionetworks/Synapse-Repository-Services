package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeRevision;
import org.sagebionetworks.repo.model.util.RandomAccessControlListUtil;
import org.sagebionetworks.repo.model.util.RandomNodeRevisionUtil;
import org.sagebionetworks.repo.model.util.RandomNodeUtil;
import org.sagebionetworks.repo.web.NotFoundException;

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
		stubSource = new NodeBackupStub(root);
		stubDestination = new NodeBackupStub();
		sourceDriver = new NodeBackupDriverImpl(stubSource);
		destinationDriver = new NodeBackupDriverImpl(stubDestination);
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
			NodeRevision rev = RandomNodeRevisionUtil.generateRandom(rand, annoCount);
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
			sourceDriver.writeBackup(temp, progress);
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

	@Test (expected=InterruptedException.class)
	public void testTerminateWrite() throws IOException, DatastoreException, NotFoundException, InterruptedException{
		// Create a temp file
		File temp = File.createTempFile("NodeBackupDriverImplTest", ".zip");
		try{
			// Try to write to the temp file
			Progress progress = new Progress();
			// This should trigger a termination
			progress.setTerminate(true);
			sourceDriver.writeBackup(temp, progress);
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
			sourceDriver.writeBackup(temp, progress);
			// This should trigger a termination
			progress = new Progress();
			progress.setTerminate(true);
			sourceDriver.restoreFromBackup(temp, progress);
		}finally{
			// Cleanup the file
			temp.delete();
		}
	}
}
